package com.pukaar.highalert

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("highalert")

class AlertSession(private val context: Context) {
    private val tokenKey = stringPreferencesKey("token")
    private val phoneKey = stringPreferencesKey("phone")
    private val handledEventsKey = stringSetPreferencesKey("handled_event_ids")

    suspend fun save(token: String, phone: String) {
        context.dataStore.edit {
            it[tokenKey] = token
            it[phoneKey] = phone
        }
    }

    suspend fun token(): String? = context.dataStore.data.map { it[tokenKey] }.first()
    suspend fun phone(): String? = context.dataStore.data.map { it[phoneKey] }.first()

    /** Events already shown / dismissed — never re-fire after process restart. */
    suspend fun handledEventIds(): Set<String> =
        context.dataStore.data.map { it[handledEventsKey] ?: emptySet() }.first()

    suspend fun markEventHandled(eventId: String) {
        if (eventId.isBlank()) return
        context.dataStore.edit { prefs ->
            val next = (prefs[handledEventsKey] ?: emptySet()).toMutableSet()
            next.add(eventId)
            // Cap growth
            if (next.size > 40) {
                prefs[handledEventsKey] = next.toList().takeLast(30).toSet()
            } else {
                prefs[handledEventsKey] = next
            }
        }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
