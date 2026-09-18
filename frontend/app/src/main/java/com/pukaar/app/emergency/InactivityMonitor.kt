package com.pukaar.app.emergency

import android.content.Context

/**
 * Silent inactivity monitoring — no notifications to the elder.
 * Delegates to [PhoneUsageTracker]; server escalates to trusted contacts only.
 */
object InactivityMonitor {

    fun syncFromServer(context: Context, softHours: Int, enabled: Boolean) {
        PhoneUsageTracker.configure(context, softHours, enabled)
        if (enabled) {
            PhoneUsageTracker.arm(context)
        } else {
            PhoneUsageTracker.disarm(context)
        }
    }

    fun recordActivity(context: Context) {
        PhoneUsageTracker.onPhoneUsed(context, "pukaar_app")
    }

    fun cancel(context: Context) {
        PhoneUsageTracker.disarm(context)
        PhoneUsageTracker.configure(context, softHours = 6, enabled = false)
    }
}
