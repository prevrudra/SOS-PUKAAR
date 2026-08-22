package com.pukaar.app.data.api

data class OtpRequest(val phone: String)
data class OtpVerifyRequest(val phone: String, val code: String, val deviceId: String? = null, val referralCode: String? = null)
data class AuthResponse(
    val accessToken: String?,
    val refreshToken: String?,
    val user: UserDto?,
    val phone: String? = null,
    val expiresInSeconds: Int? = null,
    val devCode: String? = null
)
data class UserDto(
    val id: String?,
    val phone: String?,
    val fullName: String?,
    val languageCode: String?,
    val homeMode: String?,
    val onboardingComplete: Boolean?,
    val mockDrillPassed: Boolean?,
    val protectionReady: Boolean?,
    val referralCode: String?
)
data class ProfileUpdateRequest(
    val fullName: String? = null,
    val languageCode: String? = null,
    val homeMode: String? = null,
    val consentLocation: Boolean? = null,
    val consentAudio: Boolean? = null,
    val consentTerms: Boolean? = null
)
data class ContactDto(
    val id: String?,
    val name: String?,
    val phone: String?,
    val role: String?,
    val relationship: String?,
    val priorityOrder: Int?
)
data class ContactRequest(
    val name: String,
    val phone: String,
    val role: String = "SOS_TRUSTED",
    val relationship: String? = null,
    val priorityOrder: Int = 1
)
data class TriggerRequest(
    val triggerType: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyM: Double? = null,
    val mockDrill: Boolean = false
)
data class EmergencyDto(
    val active: Boolean? = null,
    val id: String? = null,
    val triggerType: String? = null,
    val status: String? = null,
    val mockDrill: Boolean? = null,
    val mockDrillId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val call112Status: String? = null,
    val deliveries: List<DeliveryDto>? = null,
    val audioSegments: List<AudioSegmentDto>? = null,
    val policeStation: PoliceDto? = null
)
data class DrillCompleteRequest(val contactsConfirmed: Boolean = true, val notes: String? = null)
data class DeliveryDto(val name: String?, val phone: String?, val status: String?)
data class AudioSegmentDto(val id: String?, val index: Int?, val uploadStatus: String?, val cloudSafe: Boolean?)
data class PoliceDto(val name: String?, val phone: String?, val phoneVerified: Boolean?, val address: String?)
data class LocationRequest(val latitude: Double, val longitude: Double, val accuracyM: Double? = null)
data class SegmentRequest(val index: Int, val checksumSha256: String? = null, val byteSize: Long? = null)
data class UploadConfirmRequest(val storageKey: String)
data class SegmentResponse(val segmentId: String?, val uploadStatus: String?, val cloudSafe: Boolean?, val message: String?)
data class ActivateRequest(val plan: String = "INDIVIDUAL", val purchaseToken: String? = "dev-token", val storePlatform: String = "PLAY")
data class PlansDto(
    val individual: Int? = null,
    val family: Int? = null,
    val referralFamily: Int? = null,
    val referralsRequired: Int? = null
)
data class SubscriptionDto(
    val id: String? = null,
    val plan: String? = null,
    val status: String? = null,
    val priceInr: Int? = null,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val graceEndsAt: String? = null,
    val alreadyActive: Boolean? = null
)
data class SubscriptionStatusResponse(
    val plans: PlansDto? = null,
    val successfulReferrals: Long? = null,
    val eligibleForReferralFamilyPrice: Boolean? = null,
    val subscription: SubscriptionDto? = null
)
data class OkResponse(val ok: Boolean? = true, val deleted: Boolean? = null, val lastActivityAt: String? = null)
data class ElderlySettingsDto(
    val softHours: Int? = null,
    val mediumHours: Int? = null,
    val urgentHours: Int? = null,
    val escalationMinutes: Int? = null,
    val inactivityMonitoringEnabled: Boolean? = null,
    val ambulanceNumber: String? = null,
    val doctorName: String? = null,
    val doctorPhone: String? = null
)
data class DrillCompleteResponse(
    val result: String? = null,
    val protectionReady: Boolean? = null,
    val failureNotes: String? = null
)
