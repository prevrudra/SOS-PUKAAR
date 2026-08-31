package com.pukaar.app.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pukaar.app.PukaarApp
import com.pukaar.app.emergency.EmergencyForegroundService
import com.pukaar.app.ui.navigation.PukaarNavHost
import com.pukaar.app.ui.navigation.Route
import com.pukaar.app.ui.screen.contacts.ContactDraft
import com.pukaar.app.ui.screen.contacts.ContactUiModel
import com.pukaar.app.ui.theme.Black
import com.pukaar.app.ui.theme.PukaarOrange
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun PukaarAppNavHost() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var authed by remember { mutableStateOf<Boolean?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val emergencyNav = rememberNavController()

    LaunchedEffect(Unit) {
        authed = PukaarApp.instance.sessionStore.token() != null
    }

    when (authed) {
        null -> Box(Modifier.fillMaxSize().background(Black))
        false -> PukaarTheme {
            OtpLoginScreen { authed = true }
        }
        true -> PukaarTheme {
            var contacts by remember { mutableStateOf<List<ContactUiModel>>(emptyList()) }
            val actions = remember {
                PukaarActionsImpl(
                    context = context,
                    scope = scope,
                    onEmergency = { id -> emergencyNav.navigate("emergency/$id") },
                    onError = { error = it }
                )
            }

            LaunchedEffect(Unit) {
                contacts = ContactRepositoryBridge.loadContacts()
            }

            LaunchedEffect(Unit) {
                PukaarApp.instance.hardwareSos.collect {
                    actions.triggerSos()
                }
            }

            error?.let { msg ->
                LaunchedEffect(msg) {
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                    error = null
                }
            }

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
                                ContactRepositoryBridge.saveContactAndOpenSms(context, draft, name)
                                    .onSuccess {
                                        contacts = ContactRepositoryBridge.loadContacts()
                                        onDone()
                                    }
                                    .onFailure { error = it.message }
                            }
                        },
                        onContactsRefresh = {
                            scope.launch { contacts = ContactRepositoryBridge.loadContacts() }
                        }
                    )
                }
                composable("emergency/{eventId}") { entry ->
                    EmergencyActiveRoute(
                        eventId = entry.arguments?.getString("eventId") ?: "",
                        onClosed = {
                            emergencyNav.popBackStack(Route.Home.path, inclusive = false)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmergencyActiveRoute(eventId: String, onClosed: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("ACTIVE") }

    LaunchedEffect(eventId) {
        while (true) {
            val e = runCatching { PukaarApp.instance.repository.getEmergency(eventId) }.getOrNull()
            if (e?.active == false) {
                EmergencyForegroundService.stop(context)
                onClosed()
                break
            }
            status = e?.status ?: "ACTIVE"
            kotlinx.coroutines.delay(3000)
        }
    }

    Column(
        Modifier.fillMaxSize().background(Black).padding(24.dp)
    ) {
        Text("EMERGENCY ACTIVE", color = PukaarRed, fontSize = 28.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(12.dp))
        Text("Status: $status", color = Color.White)
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                scope.launch {
                    runCatching { PukaarApp.instance.repository.markSafe(eventId) }
                    EmergencyForegroundService.stop(context)
                    onClosed()
                }
            },
            modifier = Modifier.fillMaxWidth().height(60.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PukaarOrange),
            shape = RoundedCornerShape(14.dp)
        ) { Text("I'M SAFE", fontWeight = FontWeight.Black, fontSize = 20.sp) }
        Spacer(Modifier.height(8.dp))
        Text("PUKAAR does not guarantee rescue.", color = TextSecondary, fontSize = 12.sp)
    }
}
