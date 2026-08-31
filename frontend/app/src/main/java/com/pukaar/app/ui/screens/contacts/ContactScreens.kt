package com.pukaar.app.ui.screens.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.PukaarApp
import com.pukaar.app.data.api.ContactDto
import com.pukaar.app.data.api.ContactRequest
import com.pukaar.app.ui.theme.PukaarMuted
import com.pukaar.app.ui.theme.SosRed
import com.pukaar.app.util.userMessage
import kotlinx.coroutines.launch

@Composable
fun AddContactScreen(onBack: () -> Unit, onDone: () -> Unit, onboarding: Boolean = false) {
    var contacts by remember { mutableStateOf<List<ContactDto>>(emptyList()) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    var pendingId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            contacts = runCatching { PukaarApp.instance.repository.contacts() }.getOrDefault(emptyList())
        }
    }

    LaunchedEffect(Unit) { refresh() }

    val verifiedCount = contacts.count { it.verified == true }
    val canContinue = if (onboarding) verifiedCount >= 2 else contacts.isNotEmpty()

    Column(Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Text(
                if (onboarding) "Trusted Contacts" else "Add Contact",
                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold
            )
        }
        if (onboarding) {
            Spacer(Modifier.height(8.dp))
            Text("Add min 2 and max 3 trusted contacts. Each must be verified.", color = PukaarMuted)
        }
        Spacer(Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            items(contacts) { c ->
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFF1C1C1C), RoundedCornerShape(10.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(c.name ?: "", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(c.phone ?: "", color = PukaarMuted, fontSize = 13.sp)
                    }
                    if (c.verified == true) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF66BB6A))
                    } else {
                        Text("Unverified", color = SosRed, fontSize = 12.sp)
                    }
                }
            }
        }

        if (showForm || (!onboarding && contacts.isEmpty())) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(), colors = fieldColors())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") },
                modifier = Modifier.fillMaxWidth(), colors = fieldColors())
            if (pendingId != null) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Verification code") },
                    modifier = Modifier.fillMaxWidth(), colors = fieldColors())
                Text("Dev code: 123456", color = PukaarMuted, fontSize = 12.sp)
            }
        }

        error?.let { Text(it, color = SosRed, modifier = Modifier.padding(vertical = 4.dp)) }

        if (onboarding && contacts.size < 3 && !showForm) {
            OutlinedButton(
                onClick = { showForm = true; pendingId = null; name = ""; phone = ""; code = "" },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ADD CONTACT", color = SosRed) }
            Spacer(Modifier.height(8.dp))
        }

        if (showForm || (!onboarding && contacts.isEmpty())) {
            Button(
                onClick = {
                    if (busy) return@Button
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            if (pendingId == null) {
                                val c = PukaarApp.instance.repository.addContact(ContactRequest(name.trim(), phone.trim()))
                                pendingId = c.id
                                showForm = true
                            } else if (code.length == 6 || code == "123456") {
                                PukaarApp.instance.repository.verifyContact(pendingId!!, code)
                                name = ""; phone = ""; code = ""; pendingId = null; showForm = false
                                refresh()
                            } else {
                                error = "Enter the 6-digit verification code"
                            }
                        } catch (e: Exception) {
                            error = e.userMessage()
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy && (pendingId != null || (name.isNotBlank() && phone.isNotBlank())),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SosRed)
            ) {
                Text(if (pendingId == null) "SAVE & SEND CODE" else "VERIFY CONTACT")
            }
        }

        if (onboarding) {
            Button(
                onClick = onDone,
                enabled = canContinue,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SosRed)
            ) { Text("PROCEED") }
        } else if (!showForm && contacts.isNotEmpty()) {
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SosRed)
            ) { Text("Done") }
        }
    }
}

@Composable
fun ViewContactsScreen(onBack: () -> Unit) {
    var contacts by remember { mutableStateOf<List<ContactDto>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun refresh() {
        scope.launch {
            contacts = runCatching { PukaarApp.instance.repository.contacts() }.getOrDefault(emptyList())
        }
    }

    LaunchedEffect(Unit) { refresh() }

    Column(Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Text("Trusted Contacts", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        error?.let { Text(it, color = SosRed) }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(contacts, key = { it.id ?: it.phone ?: "" }) { c ->
                Row(
                    Modifier.fillMaxWidth().background(Color(0xFF1C1C1C)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(c.name ?: "", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("${c.phone} · ${if (c.verified == true) "Verified" else "Pending"}", color = PukaarMuted)
                    }
                    IconButton(onClick = {
                        c.id?.let { id ->
                            scope.launch {
                                try {
                                    PukaarApp.instance.repository.deleteContact(id)
                                    refresh()
                                } catch (e: Exception) {
                                    error = e.userMessage()
                                }
                            }
                        }
                    }) {
                        Icon(Icons.Default.Delete, null, tint = SosRed)
                    }
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = SosRed,
    unfocusedBorderColor = PukaarMuted,
    focusedLabelColor = PukaarMuted,
    unfocusedLabelColor = PukaarMuted,
    cursorColor = SosRed
)
