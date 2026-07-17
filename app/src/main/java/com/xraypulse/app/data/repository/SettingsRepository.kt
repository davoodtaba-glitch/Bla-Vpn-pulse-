package com.xraypulse.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.xraypulse.app.data.model.AppSettings
import com.xraypulse.app.data.model.RoutingMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("xraypulse_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val routing = stringPreferencesKey("routing")
        val sniffing = booleanPreferencesKey("sniffing")
        val mux = booleanPreferencesKey("mux")
        val muxConcurrency = intPreferencesKey("mux_concurrency")
        val socksPort = intPreferencesKey("socks_port")
        val httpPort = intPreferencesKey("http_port")
        val allowInsecure = booleanPreferencesKey("allow_insecure")
        val domainStrategy = stringPreferencesKey("domain_strategy")
        val logLevel = stringPreferencesKey("log_level")
        val perApp = booleanPreferencesKey("per_app")
        val perAppMode = stringPreferencesKey("per_app_mode")
        val perAppPackages = stringSetPreferencesKey("per_app_packages")
        val autoConnect = booleanPreferencesKey("auto_connect")
        val darkTheme = booleanPreferencesKey("dark_theme")
        val testUrl = stringPreferencesKey("test_url")
        val dnsRemote = stringPreferencesKey("dns_remote")
        val dnsDomestic = stringPreferencesKey("dns_domestic")
        val fragment = booleanPreferencesKey("fragment")
        val fragmentPackets = stringPreferencesKey("fragment_packets")
        val fragmentLength = stringPreferencesKey("fragment_length")
        val fragmentInterval = stringPreferencesKey("fragment_interval")
        val fragmentMaxSplit = stringPreferencesKey("fragment_max_split")
        val accentColor = stringPreferencesKey("accent_color")
        val sortByDelay = booleanPreferencesKey("sort_by_delay")
        val sessionTimeLimitMinutes = intPreferencesKey("session_time_limit_min")
        val sessionTrafficLimitMb = intPreferencesKey("session_traffic_limit_mb")
        val themeStyle = stringPreferencesKey("theme_style")
        val language = stringPreferencesKey("language")
        val limitActionOnReach = stringPreferencesKey("limit_action_on_reach")
        val keepAliveEnabled = booleanPreferencesKey("keep_alive_enabled")
        val keepAliveIntervalMinutes = intPreferencesKey("keep_alive_interval_min")
        val bypassDomains = stringPreferencesKey("bypass_domains")
        val allowLanProxy = booleanPreferencesKey("allow_lan_proxy")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            routingMode = runCatching {
                val raw = p[Keys.routing] ?: RoutingMode.BYPASS_LAN.name
                // Old installs may still store BYPASS_CN / CUSTOM — map to BYPASS_LAN
                when (raw) {
                    "GLOBAL" -> RoutingMode.GLOBAL
                    else -> RoutingMode.BYPASS_LAN
                }
            }.getOrDefault(RoutingMode.BYPASS_LAN),
            enableSniffing = p[Keys.sniffing] ?: true,
            enableMux = p[Keys.mux] ?: false,
            muxConcurrency = p[Keys.muxConcurrency] ?: 8,
            localSocksPort = p[Keys.socksPort] ?: 10808,
            localHttpPort = p[Keys.httpPort] ?: 10809,
            allowInsecure = p[Keys.allowInsecure] ?: false,
            domainStrategy = p[Keys.domainStrategy] ?: "AsIs",
            logLevel = p[Keys.logLevel] ?: "warning",
            perAppProxy = p[Keys.perApp] ?: false,
            perAppMode = p[Keys.perAppMode] ?: "bypass",
            perAppPackages = p[Keys.perAppPackages] ?: emptySet(),
            autoConnect = p[Keys.autoConnect] ?: false,
            darkTheme = p[Keys.darkTheme] ?: true,
            testUrl = p[Keys.testUrl] ?: "https://www.gstatic.com/generate_204",
            dnsRemote = p[Keys.dnsRemote] ?: "https://1.1.1.1/dns-query",
            dnsDomestic = p[Keys.dnsDomestic] ?: "localhost",
            fragmentEnabled = p[Keys.fragment] ?: false,
            fragmentPackets = p[Keys.fragmentPackets] ?: "tlshello",
            fragmentLength = p[Keys.fragmentLength] ?: "12-23",
            fragmentInterval = p[Keys.fragmentInterval] ?: "1-2",
            fragmentMaxSplit = p[Keys.fragmentMaxSplit] ?: "",
            accentColor = p[Keys.accentColor]?.let { hex ->
                hex.removePrefix("0x").removePrefix("0X").toLongOrNull(16)?.let { v ->
                    if (v <= 0xFFFFFFL) v or 0xFF000000L else v
                }
            } ?: 0xFF00E5C3,
            sortByDelay = p[Keys.sortByDelay] ?: false,
            sessionTimeLimitMinutes = p[Keys.sessionTimeLimitMinutes] ?: 60,
            sessionTrafficLimitMb = p[Keys.sessionTrafficLimitMb] ?: 512,
            limitActionOnReach = p[Keys.limitActionOnReach] ?: "notify",
            keepAliveEnabled = p[Keys.keepAliveEnabled] ?: true,
            keepAliveIntervalMinutes = (p[Keys.keepAliveIntervalMinutes] ?: 1).coerceIn(1, 120),
            bypassDomains = p[Keys.bypassDomains] ?: "",
            allowLanProxy = p[Keys.allowLanProxy] ?: false,
            // Classic Pulse is the only supported UI theme
            themeStyle = "PULSE",
            language = p[Keys.language] ?: "fa"
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(settingsFlow.first())
        context.dataStore.edit { p ->
            p[Keys.routing] = next.routingMode.name
            p[Keys.sniffing] = next.enableSniffing
            p[Keys.mux] = next.enableMux
            p[Keys.muxConcurrency] = next.muxConcurrency
            p[Keys.socksPort] = next.localSocksPort
            p[Keys.httpPort] = next.localHttpPort
            p[Keys.allowInsecure] = next.allowInsecure
            p[Keys.domainStrategy] = next.domainStrategy
            p[Keys.logLevel] = next.logLevel
            p[Keys.perApp] = next.perAppProxy
            p[Keys.perAppMode] = next.perAppMode
            p[Keys.perAppPackages] = next.perAppPackages
            p[Keys.autoConnect] = next.autoConnect
            p[Keys.darkTheme] = next.darkTheme
            p[Keys.testUrl] = next.testUrl
            p[Keys.dnsRemote] = next.dnsRemote
            p[Keys.dnsDomestic] = next.dnsDomestic
            p[Keys.fragment] = next.fragmentEnabled
            p[Keys.fragmentPackets] = next.fragmentPackets
            p[Keys.fragmentLength] = next.fragmentLength
            p[Keys.fragmentInterval] = next.fragmentInterval
            p[Keys.fragmentMaxSplit] = next.fragmentMaxSplit
            p[Keys.accentColor] = next.accentColor.toString(16)
            p[Keys.sortByDelay] = next.sortByDelay
            p[Keys.sessionTimeLimitMinutes] = next.sessionTimeLimitMinutes
            p[Keys.sessionTrafficLimitMb] = next.sessionTrafficLimitMb
            p[Keys.limitActionOnReach] = next.limitActionOnReach
            p[Keys.keepAliveEnabled] = next.keepAliveEnabled
            p[Keys.keepAliveIntervalMinutes] = next.keepAliveIntervalMinutes.coerceIn(1, 120)
            p[Keys.bypassDomains] = next.bypassDomains
            p[Keys.allowLanProxy] = next.allowLanProxy
            p[Keys.themeStyle] = "PULSE"
            p[Keys.language] = next.language
        }
    }
}
