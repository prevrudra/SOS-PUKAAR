package com.pukaar.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pukaar.app.PukaarApp
import com.pukaar.app.R
import com.pukaar.app.ui.navigation.SubscriptionUi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.pukaar.app.ui.component.SuccessScreen
import com.pukaar.app.ui.screen.about.AboutScreen
import com.pukaar.app.ui.screen.aftersos.WhatHappensAfterSosScreen
import com.pukaar.app.ui.screen.contacts.ContactFormScreen
import com.pukaar.app.ui.screen.contacts.ViewContactsScreen
import com.pukaar.app.ui.screen.contacts.toDraft
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
import com.pukaar.app.ui.screen.notifications.NotificationPreferences
import com.pukaar.app.ui.screen.sossettings.SosSettingsForm
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
    onDeleteContact: ((String, () -> Unit) -> Unit)? = null,
    onResendVerification: ((com.pukaar.app.ui.screen.contacts.ContactUiModel) -> Unit)? = null,
    onContactsRefresh: (() -> Unit)? = null,
    onRequestEmergency: (HomeMode) -> Unit = { mode ->
        when (mode) {
            HomeMode.SOS -> actions.triggerSos()
            HomeMode.HELP -> actions.triggerHelp()
        }
    },
    onRequestMockDrill: (HomeMode) -> Unit = { mode ->
        actions.startMockDrill(mode == HomeMode.SOS)
    }
) {
    // Every save in the mock-ups lands on the same confirmation panel.
    fun showSuccess(type: SuccessType) = navController.navigate(Route.Success.pathFor(type))

    // The chosen mode belongs to the whole graph, not just Home: Mock Drill
    // rehearses whichever of the two the user is currently in.
    var mode by rememberSaveable { mutableStateOf(HomeMode.SOS) }

    LaunchedEffect(Unit) {
        val stored = PukaarApp.instance.sessionStore.homeMode.first()
        mode = if (stored == "HELP") HomeMode.HELP else HomeMode.SOS
    }

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
                onModeChange = { newMode ->
                    mode = newMode
                    actions.updateHomeMode(newMode)
                },
                onPrimaryAction = { active -> onRequestEmergency(active) },
                onMenuClick = { navController.navigate(Route.Menu.path) }
            )
        }

        composable(Route.Menu.path) {
            MenuScreen(
                onItemClick = { item -> navController.navigate(item.route.path) },
                onSettingsClick = { navController.navigate(Route.Settings.path) },
                onClose = { navController.popBackStack() }
            )
        }

        composable(Route.Settings.path) {
            val context = androidx.compose.ui.platform.LocalContext.current
            com.pukaar.app.ui.screen.settings.SettingsScreen(
                onBack = { navController.popBackStack() },
                onBatteryOptimization = { com.pukaar.app.emergency.OemBatteryHelper.requestUnrestrictedBattery(context) },
                onAutostart = { com.pukaar.app.emergency.OemBatteryHelper.openOemAutostartSettings(context) },
                onOverlayPermission = { com.pukaar.app.emergency.OemBatteryHelper.requestOverlayPermission(context) }
            )
        }

        composable(Route.AddContact.path) {
            ContactFormScreen(
                onBack = { navController.popBackStack() },
                onSave = { draft ->
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

        composable(
            route = Route.EditContact.path,
            arguments = listOf(navArgument(Route.EditContact.ARG_CONTACT_ID) { type = NavType.StringType })
        ) { entry ->
            val contactId = entry.arguments?.getString(Route.EditContact.ARG_CONTACT_ID) ?: ""
            val contact = contacts.firstOrNull { it.id == contactId }
            if (contact != null) {
                ContactFormScreen(
                    initial = contact.toDraft(),
                    onBack = { navController.popBackStack() },
                    onSave = { draft ->
                        if (onSaveContact != null) {
                            onSaveContact(draft) {
                                showSuccess(SuccessType.CONTACT_UPDATED)
                            }
                        } else {
                            actions.saveContact(draft)
                            showSuccess(SuccessType.CONTACT_UPDATED)
                        }
                    },
                    onDelete = {
                        if (onDeleteContact != null) {
                            onDeleteContact(contactId) {
                                navController.popBackStack()
                                showSuccess(SuccessType.CONTACT_DELETED)
                            }
                        }
                    },
                    onResendVerification = {
                        onResendVerification?.invoke(contact)
                    }
                )
            }
        }

        composable(Route.SosSettings.path) {
            var initial by remember { mutableStateOf<SosSettingsForm?>(null) }
            LaunchedEffect(Unit) {
                initial = actions.loadSosSettings()
            }
            if (initial != null) {
                SosSettingsScreen(
                    onBack = { navController.popBackStack() },
                    initialForm = initial!!,
                    onSave = { form ->
                        actions.saveSosSettings(form)
                        showSuccess(SuccessType.SOS_SETTINGS_SAVED)
                    }
                )
            }
        }

        composable(Route.MockDrill.path) {
            MockDrillScreen(
                mode = mode,
                onBack = { navController.popBackStack() },
                onStartLiveDrill = {
                    onRequestMockDrill(mode)
                }
            )
        }

        composable(Route.ViewContacts.path) {
            val list = if (contacts.isNotEmpty()) contacts else actions.loadContacts()
            ViewContactsScreen(
                contacts = list,
                onBack = { navController.popBackStack() },
                onAddContact = { navController.navigate(Route.AddContact.path) },
                onEditContact = { contact ->
                    navController.navigate(Route.EditContact.pathFor(contact.id))
                }
            )
        }

        composable(Route.ElderlyHelp.path) {
            var initial by remember { mutableStateOf<Pair<com.pukaar.app.ui.screen.elderlyhelp.InactivityWindow, Boolean>?>(null) }
            LaunchedEffect(Unit) {
                initial = actions.loadElderlyHelp()
            }
            if (initial != null) {
                ElderlyHelpScreen(
                    onBack = { navController.popBackStack() },
                    initialWindow = initial!!.first,
                    initialMedicationReminder = initial!!.second,
                    onSave = { window, medicationReminder ->
                        actions.saveElderlyHelp(window, medicationReminder)
                        showSuccess(SuccessType.ELDERLY_HELP_SAVED)
                    }
                )
            }
        }

        composable(Route.EmergencyInfo.path) {
            var form by remember { mutableStateOf<com.pukaar.app.ui.screen.emergencyinfo.EmergencyInfoForm?>(null) }
            LaunchedEffect(Unit) {
                form = actions.loadEmergencyInfo()
            }
            if (form != null) {
                EmergencyInfoScreen(
                    onBack = { navController.popBackStack() },
                    initial = form!!,
                    onSave = { updated ->
                        actions.saveEmergencyInfo(updated)
                        showSuccess(SuccessType.EMERGENCY_INFO_SAVED)
                    }
                )
            }
        }

        composable(Route.PaymentReferral.path) {
            var subUi by remember { mutableStateOf<SubscriptionUi?>(null) }
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                subUi = actions.loadSubscriptionUi()
            }
            val ui = subUi
            if (ui != null) {
                PaymentReferralScreen(
                    planName = ui.planName,
                    validTill = ui.validTill,
                    referralCode = ui.referralCode,
                    isActive = ui.isActive,
                    individualPrice = ui.individualPrice,
                    familyPrice = ui.familyPrice,
                    referralCount = ui.referralCount,
                    onBack = { navController.popBackStack() },
                    onUpgradeIndividual = {
                        actions.upgradePlan(
                            plan = "INDIVIDUAL",
                            onSuccess = { showSuccess(SuccessType.PAYMENT_COMPLETED) },
                            onFailure = { msg ->
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    onUpgradeFamily = {
                        actions.upgradePlan(
                            plan = "FAMILY",
                            onSuccess = { showSuccess(SuccessType.PAYMENT_COMPLETED) },
                            onFailure = { msg ->
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    onViewHistory = actions::viewPaymentHistory,
                    onShareReferral = actions::shareReferralCode
                )
            }
        }

        composable(Route.HelpVideo.path) {
            HelpVideoScreen(
                onBack = { navController.popBackStack() },
                onPlayMainVideo = actions::playIntroVideo,
                onTopicClick = actions::playTopic
            )
        }

        composable(Route.Language.path) {
            var initial by remember { mutableStateOf<com.pukaar.app.ui.screen.language.AppLanguage?>(null) }
            LaunchedEffect(Unit) {
                initial = actions.loadLanguage()
            }
            if (initial != null) {
                LanguageScreen(
                    onBack = { navController.popBackStack() },
                    initialLanguage = initial!!,
                    onSave = { language ->
                        actions.saveLanguage(language)
                        showSuccess(SuccessType.LANGUAGE_SAVED)
                    }
                )
            }
        }

        composable(Route.Notifications.path) {
            var prefs by remember { mutableStateOf<NotificationPreferences?>(null) }
            LaunchedEffect(Unit) {
                prefs = actions.loadNotificationPreferences()
            }
            if (prefs != null) {
                NotificationsScreen(
                    onBack = { navController.popBackStack() },
                    initialPreferences = prefs!!,
                    onSave = { preferences ->
                        actions.saveNotificationPreferences(preferences)
                        showSuccess(SuccessType.NOTIFICATIONS_SAVED)
                    }
                )
            }
        }

        composable(Route.Faq.path) {
            var selected by remember { mutableStateOf<com.pukaar.app.ui.screen.faq.FaqEntry?>(null) }
            FaqScreen(
                onBack = { navController.popBackStack() },
                onEntryClick = { selected = it }
            )
            selected?.let { entry ->
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { selected = null },
                    title = { androidx.compose.material3.Text(stringResource(entry.questionRes)) },
                    text = { androidx.compose.material3.Text(stringResource(entry.answerRes)) },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = { selected = null }) {
                            androidx.compose.material3.Text(stringResource(R.string.action_ok))
                        }
                    }
                )
            }
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
