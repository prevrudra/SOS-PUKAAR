package com.pukaar.app.integration

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.pukaar.app.PukaarApp
import com.pukaar.app.R
import com.pukaar.app.data.api.EmergencyDto
import com.pukaar.app.emergency.EmergencyForegroundService
import com.pukaar.app.ui.navigation.PukaarNavHost
import com.pukaar.app.ui.navigation.Route
import com.pukaar.app.ui.screen.contacts.ContactDraft
import com.pukaar.app.ui.screen.contacts.ContactUiModel
import com.pukaar.app.ui.screen.emergency.EmergencyActiveScreen
import com.pukaar.app.ui.screen.home.HomeMode
import com.pukaar.app.ui.screen.home.SosCountdownOverlay
import com.pukaar.app.ui.screen.home.EmergencySendingOverlay
import com.pukaar.app.ui.screen.splash.SplashScreen
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.util.EmergencyAlertHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun PukaarAppNavHost() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var authed by remember { mutableStateOf<Boolean?>(null) }
    var onboardingDone by remember { mutableStateOf<Boolean?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var countdownMode by remember { mutableStateOf<HomeMode?>(null) }
    /** Keeps home covered after countdown until SOS/HELP active screen opens. */
    var sendingMode by remember { mutableStateOf<HomeMode?>(null) }
    val emergencyNav = rememberNavController()

    var showDeviceRestore by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val token = PukaarApp.instance.sessionStore.token()
            if (token != null) {
                runCatching { PukaarApp.instance.repository.syncSession() }
                val done = PukaarApp.instance.sessionStore.onboardingComplete.first()
                withContext(Dispatchers.Main) {
                    authed = true
                    onboardingDone = done
                }
            } else {
                withContext(Dispatchers.Main) { authed = false }
            }
        }
    }

    when {
        authed == null -> SplashScreen()
        authed == false -> PukaarTheme {
            OtpLoginScreen { restore ->
                showDeviceRestore = restore
                authed = true
                // Returning users already have onboarding on server; syncSession fills local prefs
                scope.launch {
                    withContext(Dispatchers.IO) {
                        runCatching { PukaarApp.instance.repository.syncSession() }
                    }
                    onboardingDone = PukaarApp.instance.sessionStore.onboardingComplete.first()
                    com.pukaar.app.emergency.PukaarGuardService.start(context, hasSession = true)
                }
            }
        }
        showDeviceRestore -> PukaarTheme {
            DeviceRestoreScreen {
                showDeviceRestore = false
            }
        }
        onboardingDone == null -> SplashScreen()
        else -> PukaarTheme {
            val forceQuickOnboarding = onboardingDone == false
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { }

            LaunchedEffect(Unit) {
                val needed = buildList {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                    add(Manifest.permission.RECORD_AUDIO)
                    add(Manifest.permission.CALL_PHONE)
                    if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                }.filter {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }
                if (needed.isNotEmpty()) permissionLauncher.launch(needed.toTypedArray())
            }

            var contacts by remember { mutableStateOf<List<ContactUiModel>>(emptyList()) }
            val actions = remember {
                PukaarActionsImpl(
                    context = context,
                    scope = scope,
                    onEmergency = { id, isMock ->
                        sendingMode = null
                        countdownMode = null
                        runCatching {
                            emergencyNav.navigate("emergency/$id?mock=$isMock") {
                                launchSingleTop = true
                            }
                        }.onFailure {
                            error = "Could not open emergency screen"
                        }
                    },
                    onError = {
                        sendingMode = null
                        error = it
                    },
                    onContactsChanged = {
                        scope.launch {
                            contacts = ContactRepositoryBridge.loadContacts()
                        }
                    },
                    onOnboardingFinished = {
                        onboardingDone = true
                        com.pukaar.app.emergency.PukaarGuardService.start(context, hasSession = true)
                    }
                )
            }

            LaunchedEffect(Unit) {
                contacts = ContactRepositoryBridge.loadContacts()
            }

            LaunchedEffect(Unit) {
                if (PukaarApp.instance.consumePendingHardwareSos()) {
                    countdownMode = HomeMode.SOS
                }
                PukaarApp.instance.hardwareSos.collect {
                    countdownMode = HomeMode.SOS
                }
            }

            LaunchedEffect(Unit) {
                val active = withContext(Dispatchers.IO) {
                    runCatching { PukaarApp.instance.repository.activeEmergency() }.getOrNull()
                } ?: return@LaunchedEffect
                if (active.active != true || active.id == null) return@LaunchedEffect
                val mock = active.mockDrill == true
                // Don't reopen Finish Drill after user already passed mock, or for stale drills
                if (mock) {
                    val passed = PukaarApp.instance.sessionStore.mockDrillPassed.first()
                    if (passed) {
                        withContext(Dispatchers.IO) {
                            runCatching { PukaarApp.instance.repository.markSafe(active.id) }
                        }
                        return@LaunchedEffect
                    }
                    val started = active.startedAt
                    if (!started.isNullOrBlank()) {
                        val stale = runCatching {
                            java.time.Instant.parse(started)
                                .isBefore(java.time.Instant.now().minusSeconds(10 * 60))
                        }.getOrDefault(false)
                        if (stale) {
                            withContext(Dispatchers.IO) {
                                runCatching { PukaarApp.instance.repository.markSafe(active.id) }
                            }
                            return@LaunchedEffect
                        }
                    }
                }
                emergencyNav.navigate("emergency/${active.id}?mock=$mock") {
                    launchSingleTop = true
                }
            }

            error?.let { msg ->
                LaunchedEffect(msg) {
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                    error = null
                }
            }

            Box(Modifier.fillMaxSize()) {
                NavHost(
                    navController = emergencyNav,
                    startDestination = "main"
                ) {
                    composable("main") {
                        PukaarNavHost(
                            actions = actions,
                            contacts = contacts,
                            startDestination = if (forceQuickOnboarding) {
                                Route.QuickOnboarding
                            } else {
                                Route.Splash
                            },
                            onSaveContact = { draft, onDone ->
                                scope.launch {
                                    val name = runCatching { PukaarApp.instance.repository.me().fullName }.getOrNull()
                                    ContactRepositoryBridge.saveContact(context, draft, name)
                                        .onSuccess {
                                            contacts = ContactRepositoryBridge.loadContacts()
                                            onDone()
                                        }
                                        .onFailure { error = it.message }
                                }
                            },
                            onDeleteContact = { id, onDone ->
                                scope.launch {
                                    ContactRepositoryBridge.deleteContact(id)
                                        .onSuccess {
                                            contacts = ContactRepositoryBridge.loadContacts()
                                            onDone()
                                        }
                                        .onFailure { error = it.message }
                                }
                            },
                            onResendVerification = { contact ->
                                scope.launch {
                                    val name = runCatching { PukaarApp.instance.repository.me().fullName }.getOrNull()
                                    ContactRepositoryBridge.resendVerification(context, contact, name)
                                }
                            },
                            onContactsRefresh = {
                                scope.launch { contacts = ContactRepositoryBridge.loadContacts() }
                            },
                            onRequestEmergency = { mode -> countdownMode = mode },
                            onRequestMockDrill = { mode ->
                                actions.startMockDrill(mode == HomeMode.SOS)
                            }
                        )
                    }
                    composable(
                        route = "emergency/{eventId}?mock={mock}",
                        arguments = listOf(
                            navArgument("eventId") { type = NavType.StringType },
                            navArgument("mock") { type = NavType.BoolType; defaultValue = false }
                        )
                    ) { entry ->
                        val eventId = entry.arguments?.getString("eventId") ?: ""
                        val isMock = entry.arguments?.getBoolean("mock") ?: false
                        EmergencyActiveRoute(
                            eventId = eventId,
                            isMockDrill = isMock,
                            onClosed = {
                                emergencyNav.popBackStack("main", inclusive = false)
                            }
                        )
                    }
                }

                countdownMode?.let { mode ->
                    SosCountdownOverlay(
                        mode = mode,
                        onComplete = {
                            // Stay full-screen while API + GPS finish — avoid home flash.
                            sendingMode = mode
                            countdownMode = null
                            when (mode) {
                                HomeMode.SOS -> actions.triggerSos()
                                HomeMode.HELP -> actions.triggerHelp()
                            }
                        },
                        onCancel = { countdownMode = null }
                    )
                }
                if (countdownMode == null) {
                    sendingMode?.let { mode ->
                        EmergencySendingOverlay(mode = mode)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmergencyActiveRoute(
    eventId: String,
    isMockDrill: Boolean,
    onClosed: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var event by remember { mutableStateOf<EmergencyDto?>(null) }
    var finishing by remember { mutableStateOf(false) }

    LaunchedEffect(eventId) {
        while (true) {
            if (finishing) break
            var e = runCatching { PukaarApp.instance.repository.getEmergency(eventId) }.getOrNull()
            if (e != null && e.active != false) {
                e = runCatching {
                    com.pukaar.app.util.NearbyServicesHelper.enrich(e!!, context)
                }.getOrDefault(e)
            }
            event = e
            if (e?.active == false) {
                EmergencyForegroundService.stop(context)
                onClosed()
                break
            }
            kotlinx.coroutines.delay(4000)
        }
    }

    EmergencyActiveScreen(
        event = event,
        isMockDrill = isMockDrill,
        onMarkSafe = {
            if (finishing) return@EmergencyActiveScreen
            finishing = true
            // Stop background recording immediately — do not wait for the API
            EmergencyForegroundService.stop(context)
            scope.launch {
                try {
                    if (isMockDrill) {
                        val result = runCatching {
                            PukaarApp.instance.repository.completeLatestDrill(confirmed = true)
                        }
                        if (result.isFailure) {
                            // Soft-close the drill event so UI never hangs if contacts aren't verified yet
                            runCatching { PukaarApp.instance.repository.markSafe(eventId) }
                            android.widget.Toast.makeText(
                                context,
                                result.exceptionOrNull()?.message
                                    ?: "Drill closed. Add & verify 2 contacts, then try again for protection unlock.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        } else {
                            PukaarApp.instance.sessionStore.setMockDrillPassed(true)
                            PukaarApp.instance.sessionStore.setProtectionReady(true)
                        }
                    } else {
                        runCatching { PukaarApp.instance.repository.markSafe(eventId) }
                        android.widget.Toast.makeText(
                            context,
                            "Contacts notified via WhatsApp that you are safe",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                } finally {
                    runCatching { EmergencyForegroundService.stop(context) }
                    onClosed()
                }
            }
        }
    )
}
