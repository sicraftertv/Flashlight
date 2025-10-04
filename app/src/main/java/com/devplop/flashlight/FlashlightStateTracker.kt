package com.devplop.flashlight

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FlashlightStateTracker(context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraIdWithFlash: String? = null

    // Use a StateFlow to observe the flashlight state from your UI (e.g., Jetpack Compose)
    private val _isFlashlightOn = MutableStateFlow(false)
    val isFlashlightOn = _isFlashlightOn.asStateFlow()

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            super.onTorchModeChanged(cameraId, enabled)
            if (cameraId == cameraIdWithFlash) {
                _isFlashlightOn.value = enabled
                Log.d("FlashlightStateTracker", "Flashlight is now ${if (enabled) "ON" else "OFF"}")
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            super.onTorchModeUnavailable(cameraId)
            if (cameraId == cameraIdWithFlash) {
                _isFlashlightOn.value = false
                Log.e("FlashlightStateTracker", "Flashlight became unavailable.")
            }
        }
    }

    init {
        findCameraWithFlash()
    }

    private fun findCameraWithFlash() {
        try {
            val cameraIds = cameraManager.cameraIdList
            for (id in cameraIds) {
                val characteristics = cameraManager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                if (hasFlash == true) {
                    cameraIdWithFlash = id
                    Log.d("FlashlightStateTracker", "Found camera with flash: $cameraIdWithFlash")
                    return // Found the first camera with a flash
                }
            }
            Log.w("FlashlightStateTracker", "No camera with a flash unit found.")
        } catch (e: CameraAccessException) {
            Log.e("FlashlightStateTracker", "Error accessing camera: ${e.message}")
        }
    }

    fun startListening() {
        if (cameraIdWithFlash != null) {
            try {
                // The handler ensures callbacks are on the main thread
                cameraManager.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
                Log.d("FlashlightStateTracker", "Registered torch callback.")
            } catch (e: CameraAccessException) {
                Log.e("FlashlightStateTracker", "Error registering torch callback: ${e.message}")
            }
        }
    }

    fun stopListening() {
        if (cameraIdWithFlash != null) {
            try {
                cameraManager.unregisterTorchCallback(torchCallback)
                Log.d("FlashlightStateTracker", "Unregistered torch callback.")
            } catch (e: CameraAccessException) {
                Log.e("FlashlightStateTracker", "Error unregistering torch callback: ${e.message}")
            }
        }
    }
}