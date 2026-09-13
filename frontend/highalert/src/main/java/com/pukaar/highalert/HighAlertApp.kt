package com.pukaar.highalert

import android.app.Application
import kotlinx.coroutines.launch

class HighAlertApp : Application() {
    lateinit var session: AlertSession
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        session = AlertSession(this)
        // Re-arm OEM survival paths after process death
        runCatching {
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                if (!session.token().isNullOrBlank()) {
                    MonitorWatchdogReceiver.schedule(this@HighAlertApp)
                    MonitorKeepAliveWorker.enqueue(this@HighAlertApp)
                    AlertMonitorService.start(this@HighAlertApp)
                }
            }
        }
    }

    companion object {
        lateinit var instance: HighAlertApp
            private set
    }
}
