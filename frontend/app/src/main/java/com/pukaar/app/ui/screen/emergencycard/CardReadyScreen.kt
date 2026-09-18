package com.pukaar.app.ui.screen.emergencycard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.theme.PukaarTheme

/**
 * The card, made — in both the styles it was made in.
 *
 * One card, two faces: the detailed one, which identifies the owner at a glance,
 * and the plain one, which is only a code. Both are generated, because the choice
 * is not made once — the detailed face belongs in a wallet and the plain one on
 * the back of a laptop, and most people want both.
 *
 * The choice is offered twice over, as a toggle and as a swipe, because the two
 * answer different questions: the toggle says a choice exists, the swipe lets it
 * be compared. Whichever is settled on is what the actions below print, share and
 * set as a wallpaper, so there is never a preview of one card and a file of the
 * other.
 *
 * Everything below the card is something to do with it, in the order people want
 * them: keep the code to hand, pass it to someone, print it, put it where a
 * stranger will look first. Editing is last, because by this point the common
 * case is being finished.
 */
@Composable
fun CardReadyScreen(
    draft: EmergencyCardDraft,
    style: CardQrStyle,
    onStyleChange: (CardQrStyle) -> Unit,
    onSaveQr: () -> Unit,
    onShareQr: () -> Unit,
    onDownload: () -> Unit,
    onSetLockScreen: () -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val relationLabel = rememberRelationLabel()
    val payload = remember(draft) { emergencyCardPayload(draft, relationLabel) }

    EmergencyCardScaffold(onBack = onBack, modifier = modifier) {
        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.card_ready_title),
                color = CardPalette.TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = stringResource(R.string.card_ready_subtitle),
                color = CardPalette.TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center
            )
        }

        CardStylePicker(
            selected = style,
            onSelect = onStyleChange,
            draft = draft,
            payload = payload,
            relationLabel = relationLabel
        )

        CardNote(
            text = stringResource(R.string.card_keep_handy),
            icon = Icons.Filled.Lightbulb,
            tint = CardPalette.TextPrimary,
            background = CardPalette.AccentWash
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CardSecondaryButton(
                text = stringResource(R.string.card_save_qr),
                onClick = onSaveQr,
                icon = Icons.Filled.QrCode2,
                modifier = Modifier.weight(1f)
            )
            CardSecondaryButton(
                text = stringResource(R.string.card_share_qr),
                onClick = onShareQr,
                icon = Icons.Filled.Share,
                modifier = Modifier.weight(1f)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CardSecondaryButton(
                text = stringResource(R.string.card_download_pdf),
                onClick = onDownload,
                icon = Icons.Filled.Download,
                modifier = Modifier.weight(1f)
            )
            CardSecondaryButton(
                text = stringResource(R.string.card_set_lock_screen),
                onClick = onSetLockScreen,
                icon = Icons.Filled.PhoneAndroid,
                modifier = Modifier.weight(1f)
            )
        }

        CardSecondaryButton(
            text = stringResource(R.string.card_edit),
            onClick = onEdit,
            icon = Icons.Filled.Edit
        )

        Spacer(modifier = Modifier.height(4.dp))
    }
}

/**
 * The tabs, the swipeable cards, the dots and the hint — one control over one
 * choice, kept together so they cannot fall out of step.
 */
@Composable
private fun CardStylePicker(
    selected: CardQrStyle,
    onSelect: (CardQrStyle) -> Unit,
    draft: EmergencyCardDraft,
    payload: String,
    relationLabel: (CardContact) -> String?,
    modifier: Modifier = Modifier
) {
    val styles = CardQrStyle.entries
    val pagerState = rememberPagerState(initialPage = selected.ordinal) { styles.size }

    // Tapping a tab scrolls the pager; swiping the pager selects a tab. Both
    // settle on the same value, so neither drives the other in a loop.
    LaunchedEffect(selected) {
        if (pagerState.currentPage != selected.ordinal) {
            pagerState.animateScrollToPage(selected.ordinal)
        }
    }
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page -> onSelect(styles[page]) }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            styles.forEach { style ->
                CardStyleTab(
                    style = style,
                    selected = style == selected,
                    onClick = { onSelect(style) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // A pager is a lazy layout and cannot size itself to the page it is
        // showing, so an invisible stack of both faces behind it sets the height
        // to whichever is taller. Fixed dp would clip at large font scales.
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .alpha(0f)
                    .clearAndSetSemantics { }
            ) {
                styles.forEach { style ->
                    EmergencyCardFace(
                        draft = draft,
                        payload = payload,
                        relationLabel = { contact -> relationLabel(contact) },
                        style = style
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                pageSpacing = 12.dp,
                modifier = Modifier.matchParentSize()
            ) { page ->
                EmergencyCardFace(
                    draft = draft,
                    payload = payload,
                    relationLabel = { contact -> relationLabel(contact) },
                    style = styles[page]
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            styles.forEachIndexed { index, _ ->
                val active = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (active) 9.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (active) CardPalette.Accent else CardPalette.BorderStrong)
                )
            }
        }

        SwipeHint(text = stringResource(selected.swipeToOtherRes))
    }
}

/** One tab: the style's name, what it is for, and whether it is the one showing. */
@Composable
private fun CardStyleTab(
    style: CardQrStyle,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    val icon = when (style) {
        CardQrStyle.WITH_DETAILS -> Icons.Filled.Person
        CardQrStyle.PLAIN -> Icons.Filled.QrCode2
    }
    val content = if (selected) Color.White else CardPalette.TextPrimary

    Row(
        modifier = modifier
            .background(if (selected) CardPalette.Accent else CardPalette.Note, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = stringResource(style.labelRes),
                color = content,
                fontSize = 11.5.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(style.captionRes),
                color = if (selected) Color.White.copy(alpha = 0.85f) else CardPalette.TextSecondary,
                fontSize = 10.sp,
                lineHeight = 13.sp
            )
        }
    }
}

/** "Swipe left to…", with an arrow either side of it. */
@Composable
private fun SwipeHint(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HintArrow(Icons.AutoMirrored.Filled.ArrowBack)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = CardPalette.TextSecondary,
            fontSize = 11.5.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.width(8.dp))
        HintArrow(Icons.AutoMirrored.Filled.ArrowForward)
    }
}

@Composable
private fun HintArrow(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = CardPalette.TextTertiary,
        modifier = Modifier.size(15.dp)
    )
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun CardReadyScreenPreview() {
    PukaarTheme {
        CardReadyScreen(
            draft = SampleCardDraft,
            style = CardQrStyle.WITH_DETAILS,
            onStyleChange = {},
            onSaveQr = {},
            onShareQr = {},
            onDownload = {},
            onSetLockScreen = {},
            onEdit = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun CardReadyScreenPlainPreview() {
    PukaarTheme {
        CardReadyScreen(
            draft = SampleCardDraft,
            style = CardQrStyle.PLAIN,
            onStyleChange = {},
            onSaveQr = {},
            onShareQr = {},
            onDownload = {},
            onSetLockScreen = {},
            onEdit = {},
            onBack = {}
        )
    }
}
