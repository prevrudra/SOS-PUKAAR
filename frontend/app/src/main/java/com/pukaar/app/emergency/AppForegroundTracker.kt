package com.pukaar.app.emergency

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log

/**
 * Tracks whether any activity is visible, and resumes emergency/guard FGS only
 * while the app is in the foreground (Android 12+ forbids background FGS starts).
 */
object AppForegroundTracker {
    @Volatile
    var isInForeground: Boolean = false
        private set

    private var startedActivities = 0
    private var initialized = false

    fun init(application: Application) {
        if (initialized) return
        initialized = true
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityStarted(activity: Activity) {
                    startedActivities++
                    val becameForeground = !isInForeground && startedActivities > 0
                    isInForeground = startedActivities > 0
                    if (becameForeground) {
                        // Safe window to start FGS — never do this from background.
                        runCatching { EmergencyForegroundService.resumeIfNeeded(activity.applicationContext) }
                        runCatching { PukaarGuardService.start(activity.applicationContext) }
                    }
                }

                override fun onActivityStopped(activity: Activity) {
                    startedActivities = (startedActivities - 1).coerceAtLeast(0)
                    isInForeground = startedActivities > 0
                }

                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
                override fun onActivityResumed(activity: Activity) = Unit
                override fun onActivityPaused(activity: Activity) = Unit
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
                override fun onActivityDestroyed(activity: Activity) = Unit
            }
        )
        Log.i("PUKAAR", "AppForegroundTracker initialized")
    }
}
