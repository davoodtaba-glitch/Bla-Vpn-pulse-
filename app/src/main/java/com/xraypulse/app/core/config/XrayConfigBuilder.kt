package com.xraypulse.app.core.config

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.xraypulse.app.data.model.AppSettings
import com.xraypulse.app.data.model.ProtocolType
import com.xraypulse.app.data.model.RoutingMode
import com.xraypulse.app.data.model.ServerProfile
import com.xraypulse.app.data.model.StreamSecurity
import com.xraypulse.app.data.model.TransportNetwork

/**
 * Builds Xray-core JSON config with modern features:
 * VLESS + REALITY + Vision, XHTTP, gRPC, TLS fragment (dialerProxy), sniffing, DNS, routing.
 */
object XrayConfigBuilder {

    private val gson = GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create()

    data class FragmentVerifyResult(val ok: Boolean, val message: String)

    /**
     * Builds a sample config and checks that fragment parameters are actually emitted.
     * This proves the UI values are wired into Xray JSON (not just decorative).
     */
    fun verifyFragmentConfig(settings: AppSettings): FragmentVerifyResult {
        if (!settings.fragmentEnabled) {
            return FragmentVerifyResult(false, "Fragment is OFF — enable it first, then Verify.")
        }
        val sample = ServerProfile(
            remark = "fragment-verify",
            protocol = ProtocolType.VLESS,
            address = "example.com",
            port = 443,
            uuid = "00000000-0000-0000-0000-000000000001",
            encryption = "none",
            network = TransportNetwork.TCP,
            security = StreamSecurity.TLS,
            sni = "example.com",
            fingerprint = "chrome"
        )
        val json = build(sample, settings)
        val checks = mutableListOf<String>()
        var ok = true

        fun need(label: String, present: Boolean) {
            if (present) checks += "✓ $label"
            else {
                ok = false
                checks += "✗ MISSING $label"
            }
        }

        need("\"tag\": \"fragment\" outbound", json.contains("\"tag\": \"fragment\"") || json.contains("\"tag\":\"fragment\""))
        need("dialerProxy → fragment", json.contains("\"dialerProxy\": \"fragment\"") || json.contains("\"dialerProxy\":\"fragment\""))
        need("fragment.packets", json.contains(settings.fragmentPackets.ifBlank { "tlshello" }))
        need("fragment.length", json.contains(settings.fragmentLength.ifBlank { "100-200" }))
        need("fragment.interval", json.contains(settings.fragmentInterval.ifBlank { "10-20" }))
        if (settings.fragmentMaxSplit.isNotBlank()) {
            need("fragment.maxSplit", json.contains(settings.fragmentMaxSplit))
        }

        val header = if (ok) {
            "OK — fragment will be written into the live Xray config on Apply + reconnect."
        } else {
            "FAIL — fragment block incomplete. Do not expect anti-DPI from these values."
        }
        return FragmentVerifyResult(ok, header + "\n" + checks.joinToString("\n"))
    }

    /** Short JSON snippet of the fragment-related parts for the UI preview. */
    fun fragmentConfigSnippet(settings: AppSettings): String {
        if (!settings.fragmentEnabled) return "(fragment disabled)"
        val sample = ServerProfile(
            remark = "preview",
            protocol = ProtocolType.VLESS,
            address = "example.com",
            port = 443,
            uuid = "00000000-0000-0000-0000-000000000001",
            encryption = "none",
            network = TransportNetwork.TCP,
            security = StreamSecurity.TLS,
            sni = "example.com"
        )
        val full = build(sample, settings)
        // Extract sockopt + fragment outbound blocks roughly
        val lines = full.lines()
        val interesting = lines.filter { line ->
            val l = line.lowercase()
            l.contains("fragment") || l.contains("dialerproxy") ||
                l.contains("packets") || l.contains("\"length\"") ||
                l.contains("interval") || l.contains("maxsplit") ||
                l.contains("\"tag\": \"proxy\"") || l.contains("\"tag\": \"fragment\"")
        }
        return if (interesting.isEmpty()) full.take(800) else interesting.joinToString("\n")
    }

    fun build(profile: ServerProfile, settings: AppSettings): String {
        if (profile.protocol == ProtocolType.CUSTOM_JSON && profile.rawConfig.isNotBlank()) {
            return profile.rawConfig
        }

        val root = JsonObject()
        root.add("log", buildLog(settings))
        root.add("dns", buildDns(settings))
        // FakeDNS is required for Android full-tunnel when Vision (no UDP) is used
        root.add("fakedns", JsonObject().apply {
            addProperty("ipPool", "198.18.0.0/15")
            addProperty("poolSize", 65535)
        })
        root.add("inbounds", buildInbounds(settings))
        root.add("outbounds", buildOutbounds(profile, settings))
        root.add("routing", buildRouting(settings))
        root.add("policy", buildPolicy())
        root.add("stats", JsonObject())
        return gson.toJson(root)
    }

    private fun buildLog(settings: AppSettings) = JsonObject().apply {
        addProperty("loglevel", settings.logLevel)
    }

    /**
     * FakeDNS first so apps get 198.18.x.x addresses; Xray recovers real domains via SNI sniffing.
     * Upstream DoH uses +local so Xray resolves DNS outside the tunnel (process excluded from VPN).
     */
    private fun buildDns(settings: AppSettings) = JsonObject().apply {
        add("hosts", JsonObject().apply {
            // Keep DoH endpoints resolvable without recursion
            addProperty("cloudflare-dns.com", "1.1.1.1")
            addProperty("dns.google", "8.8.8.8")
            addProperty("dns.cloudflare.com", "1.1.1.1")
        })
        val servers = JsonArray()
        // FakeDNS: critical for full-tunnel Android + VLESS Vision (UDP often broken)
        servers.add("fakedns")
        // Prefer DoH over the process network (not through TUN)
        val remote = settings.dnsRemote.ifBlank { "https://1.1.1.1/dns-query" }
        val doh = if (remote.startsWith("https://") && !remote.contains("+local")) {
            remote.replace("https://", "https+local://")
        } else if (remote.startsWith("https+local://") || remote.startsWith("https://")) {
            remote
        } else {
            "https+local://1.1.1.1/dns-query"
        }
        servers.add(doh)
        add("servers", servers)
        addProperty("queryStrategy", "UseIPv4")
        addProperty("disableFallbackIfMatch", true)
    }

    private fun buildInbounds(settings: AppSettings): JsonArray {
        // 0.0.0.0 = other devices on Wi‑Fi can use SOCKS/HTTP on this phone
        val listen = if (settings.allowLanProxy) "0.0.0.0" else "127.0.0.1"
        val arr = JsonArray()
        arr.add(JsonObject().apply {
            addProperty("tag", "socks-in")
            addProperty("port", settings.localSocksPort)
            addProperty("listen", listen)
            addProperty("protocol", "socks")
            add("settings", JsonObject().apply {
                addProperty("udp", true)
                addProperty("auth", "noauth")
            })
            add("sniffing", sniffing(settings))
        })
        arr.add(JsonObject().apply {
            addProperty("tag", "http-in")
            addProperty("port", settings.localHttpPort)
            addProperty("listen", listen)
            addProperty("protocol", "http")
            add("settings", JsonObject().apply {
                addProperty("allowTransparent", false)
            })
            add("sniffing", sniffing(settings))
        })
        // Local DNS door — VPN points DNS here so queries never depend on UDP-through-proxy
        arr.add(JsonObject().apply {
            addProperty("tag", "dns-in")
            addProperty("port", 10853)
            addProperty("listen", "127.0.0.1")
            addProperty("protocol", "dokodemo-door")
            add("settings", JsonObject().apply {
                addProperty("address", "1.1.1.1")
                addProperty("port", 53)
                addProperty("network", "tcp,udp")
                addProperty("followRedirect", false)
            })
        })
        return arr
    }

    private fun sniffing(settings: AppSettings) = JsonObject().apply {
        addProperty("enabled", settings.enableSniffing)
        add("destOverride", JsonArray().apply {
            add("http")
            add("tls")
            add("quic")
            add("fakedns")
        })
        // false = override destination with sniffed domain (required for FakeDNS)
        addProperty("routeOnly", false)
        addProperty("metadataOnly", false)
    }

    private fun buildOutbounds(profile: ServerProfile, settings: AppSettings): JsonArray {
        val arr = JsonArray()
        // Order: proxy first (default), then fragment (if enabled), dns, direct, block
        arr.add(buildProxyOutbound(profile, settings))
        if (settings.fragmentEnabled) {
            arr.add(buildFragmentOutbound(settings))
        }
        // DNS outbound — answers DNS queries via Xray DNS module
        arr.add(JsonObject().apply {
            addProperty("tag", "dns-out")
            addProperty("protocol", "dns")
            add("settings", JsonObject())
        })
        arr.add(JsonObject().apply {
            addProperty("tag", "direct")
            addProperty("protocol", "freedom")
            add("settings", JsonObject().apply {
                addProperty("domainStrategy", "UseIPv4")
            })
        })
        arr.add(JsonObject().apply {
            addProperty("tag", "block")
            addProperty("protocol", "blackhole")
            add("settings", JsonObject().apply {
                add("response", JsonObject().apply { addProperty("type", "http") })
            })
        })
        return arr
    }

    /**
     * Freedom outbound with fragment — used as dialerProxy for the main proxy.
     * packets: tlshello | 1-1 | 1-2 | 1-3 | 1-5
     * length / interval / maxSplit: range strings e.g. "100-200"
     */
    private fun buildFragmentOutbound(settings: AppSettings): JsonObject {
        return JsonObject().apply {
            addProperty("tag", "fragment")
            addProperty("protocol", "freedom")
            add("settings", JsonObject().apply {
                addProperty("domainStrategy", "AsIs")
                add("fragment", JsonObject().apply {
                    addProperty("packets", settings.fragmentPackets.ifBlank { "tlshello" })
                    addProperty("length", settings.fragmentLength.ifBlank { "100-200" })
                    addProperty("interval", settings.fragmentInterval.ifBlank { "10-20" })
                    if (settings.fragmentMaxSplit.isNotBlank()) {
                        addProperty("maxSplit", settings.fragmentMaxSplit)
                    }
                })
            })
            add("streamSettings", JsonObject().apply {
                add("sockopt", JsonObject().apply {
                    addProperty("tcpKeepAliveIdle", 100)
                    addProperty("tcpNoDelay", true)
                })
            })
        }
    }

    private fun buildProxyOutbound(profile: ServerProfile, settings: AppSettings): JsonObject {
        val outbound = JsonObject()
        outbound.addProperty("tag", "proxy")
        outbound.addProperty("protocol", protocolName(profile.protocol))
        outbound.add("settings", protocolSettings(profile))
        outbound.add("streamSettings", streamSettings(profile, settings))

        // Mux is incompatible with Vision flow and often with fragment — skip when flow/fragment set
        val canMux = (settings.enableMux || profile.muxEnabled) &&
            profile.flow.isBlank() &&
            !settings.fragmentEnabled
        if (canMux) {
            outbound.add("mux", JsonObject().apply {
                addProperty("enabled", true)
                addProperty(
                    "concurrency",
                    profile.muxConcurrency.takeIf { it > 0 } ?: settings.muxConcurrency
                )
                addProperty("xudpConcurrency", 16)
                addProperty("xudpProxyUDP443", "reject")
            })
        }
        return outbound
    }

    private fun protocolName(p: ProtocolType) = when (p) {
        ProtocolType.VLESS -> "vless"
        ProtocolType.VMESS -> "vmess"
        ProtocolType.TROJAN -> "trojan"
        ProtocolType.SHADOWSOCKS -> "shadowsocks"
        ProtocolType.SOCKS -> "socks"
        ProtocolType.HTTP -> "http"
        ProtocolType.WIREGUARD -> "wireguard"
        else -> "vless"
    }

    private fun protocolSettings(profile: ServerProfile): JsonObject {
        val settings = JsonObject()
        when (profile.protocol) {
            ProtocolType.VLESS -> {
                val vnext = JsonArray()
                vnext.add(JsonObject().apply {
                    addProperty("address", profile.address)
                    addProperty("port", profile.port)
                    add("users", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("id", profile.uuid)
                            addProperty("encryption", profile.encryption.ifBlank { "none" })
                            if (profile.flow.isNotBlank()) addProperty("flow", profile.flow)
                            addProperty("level", 0)
                        })
                    })
                })
                settings.add("vnext", vnext)
            }
            ProtocolType.VMESS -> {
                val vnext = JsonArray()
                vnext.add(JsonObject().apply {
                    addProperty("address", profile.address)
                    addProperty("port", profile.port)
                    add("users", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("id", profile.uuid)
                            addProperty("alterId", profile.alterId)
                            addProperty("security", profile.encryption.ifBlank { "auto" })
                            addProperty("level", 0)
                        })
                    })
                })
                settings.add("vnext", vnext)
            }
            ProtocolType.TROJAN -> {
                val servers = JsonArray()
                servers.add(JsonObject().apply {
                    addProperty("address", profile.address)
                    addProperty("port", profile.port)
                    addProperty("password", profile.password.ifBlank { profile.uuid })
                    if (profile.flow.isNotBlank()) addProperty("flow", profile.flow)
                    addProperty("level", 0)
                })
                settings.add("servers", servers)
            }
            ProtocolType.SHADOWSOCKS -> {
                val servers = JsonArray()
                servers.add(JsonObject().apply {
                    addProperty("address", profile.address)
                    addProperty("port", profile.port)
                    addProperty("method", profile.encryption.ifBlank { "aes-256-gcm" })
                    addProperty("password", profile.password)
                    addProperty("level", 0)
                })
                settings.add("servers", servers)
            }
            ProtocolType.SOCKS, ProtocolType.HTTP -> {
                val servers = JsonArray()
                servers.add(JsonObject().apply {
                    addProperty("address", profile.address)
                    addProperty("port", profile.port)
                    if (profile.uuid.isNotBlank() || profile.password.isNotBlank()) {
                        add("users", JsonArray().apply {
                            add(JsonObject().apply {
                                addProperty("user", profile.uuid)
                                addProperty("pass", profile.password)
                                addProperty("level", 0)
                            })
                        })
                    }
                })
                settings.add("servers", servers)
            }
            else -> {
                val vnext = JsonArray()
                vnext.add(JsonObject().apply {
                    addProperty("address", profile.address)
                    addProperty("port", profile.port)
                    add("users", JsonArray().apply {
                        add(JsonObject().apply {
                            addProperty("id", profile.uuid)
                            addProperty("encryption", "none")
                        })
                    })
                })
                settings.add("vnext", vnext)
            }
        }
        return settings
    }

    private fun streamSettings(profile: ServerProfile, appSettings: AppSettings): JsonObject {
        val stream = JsonObject()
        stream.addProperty("network", networkName(profile.network))

        when (profile.security) {
            StreamSecurity.TLS -> {
                stream.addProperty("security", "tls")
                stream.add("tlsSettings", JsonObject().apply {
                    addProperty("serverName", profile.sni.ifBlank { profile.address })
                    addProperty("allowInsecure", profile.allowInsecure || appSettings.allowInsecure)
                    if (profile.fingerprint.isNotBlank()) {
                        addProperty("fingerprint", profile.fingerprint)
                    }
                    if (profile.alpn.isNotBlank()) {
                        add("alpn", JsonArray().apply {
                            profile.alpn.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                                .forEach { add(it) }
                        })
                    }
                })
            }
            StreamSecurity.REALITY -> {
                stream.addProperty("security", "reality")
                stream.add("realitySettings", JsonObject().apply {
                    addProperty("serverName", profile.sni.ifBlank { profile.address })
                    addProperty("fingerprint", profile.fingerprint.ifBlank { "chrome" })
                    addProperty("publicKey", profile.publicKey)
                    addProperty("shortId", profile.shortId)
                    addProperty("spiderX", profile.spiderX.ifBlank { "/" })
                })
            }
            StreamSecurity.NONE -> stream.addProperty("security", "none")
        }

        when (profile.network) {
            TransportNetwork.TCP -> {
                stream.add("tcpSettings", JsonObject().apply {
                    if (profile.headerType == "http") {
                        add("header", JsonObject().apply {
                            addProperty("type", "http")
                            add("request", JsonObject().apply {
                                add("path", JsonArray().apply {
                                    add(profile.path.ifBlank { "/" })
                                })
                                add("headers", JsonObject().apply {
                                    if (profile.host.isNotBlank()) {
                                        add("Host", JsonArray().apply { add(profile.host) })
                                    }
                                })
                            })
                        })
                    } else {
                        add("header", JsonObject().apply { addProperty("type", "none") })
                    }
                })
            }
            TransportNetwork.WS -> {
                stream.add("wsSettings", JsonObject().apply {
                    addProperty("path", profile.path.ifBlank { "/" })
                    if (profile.host.isNotBlank()) {
                        add("headers", JsonObject().apply {
                            addProperty("Host", profile.host)
                        })
                    }
                })
            }
            TransportNetwork.HTTP, TransportNetwork.H2 -> {
                stream.add("httpSettings", JsonObject().apply {
                    add("host", JsonArray().apply {
                        if (profile.host.isNotBlank()) add(profile.host)
                        else add(profile.address)
                    })
                    addProperty("path", profile.path.ifBlank { "/" })
                })
            }
            TransportNetwork.HTTPUPGRADE -> {
                stream.add("httpupgradeSettings", JsonObject().apply {
                    addProperty("path", profile.path.ifBlank { "/" })
                    if (profile.host.isNotBlank()) addProperty("host", profile.host)
                })
            }
            TransportNetwork.XHTTP -> {
                stream.add("xhttpSettings", JsonObject().apply {
                    addProperty("path", profile.path.ifBlank { "/" })
                    if (profile.host.isNotBlank()) addProperty("host", profile.host)
                    if (profile.mode.isNotBlank()) addProperty("mode", profile.mode)
                })
            }
            TransportNetwork.GRPC -> {
                stream.add("grpcSettings", JsonObject().apply {
                    addProperty("serviceName", profile.serviceName.ifBlank { profile.path })
                    addProperty("multiMode", profile.mode == "multi")
                    addProperty("idle_timeout", 60)
                    addProperty("health_check_timeout", 20)
                    addProperty("permit_without_stream", false)
                    addProperty("initial_windows_size", 0)
                })
            }
            TransportNetwork.KCP -> {
                stream.add("kcpSettings", JsonObject().apply {
                    addProperty("mtu", 1350)
                    addProperty("tti", 50)
                    addProperty("uplinkCapacity", 12)
                    addProperty("downlinkCapacity", 100)
                    addProperty("congestion", false)
                    addProperty("readBufferSize", 2)
                    addProperty("writeBufferSize", 2)
                    add("header", JsonObject().apply {
                        addProperty("type", profile.headerType.ifBlank { "none" })
                    })
                    if (profile.seed.isNotBlank()) addProperty("seed", profile.seed)
                })
            }
            TransportNetwork.QUIC -> {
                stream.add("quicSettings", JsonObject().apply {
                    addProperty("security", profile.encryption.ifBlank { "none" })
                    addProperty("key", profile.password)
                    add("header", JsonObject().apply {
                        addProperty("type", profile.headerType.ifBlank { "none" })
                    })
                })
            }
        }

        stream.add("sockopt", JsonObject().apply {
            addProperty("tcpFastOpen", false)
            addProperty("domainStrategy", "UseIP")
            addProperty("tcpKeepAliveIdle", 100)
            addProperty("tcpNoDelay", true)
            // Chain through freedom+fragment outbound when enabled
            if (appSettings.fragmentEnabled) {
                addProperty("dialerProxy", "fragment")
            }
        })

        return stream
    }

    private fun networkName(n: TransportNetwork) = when (n) {
        TransportNetwork.TCP -> "tcp"
        TransportNetwork.KCP -> "kcp"
        TransportNetwork.WS -> "ws"
        TransportNetwork.HTTP, TransportNetwork.H2 -> "h2"
        TransportNetwork.HTTPUPGRADE -> "httpupgrade"
        TransportNetwork.XHTTP -> "xhttp"
        TransportNetwork.GRPC -> "grpc"
        TransportNetwork.QUIC -> "quic"
    }

    private fun buildRouting(settings: AppSettings): JsonObject {
        val routing = JsonObject()
        // IPIfNonMatch helps when FakeDNS + sniffing rewrites destinations
        routing.addProperty(
            "domainStrategy",
            if (settings.domainStrategy == "AsIs") "AsIs" else settings.domainStrategy
        )
        routing.addProperty("domainMatcher", "hybrid")
        val rules = JsonArray()

        // 1) DNS traffic → dns-out (handles FakeDNS + DoH)
        rules.add(JsonObject().apply {
            addProperty("type", "field")
            add("inboundTag", JsonArray().apply { add("dns-in") })
            addProperty("outboundTag", "dns-out")
        })
        rules.add(JsonObject().apply {
            addProperty("type", "field")
            addProperty("port", "53")
            addProperty("outboundTag", "dns-out")
        })

        // 2) Block QUIC (UDP/443) so browsers fall back to stable TCP HTTPS
        //    Broken QUIC through proxy is a common ERR_CONNECTION_CLOSED cause
        rules.add(JsonObject().apply {
            addProperty("type", "field")
            addProperty("network", "udp")
            addProperty("port", "443")
            addProperty("outboundTag", "block")
        })

        // 3) Private LAN (do not send LAN through proxy) — exclude FakeDNS pool
        rules.add(rule(ip = listOf(
            "0.0.0.0/8",
            "10.0.0.0/8",
            "127.0.0.0/8",
            "169.254.0.0/16",
            "172.16.0.0/12",
            "192.168.0.0/16",
            "224.0.0.0/4",
            "255.255.255.255/32"
        ), outbound = "direct"))
        rules.add(rule(domain = listOf("geosite:private"), outbound = "direct"))
        rules.add(rule(domain = listOf("geosite:category-ads-all"), outbound = "block"))

        // User bypass domains → direct (support wildcards like *.example.com*)
        val bypass = parseBypassDomainPatterns(settings.bypassDomains)
        if (bypass.isNotEmpty()) {
            rules.add(rule(domain = bypass, outbound = "direct"))
        }

        // GLOBAL and BYPASS_LAN share private-LAN direct rules above.
        // BYPASS_LAN is the default friendly mode; GLOBAL does not add extra CN rules.
        routing.add("rules", rules)
        return routing
    }

    /**
     * Convert user patterns to Xray domain matchers.
     * - plain host → domain:host (suffix / subdomain match)
     * - patterns with * → regexp after escaping
     */
    fun parseBypassDomainPatterns(raw: String): List<String> {
        if (raw.isBlank()) return emptyList()
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { pattern ->
                val p = pattern.removePrefix("http://").removePrefix("https://").trim('/')
                when {
                    p.contains('*') -> {
                        val escaped = buildString {
                            for (ch in p) {
                                when (ch) {
                                    '*' -> append(".*")
                                    '.', '+', '?', '^', '$', '{', '}', '(', ')', '|', '[', ']', '\\' -> {
                                        append('\\'); append(ch)
                                    }
                                    else -> append(ch)
                                }
                            }
                        }
                        "regexp:$escaped"
                    }
                    p.startsWith("regexp:") || p.startsWith("domain:") ||
                        p.startsWith("full:") || p.startsWith("keyword:") ||
                        p.startsWith("geosite:") -> p
                    else -> "domain:$p"
                }
            }
            .distinct()
            .toList()
    }

    private fun rule(
        domain: List<String>? = null,
        ip: List<String>? = null,
        port: String? = null,
        network: String? = null,
        outbound: String
    ) = JsonObject().apply {
        addProperty("type", "field")
        domain?.let { d -> add("domain", JsonArray().apply { d.forEach { add(it) } }) }
        ip?.let { i -> add("ip", JsonArray().apply { i.forEach { add(it) } }) }
        port?.let { addProperty("port", it) }
        network?.let { addProperty("network", it) }
        addProperty("outboundTag", outbound)
    }

    private fun buildPolicy() = JsonObject().apply {
        add("levels", JsonObject().apply {
            add("0", JsonObject().apply {
                addProperty("handshake", 4)
                addProperty("connIdle", 300)
                addProperty("uplinkOnly", 2)
                addProperty("downlinkOnly", 5)
                addProperty("statsUserUplink", true)
                addProperty("statsUserDownlink", true)
                addProperty("bufferSize", 4)
            })
        })
        add("system", JsonObject().apply {
            addProperty("statsInboundUplink", true)
            addProperty("statsInboundDownlink", true)
            addProperty("statsOutboundUplink", true)
            addProperty("statsOutboundDownlink", true)
        })
    }
}
