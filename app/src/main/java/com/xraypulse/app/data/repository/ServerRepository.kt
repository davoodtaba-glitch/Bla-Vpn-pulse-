package com.xraypulse.app.data.repository

import android.content.Context
import com.xraypulse.app.core.config.XrayConfigBuilder
import com.xraypulse.app.core.parser.ShareLinkParser
import com.xraypulse.app.core.xray.XrayController
import com.xraypulse.app.data.db.AppDatabase
import com.xraypulse.app.data.model.AppSettings
import com.xraypulse.app.data.model.ProtocolType
import com.xraypulse.app.data.model.ServerProfile
import com.xraypulse.app.data.model.Subscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ServerRepository(context: Context) {
    private val db = AppDatabase.get(context)
    private val serverDao = db.serverDao()
    private val subDao = db.subscriptionDao()
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    val servers: Flow<List<ServerProfile>> = serverDao.observeAll()
    val selected: Flow<ServerProfile?> = serverDao.observeSelected()
    val subscriptions: Flow<List<Subscription>> = subDao.observeAll()

    suspend fun addFromLink(link: String): Int = withContext(Dispatchers.IO) {
        val list = ShareLinkParser.parseMulti(link)
        if (list.isEmpty()) {
            ShareLinkParser.parseSingle(link)?.let {
                serverDao.insert(it)
                return@withContext 1
            }
            return@withContext 0
        }
        serverDao.insertAll(list)
        list.size
    }

    suspend fun insert(server: ServerProfile): Long = serverDao.insert(server)

    suspend fun update(server: ServerProfile) = serverDao.update(server.copy(updatedAt = System.currentTimeMillis()))

    suspend fun delete(id: Long) = serverDao.delete(id)

    suspend fun deleteMany(ids: Collection<Long>): Int {
        var n = 0
        ids.forEach {
            serverDao.delete(it)
            n++
        }
        return n
    }

    suspend fun deleteAll(): Int {
        val all = serverDao.getAll()
        all.forEach { serverDao.delete(it.id) }
        return all.size
    }

    /** Removes configs with failed latency test (latencyMs == -2 marker or empty address). */
    suspend fun deleteInvalid(): Int {
        val all = serverDao.getAll()
        val bad = all.filter {
            it.address.isBlank() || it.latencyMs == -2L || (it.uuid.isBlank() && it.password.isBlank() && it.protocol != ProtocolType.CUSTOM_JSON)
        }
        bad.forEach { serverDao.delete(it.id) }
        return bad.size
    }

    /** Mark as invalid after a failed test */
    suspend fun markInvalid(id: Long) = serverDao.updateLatency(id, -2L)

    suspend fun select(id: Long) = serverDao.select(id)

    suspend fun getSelected(): ServerProfile? = serverDao.getSelected()

    suspend fun getById(id: Long): ServerProfile? = serverDao.getById(id)

    suspend fun getAll(): List<ServerProfile> = serverDao.getAll()

    suspend fun addSubscription(name: String, url: String): Long {
        val id = subDao.insert(Subscription(name = name.ifBlank { "Subscription" }, url = url))
        refreshSubscription(id)
        return id
    }

    suspend fun deleteSubscription(id: Long) {
        serverDao.deleteBySubscription(id)
        subDao.delete(id)
    }

    suspend fun renameSubscription(id: Long, name: String) {
        val sub = subDao.getById(id) ?: return
        val n = name.trim()
        if (n.isEmpty()) return
        subDao.update(sub.copy(name = n))
    }

    suspend fun refreshSubscription(id: Long): Int = withContext(Dispatchers.IO) {
        val sub = subDao.getById(id) ?: return@withContext 0
        val req = Request.Builder()
            .url(sub.url)
            .header("User-Agent", sub.userAgent)
            .get()
            .build()
        val (body, userInfo) = http.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            val infoHeader = resp.header("subscription-userinfo")
                ?: resp.header("Subscription-Userinfo")
                ?: resp.header("Subscription-UserInfo")
                ?: ""
            val text = resp.body?.string().orEmpty()
            text to infoHeader
        }
        val info = parseSubscriptionUserInfo(userInfo, body)
        val profiles = ShareLinkParser.parseMulti(body).map {
            it.copy(subscriptionId = id, updatedAt = System.currentTimeMillis())
        }
        serverDao.deleteBySubscription(id)
        if (profiles.isNotEmpty()) serverDao.insertAll(profiles)
        subDao.update(
            sub.copy(
                lastUpdated = System.currentTimeMillis(),
                serverCount = profiles.size,
                usedUpload = info.upload,
                usedDownload = info.download,
                totalTraffic = info.total,
                expireAt = info.expire
            )
        )
        profiles.size
    }

    /**
     * Parse Clash/v2rayN-style subscription-userinfo:
     * upload=; download=; total=; expire= (seconds since epoch)
     * Also scans body for `# subscription-userinfo: ...` comments.
     */
    private fun parseSubscriptionUserInfo(header: String, body: String): SubUserInfo {
        var raw = header.trim()
        if (raw.isBlank()) {
            for (line in body.lineSequence()) {
                val t = line.trim().removePrefix("#").trim()
                if (t.contains("upload=", ignoreCase = true) &&
                    t.contains("download=", ignoreCase = true)
                ) {
                    raw = t.substringAfter(":", t).trim()
                    break
                }
            }
        }
        if (raw.isBlank()) return SubUserInfo()
        fun pick(key: String): Long {
            val re = Regex("""(?i)$key\s*=\s*(\d+)""")
            return re.find(raw)?.groupValues?.getOrNull(1)?.toLongOrNull() ?: 0L
        }
        return SubUserInfo(
            upload = pick("upload"),
            download = pick("download"),
            total = pick("total"),
            expire = pick("expire")
        )
    }

    private data class SubUserInfo(
        val upload: Long = 0,
        val download: Long = 0,
        val total: Long = 0,
        val expire: Long = 0
    )

    suspend fun refreshAllSubscriptions(): Int {
        var total = 0
        subDao.getAll().filter { it.enabled }.forEach {
            try {
                total += refreshSubscription(it.id)
            } catch (_: Exception) {
            }
        }
        return total
    }

    suspend fun testLatency(profile: ServerProfile, settings: AppSettings): Long =
        withContext(Dispatchers.IO) {
            val config = XrayConfigBuilder.build(profile, settings)
            val ms = XrayController.measureDelay(config, settings.testUrl)
            if (profile.id > 0) serverDao.updateLatency(profile.id, ms)
            ms
        }

    suspend fun testAllLatencies(settings: AppSettings) = withContext(Dispatchers.IO) {
        serverDao.getAll().forEach { testLatency(it, settings) }
    }
}
