package com.pukaar.highalert

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

/** Persists the active SOS so alarm-clock repeats can re-ring until dismissed. */
object AlertRingState {
    private val Context.ringStore: DataStore<Preferences> by preferencesDataStore("alert_ring")
    private val keyJson = stringPreferencesKey("active_alert_json")
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(PendingAlertResponse::class.java)

    fun setActive(context: Context, alert: PendingAlertResponse) {
        runBlocking {
            context.ringStore.edit { prefs ->
                prefs[keyJson] = adapter.toJson(alert)
            }
        }
    }

    fun getActive(context: Context): PendingAlertResponse? {
        return runBlocking {
            context.ringStore.data.map { prefs ->
                prefs[keyJson]?.let { adapter.fromJson(it) }
            }.first()
        }
    }

    fun clear(context: Context) {
        runBlocking {
            context.ringStore.edit { it.remove(keyJson) }
        }
    }

    fun isRinging(context: Context): Boolean = getActive(context)?.eventId != null
}
