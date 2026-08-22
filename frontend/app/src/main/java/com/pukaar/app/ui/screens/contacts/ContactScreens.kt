package com.pukaar.app.ui.screens.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
fun AddContactScreen(onBack: () -> Unit, onDone: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Text("Add Contact", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors())
        error?.let { Text(it, color = SosRed) }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                scope.launch {
                    try {
                        PukaarApp.instance.repository.addContact(ContactRequest(name, phone))
                        onDone()
                    } catch (e: Exception) {
                        error = e.userMessage()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SosRed)
        ) { Text("Save Contact") }
        TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
            Text("Skip for now", color = PukaarMuted)
        }
    }
}

@Composable
fun ViewContactsScreen(onBack: () -> Unit) {
    var contacts by remember { mutableStateOf<List<ContactDto>>(emptyList()) }
    LaunchedEffect(Unit) {
        contacts = runCatching { PukaarApp.instance.repository.contacts() }.getOrDefault(emptyList())
    }
    Column(Modifier.fillMaxSize().background(Color.Black).padding(24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            Text("Trusted Contacts", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(contacts) { c ->
                Column(Modifier.fillMaxWidth().background(Color(0xFF1C1C1C)).padding(12.dp)) {
                    Text(c.name ?: "", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("${c.phone} · ${c.role}", color = PukaarMuted)
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
