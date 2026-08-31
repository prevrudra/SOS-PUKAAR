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

class PukaarApp : Application() {
    lateinit var sessionStore: SessionStore
        private set
    lateinit var repository: PukaarRepository
        private set

    private val _hardwareSos = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val hardwareSos: SharedFlow<Unit> = _hardwareSos

    fun signalHardwareSos() {
        _hardwareSos.tryEmit(Unit)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        sessionStore = SessionStore(this)
        repository = PukaarRepository(sessionStore)
        createNotificationChannels()
        OemBatteryHelper.ensureChannel(this)
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
        }
    }

    companion object {
        const val CHANNEL_EMERGENCY = "pukaar_emergency"
        const val ACTION_SOS = "com.pukaar.app.ACTION_SOS"
        lateinit var instance: PukaarApp
            private set
    }
}
