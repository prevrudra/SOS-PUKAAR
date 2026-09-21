package com.pukaar.highalert

import android.app.Activity
import android.app.Application
import android.os.Bundle

/** Tracks whether any activity is visible — FGS must only start from the foreground. */
object AppForegroundTracker {
    @Volatile
    var isInForeground: Boolean = false
        private set

    private var startedActivities = 0

    fun init(application: Application) {
        application.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityStarted(activity: Activity) {
                    startedActivities++
                    isInForeground = startedActivities > 0
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
    }
}
