package com.pukaar.app.util

import android.content.Intent
import android.net.Uri
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat

object SmsHelper {

    private const val TAG = "PUKAAR_SMS"

    data class SendResult(val sent: Int, val failed: Int, val numbers: List<String>) {
        val success: Boolean get() = sent > 0
    }

    fun generateVerificationCode(): String = kotlin.random.Random.nextInt(100000, 999999).toString()

    fun buildVerificationMessage(contactName: String, code: String, senderName: String?): String {
        val from = senderName?.takeIf { it.isNotBlank() } ?: "I"
        return "$from is adding you as a PUKAAR emergency contact ($contactName). " +
            "Verification code: $code. Please save this number."
    }

    fun hasSendSmsPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Sends SMS silently in the background to every number. Requires SEND_SMS permission.
     */
    fun sendSmsInBackground(context: Context, phones: List<String>, message: String): SendResult {
        if (!hasSendSmsPermission(context)) {
            Log.w(TAG, "SEND_SMS permission not granted")
            return SendResult(0, phones.size, emptyList())
        }

        val numbers = phones.map(::normalizeSmsNumber).filter { it.isNotBlank() }.distinct()
        if (numbers.isEmpty()) return SendResult(0, 0, emptyList())

        val sms = smsManager()
        val parts = sms.divideMessage(message)
        var sent = 0
        var failed = 0
        val sentNumbers = mutableListOf<String>()

        for (number in numbers) {
            val ok = try {
                if (parts.size <= 1) {
                    sms.sendTextMessage(number, null, message, null, null)
                } else {
                    sms.sendMultipartTextMessage(number, null, parts, null, null)
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send SMS to $number", e)
                false
            }
            if (ok) {
                sent++
                sentNumbers += number
                Log.i(TAG, "SMS sent to $number")
            } else {
                failed++
            }
        }
        return SendResult(sent, failed, sentNumbers)
    }

    fun sendSmsWithFallback(context: Context, phones: List<String>, message: String): SendResult {
        val background = sendSmsInBackground(context, phones, message)
        if (!background.success && phones.isNotEmpty()) {
            openSmsComposer(context, phones.first(), message)
        }
        return background
    }

    fun openSmsComposer(context: Context, phone: String, message: String) {
        val normalized = normalizeSmsNumber(phone).removePrefix("+")
        val uri = Uri.parse("smsto:$normalized")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { Log.e(TAG, "Could not open SMS composer", it) }
    }

    fun sendSmsInBackground(context: Context, phone: String, message: String): SendResult =
        sendSmsInBackground(context, listOf(phone), message)

    fun normalizeSmsNumber(phone: String): String {
        return runCatching { PhoneNumbers.toE164(phone) }.getOrElse {
            val digits = phone.filter { it.isDigit() }
            when {
                digits.isNotEmpty() -> "+$digits"
                else -> phone.trim()
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun smsManager(): SmsManager = SmsManager.getDefault()
}
