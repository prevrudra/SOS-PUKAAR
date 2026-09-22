package com.pukaar.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.pukaar.app.ui.screen.contacts.ViewContactsScreen
import com.pukaar.app.ui.screen.emergencycard.CardQrStyle
import com.pukaar.app.ui.screen.emergencycard.CardReadyScreen
import com.pukaar.app.ui.screen.emergencycard.DownloadCardScreen
import com.pukaar.app.ui.screen.emergencycard.EmergencyCardDraft
import com.pukaar.app.ui.screen.emergencycard.EmergencyCardFlowScreen
import com.pukaar.app.ui.screen.emergencycard.LockScreenCardScreen
import com.pukaar.app.ui.screen.emergencycard.PlanInactiveDialog
import com.pukaar.app.ui.screen.emergencycard.PrintOptions
import com.pukaar.app.ui.screen.emergencycard.PrintableCardScreen
import com.pukaar.app.ui.screen.faq.FaqScreen
import com.pukaar.app.ui.screen.general.GeneralSettingsScreen
import com.pukaar.app.ui.screen.home.HomeScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.pukaar.app.PukaarApp
import com.pukaar.app.integration.ContactRepositoryBridge
import com.pukaar.app.ui.screen.contacts.ContactDraft
import com.pukaar.app.ui.screen.contacts.ContactFormScreen
import com.pukaar.app.ui.screen.contacts.ContactType
import com.pukaar.app.ui.screen.contacts.alertContactsOnly
import com.pukaar.app.ui.screen.contacts.asPreSavedNumbers
import com.pukaar.app.ui.screen.contacts.toDraft
import com.pukaar.app.ui.screen.home.HomeMode
import com.pukaar.app.ui.screen.splash.SplashRoute
import com.pukaar.app.ui.share.rememberInviteAction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.pukaar.app.ui.screen.home.SosNotActiveDialog
import com.pukaar.app.ui.screen.howitworks.EmergencyCardGuideScreen
import com.pukaar.app.ui.screen.howitworks.HowItWorksHubScreen
import com.pukaar.app.ui.screen.howitworks.InactivityGuideScreen
import com.pukaar.app.ui.screen.howitworks.SosGuideScreen
import com.pukaar.app.ui.screen.howitworks.SafeArrivalGuideScreen
import com.pukaar.app.ui.screen.howitworks.WhenToUseScreen
import com.pukaar.app.ui.screen.language.LanguageScreen
import com.pukaar.app.ui.screen.menu.MenuItem
import com.pukaar.app.ui.screen.menu.MenuScreen
import com.pukaar.app.ui.screen.mockdrill.MockDrillChooserScreen
import com.pukaar.app.ui.screen.mockdrill.SosDrillScreen
import com.pukaar.app.ui.screen.onboarding.BeforeYouStartDialog
import com.pukaar.app.ui.screen.onboarding.CouponDialog
import com.pukaar.app.ui.screen.onboarding.InactivityOnboardingScreen
import com.pukaar.app.ui.screen.onboarding.OnboardingHubScreen
import com.pukaar.app.ui.screen.onboarding.SosOnboardingScreen
import com.pukaar.app.ui.screen.payment.PlansScreen
import com.pukaar.app.ui.screen.protection.ProtectionType
import com.pukaar.app.ui.screen.success.SuccessType
import com.pukaar.app.ui.screen.tripshield.TripShieldScreen
import com.pukaar.app.ui.screen.whoispukaarfor.WhoIsPukaarForScreen

/**
 * The app's single navigation graph.
 *
 * Screens are added here and nowhere else, so the set of reachable destinations
 * stays readable in one file. All behaviour arrives through [actions] — by default
 * [InMemoryPukaarActions], which remembers what the setups collect for as long as
 * the app is running; swap in a repository-backed one to make it outlive that.
 */
@Composable
fun PukaarNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    actions: PukaarActions = InMemoryPukaarActions,
    startDestination: Route = Route.Splash,
    contacts: List<com.pukaar.app.ui.screen.contacts.ContactUiModel> = emptyList(),
    onSaveContact: ((com.pukaar.app.ui.screen.contacts.ContactDraft, () -> Unit) -> Unit)? = null,
    onDeleteContact: ((String, () -> Unit) -> Unit)? = null,
    onResendVerification: ((com.pukaar.app.ui.screen.contacts.ContactUiModel) -> Unit)? = null,
    onContactsRefresh: (() -> Unit)? = null,
    onRequestEmergency: (com.pukaar.app.ui.screen.home.HomeMode) -> Unit = {
        actions.triggerSos()
    },
    onRequestMockDrill: (com.pukaar.app.ui.screen.home.HomeMode) -> Unit = {
        actions.startMockDrill(it == com.pukaar.app.ui.screen.home.HomeMode.SOS)
    }
) {
    // Every save in the mock-ups lands on the same confirmation panel.
    fun showSuccess(type: SuccessType) = navController.navigate(Route.Success.pathFor(type))

    // The Emergency Card, held above the five destinations that read it. Seeded
    // from whatever was last submitted so re-entering the flow is an edit.
    var cardDraft by remember {
        mutableStateOf(actions.loadEmergencyCard() ?: EmergencyCardDraft())
    }
    var printOptions by remember { mutableStateOf(PrintOptions()) }

    // Which of the two card faces is being looked at. Held here rather than on the
    // finished-card screen so printing it and setting it as a wallpaper show the
    // same face that was on screen when those were tapped.
    var cardQrStyle by rememberSaveable { mutableStateOf(CardQrStyle.WITH_DETAILS) }

    NavHost(
        navController = navController,
        startDestination = startDestination.path,
        modifier = modifier
    ) {
        composable(Route.Splash.path) {
            val scope = rememberCoroutineScope()
            SplashRoute(
                onFinished = {
                    scope.launch {
                        val passed = PukaarApp.instance.sessionStore.mockDrillPassed.first()
                        val dest = if (passed) Route.Home.path else Route.MockDrill.path
                        navController.navigate(dest) {
                            popUpTo(Route.Splash.path) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Route.Home.path) {
            // Production SOS: always fire through the countdown overlay host.
            // Plan gate is skipped so a real emergency is never blocked by UI.
            HomeScreen(
                onSosClick = { onRequestEmergency(HomeMode.SOS) },
                onMenuClick = { navController.navigate(Route.Menu.path) }
            )
        }

        composable(Route.Menu.path) {
            val invite = rememberInviteAction()

            MenuScreen(
                onItemClick = { item ->
                    when (item) {
                        // Hands the invite to WhatsApp and stays put; every other
                        // tile is a destination.
                        MenuItem.INVITE -> invite()
                        MenuItem.ADD_CONTACT -> navController.navigate(
                            Route.AddContact.pathFor(ContactType.SOS.name)
                        )
                        else -> item.route?.let { navController.navigate(it.path) }
                    }
                },
                onSettingsClick = { navController.navigate(Route.Settings.path) },
                onClose = { navController.popBackStack() }
            )
        }

        composable(Route.WhoIsPukaarFor.path) {
            WhoIsPukaarForScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.HowItWorks.path) {
            HowItWorksHubScreen(
                onTypeSelected = { type -> navController.navigate(type.guideRoute.path) },
                onEmergencyCardClick = {
                    navController.navigate(Route.EmergencyCardGuide.path)
                },
                onWhenToUseClick = { navController.navigate(Route.WhenToUse.path) },
                onSafeArrivalClick = { navController.navigate(Route.SafeArrivalGuide.path) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.SafeArrivalGuide.path) {
            SafeArrivalGuideScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.WhenToUse.path) {
            WhenToUseScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.SosGuide.path) {
            SosGuideScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.InactivityGuide.path) {
            InactivityGuideScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.EmergencyCardGuide.path) {
            EmergencyCardGuideScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.MockDrill.path) {
            MockDrillChooserScreen(
                onDrillSelected = { drill ->
                    // Production: selecting SOS also kicks off a live mock drill session.
                    if (drill.route == Route.SosDrill) {
                        onRequestMockDrill(HomeMode.SOS)
                    }
                    navController.navigate(drill.route.path)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.SosDrill.path) {
            SosDrillScreen(onBack = { navController.popBackStack() })
        }

        composable(Route.TripShield.path) {
            TripShieldScreen(
                onBack = { navController.popBackStack() },
                // Straight out to the SOS screen, past the menu in between.
                onBackToHome = {
                    navController.popBackStack(Route.Home.path, inclusive = false)
                }
                // onItemClick is left at its no-op default: the rows press but
                // nothing behind them is built yet.
            )
        }

        composable(Route.PaymentReferral.path) {
            PlansScreen(
                onBack = { navController.popBackStack() },
                onPlanSelected = { plan ->
                    actions.upgradePlan(plan)
                    showSuccess(SuccessType.PAYMENT_COMPLETED)
                }
            )
        }

        composable(Route.QuickOnboarding.path) {
            // The notice gates the hub rather than sitting on it: whose phone this
            // is decides everything the setups collect, and it cannot be changed
            // from inside them. Saved against this back stack entry, so stepping
            // into a setup and back does not ask again, while leaving for the menu
            // and returning does.
            var readTheNotice by rememberSaveable { mutableStateOf(false) }

            // Which setup is waiting on a coupon; null when none has been picked.
            var couponFor by rememberSaveable { mutableStateOf<ProtectionType?>(null) }

            OnboardingHubScreen(
                // Picking a protection opens its coupon gate rather than its first
                // step — the code is asked for before any work, so a refused one
                // costs nothing rather than an evening of verifying contacts.
                onTypeSelected = { type -> couponFor = type },
                onBack = { navController.popBackStack() }
            )

            if (!readTheNotice) {
                var loadedName by remember { mutableStateOf("") }
                LaunchedEffect(Unit) {
                    loadedName = runCatching {
                        PukaarApp.instance.repository.me().fullName.orEmpty()
                    }.getOrDefault("")
                }
                BeforeYouStartDialog(
                    onContinue = { name ->
                        actions.saveUserDisplayName(name)
                        readTheNotice = true
                    },
                    initialName = loadedName,
                    onDismiss = {
                        // First-run starts here — dismissing must still leave a usable app.
                        if (!navController.popBackStack()) {
                            navController.navigate(Route.Home.path) {
                                popUpTo(Route.QuickOnboarding.path) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            } else {
                couponFor?.let { type ->
                    CouponDialog(
                        accent = type.accent,
                        onProceed = { code ->
                            actions.redeemCoupon(code, type)
                            couponFor = null
                            navController.navigate(type.setupRoute.path)
                        },
                        // Closing this one returns to the hub, not to the menu: the
                        // notice has been read, and the other protection may still
                        // be what they wanted.
                        onDismiss = { couponFor = null }
                    )
                }
            }
        }

        // Wait until contacts are saved+verified on the server before leaving.
        fun finishOnboarding(result: com.pukaar.app.ui.screen.onboarding.OnboardingResult) {
            actions.completeOnboarding(result) { ok ->
                if (!ok) return@completeOnboarding
                navController.navigate(Route.Home.path) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }

        composable(Route.SosOnboarding.path) {
            SosOnboardingScreen(
                onBack = { navController.popBackStack() },
                onFinished = ::finishOnboarding,
                onShareApp = actions::shareApp
            )
        }

        composable(Route.InactivityOnboarding.path) {
            InactivityOnboardingScreen(
                onBack = { navController.popBackStack() },
                onFinished = ::finishOnboarding,
                onShareApp = actions::shareApp
            )
        }

        composable(Route.ViewContacts.path) {
            LaunchedEffect(Unit) { onContactsRefresh?.invoke() }
            val listed = contacts.ifEmpty { actions.loadContacts() }
            ViewContactsScreen(
                contacts = listed.alertContactsOnly(),
                preSavedNumbers = listed.asPreSavedNumbers() + actions.loadPreSavedNumbers(),
                inactivityTiming = actions.loadInactivityTiming(),
                onBack = { navController.popBackStack() },
                onAddContact = { type ->
                    navController.navigate(Route.AddContact.pathFor(type.name))
                },
                onSaveContact = { contact ->
                    val draft = contact.toDraft()
                    if (onSaveContact != null) {
                        onSaveContact(draft) { onContactsRefresh?.invoke() }
                    } else {
                        actions.saveContact(contact)
                        onContactsRefresh?.invoke()
                    }
                },
                onDeleteContact = { contact ->
                    if (onDeleteContact != null) {
                        onDeleteContact(contact.id) { onContactsRefresh?.invoke() }
                    } else {
                        actions.deleteContact(contact)
                        onContactsRefresh?.invoke()
                    }
                },
                onSavePreSavedNumber = { number ->
                    val draft = number.toDraft()
                    if (onSaveContact != null) {
                        onSaveContact(draft) { onContactsRefresh?.invoke() }
                    } else {
                        actions.savePreSavedNumber(number)
                        onContactsRefresh?.invoke()
                    }
                },
                onDeletePreSavedNumber = { number ->
                    if (onDeleteContact != null) {
                        onDeleteContact(number.id) { onContactsRefresh?.invoke() }
                    } else {
                        actions.deletePreSavedNumber(number)
                        onContactsRefresh?.invoke()
                    }
                },
                onSaveInactivityTiming = { timing ->
                    actions.saveInactivityTiming(timing)
                }
            )
        }

        composable(
            route = Route.AddContact.path,
            arguments = listOf(
                navArgument(Route.AddContact.ARG_TYPE) {
                    type = NavType.StringType
                    defaultValue = ContactType.SOS.name
                }
            )
        ) { entry ->
            val typeName = entry.arguments?.getString(Route.AddContact.ARG_TYPE)
            val initialType = runCatching {
                ContactType.valueOf(typeName ?: ContactType.SOS.name)
            }.getOrDefault(ContactType.SOS)
            ContactFormScreen(
                initial = ContactDraft(
                    name = "",
                    mobile = "",
                    relationship = "",
                    type = initialType
                ),
                onBack = { navController.popBackStack() },
                onSave = { draft ->
                    if (onSaveContact != null) {
                        onSaveContact(draft) {
                            showSuccess(SuccessType.CONTACT_ADDED)
                            navController.popBackStack()
                        }
                    } else {
                        actions.saveContact(draft)
                        showSuccess(SuccessType.CONTACT_ADDED)
                        navController.popBackStack()
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
                ?: actions.loadContacts().firstOrNull { it.id == contactId }
            if (contact != null) {
                val scope = rememberCoroutineScope()
                val context = androidx.compose.ui.platform.LocalContext.current
                ContactFormScreen(
                    initial = contact.toDraft(),
                    onBack = { navController.popBackStack() },
                    onSave = { draft ->
                        if (onSaveContact != null) {
                            onSaveContact(draft) { showSuccess(SuccessType.CONTACT_ADDED) }
                        } else {
                            actions.saveContact(draft)
                            showSuccess(SuccessType.CONTACT_ADDED)
                        }
                    },
                    onDelete = {
                        onDeleteContact?.invoke(contactId) { navController.popBackStack() }
                    },
                    onResendVerification = { onResendVerification?.invoke(contact) },
                    onVerifyCode = if (!contact.verified) {
                        { code ->
                            scope.launch {
                                ContactRepositoryBridge.verifyContact(contact.id, code)
                                    .onSuccess {
                                        onContactsRefresh?.invoke()
                                        android.widget.Toast.makeText(
                                            context,
                                            "Contact verified",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    .onFailure {
                                        android.widget.Toast.makeText(
                                            context,
                                            it.message ?: "Verification failed",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                            }
                        }
                    } else null
                )
            }
        }

        composable(Route.Settings.path) {
            val context = androidx.compose.ui.platform.LocalContext.current
            com.pukaar.app.ui.screen.settings.SettingsScreen(
                onBack = { navController.popBackStack() },
                onBatteryOptimization = { com.pukaar.app.emergency.OemBatteryHelper.requestUnrestrictedBattery(context) },
                onAutostart = { com.pukaar.app.emergency.OemBatteryHelper.openOemAutostartSettings(context) },
                onOverlayPermission = { com.pukaar.app.emergency.OemBatteryHelper.requestOverlayPermission(context) },
                onVolumeSosAccessibility = { com.pukaar.app.emergency.AccessibilityHelper.openAccessibilitySettings(context) }
            )
        }

        composable(Route.GeneralSettings.path) {
            GeneralSettingsScreen(
                onBack = { navController.popBackStack() },
                onSave = { settings ->
                    actions.saveGeneralSettings(settings)
                    showSuccess(SuccessType.GENERAL_SETTINGS_SAVED)
                }
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

        composable(Route.Faq.path) {
            FaqScreen(
                onBack = { navController.popBackStack() },
                onEntryClick = actions::openFaqEntry
            )
        }

        composable(Route.EmergencyCard.path) {
            // The gate sits on the form rather than in front of it: seeing the card
            // they were about to make is the argument for paying for it.
            var planGate by rememberSaveable { mutableStateOf(!actions.isPlanActive()) }

            EmergencyCardFlowScreen(
                draft = cardDraft,
                onDraftChange = { cardDraft = it },
                onGenerate = {
                    actions.saveEmergencyCard(cardDraft)
                    // Replaces the builder rather than stacking on it: Back from
                    // the finished card returns to the menu, and "Edit Card" is
                    // the way back into the form. launchSingleTop keeps a second
                    // trip through the form from leaving two finished cards on
                    // the stack.
                    navController.navigate(Route.EmergencyCardReady.path) {
                        popUpTo(Route.EmergencyCard.path) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onExit = { navController.popBackStack() }
            )

            if (planGate) {
                PlanInactiveDialog(
                    onActivate = {
                        planGate = false
                        navController.navigate(Route.PaymentReferral.path)
                    },
                    // Left on the form rather than sent back: somebody who wants to
                    // read the fields before deciding should be able to.
                    onDismiss = { planGate = false }
                )
            }
        }

        composable(Route.EmergencyCardReady.path) {
            CardReadyScreen(
                draft = cardDraft,
                style = cardQrStyle,
                onStyleChange = { cardQrStyle = it },
                onSaveQr = { actions.saveCardQr(cardDraft) },
                onShareQr = { actions.shareCardQr(cardDraft) },
                onDownload = { navController.navigate(Route.EmergencyCardDownload.path) },
                onSetLockScreen = {
                    navController.navigate(Route.EmergencyCardLockScreen.path)
                },
                onEdit = { navController.navigate(Route.EmergencyCard.path) },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.EmergencyCardDownload.path) {
            DownloadCardScreen(
                options = printOptions,
                onOptionsChange = { printOptions = it },
                onDownload = {
                    actions.downloadCard(cardDraft, printOptions)
                    navController.navigate(Route.EmergencyCardPrintable.path)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Route.EmergencyCardPrintable.path) {
            PrintableCardScreen(
                draft = cardDraft,
                size = printOptions.size,
                onBack = { navController.popBackStack() },
                style = cardQrStyle
            )
        }

        composable(Route.EmergencyCardLockScreen.path) {
            LockScreenCardScreen(
                draft = cardDraft,
                onBack = { navController.popBackStack() },
                style = cardQrStyle
            )
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
