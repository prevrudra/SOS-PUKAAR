package com.pukaar.app.ui.screen.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.screen.contacts.ContactRelation
import com.pukaar.app.ui.screen.protection.ProtectionType
import com.pukaar.app.ui.component.CircularBackButton
import com.pukaar.app.ui.theme.AccentBlue
import com.pukaar.app.ui.theme.AccentPurple
import com.pukaar.app.ui.theme.Black
import com.pukaar.app.ui.theme.Outline
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.SuccessGreen
import com.pukaar.app.ui.theme.SurfaceCard
import com.pukaar.app.ui.theme.SurfaceElevated
import com.pukaar.app.ui.theme.SurfaceInput
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary
import com.pukaar.app.ui.theme.TextTertiary

private val CardShape = RoundedCornerShape(12.dp)
private val FieldShape = RoundedCornerShape(8.dp)

/** Amber used only for the "verification pending" state. */
internal val PendingAmber = Color(0xFFE0A21E)

/**
 * The frame every onboarding page sits in.
 *
 * One page is one call: a back control, an optional progress bar across the top,
 * a scrolling body and a pinned footer. Keeping the chrome here means each step
 * below is only its own content.
 */
@Composable
fun OnboardingScaffold(
    onBack: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    progressSteps: Int = 0,
    progressCurrent: Int = 0,
    footer: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularBackButton(onClick = onBack)
            if (progressSteps > 0) {
                Spacer(modifier = Modifier.width(14.dp))
                SegmentedProgress(
                    total = progressSteps,
                    current = progressCurrent,
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
                // Balances the back button so the bar stays centred.
                Spacer(modifier = Modifier.width(48.dp))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )

        if (footer != null) {
            Spacer(modifier = Modifier.height(12.dp))
            footer()
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

/** The bar of filled segments that tracks progress through a flow. */
@Composable
fun SegmentedProgress(
    total: Int,
    current: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .background(
                        if (index <= current) accent else Outline,
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

/** The row of dots the paged flows carry under their footer. */
@Composable
fun StepDots(
    total: Int,
    current: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(total) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (index == current) 7.dp else 6.dp)
                    .background(if (index == current) accent else Outline, CircleShape)
            )
        }
    }
}

/** The flow's identity, repeated at the top of every page: badge, name, tagline. */
@Composable
fun FlowBrandHeader(
    type: ProtectionType,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .border(2.dp, type.accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = type.icon,
                contentDescription = null,
                tint = type.accent,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = stringResource(type.labelRes),
                color = type.accent,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(type.taglineRes),
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

/** Centred page title with its explanatory line. */
@Composable
fun FlowTitle(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    suffix: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (suffix != null) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = suffix,
                    color = TextTertiary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 12.5.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/** The "Minimum 2 contacts" style pills that state a rule before the form. */
@Composable
fun RequirementChips(
    labels: List<String>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        labels.forEach { label ->
            Row(
                modifier = Modifier
                    .background(SurfaceCard, RoundedCornerShape(18.dp))
                    .border(1.dp, Outline, RoundedCornerShape(18.dp))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = label, color = TextPrimary, fontSize = 11.5.sp)
            }
        }
    }
}

/** A numbered heading — the small filled circle followed by its title. */
@Composable
fun NumberedHeading(
    number: Int,
    title: String,
    accent: Color,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    subtitle: String? = null
) {
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(accent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = number.toString(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (suffix != null) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = suffix, color = TextTertiary, fontSize = 11.5.sp)
                }
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/**
 * A bare text field for the manual-entry forms.
 *
 * [leadingIcon] and [minHeight] exist for the dialogs, where a field stands on its
 * own rather than in a labelled column and has to say for itself what it wants.
 */
@Composable
fun OnboardingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    accent: Color,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    leadingIcon: ImageVector? = null,
    minHeight: Int = 38
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(minHeight.dp)
            .background(SurfaceInput, FieldShape)
            .border(1.dp, Outline, FieldShape)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = TextTertiary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = TextPrimary, fontSize = 12.sp),
                cursorBrush = SolidColor(accent),
                keyboardOptions = KeyboardOptions(
                    keyboardType = keyboardType,
                    capitalization = capitalization
                ),
                modifier = Modifier.fillMaxWidth()
            )
            if (value.isEmpty()) {
                Text(text = placeholder, color = TextTertiary, fontSize = 12.sp)
            }
        }
    }
}

/** The circular initials that stand in for a contact photo. */
@Composable
fun ContactAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Int = 36
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(avatarColorFor(name), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initialsFor(name),
            color = Color.White,
            fontSize = (size * 0.38f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * A contact waiting on its code, with the controls that resolve it.
 *
 * The same card shows the verified state once the code comes back, so the user
 * watches one row change rather than the list being swapped underneath them.
 *
 * [onVerify] is what marks the number verified — the contact confirms, and the
 * user taps it here. Sharing the app is not offered per contact: one
 * [PukaarAlertShareCard] under the whole list covers everybody on the page.
 */
@Composable
fun ContactStatusCard(
    contact: OnboardingContact,
    accent: Color,
    onResend: () -> Unit,
    onVerify: () -> Unit,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard, CardShape)
            .border(1.dp, Outline, CardShape)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ContactAvatar(name = contact.name)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                ContactNameLine(contact = contact, fontSize = 13.5.sp)
                Text(text = contact.phone, color = TextSecondary, fontSize = 12.sp)
            }
            if (contact.verified) {
                VerifiedMark()
            } else if (onDelete != null) {
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.onboarding_remove),
                        tint = PukaarRed,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }

        if (!contact.verified) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                    tint = PendingAmber,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.onboarding_verification_pending),
                    color = PendingAmber,
                    fontSize = 11.5.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .background(SurfaceElevated, RoundedCornerShape(7.dp))
                        .border(1.dp, Outline, RoundedCornerShape(7.dp))
                        .clickable(onClick = onResend)
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_resend_code),
                        color = TextPrimary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            StatusAction(
                text = stringResource(R.string.onboarding_verify_now),
                onClick = onVerify,
                accent = accent,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** The action under a pending contact. */
@Composable
private fun StatusAction(
    text: String,
    onClick: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(9.dp)
    Box(
        modifier = modifier
            .background(accent, shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * The one invitation to put PUKAAR Alert on a trusted contact's phone.
 *
 * It replaces the per-contact "Share App" button: the app is shared once and
 * serves everybody on the page, so repeating the action beside every name only
 * made the list noisier. Wording and colour come from [type] — the contact is
 * being asked to receive that flow's alerts, not alerts in general.
 */
@Composable
fun PukaarAlertShareCard(
    type: ProtectionType,
    onShareApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = type.accent
    val shape = RoundedCornerShape(14.dp)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(alpha = 0.16f), accent.copy(alpha = 0.04f))
                    )
                )
                .border(1.dp, accent, shape)
        ) {
            // Sits behind the content and runs off the right edge, where the clip
            // cuts it — the rings read as a signal spreading out of the card.
            SignalRings(
                accent = accent,
                modifier = Modifier.align(Alignment.CenterEnd)
            )

            Row(
                modifier = Modifier.padding(
                    start = 14.dp,
                    end = 52.dp,
                    top = 14.dp,
                    bottom = 14.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AlertGlyph(icon = type.icon, accent = accent)
                Spacer(modifier = Modifier.width(11.dp))
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(76.dp)
                        .background(accent.copy(alpha = 0.35f))
                )
                Spacer(modifier = Modifier.width(11.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.onboarding_alert_app_title),
                        color = accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = alertAppBody(type, accent),
                        color = TextPrimary.copy(alpha = 0.88f),
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ShareAlertButton(accent = accent, onClick = onShareApp)
                }
            }
        }

        AlertReadyNote()
    }
}

/** The card's body line, with the flow's own word carrying the accent. */
@Composable
private fun alertAppBody(type: ProtectionType, accent: Color) = buildAnnotatedString {
    val keyword = stringResource(
        when (type) {
            ProtectionType.SOS -> R.string.onboarding_alert_app_keyword_sos
            ProtectionType.INACTIVITY -> R.string.onboarding_alert_app_keyword_inactivity
        }
    )
    val body = stringResource(
        when (type) {
            ProtectionType.SOS -> R.string.onboarding_alert_app_body_sos
            ProtectionType.INACTIVITY -> R.string.onboarding_alert_app_body_inactivity
        },
        keyword
    )

    val start = body.indexOf(keyword)
    if (start < 0) {
        // A translation that dropped the placeholder still has to read correctly.
        append(body)
    } else {
        append(body.take(start))
        withStyle(SpanStyle(color = accent, fontWeight = FontWeight.Bold)) {
            append(keyword)
        }
        append(body.drop(start + keyword.length))
    }
}

/** The flow's badge, lit from behind. */
@Composable
private fun AlertGlyph(
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(58.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(
                    Brush.radialGradient(
                        listOf(accent.copy(alpha = 0.45f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(accent, CircleShape)
                .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(23.dp)
            )
        }
    }
}

/** The dashed rings and shield that decorate the card's trailing edge. */
@Composable
private fun SignalRings(
    accent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(96.dp)
            .offset(x = 26.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val dashes = PathEffect.dashPathEffect(
                floatArrayOf(2.dp.toPx(), 4.dp.toPx())
            )
            listOf(0.30f, 0.40f, 0.50f).forEach { fraction ->
                drawCircle(
                    color = accent.copy(alpha = 0.40f),
                    radius = size.minDimension * fraction,
                    style = Stroke(width = 1.dp.toPx(), pathEffect = dashes)
                )
            }
        }
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = accent.copy(alpha = 0.55f),
            modifier = Modifier.size(26.dp)
        )
    }
}

/** The filled call to action inside the share card. */
@Composable
private fun ShareAlertButton(
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(accent, shape)
            .clip(shape)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.IosShare,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = stringResource(R.string.onboarding_alert_app_share).uppercase(),
            color = Color.White,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** What happens once the contact has the app — the line under the share card. */
@Composable
private fun AlertReadyNote(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard, CardShape)
            .border(1.dp, Outline, CardShape)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.VerifiedUser,
            contentDescription = null,
            tint = AccentPurple,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.onboarding_alert_app_ready),
            color = TextSecondary,
            fontSize = 11.5.sp,
            lineHeight = 16.sp
        )
    }
}

/**
 * A contact's name with the relation it was filed under.
 *
 * Every row that shows a person goes through here, so a contact added in a flow
 * that asks for a relation carries it on every later screen, and one added where
 * the question is not asked simply shows the name.
 */
@Composable
fun ContactNameLine(
    contact: OnboardingContact,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 13.sp
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = contact.name,
            color = TextPrimary,
            fontSize = fontSize,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (contact.relation != null) {
            Spacer(modifier = Modifier.width(6.dp))
            RelationTag(relation = contact.relation)
        }
    }
}

/** The pill carrying a relation — deliberately quiet, it labels rather than shouts. */
@Composable
fun RelationTag(
    relation: ContactRelation,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(percent = 50)
    Text(
        text = stringResource(relation.labelRes),
        color = TextSecondary,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        modifier = modifier
            .background(SurfaceElevated, shape)
            .border(1.dp, Outline, shape)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/** The compact verified row used by the summaries. */
@Composable
fun VerifiedContactRow(
    contact: OnboardingContact,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard, CardShape)
            .border(1.dp, Outline, CardShape)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(10.dp))
        } else {
            ContactAvatar(name = contact.name, size = 32)
            Spacer(modifier = Modifier.width(10.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            ContactNameLine(contact = contact)
            Text(text = contact.phone, color = TextSecondary, fontSize = 11.5.sp)
        }
        // A contact reached through "I'll do this later" lands in the summary
        // unverified, and must not be shown as though it came back.
        if (contact.verified) {
            VerifiedMark()
        } else {
            Text(
                text = stringResource(R.string.onboarding_verification_pending),
                color = PendingAmber,
                fontSize = 11.5.sp
            )
        }
    }
}

@Composable
private fun VerifiedMark(modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.onboarding_verified),
            color = SuccessGreen,
            fontSize = 11.5.sp
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** The blue "How it works?" panel. */
@Composable
fun InfoBox(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(AccentBlue.copy(alpha = 0.08f), CardShape)
            .border(1.dp, AccentBlue.copy(alpha = 0.45f), CardShape)
            .padding(12.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Info,
            contentDescription = null,
            tint = AccentBlue,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                color = AccentBlue,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = body,
                color = TextSecondary,
                fontSize = 11.5.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 3.dp)
            )
        }
    }
}

/** The red-shield "Important" note. */
@Composable
fun ImportantNote(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                tint = PukaarRed,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                color = PukaarRed,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            text = body,
            color = TextSecondary,
            fontSize = 11.5.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(top = 5.dp)
        )
    }
}

/** The outlined "+ Add Contact" slot. */
@Composable
fun AddSlotButton(
    text: String,
    onClick: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector = Icons.Filled.Add
) {
    val tint = if (enabled) accent else TextTertiary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceInput, FieldShape)
            .border(1.dp, tint.copy(alpha = 0.55f), FieldShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = text,
            color = tint,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** The flow's filled call-to-action, tinted by the accent it is given. */
@Composable
fun AccentButton(
    text: String,
    onClick: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = TextPrimary,
            disabledContainerColor = SurfaceElevated,
            disabledContentColor = TextTertiary
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text.uppercase(), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

/** The quiet outlined action, for "Add another" and the like. */
@Composable
fun AccentOutlineButton(
    text: String,
    onClick: () -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.6f)), RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(7.dp))
        Text(
            text = text.uppercase(),
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** A plain text action, such as "I'll do this later". */
@Composable
fun TextAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = TextSecondary
) {
    Text(
        text = text,
        color = color,
        fontSize = 12.sp,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    )
}

/** A saved help number, with its edit and delete controls. */
@Composable
fun HelpNumberRow(
    index: Int,
    number: HelpNumber,
    accent: Color,
    modifier: Modifier = Modifier,
    optional: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard, CardShape)
            .border(1.dp, Outline, CardShape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = index.toString(),
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = number.label,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (number.relation != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    RelationTag(relation = number.relation)
                }
                if (optional) {
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.onboarding_optional),
                        color = TextTertiary,
                        fontSize = 10.5.sp
                    )
                }
            }
            Text(text = number.phone, color = TextSecondary, fontSize = 11.5.sp)
        }
        if (onEdit != null) {
            IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.onboarding_edit),
                    tint = accent,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        if (onDelete != null) {
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.onboarding_remove),
                    tint = PukaarRed,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/** The hours dropdown used by the inactivity timings. */
@Composable
fun HoursDropdown(
    hours: Int,
    onHoursChange: (Int) -> Unit,
    accent: Color,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .background(SurfaceInput, FieldShape)
                .border(1.dp, Outline, FieldShape)
                .clickable { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = hours.toString(),
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = stringResource(R.string.onboarding_hours),
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            InactivityHourOptions.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(R.string.onboarding_hours_value, option),
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        onHoursChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** A label/value line, used by the completion summary. */
@Composable
fun SummaryLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TextPrimary
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** A contact that has been added but not yet asked to verify. */
@Composable
fun AddedContactRow(
    contact: OnboardingContact,
    accent: Color,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    caption: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceCard, CardShape)
            .border(1.dp, Outline, CardShape)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ContactAvatar(name = contact.name, size = 34)
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            ContactNameLine(contact = contact)
            Text(text = contact.phone, color = TextSecondary, fontSize = 11.5.sp)
            if (caption != null) {
                Text(
                    text = caption,
                    color = accent,
                    fontSize = 10.5.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(30.dp)) {
            Icon(
                imageVector = Icons.Filled.Delete,
                contentDescription = stringResource(R.string.onboarding_remove),
                tint = PukaarRed,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** A small heading above a group inside a summary card. */
@Composable
fun SummaryHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 6.dp, bottom = 2.dp)
    )
}
