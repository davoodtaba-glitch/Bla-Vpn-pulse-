package com.xraypulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xraypulse.app.ui.components.GlassCard
import com.xraypulse.app.ui.components.NeonIcon
import com.xraypulse.app.ui.i18n.AppStrings
import com.xraypulse.app.ui.i18n.LocalStrings
import com.xraypulse.app.ui.i18n.t
import com.xraypulse.app.ui.theme.LocalAccent
import com.xraypulse.app.ui.theme.LocalPalette

/**
 * Unified import: one box for config share-links and/or subscription URLs.
 * Multi-line paste supported. Subscriptions prompt for a name.
 */
@Composable
fun ImportScreen(
    busy: Boolean,
    onImportMixed: (
        configsText: String,
        subscriptions: List<Pair<String, String>>, // name to url
        onDone: (String) -> Unit
    ) -> Unit,
    onScanQr: () -> Unit,
    onManual: () -> Unit,
    onOpenSubscriptions: () -> Unit = {}
) {
    var paste by remember { mutableStateOf("") }
    var pendingSubs by remember { mutableStateOf<List<String>>(emptyList()) }
    var subNames by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var showNameDialog by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val accent = LocalAccent.current
    val p = LocalPalette.current
    val strings = LocalStrings.current
    fun tr(key: String): String = strings[key] ?: AppStrings.en[key] ?: key

    fun classifyAndImport() {
        val (subs, configs) = splitImportInput(paste)
        if (subs.isEmpty() && configs.isBlank()) return
        if (subs.isNotEmpty()) {
            pendingSubs = subs
            subNames = subs.indices.associateWith { i ->
                tr("subscription") + " ${i + 1}"
            }
            showNameDialog = true
        } else {
            onImportMixed(configs, emptyList()) { }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(t("import"), color = p.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(
            t("import_page_subtitle"),
            color = p.muted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(20.dp))

        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(t("import_links"), color = p.text, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    t("import_links_hint"),
                    color = p.muted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = paste,
                    onValueChange = { paste = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    placeholder = {
                        Text(
                            t("import_paste_placeholder"),
                            color = p.muted
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = p.border,
                        focusedTextColor = p.text,
                        unfocusedTextColor = p.text,
                        cursorColor = accent,
                        focusedContainerColor = p.surface2,
                        unfocusedContainerColor = p.surface2
                    ),
                    shape = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = {
                            clipboard.getText()?.text?.let { paste = it }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        NeonIcon(Icons.Rounded.ContentPaste, null, size = 20.dp)
                        Spacer(Modifier.size(6.dp))
                        Text(t("paste"))
                    }
                    Button(
                        onClick = { classifyAndImport() },
                        enabled = paste.isNotBlank() && !busy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.Black
                        )
                    ) {
                        NeonIcon(Icons.Rounded.Link, null, size = 20.dp, tintOverride = Color.Black)
                        Spacer(Modifier.size(6.dp))
                        Text(t("import_btn"))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(
                onClick = onScanQr,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                NeonIcon(Icons.Rounded.QrCodeScanner, null, size = 22.dp)
                Spacer(Modifier.size(6.dp))
                Text(t("scan_qr"))
            }
            OutlinedButton(
                onClick = onManual,
                modifier = Modifier.weight(1f).height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(t("manual_vless"))
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpenSubscriptions,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(t("manage_subscriptions"))
        }

        Spacer(Modifier.height(24.dp))
        Text(
            t("import_tip"),
            color = p.muted,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )
    }

    if (showNameDialog && pendingSubs.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(t("name_subscription")) },
            text = {
                Column {
                    Text(
                        t("import_detected_subs").replace("{n}", pendingSubs.size.toString()),
                        color = p.muted,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    pendingSubs.forEachIndexed { i, url ->
                        Text(
                            url.take(48) + if (url.length > 48) "…" else "",
                            color = p.muted,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = subNames[i].orEmpty(),
                            onValueChange = { v -> subNames = subNames + (i to v) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(t("name")) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accent,
                                unfocusedBorderColor = p.border,
                                focusedTextColor = p.text,
                                unfocusedTextColor = p.text,
                                cursorColor = accent
                            )
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showNameDialog = false
                    val (_, configs) = splitImportInput(paste)
                    val pairs = pendingSubs.mapIndexed { i, url ->
                        val fallback = tr("subscription") + " ${i + 1}"
                        (subNames[i]?.ifBlank { fallback } ?: fallback) to url
                    }
                    pendingSubs = emptyList()
                    onImportMixed(configs, pairs) { }
                }) { Text(t("import_all")) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNameDialog = false
                    pendingSubs = emptyList()
                }) { Text(t("cancel")) }
            }
        )
    }
}

/**
 * Split paste into subscription URLs (http/https lines without share schemes)
 * and remaining config text.
 */
fun splitImportInput(raw: String): Pair<List<String>, String> {
    val subs = mutableListOf<String>()
    val configLines = mutableListOf<String>()
    raw.lines().forEach { line ->
        val t = line.trim()
        if (t.isEmpty()) return@forEach
        if (isSubscriptionLine(t)) {
            subs += t
        } else {
            configLines += t
        }
    }
    val whole = raw.trim()
    if (subs.isEmpty() && configLines.isEmpty() && isSubscriptionLine(whole)) {
        return listOf(whole) to ""
    }
    return subs to configLines.joinToString("\n")
}

fun isSubscriptionLine(text: String): Boolean {
    val t = text.trim().lowercase()
    if (!(t.startsWith("http://") || t.startsWith("https://"))) return false
    if (t.contains("vless://") || t.contains("vmess://") || t.contains("trojan://")) return false
    return true
}
