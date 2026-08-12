package com.xraypulse.app.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object JsonStore {
    val gson: Gson = GsonBuilder().disableHtmlEscaping().create()

    fun toJson(any: Any): String = gson.toJson(any)

    fun <T> fromJson(json: String, clazz: Class<T>): T? = try {
        gson.fromJson(json, clazz)
    } catch (_: Exception) {
        null
    }
}

/**
 * Force LTR display for mixed Latin digits/units inside RTL (Farsi) layouts.
 * Uses LRE…PDF (U+202A…U+202C) which is widely respected by Android text layout.
 */
fun String.asLtr(): String {
    val clean = removeBidiMarks()
    // LRE + string + PDF — embed as left-to-right run inside RTL paragraph
    return "\u202A$clean\u202C"
}

/** Strip all common bidi control marks so we never nest isolates. */
fun String.removeBidiMarks(): String =
    replace("\u2066", "").replace("\u2067", "").replace("\u2068", "")
        .replace("\u2069", "").replace("\u200E", "").replace("\u200F", "")
        .replace("\u202A", "").replace("\u202B", "").replace("\u202C", "")
        .replace("\u202D", "").replace("\u202E", "")

/**
 * Fill `{placeholders}` with LTR-isolated values inside an RTL (or LTR) sentence.
 * The sentence itself stays in the ambient layout direction — only values flip LTR.
 */
fun String.withLtrPlaceholders(vararg pairs: Pair<String, String>): String {
    var out = this
    for ((key, value) in pairs) {
        out = out.replace(key, value.removeBidiMarks().asLtr())
    }
    return out
}

fun Long.formatBytes(): String {
    // Pure ASCII units — never rely on locale unit symbols (avoids strange glyphs in RTL)
    if (this < 1024L) return ("$this B").asLtr()
    val units = arrayOf("KB", "MB", "GB", "TB")
    var v = this.toDouble()
    var i = -1
    do {
        v /= 1024.0
        i++
    } while (v >= 1024.0 && i < units.lastIndex)
    // Locale.US: Western digits + '.' decimal; unit is ASCII "KB"/"MB"/…
    val body = String.format(java.util.Locale.US, "%.1f %s", v, units[i])
    return body.asLtr()
}

fun Long.formatSpeed(): String {
    val body = formatBytes().removeBidiMarks()
    // e.g. "12.5 KB/s"
    return ("$body/s").asLtr()
}

fun Long.formatDuration(): String {
    val totalSec = this / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    val raw = if (h > 0) String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(java.util.Locale.US, "%02d:%02d", m, s)
    return raw.asLtr()
}

/** @deprecated use [removeBidiMarks] */
fun String.removeLtrMarks(): String = removeBidiMarks()
