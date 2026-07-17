package com.xraypulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xraypulse.app.ui.components.GlassCard
import com.xraypulse.app.ui.i18n.AppStrings
import com.xraypulse.app.ui.i18n.LocalStrings
import com.xraypulse.app.ui.i18n.t
import com.xraypulse.app.ui.theme.LocalAccent
import com.xraypulse.app.ui.theme.LocalPalette

private enum class QsStep {
    WHAT,
    HAVE_CONFIG,
    NO_CONFIG_HELP,
    HAVE_COPIED,
    COPY_HINT,
    PASTE,
    DONE,
    ERROR,
    HELP_COLOR,
    HELP_LIMITS,
    HELP_LANGUAGE,
    HELP_SETTINGS
}

@Composable
fun QuickSetupScreen(
    busy: Boolean = false,
    onBack: () -> Unit,
    onOpenSettingsAppearance: () -> Unit,
    onOpenSettingsLimits: () -> Unit,
    onOpenSettingsLanguage: () -> Unit,
    onOpenFullSettings: () -> Unit,
    onImportAndConnect: (
        text: String,
        onResult: (ok: Boolean, messageKey: String) -> Unit
    ) -> Unit
) {
    val accent = LocalAccent.current
    val p = LocalPalette.current
    val clipboard = LocalClipboardManager.current
    val strings = LocalStrings.current
    fun tr(key: String): String = strings[key] ?: AppStrings.en[key] ?: key

    var step by remember { mutableStateOf(QsStep.WHAT) }
    var pasteText by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                when (step) {
                    QsStep.WHAT -> onBack()
                    QsStep.HAVE_CONFIG, QsStep.NO_CONFIG_HELP -> step = QsStep.WHAT
                    QsStep.HAVE_COPIED, QsStep.COPY_HINT -> step = QsStep.HAVE_CONFIG
                    QsStep.PASTE -> step = QsStep.HAVE_COPIED
                    QsStep.DONE, QsStep.ERROR -> step = QsStep.WHAT
                    QsStep.HELP_COLOR, QsStep.HELP_LIMITS,
                    QsStep.HELP_LANGUAGE, QsStep.HELP_SETTINGS -> step = QsStep.WHAT
                }
            }) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = t("qs_back"), tint = p.text)
            }
            Text(
                t("qs_title"),
                color = p.text,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))

        when (step) {
            QsStep.WHAT -> {
                Text(t("qs_what"), color = p.muted, fontSize = 15.sp)
                Spacer(Modifier.height(16.dp))
                QsOption(Icons.Rounded.Upload, t("qs_import")) {
                    step = QsStep.HAVE_CONFIG
                }
                QsOption(Icons.Rounded.Palette, t("qs_color")) {
                    step = QsStep.HELP_COLOR
                }
                QsOption(Icons.Rounded.Timer, t("qs_limits")) {
                    step = QsStep.HELP_LIMITS
                }
                QsOption(Icons.Rounded.Language, t("qs_language")) {
                    step = QsStep.HELP_LANGUAGE
                }
                QsOption(Icons.Rounded.Settings, t("qs_settings")) {
                    step = QsStep.HELP_SETTINGS
                }
            }

            QsStep.HELP_COLOR -> QsHelpCard(
                title = t("qs_help_color_title"),
                body = t("qs_help_color_body"),
                onContinue = onOpenSettingsAppearance
            )
            QsStep.HELP_LIMITS -> QsHelpCard(
                title = t("qs_help_limits_title"),
                body = t("qs_help_limits_body"),
                onContinue = onOpenSettingsLimits
            )
            QsStep.HELP_LANGUAGE -> QsHelpCard(
                title = t("qs_help_language_title"),
                body = t("qs_help_language_body"),
                onContinue = onOpenSettingsLanguage
            )
            QsStep.HELP_SETTINGS -> QsHelpCard(
                title = t("qs_help_settings_title"),
                body = t("qs_help_settings_body"),
                onContinue = onOpenFullSettings
            )

            QsStep.HAVE_CONFIG -> {
                Text(t("qs_have_config"), color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                QsYesNo(
                    onYes = { step = QsStep.HAVE_COPIED },
                    onNo = { step = QsStep.NO_CONFIG_HELP }
                )
            }

            QsStep.NO_CONFIG_HELP -> {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Icon(Icons.Rounded.Info, null, tint = accent, modifier = Modifier.padding(bottom = 8.dp))
                        Text(
                            t("qs_no_config_msg"),
                            color = p.text,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
                        ) {
                            Text(t("ok"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            QsStep.HAVE_COPIED -> {
                Text(t("qs_have_copied"), color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                QsYesNo(
                    onYes = { step = QsStep.PASTE },
                    onNo = { step = QsStep.COPY_HINT }
                )
            }

            QsStep.COPY_HINT -> {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Icon(Icons.Rounded.ContentPaste, null, tint = accent, modifier = Modifier.padding(bottom = 8.dp))
                        Text(
                            t("qs_copied_hint"),
                            color = p.text,
                            fontSize = 14.sp,
                            lineHeight = 22.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            t("qs_no_config_msg"),
                            color = p.muted,
                            fontSize = 13.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { step = QsStep.HAVE_COPIED },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
                        ) {
                            Text(t("qs_continue"), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(t("ok"))
                        }
                    }
                }
            }

            QsStep.PASTE -> {
                Text(t("qs_paste_title"), color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = pasteText,
                    onValueChange = { pasteText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    placeholder = { Text("vless://… or https://…/sub", color = p.muted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = p.text,
                        unfocusedTextColor = p.text,
                        focusedBorderColor = accent,
                        unfocusedBorderColor = p.border,
                        cursorColor = accent,
                        focusedContainerColor = p.surface,
                        unfocusedContainerColor = p.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clip = clipboard.getText()?.text.orEmpty()
                            if (clip.isNotBlank()) pasteText = clip
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.ContentPaste, null, modifier = Modifier.padding(end = 6.dp))
                        Text(t("qs_paste"))
                    }
                    Button(
                        onClick = {
                            if (pasteText.isBlank()) {
                                statusMsg = tr("qs_empty")
                                step = QsStep.ERROR
                                return@Button
                            }
                            onImportAndConnect(pasteText) { ok, key ->
                                statusMsg = tr(key)
                                step = if (ok) QsStep.DONE else QsStep.ERROR
                            }
                        },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(end = 4.dp),
                                strokeWidth = 2.dp,
                                color = Color.Black
                            )
                        }
                        Text(t("qs_continue"), fontWeight = FontWeight.Bold)
                    }
                }
            }

            QsStep.DONE -> {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(20.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = p.success, modifier = Modifier.padding(bottom = 12.dp))
                        Text(
                            statusMsg.ifBlank { t("qs_done_config") },
                            color = p.text,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onBack,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
                        ) {
                            Text(t("ok"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            QsStep.ERROR -> {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            statusMsg.ifBlank { t("qs_invalid") },
                            color = p.error,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { step = QsStep.PASTE },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
                        ) {
                            Text(t("qs_back"), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QsOption(icon: ImageVector, label: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    val accent = LocalAccent.current
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        onClick = onClick
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = accent)
            Spacer(Modifier.padding(horizontal = 8.dp))
            Text(label, color = p.text, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun QsYesNo(onYes: () -> Unit, onNo: () -> Unit) {
    val accent = LocalAccent.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onYes,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
        ) {
            Text(t("qs_yes"), fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onNo,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(t("qs_no"))
        }
    }
}

@Composable
private fun QsHelpCard(title: String, body: String, onContinue: () -> Unit) {
    val accent = LocalAccent.current
    val p = LocalPalette.current
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Icon(Icons.Rounded.Info, null, tint = accent, modifier = Modifier.padding(bottom = 8.dp))
            Text(title, color = p.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            Text(body, color = p.text, fontSize = 14.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black)
            ) {
                Text(t("qs_continue"), fontWeight = FontWeight.Bold)
            }
        }
    }
}
