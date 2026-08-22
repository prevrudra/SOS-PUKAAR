package com.pukaar.app.emergency

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import kotlinx.coroutines.delay
import java.io.File

class EmergencyAudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null

    suspend fun recordSegment(durationMs: Long): File? {
        val dir = File(context.filesDir, "evidence").apply { mkdirs() }
        val out = File(dir, "seg_${System.currentTimeMillis()}.m4a")
        return try {
            val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder = r
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(128_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(out.absolutePath)
            r.prepare()
            r.start()
            delay(durationMs)
            runCatching { r.stop() }
            r.release()
            recorder = null
            out
        } catch (_: Exception) {
            release()
            null
        }
    }

    fun release() {
        runCatching {
            recorder?.stop()
            recorder?.release()
        }
        recorder = null
    }
}
