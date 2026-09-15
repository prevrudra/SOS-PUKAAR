package com.pukaar.highalert.data

import android.media.ToneGenerator

/** ToneGenerator's maximum, i.e. the full level of the alarm stream. */
private const val FULL_VOLUME = 100

/**
 * The three selectable alert tones. Each level escalates in pace and vibration so the
 * recipient can tell the urgency apart without looking at the phone.
 *
 * Loudness is deliberately *not* part of that escalation: every level plays at FULL_VOLUME,
 * because a level 1 alert still has to be heard. Urgency is carried by the beep pattern
 * instead.
 */
enum class AlertTone(
    val id: String,
    val title: String,
    val toneType: Int,
    /** Percentage of the alarm stream's volume, 0-100. Full for every level — see above. */
    val volume: Int,
    val beepMillis: Long,
    val gapMillis: Long,
    val beepsPerBurst: Int,
    val bursts: Int,
    val burstGapMillis: Long,
    val vibrateMillis: Long
) {
    LEVEL_1(
        id = "level_1",
        title = "Alert Tone Level 1",
        toneType = ToneGenerator.TONE_PROP_BEEP2,
        volume = FULL_VOLUME,
        beepMillis = 220,
        gapMillis = 380,
        beepsPerBurst = 3,
        bursts = 3,
        burstGapMillis = 500,
        vibrateMillis = 0
    ),
    LEVEL_2(
        id = "level_2",
        title = "Alert Tone Level 2",
        toneType = ToneGenerator.TONE_CDMA_ABBR_ALERT,
        volume = FULL_VOLUME,
        beepMillis = 180,
        gapMillis = 200,
        beepsPerBurst = 4,
        bursts = 4,
        burstGapMillis = 400,
        vibrateMillis = 220
    ),
    LEVEL_3(
        id = "level_3",
        title = "Alert Tone Level 3",
        toneType = ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,
        volume = FULL_VOLUME,
        beepMillis = 150,
        gapMillis = 120,
        beepsPerBurst = 6,
        bursts = 5,
        burstGapMillis = 320,
        vibrateMillis = 400
    );

    companion object {
        fun fromId(id: String?): AlertTone? = entries.firstOrNull { it.id == id }
        val DEFAULT = LEVEL_2
    }
}
