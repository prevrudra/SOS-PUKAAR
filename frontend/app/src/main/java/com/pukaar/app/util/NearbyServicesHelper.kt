package com.pukaar.app.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.pukaar.app.PukaarApp
import com.pukaar.app.data.api.EmergencyDto
import com.pukaar.app.data.api.HospitalDto
import com.pukaar.app.data.api.NearbyPlaceDto
import com.pukaar.app.data.api.PoliceDto
import kotlinx.coroutines.tasks.await

/**
 * Fills nearest police / hospital / ambulance from the live /nearby API.
 * Never downgrades a real place to a national placeholder on a later refresh.
 */
object NearbyServicesHelper {

    data class Fix(val latitude: Double, val longitude: Double, val accuracyM: Double?)

    @SuppressLint("MissingPermission")
    suspend fun currentFix(context: Context): Fix? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
            && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        return runCatching {
            val loc = LocationServices.getFusedLocationProviderClient(context)
                .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                .await()
            loc?.let { Fix(it.latitude, it.longitude, it.accuracy.toDouble()) }
        }.getOrNull()
    }

    /**
     * @param pushLocation when true, also posts GPS to the server (SOS trigger only).
     * Prefer false on the active-screen poll — FGS already uploads location, and
     * pushing here every few seconds caused Places lookups to race and wipe UI.
     */
    suspend fun enrich(
        event: EmergencyDto,
        context: Context? = null,
        pushLocation: Boolean = false
    ): EmergencyDto {
        var e = event
        if (pushLocation && context != null && e.id != null) {
            val fix = currentFix(context)
            if (fix != null) {
                runCatching {
                    PukaarApp.instance.repository.updateLocation(
                        e.id!!, fix.latitude, fix.longitude, fix.accuracyM
                    )
                }.getOrNull()?.let { updated ->
                    // Keep any real nearby places already on [e]; updateLocation DTO
                    // often only has national fallbacks when Places times out.
                    e = mergeNearbyPreferReal(e, updated).copy(
                        latitude = updated.latitude ?: e.latitude ?: fix.latitude,
                        longitude = updated.longitude ?: e.longitude ?: fix.longitude,
                        batteryPct = updated.batteryPct ?: e.batteryPct,
                        networkType = updated.networkType ?: e.networkType,
                        status = updated.status ?: e.status,
                        deliveries = updated.deliveries ?: e.deliveries,
                        audioSegments = updated.audioSegments ?: e.audioSegments
                    )
                }
                if (e.latitude == null || e.longitude == null) {
                    e = e.copy(latitude = fix.latitude, longitude = fix.longitude)
                }
            }
        }

        val lat = e.latitude ?: return withNationalFallbacks(e)
        val lng = e.longitude ?: return withNationalFallbacks(e)

        val nearby = runCatching {
            PukaarApp.instance.repository.nearby(lat, lng, 3)
        }.getOrNull()

        if (nearby == null) {
            // Keep whatever real places we already have — do not wipe to national.
            return withNationalFallbacks(e)
        }

        val police = nearby.police?.firstOrNull()?.toPolice()
        val hospital = nearby.hospitals?.firstOrNull()?.toHospital()
        val ambulance = nearby.ambulance
            ?.firstOrNull { it.source != "NATIONAL" }
            ?.toHospital()
            ?: nearby.ambulance?.firstOrNull()?.toHospital()

        return withNationalFallbacks(
            e.copy(
                policeStation = preferRealPolice(e.policeStation, police),
                nearestHospital = preferRealHospital(e.nearestHospital, hospital),
                nearestAmbulance = preferRealHospital(e.nearestAmbulance, ambulance),
                nearbySource = nearby.source ?: e.nearbySource
            )
        )
    }

    /** Prefer real place names over "Nearest Hospital" / "National Ambulance" placeholders. */
    fun mergeNearbyPreferReal(previous: EmergencyDto?, incoming: EmergencyDto): EmergencyDto {
        if (previous == null) return withNationalFallbacks(incoming)
        return withNationalFallbacks(
            incoming.copy(
                policeStation = preferRealPolice(previous.policeStation, incoming.policeStation),
                nearestHospital = preferRealHospital(previous.nearestHospital, incoming.nearestHospital),
                nearestAmbulance = preferRealHospital(previous.nearestAmbulance, incoming.nearestAmbulance),
                nearbySource = when {
                    !isGenericSource(incoming.nearbySource) -> incoming.nearbySource
                    else -> previous.nearbySource ?: incoming.nearbySource
                },
                deliveries = incoming.deliveries ?: previous.deliveries,
                audioSegments = incoming.audioSegments ?: previous.audioSegments,
                batteryPct = incoming.batteryPct ?: previous.batteryPct,
                networkType = incoming.networkType ?: previous.networkType,
                latitude = incoming.latitude ?: previous.latitude,
                longitude = incoming.longitude ?: previous.longitude
            )
        )
    }

    fun withNationalFallbacks(e: EmergencyDto): EmergencyDto = e.copy(
        policeStation = e.policeStation?.withPhoneFallback("100")
            ?: PoliceDto(
                name = "Police Emergency",
                phone = "100",
                phoneVerified = true,
                address = null
            ),
        nearestHospital = e.nearestHospital?.withPhoneFallback("112")
            ?: HospitalDto(
                name = "Nearest Hospital / Emergency",
                phone = "112",
                address = null
            ),
        nearestAmbulance = e.nearestAmbulance?.withPhoneFallback("108")
            ?: HospitalDto(
                name = "National Ambulance",
                phone = "108",
                address = null
            )
    )

    fun isGenericPlaceName(name: String?): Boolean {
        val n = name?.trim()?.lowercase().orEmpty()
        if (n.isBlank()) return true
        return n == "police emergency"
            || n == "national ambulance"
            || n == "nearest hospital"
            || n == "nearest hospital / emergency"
            || n.startsWith("nearest hospital")
    }

    private fun isGenericSource(source: String?): Boolean =
        source.isNullOrBlank() || source.equals("NATIONAL", ignoreCase = true)

    private fun preferRealPolice(existing: PoliceDto?, incoming: PoliceDto?): PoliceDto? {
        if (incoming == null) return existing
        if (existing == null) return incoming
        if (isGenericPlaceName(incoming.name) && !isGenericPlaceName(existing.name)) {
            return existing.copy(
                phone = existing.phone?.takeIf { it.isNotBlank() } ?: incoming.phone,
                address = existing.address ?: incoming.address
            )
        }
        return incoming.copy(
            phone = incoming.phone?.takeIf { it.isNotBlank() } ?: existing.phone,
            address = incoming.address ?: existing.address
        )
    }

    private fun preferRealHospital(existing: HospitalDto?, incoming: HospitalDto?): HospitalDto? {
        if (incoming == null) return existing
        if (existing == null) return incoming
        if (isGenericPlaceName(incoming.name) && !isGenericPlaceName(existing.name)) {
            return existing.copy(
                phone = existing.phone?.takeIf { it.isNotBlank() } ?: incoming.phone,
                address = existing.address ?: incoming.address
            )
        }
        return incoming.copy(
            phone = incoming.phone?.takeIf { it.isNotBlank() } ?: existing.phone,
            address = incoming.address ?: existing.address
        )
    }

    private fun NearbyPlaceDto.toPolice() = PoliceDto(
        name = name,
        phone = phone?.takeIf { it.isNotBlank() && it != "null" },
        phoneVerified = !phone.isNullOrBlank(),
        address = address,
        latitude = latitude,
        longitude = longitude
    )

    private fun NearbyPlaceDto.toHospital() = HospitalDto(
        name = name,
        phone = phone?.takeIf { it.isNotBlank() && it != "null" },
        address = address,
        latitude = latitude,
        longitude = longitude,
        phoneVerified = !phone.isNullOrBlank()
    )

    private fun PoliceDto.withPhoneFallback(fallback: String) =
        if (phone.isNullOrBlank()) copy(phone = fallback, phoneVerified = true) else this

    private fun HospitalDto.withPhoneFallback(fallback: String) =
        if (phone.isNullOrBlank()) copy(phone = fallback, phoneVerified = true) else this
}
