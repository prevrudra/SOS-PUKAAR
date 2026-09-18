package com.pukaar.app.ui.screen.emergencycard

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.theme.PukaarTheme

/**
 * The card at print size, with nothing else on the page.
 *
 * What comes out of the printer, shown before it does — the same
 * [EmergencyCardFace] the rest of the flow draws, in the [style] left showing on
 * the finished card and at the proportions of the size that was picked, so the
 * preview cannot promise a layout the print does not give.
 */
@Composable
fun PrintableCardScreen(
    draft: EmergencyCardDraft,
    size: CardSize,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    style: CardQrStyle = CardQrStyle.WITH_DETAILS
) {
    val relationLabel = rememberRelationLabel()
    val payload = remember(draft) { emergencyCardPayload(draft, relationLabel) }

    EmergencyCardScaffold(onBack = onBack, modifier = modifier) {
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.card_printable_title, stringResource(size.labelRes)),
            color = CardPalette.TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        EmergencyCardFace(
            draft = draft,
            payload = payload,
            relationLabel = { contact -> relationLabel(contact) },
            style = style,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Text(
            text = stringResource(size.dimensionsRes),
            color = CardPalette.TextTertiary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Preview(showBackground = true, heightDp = 800)
@Composable
private fun PrintableCardScreenPreview() {
    PukaarTheme {
        PrintableCardScreen(
            draft = SampleCardDraft,
            size = CardSize.STANDARD,
            onBack = {}
        )
    }
}
