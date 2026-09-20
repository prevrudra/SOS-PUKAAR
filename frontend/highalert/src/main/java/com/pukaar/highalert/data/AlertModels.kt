package com.pukaar.highalert.data

import androidx.annotation.DrawableRes

/** Why a trusted contact was alerted. */
enum class AlertType(val label: String) {
    SOS("SOS"),
    HELP("HELP"),
    INACTIVE("INACTIVE")
}

/** How far the alert got to a trusted contact. */
enum class DeliveryStatus(val label: String) {
    READ("READ"),
    DELIVERED("DELIVERED"),
    SENT("SENT"),
    FAILED("FAILED"),
    PENDING("PENDING")
}

/** A contact that received the alert and can coordinate with the others. */
data class TrustedContact(
    val name: String,
    val relation: String,
    val phone: String,
    val place: String,
    val status: DeliveryStatus,
    @DrawableRes val photoRes: Int? = null
)

/** A number the sender saved for extra help; not alerted, but callable directly. */
data class HelpContact(
    val name: String,
    val relation: String,
    val phone: String,
    val place: String,
    @DrawableRes val photoRes: Int? = null
)

enum class EmergencyServiceKind { POLICE, AMBULANCE, HOSPITAL }

data class EmergencyService(
    val kind: EmergencyServiceKind,
    val title: String,
    val detailLines: List<String>,
    /** Number the Call button dials. */
    val phone: String,
    /** How the number is shown; defaults to [phone]. */
    val phoneLabel: String = phone
)

data class AlertLocation(
    val addressLines: List<String>,
    val latitude: Double,
    val longitude: Double,
    val mapLabel: String,
    val highwayLabel: String?,
    /** Static map preview; the drawn thumbnail is used when absent. */
    @DrawableRes val mapPreviewRes: Int? = null
)

data class DeviceStatus(
    val batteryPercent: Int,
    val networkQuality: String,
    val networkType: String
)

/**
 * A single received alert. Everything the alert screen renders comes from here, so
 * swapping [AlertRepository] for a real API later needs no UI changes.
 */
data class SosAlert(
    val id: String,
    val senderName: String,
    val senderPhone: String,
    val senderSubtitle: String,
    @DrawableRes val senderPhotoRes: Int?,
    val type: AlertType,
    val dateLabel: String,
    val timeLabel: String,
    val headline: String,
    val message: String,
    val location: AlertLocation,
    val deviceStatus: DeviceStatus,
    val trustedContacts: List<TrustedContact>,
    val helpContacts: List<HelpContact>,
    val emergencyServices: List<EmergencyService>
)
