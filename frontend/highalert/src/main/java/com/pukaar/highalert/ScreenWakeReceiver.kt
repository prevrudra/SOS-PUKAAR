package com.pukaar.highalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Oppo/Motorola often block background activity starts until the user touches the phone.
 * Poll for pending SOS when the screen turns on or the user unlocks.
 */
class ScreenWakeReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_SCREEN_ON
            && action != Intent.ACTION_USER_PRESENT
            && action != "android.intent.action.SCREEN_ON"
        ) {
            return
        }
        val pending = goAsync()
        scope.launch {
            try {
                if (!AlertSession(context).token().isNullOrBlank()) {
                    Log.i(TAG, "Screen wake — checking pending alert")
                    val appCtx = context.applicationContext
                    PendingAlertChecker.checkAndFire(appCtx)
                    AlertMonitorService.startGuard(appCtx)
                }
            } finally {
                runCatching { pending.finish() }
            }
        }
    }

    companion object {
        private const val TAG = "HighAlertScreenWake"
    }
}
