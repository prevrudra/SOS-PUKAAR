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
import com.pukaar.app.ui.screen.contacts.ContactType
import com.pukaar.app.ui.screen.contacts.ContactUiModel
import com.pukaar.app.ui.screen.emergency.EmergencyActiveScreen
import com.pukaar.app.ui.screen.home.HomeMode
import com.pukaar.app.ui.screen.home.SosCountdownOverlay
import com.pukaar.app.ui.screen.home.SosModeSelectOverlay
import com.pukaar.app.ui.screen.home.EmergencySendingOverlay
import com.pukaar.app.ui.screen.splash.SplashScreen
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.util.EmergencyAlertHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

@Composable
fun PukaarAppNavHost() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var authed by remember { mutableStateOf<Boolean?>(null) }
    var onboardingDone by remember { mutableStateOf<Boolean?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var countdownMode by remember { mutableStateOf<HomeMode?>(null) }
    /** SOS button → pick SOS/HELP, then countdown. */
    var showModeSelect by remember { mutableStateOf(false) }
    /** Keeps home covered after countdown until SOS/HELP active screen opens. */
    var sendingMode by remember { mutableStateOf<HomeMode?>(null) }
    val emergencyNav = rememberNavController()

    var showDeviceRestore by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val token = PukaarApp.instance.sessionStore.token()
            if (token != null) {
                val done = runCatching {
                    PukaarApp.instance.sessionStore.onboardingComplete.first()
                }.getOrDefault(false)
                withContext(Dispatchers.Main) {
                    authed = true
                    onboardingDone = done
                }
                runCatching {
                    withTimeout(12_000) {
                        PukaarApp.instance.repository.syncSession()
                    }
                    val synced = PukaarApp.instance.sessionStore.onboardingComplete.first()
                    withContext(Dispatchers.Main) { onboardingDone = synced }
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
                scope.launch {
                    onboardingDone = runCatching {
                        PukaarApp.instance.sessionStore.onboardingComplete.first()
                    }.getOrDefault(false)
                    authed = true
                    com.pukaar.app.emergency.PukaarGuardService.start(context, hasSession = true)
                    withContext(Dispatchers.IO) {
                        runCatching {
                            withTimeout(12_000) {
                                PukaarApp.instance.repository.syncSession()
                            }
                        }
                        onboardingDone = runCatching {
                            PukaarApp.instance.sessionStore.onboardingComplete.first()
                        }.getOrDefault(onboardingDone == true)
                    }
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
                    showModeSelect = true
                }
                PukaarApp.instance.hardwareSos.collect {
                    showModeSelect = true
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
                                        .onSuccess { id ->
                                            contacts = ContactRepositoryBridge.loadContacts()
                                            val saved = contacts.firstOrNull { it.id == id }
                                            if (saved != null && !saved.verified &&
                                                (saved.type == ContactType.SOS || saved.type == ContactType.INACTIVITY)
                                            ) {
                                                android.widget.Toast.makeText(
                                                    context,
                                                    "Number changed — enter the OTP sent to ${saved.name} before they can receive alerts.",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
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
                            onRequestEmergency = { _ -> showModeSelect = true },
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

                if (showModeSelect) {
                    SosModeSelectOverlay(
                        onSelect = { mode ->
                            showModeSelect = false
                            countdownMode = mode
                        },
                        onCancel = { showModeSelect = false }
                    )
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
                if (countdownMode == null && !showModeSelect) {
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
            val fetched = runCatching { PukaarApp.instance.repository.getEmergency(eventId) }.getOrNull()
            if (fetched != null && fetched.active != false) {
                // Enrich nearby without re-pushing GPS every poll (FGS already does that).
                val enriched = runCatching {
                    com.pukaar.app.util.NearbyServicesHelper.enrich(
                        fetched,
                        context = context,
                        pushLocation = false
                    )
                }.getOrDefault(fetched)
                // Never let a later poll wipe real hospital/ambulance names with placeholders.
                event = com.pukaar.app.util.NearbyServicesHelper.mergeNearbyPreferReal(event, enriched)
            } else if (fetched != null) {
                event = fetched
            }
            if (fetched?.active == false) {
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
                var closedOk = false
                try {
                    if (isMockDrill) {
                        val result = runCatching {
                            PukaarApp.instance.repository.completeLatestDrill(confirmed = true)
                        }
                        if (result.isFailure) {
                            // Soft-close the drill event so UI never hangs if contacts aren't verified yet
                            closedOk = runCatching {
                                PukaarApp.instance.repository.markSafe(eventId)
                            }.isSuccess
                            android.widget.Toast.makeText(
                                context,
                                result.exceptionOrNull()?.message
                                    ?: "Drill closed. Add & verify 2 contacts, then try again for protection unlock.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        } else {
                            closedOk = true
                            PukaarApp.instance.sessionStore.setMockDrillPassed(true)
                            PukaarApp.instance.sessionStore.setProtectionReady(true)
                        }
                    } else {
                        var safeResult: Result<Any?> = Result.failure(IllegalStateException("not tried"))
                        repeat(3) { attempt ->
                            safeResult = runCatching {
                                PukaarApp.instance.repository.markSafe(eventId)
                            }
                            if (safeResult.isSuccess) return@repeat
                            // Already closed on server after a slow first attempt — treat as success.
                            if (isAlreadyClosed(safeResult.exceptionOrNull())) {
                                safeResult = Result.success(Unit)
                                return@repeat
                            }
                            kotlinx.coroutines.delay(600L * (attempt + 1))
                        }
                        // If API still timed out, poll once — WA may already have been sent.
                        if (safeResult.isFailure) {
                            val stillOpen = runCatching {
                                PukaarApp.instance.repository.activeEmergency()?.active == true
                            }.getOrDefault(true)
                            if (!stillOpen) {
                                safeResult = Result.success(Unit)
                            }
                        }
                        closedOk = safeResult.isSuccess
                        if (!closedOk) {
                            val who = event?.userName
                                ?: runCatching { PukaarApp.instance.repository.me().fullName }.getOrNull()
                                ?: "PUKAAR user"
                            runCatching {
                                com.pukaar.app.util.EmergencyAlertHelper.sendSafeSmsToContacts(context, who)
                            }
                        }
                        android.widget.Toast.makeText(
                            context,
                            if (closedOk) {
                                "Contacts notified that you are safe"
                            } else {
                                "Could not reach server — safe SMS sent. Tap I'm Safe again if alert stays open."
                            },
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }
                } finally {
                    if (closedOk || isMockDrill) {
                        runCatching { EmergencyForegroundService.stop(context) }
                        com.pukaar.app.emergency.EmergencySessionStore.clear(context)
                        onClosed()
                    } else {
                        // Keep screen open and resume location until server confirms close
                        val isSos = event?.triggerType?.equals("HELP", ignoreCase = true) != true
                        EmergencyForegroundService.start(
                            context,
                            eventId,
                            isSos = isSos,
                            recordAudio = false
                        )
                        finishing = false
                    }
                }
            }
        }
    )
}

private fun isAlreadyClosed(error: Throwable?): Boolean {
    if (error == null) return false
    val msg = error.message?.uppercase() ?: ""
    if (msg.contains("EVENT_CLOSED") || msg.contains("ALREADY CLOSED")) return true
    if (error is retrofit2.HttpException) {
        val body = runCatching { error.response()?.errorBody()?.string() }.getOrNull()?.uppercase() ?: ""
        if (body.contains("EVENT_CLOSED") || body.contains("ALREADY CLOSED")) return true
        // Idempotent success from server after close can also be 200; 409/400 with code.
        if (error.code() == 409 || error.code() == 400) {
            return body.contains("CLOSED")
        }
    }
    return false
}
