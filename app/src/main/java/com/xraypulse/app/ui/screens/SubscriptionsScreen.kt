package com.xraypulse.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xraypulse.app.data.model.Subscription
import com.xraypulse.app.ui.components.GlassCard
import com.xraypulse.app.ui.components.NeonIcon
import com.xraypulse.app.ui.components.ltrWrap
import com.xraypulse.app.ui.i18n.t
import com.xraypulse.app.ui.theme.LocalAccent
import com.xraypulse.app.ui.theme.LocalPalette
import com.xraypulse.app.util.formatBytes
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

@Composable
fun SubscriptionsScreen(
    subscriptions: List<Subscription>,
    onRefresh: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onRename: (Long, String) -> Unit = { _, _ -> },
    onUpdate: (Long, String, String) -> Unit = { _, _, _ -> }
) {
    val p = LocalPalette.current
    val accent = LocalAccent.current
    var editTarget by remember { mutableStateOf<Subscription?>(null) }
    var editName by remember { mutableStateOf("") }
    var editUrl by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(p.bg)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(t("subscriptions"), color = p.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("${subscriptions.size} ${t("sources")}", color = p.muted, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))

        if (subscriptions.isEmpty()) {
            Text(t("no_subscriptions"), color = p.muted)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(subscriptions, key = { it.id }) { sub ->
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(sub.name, color = p.text, fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(4.dp))
                                    Text(sub.url, color = p.muted, fontSize = 12.sp, maxLines = 1)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${sub.serverCount} ${t("servers")} · ${formatTime(sub.lastUpdated)}".ltrWrap(),
                                        color = p.muted,
                                        fontSize = 11.sp
                                    )
                                }
                                IconButton(onClick = {
                                    editTarget = sub
                                    editName = sub.name
                                    editUrl = sub.url
                                }) {
                                    NeonIcon(Icons.Rounded.Edit, t("edit_subscription"))
                                }
                                IconButton(onClick = { onRefresh(sub.id) }) {
                                    NeonIcon(Icons.Rounded.Refresh, t("refresh"))
                                }
                                IconButton(onClick = { onDelete(sub.id) }) {
                                    NeonIcon(Icons.Rounded.Delete, t("delete"), tintOverride = p.error)
                                }
                            }

                            val used = sub.usedTraffic
                            val total = sub.totalTraffic
                            val hasTraffic = total > 0 || used > 0
                            val hasExpire = sub.expireAt > 0
                            if (hasTraffic || hasExpire) {
                                Spacer(Modifier.height(12.dp))
                                if (hasTraffic) {
                                    val prog = if (total > 0)
                                        min(1f, used.toFloat() / total.toFloat()) else 0f
                                    SubBar(
                                        label = t("traffic"),
                                        value = if (total > 0)
                                            "${used.formatBytes()} / ${total.formatBytes()}"
                                        else
                                            "${used.formatBytes()} ${t("used")}",
                                        progress = prog,
                                        indeterminate = total <= 0,
                                        color = when {
                                            prog >= 0.95f -> p.error
                                            prog >= 0.80f -> p.warning
                                            else -> accent
                                        }
                                    )
                                    Spacer(Modifier.height(8.dp))
                                }
                                if (hasExpire) {
                                    val rem = sub.timeRemainingProgress()
                                    val usedFrac = if (rem < 0f) 0f else (1f - rem).coerceIn(0f, 1f)
                                    SubBar(
                                        label = t("expire"),
                                        value = sub.expireLabel().ifBlank { "—" },
                                        progress = usedFrac,
                                        indeterminate = rem < 0f,
                                        color = when {
                                            usedFrac >= 0.95f -> p.error
                                            usedFrac >= 0.80f -> p.warning
                                            else -> p.success
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editTarget?.let { sub ->
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text(t("edit_subscription")) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        singleLine = true,
                        label = { Text(t("name")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = p.border,
                            focusedTextColor = p.text,
                            unfocusedTextColor = p.text,
                            cursorColor = accent
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editUrl,
                        onValueChange = { editUrl = it },
                        singleLine = false,
                        maxLines = 4,
                        label = { Text(t("subscription_url")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = p.border,
                            focusedTextColor = p.text,
                            unfocusedTextColor = p.text,
                            cursorColor = accent
                        )
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        t("edit_subscription_hint"),
                        color = p.muted,
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editName.isNotBlank() && editUrl.isNotBlank()) {
                            onUpdate(sub.id, editName, editUrl)
                        }
                        editTarget = null
                    },
                    enabled = editName.isNotBlank() && editUrl.isNotBlank()
                ) { Text(t("ok"), color = accent) }
            },
            dismissButton = {
                TextButton(onClick = { editTarget = null }) { Text(t("cancel")) }
            }
        )
    }
}

@Composable
private fun SubBar(
    label: String,
    value: String,
    progress: Float,
    indeterminate: Boolean,
    color: androidx.compose.ui.graphics.Color
) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = p.muted, fontSize = 11.sp)
            Text(value, color = p.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(4.dp))
        if (indeterminate) {
            androidx.compose.foundation.layout.Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(p.surface2)
            )
        } else {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = color,
                trackColor = p.surface2,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "—"
    return SimpleDateFormat("MMM d HH:mm", Locale.getDefault()).format(Date(ms))
}
