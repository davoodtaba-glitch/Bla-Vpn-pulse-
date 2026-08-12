package com.xraypulse.app.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import com.xraypulse.app.MainActivity
import com.xraypulse.app.data.repository.ServerRepository
import com.xraypulse.app.data.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Quick Settings tile: one-tap connect / disconnect for BLA VPN.
 * Appears in the Android status-bar shade (edit tiles to add "BLA VPN").
 */
@RequiresApi(Build.VERSION_CODES.N)
class VpnTileService : TileService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        val connected = XrayVpnService.state.value.isConnected ||
            XrayVpnService.state.value.isConnecting
        if (connected) {
            XrayVpnService.stop(applicationContext)
            updateTile(forceActive = false)
            return
        }
        // Connect needs VPN permission UI on first run — open MainActivity quick-toggle
        val prepare = XrayVpnService.prepare(applicationContext)
        if (prepare != null) {
            val open = Intent(this, MainActivity::class.java).apply {
                action = XrayVpnService.ACTION_QUICK_TOGGLE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivityAndCollapse(open)
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                val servers = ServerRepository(applicationContext)
                val settings = SettingsRepository(applicationContext)
                val selected = servers.selected.first()
                val appSettings = settings.settingsFlow.first()
                if (selected == null) {
                    val open = Intent(this@VpnTileService, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivityAndCollapse(open)
                    return@launch
                }
                XrayVpnService.start(applicationContext, selected, appSettings)
                launch(Dispatchers.Main) { updateTile(forceActive = true) }
            } catch (_: Exception) {
                val open = Intent(this@VpnTileService, MainActivity::class.java).apply {
                    action = XrayVpnService.ACTION_QUICK_TOGGLE
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivityAndCollapse(open)
            }
        }
    }

    private fun updateTile(forceActive: Boolean? = null) {
        val tile = qsTile ?: return
        val active = forceActive
            ?: (XrayVpnService.state.value.isConnected || XrayVpnService.state.value.isConnecting)
        tile.state = if (active) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "BLA VPN"
        tile.contentDescription = if (active) "Disconnect BLA VPN" else "Connect BLA VPN"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (active) {
                XrayVpnService.state.value.selectedRemark.ifBlank { "Connected" }
            } else {
                "Tap to connect"
            }
        }
        tile.updateTile()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
