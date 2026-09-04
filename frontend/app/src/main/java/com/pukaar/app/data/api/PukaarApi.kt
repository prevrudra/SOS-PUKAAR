package com.pukaar.app.data.api

import okhttp3.MultipartBody
import retrofit2.http.*

interface PukaarApi {
    @POST("api/v1/auth/otp/request")
    suspend fun requestOtp(@Body body: OtpRequest): AuthResponse

    @POST("api/v1/auth/otp/verify")
    suspend fun verifyOtp(@Body body: OtpVerifyRequest): AuthResponse

    @GET("api/v1/me")
    suspend fun me(): UserDto

    @PUT("api/v1/me")
    suspend fun updateMe(@Body body: ProfileUpdateRequest): UserDto

    @POST("api/v1/me/onboarding/complete")
    suspend fun completeOnboarding(): UserDto

    @GET("api/v1/contacts")
    suspend fun contacts(): List<ContactDto>

    @POST("api/v1/contacts")
    suspend fun addContact(@Body body: ContactRequest): ContactDto

    @PUT("api/v1/contacts/{id}")
    suspend fun updateContact(@Path("id") id: String, @Body body: ContactRequest): ContactDto

    @DELETE("api/v1/contacts/{id}")
    suspend fun deleteContact(@Path("id") id: String): OkResponse

    @POST("api/v1/contacts/{id}/verify")
    suspend fun verifyContact(@Path("id") id: String, @Body body: VerifyContactRequest): ContactDto

    @POST("api/v1/emergencies/trigger")
    suspend fun trigger(@Body body: TriggerRequest): EmergencyDto

    @GET("api/v1/emergencies/active")
    suspend fun activeEmergency(): EmergencyDto

    @GET("api/v1/emergencies/{id}")
    suspend fun getEmergency(@Path("id") id: String): EmergencyDto

    @POST("api/v1/emergencies/{id}/location")
    suspend fun updateLocation(@Path("id") id: String, @Body body: LocationRequest): EmergencyDto

    @POST("api/v1/emergencies/{id}/telemetry")
    suspend fun updateTelemetry(@Path("id") id: String, @Body body: TelemetryRequest): EmergencyDto

    @POST("api/v1/emergencies/{id}/safe")
    suspend fun markSafe(@Path("id") id: String, @Body body: SafeRequest = SafeRequest()): EmergencyDto

    @POST("api/v1/emergencies/{id}/audio-segments")
    suspend fun createSegment(@Path("id") id: String, @Body body: SegmentRequest): SegmentResponse

    @POST("api/v1/emergencies/{id}/audio-segments/{segmentId}/uploaded")
    suspend fun markUploaded(
        @Path("id") id: String,
        @Path("segmentId") segmentId: String,
        @Body body: UploadConfirmRequest
    ): SegmentResponse

    @Multipart
    @POST("api/v1/emergencies/{id}/audio-segments/{segmentId}/upload")
    suspend fun uploadSegment(
        @Path("id") id: String,
        @Path("segmentId") segmentId: String,
        @Part file: MultipartBody.Part
    ): SegmentResponse

    @GET("api/v1/subscription")
    suspend fun subscription(): SubscriptionStatusResponse

    @POST("api/v1/subscription/activate")
    suspend fun activate(@Body body: ActivateRequest): SubscriptionDto

    @GET("api/v1/payments/config")
    suspend fun paymentConfig(): PaymentConfigResponse

    @POST("api/v1/payments/orders")
    suspend fun createPaymentOrder(@Body body: CreatePaymentOrderRequest): PaymentOrderDto

    @POST("api/v1/payments/verify")
    suspend fun verifyPayment(@Body body: VerifyPaymentRequest): PaymentVerifyResponse

    @GET("api/v1/elderly/settings")
    suspend fun elderlySettings(): ElderlySettingsDto

    @PUT("api/v1/elderly/settings")
    suspend fun updateElderlySettings(@Body body: ElderlySettingsDto): ElderlySettingsDto

    @POST("api/v1/elderly/heartbeat")
    suspend fun heartbeat(@Body body: OkResponse = OkResponse()): OkResponse

    @POST("api/v1/emergencies/mock-drills/latest/complete")
    suspend fun completeLatestDrill(@Body body: DrillCompleteRequest): DrillCompleteResponse
}

data class SafeRequest(val reason: String? = "IM_SAFE")
