package com.xraypulse.app.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.xraypulse.app.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = SettingsRepository(context).settingsFlow.first()
                // Auto-connect is handled by MainActivity / VpnController when enabled
                // and a selected server exists — keep receiver light.
                if (settings.autoConnect) {
                    // Flag file for MainActivity to pick up
                    context.getSharedPreferences("boot", Context.MODE_PRIVATE)
                        .edit().putBoolean("pending_auto_connect", true).apply()
                }
            } finally {
                pending.finish()
            }
        }
    }
}
