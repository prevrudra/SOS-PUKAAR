package com.pukaar.highalert

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Arms wake paths when a contact is logged in.
 * [allowForegroundService] — only true from MainActivity (foreground). Boot/watchdog
 * must not start FGS or Android 12+ kills the process.
 */
object AlertReliabilityEngine {
    private const val TAG = "HighAlertReliability"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun armAll(context: Context, allowForegroundService: Boolean = false) {
        val appCtx = context.applicationContext
        scope.launch {
            val token = AlertSession(appCtx).token()
            if (token.isNullOrBlank()) {
                Log.i(TAG, "Skip arm — not logged in")
                return@launch
            }
            Log.i(TAG, "Arming SOS paths (fgs=$allowForegroundService)")
            MonitorWatchdogReceiver.schedule(appCtx)
            MonitorKeepAliveWorker.enqueue(appCtx)
            MonitorBoostWorker.kick(appCtx)
            FcmRegistrar.refreshAndRegister(appCtx)
            if (allowForegroundService) {
                AlertMonitorService.startGuard(appCtx)
            }
        }
    }

    fun disarm(context: Context) {
        AlertMonitorService.stop(context.applicationContext)
    }
}
