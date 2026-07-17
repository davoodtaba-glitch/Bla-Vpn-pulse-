package com.xraypulse.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ProtocolType {
    VLESS, VMESS, TROJAN, SHADOWSOCKS, SOCKS, HTTP, WIREGUARD, HYSTERIA2, CUSTOM_JSON
}

enum class TransportNetwork {
    TCP, KCP, WS, HTTP, HTTPUPGRADE, XHTTP, GRPC, QUIC, H2
}

enum class StreamSecurity {
    NONE, TLS, REALITY
}

@Entity(tableName = "servers")
data class ServerProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remark: String = "",
    val protocol: ProtocolType = ProtocolType.VLESS,
    val address: String = "",
    val port: Int = 443,
    val uuid: String = "",
    val alterId: Int = 0,
    val encryption: String = "none", // vless: none | vmess: auto/aes-128-gcm | ss: method
    val password: String = "",
    val flow: String = "", // xtls-rprx-vision
    val network: TransportNetwork = TransportNetwork.TCP,
    val security: StreamSecurity = StreamSecurity.NONE,
    val sni: String = "",
    val fingerprint: String = "chrome", // uTLS: chrome, firefox, safari, ios, android, edge, 360, qq, random, randomized
    val alpn: String = "",
    val allowInsecure: Boolean = false,
    // REALITY
    val publicKey: String = "",
    val shortId: String = "",
    val spiderX: String = "",
    // Transport specifics
    val path: String = "",
    val host: String = "",
    val serviceName: String = "", // gRPC
    val mode: String = "", // gRPC multi / xhttp mode
    val headerType: String = "none",
    val seed: String = "",
    // Mux / advanced
    val muxEnabled: Boolean = false,
    val muxConcurrency: Int = 8,
    // Metadata
    val subscriptionId: Long? = null,
    val rawConfig: String = "",
    val shareLink: String = "",
    val latencyMs: Long = -1,
    val uploadBytes: Long = 0,
    val downloadBytes: Long = 0,
    val isSelected: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun displayTitle(): String =
        remark.ifBlank { "${protocol.name.lowercase()}://$address:$port" }

    fun subtitle(): String {
        val sec = when (security) {
            StreamSecurity.REALITY -> "REALITY"
            StreamSecurity.TLS -> "TLS"
            StreamSecurity.NONE -> "plain"
        }
        val net = network.name.lowercase()
        val flowPart = if (flow.isNotBlank()) " · $flow" else ""
        return "${protocol.name} · $net · $sec$flowPart"
    }

    fun protocolBadge(): String = when (protocol) {
        ProtocolType.VLESS -> "VLESS"
        ProtocolType.VMESS -> "VMess"
        ProtocolType.TROJAN -> "Trojan"
        ProtocolType.SHADOWSOCKS -> "SS"
        ProtocolType.SOCKS -> "SOCKS"
        ProtocolType.HTTP -> "HTTP"
        ProtocolType.WIREGUARD -> "WG"
        ProtocolType.HYSTERIA2 -> "Hy2"
        ProtocolType.CUSTOM_JSON -> "JSON"
    }
}

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val url: String = "",
    val userAgent: String = "XrayPulse/1.0",
    val autoUpdate: Boolean = true,
    val updateIntervalHours: Int = 12,
    val lastUpdated: Long = 0,
    val serverCount: Int = 0,
    val enabled: Boolean = true,
    /** Bytes uploaded (from subscription-userinfo header). */
    val usedUpload: Long = 0,
    /** Bytes downloaded (from subscription-userinfo header). */
    val usedDownload: Long = 0,
    /** Total traffic quota in bytes; 0 = unknown/unlimited. */
    val totalTraffic: Long = 0,
    /** Expiry as Unix epoch seconds; 0 = unknown. */
    val expireAt: Long = 0
) {
    val usedTraffic: Long get() = usedUpload + usedDownload

    fun trafficProgress(): Float {
        if (totalTraffic <= 0) return 0f
        return (usedTraffic.toFloat() / totalTraffic.toFloat()).coerceIn(0f, 1f)
    }

    /** 0..1 of time remaining until expire (1 = full remaining). -1 if unknown. */
    fun timeRemainingProgress(): Float {
        if (expireAt <= 0) return -1f
        val nowSec = System.currentTimeMillis() / 1000L
        val expire = if (expireAt > 1_000_000_000_000L) expireAt / 1000L else expireAt
        val remaining = expire - nowSec
        if (remaining <= 0) return 0f
        // Assume 30-day window if we only have expire (no start)
        val window = 30L * 24 * 3600
        return (remaining.toFloat() / window.toFloat()).coerceIn(0f, 1f)
    }

    fun expireLabel(): String {
        if (expireAt <= 0) return ""
        val expireMs = if (expireAt > 1_000_000_000_000L) expireAt else expireAt * 1000L
        val remaining = expireMs - System.currentTimeMillis()
        if (remaining <= 0) return "Expired"
        val days = remaining / (24 * 3600_000L)
        val hours = (remaining % (24 * 3600_000L)) / 3600_000L
        return if (days > 0) "${days}d ${hours}h left" else "${hours}h left"
    }
}

data class ConnectionState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val selectedServerId: Long? = null,
    val selectedRemark: String = "",
    val startTimeMs: Long = 0,
    val uploadSpeed: Long = 0,
    val downloadSpeed: Long = 0,
    val totalUpload: Long = 0,
    val totalDownload: Long = 0,
    val errorMessage: String? = null,
    /** True when tunnel is up but no data for a while */
    val noTraffic: Boolean = false,
    /**
     * Limit warning for UI banner / toast.
     * level: 0=none, 1≈80%, 2≈95%
     */
    val limitWarning: String? = null,
    val limitWarningLevel: Int = 0
)

enum class RoutingMode {
    /** All traffic via proxy (private LAN IPs still direct for safety). */
    GLOBAL,
    /** Proxy + keep private/local addresses direct. */
    BYPASS_LAN
}

data class AppSettings(
    val routingMode: RoutingMode = RoutingMode.BYPASS_LAN,
    val enableSniffing: Boolean = true,
    val enableMux: Boolean = false,
    val muxConcurrency: Int = 8,
    val localSocksPort: Int = 10808,
    val localHttpPort: Int = 10809,
    val allowInsecure: Boolean = false,
    val domainStrategy: String = "AsIs", // AsIs, IPIfNonMatch, IPOnDemand
    val logLevel: String = "warning",
    val perAppProxy: Boolean = false,
    val perAppMode: String = "bypass", // bypass | proxy
    val perAppPackages: Set<String> = emptySet(),
    val autoConnect: Boolean = false,
    val darkTheme: Boolean = true,
    val testUrl: String = "https://www.gstatic.com/generate_204",
    val dnsRemote: String = "https://1.1.1.1/dns-query",
    val dnsDomestic: String = "localhost",
    val fragmentEnabled: Boolean = false,
    /** tlshello | 1-1 | 1-2 | 1-3 | 1-5 */
    val fragmentPackets: String = "tlshello",
    /** e.g. 100-200 (bytes) */
    val fragmentLength: String = "12-23",
    /** e.g. 1-2 (ms delay between fragments) */
    val fragmentInterval: String = "1-2",
    /** e.g. 100-200 max split size; blank = core default */
    val fragmentMaxSplit: String = "",
    /** Theme accent color as ARGB long (e.g. 0xFF00E5C3) */
    val accentColor: Long = 0xFF00E5C3,
    val sortByDelay: Boolean = false,
    /**
     * Session time limit in minutes. 0 = unlimited.
     * Progress bar uses this when > 0.
     */
    val sessionTimeLimitMinutes: Int = 60,
    /**
     * Session traffic limit in megabytes (upload + download). 0 = unlimited.
     * Progress bar uses this when > 0.
     */
    val sessionTrafficLimitMb: Int = 512,
    /**
     * When a session limit is fully reached:
     * "notify" = friendly notification only (default)
     * "disconnect" = stop VPN after notification
     */
    val limitActionOnReach: String = "notify",
    /**
     * When VPN is connected, send a light keep-alive request through the local
     * proxy so idle tunnels stay warm. Default on.
     */
    val keepAliveEnabled: Boolean = true,
    /**
     * Keep-alive interval in minutes (1–120). Default 1.
     */
    val keepAliveIntervalMinutes: Int = 1,
    /**
     * Domains/hosts that go direct (bypass VPN). One pattern per line.
     * Supports wildcards: `*.example.com`, `*cdn*`, `example.com`.
     */
    val bypassDomains: String = "",
    /**
     * Bind SOCKS/HTTP to all interfaces so other devices on Wi‑Fi can use this phone as a proxy.
     */
    val allowLanProxy: Boolean = false,
    /**
     * Visual style. Only Classic Pulse is exposed in UI (others hidden).
     */
    val themeStyle: String = "PULSE",
    /** UI language: "en" | "fa" | custom code from imported JSON. */
    val language: String = "fa"
)

/** Common presets for session limits UI. */
object SessionLimitPresets {
    val TIME_MINUTES = listOf(0, 15, 30, 60, 120, 180, 360, 720)
    val TRAFFIC_MB = listOf(0, 100, 256, 512, 1024, 2048, 5120, 10240)
}

/** Common Xray fragment "packets" presets (v2rayN-compatible). */
object FragmentPresets {
    val PACKETS = listOf("tlshello", "1-1", "1-2", "1-3", "1-5")
    val LENGTHS = listOf("12-23", "50-100", "100-200", "10-30", "50-200")
    val INTERVALS = listOf("1-2", "1-5", "5-10", "10-20", "10-30")
    val MAX_SPLITS = listOf("", "50-100", "100-200", "100-300")
}
