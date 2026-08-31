package com.pukaar.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.theme.Black
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.SuccessGreen
import com.pukaar.app.ui.theme.TextPrimary

/**
 * The confirmation state shared by Add Contact, SOS Settings, Mock Drill and the
 * rest: a coloured badge, a short message, and one way out.
 */
@Composable
fun SuccessScreen(
    message: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Check,
    badgeColor: Color = SuccessGreen
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(64.dp)
                .background(badgeColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = message,
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp
        )

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(text = stringResource(R.string.action_ok), onClick = onDismiss)
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun SuccessScreenPreview() {
    PukaarTheme {
        SuccessScreen(message = "Contact Added\nSuccessfully", onDismiss = {})
    }
}
