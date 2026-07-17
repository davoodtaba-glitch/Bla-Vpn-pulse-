package com.xraypulse.app.ui.screens

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
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
import com.xraypulse.app.core.config.XrayConfigBuilder
import com.xraypulse.app.data.model.AppSettings
import com.xraypulse.app.data.model.FragmentPresets
import com.xraypulse.app.data.model.RoutingMode
import com.xraypulse.app.data.model.SessionLimitPresets
import com.xraypulse.app.ui.components.GlassCard
import com.xraypulse.app.ui.theme.AppThemeStyle
import com.xraypulse.app.ui.theme.LocalAccent
import com.xraypulse.app.ui.theme.LocalPalette
import com.xraypulse.app.ui.theme.ThemeAccentPresets
import com.xraypulse.app.ui.theme.toAppThemeStyle
import com.xraypulse.app.ui.theme.toComposeColor
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: AppSettings,
    coreVersion: String,
    onApply: (AppSettings) -> Unit,
    onApplyAppearance: (themeStyle: String, accentColor: Long) -> Unit = { _, _ -> },
    onApplyLanguage: (String) -> Unit = {},
    onOpenPerApp: () -> Unit = {}
) {
    val accent = LocalAccent.current
    val p = LocalPalette.current
    // Draft copy — edits stay local until Apply (fixes reverse typing + accidental apply)
    var draft by remember { mutableStateOf(settings) }
    var helpKey by remember { mutableStateOf<String?>(null) }

    // Sync draft when saved settings change from outside (and no dirty edits)
    LaunchedEffect(settings) {
        draft = settings
    }

    val dirty = draft != settings
    val scroll = rememberScrollState()

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
            .verticalScroll(scroll)
            .padding(16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(com.xraypulse.app.ui.i18n.t("settings"), color = p.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            if (dirty) com.xraypulse.app.ui.i18n.t("unsaved_changes") else com.xraypulse.app.ui.i18n.t("settings_subtitle"),
            color = if (dirty) p.warning else p.muted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(16.dp))

        // Sticky-style apply bar
        if (dirty) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { draft = settings },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(com.xraypulse.app.ui.i18n.t("discard"))
                }
                Button(
                    onClick = { onApply(draft) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
                ) {
                    Text(com.xraypulse.app.ui.i18n.t("apply_changes"), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        SectionTitle(com.xraypulse.app.ui.i18n.t("routing_mode"), onHelp = { helpKey = "routing" })
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

        Spacer(Modifier.height(16.dp))
        SectionTitle(com.xraypulse.app.ui.i18n.t("core_options"), onHelp = { helpKey = "core" })
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

        Spacer(Modifier.height(16.dp))
        SectionTitle(com.xraypulse.app.ui.i18n.t("bypass_domains"), onHelp = { helpKey = "bypass" })
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    com.xraypulse.app.ui.i18n.t("bypass_domains_hint"),
                    color = p.muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(10.dp))
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
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle(com.xraypulse.app.ui.i18n.t("session_limits"), onHelp = { helpKey = "limits" })
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    com.xraypulse.app.ui.i18n.t("limit_action_hint"),
                    color = p.muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
                Spacer(Modifier.height(10.dp))
                Text(com.xraypulse.app.ui.i18n.t("limit_action"), color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = draft.limitActionOnReach != "disconnect",
                        onClick = { draft = draft.copy(limitActionOnReach = "notify") },
                        label = { Text(com.xraypulse.app.ui.i18n.t("limit_action_notify"), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(0.25f),
                            selectedLabelColor = accent,
                            containerColor = p.surface2,
                            labelColor = p.text
                        )
                    )
                    FilterChip(
                        selected = draft.limitActionOnReach == "disconnect",
                        onClick = { draft = draft.copy(limitActionOnReach = "disconnect") },
                        label = { Text(com.xraypulse.app.ui.i18n.t("limit_action_disconnect"), fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent.copy(0.25f),
                            selectedLabelColor = accent,
                            containerColor = p.surface2,
                            labelColor = p.text
                        )
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(com.xraypulse.app.ui.i18n.t("time_limit"), color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                ChipRow(
                    options = SessionLimitPresets.TIME_MINUTES.map { formatTimeLimitChip(it) },
                    selected = formatTimeLimitChip(draft.sessionTimeLimitMinutes),
                    enabled = true,
                    onSelect = { label ->
                        draft = draft.copy(sessionTimeLimitMinutes = parseTimeLimitChip(label))
                    }
                )
                Spacer(Modifier.height(8.dp))
                LtrSettingsField(
                    label = "Custom minutes (0 = off)",
                    value = draft.sessionTimeLimitMinutes.toString(),
                    enabled = true,
                    placeholder = "60",
                    onChange = { v ->
                        val n = v.filter { it.isDigit() }.toIntOrNull() ?: 0
                        draft = draft.copy(sessionTimeLimitMinutes = n.coerceIn(0, 24 * 60))
                    }
                )

                Spacer(Modifier.height(14.dp))
                Text(com.xraypulse.app.ui.i18n.t("traffic_limit"), color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                ChipRow(
                    options = SessionLimitPresets.TRAFFIC_MB.map { formatTrafficLimitChip(it) },
                    selected = formatTrafficLimitChip(draft.sessionTrafficLimitMb),
                    enabled = true,
                    onSelect = { label ->
                        draft = draft.copy(sessionTrafficLimitMb = parseTrafficLimitChip(label))
                    }
                )
                Spacer(Modifier.height(8.dp))
                LtrSettingsField(
                    label = "Custom MB (0 = off)",
                    value = draft.sessionTrafficLimitMb.toString(),
                    enabled = true,
                    placeholder = "512",
                    onChange = { v ->
                        val n = v.filter { it.isDigit() }.toIntOrNull() ?: 0
                        draft = draft.copy(sessionTrafficLimitMb = n.coerceIn(0, 100_000))
                    }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Current: time=${formatTimeLimitChip(draft.sessionTimeLimitMinutes)} · traffic=${formatTrafficLimitChip(draft.sessionTrafficLimitMb)}",
                    color = accent,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle(com.xraypulse.app.ui.i18n.t("appearance"), onHelp = { helpKey = "appearance" })
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    com.xraypulse.app.ui.i18n.t("help_appearance_body").lines().firstOrNull().orEmpty(),
                    color = p.muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
                Text(com.xraypulse.app.ui.i18n.t("theme_frame_color"), color = p.text, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ThemeAccentPresets.forEach { (argb, name) ->
                        val selected = settings.accentColor == argb
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(argb.toComposeColor())
                                    .then(
                                        if (selected) Modifier.border(3.dp, Color.White, CircleShape)
                                        else Modifier.border(1.dp, p.border, CircleShape)
                                    )
                                    .clickable {
                                        draft = draft.copy(themeStyle = "PULSE", accentColor = argb)
                                        onApplyAppearance("PULSE", argb)
                                    }
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(name, color = p.muted, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(com.xraypulse.app.ui.i18n.t("custom_color"), color = p.text, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(com.xraypulse.app.ui.i18n.t("custom_color_hint"), color = p.muted, fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))
                CustomColorPicker(
                    colorArgb = settings.accentColor,
                    onColorChange = { argb ->
                        draft = draft.copy(themeStyle = "PULSE", accentColor = argb)
                        onApplyAppearance("PULSE", argb)
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle(com.xraypulse.app.ui.i18n.t("language"), onHelp = { helpKey = "language" })
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    com.xraypulse.app.ui.i18n.t("ui_language_hint"),
                    color = p.muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
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

        Spacer(Modifier.height(16.dp))
        SectionTitle(com.xraypulse.app.ui.i18n.t("per_app_proxy"), onHelp = { helpKey = "perapp" })
        GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenPerApp) {
            Column(Modifier.padding(16.dp)) {
                Text(com.xraypulse.app.ui.i18n.t("configure_apps"), color = p.text, fontWeight = FontWeight.SemiBold)
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
                Text(com.xraypulse.app.ui.i18n.t("per_app_saved"), color = p.muted, fontSize = 11.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle(com.xraypulse.app.ui.i18n.t("tls_fragment"), onHelp = { helpKey = "fragment" })
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
                    "packets=${draft.fragmentPackets} · length=${draft.fragmentLength} · interval=${draft.fragmentInterval}" +
                        if (draft.fragmentMaxSplit.isNotBlank()) " · maxSplit=${draft.fragmentMaxSplit}" else "",
                    color = if (draft.fragmentEnabled) accent else p.muted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle(com.xraypulse.app.ui.i18n.t("local_ports"), onHelp = { helpKey = "ports" })
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    com.xraypulse.app.ui.i18n.t("ports_edit_hint"),
                    color = p.muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(12.dp))
                LtrSettingsField(
                    label = "SOCKS port",
                    value = draft.localSocksPort.toString(),
                    enabled = true,
                    placeholder = "10808",
                    onChange = { v ->
                        val n = v.filter { it.isDigit() }.toIntOrNull() ?: 0
                        if (n in 1..65535) draft = draft.copy(localSocksPort = n)
                        else if (v.isEmpty()) draft = draft.copy(localSocksPort = 10808)
                    }
                )
                Spacer(Modifier.height(10.dp))
                LtrSettingsField(
                    label = "HTTP port",
                    value = draft.localHttpPort.toString(),
                    enabled = true,
                    placeholder = "10809",
                    onChange = { v ->
                        val n = v.filter { it.isDigit() }.toIntOrNull() ?: 0
                        if (n in 1..65535) draft = draft.copy(localHttpPort = n)
                        else if (v.isEmpty()) draft = draft.copy(localHttpPort = 10809)
                    }
                )
                Spacer(Modifier.height(10.dp))
                LtrSettingsField(
                    label = "DNS remote",
                    value = draft.dnsRemote,
                    enabled = true,
                    placeholder = "https://1.1.1.1/dns-query",
                    onChange = { draft = draft.copy(dnsRemote = it) }
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    com.xraypulse.app.ui.i18n.t("ports_restart_hint"),
                    color = p.muted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionTitle(com.xraypulse.app.ui.i18n.t("about"), onHelp = { helpKey = "about" })
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("BLA VPN", color = p.text, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("App: v${com.xraypulse.app.BuildConfig.VERSION_NAME}", color = p.muted, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text("Core: $coreVersion", color = p.muted, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { onApply(draft) },
            enabled = dirty,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accent,
                contentColor = Color.Black,
                disabledContainerColor = p.surface2,
                disabledContentColor = p.muted
            )
        ) {
            Text(
                if (dirty) "Apply changes" else "No changes to apply",
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(32.dp))
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

private fun formatTimeLimitChip(min: Int): String = when {
    min <= 0 -> "Off"
    min < 60 -> "${min}m"
    min % 60 == 0 -> "${min / 60}h"
    else -> "${min / 60}h${min % 60}m"
}

private fun parseTimeLimitChip(label: String): Int = when {
    label.equals("Off", true) -> 0
    label.endsWith("h") && !label.contains("m") ->
        label.removeSuffix("h").toIntOrNull()?.times(60) ?: 0
    label.contains("h") && label.contains("m") -> {
        val h = label.substringBefore("h").toIntOrNull() ?: 0
        val m = label.substringAfter("h").removeSuffix("m").toIntOrNull() ?: 0
        h * 60 + m
    }
    label.endsWith("m") -> label.removeSuffix("m").toIntOrNull() ?: 0
    else -> label.filter { it.isDigit() }.toIntOrNull() ?: 0
}

private fun formatTrafficLimitChip(mb: Int): String = when {
    mb <= 0 -> "Off"
    mb >= 1024 && mb % 1024 == 0 -> "${mb / 1024} GB"
    else -> "$mb MB"
}

private fun parseTrafficLimitChip(label: String): Int = when {
    label.equals("Off", true) -> 0
    label.contains("GB", ignoreCase = true) ->
        (label.filter { it.isDigit() }.toIntOrNull() ?: 0) * 1024
    else -> label.filter { it.isDigit() }.toIntOrNull() ?: 0
}

@Composable
private fun CustomColorPicker(
    colorArgb: Long,
    onColorChange: (Long) -> Unit
) {
    val accent = LocalAccent.current
    val p = LocalPalette.current
    val c = colorArgb.toComposeColor()
    var r by remember(colorArgb) { mutableFloatStateOf(c.red) }
    var g by remember(colorArgb) { mutableFloatStateOf(c.green) }
    var b by remember(colorArgb) { mutableFloatStateOf(c.blue) }

    fun push(nr: Float, ng: Float, nb: Float) {
        r = nr; g = ng; b = nb
        val color = Color(nr, ng, nb, 1f)
        val argb = (0xFF000000L or (color.toArgb().toLong() and 0xFFFFFFL))
        onColorChange(argb)
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(r, g, b))
                    .border(2.dp, accent.copy(0.6f), RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.size(12.dp))
            Text(
                "R ${(r * 255).roundToInt()}  G ${(g * 255).roundToInt()}  B ${(b * 255).roundToInt()}",
                color = p.muted,
                fontSize = 12.sp
            )
        }
        Spacer(Modifier.height(8.dp))
        ColorSlider("Red", r, Color(0xFFFF5555), accent) { push(it, g, b) }
        ColorSlider("Green", g, Color(0xFF55FF55), accent) { push(r, it, b) }
        ColorSlider("Blue", b, Color(0xFF5599FF), accent) { push(r, g, it) }
    }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    activeTrack: Color,
    thumb: Color,
    onChange: (Float) -> Unit
) {
    val p = LocalPalette.current
    Column(Modifier.padding(vertical = 2.dp)) {
        Text(label, color = p.muted, fontSize = 11.sp)
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = thumb,
                activeTrackColor = activeTrack,
                inactiveTrackColor = p.surface2
            )
        )
    }
}
