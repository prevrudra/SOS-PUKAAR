package com.pukaar.app.emergency

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        Log.i("PUKAAR", "Boot completed — arming guard service and emergency listeners")
        OemBatteryHelper.ensureChannel(context)
        PukaarGuardService.start(context)
    }
}
