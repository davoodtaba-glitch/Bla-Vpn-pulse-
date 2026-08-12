package com.xraypulse.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.OsConstants
import android.util.Log
import androidx.core.app.NotificationCompat
import com.xraypulse.app.MainActivity
import com.xraypulse.app.R
import com.xraypulse.app.core.config.XrayConfigBuilder
import com.xraypulse.app.core.vpn.HevTunnel
import com.xraypulse.app.core.xray.XrayController
import com.xraypulse.app.data.model.AppSettings
import com.xraypulse.app.data.model.ConnectionState
import com.xraypulse.app.data.model.ServerProfile
import com.xraypulse.app.util.JsonStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

/**
 * System VPN: Xray (SOCKS) + hev-socks5-tunnel (TUN → SOCKS).
 */
class XrayVpnService : VpnService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tunFd: ParcelFileDescriptor? = null
    private var statsJob: Job? = null
    private var healthJob: Job? = null
    private var keepAliveJob: Job? = null
    private var geoJob: Job? = null
    private var trafficProbeJob: Job? = null
    /** Settings for keep-alive / no-traffic probes (set on each connect). */
    @Volatile private var activeSettings: AppSettings = AppSettings()
    @Volatile private var trafficProbeInFlight: Boolean = false
    /** Last time we finished a no-traffic verification probe (success or fail). */
    @Volatile private var lastTrafficProbeAtMs: Long = 0L
    private var uiLanguage: String = "fa"

    private fun isFa(): Boolean =
        uiLanguage.lowercase() in listOf("fa", "fa-ir", "persian", "farsi")

    override fun onCreate() {
        super.onCreate()
        instance = this
        XrayController.initEnv(this)
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val profileJson = intent.getStringExtra(EXTRA_PROFILE) ?: return START_NOT_STICKY
                val settingsJson = intent.getStringExtra(EXTRA_SETTINGS)
                scope.launch { connect(profileJson, settingsJson) }
            }
            ACTION_DISCONNECT -> scope.launch { disconnect() }
            ACTION_RECONNECT -> {
                val profileJson = intent.getStringExtra(EXTRA_PROFILE)
                val settingsJson = intent.getStringExtra(EXTRA_SETTINGS)
                scope.launch {
                    cleanupCore()
                    if (profileJson != null) connect(profileJson, settingsJson)
                }
            }
        }
        return START_STICKY
    }

    private suspend fun connect(profileJson: String, settingsJson: String?) {
        _state.value = _state.value.copy(isConnecting = true, errorMessage = null, noTraffic = false)
        try {
            val profile = JsonStore.fromJson(profileJson, ServerProfile::class.java)
                ?: throw IllegalArgumentException("Invalid profile")
            val settings = settingsJson?.let {
                JsonStore.fromJson(it, AppSettings::class.java)
            } ?: AppSettings()

            // Stop previous cleanly
            cleanupCore()
            uiLanguage = settings.language

            val config = XrayConfigBuilder.build(profile, settings)
            Log.i(TAG, "DNS config:\n${XrayConfigBuilder.describeDnsConfig(settings)}")
            Log.d(TAG, "Starting Xray…")

            // 1) Start Xray first so SOCKS is ready before TUN packets arrive
            val err = XrayController.start(applicationContext, config)
            if (err != null) throw IllegalStateException(err)

            // Protect Xray outbound sockets from the VPN loop — core binds via Go;
            // also protect local SOCKS clients of hev by disallowed self package.
            delay(200)

            // 2) Establish TUN
            tunFd = buildVpnInterface(settings, profile)
            val fd = tunFd ?: throw IllegalStateException("VPN interface is null")

            // 3) TUN → SOCKS via hev-socks5-tunnel
            // ipv4 MUST match VpnService interface address (v2rayNG style)
            val mtu = settings.mtu.coerceIn(1280, 1500)
            val ok = HevTunnel.start(
                context = applicationContext,
                tun = fd,
                socksPort = settings.localSocksPort,
                mtu = mtu,
                ipv4 = VPN_IPV4,
                dnsListenPort = 10853
            )
            if (!ok) {
                throw IllegalStateException("Failed to start tun2socks (hev-socks5-tunnel)")
            }

            startForeground(NOTIFICATION_ID, buildNotification(profile.displayTitle(), true))
            activeSettings = settings
            lastTrafficProbeAtMs = 0L
            trafficProbeInFlight = false
            // Tunnel is up, but UI stays on Connecting until IP + country succeed.
            _state.value = ConnectionState(
                isConnected = true,
                isConnecting = true,
                selectedServerId = profile.id,
                selectedRemark = profile.displayTitle(),
                startTimeMs = System.currentTimeMillis(),
                noTraffic = false,
                publicIp = "",
                publicCountry = ""
            )
            startStatsLoop()
            startHealthWatch()
            startKeepAlive(settings)
            startIpGeoLookup(settings)
        } catch (e: Exception) {
            Log.e(TAG, "Connect failed", e)
            cleanupCore()
            _state.value = ConnectionState(
                isConnected = false,
                isConnecting = false,
                errorMessage = e.message
            )
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun buildVpnInterface(settings: AppSettings, profile: ServerProfile): ParcelFileDescriptor {
        val mtu = settings.mtu.coerceIn(1280, 1500)
        val builder = Builder()
            .setSession("XrayPulse · ${profile.displayTitle()}")
            .setMtu(mtu)
            // Same address as hev tunnel.ipv4 — required for correct tun2socks routing
            .addAddress(VPN_IPV4, 30)
            // Authoritative DNS: ONLY the IPs derived from the user's configured servers.
            // Do not invent a second resolver (no auto 1.0.0.1 / 8.8.4.4 pair).
            .apply {
                val ips = XrayConfigBuilder.vpnDnsIps(settings)
                ips.forEach { addDnsServer(it) }
                Log.i(TAG, "VPN interface DNS servers (authoritative)=$ips")
            }
            .addRoute("0.0.0.0", 0)

        // IPv4-only VPN: reduce IPv6 DNS/data leaks outside the tunnel.
        // Apps that would use IPv6 to reach Google DNS (2001:4860::) are forced to IPv4.
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                builder.allowFamily(OsConstants.AF_INET)
            }
        } catch (e: Exception) {
            Log.w(TAG, "allowFamily(AF_INET): ${e.message}")
        }

        // Never route our own package into the VPN (prevents Xray/hev loops)
        try {
            builder.addDisallowedApplication(packageName)
        } catch (_: Exception) {
        }

        if (settings.perAppProxy && settings.perAppPackages.isNotEmpty()) {
            for (pkg in settings.perAppPackages) {
                try {
                    if (settings.perAppMode == "proxy") {
                        builder.addAllowedApplication(pkg)
                    } else {
                        builder.addDisallowedApplication(pkg)
                    }
                } catch (_: Exception) {
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
            builder.setBlocking(false)
        }

        return builder.establish()
            ?: throw IllegalStateException("Unable to establish VPN interface")
    }

    private fun startStatsLoop() {
        statsJob?.cancel()
        statsJob = scope.launch {
            var totalUp = 0L
            var totalDown = 0L
            var idleSeconds = 0
            while (isActive && _state.value.isConnected) {
                delay(1000)
                val hev = HevTunnel.stats()
                // hev stats: [tx_packets, tx_bytes, rx_packets, rx_bytes] (typical)
                var upDelta = 0L
                var downDelta = 0L
                if (hev != null && hev.size >= 4) {
                    val tx = hev[1]
                    val rx = hev[3]
                    val prevUp = _state.value.totalUpload
                    val prevDown = _state.value.totalDownload
                    // first sample: set baseline without jump
                    if (prevUp == 0L && prevDown == 0L && (tx > 0 || rx > 0)) {
                        totalUp = tx
                        totalDown = rx
                        upDelta = 0
                        downDelta = 0
                    } else {
                        upDelta = (tx - totalUp).coerceAtLeast(0)
                        downDelta = (rx - totalDown).coerceAtLeast(0)
                        totalUp = tx
                        totalDown = rx
                    }
                } else {
                    upDelta = XrayController.queryStats("proxy", "uplink").coerceAtLeast(0)
                    downDelta = XrayController.queryStats("proxy", "downlink").coerceAtLeast(0)
                    totalUp += upDelta
                    totalDown += downDelta
                }

                val hasFlow = upDelta > 0L || downDelta > 0L
                if (hasFlow) {
                    idleSeconds = 0
                } else {
                    idleSeconds++
                }

                val elapsed = System.currentTimeMillis() - _state.value.startTimeMs
                // Real user traffic clears "no traffic" immediately (do not wait for another probe).
                var noTraffic = _state.value.noTraffic
                if (hasFlow && noTraffic) {
                    noTraffic = false
                    Log.i(TAG, "Traffic resumed — clearing no-traffic flag")
                }

                // Idle alone is NOT enough to show red "no traffic".
                // After sustained idle (and only when fully connected), verify with a test packet.
                val fullyConnected = _state.value.isConnected && !_state.value.isConnecting
                val now = System.currentTimeMillis()
                val probeCooldownOk = now - lastTrafficProbeAtMs >= 45_000L
                if (fullyConnected &&
                    !hasFlow &&
                    idleSeconds >= 45 &&
                    elapsed > 25_000L &&
                    !noTraffic &&
                    !trafficProbeInFlight &&
                    probeCooldownOk
                ) {
                    scheduleTrafficProbe()
                }

                _state.value = _state.value.copy(
                    uploadSpeed = upDelta,
                    downloadSpeed = downDelta,
                    totalUpload = totalUp,
                    totalDownload = totalDown,
                    noTraffic = noTraffic
                )
            }
        }
    }

    /**
     * Verify the tunnel can still move data before showing "connected, no traffic".
     * Sends a real HTTP probe via local Xray HTTP inbound; only sets [ConnectionState.noTraffic]
     * when the probe fails (twice).
     */
    private fun scheduleTrafficProbe() {
        if (trafficProbeInFlight) return
        trafficProbeJob?.cancel()
        trafficProbeJob = scope.launch {
            trafficProbeInFlight = true
            try {
                if (!_state.value.isConnected || _state.value.isConnecting) return@launch
                Log.i(TAG, "Idle detected — running tunnel test packet before no-traffic UI")
                val ok1 = probeTunnel(activeSettings)
                if (!_state.value.isConnected || _state.value.isConnecting) return@launch
                if (ok1) {
                    Log.i(TAG, "Traffic probe OK — tunnel works; not showing no-traffic")
                    _state.value = _state.value.copy(noTraffic = false)
                    return@launch
                }
                // Confirm dead path with a second packet
                delay(1_500)
                if (!_state.value.isConnected || _state.value.isConnecting) return@launch
                val ok2 = probeTunnel(activeSettings)
                if (!_state.value.isConnected || _state.value.isConnecting) return@launch
                if (ok2) {
                    Log.i(TAG, "Traffic probe recovered on 2nd try — not showing no-traffic")
                    _state.value = _state.value.copy(noTraffic = false)
                } else {
                    Log.w(TAG, "Traffic probe FAILED twice — showing no-traffic to user")
                    _state.value = _state.value.copy(noTraffic = true)
                }
            } finally {
                lastTrafficProbeAtMs = System.currentTimeMillis()
                trafficProbeInFlight = false
            }
        }
    }

    /**
     * HTTP GET through Xray local HTTP proxy. Success = tunnel can transfer data.
     */
    private fun probeTunnel(settings: AppSettings): Boolean {
        val httpPort = settings.localHttpPort
        val testUrl = settings.testUrl.ifBlank { "https://www.gstatic.com/generate_204" }
        return try {
            val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", httpPort))
            val conn = (URL(testUrl).openConnection(proxy) as HttpURLConnection).apply {
                connectTimeout = 12_000
                readTimeout = 12_000
                requestMethod = "GET"
                instanceFollowRedirects = false
                useCaches = false
            }
            try {
                val code = conn.responseCode
                // Any response from the origin (or proxy success) means bytes moved.
                val ok = code in 200..399
                Log.i(TAG, "Tunnel probe HTTP $code via 127.0.0.1:$httpPort → ok=$ok")
                ok
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Tunnel probe failed: ${e.message}")
            false
        }
    }

    private fun startIpGeoLookup(settings: AppSettings) {
        geoJob?.cancel()
        val httpPort = settings.localHttpPort
        geoJob = scope.launch {
            // Stay in Connecting until both public IP and country are known.
            var attempt = 0
            while (isActive && _state.value.isConnected && _state.value.isConnecting) {
                // First wait lets SOCKS/HTTP + proxy handshake settle; then space retries.
                delay(if (attempt == 0) 3000L else 4000L)
                if (!_state.value.isConnected || !_state.value.isConnecting) return@launch
                attempt++
                try {
                    val info = com.xraypulse.app.util.IpGeoLookup.lookup(
                        httpProxyPort = httpPort,
                        maxAttempts = 2
                    )
                    val ipOk = info.ip.isNotBlank()
                    val countryOk = info.country.isNotBlank()
                    if (_state.value.isConnected && ipOk && countryOk) {
                        _state.value = _state.value.copy(
                            isConnecting = false,
                            publicIp = info.ip,
                            publicCountry = info.country
                        )
                        Log.i(TAG, "Connected (geo ready): ${info.ip} / ${info.country}")
                        return@launch
                    }
                    // Partial result: keep IP if we got it, but remain Connecting for country
                    if (ipOk && _state.value.isConnected) {
                        _state.value = _state.value.copy(
                            publicIp = info.ip,
                            publicCountry = info.country // may still be blank
                        )
                    }
                    Log.w(
                        TAG,
                        "Geo not ready yet attempt=$attempt ip=${info.ip.ifBlank { "-" }} " +
                            "country=${info.country.ifBlank { "-" }} — stay Connecting"
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "IP geo lookup failed attempt=$attempt: ${e.message}")
                }
            }
        }
    }

    private fun startHealthWatch() {
        healthJob?.cancel()
        // reserved for future auto-retry
    }

    /**
     * Periodically hit testUrl through the local HTTP inbound so idle
     * provider connections / NAT mappings stay alive.
     * Interval comes from settings (minutes, default 1).
     */
    private fun startKeepAlive(settings: AppSettings) {
        keepAliveJob?.cancel()
        if (!settings.keepAliveEnabled) return
        val minutes = settings.keepAliveIntervalMinutes.coerceIn(1, 120)
        val intervalMs = minutes.toLong() * 60L * 1000L
        keepAliveJob = scope.launch {
            while (isActive && _state.value.isConnected) {
                delay(intervalMs)
                if (!_state.value.isConnected) break
                val ok = probeTunnel(settings)
                if (ok && _state.value.noTraffic) {
                    _state.value = _state.value.copy(noTraffic = false)
                }
                Log.i(TAG, "Keep-alive probe ok=$ok (every ${minutes}m)")
            }
        }
    }

    private suspend fun disconnect() {
        statsJob?.cancel()
        healthJob?.cancel()
        keepAliveJob?.cancel()
        geoJob?.cancel()
        trafficProbeJob?.cancel()
        trafficProbeInFlight = false
        cleanupCore()
        _state.value = ConnectionState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun cleanupCore() {
        try {
            HevTunnel.stop()
        } catch (_: Exception) {
        }
        try {
            XrayController.stop()
        } catch (_: Exception) {
        }
        try {
            tunFd?.close()
        } catch (_: Exception) {
        }
        tunFd = null
    }

    override fun onDestroy() {
        cleanupCore()
        scope.cancel()
        instance = null
        super.onDestroy()
    }

    override fun onRevoke() {
        scope.launch { disconnect() }
        super.onRevoke()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.vpn_channel_name),
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = getString(R.string.vpn_channel_desc)
                    setShowBadge(false)
                }
            )
        }
    }

    private fun buildNotification(title: String, connected: Boolean): Notification {
        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopPi = PendingIntent.getService(
            this, 1,
            Intent(this, XrayVpnService::class.java).setAction(ACTION_DISCONNECT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openQuickPi = PendingIntent.getActivity(
            this, 3,
            Intent(this, MainActivity::class.java).apply {
                action = ACTION_QUICK_TOGGLE
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val disconnectLabel = if (isFa()) "قطع اتصال" else "Disconnect"
        val openLabel = if (isFa()) "باز کردن" else "Open"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (connected) getString(R.string.vpn_notification_title) else getString(R.string.app_name))
            .setContentText(if (connected) title else getString(R.string.vpn_notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pi)
            .setOngoing(connected)
            .addAction(0, disconnectLabel, stopPi)
            .addAction(0, openLabel, openQuickPi)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    companion object {
        private const val TAG = "XrayVpnService"
        const val ACTION_CONNECT = "com.xraypulse.app.CONNECT"
        const val ACTION_DISCONNECT = "com.xraypulse.app.DISCONNECT"
        const val ACTION_RECONNECT = "com.xraypulse.app.RECONNECT"
        /** Open MainActivity and toggle VPN (notification / tile quick action). */
        const val ACTION_QUICK_TOGGLE = "com.xraypulse.app.QUICK_TOGGLE"
        const val EXTRA_PROFILE = "profile"
        const val EXTRA_SETTINGS = "settings"
        private const val CHANNEL_ID = "xraypulse_vpn"
        private const val NOTIFICATION_ID = 7101
        /** Must match HevTunnel ipv4 */
        const val VPN_IPV4 = "10.10.14.1"
        /** FakeDNS / mapdns address inside 198.18.0.0/15 pool */
        const val VPN_DNS = "198.18.0.2"

        @Volatile
        private var instance: XrayVpnService? = null

        private val _state = MutableStateFlow(ConnectionState())
        val state: StateFlow<ConnectionState> = _state.asStateFlow()

        fun prepare(context: Context): Intent? = VpnService.prepare(context)

        fun start(context: Context, profile: ServerProfile, settings: AppSettings) {
            val intent = Intent(context, XrayVpnService::class.java).apply {
                action = ACTION_CONNECT
                putExtra(EXTRA_PROFILE, JsonStore.toJson(profile))
                putExtra(EXTRA_SETTINGS, JsonStore.toJson(settings))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun reconnect(context: Context, profile: ServerProfile, settings: AppSettings) {
            val intent = Intent(context, XrayVpnService::class.java).apply {
                action = ACTION_RECONNECT
                putExtra(EXTRA_PROFILE, JsonStore.toJson(profile))
                putExtra(EXTRA_SETTINGS, JsonStore.toJson(settings))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, XrayVpnService::class.java).apply {
                action = ACTION_DISCONNECT
            }
            context.startService(intent)
        }
    }
}
