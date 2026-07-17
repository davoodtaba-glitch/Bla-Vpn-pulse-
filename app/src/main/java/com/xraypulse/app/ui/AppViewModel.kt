package com.xraypulse.app.ui

import android.app.Application
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xraypulse.app.core.xray.XrayController
import com.xraypulse.app.data.model.AppSettings
import com.xraypulse.app.data.model.ConnectionState
import com.xraypulse.app.data.model.ServerProfile
import com.xraypulse.app.data.model.Subscription
import com.xraypulse.app.data.repository.ServerRepository
import com.xraypulse.app.data.repository.SettingsRepository
import com.xraypulse.app.service.XrayVpnService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val servers: List<ServerProfile> = emptyList(),
    val subscriptions: List<Subscription> = emptyList(),
    val selected: ServerProfile? = null,
    val settings: AppSettings = AppSettings(),
    val connection: ConnectionState = ConnectionState(),
    val message: String? = null,
    val isBusy: Boolean = false,
    val isTesting: Boolean = false,
    val testingServerId: Long? = null,
    val coreVersion: String = "",
    val searchQuery: String = ""
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val serversRepo = ServerRepository(app)
    private val settingsRepo = SettingsRepository(app)

    private val _message = MutableStateFlow<String?>(null)
    private val _busy = MutableStateFlow(false)
    private val _search = MutableStateFlow("")
    private val _isTesting = MutableStateFlow(false)
    private val _testingServerId = MutableStateFlow<Long?>(null)
    /** Bumps when custom i18n JSON is imported so UI reloads strings. */
    private val _customI18nTick = MutableStateFlow(0)
    val customI18nTick: StateFlow<Int> = _customI18nTick

    private data class CoreUi(
        val servers: List<ServerProfile>,
        val subscriptions: List<Subscription>,
        val selected: ServerProfile?,
        val settings: AppSettings,
        val connection: ConnectionState
    )

    private data class MetaUi(
        val message: String?,
        val busy: Boolean,
        val search: String,
        val isTesting: Boolean,
        val testingServerId: Long?
    )

    private val coreFlow = combine(
        serversRepo.servers,
        serversRepo.subscriptions,
        serversRepo.selected,
        settingsRepo.settingsFlow,
        XrayVpnService.state
    ) { servers, subs, selected, settings, conn ->
        CoreUi(servers, subs, selected, settings, conn)
    }

    private val metaFlow = combine(
        _message, _busy, _search, _isTesting, _testingServerId
    ) { msg, busy, search, testing, testingId ->
        MetaUi(msg, busy, search, testing, testingId)
    }

    val uiState: StateFlow<UiState> = combine(coreFlow, metaFlow) { core, meta ->
        val sorted = if (core.settings.sortByDelay) {
            core.servers.sortedWith(
                compareBy<ServerProfile> {
                    when {
                        it.latencyMs < 0 -> Long.MAX_VALUE
                        else -> it.latencyMs
                    }
                }
            )
        } else core.servers
        UiState(
            servers = sorted,
            subscriptions = core.subscriptions,
            selected = core.selected,
            settings = core.settings,
            connection = core.connection,
            message = meta.message,
            isBusy = meta.busy,
            isTesting = meta.isTesting,
            testingServerId = meta.testingServerId,
            coreVersion = XrayController.version(),
            searchQuery = meta.search
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    init {
        XrayController.initEnv(app)
    }

    fun setSearch(q: String) { _search.value = q }
    fun clearMessage() { _message.value = null }
    fun toast(msg: String) { _message.value = msg }

    fun selectServer(id: Long) = viewModelScope.launch {
        serversRepo.select(id)
        if (uiState.value.connection.isConnected || uiState.value.connection.isConnecting) {
            reconnectCurrent("Server changed — reconnecting…")
        }
    }

    fun deleteServer(id: Long) = viewModelScope.launch {
        serversRepo.delete(id)
        toast("Server removed")
    }

    fun deleteServers(ids: Set<Long>) = viewModelScope.launch {
        if (ids.isEmpty()) return@launch
        val n = serversRepo.deleteMany(ids)
        toast("Deleted $n server(s)")
    }

    fun deleteAllServers() = viewModelScope.launch {
        val n = serversRepo.deleteAll()
        toast("Deleted $n configs")
    }

    fun deleteInvalidServers() = viewModelScope.launch {
        _isTesting.value = true
        toast("Testing connections…")
        try {
            val settings = uiState.value.settings
            serversRepo.getAll().forEach { s ->
                _testingServerId.value = s.id
                val ms = serversRepo.testLatency(s, settings)
                if (ms < 0) serversRepo.markInvalid(s.id)
            }
            val n = serversRepo.deleteInvalid()
            toast("Removed $n invalid configs")
        } finally {
            _isTesting.value = false
            _testingServerId.value = null
        }
    }

    fun importLinks(text: String) = viewModelScope.launch {
        _busy.value = true
        try {
            val n = serversRepo.addFromLink(text)
            toast(if (n > 0) "Imported $n server(s)" else "No valid links found")
        } catch (e: Exception) {
            toast(e.message ?: "Import failed")
        } finally {
            _busy.value = false
        }
    }

    /**
     * Mixed import: share-link configs + zero or more named subscriptions.
     */
    fun importMixed(
        configsText: String,
        subscriptions: List<Pair<String, String>>,
        onDone: (String) -> Unit = {}
    ) = viewModelScope.launch {
        _busy.value = true
        try {
            var servers = 0
            var subs = 0
            if (configsText.isNotBlank()) {
                servers = serversRepo.addFromLink(configsText)
            }
            subscriptions.forEach { (name, url) ->
                if (url.isNotBlank()) {
                    serversRepo.addSubscription(name, url)
                    subs++
                }
            }
            val msg = buildString {
                if (servers > 0) append("Imported $servers server(s)")
                if (subs > 0) {
                    if (isNotEmpty()) append(" · ")
                    append("Added $subs subscription(s)")
                }
                if (isEmpty()) append("No valid links found")
            }
            toast(msg)
            onDone(msg)
        } catch (e: Exception) {
            val m = e.message ?: "Import failed"
            toast(m)
            onDone(m)
        } finally {
            _busy.value = false
        }
    }

    fun addSubscription(name: String, url: String) = viewModelScope.launch {
        _busy.value = true
        try {
            serversRepo.addSubscription(name, url)
            toast("Subscription added")
        } catch (e: Exception) {
            toast(e.message ?: "Subscription failed")
        } finally {
            _busy.value = false
        }
    }



    fun refreshSubscription(id: Long) = viewModelScope.launch {
        _busy.value = true
        try {
            val n = serversRepo.refreshSubscription(id)
            toast("Updated $n servers")
        } catch (e: Exception) {
            toast(e.message ?: "Update failed")
        } finally {
            _busy.value = false
        }
    }

    fun deleteSubscription(id: Long) = viewModelScope.launch {
        serversRepo.deleteSubscription(id)
        toast("Subscription deleted")
    }

    fun renameSubscription(id: Long, name: String) = viewModelScope.launch {
        serversRepo.renameSubscription(id, name)
        toast("Subscription renamed")
    }

    fun updateSettings(transform: (AppSettings) -> AppSettings) = viewModelScope.launch {
        val prev = uiState.value.settings
        val next = transform(prev)
        settingsRepo.update { next }
        if (uiState.value.connection.isConnected && needsVpnRestart(prev, next)) {
            reconnectCurrent("Settings updated — reconnecting…")
        }
    }

    /** Apply a full settings snapshot (from Settings draft + Confirm). */
    fun applySettings(next: AppSettings) = viewModelScope.launch {
        val prev = uiState.value.settings
        settingsRepo.update { next }
        toast("Settings applied")
        if (uiState.value.connection.isConnected && needsVpnRestart(prev, next)) {
            reconnectCurrent("Settings applied — reconnecting…")
        }
    }

    /**
     * Theme / accent apply immediately — never reconnects VPN.
     * Call from appearance chips for live preview.
     */
    fun applyAppearance(themeStyle: String, accentColor: Long) = viewModelScope.launch {
        settingsRepo.update {
            it.copy(themeStyle = "PULSE", accentColor = accentColor)
        }
    }

    /** Language change — instant, no VPN restart. */
    fun applyLanguage(language: String) = viewModelScope.launch {
        settingsRepo.update { it.copy(language = language) }
    }

    /**
     * Import a translation JSON map and save as custom string overrides.
     * JSON format: { "key": "translated text", ... }
     */
    fun importTranslationJson(json: String, onDone: (Boolean, String) -> Unit = { _, _ -> }) =
        viewModelScope.launch {
            val map = com.xraypulse.app.ui.i18n.AppStrings.parseJson(json)
            if (map == null || map.isEmpty()) {
                onDone(false, "invalid_json")
                toast("Invalid translation JSON")
                return@launch
            }
            com.xraypulse.app.ui.i18n.AppStrings.saveCustom(getApplication(), map)
            _customI18nTick.value = _customI18nTick.value + 1
            toast("Imported ${map.size} translation string(s)")
            onDone(true, "Imported ${map.size} strings")
        }

    fun exportTranslationJsonTemplate(): String =
        com.xraypulse.app.ui.i18n.AppStrings.exportTemplateJson()

    fun clearCustomTranslations() = viewModelScope.launch {
        val f = com.xraypulse.app.ui.i18n.AppStrings.customFile(getApplication())
        if (f.exists()) f.delete()
        _customI18nTick.value = _customI18nTick.value + 1
        toast("Custom translations cleared")
    }

    /** Fields that require a VPN restart when changed. */
    private fun needsVpnRestart(prev: AppSettings, next: AppSettings): Boolean {
        // Appearance-only changes (theme, color, language, sort)
        if (prev.copy(
                themeStyle = next.themeStyle,
                accentColor = next.accentColor,
                darkTheme = next.darkTheme,
                sortByDelay = next.sortByDelay,
                language = next.language,
                limitActionOnReach = next.limitActionOnReach,
                keepAliveEnabled = next.keepAliveEnabled,
                keepAliveIntervalMinutes = next.keepAliveIntervalMinutes
            ) == next
        ) {
            return false
        }
        // Session limits / keep-alive can update without full reconnect if only those changed
        val onlyLimits = prev.copy(
            sessionTimeLimitMinutes = next.sessionTimeLimitMinutes,
            sessionTrafficLimitMb = next.sessionTrafficLimitMb,
            limitActionOnReach = next.limitActionOnReach,
            keepAliveEnabled = next.keepAliveEnabled,
            keepAliveIntervalMinutes = next.keepAliveIntervalMinutes,
            themeStyle = next.themeStyle,
            accentColor = next.accentColor,
            darkTheme = next.darkTheme,
            sortByDelay = next.sortByDelay,
            language = next.language
        ) == next
        if (onlyLimits) return false
        return true
    }

    /**
     * Quick Setup: detect subscription URL vs share-link config, import,
     * select a server, then start VPN (via permission launcher if needed).
     */
    fun quickSetupImportAndConnect(
        raw: String,
        vpnPermissionLauncher: ActivityResultLauncher<Intent>?,
        onResult: (ok: Boolean, messageKey: String) -> Unit
    ) = viewModelScope.launch {
        val text = raw.trim()
        if (text.isEmpty()) {
            onResult(false, "qs_empty")
            return@launch
        }
        _busy.value = true
        try {
            when {
                isSubscriptionUrl(text) -> {
                    val id = serversRepo.addSubscription("Quick setup", text)
                    val all = serversRepo.getAll().filter { it.subscriptionId == id }
                    val pick = all.firstOrNull()
                    if (pick == null) {
                        onResult(false, "qs_invalid")
                        toast("Subscription has no servers")
                        return@launch
                    }
                    serversRepo.select(pick.id)
                    toast("Subscription updated — connecting…")
                    onResult(true, "qs_done_sub")
                    kotlinx.coroutines.delay(200)
                    forceConnect(pick, vpnPermissionLauncher)
                }
                isShareConfig(text) -> {
                    val n = serversRepo.addFromLink(text)
                    if (n <= 0) {
                        onResult(false, "qs_invalid")
                        return@launch
                    }
                    val newest = serversRepo.getAll().maxByOrNull { it.id }
                    if (newest != null) {
                        serversRepo.select(newest.id)
                        toast("Config added — connecting…")
                        onResult(true, "qs_done_config")
                        kotlinx.coroutines.delay(200)
                        forceConnect(newest, vpnPermissionLauncher)
                    } else {
                        onResult(false, "qs_invalid")
                    }
                }
                else -> {
                    onResult(false, "qs_invalid")
                    toast("Not a valid config or subscription URL")
                }
            }
        } catch (e: Exception) {
            onResult(false, "qs_invalid")
            toast(e.message ?: "Import failed")
        } finally {
            _busy.value = false
        }
    }

    private fun isSubscriptionUrl(text: String): Boolean {
        val t = text.trim().lowercase()
        if (t.startsWith("http://") || t.startsWith("https://")) {
            // Not a share deep-link disguised as http
            if (t.contains("vless://") || t.contains("vmess://") || t.contains("trojan://")) return false
            return true
        }
        return false
    }

    private fun isShareConfig(text: String): Boolean {
        val t = text.trim().lowercase()
        val schemes = listOf(
            "vless://", "vmess://", "trojan://", "ss://", "ssr://",
            "socks://", "socks5://", "wireguard://", "wg://", "hysteria://",
            "hysteria2://", "hy2://", "tuic://", "http://", "https://"
        )
        if (schemes.any { t.startsWith(it) || t.contains("\n$it") || t.contains(" $it") }) {
            // pure http(s) subscription already handled
            if ((t.startsWith("http://") || t.startsWith("https://")) &&
                !t.contains("vmess://") && !t.contains("vless://")
            ) {
                // multi-line base64 body could still be subscription content pasted as-is —
                // if single line URL without share scheme, treat as sub (handled above)
                return !(t.startsWith("http://") || t.startsWith("https://"))
            }
            return true
        }
        // base64 multi-config blob or bare JSON
        if (text.length > 40 && !text.contains(' ') && text.matches(Regex("^[A-Za-z0-9+/=\\s\\n\\r]+$"))) {
            return true
        }
        return com.xraypulse.app.core.parser.ShareLinkParser.parseMulti(text).isNotEmpty() ||
            com.xraypulse.app.core.parser.ShareLinkParser.parseSingle(text) != null
    }

    fun setSortByDelay(enabled: Boolean) = viewModelScope.launch {
        settingsRepo.update { it.copy(sortByDelay = enabled) }
    }

    fun testLatency(server: ServerProfile) = viewModelScope.launch {
        _isTesting.value = true
        _testingServerId.value = server.id
        toast("Testing…")
        try {
            val ms = serversRepo.testLatency(server, uiState.value.settings)
            toast(if (ms >= 0) "Latency: ${ms}ms" else "Test failed")
        } finally {
            _isTesting.value = false
            _testingServerId.value = null
        }
    }

    fun testAll() = viewModelScope.launch {
        _isTesting.value = true
        toast("Testing connections…")
        try {
            val settings = uiState.value.settings
            serversRepo.getAll().forEach { s ->
                _testingServerId.value = s.id
                serversRepo.testLatency(s, settings)
            }
            toast("Latency test finished")
        } finally {
            _isTesting.value = false
            _testingServerId.value = null
        }
    }

    fun connectOrDisconnect(vpnPermissionLauncher: ActivityResultLauncher<Intent>?) {
        val state = uiState.value
        if (state.connection.isConnected || state.connection.isConnecting) {
            XrayVpnService.stop(getApplication())
            return
        }
        val selected = state.selected
        if (selected == null) {
            toast("Select a server first")
            return
        }
        forceConnect(selected, vpnPermissionLauncher)
    }

    /** Always start/reconnect VPN for the given profile (used by Quick Setup). */
    private fun forceConnect(
        profile: ServerProfile,
        vpnPermissionLauncher: ActivityResultLauncher<Intent>?
    ) {
        val state = uiState.value
        if (state.connection.isConnected || state.connection.isConnecting) {
            XrayVpnService.reconnect(getApplication(), profile, state.settings)
            return
        }
        val prepare = XrayVpnService.prepare(getApplication())
        if (prepare != null) {
            vpnPermissionLauncher?.launch(prepare)
        } else {
            startVpn(profile)
        }
    }

    fun onVpnPermissionGranted() {
        val selected = uiState.value.selected ?: run {
            toast("Select a server first")
            return
        }
        startVpn(selected)
    }

    private fun startVpn(profile: ServerProfile) {
        XrayVpnService.start(getApplication(), profile, uiState.value.settings)
        if (!XrayController.isCoreAvailable()) {
            toast("Xray core not linked — rebuild with libv2ray.aar")
        }
    }

    fun reconnectCurrent(msg: String? = null) {
        val selected = uiState.value.selected ?: return
        msg?.let { toast(it) }
        XrayVpnService.reconnect(getApplication(), selected, uiState.value.settings)
    }

    fun saveManualServer(profile: ServerProfile) = viewModelScope.launch {
        val id = if (profile.id == 0L) {
            serversRepo.insert(profile)
        } else {
            serversRepo.update(profile)
            profile.id
        }
        serversRepo.select(if (id > 0) id else profile.id)
        toast("Server saved")
        if (uiState.value.connection.isConnected) {
            reconnectCurrent("Config edited — reconnecting…")
        }
    }
}
