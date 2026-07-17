package com.xraypulse.app.core.parser

import android.util.Base64
import com.google.gson.JsonObject
import com.xraypulse.app.data.model.ProtocolType
import com.xraypulse.app.data.model.ServerProfile
import com.xraypulse.app.data.model.StreamSecurity
import com.xraypulse.app.data.model.TransportNetwork
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Exports ServerProfile to v2rayN-compatible share links.
 */
object ShareLinkExporter {

    fun export(profile: ServerProfile): String {
        if (profile.shareLink.isNotBlank()) return profile.shareLink
        return when (profile.protocol) {
            ProtocolType.VLESS -> exportVless(profile)
            ProtocolType.VMESS -> exportVmess(profile)
            ProtocolType.TROJAN -> exportTrojan(profile)
            ProtocolType.SHADOWSOCKS -> exportSs(profile)
            else -> profile.rawConfig.ifBlank { "" }
        }
    }

    private fun exportVless(p: ServerProfile): String {
        val params = linkedMapOf<String, String>()
        params["encryption"] = p.encryption.ifBlank { "none" }
        params["security"] = securityName(p.security)
        params["type"] = networkName(p.network)
        if (p.flow.isNotBlank()) params["flow"] = p.flow
        if (p.sni.isNotBlank()) params["sni"] = p.sni
        if (p.fingerprint.isNotBlank()) params["fp"] = p.fingerprint
        if (p.alpn.isNotBlank()) params["alpn"] = p.alpn
        if (p.publicKey.isNotBlank()) params["pbk"] = p.publicKey
        if (p.shortId.isNotBlank()) params["sid"] = p.shortId
        if (p.spiderX.isNotBlank()) params["spx"] = p.spiderX
        if (p.path.isNotBlank()) params["path"] = p.path
        if (p.host.isNotBlank()) params["host"] = p.host
        if (p.serviceName.isNotBlank()) params["serviceName"] = p.serviceName
        if (p.mode.isNotBlank()) params["mode"] = p.mode
        if (p.headerType.isNotBlank() && p.headerType != "none") params["headerType"] = p.headerType
        val query = params.entries.joinToString("&") { (k, v) ->
            "$k=${enc(v)}"
        }
        val fragment = enc(p.remark.ifBlank { "${p.address}:${p.port}" })
        return "vless://${p.uuid}@${p.address}:${p.port}?$query#$fragment"
    }

    private fun exportVmess(p: ServerProfile): String {
        val obj = JsonObject().apply {
            addProperty("v", "2")
            addProperty("ps", p.remark)
            addProperty("add", p.address)
            addProperty("port", p.port.toString())
            addProperty("id", p.uuid)
            addProperty("aid", p.alterId.toString())
            addProperty("scy", p.encryption.ifBlank { "auto" })
            addProperty("net", networkName(p.network))
            addProperty("type", p.headerType.ifBlank { "none" })
            addProperty("host", p.host)
            addProperty("path", p.path)
            addProperty(
                "tls", when (p.security) {
                    StreamSecurity.TLS -> "tls"
                    StreamSecurity.REALITY -> "reality"
                    else -> ""
                }
            )
            addProperty("sni", p.sni)
            addProperty("fp", p.fingerprint)
            addProperty("alpn", p.alpn)
            if (p.publicKey.isNotBlank()) addProperty("pbk", p.publicKey)
            if (p.shortId.isNotBlank()) addProperty("sid", p.shortId)
        }
        val b64 = Base64.encodeToString(
            obj.toString().toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP
        )
        return "vmess://$b64"
    }

    private fun exportTrojan(p: ServerProfile): String {
        val params = linkedMapOf<String, String>()
        params["security"] = securityName(p.security).ifBlank { "tls" }
        params["type"] = networkName(p.network)
        if (p.sni.isNotBlank()) params["sni"] = p.sni
        if (p.fingerprint.isNotBlank()) params["fp"] = p.fingerprint
        if (p.path.isNotBlank()) params["path"] = p.path
        if (p.host.isNotBlank()) params["host"] = p.host
        if (p.publicKey.isNotBlank()) params["pbk"] = p.publicKey
        if (p.shortId.isNotBlank()) params["sid"] = p.shortId
        val query = params.entries.joinToString("&") { "${it.key}=${enc(it.value)}" }
        val pass = enc(p.password.ifBlank { p.uuid })
        val fragment = enc(p.remark)
        return "trojan://$pass@${p.address}:${p.port}?$query#$fragment"
    }

    private fun exportSs(p: ServerProfile): String {
        val userInfo = Base64.encodeToString(
            "${p.encryption}:${p.password}".toByteArray(StandardCharsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP
        )
        return "ss://$userInfo@${p.address}:${p.port}#${enc(p.remark)}"
    }

    private fun securityName(s: StreamSecurity) = when (s) {
        StreamSecurity.NONE -> "none"
        StreamSecurity.TLS -> "tls"
        StreamSecurity.REALITY -> "reality"
    }

    private fun networkName(n: TransportNetwork) = when (n) {
        TransportNetwork.TCP -> "tcp"
        TransportNetwork.KCP -> "kcp"
        TransportNetwork.WS -> "ws"
        TransportNetwork.HTTP, TransportNetwork.H2 -> "http"
        TransportNetwork.HTTPUPGRADE -> "httpupgrade"
        TransportNetwork.XHTTP -> "xhttp"
        TransportNetwork.GRPC -> "grpc"
        TransportNetwork.QUIC -> "quic"
    }

    private fun enc(s: String): String =
        URLEncoder.encode(s, StandardCharsets.UTF_8.name()).replace("+", "%20")
}
