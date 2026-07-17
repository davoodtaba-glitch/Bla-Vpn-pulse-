package com.xraypulse.app.ui.components

import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xraypulse.app.data.model.ServerProfile
import com.xraypulse.app.ui.theme.AppThemeStyle
import com.xraypulse.app.ui.theme.LocalAccent
import com.xraypulse.app.ui.theme.LocalPalette
import com.xraypulse.app.ui.theme.LocalThemeStyle
import com.xraypulse.app.ui.theme.ledCycleFromAccent
import com.xraypulse.app.ui.theme.neonFamily

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    content: @Composable () -> Unit
) {
    val p = LocalPalette.current
    val accent = LocalAccent.current
    val style = LocalThemeStyle.current
    val cyber = style == AppThemeStyle.CYBERPUNK
    val rgb = style == AppThemeStyle.RGB
    val vivid = cyber || rgb
    val shape = RoundedCornerShape(if (vivid) 18.dp else 20.dp)
    val family = remember(accent) { accent.neonFamily() }
    val ledCycle = remember(accent) { accent.ledCycleFromAccent() }
    val infinite = rememberInfiniteTransition(label = "frameGlow")

    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (rgb) 2200 else 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )
    val glowPulse by infinite.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "glowPulse"
    )
    val c1 by infinite.animateColor(
        initialValue = family[0],
        targetValue = family[1],
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Reverse),
        label = "c1"
    )
    val c2 by infinite.animateColor(
        initialValue = family[1],
        targetValue = family[2],
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Reverse),
        label = "c2"
    )
    val c3 by infinite.animateColor(
        initialValue = family[2],
        targetValue = family[3],
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Reverse),
        label = "c3"
    )

    val borderBrush = when {
        rgb -> {
            val shift = phase
            Brush.linearGradient(
                colors = ledCycle,
                start = Offset(shift * 600f, 0f),
                end = Offset(600f - shift * 600f, 400f)
            )
        }
        cyber -> {
            val shift = phase
            Brush.linearGradient(
                colors = listOf(
                    c1.copy(alpha = glowPulse),
                    c2,
                    c3.copy(alpha = glowPulse),
                    family[0]
                ),
                start = Offset(0f, shift * 480f),
                end = Offset(480f, 480f - shift * 480f)
            )
        }
        else -> Brush.linearGradient(listOf(p.border, accent.copy(alpha = 0.45f), p.border))
    }

    val glowColor = when {
        highlighted -> accent
        rgb -> ledCycle[((phase * (ledCycle.size - 1)).toInt()).coerceIn(0, ledCycle.lastIndex)]
        cyber -> c1
        else -> p.border
    }
    val cardBg = when {
        highlighted && vivid -> Brush.verticalGradient(
            listOf(
                accent.copy(alpha = 0.22f),
                p.card.copy(alpha = 0.96f),
                accent.copy(alpha = 0.10f)
            )
        )
        vivid -> Brush.verticalGradient(
            listOf(p.card.copy(alpha = 0.96f), Color(0xE0081018))
        )
        else -> Brush.linearGradient(listOf(p.card, p.card))
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (vivid) (if (highlighted) 20.dp else 14.dp) else 2.dp,
                shape = shape,
                ambientColor = glowColor.copy(alpha = if (highlighted) 0.55f else 0.28f),
                spotColor = glowColor.copy(alpha = if (highlighted) 0.50f else 0.22f)
            )
            .drawBehind {
                val stroke = when {
                    highlighted -> 2.5.dp.toPx()
                    vivid -> 2.dp.toPx()
                    else -> 1.2.dp.toPx()
                }
                if (vivid || highlighted) {
                    drawRoundRect(
                        color = glowColor.copy(alpha = if (highlighted) 0.35f else 0.18f * glowPulse),
                        style = Stroke(width = stroke + 6.dp.toPx()),
                        cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                    )
                }
                drawRoundRect(
                    brush = borderBrush,
                    style = Stroke(width = stroke),
                    cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                )
            }
            .clip(shape)
            .background(cardBg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}

@Composable
fun ProtocolChip(label: String, accent: Color? = null) {
    val a = accent ?: LocalAccent.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = a.copy(alpha = 0.18f)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = a,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LatencyBadge(ms: Long, testing: Boolean = false) {
    val p = LocalPalette.current
    val (color, text) = when {
        testing -> p.warning to "Testing…"
        ms == -2L -> p.error to "Invalid"
        ms < 0 -> p.muted to "—"
        ms < 100 -> p.success to "${ms}ms"
        ms < 300 -> p.warning to "${ms}ms"
        else -> p.error to "${ms}ms"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Rounded.Speed,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ServerListItem(
    server: ServerProfile,
    selected: Boolean,
    testing: Boolean = false,
    multiSelect: Boolean = false,
    checked: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onEdit: () -> Unit = {},
    onTest: () -> Unit = {}
) {
    val accent = LocalAccent.current
    val p = LocalPalette.current
    val family = remember(accent) { accent.neonFamily() }
    val highlight = selected || checked

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        highlighted = highlight
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (multiSelect) {
                NeonIcon(
                    imageVector = if (checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                    contentDescription = null,
                    size = 26.dp
                )
                Spacer(Modifier.width(10.dp))
            } else if (selected) {
                Box(
                    Modifier
                        .width(4.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.verticalGradient(listOf(accent, family[1], family[2]))
                        )
                )
                Spacer(Modifier.width(12.dp))
            }

            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (highlight) {
                            Brush.linearGradient(listOf(accent.copy(0.5f), family[1].copy(0.35f)))
                        } else {
                            Brush.linearGradient(listOf(accent.copy(0.3f), p.violet.copy(0.3f)))
                        }
                    )
                    .clickable(onClick = onTest),
                contentAlignment = Alignment.Center
            ) {
                NeonIcon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tintOverride = if (testing) p.warning else null,
                    size = 22.dp
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        server.displayTitle(),
                        color = if (highlight) accent else p.text,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (selected && !multiSelect) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = accent.copy(alpha = 0.2f)
                        ) {
                            Text(
                                com.xraypulse.app.ui.i18n.t("active"),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = accent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${server.address}:${server.port}",
                    color = if (highlight) accent.copy(alpha = 0.85f) else p.muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                LatencyBadge(server.latencyMs, testing = testing)
                if (selected && !multiSelect) {
                    Spacer(Modifier.height(6.dp))
                    NeonIcon(Icons.Rounded.CheckCircle, null, size = 18.dp)
                }
                if (!multiSelect) {
                    Text(
                        com.xraypulse.app.ui.i18n.t("edit"),
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .clickable(onClick = onEdit)
                    )
                }
            }
        }
    }
}

/** Neon-styled icon: in RGB theme animates through accent-based LED colors. */
@Composable
fun NeonIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    tintOverride: Color? = null
) {
    val accent = LocalAccent.current
    val style = LocalThemeStyle.current
    val rgb = style == AppThemeStyle.RGB
    val family = remember(accent) { accent.neonFamily() }
    val led = remember(accent) { accent.ledCycleFromAccent() }
    val infinite = rememberInfiniteTransition(label = "neonIcon")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "iconPhase"
    )
    val tint = when {
        tintOverride != null -> tintOverride
        rgb -> led[((phase * (led.size - 1)).toInt()).coerceIn(0, led.lastIndex)]
        style == AppThemeStyle.CYBERPUNK -> family[0]
        else -> accent
    }
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size)
    )
}

/** Circular neon action button (home quick-test, etc.). */
@Composable
fun NeonCircleButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    enabled: Boolean = true
) {
    val accent = LocalAccent.current
    val p = LocalPalette.current
    val style = LocalThemeStyle.current
    val rgb = style == AppThemeStyle.RGB
    val led = remember(accent) { accent.ledCycleFromAccent() }
    val family = remember(accent) { accent.neonFamily() }
    val infinite = rememberInfiniteTransition(label = "neonCircle")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "cphase"
    )
    val borderBrush = if (rgb) {
        Brush.sweepGradient(led)
    } else {
        Brush.sweepGradient(listOf(accent, family[1], family[2], accent))
    }
    val glow = if (rgb) led[((phase * (led.size - 1)).toInt()).coerceIn(0, led.lastIndex)] else accent

    Box(
        modifier = modifier
            .size(size)
            .shadow(12.dp, CircleShape, ambientColor = glow.copy(0.45f), spotColor = glow.copy(0.4f))
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(accent.copy(0.35f), p.surface2.copy(0.95f))
                )
            )
            .border(2.dp, borderBrush, CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        NeonIcon(icon, contentDescription, size = size * 0.42f)
    }
}

@Composable
fun PowerButton(
    connected: Boolean,
    connecting: Boolean,
    noTraffic: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = LocalAccent.current
    val p = LocalPalette.current
    val style = LocalThemeStyle.current
    val cyber = style == AppThemeStyle.CYBERPUNK
    val rgb = style == AppThemeStyle.RGB
    val vivid = cyber || rgb
    val family = remember(accent) { accent.neonFamily() }
    val led = remember(accent) { accent.ledCycleFromAccent() }
    val infinite = rememberInfiniteTransition(label = "pulse")
    val scale by infinite.animateFloat(
        initialValue = 1f,
        targetValue = if (connected || connecting) 1.06f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (noTraffic) 600 else 1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val rgbPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "rgbPhase"
    )
    val neonA by infinite.animateColor(
        initialValue = family[0],
        targetValue = family[1],
        animationSpec = infiniteRepeatable(tween(1600), RepeatMode.Reverse),
        label = "neonA"
    )
    val neonB by infinite.animateColor(
        initialValue = family[2],
        targetValue = family[3],
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "neonB"
    )
    val ledIdx = ((rgbPhase * (led.size - 1)).toInt()).coerceIn(0, led.lastIndex)
    val rgbA = led[ledIdx]
    val rgbB = led[(ledIdx + 3) % led.size]

    val targetGlow = when {
        connecting -> p.warning
        connected && noTraffic -> p.error
        connected && rgb -> rgbA
        connected && cyber -> neonA
        connected -> accent
        else -> Color(0xFF3A4560)
    }
    val glow by animateColorAsState(targetGlow, tween(450), label = "glow")

    val ringOuter = when {
        connecting -> p.warning.copy(0.28f)
        connected && noTraffic -> p.error.copy(0.32f)
        connected && rgb -> rgbA.copy(0.28f)
        connected && cyber -> neonA.copy(0.30f)
        connected -> accent.copy(0.20f)
        else -> Color(0xFF1A2030)
    }
    val ringMid = when {
        connecting -> p.warning.copy(0.40f)
        connected && noTraffic -> p.error.copy(0.45f)
        connected && rgb -> rgbB.copy(0.38f)
        connected && cyber -> neonB.copy(0.38f)
        connected -> accent.copy(0.30f)
        else -> Color(0xFF252D42)
    }
    // Modern RGB: rotating multi-stop sweep anchored on accent color picker
    val rgbSweep = remember(led) {
        val doubled = led + led.first()
        doubled
    }
    val centerBrush = when {
        connecting -> Brush.linearGradient(listOf(p.warning, Color(0xFFFF8A3D)))
        connected && noTraffic -> Brush.linearGradient(listOf(p.error, Color(0xFFB00020)))
        connected && rgb -> Brush.radialGradient(
            colors = listOf(
                rgbA.copy(0.95f),
                accent.copy(0.55f),
                Color(0xFF0A0A12)
            )
        )
        connected && cyber -> Brush.linearGradient(listOf(neonA, neonB, family[0]))
        connected -> Brush.linearGradient(listOf(accent, p.blue))
        else -> Brush.linearGradient(listOf(Color(0xFF2A3348), Color(0xFF1A2235)))
    }
    val borderBrush = when {
        connected && rgb -> Brush.sweepGradient(rgbSweep)
        connected && vivid -> Brush.sweepGradient(listOf(neonA, neonB, family[0], neonA))
        else -> Brush.linearGradient(listOf(Color(0xFF4A5568), Color(0xFF4A5568)))
    }

    val outer = 300.dp
    val mid = 248.dp
    val center = 196.dp
    val icon = 84.dp
    Box(
        modifier = modifier
            .size(outer)
            .scale(if (connected || connecting) scale else 1f),
        contentAlignment = Alignment.Center
    ) {
        // Outer soft aura
        Box(Modifier.size(outer).clip(CircleShape).background(ringOuter))
        // Mid ring with optional rotating LED edge
        Box(
            Modifier
                .size(mid)
                .clip(CircleShape)
                .background(ringMid)
                .then(
                    if (connected && rgb) {
                        Modifier.border(
                            width = 3.dp,
                            brush = Brush.sweepGradient(rgbSweep),
                            shape = CircleShape
                        )
                    } else Modifier
                )
        )
        Box(
            Modifier
                .size(center)
                .shadow(
                    elevation = if (connected) (if (rgb) 36.dp else 28.dp) else 6.dp,
                    shape = CircleShape,
                    ambientColor = glow.copy(if (rgb) 0.75f else 1f),
                    spotColor = glow
                )
                .clip(CircleShape)
                .background(centerBrush)
                .then(
                    if (!connected) {
                        Modifier.border(3.dp, Color(0xFF4A5568), CircleShape)
                    } else if (noTraffic) {
                        Modifier.border(4.dp, p.error.copy(0.8f), CircleShape)
                    } else {
                        Modifier.border(
                            width = if (rgb) 4.dp else 3.dp,
                            brush = borderBrush,
                            shape = CircleShape
                        )
                    }
                )
                .clickable(enabled = !connecting, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    connected && noTraffic -> Icons.Rounded.CloudOff
                    connected -> Icons.Rounded.PowerSettingsNew
                    else -> Icons.Rounded.LinkOff
                },
                contentDescription = if (connected) "Disconnect" else "Connect",
                tint = when {
                    connected && rgb -> Color.White
                    connected -> Color.White
                    else -> Color(0xFF9AA3B5)
                },
                modifier = Modifier.size(icon)
            )
        }
    }
}

@Composable
fun StatPill(label: String, value: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(p.surface2)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(label, color = p.muted, fontSize = 10.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = p.text, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

@Composable
fun SectionHeader(title: String, action: String? = null, onAction: (() -> Unit)? = null) {
    val p = LocalPalette.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = p.text, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action,
                color = LocalAccent.current,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.clickable(onClick = onAction)
            )
        }
    }
}
