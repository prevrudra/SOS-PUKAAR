package com.pukaar.app.integration

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.PukaarApp
import com.pukaar.app.R
import com.pukaar.app.util.userMessage
import kotlinx.coroutines.launch

private object LoginColors {
    val greenPrimary = Color(0xFF608D3D)
    val greenBorder = Color(0xFF315D1C)
    val greenDisabled = Color(0xFFA8B998)
    val panelBackground = Color(0xFFF2F4F6)
    val textPrimary = Color(0xFF3E2E2B)
    val textMuted = Color(0xFF818185)
    val fieldBackground = Color(0xFFF1F3F7)
    val fieldBorder = Color(0xFFD8DBE2)
    val divider = Color(0xFFD1D5DD)
    val keyBackground = Color(0xFFE6E8EE)
    val keyText = Color(0xFF34343A)
    val creamBackground = Color(0xFFF0E3D6)
}

@Composable
fun OtpLoginScreen(onLoggedIn: () -> Unit) {
    var step by remember { mutableStateOf(LoginStep.Phone) }
    var phoneDigits by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val phoneE164 = remember(phoneDigits) { "+91$phoneDigits" }

    LoginShell {
        when (step) {
            LoginStep.Phone -> PhoneLoginContent(
                phoneDigits = phoneDigits,
                loading = loading,
                error = error,
                onDigit = { digit ->
                    if (phoneDigits.length < 10) phoneDigits += digit
                    error = null
                },
                onBackspace = {
                    if (phoneDigits.isNotEmpty()) phoneDigits = phoneDigits.dropLast(1)
                    error = null
                },
                onContinue = {
                    scope.launch {
                        error = null
                        loading = true
                        try {
                            PukaarApp.instance.repository.requestOtp(phoneE164)
                            step = LoginStep.Otp
                            otp = ""
                        } catch (e: Exception) {
                            error = e.userMessage()
                        } finally {
                            loading = false
                        }
                    }
                }
            )

            LoginStep.Otp -> OtpVerificationContent(
                phoneDisplay = formatPhoneDigits(phoneDigits),
                otp = otp,
                loading = loading,
                error = error,
                onOtpChange = {
                    otp = it.filter { c -> c.isDigit() }.take(6)
                    error = null
                },
                onBack = {
                    step = LoginStep.Phone
                    otp = ""
                    error = null
                },
                onVerify = {
                    scope.launch {
                        error = null
                        loading = true
                        try {
                            PukaarApp.instance.repository.verifyOtp(phoneE164, otp)
                            onLoggedIn()
                        } catch (e: Exception) {
                            error = e.userMessage()
                        } finally {
                            loading = false
                        }
                    }
                }
            )
        }
    }
}

private enum class LoginStep { Phone, Otp }

@Composable
private fun LoginShell(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val panelTop = maxHeight * 0.41f
        Image(
            painter = painterResource(R.drawable.loginpageimage),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.06f))
        )
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = panelTop)
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp))
                .background(LoginColors.panelBackground)
                .padding(horizontal = 16.dp, vertical = 18.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun PhoneLoginContent(
    phoneDigits: String,
    loading: Boolean,
    error: String?,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onContinue: () -> Unit
) {
    val canContinue = phoneDigits.length >= 10

    Column(Modifier.fillMaxSize()) {
        Text(
            "Mobile Number",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = LoginColors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        PhoneField(formatted = formatPhoneDigits(phoneDigits))
        error?.let {
            Text(it, color = Color(0xFFDA4D20), fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
        }
        Spacer(Modifier.height(22.dp))
        LoginPrimaryButton(
            text = "Abhi Shuru karo  ->",
            enabled = canContinue,
            loading = loading,
            onClick = onContinue
        )
        Spacer(Modifier.weight(1f))
        NumberPad(onDigit = onDigit, onBackspace = onBackspace)
    }
}

@Composable
private fun OtpVerificationContent(
    phoneDisplay: String,
    otp: String,
    loading: Boolean,
    error: String?,
    onOtpChange: (String) -> Unit,
    onBack: () -> Unit,
    onVerify: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(otp) {
        if (otp.length == 6 && !loading) onVerify()
    }

    Column(Modifier.fillMaxSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, enabled = !loading) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = LoginColors.textPrimary)
            }
            Text(
                "OTP Verification",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = LoginColors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(48.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "We've sent a 6-digit OTP to\n+91 $phoneDisplay",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = LoginColors.textMuted,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(24.dp))
        OtpPinRow(otp = otp, onOtpChange = onOtpChange, focusRequester = focusRequester, enabled = !loading)
        error?.let {
            Text(it, color = Color(0xFFDA4D20), fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
        }
        Spacer(Modifier.height(28.dp))
        LoginPrimaryButton(
            text = "Verify OTP",
            enabled = otp.length == 6,
            loading = loading,
            onClick = onVerify
        )
        Spacer(Modifier.height(16.dp))
        TextButton(
            onClick = onBack,
            enabled = !loading,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Change Phone Number", color = LoginColors.greenPrimary, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun PhoneField(formatted: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LoginColors.fieldBackground)
            .border(1.dp, LoginColors.fieldBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("IN", color = LoginColors.textMuted, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.width(8.dp))
        Text("+91", color = LoginColors.textMuted, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = LoginColors.textMuted)
        Spacer(Modifier.width(12.dp))
        Box(Modifier.width(1.dp).height(30.dp).background(LoginColors.divider))
        Spacer(Modifier.width(12.dp))
        Text(
            formatted.ifBlank { " " },
            color = LoginColors.textPrimary,
            fontSize = 20.sp,
            letterSpacing = 0.4.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OtpPinRow(
    otp: String,
    onOtpChange: (String) -> Unit,
    focusRequester: FocusRequester,
    enabled: Boolean
) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        BasicTextField(
            value = otp,
            onValueChange = onOtpChange,
            enabled = enabled,
            modifier = Modifier
                .size(1.dp)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            cursorBrush = SolidColor(Color.Transparent)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(6) { index ->
                val char = otp.getOrNull(index)?.toString() ?: ""
                val focused = otp.length == index
                Box(
                    modifier = Modifier
                        .size(width = 50.dp, height = 56.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (focused) LoginColors.creamBackground else LoginColors.fieldBackground)
                        .border(
                            width = if (focused || char.isNotEmpty()) 2.dp else 1.5.dp,
                            color = if (focused || char.isNotEmpty()) LoginColors.greenPrimary else LoginColors.fieldBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable(enabled = enabled) { focusRequester.requestFocus() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(char, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = LoginColors.textPrimary)
                }
            }
        }
    }
}

@Composable
private fun LoginPrimaryButton(
    text: String,
    enabled: Boolean,
    loading: Boolean,
    onClick: () -> Unit
) {
    val active = enabled && !loading
  Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (active) LoginColors.greenPrimary else LoginColors.greenDisabled)
            .border(2.dp, LoginColors.greenBorder, RoundedCornerShape(14.dp))
            .clickable(enabled = active) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun NumberPad(onDigit: (String) -> Unit, onBackspace: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        NumberPadRow(listOf("1", "2", "3"), onDigit)
        NumberPadRow(listOf("4", "5", "6"), onDigit)
        NumberPadRow(listOf("7", "8", "9"), onDigit)
        Row {
            Spacer(Modifier.weight(1f))
            NumberPadKey("0", onClick = { onDigit("0") }, modifier = Modifier.weight(1f))
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                IconButton(onClick = onBackspace) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Backspace,
                        contentDescription = "Backspace",
                        tint = LoginColors.textPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberPadRow(labels: List<String>, onDigit: (String) -> Unit) {
    Row {
        labels.forEach { label ->
            NumberPadKey(label, onClick = { onDigit(label) }, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun NumberPadKey(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .height(54.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(LoginColors.keyBackground)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = LoginColors.keyText)
    }
}

private fun formatPhoneDigits(digits: String): String = when {
    digits.isEmpty() -> ""
    digits.length <= 4 -> digits
    digits.length <= 7 -> "${digits.substring(0, 4)}-${digits.substring(4)}"
    else -> "${digits.substring(0, 4)}-${digits.substring(4, 7)}-${digits.substring(7)}"
}
