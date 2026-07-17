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

fun Long.formatBytes(): String {
    if (this < 1024) return "$this B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var v = this.toDouble()
    var i = -1
    do {
        v /= 1024.0
        i++
    } while (v >= 1024 && i < units.lastIndex)
    return String.format("%.1f %s", v, units[i])
}

fun Long.formatSpeed(): String = "${formatBytes()}/s"

fun Long.formatDuration(): String {
    val totalSec = this / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%02d:%02d", m, s)
}
