package com.pukaar.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pukaar.app.data.api.UserDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("pukaar_session")

class SessionStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("access_token")
    private val homeModeKey = stringPreferencesKey("home_mode")
    private val onboardingKey = booleanPreferencesKey("onboarding_complete")
    private val protectionReadyKey = booleanPreferencesKey("protection_ready")
    private val mockDrillPassedKey = booleanPreferencesKey("mock_drill_passed")
    private val nameKey = stringPreferencesKey("full_name")
    private val phoneKey = stringPreferencesKey("phone")
    private val sosAutoCallKey = booleanPreferencesKey("sos_auto_call")
    private val sosLocationKey = booleanPreferencesKey("sos_location")
    private val sosAudioKey = booleanPreferencesKey("sos_audio")
    private val sosAlertContactsKey = booleanPreferencesKey("sos_alert_contacts")
    private val notifEmergencyKey = booleanPreferencesKey("notif_emergency")
    private val notifDrillKey = booleanPreferencesKey("notif_drill")
    private val notifMarketingKey = booleanPreferencesKey("notif_marketing")
    private val doctorPhoneKey = stringPreferencesKey("doctor_phone")
    private val referralCodeKey = stringPreferencesKey("referral_code")

    val accessToken: Flow<String?> = context.dataStore.data.map { it[tokenKey] }
    val homeMode: Flow<String> = context.dataStore.data.map { it[homeModeKey] ?: "SOS" }
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[onboardingKey] ?: false }
    val protectionReady: Flow<Boolean> = context.dataStore.data.map { it[protectionReadyKey] ?: false }

    suspend fun token(): String? = accessToken.first()

    suspend fun saveAuth(
        token: String,
        phone: String,
        name: String?,
        homeMode: String,
        onboarding: Boolean,
        protectionReady: Boolean = false,
        mockDrillPassed: Boolean = false
    ) {
        context.dataStore.edit {
            it[tokenKey] = token
            it[phoneKey] = phone
            if (name != null) it[nameKey] = name
            it[homeModeKey] = homeMode
            it[onboardingKey] = onboarding
            it[protectionReadyKey] = protectionReady
            it[mockDrillPassedKey] = mockDrillPassed
        }
    }

    suspend fun syncFromUser(user: UserDto) {
        context.dataStore.edit {
            user.fullName?.let { n -> it[nameKey] = n }
            user.phone?.let { p -> it[phoneKey] = p }
            user.homeMode?.let { m -> it[homeModeKey] = m }
            it[onboardingKey] = user.onboardingComplete == true
            it[protectionReadyKey] = user.protectionReady == true
            it[mockDrillPassedKey] = user.mockDrillPassed == true
            user.referralCode?.let { c -> it[referralCodeKey] = c }
        }
    }

    suspend fun setHomeMode(mode: String) {
        context.dataStore.edit { it[homeModeKey] = mode }
    }

    suspend fun setOnboardingComplete(done: Boolean) {
        context.dataStore.edit { it[onboardingKey] = done }
    }

    suspend fun setProtectionReady(ready: Boolean) {
        context.dataStore.edit { it[protectionReadyKey] = ready }
    }

    suspend fun setMockDrillPassed(passed: Boolean) {
        context.dataStore.edit { it[mockDrillPassedKey] = passed }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }

    suspend fun saveSosSettings(autoCall: Boolean, location: Boolean, audio: Boolean, alertContacts: Boolean) {
        context.dataStore.edit {
            it[sosAutoCallKey] = autoCall
            it[sosLocationKey] = location
            it[sosAudioKey] = audio
            it[sosAlertContactsKey] = alertContacts
        }
    }

    suspend fun sosSettings(): SosSettingsPrefs {
        val prefs = context.dataStore.data.first()
        return SosSettingsPrefs(
            autoCall = prefs[sosAutoCallKey] ?: true,
            location = prefs[sosLocationKey] ?: true,
            audio = prefs[sosAudioKey] ?: true,
            alertContacts = prefs[sosAlertContactsKey] ?: true
        )
    }

    suspend fun saveNotificationPrefs(emergency: Boolean, drill: Boolean, marketing: Boolean) {
        context.dataStore.edit {
            it[notifEmergencyKey] = emergency
            it[notifDrillKey] = drill
            it[notifMarketingKey] = marketing
        }
    }

    suspend fun notificationPrefs(): NotificationPrefs {
        val prefs = context.dataStore.data.first()
        return NotificationPrefs(
            emergency = prefs[notifEmergencyKey] ?: true,
            drill = prefs[notifDrillKey] ?: true,
            marketing = prefs[notifMarketingKey] ?: false
        )
    }

    suspend fun saveDoctorPhone(phone: String) {
        context.dataStore.edit { it[doctorPhoneKey] = phone }
    }

    suspend fun doctorPhone(): String = context.dataStore.data.first()[doctorPhoneKey] ?: ""

    suspend fun saveReferralCode(code: String) {
        context.dataStore.edit { it[referralCodeKey] = code }
    }

    suspend fun referralCode(): String = context.dataStore.data.first()[referralCodeKey] ?: ""
}

data class SosSettingsPrefs(
    val autoCall: Boolean,
    val location: Boolean,
    val audio: Boolean,
    val alertContacts: Boolean
)

data class NotificationPrefs(
    val emergency: Boolean,
    val drill: Boolean,
    val marketing: Boolean
)
