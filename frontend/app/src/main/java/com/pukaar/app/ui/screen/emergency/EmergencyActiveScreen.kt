package com.pukaar.app.ui.screen.emergency

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.OutlinedButton
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
    rootModifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scroll = rememberScrollState()
    val police = event?.policeStation
    val hospital = event?.nearestHospital
    val ambulance = event?.nearestAmbulance

    Column(
        modifier = rootModifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp)
    ) {
        val isHelp = !isMockDrill &&
            event?.triggerType?.equals("HELP", ignoreCase = true) == true
        Text(
            text = when {
                isMockDrill -> stringResource(R.string.emergency_mock_drill_active)
                isHelp -> stringResource(R.string.emergency_help_active)
                else -> stringResource(R.string.emergency_sos_active)
            }.uppercase(),
            color = if (isMockDrill || isHelp) PukaarOrange else PukaarRed,
            fontSize = 24.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            text = event?.status?.replace('_', ' ') ?: "ACTIVE",
            color = TextSecondary,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        Gap(16)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (event?.latitude != null && event.longitude != null) {
                ServiceCard(
                    title = stringResource(R.string.emergency_location),
                    body = String.format("%.5f, %.5f", event.latitude, event.longitude),
                    mapLat = event.latitude,
                    mapLng = event.longitude,
                    callPhone = null
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

            Text(
                text = stringResource(R.string.emergency_nearby_section).uppercase(),
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = stringResource(R.string.emergency_nearby_section_hint),
                color = TextSecondary,
                fontSize = 11.sp
            )

            ServiceCard(
                title = stringResource(R.string.emergency_police),
                body = placeBody(
                    police?.name ?: "Police Emergency",
                    police?.address,
                    police?.phone ?: "100"
                ),
                callPhone = police?.phone?.takeIf { it.isNotBlank() } ?: "100",
                mapLat = police?.latitude,
                mapLng = police?.longitude
            )

            ServiceCard(
                title = stringResource(R.string.emergency_hospital),
                body = placeBody(
                    hospital?.name ?: "Nearest Hospital",
                    hospital?.address,
                    hospital?.phone ?: "112"
                ),
                callPhone = hospital?.phone?.takeIf { it.isNotBlank() } ?: "112",
                mapLat = hospital?.latitude,
                mapLng = hospital?.longitude
            )

            ServiceCard(
                title = stringResource(R.string.emergency_ambulance),
                body = placeBody(
                    ambulance?.name ?: "National Ambulance",
                    ambulance?.address,
                    ambulance?.phone ?: "108"
                ),
                callPhone = ambulance?.phone?.takeIf { it.isNotBlank() } ?: "108",
                mapLat = ambulance?.latitude,
                mapLng = ambulance?.longitude
            )

            event?.nearbySource?.takeIf { it.isNotBlank() }?.let { src ->
                Text(
                    text = stringResource(R.string.emergency_nearby_via, src),
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

            // Audio is SOS/mock only — never show "recording" on HELP.
            if (isMockDrill || !isHelp) {
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
            Gap(8)
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

        Gap(8)
        Text(
            text = stringResource(R.string.emergency_disclaimer),
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun Gap(heightDp: Int) {
    Box(Modifier.height(heightDp.dp))
}

private fun placeBody(name: String, address: String?, phone: String): String = buildString {
    append(name)
    if (!address.isNullOrBlank()) append("\n").append(address)
    append("\n").append(phone)
}

@Composable
private fun InfoCard(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceElevated, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(text = title.uppercase(), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Gap(6)
        Text(text = body, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun ServiceCard(
    title: String,
    body: String,
    callPhone: String?,
    mapLat: Double? = null,
    mapLng: Double? = null
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceElevated, RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(text = title.uppercase(), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Gap(6)
        Text(text = body, color = TextSecondary, fontSize = 13.sp, lineHeight = 19.sp)
        Gap(10)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!callPhone.isNullOrBlank()) {
                Button(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$callPhone")))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PukaarRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(stringResource(R.string.emergency_call_place).uppercase(), fontWeight = FontWeight.Bold)
                }
            }
            if (mapLat != null && mapLng != null) {
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://maps.google.com/?q=$mapLat,$mapLng")
                            )
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        stringResource(R.string.emergency_open_map).uppercase(),
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
