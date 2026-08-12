package com.xraypulse.app.core.config

import com.xraypulse.app.data.model.AppSettings
import com.xraypulse.app.data.model.ServerProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies authoritative DNS config generation (no silent extra resolvers).
 * Default path is DoH **through the proxy** (not +local) so censored ISPs work.
 */
class DnsConfigTest {

    @Test
    fun singleDns_noAutoSecondary() {
        val s = AppSettings(dnsRemote = "1.1.1.1", dnsDomestic = "")
        assertEquals(listOf("1.1.1.1"), XrayConfigBuilder.parseUserDnsEntries(s))
        assertEquals(listOf("1.1.1.1"), XrayConfigBuilder.vpnDnsIps(s))
        // Through proxy DoH — not +local (ISP may block direct DoH)
        assertEquals("https://1.1.1.1/dns-query", XrayConfigBuilder.normalizeDnsServer("1.1.1.1"))
    }

    @Test
    fun dualDns_exactBoth() {
        val s = AppSettings(dnsRemote = "1.1.1.1", dnsDomestic = "1.0.0.1")
        assertEquals(listOf("1.1.1.1", "1.0.0.1"), XrayConfigBuilder.parseUserDnsEntries(s))
        assertEquals(listOf("1.1.1.1", "1.0.0.1"), XrayConfigBuilder.vpnDnsIps(s))
    }

    @Test
    fun keepsLocalSchemeIfExplicit() {
        assertEquals(
            "https+local://1.1.1.1/dns-query",
            XrayConfigBuilder.normalizeDnsServer("https+local://1.1.1.1/dns-query")
        )
        assertEquals(
            "tcp+local://8.8.8.8:53",
            XrayConfigBuilder.normalizeDnsServer("tcp+local://8.8.8.8:53")
        )
    }

    @Test
    fun buildJson_userServersOnlyInDnsModule() {
        val s = AppSettings(dnsRemote = "1.1.1.1", dnsDomestic = "1.0.0.1")
        val json = XrayConfigBuilder.build(
            ServerProfile(address = "10.0.0.1", port = 443, uuid = "00000000-0000-0000-0000-000000000001"),
            s
        )
        assertTrue(json.contains("https://1.1.1.1/dns-query"))
        assertTrue(json.contains("https://1.0.0.1/dns-query"))
        assertTrue(json.contains("disableFallback"))
        // Must not inject Google as a configured upstream
        assertFalse(json.contains("https://8.8.8.8"))
        assertFalse(json.contains("tcp://8.8.8.8"))
        // Default path is through proxy, not +local
        assertFalse(json.contains("https+local://1.1.1.1"))
    }

    @Test
    fun buildJson_userGoogleAllowed() {
        val s = AppSettings(dnsRemote = "8.8.8.8", dnsDomestic = "")
        val json = XrayConfigBuilder.build(
            ServerProfile(address = "10.0.0.1", port = 443, uuid = "00000000-0000-0000-0000-000000000001"),
            s
        )
        assertTrue(json.contains("https://8.8.8.8/dns-query"))
    }
}
