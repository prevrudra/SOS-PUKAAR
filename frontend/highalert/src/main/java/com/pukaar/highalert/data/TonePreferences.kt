package com.pukaar.highalert.data

import android.content.Context

/** Remembers which alert tone the user picked, across app launches. */
class TonePreferences(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun selectedTone(): AlertTone? = AlertTone.fromId(prefs.getString(KEY_TONE, null))

    fun setSelectedTone(tone: AlertTone) {
        prefs.edit().putString(KEY_TONE, tone.id).apply()
    }

    private companion object {
        const val PREFS_NAME = "pukaar_alert_prefs"
        const val KEY_TONE = "selected_tone"
    }
}
