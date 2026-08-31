package com.pukaar.app.ui.screens.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.PukaarApp
import com.pukaar.app.data.api.SubscriptionStatusResponse
import com.pukaar.app.ui.theme.PukaarMuted
import com.pukaar.app.ui.theme.SosRed
import com.pukaar.app.util.userMessage
import com.pukaar.app.ui.theme.TileGray
import kotlinx.coroutines.launch

data class MenuTile(val title: String, val route: String, val priority: Boolean)

private val tiles = listOf(
    MenuTile("How PUKAAR Works", "how_works", true),
    MenuTile("What Happens After SOS", "after_sos", true),
    MenuTile("Add Contact", "add_contact", true),
    MenuTile("View Contacts", "view_contacts", true),
    MenuTile("Mock Drill", "mock_drill", true),
    MenuTile("Elderly Help", "elderly_help", true),
    MenuTile("Payment / Plan", "payment", false),
    MenuTile("Instructions", "instructions", false),
    MenuTile("Watch Video", "watch_video", false),
    MenuTile("FAQ", "faq", false),
    MenuTile("Language", "language", false),
    MenuTile("Settings / Privacy", "settings", false),
    MenuTile("Permissions", "permissions", false)
)

@Composable
fun MenuScreen(onBack: () -> Unit, onTile: (String) -> Unit) {
    Column(Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("MENU", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Text("Priority", color = PukaarMuted, fontSize = 12.sp)
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(tiles.filter { it.priority }) { tile ->
                Tile(tile.title) { onTile(tile.route) }
            }
            item {
                Text("More", color = PukaarMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
            }
            item { Spacer(Modifier) }
            items(tiles.filter { !it.priority }) { tile ->
                Tile(tile.title) { onTile(tile.route) }
            }
        }
    }
}

@Composable
private fun Tile(title: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(110.dp)
            .background(TileGray, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(title, color = Color.White, textAlign = TextAlign.Center, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun InfoScreen(title: String, onBack: () -> Unit) {
    val copy = when (title) {
        "how_works" -> "Three triggers feed one engine: App SOS/HELP, hardware power sequence (device dependent), and voice phrase (device dependent). PUKAAR then notifies contacts, shares location, records audio evidence in one-minute segments, and opens 112 pathway where supported."
        "after_sos" -> "TRIGGERED → LOCATION → CONTACTS NOTIFIED → AUDIO RECORDING → SEGMENTS UPLOADING → 112 PATHWAY → LIVE LOCATION → WAITING FOR I'M SAFE → CLOSED. One failed step never stops the others."
        "elderly_help" -> "HELP is not automatic police/112. Monitoring contacts get a call-first alert. Configure inactivity soft/medium/urgent checks. Wording: No qualifying activity detected."
        "instructions" -> "Keep PUKAAR unrestricted in battery settings. Complete mock drill before protection is ready. One home button. Everything else lives in MENU."
        "faq" -> "Does PUKAAR replace 112? No. Is evidence cloud-safe offline? No — only after successful upload. Can home mode change? Yes, from Settings."
        else -> "PUKAAR content page."
    }
    Column(Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Text(title.replace('_', ' ').uppercase(), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Text(copy, color = PukaarMuted, fontSize = 16.sp, lineHeight = 24.sp)
    }
}

@Composable
fun PaymentScreen(onBack: () -> Unit, onActivated: () -> Unit) {
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf("No free plan. Activate protection to stay ready.") }
    var busy by remember { mutableStateOf(false) }
    var hasSubscription by remember { mutableStateOf(false) }
    var individualPrice by remember { mutableStateOf(499) }
    var familyPrice by remember { mutableStateOf(699) }

    LaunchedEffect(Unit) {
        runCatching {
            val status: SubscriptionStatusResponse = PukaarApp.instance.repository.subscription()
            individualPrice = status.plans?.individual ?: 499
            familyPrice = status.plans?.family ?: 699
            if (status.subscription != null) {
                hasSubscription = true
                message = "Protection already active. You can continue to Home."
            }
        }
    }

    Column(Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Text("Payment / Plan", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        PlanCard("Individual", "₹$individualPrice / year", "Single user protection") {
            activatePlan("INDIVIDUAL", scope, onBusy = { busy = it }, onError = { message = it }, onSuccess = {
                hasSubscription = true
                scope.launch {
                    runCatching { PukaarApp.instance.repository.completeOnboarding() }
                    PukaarApp.instance.sessionStore.setProtectionReady(true)
                    onActivated()
                }
            })
        }
        Spacer(Modifier.height(12.dp))
        PlanCard("Family", "₹$familyPrice / year", "Up to 5 members") {
            activatePlan("FAMILY", scope, onBusy = { busy = it }, onError = { message = it }, onSuccess = {
                hasSubscription = true
                scope.launch {
                    runCatching { PukaarApp.instance.repository.completeOnboarding() }
                    PukaarApp.instance.sessionStore.setProtectionReady(true)
                    onActivated()
                }
            })
        }
        Spacer(Modifier.height(12.dp))
        if (hasSubscription) {
            TextButton(onClick = {
                scope.launch {
                    runCatching { PukaarApp.instance.repository.completeOnboarding() }
                    onActivated()
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Continue to Home", color = SosRed, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(message, color = PukaarMuted)
        Text("Referral: 3 successful paid activations unlock Family at ₹499.", color = PukaarMuted)
    }
}

private fun activatePlan(
    plan: String,
    scope: kotlinx.coroutines.CoroutineScope,
    onBusy: (Boolean) -> Unit,
    onError: (String) -> Unit,
    onSuccess: () -> Unit
) {
    scope.launch {
        onBusy(true)
        try {
            PukaarApp.instance.repository.activate(plan)
            onSuccess()
        } catch (e: Exception) {
            onError(e.userMessage())
        } finally {
            onBusy(false)
        }
    }
}

@Composable
private fun PlanCard(title: String, price: String, desc: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(100.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TileGray),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(price, color = SosRed, fontWeight = FontWeight.Bold)
            Text(desc, color = PukaarMuted)
        }
    }
}

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val homeMode by PukaarApp.instance.sessionStore.homeMode.collectAsState(initial = "SOS")
    Column(Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Text("Settings / Privacy", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        Text("Current home mode: $homeMode", color = Color.White)
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            scope.launch {
                val next = if (homeMode == "SOS") "HELP" else "SOS"
                PukaarApp.instance.repository.updateProfile(com.pukaar.app.data.api.ProfileUpdateRequest(homeMode = next))
                PukaarApp.instance.sessionStore.setHomeMode(next)
            }
        }, colors = ButtonDefaults.buttonColors(containerColor = TileGray), modifier = Modifier.fillMaxWidth()) {
            Text("Switch Home Mode")
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Evidence access is restricted. Privacy policy must match location, microphone, and cloud processing. Do not claim PUKAAR replaces police or ambulance.",
            color = PukaarMuted
        )
    }
}
