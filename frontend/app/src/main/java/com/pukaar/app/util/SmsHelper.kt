package com.pukaar.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlin.random.Random

object SmsHelper {
    fun generateVerificationCode(): String = Random.nextInt(100000, 999999).toString()

    fun buildVerificationMessage(contactName: String, code: String, senderName: String?): String {
        val from = senderName?.takeIf { it.isNotBlank() } ?: "I"
        return "$from is adding you as a PUKAAR emergency contact ($contactName). " +
                "Verification code: $code. Please save this number."
    }

    /**
     * Opens the user's default SMS app with a pre-filled message.
     * The user sends the SMS from their own number (no SEND_SMS permission needed).
     */
    fun openSmsComposer(context: Context, phoneE164: String, message: String): Boolean {
        val digits = phoneE164.filter { it.isDigit() || it == '+' }
        val uri = Uri.parse("smsto:$digits")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
            true
        } else {
            Toast.makeText(context, "No SMS app found on this device", Toast.LENGTH_LONG).show()
            false
        }
    }
}
