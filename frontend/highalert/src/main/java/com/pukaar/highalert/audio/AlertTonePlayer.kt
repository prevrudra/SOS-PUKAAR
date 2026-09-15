package com.pukaar.highalert.audio

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pukaar.highalert.data.AlertTone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Tone every device's tone table carries. Used when the level's preferred tone turns out
 * not to exist on this hardware.
 */
private const val FALLBACK_TONE = ToneGenerator.TONE_PROP_BEEP

/**
 * Plays a preview of an [AlertTone] on the alarm stream so it is audible even when the
 * ringer is down — the same stream a real incoming alert will use.
 *
 * Every tone already plays at ToneGenerator's maximum, which is only a *percentage* of the
 * alarm stream, so the stream itself is pushed to full for the length of playback and put
 * back afterwards — an alert nobody hears is an alert that failed.
 *
 * Only one tone plays at a time; starting another stops the previous one.
 */
class AlertTonePlayer(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val audioManager =
        appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private var playbackJob: Job? = null
    private var toneGenerator: ToneGenerator? = null

    /** The listener's own alarm level, held while it is overridden. Null when untouched. */
    private var previousAlarmVolume: Int? = null

    /** The tone currently previewing, or null when silent. Observable from Compose. */
    var playingTone: AlertTone? by mutableStateOf(null)
        private set

    fun toggle(tone: AlertTone) {
        if (playingTone == tone) stop() else play(tone)
    }

    fun play(tone: AlertTone) {
        stop()
        raiseAlarmVolume()
        val generator = try {
            ToneGenerator(AudioManager.STREAM_ALARM, tone.volume)
        } catch (e: RuntimeException) {
            // Audio hardware busy — nothing to preview, keep the UI in a silent state.
            restoreAlarmVolume()
            return
        }
        toneGenerator = generator
        playingTone = tone

        playbackJob = scope.launch {
            repeat(tone.bursts) { burst ->
                if (tone.vibrateMillis > 0) vibrate(tone.vibrateMillis)
                repeat(tone.beepsPerBurst) {
                    generator.startBeep(tone)
                    delay(tone.beepMillis + tone.gapMillis)
                }
                if (burst < tone.bursts - 1) delay(tone.burstGapMillis)
            }
            stop()
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        toneGenerator?.let {
            it.stopTone()
            it.release()
        }
        toneGenerator = null
        cancelVibration()
        restoreAlarmVolume()
        playingTone = null
    }

    /**
     * Sound one beep of [tone].
     *
     * A device whose tone table lacks the requested tone reports that by returning false
     * from startTone rather than by throwing, which is why the result is not ignored: the
     * CDMA tones used by levels 2 and 3 are not present on every build, and silently
     * dropping every beep is how a level ends up completely silent on one handset while
     * working on another. Falling back keeps the level's pace and vibration intact.
     */
    private fun ToneGenerator.startBeep(tone: AlertTone) {
        val duration = tone.beepMillis.toInt()
        if (startTone(tone.toneType, duration)) return
        startTone(FALLBACK_TONE, duration)
    }

    /**
     * Take the alarm stream to full, remembering what it was. Silently does nothing on a
     * device with a fixed volume policy, or when Do Not Disturb blocks the change — the
     * tone still plays, just at whatever level the system allows.
     */
    private fun raiseAlarmVolume() {
        val manager = audioManager ?: return
        val max = manager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        val current = manager.getStreamVolume(AudioManager.STREAM_ALARM)
        if (current >= max) return
        runCatching {
            manager.setStreamVolume(AudioManager.STREAM_ALARM, max, 0)
        }.onSuccess {
            previousAlarmVolume = current
        }
    }

    /** Hand the alarm stream back at the level we found it. */
    private fun restoreAlarmVolume() {
        val manager = audioManager ?: return
        val previous = previousAlarmVolume ?: return
        previousAlarmVolume = null
        runCatching { manager.setStreamVolume(AudioManager.STREAM_ALARM, previous, 0) }
    }

    /** Call when the owning screen leaves composition. */
    fun release() {
        stop()
        scope.cancel()
    }

    private fun vibrator(): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager =
                appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    private fun vibrate(millis: Long) {
        val vibrator = vibrator()?.takeIf { it.hasVibrator() } ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(millis)
        }
    }

    private fun cancelVibration() {
        runCatching { vibrator()?.cancel() }
    }
}
