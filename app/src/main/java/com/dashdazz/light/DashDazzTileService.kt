package com.dashdazz.light

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@RequiresApi(Build.VERSION_CODES.N)
class DashDazzTileService : TileService() {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var observeJob: Job? = null

    override fun onStartListening() {
        super.onStartListening()
        updateTileState(DashDazzService.stateFlow.value.isRunning)

        observeJob?.cancel()
        observeJob = DashDazzService.stateFlow
            .onEach { state ->
                updateTileState(state.isRunning)
            }
            .launchIn(scope)
    }

    override fun onStopListening() {
        observeJob?.cancel()
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()

        if (!LightsBridge.isShizukuAvailable() || !LightsBridge.hasShizukuPermission()) {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
            return
        }

        if (DashDazzService.isRunning) {
            DashDazzService.stop(this)
        } else {
            DashDazzService.start(this)
        }
    }

    private fun updateTileState(running: Boolean) {
        val tile = qsTile ?: return

        tile.state = if (running) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.qs_tile_label)
        tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_camera_ring)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (running) {
                getString(R.string.status_service_running)
            } else {
                getString(R.string.status_service_stopped)
            }
        }
        tile.updateTile()
    }
}
