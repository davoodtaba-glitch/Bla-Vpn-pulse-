package com.xraypulse.app.core.xray

import android.content.Context
import android.util.Log
import java.io.File
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Bridge to AndroidLibXrayLite (`libv2ray.aar`).
 *
 * API surface (gomobile):
 *  - Libv2ray.initCoreEnv(envPath, xudpKey)
 *  - Libv2ray.newCoreController(CoreCallbackHandler)
 *  - CoreController.startLoop(configJson, tunFd)
 *  - CoreController.stopLoop()
 *  - CoreController.measureDelay(url)
 *  - Libv2ray.measureOutboundDelay(config, url)
 *  - Libv2ray.checkVersionX()
 */
object XrayController {

    private const val TAG = "XrayController"
    private val running = AtomicBoolean(false)
    private val controllerRef = AtomicReference<Any?>(null)
    private var coreAvailable: Boolean? = null

    fun isCoreAvailable(): Boolean {
        coreAvailable?.let { return it }
        val ok = try {
            Class.forName("libv2ray.Libv2ray")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
        coreAvailable = ok
        return ok
    }

    fun isRunning(): Boolean = running.get()

    fun initEnv(context: Context) {
        val assetsDir = File(context.filesDir, "assets").apply { mkdirs() }
        copyAssetIfPresent(context, "geoip.dat", File(assetsDir, "geoip.dat"))
        copyAssetIfPresent(context, "geosite.dat", File(assetsDir, "geosite.dat"))
        if (!isCoreAvailable()) {
            Log.w(TAG, "libv2ray.aar not on classpath")
            return
        }
        try {
            val lib = Class.forName("libv2ray.Libv2ray")
            val init = lib.methods.firstOrNull {
                it.name.equals("initCoreEnv", ignoreCase = true) && it.parameterTypes.size == 2
            }
            // envPath for geo assets, empty xudp key uses default
            init?.invoke(null, assetsDir.absolutePath, "")
            Log.i(TAG, "InitCoreEnv ok: ${assetsDir.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "InitCoreEnv failed", e)
        }
    }

    /**
     * @return null on success, error message on failure
     */
    fun start(context: Context, configJson: String): String? {
        if (running.get()) stop()
        File(context.filesDir, "config.json").writeText(configJson)

        if (!isCoreAvailable()) {
            Log.w(TAG, "stub mode — no libv2ray.aar")
            running.set(true)
            return null
        }

        return try {
            initEnv(context)
            val lib = Class.forName("libv2ray.Libv2ray")
            val callbackIface = Class.forName("libv2ray.CoreCallbackHandler")
            val callback = Proxy.newProxyInstance(
                callbackIface.classLoader,
                arrayOf(callbackIface)
            ) { _, method, args ->
                Log.d(TAG, "core callback: ${method.name} args=${args?.contentToString()}")
                when (method.returnType) {
                    java.lang.Long.TYPE, java.lang.Long::class.java -> 0L
                    java.lang.Integer.TYPE, java.lang.Integer::class.java -> 0
                    else -> null
                }
            }

            val newCtrl = lib.methods.first {
                it.name.equals("newCoreController", ignoreCase = true)
            }
            val controller = newCtrl.invoke(null, callback)
                ?: throw IllegalStateException("newCoreController returned null")

            val startLoop = controller.javaClass.methods.first {
                it.name.equals("startLoop", ignoreCase = true)
            }
            // StartLoop(configContent string, tunFd int32) — tunFd 0 = no TUN inside core
            val err = when (startLoop.parameterTypes.size) {
                1 -> startLoop.invoke(controller, configJson)
                2 -> {
                    val p1 = startLoop.parameterTypes[1]
                    when {
                        p1 == java.lang.Integer.TYPE || p1 == Integer::class.java ->
                            startLoop.invoke(controller, configJson, 0)
                        p1 == java.lang.Long.TYPE || p1 == java.lang.Long::class.java ->
                            startLoop.invoke(controller, configJson, 0L)
                        else -> startLoop.invoke(controller, configJson, 0)
                    }
                }
                else -> startLoop.invoke(controller, configJson)
            }
            // Go methods returning error map to exception or String in some builds
            if (err is String && err.isNotBlank()) {
                throw IllegalStateException(err)
            }
            controllerRef.set(controller)
            running.set(true)
            Log.i(TAG, "Xray core started")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start core", e)
            running.set(false)
            controllerRef.set(null)
            // Unwrap reflection cause
            val cause = e.cause ?: e
            cause.message ?: e.message ?: "Failed to start Xray core"
        }
    }

    fun stop() {
        val controller = controllerRef.getAndSet(null)
        try {
            if (controller != null) {
                val stop = controller.javaClass.methods.firstOrNull {
                    it.name.equals("stopLoop", ignoreCase = true)
                }
                stop?.invoke(controller)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Stop core: ${e.message}")
        } finally {
            running.set(false)
        }
    }

    fun measureDelay(configJson: String, url: String = "https://www.gstatic.com/generate_204"): Long {
        if (!isCoreAvailable()) return -1
        return try {
            val lib = Class.forName("libv2ray.Libv2ray")
            val method = lib.methods.firstOrNull {
                it.name.equals("measureOutboundDelay", ignoreCase = true)
            } ?: return -1
            val result = method.invoke(null, configJson, url)
            when (result) {
                is Long -> result
                is Int -> result.toLong()
                else -> -1
            }
        } catch (e: Exception) {
            Log.w(TAG, "measureDelay: ${e.message}")
            -1
        }
    }

    fun queryStats(tag: String = "proxy", direction: String = "uplink"): Long {
        val controller = controllerRef.get() ?: return 0
        return try {
            val m = controller.javaClass.methods.firstOrNull {
                it.name.equals("queryStats", ignoreCase = true)
            } ?: return 0
            val result = m.invoke(controller, tag, direction)
            when (result) {
                is Long -> result
                is Int -> result.toLong()
                else -> 0
            }
        } catch (_: Exception) {
            0
        }
    }

    fun version(): String {
        if (!isCoreAvailable()) return "stub (missing libv2ray.aar)"
        return try {
            val lib = Class.forName("libv2ray.Libv2ray")
            val m = lib.methods.firstOrNull {
                it.name.equals("checkVersionX", ignoreCase = true) && it.parameterTypes.isEmpty()
            }
            m?.invoke(null)?.toString() ?: "xray-core"
        } catch (_: Exception) {
            "xray-core"
        }
    }

    private fun copyAssetIfPresent(context: Context, name: String, dest: File) {
        if (dest.exists() && dest.length() > 0) return
        try {
            context.assets.open(name).use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            Log.i(TAG, "Copied asset $name")
        } catch (_: Exception) {
            // optional
        }
    }
}
