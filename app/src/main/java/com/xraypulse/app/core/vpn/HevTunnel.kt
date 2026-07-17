package com.xraypulse.app.core.vpn

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import com.v2ray.ang.service.TProxyService
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Starts hev-socks5-tunnel (tun2socks) forwarding TUN traffic to local SOCKS.
 *
 * Important: [ipv4] must equal the VPN interface address set in VpnService.Builder.
 */
object HevTunnel {
    private const val TAG = "HevTunnel"
    private val running = AtomicBoolean(false)

    fun isAvailable(): Boolean = try {
        TProxyService.javaClass
        true
    } catch (e: Throwable) {
        Log.e(TAG, "hev lib not available", e)
        false
    }

    fun start(
        context: Context,
        tun: ParcelFileDescriptor,
        socksPort: Int,
        mtu: Int = 1500,
        ipv4: String = "10.10.14.1",
        dnsListenPort: Int = 10853
    ): Boolean {
        if (!isAvailable()) return false
        if (running.get()) stop()
        return try {
            val yaml = buildConfig(socksPort, mtu, ipv4, dnsListenPort)
            val file = File(context.filesDir, "hev-socks5-tunnel.yaml")
            file.writeText(yaml)
            Log.i(TAG, "Starting hev → socks 127.0.0.1:$socksPort fd=${tun.fd}\n$yaml")
            TProxyService.TProxyStartService(file.absolutePath, tun.fd)
            running.set(true)
            true
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to start hev tunnel", e)
            running.set(false)
            false
        }
    }

    fun stop() {
        try {
            TProxyService.TProxyStopService()
        } catch (e: Throwable) {
            Log.w(TAG, "Stop hev: ${e.message}")
        } finally {
            running.set(false)
        }
    }

    fun stats(): LongArray? = try {
        TProxyService.TProxyGetStats()
    } catch (_: Throwable) {
        null
    }

    private fun buildConfig(
        socksPort: Int,
        mtu: Int,
        ipv4: String,
        dnsListenPort: Int
    ): String = """
        |tunnel:
        |  mtu: $mtu
        |  ipv4: $ipv4
        |socks5:
        |  port: $socksPort
        |  address: 127.0.0.1
        |  udp: 'udp'
        |mapdns:
        |  address: 198.18.0.2
        |  port: 53
        |  network: 198.18.0.0
        |  netmask: 255.254.0.0
        |  cache-size: 10000
        |misc:
        |  tcp-read-write-timeout: 300000
        |  udp-read-write-timeout: 60000
        |  log-level: warn
        """.trimMargin()
}
