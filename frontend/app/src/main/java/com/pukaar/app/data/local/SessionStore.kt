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
}
