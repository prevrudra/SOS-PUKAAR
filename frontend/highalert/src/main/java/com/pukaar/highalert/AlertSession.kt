package com.pukaar.highalert

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("highalert")

class AlertSession(private val context: Context) {
    private val tokenKey = stringPreferencesKey("token")
    private val phoneKey = stringPreferencesKey("phone")

    suspend fun save(token: String, phone: String) {
        context.dataStore.edit {
            it[tokenKey] = token
            it[phoneKey] = phone
        }
    }

    suspend fun token(): String? = context.dataStore.data.map { it[tokenKey] }.first()
    suspend fun phone(): String? = context.dataStore.data.map { it[phoneKey] }.first()

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
