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
import com.pukaar.app.data.api.EmergencyDto
import com.pukaar.app.emergency.EmergencyForegroundService
import com.pukaar.app.ui.navigation.PukaarNavHost
import com.pukaar.app.ui.navigation.Route
import com.pukaar.app.ui.screen.contacts.ContactDraft
import com.pukaar.app.ui.screen.contacts.ContactUiModel
import com.pukaar.app.ui.screen.emergency.EmergencyActiveScreen
import com.pukaar.app.ui.screen.onboarding.OnboardingConsentScreen
import com.pukaar.app.ui.screen.home.HomeMode
import com.pukaar.app.ui.screen.home.SosCountdownOverlay
import com.pukaar.app.ui.screen.splash.SplashScreen
import com.pukaar.app.ui.theme.PukaarTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun PukaarAppNavHost() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var authed by remember { mutableStateOf<Boolean?>(null) }
    var onboardingDone by remember { mutableStateOf<Boolean?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
  var countdownMode by remember { mutableStateOf<HomeMode?>(null) }
    val emergencyNav = rememberNavController()

    LaunchedEffect(Unit) {
        authed = PukaarApp.instance.sessionStore.token() != null
        if (authed == true) {
            runCatching { PukaarApp.instance.repository.syncSession() }
            onboardingDone = PukaarApp.instance.sessionStore.onboardingComplete.first()
        }
    }

    when {
        authed == null || (authed == true && onboardingDone == null) -> SplashScreen()
        authed == false -> PukaarTheme {
            OtpLoginScreen {
                authed = true
                onboardingDone = false
                com.pukaar.app.emergency.PukaarGuardService.start(context, hasSession = true)
            }
        }
        onboardingDone == false -> PukaarTheme {
            OnboardingConsentScreen {
                onboardingDone = true
                com.pukaar.app.emergency.PukaarGuardService.start(context, hasSession = true)
            }
        }
        else -> PukaarTheme {
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { }

            LaunchedEffect(Unit) {
                val needed = buildList {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                    add(Manifest.permission.RECORD_AUDIO)
                    add(Manifest.permission.CALL_PHONE)
                    add(Manifest.permission.SEND_SMS)
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
                        runCatching {
                            emergencyNav.navigate("emergency/$id?mock=$isMock") {
                                launchSingleTop = true
                            }
                        }.onFailure {
                            error = "Could not open emergency screen"
                        }
                    },
                    onError = { error = it }
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
                val active = runCatching { PukaarApp.instance.repository.activeEmergency() }.getOrNull()
                if (active?.active == true && active.id != null) {
                    val mock = active.mockDrill == true
                    emergencyNav.navigate("emergency/${active.id}?mock=$mock")
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
                            countdownMode = null
                            when (mode) {
                                HomeMode.SOS -> actions.triggerSos()
                                HomeMode.HELP -> actions.triggerHelp()
                            }
                        },
                        onCancel = { countdownMode = null }
                    )
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
            val e = runCatching { PukaarApp.instance.repository.getEmergency(eventId) }.getOrNull()
            event = e
            if (e?.active == false && !isMockDrill) {
                EmergencyForegroundService.stop(context)
                onClosed()
                break
            }
            kotlinx.coroutines.delay(3000)
        }
    }

    EmergencyActiveScreen(
        event = event,
        isMockDrill = isMockDrill,
        onMarkSafe = {
            if (finishing) return@EmergencyActiveScreen
            finishing = true
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
                    }
                } finally {
                    runCatching { EmergencyForegroundService.stop(context) }
                    onClosed()
                }
            }
        }
    )
}
