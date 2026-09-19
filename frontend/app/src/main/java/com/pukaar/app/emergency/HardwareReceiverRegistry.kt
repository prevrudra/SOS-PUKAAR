package com.pukaar.app.emergency

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log

/**
 * Registers SCREEN_ON / power-button receivers without a foreground service.
 * Safe to call from Application, boot, or WorkManager (Android 12+ blocks FGS from background).
 */
object HardwareReceiverRegistry {
    private var screenReceiver: PhoneUsageReceiver? = null
    private var powerButtonReceiver: PowerButtonTriggerReceiver? = null

    fun register(context: Context) {
        val app = context.applicationContext
        if (screenReceiver == null) {
            val receiver = PhoneUsageReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            if (registerDynamic(app, receiver, filter)) screenReceiver = receiver
        }
        if (powerButtonReceiver == null) {
            val receiver = PowerButtonTriggerReceiver()
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            if (registerDynamic(app, receiver, filter)) powerButtonReceiver = receiver
        }
    }

    fun unregister(context: Context) {
        val app = context.applicationContext
        screenReceiver?.let { runCatching { app.unregisterReceiver(it) } }
        screenReceiver = null
        powerButtonReceiver?.let { runCatching { app.unregisterReceiver(it) } }
        powerButtonReceiver = null
    }

    private fun registerDynamic(
        app: Context,
        receiver: android.content.BroadcastReceiver,
        filter: IntentFilter
    ): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                app.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                app.registerReceiver(receiver, filter)
            }
            true
        }.getOrElse { e ->
            Log.w("PUKAAR", "Failed to register ${receiver.javaClass.simpleName}: ${e.message}")
            false
        }
    }
}
