package com.xraypulse.app.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

data class IpGeoInfo(
    val ip: String = "",
    val country: String = ""
)

/**
 * Resolve public exit IP + country **through the local Xray HTTP inbound**.
 *
 * The app package is excluded from the TUN (`addDisallowedApplication`) so a plain
 * OkHttp call would use the real ISP path and show the wrong IP. Routing via
 * `127.0.0.1:<httpPort>` forces traffic through Xray → remote server.
 */
object IpGeoLookup {
    private const val TAG = "IpGeoLookup"

    suspend fun lookup(httpProxyPort: Int = 10809, maxAttempts: Int = 4): IpGeoInfo =
        withContext(Dispatchers.IO) {
            val client = buildClient(httpProxyPort)
            // Give the tunnel a moment, then retry — first attempt often races connect.
            val delaysMs = longArrayOf(0L, 2500L, 4000L, 5000L)
            var last: IpGeoInfo = IpGeoInfo()
            for (i in 0 until maxAttempts) {
                if (i > 0) delay(delaysMs.getOrElse(i) { 3000L })
                last = tryLookup(client)
                if (last.ip.isNotBlank()) {
                    Log.i(TAG, "lookup ok attempt=${i + 1} ip=${last.ip} country=${last.country}")
                    return@withContext last
                }
                Log.w(TAG, "lookup empty attempt=${i + 1}/$maxAttempts")
            }
            last
        }

    private fun buildClient(httpProxyPort: Int): OkHttpClient {
        val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress("127.0.0.1", httpProxyPort))
        return OkHttpClient.Builder()
            .proxy(proxy)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .build()
    }

    private fun tryLookup(client: OkHttpClient): IpGeoInfo {
        // 1) ip-api (HTTP) — IP + country in one shot
        try {
            val req = Request.Builder()
                .url("http://ip-api.com/json/?fields=status,query,country,countryCode")
                .header("User-Agent", "BLA-VPN/1.0")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val json = JSONObject(resp.body?.string().orEmpty())
                    if (json.optString("status") == "success") {
                        val ip = json.optString("query").trim()
                        val country = json.optString("country").ifBlank {
                            json.optString("countryCode")
                        }.trim()
                        if (ip.isNotBlank()) return IpGeoInfo(ip = ip, country = country)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ip-api failed: ${e.message}")
        }

        // 2) ipinfo.io
        try {
            val req = Request.Builder()
                .url("https://ipinfo.io/json")
                .header("User-Agent", "BLA-VPN/1.0")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val json = JSONObject(resp.body?.string().orEmpty())
                    val ip = json.optString("ip").trim()
                    val country = json.optString("country").trim() // often ISO code
                    if (ip.isNotBlank()) return IpGeoInfo(ip = ip, country = country)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ipinfo failed: ${e.message}")
        }

        // 3) ipify IP only
        try {
            val req = Request.Builder()
                .url("https://api.ipify.org?format=json")
                .header("User-Agent", "BLA-VPN/1.0")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val ip = JSONObject(resp.body?.string().orEmpty()).optString("ip").trim()
                    if (ip.isNotBlank()) return IpGeoInfo(ip = ip)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ipify failed: ${e.message}")
        }

        // 4) ifconfig.me plain text
        try {
            val req = Request.Builder()
                .url("https://ifconfig.me/ip")
                .header("User-Agent", "BLA-VPN/1.0")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) {
                    val ip = resp.body?.string().orEmpty().trim()
                    if (ip.matches(Regex("""^\d{1,3}(\.\d{1,3}){3}$"""))) {
                        return IpGeoInfo(ip = ip)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "ifconfig.me failed: ${e.message}")
        }

        return IpGeoInfo()
    }
}
