package com.pukaar.app.emergency

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Detects phone unlock and screen-on — silent, no notification to the elder. */
class PhoneUsageReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (!PhoneUsageTracker.isEnabled(context)) return
        when (intent?.action) {
            Intent.ACTION_USER_PRESENT -> PhoneUsageTracker.onPhoneUsed(context, "unlock")
            Intent.ACTION_SCREEN_ON -> PhoneUsageTracker.onPhoneUsed(context, "screen_on")
        }
    }
}
