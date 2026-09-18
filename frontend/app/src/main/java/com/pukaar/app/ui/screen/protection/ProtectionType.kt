package com.pukaar.app.ui.screen.protection

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.pukaar.app.R
import com.pukaar.app.ui.navigation.Route
import com.pukaar.app.ui.theme.InactivityPurple
import com.pukaar.app.ui.theme.PukaarRed

/**
 * The two protections the app offers, and the identity each one carries.
 *
 * Two screens are indexed by this — the onboarding hub and the "How PUKAAR
 * Works" hub — so the badge, the wording and the colour are declared once and
 * every page behind either hub is tinted from here. Each type therefore knows
 * both of its destinations: [setupRoute] configures it, [guideRoute] explains it.
 */
enum class ProtectionType(
    @StringRes val labelRes: Int,
    @StringRes val taglineRes: Int,
    val icon: ImageVector,
    val accent: Color,
    val setupRoute: Route,
    val guideRoute: Route
) {
    SOS(
        labelRes = R.string.protection_sos,
        taglineRes = R.string.protection_sos_tagline,
        icon = Icons.Filled.NotificationsActive,
        accent = PukaarRed,
        setupRoute = Route.SosOnboarding,
        guideRoute = Route.SosGuide
    ),
    INACTIVITY(
        labelRes = R.string.protection_inactivity,
        taglineRes = R.string.protection_inactivity_tagline,
        icon = InactivityPin,
        accent = InactivityPurple,
        setupRoute = Route.InactivityOnboarding,
        guideRoute = Route.InactivityGuide
    )
}
