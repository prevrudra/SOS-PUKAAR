package com.pukaar.highalert

import android.app.Application

class HighAlertApp : Application() {
    lateinit var session: AlertSession
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        session = AlertSession(this)
        // Do NOT start foreground services here — Android 12+ kills the process with
        // ForegroundServiceStartNotAllowedException when the app is not in the foreground.
        // Monitoring is armed from MainActivity after login.
    }

    companion object {
        lateinit var instance: HighAlertApp
            private set
    }
}
