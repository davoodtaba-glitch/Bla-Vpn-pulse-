package com.xraypulse.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xraypulse.app.BuildConfig
import com.xraypulse.app.data.model.AppSettings
import com.xraypulse.app.data.model.FragmentPresets
import com.xraypulse.app.data.model.RoutingMode
import com.xraypulse.app.ui.components.GlassCard
import com.xraypulse.app.ui.components.HsvColorPicker
import com.xraypulse.app.ui.components.ltrWrap
import com.xraypulse.app.ui.theme.LocalAccent
import com.xraypulse.app.ui.theme.LocalPalette
import com.xraypulse.app.ui.theme.ThemeMainColors
import com.xraypulse.app.ui.theme.toComposeColor
import com.xraypulse.app.util.AppUpdater
import com.xraypulse.app.util.GithubReleaseInfo
import com.xraypulse.app.util.UpdateCheckResult
import com.xraypulse.app.util.withLtrPlaceholders
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Main + optional alternative DNS for each preset (user can clear alt for single-DNS). */
private val DnsPresets = listOf(
    Triple("1.1.1.1", "1.0.0.1", "dns_preset_cloudflare"),
    Triple("8.8.8.8", "8.8.4.4", "dns_preset_google"),
    Triple("9.9.9.9", "149.112.112.112", "dns_preset_quad9"),
    Triple("208.67.222.222", "208.67.220.220", "dns_preset_opendns"),
    Triple("94.140.14.14", "94.140.15.15", "dns_preset_adguard"),
    Triple("76.76.2.0", "76.76.10.0", "dns_preset_controld"),
    Triple("185.228.168.9", "185.228.169.9", "dns_preset_cleardns"),
    Triple("64.6.64.6", "64.6.65.6", "dns_preset_verisign"),
    Triple("77.88.8.8", "77.88.8.1", "dns_preset_yandex"),
    Triple("45.90.28.0", "45.90.30.0", "dns_preset_nextdns"),
    Triple("8.26.56.26", "8.20.247.20", "dns_preset_comodo"),
    Triple("156.154.70.1", "156.154.71.1", "dns_preset_neustar")
)

@Composable
fun SettingsScreen(
    settings: AppSettings,
    coreVersion: String,
    onApply: (AppSettings) -> Unit,
    onApplyAppearance: (themeStyle: String, accentColor: Long, accentSecondary: Long) -> Unit =
        { _, _, _ -> },
    onApplyLanguage: (String) -> Unit = {},
    onOpenPerApp: () -> Unit = {},
    onDirtyChange: (Boolean) -> Unit = {},
    /** Incremented by host when user tries to leave Settings (e.g. bottom nav). */
    leaveAttempt: Int = 0,
    /** Called after user resolves leave: true = navigate away, false = stay. */
    onLeaveResolved: (Boolean) -> Unit = {}
) {
    val accent = LocalAccent.current
    val p = LocalPalette.current
    // Draft copy — edits stay local until Apply (fixes reverse typing + accidental apply)
    var draft by remember { mutableStateOf(settings) }
    var helpKey by remember { mutableStateOf<String?>(null) }
    // All sections closed by default
    var openSections by remember { mutableStateOf(setOf<String>()) }
    fun isOpen(key: String) = key in openSections
    fun toggle(key: String) {
        openSections = if (key in openSections) openSections - key else openSections + key
    }

    val dirty = draft != settings
    val scroll = rememberScrollState()
    var showLeaveDialog by remember { mutableStateOf(false) }

    // Sync draft only when there are no local unsaved edits (prevents wipe/jank while typing or dragging color)
    LaunchedEffect(settings) {
        if (!dirty) {
            draft = settings
        }
    }

    LaunchedEffect(dirty) {
        onDirtyChange(dirty)
    }

    // Host requested leave (bottom nav) or system back while dirty
    LaunchedEffect(leaveAttempt) {
        if (leaveAttempt > 0) {
            if (dirty) showLeaveDialog = true
            else onLeaveResolved(true)
        }
    }

    BackHandler(enabled = dirty) {
        showLeaveDialog = true
    }

    fun applyAndLeave() {
        onApply(draft)
        showLeaveDialog = false
        onLeaveResolved(true)
    }

    fun discardAndLeave() {
        draft = settings
        showLeaveDialog = false
        onLeaveResolved(true)
    }

    fun cancelLeave() {
        showLeaveDialog = false
        onLeaveResolved(false)
    }

    if (showLeaveDialog) {
        AlertDialog(
            onDismissRequest = { cancelLeave() },
            title = { Text(com.xraypulse.app.ui.i18n.t("settings_leave_title")) },
            text = { Text(com.xraypulse.app.ui.i18n.t("settings_leave_body")) },
            confirmButton = {
                TextButton(onClick = { applyAndLeave() }) {
                    Text(com.xraypulse.app.ui.i18n.t("apply_changes"), color = accent, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { discardAndLeave() }) {
                        Text(com.xraypulse.app.ui.i18n.t("discard"))
                    }
                    TextButton(onClick = { cancelLeave() }) {
                        Text(com.xraypulse.app.ui.i18n.t("cancel"))
                    }
                }
            }
        )
    }

    if (helpKey != null) {
        SettingsHelpScreen(
            titleKey = "help_${helpKey}_title",
            bodyKey = "help_${helpKey}_body",
            onClose = { helpKey = null }
        )
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .imePadding()
    ) {
        // Fixed header (does not scroll away)
        Column(
            Modifier
                .fillMaxWidth()
                .background(p.bg)
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp, bottom = 8.dp)
        ) {
            Text(
                com.xraypulse.app.ui.i18n.t("settings"),
                color = p.text,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (dirty) com.xraypulse.app.ui.i18n.t("unsaved_changes")
                else com.xraypulse.app.ui.i18n.t("settings_subtitle"),
                color = if (dirty) p.warning else p.muted,
                fontSize = 13.sp
            )

            // Sticky Apply / Cancel — only while settings are dirty
            AnimatedVisibility(
                visible = dirty,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(p.surface.copy(alpha = 0.95f))
                        .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { draft = settings },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(com.xraypulse.app.ui.i18n.t("discard"), fontSize = 13.sp)
                    }
                    Button(
                        onClick = { onApply(draft) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            com.xraypulse.app.ui.i18n.t("apply_changes"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Scrollable body only
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = 16.dp)
                .padding(top = 8.dp, bottom = 32.dp)
        ) {
        ExpandableSection(
            title = com.xraypulse.app.ui.i18n.t("routing_mode"),
            expanded = isOpen("routing"),
            onToggle = { toggle("routing") },
            onHelp = { helpKey = "routing" }
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(8.dp)) {
                    listOf(RoutingMode.GLOBAL, RoutingMode.BYPASS_LAN).forEach { mode ->
                        val label = when (mode) {
                            RoutingMode.GLOBAL -> com.xraypulse.app.ui.i18n.t("global_proxy")
                            RoutingMode.BYPASS_LAN -> com.xraypulse.app.ui.i18n.t("bypass_lan")
                        }
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = draft.routingMode == mode,
                                    onClick = { draft = draft.copy(routingMode = mode) },
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = draft.routingMode == mode,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(selectedColor = accent)
                            )
                            Text(label, color = p.text, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ExpandableSection(
            title = com.xraypulse.app.ui.i18n.t("core_options"),
            expanded = isOpen("core"),
            onToggle = { toggle("core") },
            onHelp = { helpKey = "core" }
        ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column {
                SwitchRow(com.xraypulse.app.ui.i18n.t("domain_sniffing"), draft.enableSniffing) {
                    draft = draft.copy(enableSniffing = it)
                }
                SwitchRow(com.xraypulse.app.ui.i18n.t("mux"), draft.enableMux) {
                    draft = draft.copy(enableMux = it)
                }
                SwitchRow(com.xraypulse.app.ui.i18n.t("allow_insecure"), draft.allowInsecure) {
                    draft = draft.copy(allowInsecure = it)
                }
                SwitchRow(com.xraypulse.app.ui.i18n.t("auto_connect"), draft.autoConnect) {
                    draft = draft.copy(autoConnect = it)
                }
                SwitchRow(com.xraypulse.app.ui.i18n.t("keep_alive"), draft.keepAliveEnabled) {
                    draft = draft.copy(keepAliveEnabled = it)
                }
                Text(
                    com.xraypulse.app.ui.i18n.t("keep_alive_hint"),
                    color = p.muted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                if (draft.keepAliveEnabled) {
                    Spacer(Modifier.height(4.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            LtrSettingsField(
                                label = com.xraypulse.app.ui.i18n.t("keep_alive_interval"),
                                value = draft.keepAliveIntervalMinutes.toString(),
                                enabled = true,
                                placeholder = "1",
                                onChange = { v ->
                                    val n = v.filter { it.isDigit() }.toIntOrNull() ?: 1
                                    draft = draft.copy(keepAliveIntervalMinutes = n.coerceIn(1, 120))
                                }
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                com.xraypulse.app.ui.i18n.t("keep_alive_interval_hint"),
                                color = p.muted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                SwitchRow(com.xraypulse.app.ui.i18n.t("allow_lan_proxy"), draft.allowLanProxy) {
                    draft = draft.copy(allowLanProxy = it)
                }
                Text(
                    com.xraypulse.app.ui.i18n.t("allow_lan_proxy_hint"),
                    color = p.muted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
        }

        Spacer(Modifier.height(12.dp))

        ExpandableSection(
            title = com.xraypulse.app.ui.i18n.t("app_domain_routing"),
            expanded = isOpen("routing_apps"),
            onToggle = { toggle("routing_apps") },
            onHelp = { helpKey = "bypass" }
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        com.xraypulse.app.ui.i18n.t("app_domain_routing_hint"),
                        color = p.muted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        com.xraypulse.app.ui.i18n.t("bypass_domains"),
                        color = p.text,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        com.xraypulse.app.ui.i18n.t("bypass_domains_hint"),
                        color = p.muted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        OutlinedTextField(
                            value = draft.bypassDomains,
                            onValueChange = { draft = draft.copy(bypassDomains = it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            placeholder = {
                                Text(
                                    "*.example.com\n*cdn*\ninternal.company.com",
                                    color = p.muted,
                                    fontSize = 12.sp
                                )
                            },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                textDirection = TextDirection.Ltr
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = p.text,
                                unfocusedTextColor = p.text,
                                focusedBorderColor = accent,
                                unfocusedBorderColor = p.border,
                                cursorColor = accent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        com.xraypulse.app.ui.i18n.t("per_app_proxy"),
                        color = p.text,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenPerApp) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                com.xraypulse.app.ui.i18n.t("configure_apps"),
                                color = accent,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (settings.perAppProxy)
                                    com.xraypulse.app.ui.i18n.t("per_app_on")
                                        .replace("{n}", settings.perAppPackages.size.toString())
                                        .replace("{mode}", settings.perAppMode)
                                else
                                    com.xraypulse.app.ui.i18n.t("per_app_off"),
                                color = p.muted,
                                fontSize = 12.sp
                            )
                            Text(
                                com.xraypulse.app.ui.i18n.t("per_app_saved"),
                                color = p.muted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ExpandableSection(
            title = com.xraypulse.app.ui.i18n.t("appearance"),
            expanded = isOpen("appearance"),
            onToggle = { toggle("appearance") },
            onHelp = { helpKey = "appearance" }
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                // Custom picker only when user picks "Custom" (not a preset chip)
                val presetArgs = remember { ThemeMainColors.map { it.first }.toSet() }
                var primaryCustom by remember {
                    mutableStateOf(draft.accentColor !in presetArgs)
                }
                var secondaryCustom by remember {
                    mutableStateOf(draft.accentColorSecondary !in presetArgs)
                }
                Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(draft.accentColor.toComposeColor())
                                .border(1.dp, Color.White.copy(0.25f), CircleShape)
                        )
                        Text(
                            com.xraypulse.app.ui.i18n.t("primary_color"),
                            color = p.muted,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(draft.accentColorSecondary.toComposeColor())
                                .border(1.dp, Color.White.copy(0.25f), CircleShape)
                        )
                        Text(
                            com.xraypulse.app.ui.i18n.t("secondary_color"),
                            color = p.muted,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        com.xraypulse.app.ui.i18n.t("primary_color"),
                        color = p.text,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    ColorChipGrid(
                        selectedArgb = draft.accentColor,
                        customSelected = primaryCustom,
                        onSelect = { argb ->
                            primaryCustom = false
                            draft = draft.copy(themeStyle = "PULSE", accentColor = argb)
                            onApplyAppearance("PULSE", argb, draft.accentColorSecondary)
                        },
                        onCustomClick = { primaryCustom = true }
                    )
                    AnimatedVisibility(
                        visible = primaryCustom,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            HsvColorPicker(
                                colorArgb = draft.accentColor,
                                onCommit = { argb ->
                                    draft = draft.copy(themeStyle = "PULSE", accentColor = argb)
                                    onApplyAppearance("PULSE", argb, draft.accentColorSecondary)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                compact = true
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        com.xraypulse.app.ui.i18n.t("secondary_color"),
                        color = p.text,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    ColorChipGrid(
                        selectedArgb = draft.accentColorSecondary,
                        customSelected = secondaryCustom,
                        onSelect = { argb ->
                            secondaryCustom = false
                            draft = draft.copy(themeStyle = "PULSE", accentColorSecondary = argb)
                            onApplyAppearance("PULSE", draft.accentColor, argb)
                        },
                        onCustomClick = { secondaryCustom = true }
                    )
                    AnimatedVisibility(
                        visible = secondaryCustom,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column {
                            Spacer(Modifier.height(8.dp))
                            HsvColorPicker(
                                colorArgb = draft.accentColorSecondary,
                                onCommit = { argb ->
                                    draft = draft.copy(themeStyle = "PULSE", accentColorSecondary = argb)
                                    onApplyAppearance("PULSE", draft.accentColor, argb)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                compact = true
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ExpandableSection(
            title = com.xraypulse.app.ui.i18n.t("language"),
            expanded = isOpen("language"),
            onToggle = { toggle("language") },
            onHelp = { helpKey = "language" }
        ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.language.lowercase() in listOf("en", "english"),
                        onClick = {
                            draft = draft.copy(language = "en")
                            onApplyLanguage("en")
                        },
                        label = { Text(com.xraypulse.app.ui.i18n.t("english")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(0.25f),
                            selectedLabelColor = accent,
                            containerColor = p.surface2,
                            labelColor = p.text
                        )
                    )
                    FilterChip(
                        selected = settings.language.lowercase() in listOf("fa", "fa-ir", "persian", "farsi"),
                        onClick = {
                            draft = draft.copy(language = "fa")
                            onApplyLanguage("fa")
                        },
                        label = { Text(com.xraypulse.app.ui.i18n.t("persian")) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(0.25f),
                            selectedLabelColor = accent,
                            containerColor = p.surface2,
                            labelColor = p.text
                        )
                    )
                }
            }
        }
        }

        Spacer(Modifier.height(12.dp))

        ExpandableSection(
            title = com.xraypulse.app.ui.i18n.t("tls_fragment"),
            expanded = isOpen("fragment"),
            onToggle = { toggle("fragment") },
            onHelp = { helpKey = "fragment" }
        ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                SwitchRow(com.xraypulse.app.ui.i18n.t("enable_fragment"), draft.fragmentEnabled) {
                    draft = draft.copy(fragmentEnabled = it)
                }
                Text(
                    com.xraypulse.app.ui.i18n.t("fragment_desc"),
                    color = p.muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(com.xraypulse.app.ui.i18n.t("packets_type"), color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                ChipRow(
                    options = FragmentPresets.PACKETS,
                    selected = draft.fragmentPackets,
                    enabled = draft.fragmentEnabled,
                    onSelect = { draft = draft.copy(fragmentPackets = it) }
                )
                Text(
                    com.xraypulse.app.ui.i18n.t("packets_hint"),
                    color = p.muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                Text(com.xraypulse.app.ui.i18n.t("length_range"), color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                ChipRow(
                    options = FragmentPresets.LENGTHS,
                    selected = draft.fragmentLength,
                    enabled = draft.fragmentEnabled,
                    onSelect = { draft = draft.copy(fragmentLength = it) }
                )
                Spacer(Modifier.height(8.dp))
                LtrSettingsField(
                    label = com.xraypulse.app.ui.i18n.t("custom_length"),
                    value = draft.fragmentLength,
                    enabled = draft.fragmentEnabled,
                    placeholder = "12-23",
                    onChange = { draft = draft.copy(fragmentLength = it) }
                )

                Spacer(Modifier.height(12.dp))
                Text(com.xraypulse.app.ui.i18n.t("interval_range"), color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                ChipRow(
                    options = FragmentPresets.INTERVALS,
                    selected = draft.fragmentInterval,
                    enabled = draft.fragmentEnabled,
                    onSelect = { draft = draft.copy(fragmentInterval = it) }
                )
                Spacer(Modifier.height(8.dp))
                LtrSettingsField(
                    label = com.xraypulse.app.ui.i18n.t("custom_interval"),
                    value = draft.fragmentInterval,
                    enabled = draft.fragmentEnabled,
                    placeholder = "1-2",
                    onChange = { draft = draft.copy(fragmentInterval = it) }
                )

                Spacer(Modifier.height(12.dp))
                Text(com.xraypulse.app.ui.i18n.t("max_split"), color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                ChipRow(
                    options = listOf("off") + FragmentPresets.MAX_SPLITS.filter { it.isNotEmpty() },
                    selected = draft.fragmentMaxSplit.ifBlank { "off" },
                    enabled = draft.fragmentEnabled,
                    onSelect = { v ->
                        draft = draft.copy(fragmentMaxSplit = if (v == "off") "" else v)
                    }
                )
                Spacer(Modifier.height(8.dp))
                LtrSettingsField(
                    label = com.xraypulse.app.ui.i18n.t("custom_max_split"),
                    value = draft.fragmentMaxSplit,
                    enabled = draft.fragmentEnabled,
                    placeholder = "100-200",
                    onChange = { draft = draft.copy(fragmentMaxSplit = it) }
                )

                Spacer(Modifier.height(12.dp))
                Text(
                    "packets=${draft.fragmentPackets} · length=${draft.fragmentLength} · interval=${draft.fragmentInterval}".ltrWrap() +
                        if (draft.fragmentMaxSplit.isNotBlank()) " · maxSplit=${draft.fragmentMaxSplit}".ltrWrap() else "",
                    color = if (draft.fragmentEnabled) accent else p.muted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
        }

        Spacer(Modifier.height(12.dp))

        ExpandableSection(
            title = com.xraypulse.app.ui.i18n.t("vpn_tunnel"),
            expanded = isOpen("vpn_tunnel"),
            onToggle = { toggle("vpn_tunnel") },
            onHelp = null
        ) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    com.xraypulse.app.ui.i18n.t("vpn_tunnel_hint"),
                    color = p.muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                SwitchRow(com.xraypulse.app.ui.i18n.t("use_fake_dns"), draft.useFakeDns) {
                    draft = draft.copy(useFakeDns = it)
                }
                Text(
                    com.xraypulse.app.ui.i18n.t("use_fake_dns_hint"),
                    color = p.muted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
                Spacer(Modifier.height(8.dp))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                        LtrSettingsField(
                            label = com.xraypulse.app.ui.i18n.t("mtu"),
                            value = draft.mtu.toString(),
                            enabled = true,
                            placeholder = "1500",
                            onChange = { v ->
                                val n = v.filter { it.isDigit() }.toIntOrNull() ?: 1500
                                draft = draft.copy(mtu = n.coerceIn(1280, 1500))
                            }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            com.xraypulse.app.ui.i18n.t("mtu_hint"),
                            color = p.muted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
        }

        Spacer(Modifier.height(12.dp))

        ExpandableSection(
            title = com.xraypulse.app.ui.i18n.t("dns"),
            expanded = isOpen("dns"),
            onToggle = { toggle("dns") },
            onHelp = { helpKey = "ports" }
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        com.xraypulse.app.ui.i18n.t("dns_section_hint"),
                        color = p.muted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DnsPresets.forEach { (main, alt, labelKey) ->
                            val selected = draft.dnsRemote.trim() == main
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    draft = draft.copy(dnsRemote = main, dnsDomestic = alt)
                                },
                                label = { Text(com.xraypulse.app.ui.i18n.t(labelKey)) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent.copy(alpha = 0.22f),
                                    selectedLabelColor = accent
                                )
                            )
                        }
                        val isCustom = DnsPresets.none { it.first == draft.dnsRemote.trim() }
                        FilterChip(
                            selected = isCustom,
                            onClick = { /* keep current as custom */ },
                            label = { Text(com.xraypulse.app.ui.i18n.t("dns_preset_custom")) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent.copy(alpha = 0.22f),
                                selectedLabelColor = accent
                            )
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    LtrSettingsField(
                        label = com.xraypulse.app.ui.i18n.t("dns_main"),
                        value = draft.dnsRemote,
                        enabled = true,
                        placeholder = "1.1.1.1",
                        onChange = { draft = draft.copy(dnsRemote = it) }
                    )
                    Spacer(Modifier.height(10.dp))
                    LtrSettingsField(
                        label = com.xraypulse.app.ui.i18n.t("dns_alt"),
                        value = draft.dnsDomestic,
                        enabled = true,
                        placeholder = "1.0.0.1",
                        onChange = { draft = draft.copy(dnsDomestic = it) }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        com.xraypulse.app.ui.i18n.t("dns_remote_hint"),
                        color = p.muted,
                        fontSize = 11.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        com.xraypulse.app.ui.i18n.t("ports_restart_hint"),
                        color = p.muted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        ExpandableSection(
            title = com.xraypulse.app.ui.i18n.t("about"),
            expanded = isOpen("about"),
            onToggle = { toggle("about") },
            onHelp = { helpKey = "about" }
        ) {
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("BLA VPN", color = p.text, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "App: v${BuildConfig.VERSION_NAME}".ltrWrap(),
                        color = p.muted,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("Core: $coreVersion".ltrWrap(), color = p.muted, fontSize = 13.sp)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        com.xraypulse.app.ui.i18n.t("update_from_github_hint"),
                        color = p.muted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    GithubUpdateSection()
                }
            }
        }

            Spacer(Modifier.height(24.dp))
        } // end scroll body
    } // end root column
}

@Composable
private fun GithubUpdateSection() {
    val context = LocalContext.current
    val accent = LocalAccent.current
    val p = LocalPalette.current
    val scope = rememberCoroutineScope()
    // Snapshot strings once — t() is @Composable and cannot be used inside callbacks.
    val strings = com.xraypulse.app.ui.i18n.LocalStrings.current
    fun s(key: String): String = strings[key] ?: com.xraypulse.app.ui.i18n.AppStrings.en[key] ?: key

    var checking by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(-1) }
    var status by remember { mutableStateOf("") }
    var statusIsError by remember { mutableStateOf(false) }
    var available by remember { mutableStateOf<GithubReleaseInfo?>(null) }
    var downloadedApk by remember { mutableStateOf<File?>(null) }

    val msgChecking = s("checking_updates")
    val msgUpToDate = s("update_up_to_date")
    val msgAvailable = s("update_available")
    val msgDownloading = s("downloading_update")
    val msgDownloadingUnknown = s("downloading_update_unknown")
    val msgInstall = s("install_update")
    val msgFailed = s("update_failed")
    val msgAllowInstall = s("allow_install_unknown")
    val msgCheck = s("check_for_updates")
    val msgDownloadInstall = s("download_install")
    val msgOpenReleases = s("open_releases")

    fun check() {
        if (checking || downloading) return
        checking = true
        statusIsError = false
        status = msgChecking
        available = null
        downloadedApk = null
        scope.launch {
            val result = withContext(Dispatchers.IO) { AppUpdater.checkLatest() }
            checking = false
            when (result) {
                is UpdateCheckResult.UpToDate -> {
                    status = msgUpToDate
                    statusIsError = false
                }
                is UpdateCheckResult.Available -> {
                    available = result.release
                    status = msgAvailable.withLtrPlaceholders("{v}" to result.release.versionName)
                    statusIsError = false
                }
                is UpdateCheckResult.Error -> {
                    status = result.message
                    statusIsError = true
                }
            }
        }
    }

    fun downloadAndInstall(release: GithubReleaseInfo) {
        if (downloading) return
        if (!AppUpdater.canInstallPackages(context)) {
            status = msgAllowInstall
            statusIsError = true
            AppUpdater.openInstallPermissionSettings(context)
            return
        }
        downloading = true
        progress = 0
        statusIsError = false
        status = msgDownloading.withLtrPlaceholders("{p}" to "0")
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) {
                    AppUpdater.downloadApk(context, release) { pct ->
                        progress = pct
                        status = if (pct >= 0) {
                            msgDownloading.withLtrPlaceholders("{p}" to pct.toString())
                        } else {
                            msgDownloadingUnknown
                        }
                    }
                }
                downloadedApk = file
                downloading = false
                status = msgInstall
                AppUpdater.installApk(context, file)
            } catch (e: Exception) {
                downloading = false
                statusIsError = true
                status = "$msgFailed: ${e.message ?: ""}"
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { check() },
                enabled = !checking && !downloading,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent,
                    contentColor = Color.Black,
                    disabledContainerColor = p.surface2,
                    disabledContentColor = p.muted
                )
            ) {
                Text(
                    if (checking) msgChecking else msgCheck,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
            OutlinedButton(
                onClick = { AppUpdater.openReleasesPage(context) },
                enabled = !downloading,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(msgOpenReleases, fontSize = 12.sp, maxLines = 1)
            }
        }

        if (status.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                status,
                color = if (statusIsError) p.error else p.text,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        val release = available
        if (release != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                "GitHub: ${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}".ltrWrap(),
                color = p.muted,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    val file = downloadedApk
                    if (file != null && file.exists()) {
                        if (!AppUpdater.canInstallPackages(context)) {
                            AppUpdater.openInstallPermissionSettings(context)
                        } else {
                            AppUpdater.installApk(context, file)
                        }
                    } else {
                        downloadAndInstall(release)
                    }
                },
                enabled = !checking && !downloading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent.copy(alpha = 0.9f),
                    contentColor = Color.Black
                )
            ) {
                Text(
                    when {
                        downloading && progress >= 0 ->
                            msgDownloading.withLtrPlaceholders("{p}" to progress.toString())
                        downloading -> msgDownloadingUnknown
                        downloadedApk != null -> msgInstall
                        else -> msgDownloadInstall
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ChipRow(
    options: List<String>,
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    val accent = LocalAccent.current
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { opt ->
            FilterChip(
                selected = selected == opt,
                onClick = { if (enabled) onSelect(opt) },
                enabled = enabled,
                label = { Text(opt, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent.copy(alpha = 0.25f),
                    selectedLabelColor = accent,
                    containerColor = p.surface2,
                    labelColor = p.text
                )
            )
        }
    }
}

@Composable
private fun ColorChipGrid(
    selectedArgb: Long,
    customSelected: Boolean = false,
    onSelect: (Long) -> Unit,
    onCustomClick: () -> Unit = {}
) {
    val accent = LocalAccent.current
    val p = LocalPalette.current
    val chipSize = 40.dp
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ThemeMainColors.chunked(3).forEach { row ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { (argb, name) ->
                    val selected = !customSelected && selectedArgb == argb
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(chipSize)
                                .shadow(
                                    elevation = if (selected) 8.dp else 1.dp,
                                    shape = CircleShape,
                                    ambientColor = argb.toComposeColor().copy(0.4f),
                                    spotColor = argb.toComposeColor().copy(0.3f)
                                )
                                .clip(CircleShape)
                                .background(argb.toComposeColor())
                                .then(
                                    if (selected) {
                                        Modifier.border(2.5.dp, Color.White, CircleShape)
                                    } else {
                                        Modifier.border(
                                            1.dp,
                                            p.border.copy(alpha = 0.5f),
                                            CircleShape
                                        )
                                    }
                                )
                                .clickable { onSelect(argb) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Text(
                                    "✓",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            name,
                            color = if (selected) accent else p.muted,
                            fontSize = 10.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
        // Custom color chip — shows HSV picker when selected
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(chipSize)
                        .shadow(
                            elevation = if (customSelected) 8.dp else 1.dp,
                            shape = CircleShape,
                            ambientColor = accent.copy(0.35f),
                            spotColor = accent.copy(0.25f)
                        )
                        .clip(CircleShape)
                        .background(
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFFFF0040),
                                    Color(0xFFFFEE00),
                                    Color(0xFF00E5FF),
                                    Color(0xFFD500F9),
                                    Color(0xFFFF0040)
                                )
                            )
                        )
                        .then(
                            if (customSelected) {
                                Modifier.border(2.5.dp, Color.White, CircleShape)
                            } else {
                                Modifier.border(1.dp, p.border.copy(0.5f), CircleShape)
                            }
                        )
                        .clickable(onClick = onCustomClick),
                    contentAlignment = Alignment.Center
                ) {
                    if (customSelected) {
                        Text(
                            "✓",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    com.xraypulse.app.ui.i18n.t("dns_preset_custom"),
                    color = if (customSelected) accent else p.muted,
                    fontSize = 10.sp,
                    fontWeight = if (customSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun LtrSettingsField(
    label: String,
    value: String,
    enabled: Boolean,
    placeholder: String,
    onChange: (String) -> Unit
) {
    val accent = LocalAccent.current
    val p = LocalPalette.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder, color = p.muted) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            textStyle = TextStyle(
                color = p.text,
                textDirection = TextDirection.Ltr,
                fontFamily = FontFamily.Monospace
            ),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accent,
                unfocusedBorderColor = p.border,
                focusedTextColor = p.text,
                unfocusedTextColor = p.text,
                disabledTextColor = p.muted,
                cursorColor = accent,
                focusedContainerColor = p.surface2,
                unfocusedContainerColor = p.surface2,
                disabledContainerColor = p.surface2.copy(alpha = 0.5f),
                focusedLabelColor = p.muted,
                unfocusedLabelColor = p.muted
            )
        )
    }
}

@Composable
private fun SectionTitle(text: String, onHelp: (() -> Unit)? = null) {
    val p = LocalPalette.current
    val accent = LocalAccent.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp, start = 4.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text,
            color = p.muted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (onHelp != null) {
            Text(
                "?",
                color = accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(accent.copy(0.15f))
                    .clickable(onClick = onHelp)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun SettingsHelpScreen(
    titleKey: String,
    bodyKey: String,
    onClose: () -> Unit
) {
    val p = LocalPalette.current
    val accent = LocalAccent.current
    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            com.xraypulse.app.ui.i18n.t(titleKey),
            color = p.text,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(6.dp))
        Text(com.xraypulse.app.ui.i18n.t("help"), color = p.muted, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                com.xraypulse.app.ui.i18n.t(bodyKey),
                color = p.text,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                modifier = Modifier.padding(18.dp)
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onClose,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
        ) {
            Text(com.xraypulse.app.ui.i18n.t("got_it"), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onHelp: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val accent = LocalAccent.current
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onToggle)
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                title,
                color = p.text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            if (onHelp != null) {
                Text(
                    "?",
                    color = accent,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(accent.copy(0.15f))
                        .clickable(onClick = onHelp)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
                Spacer(Modifier.size(8.dp))
            }
            Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = p.muted
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            content()
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun SwitchRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val accent = LocalAccent.current
    val p = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = p.text, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = accent,
                checkedTrackColor = accent.copy(0.35f)
            )
        )
    }
}

