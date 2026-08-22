package com.pukaar.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pukaar.app.PukaarApp
import com.pukaar.app.ui.screens.contacts.AddContactScreen
import com.pukaar.app.ui.screens.contacts.ViewContactsScreen
import com.pukaar.app.ui.screens.emergency.EmergencyActiveScreen
import com.pukaar.app.ui.screens.emergency.TrustedAlertScreen
import com.pukaar.app.ui.screens.home.HomeScreen
import com.pukaar.app.ui.screens.menu.InfoScreen
import com.pukaar.app.ui.screens.menu.MenuScreen
import com.pukaar.app.ui.screens.menu.MockDrillScreen
import com.pukaar.app.ui.screens.menu.PaymentScreen
import com.pukaar.app.ui.screens.menu.SettingsScreen
import com.pukaar.app.ui.screens.onboarding.ConsentScreen
import com.pukaar.app.ui.screens.onboarding.HomeModeScreen
import com.pukaar.app.ui.screens.onboarding.LanguageScreen
import com.pukaar.app.ui.screens.onboarding.OtpScreen
import com.pukaar.app.ui.screens.onboarding.PermissionsScreen
import com.pukaar.app.ui.screens.onboarding.ProfileScreen
import com.pukaar.app.ui.screens.onboarding.SplashScreen
import com.pukaar.app.ui.screens.onboarding.WelcomeScreen

object Routes {
    const val Splash = "splash"
    const val Welcome = "welcome"
    const val Language = "language"
    const val Consent = "consent"
    const val Otp = "otp"
    const val Profile = "profile"
    const val HomeMode = "home_mode"
    const val Permissions = "permissions"
    const val Home = "home"
    const val Menu = "menu"
    const val AddContact = "add_contact"
    const val ViewContacts = "view_contacts"
    const val MockDrill = "mock_drill"
    const val Payment = "payment"
    const val Settings = "settings"
    const val Info = "info/{title}"
    const val EmergencyActive = "emergency_active/{eventId}"
    const val TrustedAlert = "trusted_alert/{eventId}"
}

@Composable
fun PukaarNavHost() {
    val nav = rememberNavController()
    val session = remember { PukaarApp.instance.sessionStore }
    val onboarding by session.onboardingComplete.collectAsState(initial = false)

    NavHost(navController = nav, startDestination = Routes.Splash) {
        composable(Routes.Splash) {
            SplashScreen {
                nav.navigate(if (onboarding) Routes.Home else Routes.Welcome) {
                    popUpTo(Routes.Splash) { inclusive = true }
                }
            }
        }
        composable(Routes.Welcome) { WelcomeScreen { nav.navigate(Routes.Language) } }
        composable(Routes.Language) { LanguageScreen { nav.navigate(Routes.Consent) } }
        composable(Routes.Consent) { ConsentScreen { nav.navigate(Routes.Otp) } }
        composable(Routes.Otp) { OtpScreen { nav.navigate(Routes.Profile) } }
        composable(Routes.Profile) { ProfileScreen { nav.navigate(Routes.HomeMode) } }
        composable(Routes.HomeMode) { HomeModeScreen { nav.navigate(Routes.AddContact) } }
        composable(Routes.Permissions) {
            PermissionsScreen {
                nav.navigate(Routes.MockDrill)
            }
        }
        composable(Routes.Home) {
            HomeScreen(
                onMenu = { nav.navigate(Routes.Menu) },
                onEmergency = { id -> nav.navigate("emergency_active/$id") }
            )
        }
        composable(Routes.Menu) {
            MenuScreen(
                onBack = { nav.popBackStack() },
                onTile = { route ->
                    when (route) {
                        "add_contact" -> nav.navigate(Routes.AddContact)
                        "view_contacts" -> nav.navigate(Routes.ViewContacts)
                        "mock_drill" -> nav.navigate(Routes.MockDrill)
                        "payment" -> nav.navigate(Routes.Payment)
                        "settings" -> nav.navigate(Routes.Settings)
                        "permissions" -> nav.navigate(Routes.Permissions)
                        else -> nav.navigate("info/$route")
                    }
                }
            )
        }
        composable(Routes.AddContact) {
            AddContactScreen(
                onBack = { nav.popBackStack() },
                onDone = {
                    if (!onboarding) nav.navigate(Routes.Permissions) else nav.popBackStack()
                }
            )
        }
        composable(Routes.ViewContacts) { ViewContactsScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.MockDrill) {
            MockDrillScreen(
                onBack = { nav.popBackStack() },
                onFinished = {
                    nav.navigate(Routes.Payment) {
                        popUpTo(Routes.Home) { inclusive = false }
                    }
                }
            )
        }
        composable(Routes.Payment) {
            PaymentScreen(
                onBack = { nav.popBackStack() },
                onActivated = {
                    nav.navigate(Routes.Home) {
                        popUpTo(Routes.Welcome) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Settings) { SettingsScreen(onBack = { nav.popBackStack() }) }
        composable(
            Routes.Info,
            arguments = listOf(navArgument("title") { type = NavType.StringType })
        ) { entry ->
            InfoScreen(title = entry.arguments?.getString("title") ?: "", onBack = { nav.popBackStack() })
        }
        composable(
            Routes.EmergencyActive,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { entry ->
            EmergencyActiveScreen(
                eventId = entry.arguments?.getString("eventId") ?: "",
                onClosed = {
                    nav.navigate(Routes.Home) {
                        popUpTo(Routes.Home) { inclusive = true }
                    }
                }
            )
        }
        composable(
            Routes.TrustedAlert,
            arguments = listOf(navArgument("eventId") { type = NavType.StringType })
        ) { entry ->
            TrustedAlertScreen(
                eventId = entry.arguments?.getString("eventId") ?: "",
                onBack = { nav.popBackStack() }
            )
        }
    }
}
