package com.pukaar.highalert.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Outgoing intents fired from the alert screen. Dialling uses ACTION_DIAL so the number
 * is pre-filled and the recipient still confirms the call — no CALL_PHONE permission and
 * no accidental dials from a screen full of buttons.
 */
object DeviceActions {

    /** Package of the main PUKAAR app, opened from the "Explore Main PUKAAR" row. */
    private const val MAIN_APP_PACKAGE = "pukaar.com"
    private const val MAIN_APP_STORE_URL =
        "https://play.google.com/store/apps/details?id=$MAIN_APP_PACKAGE"

    fun dial(context: Context, phoneNumber: String) {
        val normalised = phoneNumber.filter { it.isDigit() || it == '+' || it == '#' || it == '*' }
        launch(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$normalised")), "No dialer app found")
    }

    fun openMap(context: Context, latitude: Double, longitude: Double, label: String) {
        val query = Uri.encode("$latitude,$longitude($label)")
        val geoIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$latitude,$longitude?q=$query"))
        try {
            context.startActivity(geoIntent)
        } catch (e: ActivityNotFoundException) {
            launch(
                context,
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=$latitude,$longitude")
                ),
                "No maps app found"
            )
        }
    }

    fun openMainPukaarApp(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage(MAIN_APP_PACKAGE)
        if (launchIntent != null) {
            context.startActivity(launchIntent)
            return
        }
        launch(
            context,
            Intent(Intent.ACTION_VIEW, Uri.parse(MAIN_APP_STORE_URL)),
            "Could not open PUKAAR"
        )
    }

    private fun launch(context: Context, intent: Intent, failureMessage: String) {
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, failureMessage, Toast.LENGTH_SHORT).show()
        }
    }
}
