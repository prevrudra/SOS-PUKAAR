package com.pukaar.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("pukaar_session")

class SessionStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("access_token")
    private val homeModeKey = stringPreferencesKey("home_mode")
    private val onboardingKey = booleanPreferencesKey("onboarding_complete")
    private val nameKey = stringPreferencesKey("full_name")
    private val phoneKey = stringPreferencesKey("phone")

    val accessToken: Flow<String?> = context.dataStore.data.map { it[tokenKey] }
    val homeMode: Flow<String> = context.dataStore.data.map { it[homeModeKey] ?: "SOS" }
    val onboardingComplete: Flow<Boolean> = context.dataStore.data.map { it[onboardingKey] ?: false }

    suspend fun token(): String? = accessToken.first()

    suspend fun saveAuth(token: String, phone: String, name: String?, homeMode: String, onboarding: Boolean) {
        context.dataStore.edit {
            it[tokenKey] = token
            it[phoneKey] = phone
            if (name != null) it[nameKey] = name
            it[homeModeKey] = homeMode
            it[onboardingKey] = onboarding
        }
    }

    suspend fun setHomeMode(mode: String) {
        context.dataStore.edit { it[homeModeKey] = mode }
    }

    suspend fun setOnboardingComplete(done: Boolean) {
        context.dataStore.edit { it[onboardingKey] = done }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
