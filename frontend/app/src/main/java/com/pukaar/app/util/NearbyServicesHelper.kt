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
 * Fills nearest police / hospital / ambulance from the live /nearby API
 * using the emergency GPS (or a fresh device fix).
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

    suspend fun enrich(event: EmergencyDto, context: Context? = null): EmergencyDto {
        var e = event
        if (context != null && e.id != null) {
            val fix = currentFix(context)
            if (fix != null) {
                runCatching {
                    PukaarApp.instance.repository.updateLocation(
                        e.id!!, fix.latitude, fix.longitude, fix.accuracyM
                    )
                }.getOrNull()?.let { updated -> e = updated }
                if (e.latitude == null || e.longitude == null) {
                    e = e.copy(latitude = fix.latitude, longitude = fix.longitude)
                }
            }
        }

        val lat = e.latitude ?: return withNationalFallbacks(e)
        val lng = e.longitude ?: return withNationalFallbacks(e)

        val nearby = runCatching {
            PukaarApp.instance.repository.nearby(lat, lng, 3)
        }.getOrNull() ?: return withNationalFallbacks(e)

        val police = nearby.police?.firstOrNull()?.toPolice()
            ?: e.policeStation
        val hospital = nearby.hospitals?.firstOrNull()?.toHospital()
            ?: e.nearestHospital
        val ambulance = nearby.ambulance
            ?.firstOrNull { it.source != "NATIONAL" }
            ?.toHospital()
            ?: nearby.ambulance?.firstOrNull()?.toHospital()
            ?: e.nearestAmbulance

        return withNationalFallbacks(
            e.copy(
                policeStation = mergePolice(e.policeStation, police),
                nearestHospital = mergeHospital(e.nearestHospital, hospital),
                nearestAmbulance = mergeHospital(e.nearestAmbulance, ambulance),
                nearbySource = nearby.source ?: e.nearbySource
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

    private fun mergePolice(existing: PoliceDto?, live: PoliceDto?): PoliceDto? {
        if (live == null) return existing
        if (existing == null) return live
        return live.copy(
            phone = live.phone?.takeIf { it.isNotBlank() } ?: existing.phone,
            address = live.address ?: existing.address
        )
    }

    private fun mergeHospital(existing: HospitalDto?, live: HospitalDto?): HospitalDto? {
        if (live == null) return existing
        if (existing == null) return live
        return live.copy(
            phone = live.phone?.takeIf { it.isNotBlank() } ?: existing.phone,
            address = live.address ?: existing.address
        )
    }

    private fun PoliceDto.withPhoneFallback(fallback: String) =
        if (phone.isNullOrBlank()) copy(phone = fallback, phoneVerified = true) else this

    private fun HospitalDto.withPhoneFallback(fallback: String) =
        if (phone.isNullOrBlank()) copy(phone = fallback, phoneVerified = true) else this
}
