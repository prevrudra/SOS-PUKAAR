package com.pukaar.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.pukaar.app.data.local.SessionStore
import com.pukaar.app.data.repository.PukaarRepository
import com.pukaar.app.emergency.OemBatteryHelper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking

class PukaarApp : Application() {
    lateinit var sessionStore: SessionStore
        private set
    lateinit var repository: PukaarRepository
        private set

    private val _hardwareSos = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val hardwareSos: SharedFlow<Unit> = _hardwareSos

    fun signalHardwareSos() {
        pendingHardwareSos = true
        _hardwareSos.tryEmit(Unit)
    }

    fun consumePendingHardwareSos(): Boolean {
        if (!pendingHardwareSos) return false
        pendingHardwareSos = false
        return true
    }

    private var pendingHardwareSos = false

    override fun onCreate() {
        super.onCreate()
        instance = this
        sessionStore = SessionStore(this)
        repository = PukaarRepository(sessionStore)
        createNotificationChannels()
        OemBatteryHelper.ensureChannel(this)
        com.pukaar.app.emergency.HeartbeatWorker.schedule(this)
        runBlocking {
            if (sessionStore.token() != null) {
                com.pukaar.app.emergency.PukaarGuardService.start(this@PukaarApp)
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val emergency = NotificationChannel(
                CHANNEL_EMERGENCY,
                getString(R.string.emergency_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.emergency_channel_desc)
                setBypassDnd(true)
                enableVibration(true)
                setShowBadge(true)
            }
            nm.createNotificationChannel(emergency)
            val guard = NotificationChannel(
                com.pukaar.app.emergency.PukaarGuardService.CHANNEL_GUARD,
                getString(R.string.guard_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.guard_channel_desc)
                setShowBadge(false)
            }
            nm.createNotificationChannel(guard)
        }
    }

    companion object {
        const val CHANNEL_EMERGENCY = "pukaar_emergency"
        const val ACTION_SOS = "com.pukaar.app.ACTION_SOS"
        lateinit var instance: PukaarApp
            private set
    }
}
