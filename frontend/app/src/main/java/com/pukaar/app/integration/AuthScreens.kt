package com.pukaar.app.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.PukaarApp
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.TextSecondary
import com.pukaar.app.util.userMessage
import kotlinx.coroutines.launch

@Composable
fun OtpLoginScreen(onLoggedIn: () -> Unit) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var hint by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().background(Color.Black).padding(24.dp)
    ) {
        Text("PUKAAR", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(8.dp))
        Text("Verify your mobile to continue", color = TextSecondary)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = phone, onValueChange = { phone = it },
            label = { Text("Phone (+91...)") },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors()
        )
        if (sent) {
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = code, onValueChange = { code = it },
                label = { Text("OTP") },
                modifier = Modifier.fillMaxWidth(),
                colors = fieldColors()
            )
            hint?.let { Text("Dev OTP: $it", color = TextSecondary, fontSize = 12.sp) }
        }
        error?.let { Text(it, color = PukaarRed, modifier = Modifier.padding(top = 8.dp)) }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                scope.launch {
                    error = null
                    try {
                        if (!sent) {
                            val resp = PukaarApp.instance.repository.requestOtp(phone)
                            hint = resp.devCode
                            sent = true
                        } else {
                            PukaarApp.instance.repository.verifyOtp(phone, code)
                            onLoggedIn()
                        }
                    } catch (e: Exception) {
                        error = e.userMessage()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PukaarRed)
        ) { Text(if (sent) "Verify & Continue" else "Send OTP", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = PukaarRed,
    unfocusedBorderColor = TextSecondary,
    cursorColor = PukaarRed
)
