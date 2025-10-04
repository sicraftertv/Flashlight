package com.devplop.flashlight

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.setContent
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

class FlashlightActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // We set the content to our Compose UI and finish the activity when the dialog is dismissed.
        setContent {
            FlashlightDialog(onFinish = { finish() })
        }
    }
}

// A simple helper object to manage the camera and flashlight state.
object FlashlightManager {
    private var cameraId: String? = null
    private var cameraManager: CameraManager? = null
    private var maxStrength = 1

    fun init(context: Context) {
        cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            for (id in cameraManager!!.cameraIdList) {
                val characteristics = cameraManager!!.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (hasFlash == true && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id
                    // Get max flashlight strength if supported (Android 13+)
                    maxStrength = characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1
                    return
                }
            }
        } catch (e: CameraAccessException) {
            Log.e("FlashlightManager", "Error initializing camera", e)
        }
    }

    fun getStrengthLevel(): Int {
        if (cameraId != null) {
            try {
                return cameraManager?.getCameraCharacteristics(cameraId!!)
                    ?.get(CameraCharacteristics.FLASH_INFO_STRENGTH_DEFAULT_LEVEL) ?: 1
            } catch (e: Exception) {
                // Not supported on all devices
            }
        }
        return 1
    }

    fun turnOn(strength: Int) {
        cameraId?.let {
            try {
                cameraManager?.turnOnTorchWithStrengthLevel(it, strength.coerceIn(1, maxStrength))
            } catch (e: CameraAccessException) {
                Log.e("FlashlightManager", "Error turning on flashlight", e)
            }
        }
    }

    fun turnOff() {
        cameraId?.let {
            try {
                cameraManager?.setTorchMode(it, false)
            } catch (e: CameraAccessException) {
                Log.e("FlashlightManager", "Error turning off flashlight", e)
            }
        }
    }

    fun isBrightnessControlSupported(): Boolean {
        return true
    }

    fun getMaxStrength(): Int = maxStrength

}

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)
private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)


@Composable
fun FlashlightDialog(onFinish: () -> Unit, darkTheme: Boolean = isSystemInDarkTheme()) {
    val context = LocalContext.current
    // Initialize the manager when the composable enters the composition.
    LaunchedEffect(Unit) {
        FlashlightManager.init(context)
    }

    // State for the switch and slider
    var isEnabled by remember { mutableStateOf(false) }
    var lightLevel by remember { mutableFloatStateOf(1f) }

    // Check if the device supports brightness control (Android 13+)
    val supportsBrightness = remember { FlashlightManager.isBrightnessControlSupported() }
    val maxStrength = remember { FlashlightManager.getMaxStrength().toFloat() }

    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }


    MaterialTheme( colorScheme = colorScheme) {
        Dialog(onDismissRequest = onFinish) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.wrapContentSize()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "Flashlight",
                            style = TextStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = {
                                isEnabled = it
                                if (isEnabled) {
                                    FlashlightManager.turnOn(lightLevel.toInt().coerceAtLeast(1))
                                } else {
                                    FlashlightManager.turnOff()
                                }
                            },
                        )
                    }

                    if (supportsBrightness) {
                        Slider(
                            value = lightLevel,
                            onValueChange = { newValue ->
                                lightLevel = newValue
                                if (!isEnabled) {
                                    FlashlightManager.turnOn(newValue.toInt().coerceAtLeast(1))
                                    isEnabled = true
                                }
                                FlashlightManager.turnOn(lightLevel.toInt().coerceAtLeast(1))

                            },
                            valueRange = 1f..100f,
                            steps = (maxStrength - 2).toInt().coerceAtLeast(0),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }

}

