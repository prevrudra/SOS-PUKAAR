package com.pukaar.app.emergency

import android.content.Context
import android.content.Intent
import com.pukaar.app.MainActivity
import com.pukaar.app.PukaarApp

object VolumeTriggerController {
    private const val WINDOW_MS = 1_500L
    private const val REQUIRED_PRESSES = 3

    @Volatile
    private var pressCount = 0

    @Volatile
    private var lastPressMs = 0L

    /**
     * Returns true when triple volume-up is detected and PUKAAR is launched.
     */
    fun onVolumeUp(context: Context): Boolean {
        val now = System.currentTimeMillis()
        pressCount = if (now - lastPressMs < WINDOW_MS) pressCount + 1 else 1
        lastPressMs = now
        if (pressCount >= REQUIRED_PRESSES) {
            pressCount = 0
            launchPukaar(context)
            return true
        }
        return false
    }

    fun launchPukaar(context: Context) {
        val appContext = context.applicationContext
        val intent = Intent(appContext, MainActivity::class.java).apply {
            action = PukaarApp.ACTION_SOS
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            )
        }
        appContext.startActivity(intent)
        runCatching { PukaarApp.instance.signalHardwareSos() }
    }
}
