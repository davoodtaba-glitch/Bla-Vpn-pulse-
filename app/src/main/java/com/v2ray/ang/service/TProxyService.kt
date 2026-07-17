package com.v2ray.ang.service

/**
 * JNI bridge matching v2rayNG's hev-socks5-tunnel symbols.
 * Library: libhev-socks5-tunnel.so (extracted from v2rayNG, LGPL/MIT).
 *
 * Native methods must keep these exact names for JNI_OnLoad registration.
 */
object TProxyService {
    init {
        System.loadLibrary("hev-socks5-tunnel")
    }

    @JvmStatic
    @Suppress("FunctionName")
    external fun TProxyStartService(configPath: String, fd: Int)

    @JvmStatic
    @Suppress("FunctionName")
    external fun TProxyStopService()

    @JvmStatic
    @Suppress("FunctionName")
    external fun TProxyGetStats(): LongArray?
}
