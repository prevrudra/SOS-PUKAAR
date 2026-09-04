package com.pukaar.app.ui.screen.recordings

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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
                if (!out.exists() || out.length() == 0L) {
                    withContext(Dispatchers.IO) {
                        val body = PukaarApp.instance.repository.downloadAudioSegment(eventId, segmentId)
                        body.byteStream().use { input ->
                            out.outputStream().use { output -> input.copyTo(output) }
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    player.reset()
                    player.setDataSource(out.absolutePath)
                    player.setOnCompletionListener { playingKey = null }
                    player.prepare()
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
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(SurfaceElevated, RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
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
                                            text = "${seg.durationSec ?: 60}s · uploaded",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    when {
                                        bufferingKey == key -> CircularProgressIndicator(
                                            Modifier.size(22.dp),
                                            color = SuccessGreen,
                                            strokeWidth = 2.dp
                                        )
                                        else -> IconButton(onClick = { playSegment(eventId, seg) }) {
                                            Icon(
                                                imageVector = if (playingKey == key) Icons.Filled.Pause else Icons.Filled.PlayArrow,
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
