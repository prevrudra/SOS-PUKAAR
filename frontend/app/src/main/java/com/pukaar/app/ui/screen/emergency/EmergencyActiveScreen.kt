package com.pukaar.app.ui.screen.emergency

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.data.api.EmergencyDto
import com.pukaar.app.ui.theme.Black
import com.pukaar.app.ui.theme.PukaarOrange
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.SuccessGreen
import com.pukaar.app.ui.theme.SurfaceElevated
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary

@Composable
fun EmergencyActiveScreen(
    event: EmergencyDto?,
    isMockDrill: Boolean,
    onMarkSafe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        Text(
            text = when {
                isMockDrill -> stringResource(R.string.emergency_mock_drill_active)
                event?.triggerType == "HELP" -> stringResource(R.string.emergency_help_active)
                else -> stringResource(R.string.emergency_sos_active)
            }.uppercase(),
            color = if (isMockDrill) PukaarOrange else PukaarRed,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            text = event?.status?.replace('_', ' ') ?: "ACTIVE",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (event?.latitude != null && event.longitude != null) {
                InfoCard(
                    title = stringResource(R.string.emergency_location),
                    body = "${event.latitude}, ${event.longitude}"
                )
            }

            val battery = event?.batteryPct
            val network = event?.networkType
            if (battery != null || network != null) {
                InfoCard(
                    title = stringResource(R.string.emergency_device_status),
                    body = buildString {
                        battery?.let { append("Battery: $it%\n") }
                        network?.let { append("Network: $it") }
                    }.trim()
                )
            }

            event?.deliveries?.takeIf { it.isNotEmpty() }?.let { deliveries ->
                val pendingLabel = stringResource(R.string.emergency_delivery_pending)
                val sentLabel = stringResource(R.string.emergency_delivery_sent)
                val deliveredLabel = stringResource(R.string.emergency_delivery_delivered)
                val readLabel = stringResource(R.string.emergency_delivery_read)
                val failedLabel = stringResource(R.string.emergency_delivery_failed)
                InfoCard(
                    title = stringResource(R.string.emergency_contacts_notified),
                    body = deliveries.joinToString("\n") { d ->
                        val label = when (d.status?.uppercase()) {
                            "SENT" -> sentLabel
                            "DELIVERED" -> deliveredLabel
                            "READ" -> readLabel
                            "FAILED" -> failedLabel
                            else -> pendingLabel
                        }
                        "${d.name ?: "?"} (${d.phone ?: "?"}) — $label"
                    }
                )
            }

            event?.policeStation?.let { ps ->
                InfoCard(
                    title = stringResource(R.string.emergency_police),
                    body = buildString {
                        ps.name?.let { append(it) }
                        ps.address?.let { append("\n$it") }
                        ps.phone?.let { append("\n$it") }
                    },
                    phone = ps.phone
                )
            }

            event?.nearestHospital?.let { h ->
                InfoCard(
                    title = stringResource(R.string.emergency_hospital),
                    body = buildString {
                        h.name?.let { append(it) }
                        h.address?.let { append("\n$it") }
                        h.phone?.let { append("\n$it") }
                    },
                    phone = h.phone
                )
            }

            event?.nearestAmbulance?.let { a ->
                InfoCard(
                    title = stringResource(R.string.emergency_ambulance),
                    body = buildString {
                        a.name?.let { append(it) }
                        a.address?.let { append("\n$it") }
                        a.phone?.let { append("\n$it") }
                    },
                    phone = a.phone
                )
            }

            event?.nearbySource?.takeIf { it.isNotBlank() }?.let { src ->
                Text(
                    text = "Nearby via $src",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }

            if (!isMockDrill && event?.call112Status != null) {
                InfoCard(
                    title = stringResource(R.string.emergency_112),
                    body = event.call112Status ?: "INITIATED"
                )
            }

            event?.audioSegments?.let { segments ->
                val uploaded = segments.count { it.cloudSafe == true }
                InfoCard(
                    title = stringResource(R.string.emergency_audio),
                    body = if (segments.isEmpty()) {
                        stringResource(R.string.emergency_audio_starting)
                    } else {
                        stringResource(R.string.emergency_audio_segments_detail, segments.size, uploaded)
                    }
                )
            }
        }

        if (!isMockDrill) {
            Button(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:112")))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SurfaceElevated),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.emergency_call_112).uppercase(),
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = onMarkSafe,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isMockDrill) SuccessGreen else PukaarOrange
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = stringResource(
                    if (isMockDrill) R.string.emergency_finish_drill else R.string.emergency_im_safe
                ).uppercase(),
                color = Color.Black,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp
            )
        }

        if (!isMockDrill) {
            Text(
                text = stringResource(R.string.emergency_im_safe_hint),
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.emergency_disclaimer),
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun InfoCard(title: String, body: String, phone: String? = null) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceElevated, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(text = title.uppercase(), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = body, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
        if (!phone.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                },
                colors = ButtonDefaults.buttonColors(containerColor = PukaarRed),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(stringResource(R.string.emergency_call_place).uppercase(), fontWeight = FontWeight.Bold)
            }
        }
    }
}
