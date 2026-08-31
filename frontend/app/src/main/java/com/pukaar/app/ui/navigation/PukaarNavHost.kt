package com.pukaar.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pukaar.app.R
import com.pukaar.app.ui.component.SuccessScreen
import com.pukaar.app.ui.screen.about.AboutScreen
import com.pukaar.app.ui.screen.addcontact.AddContactScreen
import com.pukaar.app.ui.screen.aftersos.WhatHappensAfterSosScreen
import com.pukaar.app.ui.screen.contacts.ViewContactsScreen
import com.pukaar.app.ui.screen.elderlyhelp.ElderlyHelpScreen
import com.pukaar.app.ui.screen.elderlyworks.HowElderlyHelpWorksScreen
import com.pukaar.app.ui.screen.emergencyinfo.EmergencyInfoScreen
import com.pukaar.app.ui.screen.faq.FaqScreen
import com.pukaar.app.ui.screen.helpvideo.HelpVideoScreen
import com.pukaar.app.ui.screen.home.HomeMode
import com.pukaar.app.ui.screen.home.HomeScreen
import com.pukaar.app.ui.screen.homemodeguide.HomeModeGuideScreen
import com.pukaar.app.ui.screen.howitworks.HowThisWorksScreen
import com.pukaar.app.ui.screen.inactivity.InactivityFeatureScreen
import com.pukaar.app.ui.screen.language.LanguageScreen
import com.pukaar.app.ui.screen.legal.LegalTermsScreen
import com.pukaar.app.ui.screen.menu.MenuScreen
import com.pukaar.app.ui.screen.mockdrill.MockDrillScreen
import com.pukaar.app.ui.screen.notifications.NotificationsScreen
import com.pukaar.app.ui.screen.payment.PaymentReferralScreen
import com.pukaar.app.ui.screen.privacy.PrivacySecurityScreen
import com.pukaar.app.ui.screen.sossettings.SosSettingsScreen
import com.pukaar.app.ui.screen.splash.SplashRoute
import com.pukaar.app.ui.screen.success.SuccessType

/**
 * The app's single navigation graph.
 *
 * Screens are added here and nowhere else, so the set of reachable destinations
 * stays readable in one file. All behaviour arrives through [actions]; swap
 * [NoOpPukaarActions] for a real implementation to bring the app to life.
 */
@Composable
fun PukaarNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    actions: PukaarActions = NoOpPukaarActions,
    startDestination: Route = Route.Splash,
    contacts: List<com.pukaar.app.ui.screen.contacts.ContactUiModel> = emptyList(),
    onSaveContact: ((com.pukaar.app.ui.screen.contacts.ContactDraft, () -> Unit) -> Unit)? = null,
    onContactsRefresh: (() -> Unit)? = null
) {
    // Every save in the mock-ups lands on the same confirmation panel.
    fun showSuccess(type: SuccessType) = navController.navigate(Route.Success.pathFor(type))

    // The chosen mode belongs to the whole graph, not just Home: Mock Drill
    // rehearses whichever of the two the user is currently in.
    var mode by rememberSaveable { mutableStateOf(HomeMode.SOS) }

    NavHost(
        navController = navController,
        startDestination = startDestination.path,
        modifier = modifier
    ) {
        composable(Route.Splash.path) {
            SplashRoute(
                onFinished = {
                    navController.navigate(Route.Home.path) {
                        popUpTo(Route.Splash.path) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.Home.path) {
            HomeScreen(
                mode = mode,
                onModeChange = { mode = it },
                onPrimaryAction = { active ->
                    when (active) {
                        HomeMode.SOS -> actions.triggerSos()
                        HomeMode.HELP -> actions.triggerHelp()
                    }
                },
                onMenuClick = { navController.navigate(Route.Menu.path) }
            )
        }

        composable(Route.Menu.path) {
            MenuScreen(
                onItemClick = { item -> navController.navigate(item.route.path) },
                onSettingsClick = actions::openSettings,
                onClose = { navController.popBackStack() }
            )
        }

        composable(Route.AddContact.path) {
            AddContactScreen(
                onBack = { navController.popBackStack() },
                onSaveContact = { draft ->
                    if (onSaveContact != null) {
                        onSaveContact(draft) {
                            showSuccess(SuccessType.CONTACT_ADDED)
                        }
                    } else {
                        actions.saveContact(draft)
                        showSuccess(SuccessType.CONTACT_ADDED)
                    }
                }
            )
        }

        composable(Route.SosSettings.path) {
            SosSettingsScreen(
                onBack = { navController.popBackStack() },
                onSave = { form ->
                    actions.saveSosSettings(form)
                    showSuccess(SuccessType.SOS_SETTINGS_SAVED)
                }
            )
        }

        composable(Route.MockDrill.path) {
            MockDrillScreen(mode = mode, onBack = { navController.popBackStack() })
        }

        composable(Route.ViewContacts.path) {
            val list = if (contacts.isNotEmpty()) contacts else actions.loadContacts()
            ViewContactsScreen(
                contacts = list,
                onBack = { navController.popBackStack() },
                onContactClick = { contact ->
                    actions.openContact(contact)
                    onContactsRefresh?.invoke()
                }
            )
        }

        composable(Route.ElderlyHelp.path) {
            ElderlyHelpScreen(
                onBack = { navController.popBackStack() },
                onSave = { window, medicationReminder ->
                    actions.saveElderlyHelp(window, medicationReminder)
                    showSuccess(SuccessType.ELDERLY_HELP_SAVED)
                }
            )
        }

        composable(Route.EmergencyInfo.path) {
            EmergencyInfoScreen(
                onBack = { navController.popBackStack() },
                onBloodGroupClick = actions::editBloodGroup,
                onAllergiesClick = actions::editAllergies,
                onConditionsClick = actions::editConditions,
                onSave = { doctorContact ->
                    actions.saveEmergencyInfo(doctorContact)
                    showSuccess(SuccessType.EMERGENCY_INFO_SAVED)
                }
            )
        }

        composable(Route.PaymentReferral.path) {
            PaymentReferralScreen(
                planName = stringResource(R.string.payment_premium),
                validTill = stringResource(R.string.payment_valid_date),
                referralCode = stringResource(R.string.referral_code),
                isActive = true,
                onBack = { navController.popBackStack() },
                onUpgradePlan = {
                    actions.upgradePlan()
                    showSuccess(SuccessType.PAYMENT_COMPLETED)
                },
                onViewHistory = actions::viewPaymentHistory,
                onShareReferral = actions::shareReferralCode
            )
        }

        composable(Route.HelpVideo.path) {
            HelpVideoScreen(
                onBack = { navController.popBackStack() },
                onPlayMainVideo = actions::playIntroVideo,
                onTopicClick = actions::playTopic
            )
        }

        composable(Route.Language.path) {
            LanguageScreen(
                onBack = { navController.popBackStack() },
                onSave = { language ->
                    actions.saveLanguage(language)
                    showSuccess(SuccessType.LANGUAGE_SAVED)
                }
            )
        }

        composable(Route.Notifications.path) {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                onSave = { preferences ->
                    actions.saveNotificationPreferences(preferences)
                    showSuccess(SuccessType.NOTIFICATIONS_SAVED)
                }
            )
        }

        composable(Route.Faq.path) {
            FaqScreen(
                onBack = { navController.popBackStack() },
                onEntryClick = actions::openFaqEntry
            )
        }

        composable(Route.About.path) {
            AboutScreen(
                versionName = stringResource(R.string.about_version),
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.HowThisWorks.path) {
            HowThisWorksScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.WhatHappensAfterSos.path) {
            WhatHappensAfterSosScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.HomeModeGuide.path) {
            HomeModeGuideScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.LegalTerms.path) {
            LegalTermsScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.PrivacySecurity.path) {
            PrivacySecurityScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.HowElderlyHelpWorks.path) {
            HowElderlyHelpWorksScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.InactivityFeature.path) {
            InactivityFeatureScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Route.Success.path,
            arguments = listOf(navArgument(Route.ARG_SUCCESS_TYPE) { type = NavType.StringType })
        ) { backStackEntry ->
            val type = SuccessType.fromName(
                backStackEntry.arguments?.getString(Route.ARG_SUCCESS_TYPE)
            )
            SuccessScreen(
                message = stringResource(type.messageRes),
                // OK returns to the menu rather than the form that was just saved.
                onDismiss = {
                    navController.popBackStack(Route.Menu.path, inclusive = false)
                }
            )
        }
    }
}
