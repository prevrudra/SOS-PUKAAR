package com.pukaar.app.data.repository

import android.os.Build
import com.pukaar.app.data.api.*
import com.pukaar.app.data.local.SessionStore
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class PukaarRepository(private val sessionStore: SessionStore) {
    private val api = NetworkModule.api(sessionStore)

    suspend fun requestOtp(phone: String) = api.requestOtp(OtpRequest(phone))

    suspend fun verifyOtp(phone: String, code: String, referral: String? = null): AuthResponse {
        val deviceId = Build.MODEL + "-" + Build.FINGERPRINT.take(24)
        val resp = api.verifyOtp(OtpVerifyRequest(phone, code, deviceId, referral))
        val user = resp.user
        if (resp.accessToken != null && user != null) {
            sessionStore.saveAuth(
                token = resp.accessToken,
                phone = user.phone ?: phone,
                name = user.fullName,
                homeMode = user.homeMode ?: "SOS",
                onboarding = user.onboardingComplete == true,
                protectionReady = user.protectionReady == true,
                mockDrillPassed = user.mockDrillPassed == true
            )
            user.referralCode?.let { sessionStore.saveReferralCode(it) }
        }
        return resp
    }

    suspend fun updateProfile(req: ProfileUpdateRequest): UserDto {
        val user = api.updateMe(req)
        if (user.homeMode != null) sessionStore.setHomeMode(user.homeMode)
        return user
    }

    suspend fun completeOnboarding() = api.completeOnboarding().also {
        sessionStore.setOnboardingComplete(true)
    }

    suspend fun syncSession() {
        val user = api.me()
        sessionStore.syncFromUser(user)
    }

    suspend fun me() = api.me()
    suspend fun contacts() = api.contacts()
    suspend fun addContact(req: ContactRequest) = api.addContact(req)
    suspend fun updateContact(id: String, req: ContactRequest) = api.updateContact(id, req)
    suspend fun deleteContact(id: String) = api.deleteContact(id)
    suspend fun verifyContact(id: String, code: String = "123456") =
        api.verifyContact(id, VerifyContactRequest(code))
    suspend fun emergencyHistory() = api.emergencyHistory()
    suspend fun downloadAudioSegment(eventId: String, segmentId: String) =
        api.downloadAudioSegment(eventId, segmentId)
    suspend fun trigger(req: TriggerRequest) = api.trigger(req)
    suspend fun activeEmergency() = api.activeEmergency()
    suspend fun getEmergency(id: String) = api.getEmergency(id)
    suspend fun updateLocation(id: String, lat: Double, lng: Double, acc: Double?) =
        api.updateLocation(id, LocationRequest(lat, lng, acc))
    suspend fun updateTelemetry(id: String, batteryPct: Int?, networkType: String?) =
        api.updateTelemetry(id, TelemetryRequest(batteryPct, networkType))
    suspend fun markSafe(id: String) = api.markSafe(id)
    suspend fun createSegment(id: String, index: Int, checksum: String? = null, byteSize: Long? = null) =
        api.createSegment(id, SegmentRequest(index, checksum, byteSize))

    suspend fun uploadSegment(id: String, segmentId: String, file: File): SegmentResponse {
        val body = file.asRequestBody("audio/mp4".toMediaType())
        val part = MultipartBody.Part.createFormData("file", file.name, body)
        return api.uploadSegment(id, segmentId, part)
    }

    suspend fun markUploaded(id: String, segmentId: String, key: String) =
        api.markUploaded(id, segmentId, UploadConfirmRequest(key))
    suspend fun activate(plan: String): SubscriptionDto = api.activate(ActivateRequest(plan))
    suspend fun paymentConfig() = api.paymentConfig()
    suspend fun createPaymentOrder(plan: String) = api.createPaymentOrder(CreatePaymentOrderRequest(plan))
    suspend fun verifyPayment(orderId: String, paymentId: String, signature: String) =
        api.verifyPayment(VerifyPaymentRequest(orderId, paymentId, signature))
    suspend fun subscription(): SubscriptionStatusResponse = api.subscription()
    suspend fun elderlySettings() = api.elderlySettings()
    suspend fun updateElderlySettings(settings: ElderlySettingsDto) = api.updateElderlySettings(settings)
    suspend fun heartbeat() = api.heartbeat()
    suspend fun completeLatestDrill(confirmed: Boolean = true): DrillCompleteResponse =
        api.completeLatestDrill(DrillCompleteRequest(contactsConfirmed = confirmed))
}
