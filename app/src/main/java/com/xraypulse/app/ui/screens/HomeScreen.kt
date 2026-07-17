package com.xraypulse.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xraypulse.app.BuildConfig
import com.xraypulse.app.data.model.AppSettings
import com.xraypulse.app.data.model.ConnectionState
import com.xraypulse.app.data.model.ServerProfile
import com.xraypulse.app.data.model.Subscription
import com.xraypulse.app.ui.components.GlassCard
import com.xraypulse.app.ui.components.NeonIcon
import com.xraypulse.app.ui.components.PowerButton
import com.xraypulse.app.ui.i18n.t
import com.xraypulse.app.ui.theme.LocalAccent
import com.xraypulse.app.ui.theme.LocalPalette
import com.xraypulse.app.util.formatBytes
import com.xraypulse.app.util.formatDuration
import com.xraypulse.app.util.formatSpeed
import kotlinx.coroutines.delay
import kotlin.math.min

@Composable
fun HomeScreen(
    connection: ConnectionState,
    selected: ServerProfile?,
    activeSubscription: Subscription? = null,
    settings: AppSettings = AppSettings(),
    isTesting: Boolean = false,
    onToggle: () -> Unit,
    onOpenServers: () -> Unit,
    onQuickSetup: () -> Unit = {},
    onQuickTest: () -> Unit = {}
) {
    val accent = LocalAccent.current
    val p = LocalPalette.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(connection.isConnected) {
        while (connection.isConnected) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val elapsed = if (connection.isConnected && connection.startTimeMs > 0)
        now - connection.startTimeMs else 0L

    val statusText = when {
        connection.isConnecting -> t("connecting")
        connection.isConnected && connection.noTraffic -> t("no_data")
        connection.isConnected && connection.limitWarningLevel >= 2 -> t("limit_almost_up")
        connection.isConnected && connection.limitWarningLevel >= 1 -> t("high_usage")
        connection.isConnected -> t("protected")
        else -> t("disconnected")
    }
    val statusColor = when {
        connection.isConnecting -> p.warning
        connection.isConnected && connection.noTraffic -> p.error
        connection.isConnected && connection.limitWarningLevel >= 2 -> p.error
        connection.isConnected && connection.limitWarningLevel >= 1 -> p.warning
        connection.isConnected -> p.success
        else -> p.muted
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(p.bg, p.bg, accent.copy(alpha = 0.08f), p.bg)
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        "BLA VPN", // brand — never translated
                        color = accent,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "v${BuildConfig.VERSION_NAME}",
                        color = p.muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Shield, null, tint = statusColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(statusText, color = statusColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            // Limit warning banner
            AnimatedVisibility(
                visible = connection.isConnected && !connection.limitWarning.isNullOrBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val warnColor = if (connection.limitWarningLevel >= 2) p.error else p.warning
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(warnColor.copy(alpha = 0.15f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Warning, null, tint = warnColor, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(10.dp))
                    Text(
                        connection.limitWarning.orEmpty(),
                        color = warnColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            PowerButton(
                connected = connection.isConnected,
                connecting = connection.isConnecting,
                noTraffic = connection.noTraffic,
                onClick = onToggle
            )

            Text(
                when {
                    connection.isConnecting -> t("establishing_tunnel")
                    connection.isConnected && connection.noTraffic -> t("connected_no_traffic")
                    connection.isConnected -> t("tap_disconnect")
                    else -> t("tap_connect")
                },
                color = when {
                    connection.isConnected && connection.noTraffic -> p.error
                    connection.isConnected -> accent
                    else -> p.muted
                },
                fontSize = 14.sp,
                fontWeight = if (connection.isConnected) FontWeight.SemiBold else FontWeight.Normal
            )

            AnimatedVisibility(visible = connection.errorMessage != null, enter = fadeIn(), exit = fadeOut()) {
                Text(
                    connection.errorMessage.orEmpty(),
                    color = p.warning,
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(16.dp))
            if (connection.isConnected) {
                val timeLimitMin = settings.sessionTimeLimitMinutes
                val trafficLimitMb = settings.sessionTrafficLimitMb
                val timeCapMs = if (timeLimitMin > 0) timeLimitMin.toLong() * 60_000L else 0L
                val trafficCap = if (trafficLimitMb > 0) trafficLimitMb.toLong() * 1024L * 1024L else 0L
                val totalTraffic = connection.totalDownload + connection.totalUpload
                val timeProgress = if (timeCapMs > 0)
                    min(1f, elapsed.toFloat() / timeCapMs.toFloat()) else 0f
                val trafficProgress = if (trafficCap > 0)
                    min(1f, totalTraffic.toFloat() / trafficCap.toFloat()) else 0f
                val timeLabel = if (timeLimitMin > 0)
                    "${elapsed.formatDuration()} / ${formatMinutes(timeLimitMin)}"
                else
                    "${elapsed.formatDuration()} · unlimited"
                val trafficLabel = if (trafficLimitMb > 0)
                    "${totalTraffic.formatBytes()} / ${trafficLimitMb} MB"
                else
                    "${totalTraffic.formatBytes()} · unlimited"

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            "↓ ${connection.downloadSpeed.formatSpeed()}  ·  ↑ ${connection.uploadSpeed.formatSpeed()}",
                            color = p.muted,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(12.dp))

                        SessionProgressBar(
                            label = t("session_time"),
                            valueText = timeLabel,
                            progress = if (timeCapMs > 0) timeProgress else 0f,
                            indeterminate = timeCapMs <= 0,
                            color = when {
                                timeProgress >= 0.95f -> p.error
                                timeProgress >= 0.80f -> p.warning
                                else -> accent
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                        SessionProgressBar(
                            label = t("session_traffic"),
                            valueText = trafficLabel,
                            progress = if (trafficCap > 0) trafficProgress else 0f,
                            indeterminate = trafficCap <= 0,
                            color = when {
                                trafficProgress >= 0.95f -> p.error
                                trafficProgress >= 0.80f -> p.warning
                                else -> p.success
                            }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Limits: Settings → Session limits (remind at 80%/95%/100% — VPN stays on)",
                            color = p.muted.copy(alpha = 0.75f),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Quick test — rectangular button above main action cards
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { if (selected != null && !isTesting) onQuickTest() },
                highlighted = isTesting
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    NeonIcon(
                        Icons.Rounded.NetworkCheck,
                        t("quick_test"),
                        size = 22.dp,
                        tintOverride = if (selected == null) p.muted else if (isTesting) p.warning else accent
                    )
                    Spacer(Modifier.size(10.dp))
                    Text(
                        if (isTesting) t("testing_connections") else t("quick_test"),
                        color = if (selected == null) p.muted else p.text,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onQuickSetup) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        NeonIcon(Icons.Rounded.RocketLaunch, null)
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t("quick_setup"), color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.size(4.dp))
                        Text(
                            t("qs_what"),
                            color = p.muted,
                            fontSize = 12.sp
                        )
                    }
                    NeonIcon(Icons.Rounded.ChevronRight, null, size = 22.dp)
                }
            }

            Spacer(Modifier.height(12.dp))

            // Subscription quota bar — shown above active config when server is from a sub
            if (activeSubscription != null) {
                val sub = activeSubscription
                val used = sub.usedTraffic
                val total = sub.totalTraffic
                val trafficProg = if (total > 0) min(1f, used.toFloat() / total.toFloat()) else 0f
                val timeProgUsed = run {
                    val rem = sub.timeRemainingProgress()
                    if (rem < 0f) -1f else (1f - rem).coerceIn(0f, 1f)
                }
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            "${t("subscription")} · ${sub.name}",
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        SessionProgressBar(
                            label = t("traffic"),
                            valueText = if (total > 0)
                                "${used.formatBytes()} / ${total.formatBytes()}"
                            else if (used > 0)
                                "${used.formatBytes()} ${t("used")}"
                            else
                                "—",
                            progress = trafficProg,
                            indeterminate = total <= 0,
                            color = when {
                                trafficProg >= 0.95f -> p.error
                                trafficProg >= 0.80f -> p.warning
                                else -> accent
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                        SessionProgressBar(
                            label = t("expire"),
                            valueText = sub.expireLabel().ifBlank { "—" },
                            progress = if (timeProgUsed < 0f) 0f else timeProgUsed,
                            indeterminate = timeProgUsed < 0f,
                            color = when {
                                timeProgUsed >= 0.95f -> p.error
                                timeProgUsed >= 0.80f -> p.warning
                                else -> p.success
                            }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            GlassCard(modifier = Modifier.fillMaxWidth(), onClick = onOpenServers) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accent.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        NeonIcon(Icons.Rounded.Dns, null)
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t("choose_server"), color = p.muted, fontSize = 12.sp)
                        Spacer(Modifier.size(4.dp))
                        Text(
                            selected?.displayTitle() ?: t("no_server"),
                            color = p.text,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        if (activeSubscription != null) {
                            Spacer(Modifier.size(4.dp))
                            Text(
                                "Sub: ${activeSubscription.name}",
                                color = accent.copy(0.9f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        if (selected != null && connection.isConnected) {
                            Spacer(Modifier.size(6.dp))
                            Text(
                                "Session ${elapsed.formatDuration()}  ·  ${connection.totalDownload.formatBytes()} down",
                                color = p.muted,
                                fontSize = 12.sp
                            )
                        } else if (selected != null) {
                            Spacer(Modifier.size(6.dp))
                            Text(
                                if (selected.latencyMs >= 0) "Ping ${selected.latencyMs}ms" else "Not tested",
                                color = p.muted,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = p.muted)
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SessionProgressBar(
    label: String,
    valueText: String,
    progress: Float,
    color: androidx.compose.ui.graphics.Color,
    indeterminate: Boolean = false
) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = p.muted, fontSize = 12.sp)
            Text(valueText, color = p.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        if (indeterminate) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(p.surface2)
            )
        } else {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = color,
                trackColor = p.surface2,
                strokeCap = StrokeCap.Round
            )
        }
    }
}

private fun formatMinutes(min: Int): String = when {
    min <= 0 -> "∞"
    min < 60 -> "${min}m"
    min % 60 == 0 -> "${min / 60}h"
    else -> "${min / 60}h ${min % 60}m"
}
