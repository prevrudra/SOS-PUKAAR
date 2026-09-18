package com.pukaar.app.emergency

import android.content.Context
import android.util.Log
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.pukaar.app.PukaarApp
import com.pukaar.app.data.api.EmergencyDto
import com.pukaar.app.data.api.TriggerRequest
import com.pukaar.app.util.DeviceTelemetry
import com.pukaar.app.util.EmergencyAlertHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Headless SOS — works even if MainActivity is blocked by OEM.
 * Always attempts: server trigger → device SMS fallback → local retry outbox.
 */
object SosTriggerEngine {
    private const val TAG = "PUKAAR_SOS"
    private val inFlight = AtomicBoolean(false)

    data class Result(
        val eventId: String,
        val serverOk: Boolean,
        val smsSent: Int,
        val offline: Boolean
    )

    suspend fun triggerNow(
        context: Context,
        isSos: Boolean = true,
        mockDrill: Boolean = false,
        reason: String = "manual"
    ): Result {
        if (!inFlight.compareAndSet(false, true)) {
            Log.w(TAG, "SOS already in flight ($reason)")
            val existing = EmergencySessionStore.activeEventId(context)
            return Result(existing ?: "in-flight", serverOk = false, smsSent = 0, offline = true)
        }
        return try {
            doTrigger(context.applicationContext, isSos, mockDrill, reason)
        } finally {
            inFlight.set(false)
        }
    }

    private suspend fun doTrigger(
        context: Context,
        isSos: Boolean,
        mockDrill: Boolean,
        reason: String
    ): Result {
        Log.i(TAG, "SOS trigger start reason=$reason isSos=$isSos mock=$mockDrill")
        val settings = runCatching { PukaarApp.instance.sessionStore.sosSettings() }.getOrNull()
        val wantLocation = settings?.location != false
        val wantAudio = settings?.audio != false
        val wantCall112 = settings?.autoCall == true && isSos && !mockDrill

        val loc = if (wantLocation) {
            withTimeoutOrNull(1_500L) {
                runCatching {
                    val client = LocationServices.getFusedLocationProviderClient(context)
                    client.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        CancellationTokenSource().token
                    ).await()
                }.getOrNull()
            } ?: runCatching {
                LocationServices.getFusedLocationProviderClient(context).lastLocation.await()
            }.getOrNull()
        } else null

        val lat = loc?.latitude
        val lng = loc?.longitude
        val accuracy = loc?.accuracy?.toDouble()
        val battery = DeviceTelemetry.batteryPercent(context)
        val network = DeviceTelemetry.networkType(context)

        var serverEvent: EmergencyDto? = null
        var serverOk = false
        repeat(3) { attempt ->
            try {
                serverEvent = PukaarApp.instance.repository.trigger(
                    TriggerRequest(
                        triggerType = when {
                            mockDrill -> "MOCK_DRILL"
                            isSos -> "APP"
                            else -> "HELP"
                        },
                        latitude = lat,
                        longitude = lng,
                        accuracyM = accuracy,
                        mockDrill = mockDrill,
                        batteryPct = battery,
                        networkType = network
                    )
                )
                if (!serverEvent?.id.isNullOrBlank()) {
                    serverOk = true
                    return@repeat
                }
            } catch (e: Exception) {
                Log.w(TAG, "Server trigger attempt ${attempt + 1} failed: ${e.message}")
                delay(500L * (attempt + 1))
            }
        }

        val eventId = serverEvent?.id ?: "local-${UUID.randomUUID()}"
        EmergencySessionStore.save(
            context,
            eventId = eventId,
            isSos = isSos,
            mockDrill = mockDrill,
            serverSynced = serverOk,
            latitude = lat,
            longitude = lng
        )

        // Always start local emergency automation (location / audio) when we have a real or local id.
        EmergencyForegroundService.start(
            context,
            eventId,
            isSos = isSos && !mockDrill,
            recordAudio = wantAudio && (isSos || mockDrill)
        )

        // Device SMS — always for real SOS/HELP; also when server failed.
        var smsSent = 0
        if (!mockDrill) {
            val smsEvent = serverEvent ?: EmergencyDto(
                id = eventId,
                active = true,
                latitude = lat,
                longitude = lng,
                batteryPct = battery,
                networkType = network,
                userName = null,
                userPhone = null
            )
            val smsResult = runCatching {
                EmergencyAlertHelper.sendSmsToContactsInBackground(
                    context, smsEvent, isSos = isSos, isMockDrill = false
                )
            }.getOrNull()
            smsSent = smsResult?.sent ?: 0
            Log.i(TAG, "Device SMS fallback sent=$smsSent")
        }

        if (wantCall112) {
            withContext(Dispatchers.Main) {
                EmergencyAlertHelper.call112InBackground(context)
            }
        }

        if (!serverOk && !mockDrill) {
            SosRetryWorker.enqueue(context)
            Log.w(TAG, "Offline SOS armed — will retry server sync")
        }

        Log.i(TAG, "SOS complete eventId=$eventId serverOk=$serverOk smsSent=$smsSent")
        return Result(eventId, serverOk, smsSent, offline = !serverOk)
    }
}
