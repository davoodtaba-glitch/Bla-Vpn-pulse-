package com.xraypulse.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GppBad
import androidx.compose.material.icons.rounded.GppMaybe
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xraypulse.app.data.model.ServerProfile
import com.xraypulse.app.ui.theme.AppThemeStyle
import com.xraypulse.app.ui.theme.LocalAccent
import com.xraypulse.app.ui.theme.LocalAccentSecondary
import com.xraypulse.app.ui.theme.LocalPalette
import com.xraypulse.app.ui.theme.LocalThemeStyle
import com.xraypulse.app.ui.theme.ledCycleFromAccent
import com.xraypulse.app.ui.theme.neonFamily

/**
 * Embed Latin/digits as LTR inside RTL Persian sentences.
 * Use only for pure technical tokens (IP, "12.5 MB", "120ms") â€” never whole Persian sentences.
 */
fun String.ltrWrap(): String {
    val clean = replace("\u2066", "").replace("\u2067", "").replace("\u2068", "")
        .replace("\u2069", "").replace("\u200E", "").replace("\u200F", "")
        .replace("\u202A", "").replace("\u202B", "").replace("\u202C", "")
    return "\u202A$clean\u202C" // LRE â€¦ PDF
}

/**
 * Compose Text forced LTR â€” for pure technical values only (speed, IP, version, ping).
 * Do not use for mixed Persian sentences (that reverses the Farsi words).
 */
@Composable
fun LtrText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val clean = text
        .replace("\u2066", "").replace("\u2067", "").replace("\u2068", "")
        .replace("\u2069", "").replace("\u202A", "").replace("\u202B", "")
        .replace("\u202C", "").replace("\u200E", "").replace("\u200F", "")
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Text(
            text = clean,
            modifier = modifier,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = maxLines,
            overflow = overflow,
            style = TextStyle(textDirection = TextDirection.Ltr)
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    highlighted: Boolean = false,
    content: @Composable () -> Unit
) {
    val p = LocalPalette.current
    val accent = LocalAccent.current
    val shape = RoundedCornerShape(22.dp)
    val borderColor = if (highlighted) accent.copy(alpha = 0.55f) else p.border.copy(alpha = 0.55f)
    val cardBg = Brush.verticalGradient(
        listOf(
            Color(0xE0182438),
            p.card.copy(alpha = 0.92f),
            Color(0xD00C1424)
        )
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = if (highlighted) 12.dp else 6.dp,
                shape = shape,
                ambientColor = (if (highlighted) accent else p.blue).copy(alpha = 0.18f),
                spotColor = (if (highlighted) accent else p.cyan).copy(alpha = 0.12f)
            )
            .clip(shape)
            .background(cardBg)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        borderColor.copy(alpha = 0.85f),
                        accent.copy(alpha = if (highlighted) 0.35f else 0.12f),
                        borderColor.copy(alpha = 0.4f)
                    )
                ),
                shape = shape
            )
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        content()
    }
}

/** Premium action row used on Home (quick test / quick setup). */
@Composable
fun GlassActionRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconTint: Color,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    val p = LocalPalette.current
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = if (enabled) onClick else null,
        highlighted = highlighted
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(iconTint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = if (enabled) p.text else p.muted,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(subtitle, color = p.muted, fontSize = 12.sp)
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = p.muted.copy(alpha = 0.8f),
                modifier = Modifier.size(22.dp)
            )
        }
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
        testing -> p.warning to "â€¦"
        ms == -2L -> p.error to "!"
        ms < 0 -> p.muted to "â€”"
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
        LtrText(text = text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
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
    // Active server uses a stronger accent treatment so it stands out in the list
    val activeBg = if (selected && !multiSelect) accent.copy(alpha = 0.10f) else null

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (activeBg != null) Modifier.background(activeBg, RoundedCornerShape(20.dp))
                else Modifier
            )
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
                LtrText(
                    text = server.address,
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
    val accent2 = LocalAccentSecondary.current
    val p = LocalPalette.current
    val infinite = rememberInfiniteTransition(label = "power")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val isOn = connected && !noTraffic
    val active = connected || connecting

    // Smooth state colors
    val leftTarget = when {
        connecting -> p.warning
        connected && noTraffic -> p.error
        connected -> accent
        else -> Color(0xFF3A4A68)
    }
    val rightTarget = when {
        connecting -> Color(0xFFFFB04A)
        connected && noTraffic -> Color(0xFFE53935)
        connected -> accent2
        else -> Color(0xFF2A3548)
    }
    val leftColor by animateColorAsState(
        leftTarget,
        tween(420, easing = FastOutSlowInEasing),
        label = "leftC"
    )
    val rightColor by animateColorAsState(
        rightTarget,
        tween(420, easing = FastOutSlowInEasing),
        label = "rightC"
    )
    val iconTint by animateColorAsState(
        when {
            connecting -> p.warning
            connected && noTraffic -> p.error
            connected -> Color(
                red = (accent.red * 0.45f + 0.55f).coerceIn(0f, 1f),
                green = (accent.green * 0.45f + 0.55f).coerceIn(0f, 1f),
                blue = (accent.blue * 0.45f + 0.55f).coerceIn(0f, 1f)
            )
            else -> Color(0xFF8A96B0)
        },
        tween(380, easing = FastOutSlowInEasing),
        label = "iconTint"
    )

    // Breathing scale — soft modern pulse when active
    val breath by infinite.animateFloat(
        initialValue = 1f,
        targetValue = when {
            noTraffic && connected -> 1.045f
            connecting -> 1.035f
            isOn -> 1.028f
            else -> 1f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when {
                    noTraffic && connected -> 700
                    connecting -> 1100
                    isOn -> 2200
                    else -> 3000
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath"
    )
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "press"
    )
    val ringRotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            tween(
                durationMillis = when {
                    connecting -> 1400
                    isOn -> 10000
                    else -> 16000
                },
                easing = LinearEasing
            ),
            RepeatMode.Restart
        ),
        label = "ringRot"
    )
    // Counter-rotating secondary ring for depth
    val ringRotation2 by infinite.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            tween(if (connecting) 2200 else 14000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "ringRot2"
    )
    val glowPulse by infinite.animateFloat(
        initialValue = if (isOn || connecting) 0.68f else 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(if (connecting) 900 else if (isOn) 1600 else 2400, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "glowPulse"
    )
    // Connecting: subtle shield rock / idle: none
    val iconBob by infinite.animateFloat(
        initialValue = if (connecting) -4f else 0f,
        targetValue = if (connecting) 4f else 0f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "iconBob"
    )
    val iconAlpha by infinite.animateFloat(
        initialValue = if (connecting) 0.72f else 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(if (connecting) 700 else 2000, easing = FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "iconAlpha"
    )

    val outer = 260.dp
    val disc = 188.dp
    val iconSz = 70.dp

    val iconVector = when {
        connected && noTraffic -> Icons.Rounded.GppBad
        connecting -> Icons.Rounded.GppMaybe
        connected -> Icons.Rounded.VerifiedUser
        else -> Icons.Rounded.Shield
    }

    Box(
        modifier = modifier
            .size(outer)
            .graphicsLayer {
                val s = breath * pressScale
                scaleX = s
                scaleY = s
            },
        contentAlignment = Alignment.Center
    ) {
        // Ambient bloom
        Box(
            Modifier
                .size(if (isOn || connecting) outer * 1.14f else outer)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = when {
                            isOn -> listOf(
                                leftColor.copy(alpha = 0.42f * glowPulse),
                                rightColor.copy(alpha = 0.26f * glowPulse),
                                leftColor.copy(alpha = 0.10f),
                                Color.Transparent
                            )
                            connecting -> listOf(
                                leftColor.copy(alpha = 0.30f * glowPulse),
                                rightColor.copy(alpha = 0.18f * glowPulse),
                                Color.Transparent
                            )
                            else -> listOf(
                                leftColor.copy(alpha = 0.07f),
                                Color.Transparent
                            )
                        }
                    )
                )
        )

        // Core glow under disc
        if (isOn || connecting) {
            Box(
                Modifier
                    .size(disc * 1.22f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                leftColor.copy(alpha = (if (isOn) 0.52f else 0.32f) * glowPulse),
                                rightColor.copy(alpha = (if (isOn) 0.30f else 0.18f) * glowPulse),
                                Color.Transparent
                            )
                        )
                    )
            )
        }

        // Outer dual-tone ring (primary direction)
        Box(
            Modifier
                .size(outer * 0.94f)
                .rotate(if (active) ringRotation else -18f)
                .drawBehind {
                    val stroke = size.minDimension * (if (isOn) 0.016f else if (connecting) 0.018f else 0.013f)
                    val pad = stroke * 2.4f
                    val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
                    val tl = Offset(pad, pad)
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = size.minDimension / 2f

                    if (isOn || connecting) {
                        drawCircle(
                            color = leftColor.copy(alpha = 0.18f * glowPulse),
                            radius = r * 0.93f,
                            center = Offset(cx, cy),
                            style = Stroke(width = stroke * 1.5f)
                        )
                    }

                    drawArc(
                        color = Color.White.copy(if (isOn) 0.10f else 0.05f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = tl,
                        size = arcSize,
                        style = Stroke(width = stroke * 0.4f)
                    )
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                leftColor.copy(0.15f),
                                leftColor,
                                leftColor.copy(if (isOn) 0.95f else 0.7f),
                                leftColor.copy(0.2f),
                                leftColor.copy(0.15f)
                            )
                        ),
                        startAngle = 140f,
                        sweepAngle = if (active) 210f else 140f,
                        useCenter = false,
                        topLeft = tl,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                    // Fine ticks
                    val ticks = 40
                    for (i in 0 until ticks) {
                        val a = Math.toRadians(i * 360.0 / ticks - 90.0)
                        val major = i % 5 == 0
                        val outerR = r * 0.89f
                        val innerR = if (major) r * 0.845f else r * 0.865f
                        drawLine(
                            color = if (isOn && major) leftColor.copy(0.55f * glowPulse)
                            else Color.White.copy(if (major) 0.20f else 0.08f),
                            start = Offset(
                                cx + (innerR * kotlin.math.cos(a)).toFloat(),
                                cy + (innerR * kotlin.math.sin(a)).toFloat()
                            ),
                            end = Offset(
                                cx + (outerR * kotlin.math.cos(a)).toFloat(),
                                cy + (outerR * kotlin.math.sin(a)).toFloat()
                            ),
                            strokeWidth = if (major) 1.15f else 0.65f,
                            cap = StrokeCap.Round
                        )
                    }
                }
        )

        // Inner counter-rotating arc (secondary accent)
        Box(
            Modifier
                .size(outer * 0.82f)
                .rotate(if (active) ringRotation2 else 25f)
                .drawBehind {
                    val stroke = size.minDimension * 0.012f
                    val pad = stroke * 2.5f
                    val arcSize = Size(size.width - pad * 2, size.height - pad * 2)
                    val tl = Offset(pad, pad)
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                rightColor.copy(0.1f),
                                rightColor.copy(if (isOn || connecting) 0.95f else 0.55f),
                                rightColor.copy(0.15f),
                                rightColor.copy(0.1f)
                            )
                        ),
                        startAngle = -30f,
                        sweepAngle = if (active) 160f else 100f,
                        useCenter = false,
                        topLeft = tl,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round)
                    )
                }
        )

        // Glass disc
        Box(
            Modifier
                .size(disc)
                .shadow(
                    elevation = when {
                        isOn -> 32.dp
                        connecting -> 20.dp
                        else -> 8.dp
                    },
                    shape = CircleShape,
                    ambientColor = leftColor.copy(if (isOn) 0.88f else 0.4f),
                    spotColor = rightColor.copy(if (isOn) 0.72f else 0.28f)
                )
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = if (isOn) {
                            listOf(
                                Color(
                                    red = (0.20f + leftColor.red * 0.42f).coerceIn(0f, 1f),
                                    green = (0.24f + leftColor.green * 0.38f).coerceIn(0f, 1f),
                                    blue = (0.34f + leftColor.blue * 0.42f).coerceIn(0f, 1f)
                                ),
                                Color(0xFF0E1830),
                                Color(0xFF060A12)
                            )
                        } else if (connecting) {
                            listOf(
                                Color(0xFF2A2210),
                                Color(0xFF141018),
                                Color(0xFF08060C)
                            )
                        } else {
                            listOf(
                                Color(0xFF1A2438),
                                Color(0xFF0C1424),
                                Color(0xFF070B14)
                            )
                        }
                    )
                )
                .border(
                    width = if (isOn) 1.25.dp else 1.dp,
                    brush = Brush.sweepGradient(
                        listOf(
                            leftColor.copy(if (isOn) 1f else 0.7f),
                            Color.White.copy(if (isOn) 0.42f else 0.10f),
                            rightColor.copy(if (isOn || connecting) 0.95f else 0.55f),
                            leftColor.copy(if (isOn) 1f else 0.7f)
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isOn) {
                Box(
                    Modifier
                        .size(iconSz * 1.55f)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    leftColor.copy(alpha = 0.50f * glowPulse),
                                    rightColor.copy(alpha = 0.22f * glowPulse),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
            AnimatedContent(
                targetState = iconVector,
                transitionSpec = {
                    (fadeIn(tween(220)) + scaleIn(
                        initialScale = 0.82f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )) togetherWith (fadeOut(tween(160)) + scaleOut(targetScale = 0.88f))
                },
                label = "shieldIcon"
            ) { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = when {
                        connecting -> "Connecting"
                        connected -> "Protected — tap to disconnect"
                        else -> "Connect"
                    },
                    tint = iconTint.copy(alpha = iconAlpha),
                    modifier = Modifier
                        .size(iconSz)
                        .graphicsLayer {
                            rotationZ = iconBob
                            // Connected: slight continuous scale shimmer on icon
                            val iconScale = if (isOn) 0.97f + 0.03f * glowPulse else 1f
                            scaleX = iconScale
                            scaleY = iconScale
                        }
                )
            }
        }

        // Hit target + modern ripple
        Box(
            Modifier
                .size(outer * 0.94f)
                .clip(CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = rememberRipple(
                        bounded = true,
                        color = leftColor.copy(alpha = 0.35f)
                    ),
                    onClick = onClick
                )
        )
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
