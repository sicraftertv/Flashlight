package com.devplop.flashlight

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log

class FlashlightService : TileService() {
    // Called when the user adds the tile to their Quick Settings.
    override fun onStartListening() {
        super.onStartListening()
        // Here you could check the flashlight's current state if you were persisting it.
        // For simplicity, we'll keep it inactive by default.
        qsTile?.state = Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }

    // Called when the user taps the tile.
    override fun onClick() {
        super.onClick()

        // Create an intent to launch our FlashlightActivity.
        val intent = Intent(this, TrampolineActivity::class.java).apply {
            // This flag is necessary to start an activity from a service context.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // On modern Android, starting an activity from the background is restricted.
        // TileService is an exception, but it's best practice to ensure the
        // device is unlocked before showing a dialog. unlockAndRun() handles this.
        // The system will automatically collapse the Quick Settings panel.
        unlockAndRun {
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Catching potential exceptions for robustness
                Log.e("FlashlightTileService", "Error starting activity", e)
            }
        }
    }
}