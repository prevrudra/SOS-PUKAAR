package com.pukaar.app.ui.screen.emergencycard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
 * How the card comes off the phone and onto paper.
 *
 * PDF is recommended in the label rather than enforced: a PDF prints at the size
 * it says it is, where a PNG is at the mercy of whatever the printer decides to
 * scale it to. Someone who wants an image to put in a chat should still get one.
 */
@Composable
fun DownloadCardScreen(
    options: PrintOptions,
    onOptionsChange: (PrintOptions) -> Unit,
    onDownload: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmergencyCardScaffold(onBack = onBack, modifier = modifier) {
        Spacer(modifier = Modifier.height(6.dp))

        CardPageTitle(
            title = stringResource(R.string.card_download_title),
            subtitle = stringResource(R.string.card_download_subtitle)
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.card_file_format),
                color = CardPalette.TextPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                PrintFormat.entries.forEach { format ->
                    FormatChoice(
                        label = stringResource(format.labelRes),
                        selected = options.format == format,
                        onClick = { onOptionsChange(options.copy(format = format)) }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.card_size),
                color = CardPalette.TextPrimary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardSize.entries.forEach { size ->
                    SizeChoice(
                        size = size,
                        selected = options.size == size,
                        onClick = { onOptionsChange(options.copy(size = size)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        CardPrimaryButton(
            text = stringResource(
                if (options.format == PrintFormat.PDF) {
                    R.string.card_download_pdf
                } else {
                    R.string.card_download_png
                }
            ),
            onClick = onDownload,
            icon = Icons.Filled.Download,
            iconLeading = true
        )

        CardNote(
            text = stringResource(R.string.card_print_note),
            icon = Icons.Filled.Print
        )

        Spacer(modifier = Modifier.height(4.dp))
    }
}

/** A radio and its label. */
@Composable
private fun FormatChoice(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .border(
                    2.dp,
                    if (selected) CardPalette.Accent else CardPalette.BorderStrong,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(CardPalette.Accent, CircleShape)
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = CardPalette.TextPrimary,
            fontSize = 12.5.sp
        )
    }
}

/**
 * One paper size: a proportional outline of it, its name and its millimetres.
 *
 * The outline is drawn to the real aspect ratio so the four sit in a row in
 * order of size — the choice is easier to make by eye than by reading 105 × 148.
 */
@Composable
private fun SizeChoice(
    size: CardSize,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier = modifier
            .background(
                if (selected) CardPalette.AccentWash else CardPalette.Surface,
                shape
            )
            .border(
                if (selected) 1.5.dp else 1.dp,
                if (selected) CardPalette.Accent else CardPalette.Border,
                shape
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val tint = if (selected) CardPalette.Accent else CardPalette.TextTertiary

        if (size.isSheet) {
            Icon(
                imageVector = Icons.Filled.GridView,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        } else {
            // Landscape for a wallet card, portrait for the paper sizes.
            val (width, height) = when (size) {
                CardSize.WALLET -> 30.dp to 20.dp
                CardSize.STANDARD -> 22.dp to 30.dp
                else -> 20.dp to 32.dp
            }
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(width)
                        .height(height)
                        .border(1.5.dp, tint, RoundedCornerShape(3.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = stringResource(size.labelRes),
            color = CardPalette.TextPrimary,
            fontSize = 10.5.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = stringResource(size.dimensionsRes),
            color = CardPalette.TextTertiary,
            fontSize = 8.5.sp,
            textAlign = TextAlign.Center,
            lineHeight = 11.sp
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun DownloadCardScreenPreview() {
    PukaarTheme {
        DownloadCardScreen(
            options = PrintOptions(),
            onOptionsChange = {},
            onDownload = {},
            onBack = {}
        )
    }
}
