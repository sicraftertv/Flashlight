package com.devplop.flashlight

import android.app.PendingIntent
import android.content.Intent
import android.os.VibrationEffect
import android.os.VibratorManager
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService


class FlashlightService : TileService() {
    // Called when the user adds the tile to their Quick Settings.
    override fun onStartListening() {
        super.onStartListening()
        // Here you could check the flashlight's current state if you were persisting it.
        // For simplicity, we'll keep it inactive by default.
        qsTile?.state = updateTileState()
        qsTile?.updateTile()
    }

    // Called when the user taps the tile.
    override fun onClick() {
        super.onClick()

        val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
        val vibrator = vibratorManager.defaultVibrator
        val vibrationEffect = VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)

        vibrator.vibrate(vibrationEffect)

        //Create an intent to launch our FlashlightActivity.
        val intent = Intent(this, FlashlightActivity::class.java).apply {
            // This flag is necessary to start an activity from a service context.
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        if (!isLocked) {
            val pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

            startActivityAndCollapse(pendingIntent)

        } else {
            startActivity(intent)
        }
    }

    private fun updateTileState(): Int {
        if (isEnabled) {
            return Tile.STATE_ACTIVE
        }
        return Tile.STATE_INACTIVE
    }
}

