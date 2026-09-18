package com.pukaar.highalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Alarm-clock repeat — re-rings SOS like a snoozed alarm until dismissed. */
class AlertRingReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent?) {
        val pending = goAsync()
        val appCtx = context.applicationContext
        scope.launch {
            try {
                val alert = AlertRingState.getActive(appCtx)
                if (alert != null && alert.active == true && !AlertSilence.isSilenced(appCtx, alert.eventId)) {
                    Log.i(TAG, "Alarm-clock tick — re-ring ${alert.eventId}")
                    AlertFireHelper.reRing(appCtx, alert)
                } else {
                    AlarmClockRinger.cancel(appCtx)
                }
            } finally {
                runCatching { pending.finish() }
            }
        }
    }

    companion object {
        private const val TAG = "HighAlertRing"
    }
}
