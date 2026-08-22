package com.pukaar.app.emergency

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.i("PUKAAR", "Boot completed — emergency listeners armed where OS permits")
        // Re-register high-priority notification channel / OEM survival prompts after reboot.
        OemBatteryHelper.ensureChannel(context)
    }
}
