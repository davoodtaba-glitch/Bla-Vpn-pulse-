package com.xraypulse.app.core.parser

import android.util.Base64
import com.google.gson.JsonParser
import com.xraypulse.app.data.model.ProtocolType
import com.xraypulse.app.data.model.ServerProfile
import com.xraypulse.app.data.model.StreamSecurity
import com.xraypulse.app.data.model.TransportNetwork
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Parses share links compatible with v2rayN / v2rayNG / Xray clients:
 * vless://, vmess://, trojan://, ss://, and multi-line subscriptions (base64 or plain).
 */
object ShareLinkParser {

    fun parseMulti(content: String): List<ServerProfile> {
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return emptyList()

        // Try base64 subscription body
        val decoded = tryBase64Decode(trimmed)
        val body = decoded ?: trimmed

        val lines = body
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }

        val result = mutableListOf<ServerProfile>()
        for (line in lines) {
            try {
                parseSingle(line)?.let { result += it }
            } catch (_: Exception) {
                // skip bad lines
            }
        }
        // Single full JSON config
        if (result.isEmpty() && (trimmed.startsWith("{") || body.startsWith("{"))) {
            parseCustomJson(if (trimmed.startsWith("{")) trimmed else body)?.let { result += it }
        }
        return result
    }

    fun parseSingle(link: String): ServerProfile? {
        val raw = link.trim()
        if (raw.isEmpty()) return null
        return when {
            raw.startsWith("vless://", ignoreCase = true) -> parseVless(raw)
            raw.startsWith("vmess://", ignoreCase = true) -> parseVmess(raw)
            raw.startsWith("trojan://", ignoreCase = true) -> parseTrojan(raw)
            raw.startsWith("ss://", ignoreCase = true) -> parseShadowsocks(raw)
            raw.startsWith("socks://", ignoreCase = true) ||
                raw.startsWith("socks5://", ignoreCase = true) -> parseSocks(raw)
            raw.startsWith("{") -> parseCustomJson(raw)
            else -> null
        }
    }

    // region VLESS
    /**
     * vless://uuid@host:port?encryption=none&security=reality&sni=...&fp=chrome&pbk=...&sid=...&spx=...&type=tcp&flow=xtls-rprx-vision#remark
     */
    private fun parseVless(link: String): ServerProfile {
        val withoutScheme = link.removePrefix("vless://").removePrefix("VLESS://")
        val (main, fragment) = splitFragment(withoutScheme)
        val (userHost, query) = splitQuery(main)

        val at = userHost.lastIndexOf('@')
        require(at > 0) { "Invalid VLESS link" }
        val uuid = userHost.substring(0, at)
        val hostPort = userHost.substring(at + 1)
        val (address, port) = parseHostPort(hostPort, 443)
        val params = parseQueryParams(query)

        val security = when (params["security"]?.lowercase()) {
            "reality" -> StreamSecurity.REALITY
            "tls" -> StreamSecurity.TLS
            else -> StreamSecurity.NONE
        }

        return ServerProfile(
            remark = fragment.ifBlank { "$address:$port" },
            protocol = ProtocolType.VLESS,
            address = address,
            port = port,
            uuid = uuid,
            encryption = params["encryption"] ?: "none",
            flow = params["flow"] ?: "",
            network = parseNetwork(params["type"] ?: params["network"] ?: "tcp"),
            security = security,
            sni = params["sni"] ?: params["peer"] ?: "",
            fingerprint = params["fp"] ?: params["fingerprint"] ?: "chrome",
            alpn = params["alpn"] ?: "",
            allowInsecure = params["allowInsecure"] == "1" || params["insecure"] == "1",
            publicKey = params["pbk"] ?: params["publicKey"] ?: "",
            shortId = params["sid"] ?: params["shortId"] ?: "",
            spiderX = decodeUri(params["spx"] ?: params["spiderX"] ?: ""),
            path = decodeUri(params["path"] ?: ""),
            host = params["host"] ?: params["authority"] ?: "",
            serviceName = params["serviceName"] ?: params["servicename"] ?: "",
            mode = params["mode"] ?: "",
            headerType = params["headerType"] ?: params["header"] ?: "none",
            seed = params["seed"] ?: "",
            shareLink = link
        )
    }
    // endregion

    // region VMess
    /**
     * vmess://base64({v,ps,add,port,id,aid,scy,net,type,host,path,tls,sni,alpn,fp})
     * also supports URI query style in some clients
     */
    private fun parseVmess(link: String): ServerProfile {
        val payload = link.removePrefix("vmess://").removePrefix("VMESS://")
        val decoded = tryBase64Decode(payload)
        if (decoded != null && decoded.trimStart().startsWith("{")) {
            val obj = JsonParser.parseString(decoded).asJsonObject
            fun str(key: String, default: String = "") =
                obj.get(key)?.takeIf { !it.isJsonNull }?.asString ?: default

            val tls = str("tls").lowercase()
            val security = when {
                tls == "reality" || str("security").equals("reality", true) -> StreamSecurity.REALITY
                tls == "tls" || tls == "1" || str("security").equals("tls", true) -> StreamSecurity.TLS
                else -> StreamSecurity.NONE
            }
            return ServerProfile(
                remark = str("ps", str("remark", "${str("add")}:${str("port", "443")}")),
                protocol = ProtocolType.VMESS,
                address = str("add", str("address")),
                port = str("port", "443").toIntOrNull() ?: 443,
                uuid = str("id", str("uuid")),
                alterId = str("aid", str("alterId", "0")).toIntOrNull() ?: 0,
                encryption = str("scy", str("security", "auto")).let {
                    if (it in listOf("tls", "reality", "none", "")) "auto" else it
                },
                network = parseNetwork(str("net", str("network", "tcp"))),
                security = security,
                sni = str("sni", str("peer")),
                fingerprint = str("fp", str("fingerprint", "chrome")),
                alpn = str("alpn"),
                allowInsecure = str("allowInsecure") == "1" || str("insecure") == "1",
                publicKey = str("pbk", str("publicKey")),
                shortId = str("sid", str("shortId")),
                spiderX = str("spx"),
                path = str("path"),
                host = str("host"),
                serviceName = str("serviceName"),
                mode = str("mode"),
                headerType = str("type", str("headerType", "none")),
                shareLink = link
            )
        }
        // Fallback URI style
        return parseVless("vless://$payload").copy(protocol = ProtocolType.VMESS, shareLink = link)
    }
    // endregion

    // region Trojan
    private fun parseTrojan(link: String): ServerProfile {
        val withoutScheme = link.removePrefix("trojan://").removePrefix("TROJAN://")
        val (main, fragment) = splitFragment(withoutScheme)
        val (userHost, query) = splitQuery(main)
        val at = userHost.lastIndexOf('@')
        require(at > 0) { "Invalid Trojan link" }
        val password = decodeUri(userHost.substring(0, at))
        val (address, port) = parseHostPort(userHost.substring(at + 1), 443)
        val params = parseQueryParams(query)
        val security = when (params["security"]?.lowercase()) {
            "reality" -> StreamSecurity.REALITY
            "none", "0" -> StreamSecurity.NONE
            else -> StreamSecurity.TLS
        }
        return ServerProfile(
            remark = fragment.ifBlank { "$address:$port" },
            protocol = ProtocolType.TROJAN,
            address = address,
            port = port,
            password = password,
            uuid = password,
            network = parseNetwork(params["type"] ?: "tcp"),
            security = security,
            sni = params["sni"] ?: params["peer"] ?: address,
            fingerprint = params["fp"] ?: "chrome",
            alpn = params["alpn"] ?: "",
            allowInsecure = params["allowInsecure"] == "1",
            publicKey = params["pbk"] ?: "",
            shortId = params["sid"] ?: "",
            spiderX = decodeUri(params["spx"] ?: ""),
            path = decodeUri(params["path"] ?: ""),
            host = params["host"] ?: "",
            serviceName = params["serviceName"] ?: "",
            mode = params["mode"] ?: "",
            headerType = params["headerType"] ?: "none",
            flow = params["flow"] ?: "",
            shareLink = link
        )
    }
    // endregion

    // region Shadowsocks
    private fun parseShadowsocks(link: String): ServerProfile {
        val withoutScheme = link.removePrefix("ss://").removePrefix("SS://")
        val (main, fragment) = splitFragment(withoutScheme)

        // SIP002: ss://base64(method:password)@host:port#name
        // Legacy: ss://base64(method:password@host:port)
        if (main.contains("@")) {
            val at = main.lastIndexOf('@')
            val userInfo = tryBase64Decode(main.substring(0, at)) ?: decodeUri(main.substring(0, at))
            val hostPort = main.substring(at + 1).substringBefore('?')
            val (method, password) = splitMethodPassword(userInfo)
            val (address, port) = parseHostPort(hostPort, 8388)
            return ServerProfile(
                remark = fragment.ifBlank { "$address:$port" },
                protocol = ProtocolType.SHADOWSOCKS,
                address = address,
                port = port,
                encryption = method,
                password = password,
                network = TransportNetwork.TCP,
                security = StreamSecurity.NONE,
                shareLink = link
            )
        } else {
            val decoded = tryBase64Decode(main.substringBefore('#'))
                ?: throw IllegalArgumentException("Invalid SS link")
            // method:password@host:port
            val at = decoded.lastIndexOf('@')
            require(at > 0)
            val (method, password) = splitMethodPassword(decoded.substring(0, at))
            val (address, port) = parseHostPort(decoded.substring(at + 1), 8388)
            return ServerProfile(
                remark = fragment.ifBlank { "$address:$port" },
                protocol = ProtocolType.SHADOWSOCKS,
                address = address,
                port = port,
                encryption = method,
                password = password,
                network = TransportNetwork.TCP,
                security = StreamSecurity.NONE,
                shareLink = link
            )
        }
    }

    private fun splitMethodPassword(userInfo: String): Pair<String, String> {
        val idx = userInfo.indexOf(':')
        require(idx > 0) { "Invalid SS credentials" }
        return userInfo.substring(0, idx) to userInfo.substring(idx + 1)
    }
    // endregion

    private fun parseSocks(link: String): ServerProfile {
        val withoutScheme = link
            .removePrefix("socks5://")
            .removePrefix("socks://")
            .removePrefix("SOCKS5://")
            .removePrefix("SOCKS://")
        val (main, fragment) = splitFragment(withoutScheme)
        val (userHost, _) = splitQuery(main)
        val at = userHost.lastIndexOf('@')
        val (user, pass, hostPort) = if (at > 0) {
            val cred = userHost.substring(0, at)
            val colon = cred.indexOf(':')
            Triple(
                if (colon >= 0) cred.substring(0, colon) else cred,
                if (colon >= 0) cred.substring(colon + 1) else "",
                userHost.substring(at + 1)
            )
        } else Triple("", "", userHost)
        val (address, port) = parseHostPort(hostPort, 1080)
        return ServerProfile(
            remark = fragment.ifBlank { "socks://$address:$port" },
            protocol = ProtocolType.SOCKS,
            address = address,
            port = port,
            uuid = user,
            password = pass,
            network = TransportNetwork.TCP,
            security = StreamSecurity.NONE,
            shareLink = link
        )
    }

    private fun parseCustomJson(json: String): ServerProfile? {
        return try {
            val obj = JsonParser.parseString(json).asJsonObject
            // Full xray config
            if (obj.has("outbounds") || obj.has("inbounds")) {
                val remark = obj.get("remarks")?.asString
                    ?: obj.get("ps")?.asString
                    ?: "Custom JSON"
                ServerProfile(
                    remark = remark,
                    protocol = ProtocolType.CUSTOM_JSON,
                    rawConfig = json,
                    shareLink = ""
                )
            } else null
        } catch (_: Exception) {
            null
        }
    }

    // region helpers
    private fun parseNetwork(type: String): TransportNetwork = when (type.lowercase()) {
        "ws", "websocket" -> TransportNetwork.WS
        "h2", "http" -> TransportNetwork.HTTP
        "httpupgrade", "http_upgrade" -> TransportNetwork.HTTPUPGRADE
        "xhttp", "splithttp" -> TransportNetwork.XHTTP
        "grpc" -> TransportNetwork.GRPC
        "kcp", "mkcp" -> TransportNetwork.KCP
        "quic" -> TransportNetwork.QUIC
        else -> TransportNetwork.TCP
    }

    private fun splitFragment(s: String): Pair<String, String> {
        val idx = s.indexOf('#')
        return if (idx >= 0) {
            s.substring(0, idx) to decodeUri(s.substring(idx + 1))
        } else s to ""
    }

    private fun splitQuery(s: String): Pair<String, String> {
        val idx = s.indexOf('?')
        return if (idx >= 0) s.substring(0, idx) to s.substring(idx + 1) else s to ""
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split('&').mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq <= 0) null
            else decodeUri(part.substring(0, eq)) to decodeUri(part.substring(eq + 1))
        }.toMap()
    }

    private fun parseHostPort(hostPort: String, defaultPort: Int): Pair<String, Int> {
        val cleaned = hostPort.trim().removePrefix("[").let {
            // IPv6 [addr]:port already handled partially
            it
        }
        return if (cleaned.startsWith("[")) {
            val end = cleaned.indexOf(']')
            val host = cleaned.substring(1, end)
            val port = cleaned.substring(end + 1).removePrefix(":").toIntOrNull() ?: defaultPort
            host to port
        } else {
            val colon = cleaned.lastIndexOf(':')
            if (colon > 0 && cleaned.indexOf(':') == colon) {
                cleaned.substring(0, colon) to (cleaned.substring(colon + 1).toIntOrNull() ?: defaultPort)
            } else cleaned to defaultPort
        }
    }

    private fun decodeUri(s: String): String = try {
        URLDecoder.decode(s, StandardCharsets.UTF_8.name())
    } catch (_: Exception) {
        s
    }

    private fun tryBase64Decode(input: String): String? {
        val normalized = input.trim()
            .replace('-', '+')
            .replace('_', '/')
            .replace("\\s".toRegex(), "")
        val pad = (4 - normalized.length % 4) % 4
        val padded = normalized + "=".repeat(pad)
        return try {
            String(Base64.decode(padded, Base64.DEFAULT), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            try {
                String(Base64.decode(padded, Base64.URL_SAFE), StandardCharsets.UTF_8)
            } catch (_: Exception) {
                null
            }
        }
    }
    // endregion
}
