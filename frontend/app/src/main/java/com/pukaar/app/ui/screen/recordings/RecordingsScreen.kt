package com.pukaar.app.ui.screen.recordings

import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.PukaarApp
import com.pukaar.app.R
import com.pukaar.app.data.api.AudioSegmentDto
import com.pukaar.app.data.api.EmergencyDto
import com.pukaar.app.ui.component.PukaarScreen
import com.pukaar.app.ui.component.SectionCard
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.SuccessGreen
import com.pukaar.app.ui.theme.SurfaceElevated
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary
import com.pukaar.app.ui.theme.TextTertiary
import com.pukaar.app.util.userMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun RecordingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var events by remember { mutableStateOf<List<EmergencyDto>>(emptyList()) }
    var playingKey by remember { mutableStateOf<String?>(null) }
    var bufferingKey by remember { mutableStateOf<String?>(null) }
    var positionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val player = remember { MediaPlayer() }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                if (player.isPlaying) player.stop()
                player.reset()
                player.release()
            }
        }
    }

    LaunchedEffect(playingKey) {
        while (isActive && playingKey != null) {
            runCatching {
                positionMs = player.currentPosition.coerceAtLeast(0)
                val d = player.duration
                if (d > 0) durationMs = d
            }
            delay(200)
        }
    }

    LaunchedEffect(Unit) {
        loading = true
        error = null
        try {
            val resp = PukaarApp.instance.repository.emergencyHistory()
            events = (resp.items ?: emptyList()).filter { e ->
                e.audioSegments?.any { it.cloudSafe == true } == true
            }
        } catch (e: Exception) {
            error = e.userMessage()
        } finally {
            loading = false
        }
    }

    fun stopPlayback() {
        runCatching {
            if (player.isPlaying) player.stop()
            player.reset()
        }
        playingKey = null
        bufferingKey = null
        positionMs = 0
        durationMs = 0
    }

    fun playSegment(eventId: String, segment: AudioSegmentDto) {
        val segmentId = segment.id ?: return
        val key = "$eventId:$segmentId"
        if (playingKey == key) {
            stopPlayback()
            return
        }
        scope.launch {
            stopPlayback()
            bufferingKey = key
            try {
                val cacheDir = File(context.cacheDir, "recordings").apply { mkdirs() }
                val out = File(cacheDir, "$segmentId.m4a")
                if (!out.exists() || out.length() < 1024L) {
                    withContext(Dispatchers.IO) {
                        out.delete()
                        val body = PukaarApp.instance.repository.downloadAudioSegment(eventId, segmentId)
                        body.byteStream().use { input ->
                            out.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
                if (!out.exists() || out.length() < 1024L) {
                    error = "Recording file is empty or incomplete"
                    bufferingKey = null
                    return@launch
                }
                val metaDuration = withContext(Dispatchers.IO) { readDurationMs(out) }
                withContext(Dispatchers.Main) {
                    player.reset()
                    player.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    player.setDataSource(out.absolutePath)
                    player.prepare()
                    var resolved = player.duration
                    if (resolved <= 0 && metaDuration > 0) resolved = metaDuration
                    if (resolved <= 0) {
                        val fallbackSec = (segment.durationSec ?: 60).coerceAtLeast(1)
                        resolved = fallbackSec * 1000
                    }
                    durationMs = resolved
                    positionMs = 0
                    player.setOnCompletionListener {
                        playingKey = null
                        positionMs = durationMs
                    }
                    player.start()
                    playingKey = key
                    bufferingKey = null
                }
            } catch (e: Exception) {
                bufferingKey = null
                error = e.userMessage()
            }
        }
    }

    PukaarScreen(
        title = stringResource(R.string.recordings_title),
        onBack = {
            stopPlayback()
            onBack()
        },
        modifier = modifier
    ) {
        when {
            loading -> {
                Column(
                    Modifier.fillMaxWidth().padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = PukaarRed)
                }
            }
            error != null && events.isEmpty() -> {
                Text(error!!, color = PukaarRed, fontSize = 13.sp)
            }
            events.isEmpty() -> {
                SectionCard {
                    Text(
                        text = stringResource(R.string.recordings_empty),
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
            else -> {
                Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    error?.let { Text(it, color = PukaarRed, fontSize = 12.sp) }
                    events.forEach { event ->
                        val eventId = event.id ?: return@forEach
                        val segments = event.audioSegments?.filter { it.cloudSafe == true }.orEmpty()
                        if (segments.isEmpty()) return@forEach
                        SectionCard {
                            Text(
                                text = when {
                                    event.mockDrill == true -> stringResource(R.string.recordings_type_drill)
                                    event.triggerType == "HELP" -> stringResource(R.string.recordings_type_help)
                                    else -> stringResource(R.string.recordings_type_sos)
                                },
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = (event.startedAt ?: "").take(19).replace('T', ' '),
                                color = TextTertiary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp, bottom = 10.dp)
                            )
                            segments.sortedBy { it.index ?: 0 }.forEach { seg ->
                                val key = "$eventId:${seg.id}"
                                val isPlaying = playingKey == key
                                val listedSec = (seg.durationSec ?: 60).coerceAtLeast(1)
                                val totalMs = if (isPlaying && durationMs > 0) durationMs else listedSec * 1000
                                val progress = if (isPlaying && totalMs > 0) {
                                    (positionMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
                                } else {
                                    0f
                                }
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(SurfaceElevated, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(
                                                R.string.recordings_segment,
                                                (seg.index ?: 0) + 1
                                            ),
                                            color = TextPrimary,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = if (isPlaying) {
                                                "${formatMmSs(positionMs)} / ${formatMmSs(totalMs)}"
                                            } else {
                                                "${formatMmSs(listedSec * 1000)} · uploaded"
                                            },
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                        if (isPlaying) {
                                            LinearProgressIndicator(
                                                progress = { progress },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(top = 6.dp),
                                                color = SuccessGreen,
                                                trackColor = TextTertiary.copy(alpha = 0.3f)
                                            )
                                        }
                                    }
                                    when {
                                        bufferingKey == key -> CircularProgressIndicator(
                                            Modifier.size(22.dp),
                                            color = SuccessGreen,
                                            strokeWidth = 2.dp
                                        )
                                        else -> IconButton(onClick = { playSegment(eventId, seg) }) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                contentDescription = null,
                                                tint = SuccessGreen
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

private fun formatMmSs(ms: Int): String {
    val totalSec = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0).toLong())
    val m = totalSec / 60
    val s = totalSec % 60
    return String.format(Locale.US, "%d:%02d", m, s)
}

private fun readDurationMs(file: File): Int {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(file.absolutePath)
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            ?.toIntOrNull()
            ?.coerceAtLeast(0)
            ?: 0
    } catch (_: Exception) {
        0
    } finally {
        runCatching { retriever.release() }
    }
}
