package com.pukaar.highalert

/** Merges alert payloads — never let a thin FCM push wipe richer snapshot data. */
object AlertMerge {
    fun merge(old: PendingAlertResponse, new: PendingAlertResponse): PendingAlertResponse =
        old.copy(
            active = new.active ?: old.active,
            eventId = new.eventId ?: old.eventId,
            victimName = pick(new.victimName, old.victimName),
            victimPhone = pick(new.victimPhone, old.victimPhone),
            victimSubtitle = pick(new.victimSubtitle, old.victimSubtitle),
            latitude = new.latitude ?: old.latitude,
            longitude = new.longitude ?: old.longitude,
            locationLabel = pick(new.locationLabel, old.locationLabel),
            batteryPct = new.batteryPct ?: old.batteryPct,
            networkType = pick(new.networkType, old.networkType),
            mockDrill = new.mockDrill ?: old.mockDrill,
            triggerType = pick(new.triggerType, old.triggerType),
            startedAt = pick(new.startedAt, old.startedAt),
            policeName = pick(new.policeName, old.policeName),
            policePhone = pick(new.policePhone, old.policePhone),
            policeAddress = pick(new.policeAddress, old.policeAddress),
            hospitalName = pick(new.hospitalName, old.hospitalName),
            hospitalPhone = pick(new.hospitalPhone, old.hospitalPhone),
            hospitalAddress = pick(new.hospitalAddress, old.hospitalAddress),
            ambulanceName = pick(new.ambulanceName, old.ambulanceName),
            ambulancePhone = pick(new.ambulancePhone, old.ambulancePhone),
            ambulanceAddress = pick(new.ambulanceAddress, old.ambulanceAddress),
            trustedContacts = pickList(new.trustedContacts, old.trustedContacts),
            helpNumbers = pickList(new.helpNumbers, old.helpNumbers)
        )

    fun isRicherThan(alert: PendingAlertResponse, baseline: PendingAlertResponse): Boolean {
        if (!alert.trustedContacts.isNullOrEmpty() && baseline.trustedContacts.isNullOrEmpty()) return true
        if (!alert.helpNumbers.isNullOrEmpty() && baseline.helpNumbers.isNullOrEmpty()) return true
        if (hasPlace(alert.policeName, alert.policePhone) && !hasPlace(baseline.policeName, baseline.policePhone)) {
            return true
        }
        if (hasPlace(alert.hospitalName, alert.hospitalPhone) && !hasPlace(baseline.hospitalName, baseline.hospitalPhone)) {
            return true
        }
        if (hasPlace(alert.ambulanceName, alert.ambulancePhone) &&
            !hasPlace(baseline.ambulanceName, baseline.ambulancePhone)
        ) {
            return true
        }
        return false
    }

    private fun pick(new: String?, old: String?): String? =
        new?.takeIf { it.isNotBlank() } ?: old

    private fun pickList(
        new: List<AlertContactDto>?,
        old: List<AlertContactDto>?
    ): List<AlertContactDto>? = new?.takeIf { it.isNotEmpty() } ?: old

    private fun hasPlace(name: String?, phone: String?): Boolean =
        !name.isNullOrBlank() || !phone.isNullOrBlank()
}
