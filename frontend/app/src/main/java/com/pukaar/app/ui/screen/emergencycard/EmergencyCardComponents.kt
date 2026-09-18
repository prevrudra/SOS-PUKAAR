package com.pukaar.app.ui.screen.emergencycard

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val FieldShape = RoundedCornerShape(10.dp)
private val CardShape = RoundedCornerShape(14.dp)

/**
 * The frame every page of the Emergency Card flow sits in: the PUKAAR mark, the
 * five-step tracker, a scrolling body and a pinned footer of buttons.
 *
 * [step] drives the tracker; pass null on the pages that come after the card has
 * been made, which keep the header but have no step to be on.
 */
@Composable
fun EmergencyCardScaffold(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    step: EmergencyCardStep? = null,
    footer: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CardPalette.Background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        CardFlowHeader(onBack = onBack)

        if (step != null) {
            StepTracker(
                current = step,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )

        if (footer != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.padding(horizontal = 16.dp)) { footer() }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

/** The shield, the wordmark and "Emergency Card" under it, with a back control. */
@Composable
private fun CardFlowHeader(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardPalette.Surface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.action_back),
            tint = CardPalette.TextPrimary,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(34.dp)
                .clickable(onClick = onBack)
                .padding(6.dp)
        )

        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardShieldMark(size = 22)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    color = CardPalette.TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = stringResource(R.string.menu_emergency_card),
                    color = CardPalette.TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/** The red shield with a P in it, drawn small enough to sit in a header. */
@Composable
fun CardShieldMark(size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .background(CardPalette.Accent, RoundedCornerShape(percent = 28)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "P",
            color = Color.White,
            fontSize = (size * 0.55f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * The five numbered dots across the top, joined by a rule.
 *
 * A step behind the current one shows a tick rather than its number: the tracker
 * doubles as the answer to "how much of this is left", which is the question
 * somebody filling in a form actually has.
 */
@Composable
fun StepTracker(
    current: EmergencyCardStep,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        EmergencyCardStep.entries.forEach { step ->
            val done = step.ordinal < current.ordinal
            val active = step == current

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            when {
                                active || done -> CardPalette.Accent
                                else -> CardPalette.Border
                            },
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (done) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    } else {
                        Text(
                            text = (step.ordinal + 1).toString(),
                            color = if (active) Color.White else CardPalette.TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(step.labelRes),
                    color = if (active) CardPalette.TextPrimary else CardPalette.TextTertiary,
                    fontSize = 9.5.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/** The page's own heading and the line under it explaining what to do. */
@Composable
fun CardPageTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = CardPalette.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = subtitle,
            color = CardPalette.TextSecondary,
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

/**
 * A field's label, with the mark that says whether it has to be filled in.
 *
 * Every field carries one. A form where only some fields are marked leaves the
 * user guessing about the rest, and this one is mostly optional — saying so is
 * what stops people abandoning it half way.
 */
@Composable
fun FieldLabel(
    text: String,
    modifier: Modifier = Modifier,
    required: Boolean = false
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            color = CardPalette.TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.width(4.dp))
        if (required) {
            Text(text = "★", color = CardPalette.Accent, fontSize = 9.sp)
        } else {
            Text(
                text = stringResource(R.string.onboarding_optional),
                color = CardPalette.TextTertiary,
                fontSize = 10.5.sp
            )
        }
    }
}

/** Label and field together — the unit every step is actually built from. */
@Composable
fun LabelledField(
    label: String,
    modifier: Modifier = Modifier,
    required: Boolean = false,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        FieldLabel(text = label, required = required)
        Spacer(modifier = Modifier.height(5.dp))
        content()
    }
}

/**
 * The flow's text input: a bordered box with an optional leading glyph.
 *
 * Built from [BasicTextField] rather than an [androidx.compose.material3.TextField]
 * so it can be light while the app's Material theme stays dark.
 */
@Composable
fun CardTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingTint: Color = CardPalette.TextTertiary,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minHeight: Int = 42
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight.dp)
            .background(CardPalette.Field, FieldShape)
            .border(1.dp, CardPalette.Border, FieldShape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = leadingTint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Box(modifier = Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    color = CardPalette.TextTertiary,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = TextStyle(
                    color = CardPalette.TextPrimary,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp
                ),
                cursorBrush = SolidColor(CardPalette.Accent),
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * A field that opens a menu instead of a keyboard.
 *
 * Generic over what is being picked so the blood group, the gender and the
 * relation dropdowns are one component and cannot drift apart.
 */
@Composable
fun <T> CardDropdown(
    options: List<T>,
    selected: T?,
    onSelect: (T) -> Unit,
    labelOf: @Composable (T) -> String,
    placeholder: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    leadingTint: Color = CardPalette.TextTertiary
) {
    var open by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .background(CardPalette.Field, FieldShape)
                .border(1.dp, CardPalette.Border, FieldShape)
                .clickable { open = true }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = leadingTint,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = selected?.let { labelOf(it) } ?: placeholder,
                color = if (selected == null) CardPalette.TextTertiary else CardPalette.TextPrimary,
                fontSize = 12.5.sp,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = CardPalette.TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }

        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.background(CardPalette.Surface)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = labelOf(option),
                            color = CardPalette.TextPrimary,
                            fontSize = 13.sp
                        )
                    },
                    onClick = {
                        onSelect(option)
                        open = false
                    }
                )
            }
        }
    }
}

/**
 * A date field that opens the Material date picker.
 *
 * Dates are held as epoch millis rather than text so the review page, the card
 * face and the printed copy all format them the same way — and so a date can
 * never be stored in a form nobody can read back.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDateField(
    value: Long?,
    onValueChange: (Long) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    var picking by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .background(CardPalette.Field, FieldShape)
            .border(1.dp, CardPalette.Border, FieldShape)
            .clickable { picking = true }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.CalendarMonth,
            contentDescription = null,
            tint = CardPalette.TextTertiary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value?.let(::formatCardDate) ?: placeholder,
            color = if (value == null) CardPalette.TextTertiary else CardPalette.TextPrimary,
            fontSize = 12.5.sp,
            modifier = Modifier.weight(1f)
        )
    }

    if (picking) {
        val state = rememberDatePickerState(initialSelectedDateMillis = value)
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let(onValueChange)
                        picking = false
                    }
                ) {
                    Text(text = stringResource(R.string.action_ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { picking = false }) {
                    Text(text = stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

/**
 * Dates are formatted in UTC on purpose.
 *
 * The picker hands back midnight UTC for the day that was tapped; reading it
 * back in a local zone west of Greenwich would show the day before.
 */
fun formatCardDate(millis: Long): String {
    val format = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    format.timeZone = TimeZone.getTimeZone("UTC")
    return format.format(Date(millis))
}

/** The two-option pill switch — Indian Citizen against Foreign National. */
@Composable
fun <T> CardSegmentedToggle(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelOf: @Composable (T) -> String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CardPalette.Note, RoundedCornerShape(22.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            val active = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .background(
                        if (active) CardPalette.Accent else Color.Transparent,
                        RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelect(option) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelOf(option),
                    color = if (active) Color.White else CardPalette.TextSecondary,
                    fontSize = 12.5.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

/** The grey panel that explains something rather than asking for it. */
@Composable
fun CardNote(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Lock,
    tint: Color = CardPalette.TextSecondary,
    background: Color = CardPalette.Note
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background, FieldShape)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            color = tint,
            fontSize = 11.5.sp,
            lineHeight = 16.sp
        )
    }
}

/** A white block with a hairline border — what each group of fields sits on. */
@Composable
fun CardPanel(
    modifier: Modifier = Modifier,
    background: Color = CardPalette.Surface,
    border: Color = CardPalette.Border,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(background, CardShape)
            .border(BorderStroke(1.dp, border), CardShape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content
    )
}

/** The filled red action: Next, Generate Card, Download. */
@Composable
fun CardPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = Icons.AutoMirrored.Filled.ArrowForward,
    iconLeading: Boolean = false
) {
    val shape = RoundedCornerShape(10.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(
                if (enabled) CardPalette.Accent else CardPalette.BorderStrong,
                shape
            )
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null && iconLeading) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        if (icon != null && !iconLeading) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** The outlined counterpart: Back, and the secondary actions on the card page. */
@Composable
fun CardSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val shape = RoundedCornerShape(10.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(CardPalette.Surface, shape)
            .border(1.dp, CardPalette.Border, shape)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CardPalette.TextPrimary,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            color = CardPalette.TextPrimary,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/** Back on the left, the step's own action on the right. */
@Composable
fun CardStepFooter(
    onBack: () -> Unit,
    onNext: () -> Unit,
    nextLabel: String,
    modifier: Modifier = Modifier,
    nextEnabled: Boolean = true,
    showBack: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (showBack) {
            CardSecondaryButton(
                text = stringResource(R.string.action_back),
                onClick = onBack,
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                modifier = Modifier.weight(1f)
            )
        }
        CardPrimaryButton(
            text = nextLabel,
            onClick = onNext,
            enabled = nextEnabled,
            modifier = Modifier.weight(1f)
        )
    }
}

/** The dashed "Add Secondary Contact" control. */
@Composable
fun CardAddButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(38.dp)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = CardPalette.TextSecondary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = CardPalette.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
