package com.pukaar.app.util

import android.content.Context
import android.util.Log
import com.pukaar.app.PukaarApp
import com.pukaar.app.data.api.EmergencyDto
import com.pukaar.app.integration.ContactRepositoryBridge
import com.pukaar.app.ui.screen.contacts.ContactType

object EmergencyAlertHelper {

    private const val TAG = "PUKAAR_ALERT"

    fun buildAlertMessage(
        context: Context,
        userName: String?,
        userPhone: String?,
        isSos: Boolean,
        isMockDrill: Boolean,
        latitude: Double?,
        longitude: Double?,
        event: EmergencyDto? = null,
        otherContacts: List<Pair<String, String>> = emptyList()
    ): String {
        val who = userName?.takeIf { it.isNotBlank() } ?: "PUKAAR user"
        val phone = userPhone?.takeIf { it.isNotBlank() } ?: ""
        val prefix = when {
            isMockDrill -> "PUKAAR TEST ALERT"
            isSos -> "PUKAAR EMERGENCY SOS"
            else -> "PUKAAR HELP REQUEST"
        }
        val sb = StringBuilder()
        sb.append("$prefix\n$who")
        if (phone.isNotBlank()) sb.append(" ($phone)")
        sb.append("\n")
        when {
            isMockDrill -> sb.append("Practice drill.\n")
            isSos -> sb.append("MAY BE IN DANGER — call immediately.\n")
            else -> sb.append("Needs assistance — call immediately.\n")
        }
        if (latitude != null && longitude != null) {
            sb.append("Location: https://maps.google.com/?q=$latitude,$longitude\n")
        } else {
            sb.append("Location: unavailable — call now.\n")
        }
        val battery = event?.batteryPct ?: DeviceTelemetry.batteryPercent(context)
        val network = event?.networkType ?: DeviceTelemetry.networkType(context)
        battery?.let { sb.append("Battery: $it%\n") }
        sb.append("Network: $network\n")

        val contacts = otherContacts
        if (contacts.isNotEmpty()) {
            sb.append("Other contacts:\n")
            contacts.take(5).forEach { (name, phone) ->
                sb.append("- $name $phone\n")
            }
        }
        sb.append("Emergency: 112\n")
        event?.nearestAmbulance?.let { a ->
            sb.append("Ambulance: ${a.name ?: ""} ${a.phone ?: "108"}\n")
        } ?: sb.append("Ambulance: 108\n")
        event?.policeStation?.let { ps ->
            sb.append("Police: ${ps.name ?: ""}")
            ps.address?.takeIf { it.isNotBlank() }?.let { sb.append(" ($it)") }
            ps.phone?.takeIf { it.isNotBlank() }?.let { sb.append(" $it") }
            sb.append("\n")
        }
        event?.nearestHospital?.let { h ->
            sb.append("Hospital: ${h.name ?: ""}")
            h.address?.takeIf { it.isNotBlank() }?.let { sb.append(" ($it)") }
            h.phone?.takeIf { it.isNotBlank() }?.let { sb.append(" $it") }
            sb.append("\n")
        }
        sb.append("Install PUKAAR High Alert for grabbing alerts: https://play.google.com/store/apps/details?id=com.pukaar.highalert")
        return sb.toString().trim()
    }

    suspend fun sendSmsToContactsInBackground(
        context: Context,
        event: EmergencyDto,
        isSos: Boolean,
        isMockDrill: Boolean
    ): SmsHelper.SendResult {
        val contacts = runCatching { ContactRepositoryBridge.loadContacts() }.getOrNull().orEmpty()
        // SMS restrictions: only verified contacts for the matching alert type
        val relevant = contacts.filter { it.verified }.filter {
            when {
                isSos || isMockDrill -> it.type == ContactType.SOS
                else -> it.type == ContactType.HELP || it.type == ContactType.INACTIVITY
            }
        }
        if (relevant.isEmpty()) {
            Log.w(TAG, "No verified contacts for this alert type — SMS skipped")
            return SmsHelper.SendResult(0, 0, emptyList())
        }

        var enriched = runCatching {
            NearbyServicesHelper.enrich(event, context)
        }.getOrDefault(NearbyServicesHelper.withNationalFallbacks(event))


        val message = buildAlertMessage(
            context = context,
            userName = enriched.userName,
            userPhone = enriched.userPhone,
            isSos = isSos,
            isMockDrill = isMockDrill,
            latitude = enriched.latitude,
            longitude = enriched.longitude,
            event = enriched,
            otherContacts = relevant.map { it.name to it.phoneNumber }
        )

        val numbers = relevant.map { it.phoneNumber }.filter { it.isNotBlank() }.distinct()
        val result = SmsHelper.sendSmsWithFallback(context, numbers, message)
        Log.i(TAG, "Device SMS: sent=${result.sent} failed=${result.failed} to ${numbers.size} verified contact(s)")
        return result
    }

    suspend fun sendSafeSmsToContacts(context: Context, userName: String): SmsHelper.SendResult {
        val contacts = runCatching { ContactRepositoryBridge.loadContacts() }.getOrNull().orEmpty()
        val relevant = contacts.filter {
            it.type == ContactType.SOS || it.type == ContactType.HELP ||
                it.type == ContactType.DOCTOR || it.type == ContactType.NEIGHBOUR
        }
        if (relevant.isEmpty()) {
            Log.w(TAG, "No contacts for safe SMS")
            return SmsHelper.SendResult(0, 0, emptyList())
        }
        val who = userName.ifBlank { "PUKAAR user" }
        val message =
            "Good news! $who is safe now.\nThe earlier safety alert has been closed. Please don't worry. ❤️\n\nTeam Pukaar"
        val numbers = relevant.map { it.phoneNumber }.filter { it.isNotBlank() }.distinct()
        val result = SmsHelper.sendSmsInBackground(context, numbers, message)
        Log.i(TAG, "Safe SMS: sent=${result.sent} failed=${result.failed}")
        return result
    }

    fun call112InBackground(context: Context) {
        val hasCall = androidx.core.content.ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.CALL_PHONE
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        try {
            val intent = if (hasCall) {
                android.content.Intent(android.content.Intent.ACTION_CALL).apply {
                    data = android.net.Uri.parse("tel:112")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                    data = android.net.Uri.parse("tel:112")
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not call/dial 112", e)
        }
    }
}
