package com.pukaar.highalert.data

import com.pukaar.highalert.R

/**
 * Source of alerts shown by the app.
 *
 * For now this returns a fixed sample payload. When the backend is wired up, replace
 * [sampleAlert] with a suspending call (push payload / REST fetch) that builds the same
 * [SosAlert] model — the screens do not need to change.
 */
object AlertRepository {

    fun sampleAlert(): SosAlert = SosAlert(
        id = "sample-alert",
        senderName = "Rahul",
        senderPhone = "+1 201 555 0187",
        senderSubtitle = "Student, New Jersey, India",
        senderPhotoRes = R.drawable.sample_avatar_rahul,
        type = AlertType.SOS,
        dateLabel = "14 Oct 2024",
        timeLabel = "7:24 PM",
        headline = "Rahul is in danger!",
        message = "Rahul has pressed SOS in PUKAAR. Please check on him immediately.",
        location = AlertLocation(
            addressLines = listOf(
                "Near 5th Avenue,",
                "Manhattan, New York,",
                "NY 10022, India"
            ),
            latitude = 40.7614,
            longitude = -73.9776,
            mapLabel = "Manhattan\nNew York",
            highwayLabel = "E 57th St",
            mapPreviewRes = R.drawable.sample_map_manhattan
        ),
        deviceStatus = DeviceStatus(
            batteryPercent = 68,
            networkQuality = "Good",
            networkType = "5G"
        ),
        trustedContacts = listOf(
            TrustedContact(
                name = "Anita",
                relation = "Mother",
                phone = "+91 98765 43211",
                place = "India",
                status = DeliveryStatus.READ,
                photoRes = R.drawable.sample_avatar_anita
            ),
            TrustedContact(
                name = "Michael",
                relation = "Friend",
                phone = "+1 609 555 0148",
                place = "New Jersey, India",
                status = DeliveryStatus.DELIVERED,
                photoRes = R.drawable.sample_avatar_michael
            ),
            TrustedContact(
                name = "Rajesh",
                relation = "Father",
                phone = "+91 98765 43212",
                place = "India",
                status = DeliveryStatus.PENDING,
                photoRes = R.drawable.sample_avatar_rajesh
            )
        ),
        helpContacts = listOf(
            HelpContact(
                name = "Dr. Emily Carter",
                relation = "Doctor",
                phone = "+1 212 555 0199",
                place = "New York, India",
                photoRes = R.drawable.sample_avatar_emily
            ),
            HelpContact(
                name = "David Wilson",
                relation = "Neighbor",
                phone = "+1 201 555 0173",
                place = "New Jersey, India",
                photoRes = R.drawable.sample_avatar_david
            ),
            HelpContact(
                name = "Pooja",
                relation = "Sister",
                phone = "+91 98765 43220",
                place = "India",
                photoRes = R.drawable.sample_avatar_pooja
            )
        ),
        emergencyServices = listOf(
            EmergencyService(
                kind = EmergencyServiceKind.POLICE,
                title = "NYPD Police Station",
                detailLines = listOf("Midtown North Precinct", "357 W 35th St,", "New York, NY 10001"),
                phone = "+1 212 736 3100"
            ),
            EmergencyService(
                kind = EmergencyServiceKind.AMBULANCE,
                title = "Ambulance (EMS)",
                detailLines = listOf("FDNY Emergency", "New York, NY"),
                phone = "+1 212 639 9675",
                phoneLabel = "108 / 112"
            ),
            EmergencyService(
                kind = EmergencyServiceKind.HOSPITAL,
                title = "Nearest Hospital",
                detailLines = listOf("NYU Langone Health", "550 1st Ave,", "New York, NY 10016"),
                phone = "+1 212 263 7300"
            )
        )
    )

    const val EMERGENCY_NUMBER = "112"
    const val EMERGENCY_REGION = "India"
}
