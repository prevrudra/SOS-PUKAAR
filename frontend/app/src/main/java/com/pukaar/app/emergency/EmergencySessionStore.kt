package com.pukaar.app.emergency

import android.content.Context

/** Survives process death so Emergency FGS and offline SOS can resume. */
object EmergencySessionStore {
    private const val PREFS = "pukaar_emergency_session"
    private const val KEY_EVENT_ID = "event_id"
    private const val KEY_IS_SOS = "is_sos"
    private const val KEY_MOCK = "mock"
    private const val KEY_SERVER = "server_synced"
    private const val KEY_LAT = "lat"
    private const val KEY_LNG = "lng"
    private const val KEY_RECORD_AUDIO = "record_audio"

    fun save(
        context: Context,
        eventId: String,
        isSos: Boolean,
        mockDrill: Boolean,
        serverSynced: Boolean,
        latitude: Double?,
        longitude: Double?,
        recordAudio: Boolean = isSos && !mockDrill
    ) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_EVENT_ID, eventId)
            .putBoolean(KEY_IS_SOS, isSos)
            .putBoolean(KEY_MOCK, mockDrill)
            .putBoolean(KEY_SERVER, serverSynced)
            .putBoolean(KEY_RECORD_AUDIO, recordAudio)
            .apply {
                if (latitude != null) putLong(KEY_LAT, java.lang.Double.doubleToRawLongBits(latitude))
                else remove(KEY_LAT)
                if (longitude != null) putLong(KEY_LNG, java.lang.Double.doubleToRawLongBits(longitude))
                else remove(KEY_LNG)
            }
            .apply()
    }

    fun activeEventId(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_EVENT_ID, null)

    fun isSos(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_IS_SOS, true)

    fun isMock(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_MOCK, false)

    fun serverSynced(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SERVER, false)

    fun recordAudio(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_RECORD_AUDIO, true)

    fun latitude(context: Context): Double? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_LAT)) return null
        return java.lang.Double.longBitsToDouble(prefs.getLong(KEY_LAT, 0L))
    }

    fun longitude(context: Context): Double? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_LNG)) return null
        return java.lang.Double.longBitsToDouble(prefs.getLong(KEY_LNG, 0L))
    }

    fun markServerSynced(context: Context, eventId: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_EVENT_ID, eventId)
            .putBoolean(KEY_SERVER, true)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
