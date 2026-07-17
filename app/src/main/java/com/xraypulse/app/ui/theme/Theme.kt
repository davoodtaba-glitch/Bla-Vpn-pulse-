package com.xraypulse.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/** App visual style. Cyberpunk is the default (main) theme. */
enum class AppThemeStyle {
    /** Neon cyberpunk — dark #090B10, cyan/blue glows (main). */
    CYBERPUNK,
    /** Original Pulse soft-space look (secondary). */
    PULSE,
    /** RGB lighting — multi-color cycle like modern PC / device RGB. */
    RGB
}

data class AppPalette(
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val card: Color,
    val border: Color,
    val text: Color,
    val muted: Color,
    val success: Color,
    val warning: Color,
    val error: Color,
    val blue: Color,
    val cyan: Color,
    val violet: Color,
    val neonGlow: Color
)

// --- Cyberpunk (default / main) — vivid neon cyan/blue glass ---
val CyberpunkPalette = AppPalette(
    bg = Color(0xFF07090E),
    surface = Color(0xFF0B1018),
    surface2 = Color(0xFF121A28),
    card = Color(0xE0101824),
    border = Color(0xFF00C8FF),
    text = Color(0xFFF0FBFF),
    muted = Color(0xFF6E8FA8),
    success = Color(0xFF00FFC6),
    warning = Color(0xFFFFD166),
    error = Color(0xFFFF4D6D),
    blue = Color(0xFF3D9EFF),
    cyan = Color(0xFF00F0FF),
    violet = Color(0xFF7B6CFF),
    neonGlow = Color(0xFF00F0FF)
)

/** Extra neon colors for animated cyberpunk frames */
val NeonCyan = Color(0xFF00F0FF)
val NeonBlue = Color(0xFF3D9EFF)
val NeonViolet = Color(0xFF9B6CFF)
val NeonMint = Color(0xFF00FFC6)
val NeonPink = Color(0xFFFF4D9A)

// --- Classic Pulse (secondary) ---
val PulsePalette = AppPalette(
    bg = Color(0xFF0B0F1A),
    surface = Color(0xFF141A2A),
    surface2 = Color(0xFF1C2438),
    card = Color(0xFF1A2236),
    border = Color(0xFF2A3550),
    text = Color(0xFFF2F5FF),
    muted = Color(0xFF8B95B0),
    success = Color(0xFF3DFFB5),
    warning = Color(0xFFFFC857),
    error = Color(0xFFFF6B7A),
    blue = Color(0xFF6C8CFF),
    cyan = Color(0xFF00E5C3),
    violet = Color(0xFF9B6CFF),
    neonGlow = Color(0xFF00E5C3)
)

// --- RGB lighting (tertiary) — dark chassis + multicolor LED feel ---
val RgbPalette = AppPalette(
    bg = Color(0xFF050508),
    surface = Color(0xFF0C0C12),
    surface2 = Color(0xFF16161F),
    card = Color(0xE012121C),
    border = Color(0xFFFF2BD6),
    text = Color(0xFFF5F5FF),
    muted = Color(0xFF9A9AB0),
    success = Color(0xFF39FF14),
    warning = Color(0xFFFFAA00),
    error = Color(0xFFFF1744),
    blue = Color(0xFF00B0FF),
    cyan = Color(0xFF00E5FF),
    violet = Color(0xFFD500F9),
    neonGlow = Color(0xFFFF00AA)
)

/** Full RGB LED cycle for frames when RGB theme is active. */
val RgbLedCycle = listOf(
    Color(0xFFFF0040),
    Color(0xFFFF7A00),
    Color(0xFFFFEE00),
    Color(0xFF39FF14),
    Color(0xFF00E5FF),
    Color(0xFF2979FF),
    Color(0xFFD500F9),
    Color(0xFFFF00AA),
    Color(0xFFFF0040)
)

val LocalPalette = staticCompositionLocalOf { CyberpunkPalette }
val LocalAccent = staticCompositionLocalOf { CyberpunkPalette.cyan }
val LocalThemeStyle = staticCompositionLocalOf { AppThemeStyle.PULSE }

// Back-compat aliases used across the app (defaults = cyberpunk)
val PulseBlue get() = CyberpunkPalette.blue
val PulseCyan get() = CyberpunkPalette.cyan
val PulseViolet get() = CyberpunkPalette.violet
val PulsePink = Color(0xFFFF6CAB)
val PulseBg get() = CyberpunkPalette.bg
val PulseSurface get() = CyberpunkPalette.surface
val PulseSurface2 get() = CyberpunkPalette.surface2
val PulseCard get() = CyberpunkPalette.card
val PulseBorder get() = CyberpunkPalette.border
val PulseText get() = CyberpunkPalette.text
val PulseMuted get() = CyberpunkPalette.muted
val PulseSuccess get() = CyberpunkPalette.success
val PulseWarning get() = CyberpunkPalette.warning
val PulseError get() = CyberpunkPalette.error

val ThemeAccentPresets = listOf(
    0xFF00E5FFL to "Neon cyan",
    0xFF4DA3FFL to "Neon blue",
    0xFF00E5C3L to "Teal",
    0xFF6C8CFFL to "Indigo",
    0xFF9B6CFFL to "Violet",
    0xFF3DFFB5L to "Mint",
    0xFFFFC857L to "Amber",
    0xFFFF6B7AL to "Coral"
)

fun Long.toComposeColor(): Color = Color((this or 0xFF000000L).toInt())

fun toAppThemeStyle(name: String): AppThemeStyle =
    // Classic Pulse is the only exposed theme; map any stored value to PULSE for UI consistency
    AppThemeStyle.PULSE

/** Derive a neon palette from the user's accent so frames/glows match the theme color. */
fun Color.neonFamily(): List<Color> {
    val hsl = this.toHsl()
    fun hslColor(h: Float, s: Float = hsl.s, l: Float = hsl.l, a: Float = 1f) =
        hslToColor(h.mod(360f), s.coerceIn(0f, 1f), l.coerceIn(0f, 1f), a)
    return listOf(
        this,
        hslColor(hsl.h + 28f, s = (hsl.s * 0.95f).coerceAtLeast(0.55f), l = (hsl.l * 1.05f).coerceAtMost(0.72f)),
        hslColor(hsl.h + 55f, s = (hsl.s * 0.9f).coerceAtLeast(0.5f), l = (hsl.l * 0.95f).coerceIn(0.4f, 0.68f)),
        hslColor(hsl.h - 25f, s = (hsl.s * 0.92f).coerceAtLeast(0.5f), l = (hsl.l * 1.08f).coerceAtMost(0.75f)),
        this.copy(alpha = 1f)
    )
}

/**
 * Full RGB LED spectrum anchored on the user's chosen accent (for RGB theme frames).
 * Walks the hue wheel so borders / power ring follow the color picker.
 */
fun Color.ledCycleFromAccent(): List<Color> {
    val hsl = this.toHsl()
    val baseS = hsl.s.coerceAtLeast(0.75f)
    val baseL = hsl.l.coerceIn(0.45f, 0.62f)
    val steps = listOf(0f, 40f, 80f, 120f, 160f, 200f, 240f, 280f, 320f, 360f)
    return steps.map { d ->
        hslToColor((hsl.h + d).mod(360f), baseS, baseL)
    }
}

fun Color.glowSoft(alpha: Float = 0.35f): Color = this.copy(alpha = alpha)

private data class Hsl(val h: Float, val s: Float, val l: Float)

private fun Color.toHsl(): Hsl {
    val r = red
    val g = green
    val b = blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val l = (max + min) / 2f
    if (max == min) return Hsl(0f, 0f, l)
    val d = max - min
    val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
    val h = when (max) {
        r -> ((g - b) / d + if (g < b) 6f else 0f) / 6f
        g -> ((b - r) / d + 2f) / 6f
        else -> ((r - g) / d + 4f) / 6f
    } * 360f
    return Hsl(h, s, l)
}

private fun hslToColor(h: Float, s: Float, l: Float, a: Float = 1f): Color {
    if (s == 0f) return Color(l, l, l, a)
    fun hue2rgb(p: Float, q: Float, t0: Float): Float {
        var t = t0
        if (t < 0f) t += 1f
        if (t > 1f) t -= 1f
        return when {
            t < 1f / 6f -> p + (q - p) * 6f * t
            t < 1f / 2f -> q
            t < 2f / 3f -> p + (q - p) * (2f / 3f - t) * 6f
            else -> p
        }
    }
    val q = if (l < 0.5f) l * (1f + s) else l + s - l * s
    val p = 2f * l - q
    val hk = h / 360f
    val r = hue2rgb(p, q, hk + 1f / 3f)
    val g = hue2rgb(p, q, hk)
    val b = hue2rgb(p, q, hk - 1f / 3f)
    return Color(r, g, b, a)
}

private val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 40.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 12.sp)
)

@Composable
fun XrayPulseTheme(
    darkTheme: Boolean = true,
    themeStyle: AppThemeStyle = AppThemeStyle.PULSE,
    accentArgb: Long = 0xFF00E5C3,
    content: @Composable () -> Unit
) {
    // Only Classic Pulse is active in the product UI
    val palette = PulsePalette
    val effectiveStyle = AppThemeStyle.PULSE
    val accent = accentArgb.toComposeColor()
    val scheme = darkColorScheme(
        primary = accent,
        onPrimary = Color.Black,
        secondary = palette.cyan,
        background = palette.bg,
        onBackground = palette.text,
        surface = palette.surface,
        onSurface = palette.text,
        surfaceVariant = palette.surface2,
        onSurfaceVariant = palette.muted,
        outline = palette.border,
        error = palette.error,
        onError = Color.White
    )
    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalAccent provides accent,
        LocalThemeStyle provides effectiveStyle
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AppTypography,
            content = content
        )
    }
}

/** Convenience for composables that need the active palette. */
val appPalette: AppPalette
    @Composable
    @ReadOnlyComposable
    get() = LocalPalette.current
