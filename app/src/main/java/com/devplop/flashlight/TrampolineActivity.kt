package com.devplop.flashlight

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class TrampolineActivity : ComponentActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create an intent to launch the real activity.
        val intent = Intent(this, FlashlightActivity::class.java).apply {
            // FLAG_ACTIVITY_NEW_TASK is important here to ensure it launches correctly.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        // Start the real activity.
        startActivity(intent)

        // Immediately finish this trampoline activity so it's never seen by the user.
        finish()
    }
}