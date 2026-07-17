package com.xraypulse.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.xraypulse.app.ui.theme.LocalPalette
import com.xraypulse.app.ui.theme.toComposeColor

/**
 * Friendly HSV color chooser:
 * - large SV panel (saturation × value) for current hue
 * - hue bar under it
 * Theme is applied only when the finger is released (no scroll/crash thrash).
 */
@Composable
fun HsvColorPicker(
    colorArgb: Long,
    onCommit: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val p = LocalPalette.current
    val seed = remember(colorArgb) { colorArgb.toComposeColor() }
    var hue by remember { mutableFloatStateOf(seed.toHsv().h) }
    var sat by remember { mutableFloatStateOf(seed.toHsv().s.coerceIn(0.05f, 1f)) }
    var value by remember { mutableFloatStateOf(seed.toHsv().v.coerceIn(0.05f, 1f)) }
    var dragging by remember { mutableStateOf(false) }

    LaunchedEffect(colorArgb) {
        if (!dragging) {
            val hsv = colorArgb.toComposeColor().toHsv()
            hue = hsv.h
            sat = hsv.s.coerceIn(0f, 1f)
            value = hsv.v.coerceIn(0f, 1f)
        }
    }

    fun currentArgb(): Long = hsvToColor(hue, sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f)).toArgbLong()

    Column(modifier = modifier) {
        // SV panel
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.15f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, p.border.copy(0.5f), RoundedCornerShape(16.dp))
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(hue) {
                        detectTapGestures { pos ->
                            sat = (pos.x / size.width).coerceIn(0f, 1f)
                            value = (1f - pos.y / size.height).coerceIn(0f, 1f)
                            onCommit(currentArgb())
                        }
                    }
                    .pointerInput(hue) {
                        detectDragGestures(
                            onDragStart = { pos ->
                                dragging = true
                                sat = (pos.x / size.width).coerceIn(0f, 1f)
                                value = (1f - pos.y / size.height).coerceIn(0f, 1f)
                            },
                            onDragEnd = {
                                dragging = false
                                onCommit(currentArgb())
                            },
                            onDragCancel = {
                                dragging = false
                                onCommit(currentArgb())
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                sat = (change.position.x / size.width).coerceIn(0f, 1f)
                                value = (1f - change.position.y / size.height).coerceIn(0f, 1f)
                            }
                        )
                    }
            ) {
                val pure = hsvToColor(hue, 1f, 1f)
                // horizontal: white → pure hue; vertical overlay: transparent → black
                drawRect(
                    brush = Brush.horizontalGradient(listOf(Color.White, pure))
                )
                drawRect(
                    brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black))
                )
                val cx = sat * size.width
                val cy = (1f - value) * size.height
                drawCircle(
                    color = Color.White,
                    radius = 14.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawCircle(
                    color = Color.Black.copy(0.35f),
                    radius = 16.dp.toPx(),
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // Hue bar
        Box(
            Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, p.border.copy(0.5f), RoundedCornerShape(14.dp))
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { pos ->
                            hue = (pos.x / size.width).coerceIn(0f, 1f) * 360f
                            onCommit(currentArgb())
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { pos ->
                                dragging = true
                                hue = (pos.x / size.width).coerceIn(0f, 1f) * 360f
                            },
                            onDragEnd = {
                                dragging = false
                                onCommit(currentArgb())
                            },
                            onDragCancel = {
                                dragging = false
                                onCommit(currentArgb())
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                hue = (change.position.x / size.width).coerceIn(0f, 1f) * 360f
                            }
                        )
                    }
            ) {
                val hues = listOf(
                    Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                )
                drawRect(brush = Brush.horizontalGradient(hues))
                val cx = (hue / 360f).coerceIn(0f, 1f) * size.width
                drawCircle(
                    color = Color.White,
                    radius = 11.dp.toPx(),
                    center = Offset(cx, size.height / 2f),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        // Preview chip
        Box(
            Modifier
                .size(width = 56.dp, height = 28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(hsvToColor(hue, sat, value))
                .border(1.dp, p.border, RoundedCornerShape(8.dp))
        )
    }
}

private data class Hsv(val h: Float, val s: Float, val v: Float)

private fun Color.toHsv(): Hsv {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val d = max - min
    val v = max
    val s = if (max == 0f) 0f else d / max
    val h = when {
        d == 0f -> 0f
        max == r -> 60f * (((g - b) / d) % 6f)
        max == g -> 60f * (((b - r) / d) + 2f)
        else -> 60f * (((r - g) / d) + 4f)
    }
    val hh = if (h < 0f) h + 360f else h
    return Hsv(hh, s, v)
}

private fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val hh = ((h % 360f) + 360f) % 360f
    val c = v * s
    val x = c * (1f - kotlin.math.abs((hh / 60f) % 2f - 1f))
    val m = v - c
    val (rp, gp, bp) = when {
        hh < 60f -> Triple(c, x, 0f)
        hh < 120f -> Triple(x, c, 0f)
        hh < 180f -> Triple(0f, c, x)
        hh < 240f -> Triple(0f, x, c)
        hh < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        (rp + m).coerceIn(0f, 1f),
        (gp + m).coerceIn(0f, 1f),
        (bp + m).coerceIn(0f, 1f),
        1f
    )
}

private fun Color.toArgbLong(): Long =
    0xFF000000L or (toArgb().toLong() and 0xFFFFFFL)
