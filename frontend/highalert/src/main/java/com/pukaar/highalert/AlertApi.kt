package com.pukaar.highalert

import com.squareup.moshi.JsonClass
import retrofit2.http.*

interface AlertApi {
    @POST("api/v1/auth/otp/request")
    suspend fun requestOtp(@Body body: OtpRequest): Map<String, Any?>

    @POST("api/v1/auth/otp/verify")
    suspend fun verifyOtp(@Body body: OtpVerifyRequest): AuthResponse

    @POST("api/v1/alert-devices/register")
    suspend fun registerDevice(@Body body: RegisterDeviceRequest): Map<String, Any?>

    @GET("api/v1/alert-devices/pending")
    suspend fun pendingAlert(): PendingAlertResponse
}

@JsonClass(generateAdapter = true)
data class OtpRequest(val phone: String)

@JsonClass(generateAdapter = true)
data class OtpVerifyRequest(val phone: String, val code: String, val deviceId: String = "highalert")

@JsonClass(generateAdapter = true)
data class AuthResponse(val accessToken: String?, val user: Map<String, Any?>? = null)

@JsonClass(generateAdapter = true)
data class RegisterDeviceRequest(val phone: String, val fcmToken: String? = null, val deviceId: String, val platform: String = "ANDROID")

@JsonClass(generateAdapter = true)
data class PendingAlertResponse(
    val active: Boolean? = false,
    val eventId: String? = null,
    val victimName: String? = null,
    val victimPhone: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val batteryPct: Int? = null,
    val networkType: String? = null,
    val mockDrill: Boolean? = false,
    val triggerType: String? = null
)
