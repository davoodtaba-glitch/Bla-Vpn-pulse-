package com.xraypulse.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xraypulse.app.BuildConfig
import com.xraypulse.app.data.model.AppSettings
import com.xraypulse.app.data.model.ConnectionState
import com.xraypulse.app.data.model.ServerProfile
import com.xraypulse.app.data.model.Subscription
import com.xraypulse.app.ui.components.GlassActionRow
import com.xraypulse.app.ui.components.GlassCard
import com.xraypulse.app.ui.components.LtrText
import com.xraypulse.app.ui.components.NeonIcon
import com.xraypulse.app.ui.components.PowerButton
import com.xraypulse.app.ui.i18n.t
import com.xraypulse.app.ui.theme.LocalAccent
import com.xraypulse.app.ui.theme.LocalAccentSecondary
import com.xraypulse.app.ui.theme.LocalPalette
import com.xraypulse.app.util.formatBytes
import com.xraypulse.app.util.formatSpeed
import com.xraypulse.app.util.removeBidiMarks
import com.xraypulse.app.util.withLtrPlaceholders

@Composable
fun HomeScreen(
    connection: ConnectionState,
    selected: ServerProfile?,
    activeSubscription: Subscription? = null,
    settings: AppSettings = AppSettings(),
    isTesting: Boolean = false,
    updateAvailableVersion: String? = null,
    onToggle: () -> Unit,
    onOpenServers: () -> Unit,
    onQuickSetup: () -> Unit = {},
    onQuickTest: () -> Unit = {},
    onRefreshActiveSubscription: () -> Unit = {},
    onOpenUpdate: () -> Unit = {},
    isBusy: Boolean = false
) {
    val accent = LocalAccent.current
    val accent2 = LocalAccentSecondary.current
    val p = LocalPalette.current

    // Fully "Connected" only after tunnel is up AND geo lookup finished (IP + country).
    val fullyConnected = connection.isConnected && !connection.isConnecting &&
        connection.publicIp.isNotBlank() && connection.publicCountry.isNotBlank()
    val showConnecting = connection.isConnecting ||
        (connection.isConnected && !fullyConnected)

    val statusText = when {
        showConnecting -> t("connecting")
        fullyConnected && connection.noTraffic -> t("no_data")
        fullyConnected -> t("protected")
        else -> t("disconnected")
    }
    val statusColor = when {
        showConnecting -> p.warning
        fullyConnected && connection.noTraffic -> p.error
        fullyConnected -> p.success
        else -> p.muted
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        p.bg,
                        p.surface.copy(alpha = 0.9f),
                        accent.copy(alpha = 0.08f),
                        accent2.copy(alpha = 0.05f),
                        p.bg
                    )
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // â”€â”€ Header â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "BLA",
                        color = p.text,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "VPN",
                        color = accent,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(p.surface2.copy(alpha = 0.9f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        LtrText(
                            text = "v${BuildConfig.VERSION_NAME}",
                            color = p.muted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // Small "NEW" pill when GitHub has a newer APK
                    if (!updateAvailableVersion.isNullOrBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(accent.copy(alpha = 0.22f))
                                .border(1.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                                .clickable(onClick = onOpenUpdate)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                t("update_badge_new"),
                                color = accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Row(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Shield, null, tint = statusColor, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        statusText,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                }
            }

            // Full-width update banner on dashboard
            AnimatedVisibility(
                visible = !updateAvailableVersion.isNullOrBlank(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        accent.copy(alpha = 0.22f),
                                        accent2.copy(alpha = 0.16f)
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                Brush.horizontalGradient(
                                    listOf(accent.copy(0.55f), accent2.copy(0.4f))
                                ),
                                RoundedCornerShape(14.dp)
                            )
                            .clickable(onClick = onOpenUpdate)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(accent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.SystemUpdate,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                t("update_available_banner"),
                                color = p.text,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(2.dp))
                            LtrText(
                                text = t("update_available").withLtrPlaceholders(
                                    "{v}" to (updateAvailableVersion ?: "")
                                ),
                                color = accent,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Subscription expiry warning
            if (activeSubscription != null) {
                val daysLeft = activeSubscription.daysRemaining()
                if (daysLeft != null && daysLeft in 0..2) {
                    Spacer(Modifier.height(12.dp))
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Warning, null, tint = p.error, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                t("subscription_expiry_warning"),
                                color = p.error,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Hero power control — bolt/Connecting until IP+country are ready
            PowerButton(
                connected = fullyConnected,
                connecting = showConnecting,
                noTraffic = fullyConnected && connection.noTraffic,
                onClick = onToggle
            )

            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    showConnecting -> t("establishing_tunnel")
                    fullyConnected && connection.noTraffic -> t("connected_no_traffic")
                    fullyConnected -> t("tap_disconnect")
                    else -> t("tap_connect")
                },
                color = when {
                    fullyConnected && connection.noTraffic -> p.error
                    fullyConnected -> accent
                    showConnecting -> p.warning
                    else -> p.muted
                },
                fontSize = 14.sp,
                fontWeight = if (fullyConnected || showConnecting) FontWeight.SemiBold else FontWeight.Medium
            )

            AnimatedVisibility(
                visible = connection.errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    connection.errorMessage.orEmpty(),
                    color = p.warning,
                    modifier = Modifier.padding(top = 8.dp),
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(22.dp))

            // Connection info — only after fully connected (IP + country ready)
            if (fullyConnected) {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Speed,
                                null,
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                t("connection_info"),
                                color = p.text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        InfoRow(
                            icon = Icons.Rounded.Public,
                            label = t("public_ip"),
                            value = connection.publicIp.ifBlank { "..." }
                        )
                        Spacer(Modifier.height(10.dp))
                        InfoRow(
                            icon = Icons.Rounded.Language,
                            label = t("country"),
                            value = connection.publicCountry.ifBlank { "..." }
                        )
                        Spacer(Modifier.height(14.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(p.border.copy(alpha = 0.35f))
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth()) {
                            TrafficBlock(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.ArrowDownward,
                                iconTint = p.success,
                                title = t("download"),
                                speed = connection.downloadSpeed.formatSpeed().removeBidiMarks(),
                                total = connection.totalDownload.formatBytes().removeBidiMarks()
                            )
                            Box(
                                Modifier
                                    .width(1.dp)
                                    .height(56.dp)
                                    .background(p.border.copy(alpha = 0.35f))
                            )
                            TrafficBlock(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Rounded.ArrowUpward,
                                iconTint = p.error,
                                title = t("upload"),
                                speed = connection.uploadSpeed.formatSpeed().removeBidiMarks(),
                                total = connection.totalUpload.formatBytes().removeBidiMarks(),
                                alignEnd = true
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // â”€â”€ Quick actions â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            GlassActionRow(
                title = if (isTesting) t("testing_connections") else t("quick_test"),
                icon = Icons.Rounded.RocketLaunch,
                iconTint = if (selected == null) p.muted else if (isTesting) p.warning else accent,
                enabled = selected != null && !isTesting,
                highlighted = isTesting,
                onClick = onQuickTest
            )
            Spacer(Modifier.height(10.dp))
            GlassActionRow(
                title = t("quick_setup"),
                subtitle = t("qs_what"),
                icon = Icons.Rounded.Settings,
                iconTint = accent2,
                onClick = onQuickSetup
            )

            Spacer(Modifier.height(12.dp))

            // Active subscription
            if (activeSubscription != null) {
                val sub = activeSubscription
                val used = sub.usedTraffic
                val total = sub.totalTraffic
                val days = sub.daysRemaining()
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                t("active_subscription"),
                                color = p.text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (sub.name.isNotBlank()) {
                                // Subscription name badge next to title
                                Box(
                                    Modifier
                                        .weight(1f, fill = false)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(
                                                    accent.copy(alpha = 0.22f),
                                                    accent2.copy(alpha = 0.16f)
                                                )
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            brush = Brush.horizontalGradient(
                                                listOf(accent.copy(0.55f), accent2.copy(0.45f))
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        sub.name,
                                        color = p.text,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            when {
                                days == null -> t("subscription_expire_unknown")
                                days <= 0L -> t("subscription_expired")
                                days == 1L -> t("subscription_days_left_one")
                                else -> t("subscription_days_left").withLtrPlaceholders(
                                    "{n}" to days.toString()
                                )
                            },
                            color = p.text,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            when {
                                total > 0 -> t("subscription_traffic_used_of").withLtrPlaceholders(
                                    "{used}" to used.formatBytes().removeBidiMarks(),
                                    "{total}" to total.formatBytes().removeBidiMarks()
                                )
                                used > 0 -> t("subscription_traffic_used").withLtrPlaceholders(
                                    "{used}" to used.formatBytes().removeBidiMarks()
                                )
                                else -> t("subscription_traffic_unknown")
                            },
                            color = p.muted,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                        Spacer(Modifier.height(14.dp))
                        // Rectangular Update subscription button
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        if (isBusy) {
                                            listOf(p.surface2, p.surface2)
                                        } else {
                                            listOf(accent.copy(0.28f), accent2.copy(0.22f))
                                        }
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isBusy) p.border.copy(0.4f) else accent.copy(0.55f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable(enabled = !isBusy, onClick = onRefreshActiveSubscription)
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                t("update_subscription"),
                                color = if (isBusy) p.muted else accent,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // â”€â”€ Active server â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = onOpenServers,
                highlighted = selected != null
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (selected != null) accent.copy(0.22f) else p.surface2
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Dns,
                            null,
                            tint = if (selected != null) accent else p.muted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            t("active_server"),
                            color = if (selected != null) accent else p.muted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            selected?.displayTitle() ?: t("tap_to_add_server"),
                            color = if (selected != null) p.text else p.muted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                    Icon(Icons.Rounded.ChevronRight, null, tint = p.muted)
                }
            }

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = p.muted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = p.muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        LtrText(
            text = value,
            color = p.text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun TrafficBlock(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    speed: String,
    total: String,
    alignEnd: Boolean = false
) {
    val p = LocalPalette.current
    Column(
        modifier = modifier.padding(horizontal = 10.dp),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!alignEnd) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(8.dp))
            }
            Text(title, color = p.muted, fontSize = 11.sp)
            if (alignEnd) {
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(15.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        LtrText(
            text = speed,
            color = iconTint,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(2.dp))
        LtrText(text = total, color = p.muted, fontSize = 12.sp)
    }
}
