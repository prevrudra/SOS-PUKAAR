package com.pukaar.highalert

import android.app.Application

class HighAlertApp : Application() {
    lateinit var session: AlertSession
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        session = AlertSession(this)
    }

    companion object {
        lateinit var instance: HighAlertApp
            private set
    }
}
