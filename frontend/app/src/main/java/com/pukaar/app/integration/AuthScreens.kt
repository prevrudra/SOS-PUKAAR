package com.pukaar.app.integration

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.PukaarApp
import com.pukaar.app.ui.component.PremiumPrimaryButton
import com.pukaar.app.ui.theme.Black
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.PukaarRedBright
import com.pukaar.app.ui.theme.PukaarRedDark
import com.pukaar.app.ui.theme.SuccessGreen
import com.pukaar.app.ui.theme.SurfaceCard
import com.pukaar.app.ui.theme.TextSecondary
import com.pukaar.app.ui.theme.TextTertiary
import com.pukaar.app.util.userMessage
import kotlinx.coroutines.launch

private enum class LoginStep { Phone, Otp }

@Composable
fun OtpLoginScreen(onLoggedIn: () -> Unit) {
    var step by remember { mutableStateOf(LoginStep.Phone) }
    var phoneDigits by remember { mutableStateOf("") }
    var otpDigits by remember { mutableStateOf(List(6) { "" }) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val phoneE164 = remember(phoneDigits) { "+91$phoneDigits" }
    val otpCode = remember(otpDigits) { otpDigits.joinToString("") }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A0508), Black, Color(0xFF120608))
                )
            )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(PukaarRed.copy(alpha = 0.28f), Color.Transparent),
                        radius = 900f
                    )
                )
        )

        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            LoginBrandHeader()
            Spacer(Modifier.height(28.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut())
                },
                label = "loginStep"
            ) { current ->
                when (current) {
                    LoginStep.Phone -> PhoneStep(
                        phoneDigits = phoneDigits,
                        loading = loading,
                        error = error,
                        onPhoneChange = { value ->
                            phoneDigits = value.filter { it.isDigit() }.take(10)
                            error = null
                        },
                        onContinue = {
                            scope.launch {
                                error = null
                                loading = true
                                try {
                                    PukaarApp.instance.repository.requestOtp(phoneE164)
                                    otpDigits = List(6) { "" }
                                    step = LoginStep.Otp
                                } catch (e: Exception) {
                                    error = e.userMessage()
                                } finally {
                                    loading = false
                                }
                            }
                        }
                    )

                    LoginStep.Otp -> OtpStep(
                        phoneDisplay = formatIndianPhone(phoneDigits),
                        otpDigits = otpDigits,
                        loading = loading,
                        error = error,
                        onOtpChange = { index, value ->
                            val digit = value.filter { it.isDigit() }.takeLast(1)
                            otpDigits = otpDigits.toMutableList().also { it[index] = digit }
                            error = null
                            if (digit.isNotEmpty() && index == 5 && otpDigits.all { it.isNotEmpty() }) {
                                scope.launch {
                                    error = null
                                    loading = true
                                    try {
                                        PukaarApp.instance.repository.verifyOtp(phoneE164, otpDigits.joinToString(""))
                                        onLoggedIn()
                                    } catch (e: Exception) {
                                        error = e.userMessage()
                                    } finally {
                                        loading = false
                                    }
                                }
                            }
                        },
                        onBack = {
                            step = LoginStep.Phone
                            otpDigits = List(6) { "" }
                            error = null
                        },
                        onVerify = {
                            scope.launch {
                                error = null
                                loading = true
                                try {
                                    PukaarApp.instance.repository.verifyOtp(phoneE164, otpCode)
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
    }
}

@Composable
private fun LoginBrandHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(listOf(PukaarRedBright, PukaarRedDark))
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("PUKAAR", color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Black, letterSpacing = 4.sp)
        Text(
            "Your safety, one tap away",
            color = TextSecondary,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun PhoneStep(
    phoneDigits: String,
    loading: Boolean,
    error: String?,
    onPhoneChange: (String) -> Unit,
    onContinue: () -> Unit
) {
    LoginCard(title = "Sign in", subtitle = "Enter your mobile number to receive a secure OTP") {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceCard)
                .border(1.dp, PukaarRed.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("+91", color = PukaarRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.width(12.dp))
            Box(Modifier.width(1.dp).height(28.dp).background(TextTertiary.copy(alpha = 0.5f)))
            Spacer(Modifier.width(12.dp))
            OutlinedTextField(
                value = phoneDigits,
                onValueChange = onPhoneChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("10-digit mobile", color = TextTertiary) },
                singleLine = true,
                enabled = !loading,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (phoneDigits.length == 10) onContinue() }),
                colors = loginFieldColors(),
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp, letterSpacing = 1.sp)
            )
        }
        error?.let { Text(it, color = PukaarRedBright, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)) }
        Spacer(Modifier.height(20.dp))
        PremiumPrimaryButton(
            text = if (loading) "Sending OTP…" else "Get OTP",
            loading = loading,
            enabled = phoneDigits.length == 10,
            onClick = onContinue
        )
        Text(
            "OTP is sent via SMS. Standard rates may apply.",
            color = TextTertiary,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun OtpStep(
    phoneDisplay: String,
    otpDigits: List<String>,
    loading: Boolean,
    error: String?,
    onOtpChange: (Int, String) -> Unit,
    onBack: () -> Unit,
    onVerify: () -> Unit
) {
    val focusRequesters = remember { List(6) { FocusRequester() } }
    LaunchedEffect(Unit) { focusRequesters[0].requestFocus() }

    LoginCard(title = "Verify OTP", subtitle = "Enter the 6-digit code sent to +91 $phoneDisplay") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            otpDigits.forEachIndexed { index, digit ->
                OtpBox(
                    value = digit,
                    focused = otpDigits.take(index).all { it.isNotEmpty() } && digit.isEmpty(),
                    onValueChange = { onOtpChange(index, it) },
                    focusRequester = focusRequesters[index],
                    enabled = !loading,
                    onFilled = {
                        if (index < 5) focusRequesters[index + 1].requestFocus()
                    }
                )
            }
        }
        error?.let { Text(it, color = PukaarRedBright, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)) }
        Spacer(Modifier.height(20.dp))
        PremiumPrimaryButton(
            text = if (loading) "Verifying…" else "Verify & continue",
            loading = loading,
            enabled = otpDigits.all { it.isNotEmpty() },
            onClick = onVerify
        )
        TextButton(onClick = onBack, enabled = !loading, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Change number", color = SuccessGreen, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LoginCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceCard.copy(alpha = 0.94f))
            .border(1.dp, PukaarRed.copy(alpha = 0.2f), RoundedCornerShape(22.dp))
            .padding(20.dp)
    ) {
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(subtitle, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
        content()
    }
}

@Composable
private fun OtpBox(
    value: String,
    focused: Boolean,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
    enabled: Boolean,
    onFilled: () -> Unit
) {
    val borderColor = when {
        value.isNotEmpty() -> PukaarRed
        focused -> PukaarRedBright
        else -> TextTertiary.copy(alpha = 0.5f)
    }
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (value.isNotEmpty()) PukaarRed.copy(alpha = 0.12f) else SurfaceCard)
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        BasicTextField(
            value = value,
            onValueChange = { raw ->
                val digit = raw.filter { it.isDigit() }.takeLast(1)
                onValueChange(digit)
                if (digit.isNotEmpty()) onFilled()
            },
            enabled = enabled,
            singleLine = true,
            modifier = Modifier
                .focusRequester(focusRequester)
                .width(28.dp),
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            cursorBrush = SolidColor(PukaarRed)
        )
    }
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedBorderColor = Color.Transparent,
    unfocusedBorderColor = Color.Transparent,
    cursorColor = PukaarRed,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent
)

private fun formatIndianPhone(digits: String): String = when {
    digits.length <= 5 -> digits
    digits.length <= 10 -> "${digits.take(5)} ${digits.drop(5)}"
    else -> digits
}
