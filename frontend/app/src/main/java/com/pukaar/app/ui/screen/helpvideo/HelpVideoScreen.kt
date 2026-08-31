package com.pukaar.app.ui.screen.helpvideo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.NavigationRow
import com.pukaar.app.ui.component.PukaarScreen
import com.pukaar.app.ui.component.RowDivider
import com.pukaar.app.ui.component.SectionCard
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.SurfaceElevated
import com.pukaar.app.ui.theme.TextPrimary

/** Menu item 8. A short walkthrough, plus the per-topic clips beneath it. */
@Composable
fun HelpVideoScreen(
    onBack: () -> Unit,
    onPlayMainVideo: () -> Unit,
    onTopicClick: (HelpTopic) -> Unit,
    modifier: Modifier = Modifier
) {
    PukaarScreen(
        title = null,
        onBack = onBack,
        modifier = modifier
    ) {
        VideoPoster(onClick = onPlayMainVideo)

        Text(
            text = stringResource(R.string.help_video_title),
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        SectionCard(contentPadding = PaddingValues(horizontal = 16.dp)) {
            HelpTopic.entries.forEachIndexed { index, topic ->
                NavigationRow(
                    title = stringResource(topic.labelRes),
                    onClick = { onTopicClick(topic) }
                )
                if (index != HelpTopic.entries.lastIndex) {
                    RowDivider()
                }
            }
        }
    }
}

/** Placeholder poster frame. Swap the gradient for the real thumbnail later. */
@Composable
private fun VideoPoster(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 10f)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(listOf(SurfaceElevated, Color(0xFF0A0A0A)))
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .background(Color.White.copy(alpha = 0.92f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.help_video_play),
                tint = Color.Black,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

/** The clips listed under the main video. */
enum class HelpTopic(val labelRes: Int) {
    TRIGGER_SOS(R.string.help_video_trigger),
    FOR_FAMILY(R.string.help_video_family),
    ELDERLY_GUIDE(R.string.help_video_elderly)
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun HelpVideoScreenPreview() {
    PukaarTheme {
        HelpVideoScreen(onBack = {}, onPlayMainVideo = {}, onTopicClick = {})
    }
}
