package com.pukaar.app.ui.screen.homemodeguide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.PukaarScreen
import com.pukaar.app.ui.component.SectionCard
import com.pukaar.app.ui.screen.home.HomeMode
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary

/**
 * Menu tile: what the two home modes are for.
 *
 * The colours and button labels are read straight off [HomeMode], so this screen
 * cannot drift out of step with the home screen it describes.
 */
@Composable
fun HomeModeGuideScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PukaarScreen(
        title = stringResource(R.string.home_mode_guide_title),
        onBack = onBack,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.home_mode_guide_intro),
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ModeCard(
            mode = HomeMode.SOS,
            body = stringResource(R.string.home_mode_guide_sos_body)
        )
        ModeCard(
            mode = HomeMode.HELP,
            body = stringResource(R.string.home_mode_guide_help_body)
        )
    }
}

@Composable
private fun ModeCard(
    mode: HomeMode,
    body: String,
    modifier: Modifier = Modifier
) {
    SectionCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(mode.accent, CircleShape)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(mode.buttonLabelRes).uppercase(),
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = body,
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 19.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun HomeModeGuideScreenPreview() {
    PukaarTheme {
        HomeModeGuideScreen(onBack = {})
    }
}
