package com.xraypulse.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.xraypulse.app.MainActivity
import com.xraypulse.app.R
import com.xraypulse.app.core.config.XrayConfigBuilder
import com.xraypulse.app.core.xray.XrayController
import com.xraypulse.app.data.model.AppSettings
import com.xraypulse.app.data.model.ServerProfile
import com.xraypulse.app.util.JsonStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Proxy-only mode: runs Xray SOCKS/HTTP on localhost without system VPN.
 * Useful for advanced users and debugging.
 */
class XrayProxyService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val profileJson = intent.getStringExtra(EXTRA_PROFILE) ?: return START_NOT_STICKY
                val settingsJson = intent.getStringExtra(EXTRA_SETTINGS)
                scope.launch {
                    val profile = JsonStore.fromJson(profileJson, ServerProfile::class.java) ?: return@launch
                    val settings = settingsJson?.let { JsonStore.fromJson(it, AppSettings::class.java) }
                        ?: AppSettings()
                    createChannel()
                    startForeground(NOTIFICATION_ID, notification(profile.displayTitle()))
                    XrayController.initEnv(this@XrayProxyService)
                    val config = XrayConfigBuilder.build(profile, settings)
                    XrayController.start(applicationContext, config)
                }
            }
            ACTION_STOP -> {
                XrayController.stop()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        XrayController.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Proxy", NotificationManager.IMPORTANCE_LOW)
            )
        }
    }

    private fun notification(title: String) = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("XrayPulse Proxy")
        .setContentText("SOCKS ${title}")
        .setSmallIcon(R.drawable.ic_notification)
        .setContentIntent(
            PendingIntent.getActivity(
                this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        .setOngoing(true)
        .build()

    companion object {
        const val ACTION_START = "com.xraypulse.app.PROXY_START"
        const val ACTION_STOP = "com.xraypulse.app.PROXY_STOP"
        const val EXTRA_PROFILE = "profile"
        const val EXTRA_SETTINGS = "settings"
        private const val CHANNEL_ID = "xraypulse_proxy"
        private const val NOTIFICATION_ID = 7102

        fun start(context: Context, profile: ServerProfile, settings: AppSettings) {
            val i = Intent(context, XrayProxyService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PROFILE, JsonStore.toJson(profile))
                putExtra(EXTRA_SETTINGS, JsonStore.toJson(settings))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }

        fun stop(context: Context) {
            context.startService(Intent(context, XrayProxyService::class.java).setAction(ACTION_STOP))
        }
    }
}
