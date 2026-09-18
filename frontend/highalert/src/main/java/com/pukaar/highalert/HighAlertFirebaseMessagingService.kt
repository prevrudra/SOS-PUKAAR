package com.pukaar.highalert

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Instant SOS wake path — server sends high-priority FCM when elder triggers alert.
 * Polling (watchdog alarms) remains as OEM fallback when FCM is delayed.
 */
class HighAlertFirebaseMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        scope.launch {
            FcmRegistrar.registerToken(applicationContext, token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val type = data["type"] ?: return
        Log.i(TAG, "FCM received type=$type event=${data["eventId"]}")

        when (type.uppercase()) {
            "EMERGENCY_ALERT", "SOS", "HELP" -> scope.launch {
                handleEmergencyPush(data)
            }
            "INACTIVITY_ALERT" -> scope.launch {
                if (data["alertStyle"] == "high" || data["inactivityLevel"] == "URGENT") {
                    handleEmergencyPush(data)
                } else {
                    handleInactivitySoftPush(data)
                }
            }
            "USER_SAFE" -> {
                AlertFireHelper.dismissRinging(applicationContext)
            }
        }
    }

    private suspend fun handleEmergencyPush(data: Map<String, String>) {
        val appCtx = applicationContext
        val eventId = data["eventId"]
        if (eventId.isNullOrBlank()) return

        // Ring immediately from thin FCM — never wait on network for sound.
        val fromPush = alertFromPushData(data)
        AlertFireHelper.fire(appCtx, fromPush)

        // Enrich UI in background; must not be debounced like a second ring.
        val enriched = AlertDataFetcher.fetchEventSnapshot(appCtx, eventId)
        if (enriched?.active == true) {
            AlertFireHelper.updateAlertData(appCtx, enriched)
        }
    }

    private fun handleInactivitySoftPush(data: Map<String, String>) {
        val title = data["title"] ?: "PUKAAR Inactivity"
        val body = data["body"] ?: "A trusted contact may need a check-in."
        InactivitySoftNotifier.show(applicationContext, title, body, data["eventId"])
    }

    private fun alertFromPushData(data: Map<String, String>): PendingAlertResponse {
        return PendingAlertResponse(
            active = true,
            eventId = data["eventId"],
            victimName = data["victimName"],
            victimPhone = data["victimPhone"],
            latitude = data["latitude"]?.toDoubleOrNull(),
            longitude = data["longitude"]?.toDoubleOrNull(),
            batteryPct = data["batteryPct"]?.toIntOrNull(),
            networkType = data["networkType"],
            mockDrill = data["mockDrill"]?.toBooleanStrictOrNull() ?: false,
            triggerType = data["triggerType"],
            policeName = data["policeName"],
            policePhone = data["policePhone"],
            hospitalName = data["hospitalName"],
            hospitalPhone = data["hospitalPhone"]
        )
    }

    companion object {
        private const val TAG = "HighAlertFCM"
    }
}
