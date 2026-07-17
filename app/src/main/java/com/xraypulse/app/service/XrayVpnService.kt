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
    private var sessionTimeLimitMs: Long = 0L
    private var sessionTrafficLimitBytes: Long = 0L
    /** "notify" or "disconnect" when a limit is fully reached. */
    private var limitActionOnReach: String = "notify"
    /** Highest warning level already shown this session (0/1/2) for time & traffic separately */
    private var timeWarnLevel = 0
    private var trafficWarnLevel = 0
    /** True after friendly "limit reached" notification was sent. */
    private var limitReachedNotified = false

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

            sessionTimeLimitMs = if (settings.sessionTimeLimitMinutes > 0)
                settings.sessionTimeLimitMinutes.toLong() * 60_000L else 0L
            sessionTrafficLimitBytes = if (settings.sessionTrafficLimitMb > 0)
                settings.sessionTrafficLimitMb.toLong() * 1024L * 1024L else 0L
            limitActionOnReach = settings.limitActionOnReach.ifBlank { "notify" }
            timeWarnLevel = 0
            trafficWarnLevel = 0
            limitReachedNotified = false

            val config = XrayConfigBuilder.build(profile, settings)
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
            val ok = HevTunnel.start(
                context = applicationContext,
                tun = fd,
                socksPort = settings.localSocksPort,
                mtu = 1500,
                ipv4 = VPN_IPV4,
                dnsListenPort = 10853
            )
            if (!ok) {
                throw IllegalStateException("Failed to start tun2socks (hev-socks5-tunnel)")
            }

            startForeground(NOTIFICATION_ID, buildNotification(profile.displayTitle(), true))
            _state.value = ConnectionState(
                isConnected = true,
                isConnecting = false,
                selectedServerId = profile.id,
                selectedRemark = profile.displayTitle(),
                startTimeMs = System.currentTimeMillis(),
                noTraffic = false
            )
            startStatsLoop()
            startHealthWatch()
            startKeepAlive(settings)
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
        val builder = Builder()
            .setSession("XrayPulse · ${profile.displayTitle()}")
            .setMtu(1500)
            // Same address as hev tunnel.ipv4 — required for correct tun2socks routing
            .addAddress(VPN_IPV4, 30)
            // Point system DNS at FakeDNS/mapdns pool so resolution works without UDP-through-proxy
            .addDnsServer(VPN_DNS)
            .addRoute("0.0.0.0", 0)

        // Intentionally NO IPv6: broken IPv6 paths cause ERR_CONNECTION_CLOSED on many sites
        // (Google/Facebook prefer AAAA records when available).

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
                    // Use absolute counters if increasing, else fall back to xray stats
                    val tx = hev[1]
                    val rx = hev[3]
                    // treat as cumulative from hev — compute delta via state
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

                if (upDelta == 0L && downDelta == 0L) idleSeconds++ else idleSeconds = 0
                val elapsed = System.currentTimeMillis() - _state.value.startTimeMs
                val noTraffic = idleSeconds >= 45 && elapsed > 20_000
                val totalTraffic = totalUp + totalDown

                // Approaching-limit warnings (80% / 95%)
                val warns = mutableListOf<String>()
                var maxLevel = 0
                if (sessionTimeLimitMs > 0) {
                    val ratio = elapsed.toDouble() / sessionTimeLimitMs.toDouble()
                    val lvl = when {
                        ratio >= 0.95 -> 2
                        ratio >= 0.80 -> 1
                        else -> 0
                    }
                    if (lvl > timeWarnLevel) {
                        timeWarnLevel = lvl
                        warns += if (lvl >= 2)
                            "Time limit almost reached (${(ratio * 100).toInt()}%)"
                        else
                            "Time usage high (${(ratio * 100).toInt()}% of limit)"
                    }
                    maxLevel = maxOf(maxLevel, lvl)
                }
                if (sessionTrafficLimitBytes > 0) {
                    val ratio = totalTraffic.toDouble() / sessionTrafficLimitBytes.toDouble()
                    val lvl = when {
                        ratio >= 0.95 -> 2
                        ratio >= 0.80 -> 1
                        else -> 0
                    }
                    if (lvl > trafficWarnLevel) {
                        trafficWarnLevel = lvl
                        warns += if (lvl >= 2)
                            "Traffic limit almost reached (${(ratio * 100).toInt()}%)"
                        else
                            "Traffic usage high (${(ratio * 100).toInt()}% of limit)"
                    }
                    maxLevel = maxOf(maxLevel, lvl)
                }
                val warnText = warns.joinToString(" · ").ifBlank {
                    // Keep last banner while still in danger zone
                    when {
                        maxLevel >= 2 -> "Approaching session limits"
                        maxLevel >= 1 -> "Session limits: high usage"
                        else -> null
                    }
                }

                _state.value = _state.value.copy(
                    uploadSpeed = upDelta,
                    downloadSpeed = downDelta,
                    totalUpload = totalUp,
                    totalDownload = totalDown,
                    noTraffic = noTraffic,
                    limitWarning = warnText,
                    limitWarningLevel = maxLevel
                )

                // Fire toast-style message via errorMessage only on new threshold (UI also shows banner)
                if (warns.isNotEmpty()) {
                    _state.value = _state.value.copy(
                        limitWarning = warns.joinToString(" · "),
                        limitWarningLevel = maxLevel
                    )
                    Log.i(TAG, "Limit warning: ${warns.joinToString()}")
                }

                // User session limits fully reached
                val timeHit = sessionTimeLimitMs > 0 && elapsed >= sessionTimeLimitMs
                val trafficHit = sessionTrafficLimitBytes > 0 && totalTraffic >= sessionTrafficLimitBytes
                if ((timeHit || trafficHit) && !limitReachedNotified) {
                    limitReachedNotified = true
                    val disconnect = limitActionOnReach.equals("disconnect", ignoreCase = true)
                    val title = if (disconnect) "Session limit reached" else "You're all set for now ✨"
                    val body = when {
                        disconnect && timeHit && trafficHit ->
                            "Time and data limits reached. VPN is disconnecting."
                        disconnect && timeHit ->
                            "Session time limit reached. VPN is disconnecting."
                        disconnect && trafficHit ->
                            "Session data limit reached. VPN is disconnecting."
                        timeHit && trafficHit ->
                            "You've used your planned time and data for this session. Your connection is still on — take a short break if you like, or keep browsing."
                        timeHit ->
                            "Your session time goal is complete. BLA VPN is still connected so nothing drops suddenly — just a friendly heads-up!"
                        else ->
                            "You've reached your data goal for this session. No worries — the VPN stays connected so you can finish what you're doing."
                    }
                    Log.i(TAG, "Limit reached action=$limitActionOnReach time=$timeHit traffic=$trafficHit")
                    _state.value = _state.value.copy(
                        limitWarning = body,
                        limitWarningLevel = 2
                    )
                    showLimitNotification(title, body)
                    if (disconnect) {
                        delay(400)
                        disconnect()
                        return@launch
                    }
                }
            }
        }
    }

    private fun showLimitNotification(title: String, body: String) {
        try {
            val nm = getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        LIMIT_CHANNEL_ID,
                        "Session reminders",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Friendly reminders when session time or traffic goals are met"
                    }
                )
            }
            val pi = PendingIntent.getActivity(
                this, 2,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val n = NotificationCompat.Builder(this, LIMIT_CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .build()
            nm.notify(LIMIT_NOTIFICATION_ID, n)
        } catch (e: Exception) {
            Log.w(TAG, "Limit notification failed: ${e.message}")
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
        val httpPort = settings.localHttpPort
        val testUrl = settings.testUrl.ifBlank { "https://www.gstatic.com/generate_204" }
        keepAliveJob = scope.launch {
            while (isActive && _state.value.isConnected) {
                delay(intervalMs)
                if (!_state.value.isConnected) break
                try {
                    val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", httpPort))
                    val conn = (URL(testUrl).openConnection(proxy) as HttpURLConnection).apply {
                        connectTimeout = 15_000
                        readTimeout = 15_000
                        requestMethod = "GET"
                        instanceFollowRedirects = false
                        useCaches = false
                    }
                    try {
                        val code = conn.responseCode
                        Log.i(TAG, "Keep-alive ping HTTP $code via 127.0.0.1:$httpPort (every ${minutes}m)")
                    } finally {
                        conn.disconnect()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Keep-alive ping failed: ${e.message}")
                }
            }
        }
    }

    private suspend fun disconnect() {
        statsJob?.cancel()
        healthJob?.cancel()
        keepAliveJob?.cancel()
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
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(if (connected) getString(R.string.vpn_notification_title) else getString(R.string.app_name))
            .setContentText(if (connected) title else getString(R.string.vpn_notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pi)
            .setOngoing(connected)
            .addAction(0, "Disconnect", stopPi)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "XrayVpnService"
        const val ACTION_CONNECT = "com.xraypulse.app.CONNECT"
        const val ACTION_DISCONNECT = "com.xraypulse.app.DISCONNECT"
        const val ACTION_RECONNECT = "com.xraypulse.app.RECONNECT"
        const val EXTRA_PROFILE = "profile"
        const val EXTRA_SETTINGS = "settings"
        private const val CHANNEL_ID = "xraypulse_vpn"
        private const val NOTIFICATION_ID = 7101
        private const val LIMIT_CHANNEL_ID = "bla_vpn_session_limits"
        private const val LIMIT_NOTIFICATION_ID = 7103
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
