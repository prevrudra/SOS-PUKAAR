package com.pukaar.app.util

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException

fun Throwable.userMessage(): String {
    when (this) {
        is HttpException -> {
            val raw = try {
                response()?.errorBody()?.string()
            } catch (_: Exception) {
                null
            }
            if (!raw.isNullOrBlank()) {
                try {
                    val json = JSONObject(raw)
                    val message = json.optString("message")
                    val code = json.optString("code")
                    if (message.isNotBlank()) {
                        return if (code.isNotBlank()) "$message ($code)" else message
                    }
                } catch (_: Exception) {
                    // fall through
                }
            }
            return when (code()) {
                401 -> "Session expired. Please verify OTP again."
                403 -> "Access denied."
                404 -> "Not found."
                409 -> "Already in progress."
                else -> "Request failed (${code()})"
            }
        }
        is IOException -> return "Network error. Check Wi‑Fi and that the backend is running."
        else -> return message?.takeIf { it.isNotBlank() } ?: "Something went wrong"
    }
}
