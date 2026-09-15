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

    @GET("api/v1/alert-devices/events/{eventId}")
    suspend fun eventSnapshot(@Path("eventId") eventId: String): PendingAlertResponse

    @POST("api/v1/alert-devices/acknowledge")
    suspend fun acknowledge(@Body body: AcknowledgeRequest): Map<String, Any?>
}

@JsonClass(generateAdapter = true)
data class OtpRequest(val phone: String)

@JsonClass(generateAdapter = true)
data class OtpVerifyRequest(val phone: String, val code: String, val deviceId: String? = null)

@JsonClass(generateAdapter = true)
data class AuthResponse(val accessToken: String?, val user: Map<String, Any?>? = null)

@JsonClass(generateAdapter = true)
data class RegisterDeviceRequest(val phone: String, val fcmToken: String? = null, val deviceId: String, val platform: String = "ANDROID")

@JsonClass(generateAdapter = true)
data class AcknowledgeRequest(val eventId: String, val status: String = "READ")

@JsonClass(generateAdapter = true)
data class AlertContactDto(
    val name: String? = null,
    val phone: String? = null,
    val role: String? = null,
    val relationship: String? = null,
    val status: String? = null
)

@JsonClass(generateAdapter = true)
data class PendingAlertResponse(
    val active: Boolean? = false,
    val eventId: String? = null,
    val victimName: String? = null,
    val victimPhone: String? = null,
    val victimSubtitle: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationLabel: String? = null,
    val batteryPct: Int? = null,
    val networkType: String? = null,
    val mockDrill: Boolean? = false,
    val triggerType: String? = null,
    val startedAt: String? = null,
    val policeName: String? = null,
    val policePhone: String? = null,
    val policeAddress: String? = null,
    val hospitalName: String? = null,
    val hospitalPhone: String? = null,
    val hospitalAddress: String? = null,
    val ambulanceName: String? = null,
    val ambulancePhone: String? = null,
    val ambulanceAddress: String? = null,
    val trustedContacts: List<AlertContactDto>? = null,
    val helpNumbers: List<AlertContactDto>? = null
)
