package com.pukaar.app.ui.screen.emergencycard

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R

/**
 * Decodes a picked photo, once per uri.
 *
 * No image library in the project, and one is not worth adding for a single
 * portrait: the picker hands back a content uri, this opens it and decodes it.
 * Returns null on anything that is not readable — a revoked permission after a
 * restart, or a uri that no longer resolves — and the card falls back to a
 * placeholder rather than failing to draw.
 */
@Composable
fun rememberPhoto(uri: String?): ImageBitmap? {
    val context = LocalContext.current

    return remember(uri) {
        if (uri == null) {
            null
        } else {
            try {
                context.contentResolver.openInputStream(Uri.parse(uri)).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            } catch (error: Exception) {
                null
            }
        }
    }
}

/** The portrait, or a grey silhouette where there is none yet. */
@Composable
fun CardPhoto(
    photoUri: String?,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(10.dp)
) {
    val photo = rememberPhoto(photoUri)

    Box(
        modifier = modifier
            .clip(shape)
            .background(CardPalette.Note),
        contentAlignment = Alignment.Center
    ) {
        if (photo != null) {
            Image(
                bitmap = photo,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = CardPalette.TextTertiary,
                modifier = Modifier.fillMaxSize(0.6f)
            )
        }
    }
}

/**
 * The card itself: the thing that gets printed, shared and put on a lock screen.
 *
 * One composable for every place it appears, varied by [compact] and [style]
 * rather than rewritten — a card that looked different on the preview from the
 * printout would defeat the point of the preview.
 *
 * Both styles carry the same QR code and therefore the same information. The
 * difference is only what a passer-by can read without scanning, which is a
 * privacy decision the user makes, not a different card.
 *
 * Colours are fixed rather than themed. This is a piece of paper, and it stays
 * white whatever the phone is set to.
 */
@Composable
fun EmergencyCardFace(
    draft: EmergencyCardDraft,
    payload: String,
    relationLabel: @Composable (CardContact) -> String?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    style: CardQrStyle = CardQrStyle.WITH_DETAILS
) {
    CardFaceShell(modifier = modifier, compact = compact) {
        when (style) {
            CardQrStyle.WITH_DETAILS -> DetailedFaceBody(
                draft = draft,
                payload = payload,
                relationLabel = relationLabel,
                compact = compact
            )

            CardQrStyle.PLAIN -> PlainFaceBody(
                payload = payload,
                compact = compact
            )
        }
    }
}

/**
 * What both faces share: the masthead, the red keyline and the closing sentence.
 *
 * Held in one place so the two styles can never drift into looking like cards
 * from two different apps — which, printed side by side on a fridge and in a
 * wallet, is exactly what they will be seen as.
 */
@Composable
private fun CardFaceShell(
    modifier: Modifier,
    compact: Boolean,
    body: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(if (compact) 10.dp else 14.dp)
    val pad = if (compact) 10.dp else 14.dp

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardPalette.Surface, shape)
            .border(1.dp, CardPalette.Accent, shape)
            .padding(pad),
        verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp)
    ) {
        // Masthead — the brand on the left, what the card is on the right.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardShieldMark(size = if (compact) 16 else 20)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.app_name).uppercase(),
                color = CardPalette.TextPrimary,
                fontSize = if (compact) 12.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.menu_emergency_card),
                color = CardPalette.TextSecondary,
                fontSize = if (compact) 9.sp else 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        CardFaceRule()
        body()
        CardFaceRule()

        Text(
            text = stringResource(R.string.card_face_every_second),
            color = CardPalette.TextSecondary,
            fontSize = if (compact) 8.5.sp else 10.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * The face for someone who wants to be identified.
 *
 * Read top to bottom it answers the questions a responder asks in order: who is
 * this, what do I need to know about them medically, and who do I ring. The code
 * sits beside the photo because everything under it is a summary of what the code
 * carries in full.
 */
@Composable
private fun ColumnScope.DetailedFaceBody(
    draft: EmergencyCardDraft,
    payload: String,
    relationLabel: @Composable (CardContact) -> String?,
    compact: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        CardPhoto(
            photoUri = draft.personal.photoUri,
            modifier = Modifier.size(if (compact) 42.dp else 60.dp)
        )
        Spacer(modifier = Modifier.width(if (compact) 8.dp else 10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = draft.personal.fullName,
                color = CardPalette.TextPrimary,
                fontSize = if (compact) 12.sp else 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (draft.personal.nationality.isNotBlank()) {
                Text(
                    text = draft.personal.nationality,
                    color = CardPalette.TextSecondary,
                    fontSize = if (compact) 10.sp else 12.sp,
                    maxLines = 1
                )
            }
            draft.bloodGroup?.let { group ->
                Text(
                    text = stringResource(R.string.card_face_blood_group, group.label),
                    color = CardPalette.TextPrimary,
                    fontSize = if (compact) 10.sp else 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            QrCode(
                payload = payload,
                logo = true,
                modifier = Modifier
                    .size(if (compact) 52.dp else 76.dp)
                    .aspectRatio(1f)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = stringResource(R.string.card_face_scan_to_help),
                color = CardPalette.Accent,
                fontSize = if (compact) 8.5.sp else 10.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.card_face_scan_details),
                color = CardPalette.TextSecondary,
                fontSize = if (compact) 7.5.sp else 9.sp
            )
        }
    }

    // The line a responder actually acts on.
    draft.personal.primaryContact.takeIf { it.isComplete }?.let { contact ->
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Filled.Phone,
                contentDescription = null,
                tint = CardPalette.Accent,
                modifier = Modifier.size(if (compact) 13.dp else 16.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            Column {
                Text(
                    text = stringResource(R.string.card_face_emergency_contact),
                    color = CardPalette.Accent,
                    fontSize = if (compact) 9.5.sp else 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = relationLabel(contact)
                        ?.let { "${contact.name} ($it)" }
                        ?: contact.name,
                    color = CardPalette.TextPrimary,
                    fontSize = if (compact) 10.sp else 12.5.sp
                )
                Text(
                    text = contact.phone,
                    color = CardPalette.TextPrimary,
                    fontSize = if (compact) 10.sp else 12.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }

    // What else is behind the code. Dropped on the compact card: a lock screen is
    // read at arm's length and this is the first thing that stops being legible.
    if (!compact) {
        CardFaceRule()
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            FaceFeature(
                icon = Icons.Filled.MonitorHeart,
                title = stringResource(R.string.card_feature_medical),
                subtitle = stringResource(R.string.card_feature_medical_sub),
                modifier = Modifier.weight(1f)
            )
            FaceFeatureDivider()
            FaceFeature(
                icon = Icons.Filled.Person,
                title = stringResource(R.string.card_feature_contacts),
                subtitle = stringResource(R.string.card_feature_contacts_sub),
                modifier = Modifier.weight(1f)
            )
            FaceFeatureDivider()
            FaceFeature(
                icon = Icons.Filled.VerifiedUser,
                title = stringResource(R.string.card_assure_help),
                subtitle = null,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * The face that gives nothing away.
 *
 * Nothing on it identifies the owner, so it can go on the back of a phone, a
 * laptop lid or a bag tag where it is seen daily by people with no business
 * knowing a blood group. The three lines beside the code exist to tell whoever
 * finds it that scanning is worth their trouble, since the code itself makes no
 * promise about what is behind it.
 */
@Composable
private fun ColumnScope.PlainFaceBody(
    payload: String,
    compact: Boolean
) {
    // Intrinsic height so the rule between the two halves runs the full depth of
    // whichever half is taller, rather than being guessed at in dp.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            QrCode(
                payload = payload,
                logo = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            Spacer(modifier = Modifier.height(if (compact) 4.dp else 6.dp))
            Text(
                text = stringResource(R.string.card_face_scan_to_help),
                color = CardPalette.Accent,
                fontSize = if (compact) 12.sp else 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.card_plain_scan_caption),
                color = CardPalette.TextSecondary,
                fontSize = if (compact) 8.sp else 10.sp,
                lineHeight = if (compact) 11.sp else 14.sp,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.width(if (compact) 8.dp else 12.dp))
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(CardPalette.Border)
        )
        Spacer(modifier = Modifier.width(if (compact) 8.dp else 12.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardPalette.AccentWash, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.CropFree,
                    contentDescription = null,
                    tint = CardPalette.TextPrimary,
                    modifier = Modifier.size(if (compact) 14.dp else 20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.card_plain_intro),
                    color = CardPalette.TextPrimary,
                    fontSize = if (compact) 8.5.sp else 11.sp,
                    lineHeight = if (compact) 12.sp else 15.sp
                )
            }

            AssuranceRow(
                icon = Icons.Filled.VerifiedUser,
                text = stringResource(R.string.card_assure_help),
                compact = compact
            )
            AssuranceRow(
                icon = Icons.Filled.Lock,
                text = stringResource(R.string.card_assure_private),
                compact = compact
            )
            AssuranceRow(
                icon = Icons.Filled.VisibilityOff,
                text = stringResource(R.string.card_assure_scan),
                compact = compact
            )
        }
    }
}

/** One "here is what this code is for" line on the plain face. */
@Composable
private fun AssuranceRow(
    icon: ImageVector,
    text: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CardPalette.TextPrimary,
            modifier = Modifier.size(if (compact) 13.dp else 18.dp)
        )
        Spacer(modifier = Modifier.width(if (compact) 6.dp else 9.dp))
        Text(
            text = text,
            color = CardPalette.TextPrimary,
            fontSize = if (compact) 8.5.sp else 11.sp,
            lineHeight = if (compact) 12.sp else 15.sp
        )
    }
}

/** One cell of the strip along the bottom of the detailed face. */
@Composable
private fun FaceFeature(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = CardPalette.TextPrimary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Column {
            Text(
                text = title,
                color = CardPalette.TextPrimary,
                fontSize = 8.5.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = CardPalette.TextSecondary,
                    fontSize = 8.sp,
                    lineHeight = 10.sp
                )
            }
        }
    }
}

@Composable
private fun FaceFeatureDivider() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(22.dp)
            .background(CardPalette.Border)
    )
}

/** The hairline that separates one band of the card from the next. */
@Composable
private fun CardFaceRule() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(CardPalette.Border)
    )
}

/** The confetti-and-tick banner over the finished card. */
@Composable
fun CardReadyMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(52.dp)
            .background(CardPalette.Success, RoundedCornerShape(percent = 50)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(30.dp)
        )
    }
}
