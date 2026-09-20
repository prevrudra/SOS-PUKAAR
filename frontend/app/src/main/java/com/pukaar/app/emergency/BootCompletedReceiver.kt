package com.pukaar.app.emergency

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        try {
            Log.i("PUKAAR", "Boot completed — arming hardware listeners (no background FGS)")
            OemBatteryHelper.ensureChannel(context)
            HardwareReceiverRegistry.register(context)
            GuardBoostWorker.kick(context)
            HeartbeatWorker.schedule(context)
            if (PhoneUsageTracker.isEnabled(context)) {
                PhoneUsageTracker.arm(context)
            }
        } catch (e: Exception) {
            Log.e("PUKAAR", "Boot receiver failed", e)
        }
    }
}
