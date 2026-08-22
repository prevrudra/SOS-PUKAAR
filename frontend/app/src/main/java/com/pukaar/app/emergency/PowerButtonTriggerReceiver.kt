package com.pukaar.app.emergency

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Approximate hardware trigger using screen on/off cadence.
 * True power-button interception is OEM/OS restricted; this is a best-effort fallback
 * and must be validated per device.
 */
class PowerButtonTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val now = System.currentTimeMillis()
        synchronized(lock) {
            if (now - lastEventMs < 800) {
                pressCount++
            } else {
                pressCount = 1
            }
            lastEventMs = now
            if (pressCount >= 5) {
                pressCount = 0
                Log.w("PUKAAR", "Hardware-like trigger detected via $action — launching SOS activity")
                val launch = Intent(context, Class.forName("com.pukaar.app.MainActivity")).apply {
                    this.action = "com.pukaar.app.ACTION_SOS"
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(launch)
            }
        }
    }

    companion object {
        private val lock = Any()
        private var lastEventMs = 0L
        private var pressCount = 0
    }
}
