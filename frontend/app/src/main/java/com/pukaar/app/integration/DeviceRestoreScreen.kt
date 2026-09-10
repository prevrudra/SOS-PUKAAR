package com.pukaar.app.integration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.ui.component.PremiumPrimaryButton
import com.pukaar.app.ui.theme.Black
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.SuccessGreen
import com.pukaar.app.ui.theme.TextSecondary

@Composable
fun DeviceRestoreScreen(onActivated: () -> Unit) {
    var activated by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1A0508), Black)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "New phone detected",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (activated) {
                "Your previous phone has been deactivated. This device is now your active PUKAAR safety phone."
            } else {
                "Your PUKAAR safety profile is ready to restore from your account."
            },
            color = TextSecondary,
            fontSize = 15.sp,
            lineHeight = 22.sp
        )
        Spacer(Modifier.height(28.dp))
        RestoreCheck("SOS contacts")
        RestoreCheck("Inactivity protection")
        RestoreCheck("Emergency Card")
        RestoreCheck("Help numbers")
        RestoreCheck("Subscription validity")
        Spacer(Modifier.height(32.dp))
        if (!activated) {
            PremiumPrimaryButton(
                text = "ACTIVATE THIS PHONE",
                onClick = { activated = true }
            )
        } else {
            PremiumPrimaryButton(
                text = "Continue",
                onClick = onActivated
            )
            Text(
                "If your Emergency Card QR is device-specific, regenerate it from Settings.",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}

@Composable
private fun RestoreCheck(label: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(PukaarRed.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen)
        Spacer(Modifier.width(12.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
}
