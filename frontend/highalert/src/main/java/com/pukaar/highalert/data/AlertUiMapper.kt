package com.pukaar.highalert.data

import com.pukaar.highalert.AlertContactDto
import com.pukaar.highalert.PendingAlertResponse
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Maps live High Alert API payloads into Ritik's SosAlert UI model. */
object AlertUiMapper {
    private val dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
    private val timeFmt = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)
    private val ist = ZoneId.of("Asia/Kolkata")

    fun fromPending(alert: PendingAlertResponse, mockDrill: Boolean = alert.mockDrill == true): SosAlert {
        val name = alert.victimName?.takeIf { it.isNotBlank() } ?: "Someone"
        val phone = alert.victimPhone.orEmpty()
        val lat = alert.latitude ?: 0.0
        val lng = alert.longitude ?: 0.0
        val (dateLabel, timeLabel) = formatStarted(alert.startedAt)
        val type = when (alert.triggerType?.uppercase(Locale.ROOT)) {
            "HELP", "HELP_MODE", "INACTIVE_CHECK" -> AlertType.HELP
            "INACTIVE" -> AlertType.INACTIVE
            else -> AlertType.SOS
        }
        val headline = when {
            mockDrill -> "$name — practice alert"
            type == AlertType.HELP -> "$name needs help"
            else -> "$name is in danger!"
        }
        val message = when {
            mockDrill -> "$name activated a PUKAAR practice SOS. Please confirm they are safe."
            else -> "$name has pressed SOS in PUKAAR. Please check on them immediately."
        }
        val locationText = when {
            !alert.locationLabel.isNullOrBlank() -> alert.locationLabel!!
            lat != 0.0 && lng != 0.0 -> String.format(Locale.US, "%.5f, %.5f", lat, lng)
            else -> "Location acquiring…"
        }
        val battery = alert.batteryPct ?: -1
        val network = alert.networkType?.takeIf { it.isNotBlank() } ?: "—"
        val quality = networkQuality(network)

        return SosAlert(
            id = alert.eventId ?: "unknown",
            senderName = name,
            senderPhone = phone,
            senderSubtitle = alert.victimSubtitle
                ?: if (mockDrill) "Practice drill" else "SOS protected user",
            senderPhotoRes = null,
            type = if (mockDrill) AlertType.SOS else type,
            dateLabel = dateLabel,
            timeLabel = timeLabel,
            headline = headline,
            message = message,
            location = AlertLocation(
                addressLines = listOf(locationText),
                latitude = lat,
                longitude = lng,
                mapLabel = name,
                highwayLabel = null,
                mapPreviewRes = null
            ),
            deviceStatus = DeviceStatus(
                batteryPercent = if (battery >= 0) battery else 0,
                networkQuality = quality,
                networkType = network
            ),
            trustedContacts = alert.trustedContacts.orEmpty().map { it.toTrusted() },
            helpContacts = alert.helpNumbers.orEmpty().map { it.toHelp() },
            emergencyServices = listOf(
                EmergencyService(
                    kind = EmergencyServiceKind.POLICE,
                    title = alert.policeName ?: "Police Emergency",
                    detailLines = listOfNotNull(alert.policeAddress?.takeIf { it.isNotBlank() }),
                    phone = alert.policePhone ?: "100"
                ),
                EmergencyService(
                    kind = EmergencyServiceKind.AMBULANCE,
                    title = alert.ambulanceName ?: "National Ambulance",
                    detailLines = listOfNotNull(alert.ambulanceAddress?.takeIf { it.isNotBlank() }),
                    phone = alert.ambulancePhone ?: "108",
                    phoneLabel = alert.ambulancePhone ?: "108"
                ),
                EmergencyService(
                    kind = EmergencyServiceKind.HOSPITAL,
                    title = alert.hospitalName ?: "Nearest Hospital",
                    detailLines = listOfNotNull(alert.hospitalAddress?.takeIf { it.isNotBlank() }),
                    phone = alert.hospitalPhone ?: "112"
                )
            )
        )
    }

    private fun AlertContactDto.toTrusted(): TrustedContact {
        val (nameOnly, relation) = splitNameRelation(name, this.relationship, role)
        return TrustedContact(
            name = nameOnly,
            relation = relation,
            phone = phone.orEmpty(),
            place = "",
            status = parseStatus(status),
            photoRes = null
        )
    }

    private fun AlertContactDto.toHelp(): HelpContact {
        val (nameOnly, relation) = splitNameRelation(name, this.relationship, role)
        return HelpContact(
            name = nameOnly,
            relation = relation,
            phone = phone.orEmpty(),
            place = "",
            photoRes = null
        )
    }

    private fun splitNameRelation(rawName: String?, relationship: String?, role: String?): Pair<String, String> {
        val raw = rawName?.trim().orEmpty().ifBlank { "Contact" }
        val paren = Regex("""^(.*)\((.+)\)\s*$""").find(raw)
        if (paren != null) {
            return paren.groupValues[1].trim() to paren.groupValues[2].trim()
        }
        val rel = relationship?.takeIf { it.isNotBlank() }
            ?: role?.replace('_', ' ')?.lowercase(Locale.ROOT)?.replaceFirstChar { it.titlecase(Locale.ROOT) }
            ?: ""
        return raw to rel
    }

    private fun parseStatus(raw: String?): DeliveryStatus = when (raw?.uppercase(Locale.ROOT)) {
        "READ" -> DeliveryStatus.READ
        "DELIVERED", "SENT" -> DeliveryStatus.DELIVERED
        else -> DeliveryStatus.PENDING
    }

    private fun formatStarted(startedAt: String?): Pair<String, String> {
        if (startedAt.isNullOrBlank()) return "" to ""
        return try {
            val instant = Instant.parse(startedAt)
            val zdt = instant.atZone(ist)
            dateFmt.format(zdt) to timeFmt.format(zdt)
        } catch (_: Exception) {
            startedAt to ""
        }
    }

    private fun networkQuality(network: String): String {
        val n = network.uppercase(Locale.ROOT)
        return when {
            n.contains("5G") || n.contains("WIFI") || n.contains("WI-FI") -> "Good"
            n.contains("4G") || n.contains("LTE") -> "Good"
            n.contains("3G") || n.contains("2G") || n.contains("EDGE") -> "Fair"
            n == "—" || n.isBlank() -> "—"
            else -> "OK"
        }
    }
}
