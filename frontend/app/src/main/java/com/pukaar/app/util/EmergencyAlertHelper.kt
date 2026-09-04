package com.pukaar.app.util

import android.content.Context
import android.util.Log
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
        sb.append("Emergency: 112\nAmbulance: 108\n")
        event?.policeStation?.let { ps ->
            sb.append("Police: ${ps.name ?: ""} ${ps.phone ?: ""}\n")
        }
        event?.nearestHospital?.let { h ->
            sb.append("Hospital: ${h.name ?: ""} ${h.phone ?: ""}\n")
        }
        sb.append("Install PUKAAR High Alert app for grabbing alert.")
        return sb.toString().trim()
    }

    suspend fun sendSmsToContactsInBackground(
        context: Context,
        event: EmergencyDto,
        isSos: Boolean,
        isMockDrill: Boolean
    ): SmsHelper.SendResult {
        val contacts = runCatching { ContactRepositoryBridge.loadContacts() }.getOrNull().orEmpty()
        val relevant = contacts.filter {
            when {
                isSos || isMockDrill -> it.type == ContactType.SOS ||
                    it.type == ContactType.DOCTOR || it.type == ContactType.NEIGHBOUR
                else -> it.type == ContactType.SOS || it.type == ContactType.HELP ||
                    it.type == ContactType.DOCTOR || it.type == ContactType.NEIGHBOUR
            }
        }
        if (relevant.isEmpty()) {
            Log.w(TAG, "No contacts to alert")
            return SmsHelper.SendResult(0, 0, emptyList())
        }

        val message = buildAlertMessage(
            context = context,
            userName = event.userName,
            userPhone = event.userPhone,
            isSos = isSos,
            isMockDrill = isMockDrill,
            latitude = event.latitude,
            longitude = event.longitude,
            event = event,
            otherContacts = relevant.map { it.name to it.phoneNumber }
        )

        val numbers = relevant.map { it.phoneNumber }.filter { it.isNotBlank() }.distinct()
        val result = SmsHelper.sendSmsWithFallback(context, numbers, message)
        Log.i(TAG, "Device SMS fallback: sent=${result.sent} failed=${result.failed}")
        return result
    }

    fun call112InBackground(context: Context) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.CALL_PHONE
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_CALL).apply {
                data = android.net.Uri.parse("tel:112")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Could not call 112", e)
        }
    }
}
