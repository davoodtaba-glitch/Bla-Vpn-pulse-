package com.xraypulse.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.xraypulse.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class GithubReleaseInfo(
    val tagName: String,
    val versionName: String,
    val versionCodeHint: Int?,
    val releaseNotes: String,
    val apkUrl: String,
    val apkName: String,
    val htmlUrl: String
)

sealed class UpdateCheckResult {
    data class UpToDate(val current: String) : UpdateCheckResult()
    data class Available(val release: GithubReleaseInfo) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * Check GitHub Releases and download/install APK updates in-app.
 * Repo: [BuildConfig.GITHUB_OWNER]/[BuildConfig.GITHUB_REPO]
 */
object AppUpdater {
    private const val TAG = "AppUpdater"

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val apiBase: String
        get() = "https://api.github.com/repos/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}"

    suspend fun checkLatest(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("$apiBase/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "BLA-VPN/${BuildConfig.VERSION_NAME}")
                .get()
                .build()
            http.newCall(req).execute().use { resp ->
                if (resp.code == 404) {
                    return@withContext UpdateCheckResult.Error("No releases found on GitHub")
                }
                if (!resp.isSuccessful) {
                    return@withContext UpdateCheckResult.Error("GitHub HTTP ${resp.code}")
                }
                val body = resp.body?.string().orEmpty()
                if (body.isBlank()) {
                    return@withContext UpdateCheckResult.Error("Empty GitHub response")
                }
                val json = JSONObject(body)
                val tag = json.optString("tag_name").trim()
                val name = json.optString("name").trim()
                val notes = json.optString("body").orEmpty()
                val htmlUrl = json.optString("html_url").ifBlank {
                    "https://github.com/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases"
                }
                val versionName = normalizeVersion(tag.ifBlank { name })
                val versionCodeHint = extractVersionCode(notes) ?: extractVersionCode(name)

                val assets = json.optJSONArray("assets")
                    ?: return@withContext UpdateCheckResult.Error("Release has no assets")
                var apkUrl = ""
                var apkName = ""
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    val n = a.optString("name")
                    val url = a.optString("browser_download_url")
                    if (n.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) {
                        // Prefer release APK over debug
                        if (apkUrl.isEmpty() ||
                            n.contains("release", ignoreCase = true) ||
                            !n.contains("debug", ignoreCase = true)
                        ) {
                            apkUrl = url
                            apkName = n
                            if (n.contains("release", ignoreCase = true) &&
                                !n.contains("debug", ignoreCase = true)
                            ) break
                        }
                    }
                }
                if (apkUrl.isBlank()) {
                    return@withContext UpdateCheckResult.Error("No APK asset in latest release")
                }

                val release = GithubReleaseInfo(
                    tagName = tag.ifBlank { versionName },
                    versionName = versionName,
                    versionCodeHint = versionCodeHint,
                    releaseNotes = notes.take(800),
                    apkUrl = apkUrl,
                    apkName = apkName.ifBlank { "update.apk" },
                    htmlUrl = htmlUrl
                )

                val current = BuildConfig.VERSION_NAME
                val newer = isRemoteNewer(
                    remoteVersion = release.versionName,
                    remoteCode = release.versionCodeHint,
                    localVersion = current,
                    localCode = BuildConfig.VERSION_CODE
                )
                if (newer) UpdateCheckResult.Available(release)
                else UpdateCheckResult.UpToDate(current)
            }
        } catch (e: Exception) {
            Log.e(TAG, "checkLatest failed", e)
            UpdateCheckResult.Error(e.message ?: "Update check failed")
        }
    }

    /**
     * Download APK to app cache. [onProgress] is 0..100 (or -1 if unknown).
     */
    suspend fun downloadApk(
        context: Context,
        release: GithubReleaseInfo,
        onProgress: (Int) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        val out = File(dir, "bla-vpn-${release.versionName}.apk")
        if (out.exists()) out.delete()

        val req = Request.Builder()
            .url(release.apkUrl)
            .header("User-Agent", "BLA-VPN/${BuildConfig.VERSION_NAME}")
            .header("Accept", "application/octet-stream")
            .get()
            .build()

        http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IllegalStateException("Download HTTP ${resp.code}")
            }
            val body = resp.body ?: throw IllegalStateException("Empty download body")
            val total = body.contentLength()
            body.byteStream().use { input ->
                out.outputStream().use { output ->
                    val buf = ByteArray(64 * 1024)
                    var readTotal = 0L
                    var lastPct = -1
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        readTotal += n
                        if (total > 0) {
                            val pct = ((readTotal * 100) / total).toInt().coerceIn(0, 100)
                            if (pct != lastPct) {
                                lastPct = pct
                                onProgress(pct)
                            }
                        } else {
                            onProgress(-1)
                        }
                    }
                    output.flush()
                }
            }
        }
        onProgress(100)
        if (!out.exists() || out.length() < 1024) {
            throw IllegalStateException("Downloaded APK is invalid")
        }
        Log.i(TAG, "Downloaded ${out.length()} bytes → ${out.absolutePath}")
        out
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun installApk(context: Context, apkFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun openReleasesPage(context: Context) {
        val url = "https://github.com/${BuildConfig.GITHUB_OWNER}/${BuildConfig.GITHUB_REPO}/releases"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    internal fun normalizeVersion(raw: String): String {
        var s = raw.trim()
        if (s.startsWith("v", ignoreCase = true) && s.length > 1 && s[1].isDigit()) {
            s = s.substring(1)
        }
        // "BLA VPN 1.36" → try last token that looks like a version
        if (!s.firstOrNull()?.isDigit().orFalse()) {
            val m = Regex("""(\d+(?:\.\d+)+)""").find(s)
            if (m != null) return m.groupValues[1]
        }
        return s
    }

    private fun Boolean?.orFalse() = this == true

    internal fun extractVersionCode(text: String): Int? {
        if (text.isBlank()) return null
        // versionCode 40 / versionCode=40 / code: 40
        val m = Regex(
            """(?:versionCode|version_code|code)\s*[:=]?\s*(\d+)""",
            RegexOption.IGNORE_CASE
        ).find(text)
        return m?.groupValues?.getOrNull(1)?.toIntOrNull()
    }

    /**
     * Prefer versionCode when remote provides it; else compare dotted version names.
     */
    internal fun isRemoteNewer(
        remoteVersion: String,
        remoteCode: Int?,
        localVersion: String,
        localCode: Int
    ): Boolean {
        if (remoteCode != null && remoteCode > 0) {
            return remoteCode > localCode
        }
        return compareVersions(remoteVersion, localVersion) > 0
    }

    /** Positive if a > b */
    internal fun compareVersions(a: String, b: String): Int {
        val pa = parseVersionParts(a)
        val pb = parseVersionParts(b)
        val n = maxOf(pa.size, pb.size)
        for (i in 0 until n) {
            val x = pa.getOrElse(i) { 0 }
            val y = pb.getOrElse(i) { 0 }
            if (x != y) return x.compareTo(y)
        }
        return 0
    }

    private fun parseVersionParts(v: String): List<Int> {
        val clean = normalizeVersion(v)
        return clean.split('.', '-', '_', ' ')
            .mapNotNull { part ->
                val digits = part.takeWhile { it.isDigit() }
                digits.toIntOrNull()
            }
            .ifEmpty { listOf(0) }
    }
}
