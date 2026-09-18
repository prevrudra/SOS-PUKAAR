package com.pukaar.highalert

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

/**
 * Registers this phone's FCM token with PUKAAR so the server can wake High Alert
 * instantly on SOS — no need to open the app or wait for polling.
 */
object FcmRegistrar {
    private const val TAG = "HighAlertFCM"

    fun isAvailable(): Boolean = true

    suspend fun refreshAndRegister(context: Context) {
        if (!isAvailable()) {
            Log.i(TAG, "FCM disabled — add google-services.json to enable instant push")
            return
        }
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            registerToken(context, token)
        }.onFailure {
            Log.w(TAG, "FCM token fetch failed: ${it.message}")
        }
    }

    suspend fun registerToken(context: Context, token: String) {
        if (token.isBlank()) return
        val appCtx = context.applicationContext
        val session = AlertSession(appCtx)
        val jwt = session.token()
        val phone = session.phone()
        if (jwt.isNullOrBlank() || phone.isNullOrBlank()) {
            Log.w(TAG, "Skip FCM register — not logged in")
            return
        }
        runCatching {
            AlertNetwork.api { jwt }.registerDevice(
                RegisterDeviceRequest(
                    phone = phone,
                    fcmToken = token,
                    deviceId = stableDeviceId(appCtx)
                )
            )
            Log.i(TAG, "FCM token registered for $phone")
        }.onFailure {
            Log.w(TAG, "FCM register failed: ${it.message}")
        }
    }

    private fun stableDeviceId(context: Context): String {
        val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return "ha-${id ?: "unknown"}"
    }
}
