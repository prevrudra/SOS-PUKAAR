package com.pukaar.app.emergency

import android.content.Context
import android.content.Intent
import android.util.Log
import com.pukaar.app.MainActivity
import com.pukaar.app.PukaarApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object VolumeTriggerController {
    private const val WINDOW_MS = 1_500L
    private const val REQUIRED_PRESSES = 3
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var pressCount = 0

    @Volatile
    private var lastPressMs = 0L

    /** Triple volume-up → headless SOS immediately, then open UI. */
    fun onVolumeUp(context: Context): Boolean {
        val now = System.currentTimeMillis()
        pressCount = if (now - lastPressMs < WINDOW_MS) pressCount + 1 else 1
        lastPressMs = now
        if (pressCount >= REQUIRED_PRESSES) {
            pressCount = 0
            fireHeadlessSos(context)
            return true
        }
        return false
    }

    fun fireHeadlessSos(context: Context) {
        val appContext = context.applicationContext
        Log.w("PUKAAR", "Hardware SOS — triggering headless then opening UI")
        scope.launch {
            runCatching {
                SosTriggerEngine.triggerNow(appContext, isSos = true, mockDrill = false, reason = "volume")
            }
        }
        runCatching { PukaarApp.instance.signalHardwareSos() }
        runCatching {
            appContext.startActivity(
                Intent(appContext, MainActivity::class.java).apply {
                    action = PukaarApp.ACTION_SOS
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    )
                }
            )
        }
    }

    fun launchPukaar(context: Context) = fireHeadlessSos(context)
}
