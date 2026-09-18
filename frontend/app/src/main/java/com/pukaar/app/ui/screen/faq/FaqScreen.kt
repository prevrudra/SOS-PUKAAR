package com.pukaar.app.ui.screen.faq

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.PukaarScreen
import com.pukaar.app.ui.component.RowDivider
import com.pukaar.app.ui.component.SectionCard
import com.pukaar.app.ui.theme.AccentBlue
import com.pukaar.app.ui.theme.PukaarOrange
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.TextPrimary
import com.pukaar.app.ui.theme.TextSecondary

/**
 * The headings the questions are filed under.
 *
 * One per thing a user is actually worried about, rather than one long list: the
 * two protections explain themselves, and anything that spans them sits under
 * General at the top.
 */
enum class FaqSection(
    @StringRes val titleRes: Int,
    @StringRes val introRes: Int,
    val icon: ImageVector,
    val accent: Color
) {
    GENERAL(
        R.string.faq_section_general,
        R.string.faq_section_general_intro,
        Icons.Filled.HelpOutline,
        AccentBlue
    ),
    SOS(
        R.string.faq_section_sos,
        R.string.faq_section_sos_intro,
        Icons.Filled.Sos,
        PukaarRed
    ),
    INACTIVITY(
        R.string.faq_section_inactivity,
        R.string.faq_section_inactivity_intro,
        Icons.Filled.Timer,
        PukaarOrange
    )
}

/**
 * Every question the FAQ answers, in the order it is listed.
 *
 * Question and answer are declared together so a row can never be shown without
 * what it says; the screen groups them by [section] rather than keeping a
 * separate list per heading.
 */
enum class FaqEntry(
    val section: FaqSection,
    @StringRes val questionRes: Int,
    @StringRes val answerRes: Int
) {
    HOW_IT_WORKS(FaqSection.GENERAL, R.string.faq_q1, R.string.faq_a1),
    HOW_TO_TRIGGER(FaqSection.GENERAL, R.string.faq_q2, R.string.faq_a2),
    EMERGENCY_NUMBER(FaqSection.GENERAL, R.string.faq_q3, R.string.faq_a3),
    DATA_SAFETY(FaqSection.GENERAL, R.string.faq_q4, R.string.faq_a4),
    MORE(FaqSection.GENERAL, R.string.faq_q5, R.string.faq_a5),

    SOS_WHO_IS_ALERTED(FaqSection.SOS, R.string.faq_sos_q1, R.string.faq_sos_a1),
    SOS_RECORDING(FaqSection.SOS, R.string.faq_sos_q2, R.string.faq_sos_a2),
    SOS_CLOUD(FaqSection.SOS, R.string.faq_sos_q3, R.string.faq_sos_a3),
    SOS_DETAILS_SENT(FaqSection.SOS, R.string.faq_sos_q4, R.string.faq_sos_a4),
    SOS_EMERGENCY_SERVICES(FaqSection.SOS, R.string.faq_sos_q5, R.string.faq_sos_a5),
    SOS_NOTHING_ELSE(FaqSection.SOS, R.string.faq_sos_q6, R.string.faq_sos_a6),

    INACTIVITY_WINDOW(FaqSection.INACTIVITY, R.string.faq_inactivity_q1, R.string.faq_inactivity_a1),
    INACTIVITY_RESET(FaqSection.INACTIVITY, R.string.faq_inactivity_q2, R.string.faq_inactivity_a2),
    INACTIVITY_REMINDER(FaqSection.INACTIVITY, R.string.faq_inactivity_q3, R.string.faq_inactivity_a3),
    INACTIVITY_ALERT(FaqSection.INACTIVITY, R.string.faq_inactivity_q4, R.string.faq_inactivity_a4);

    companion object {
        /** The entries under [section], in declaration order. */
        fun of(section: FaqSection): List<FaqEntry> = entries.filter { it.section == section }
    }
}

/**
 * Menu item 11.
 *
 * Answers open in place: one at a time, so the list never becomes a wall of text
 * and the question being read stays near the top of the screen. [onEntryClick]
 * still reports which question was opened.
 */
@Composable
fun FaqScreen(
    onBack: () -> Unit,
    onEntryClick: (FaqEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    var openEntry by rememberSaveable { mutableStateOf<FaqEntry?>(null) }

    PukaarScreen(
        title = stringResource(R.string.faq_title),
        onBack = onBack,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            FaqSection.entries.forEach { section ->
                FaqSectionBlock(
                    section = section,
                    openEntry = openEntry,
                    onEntryClick = { entry ->
                        // Tapping the open question closes it again.
                        openEntry = if (openEntry == entry) null else entry
                        if (openEntry == entry) onEntryClick(entry)
                    }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun FaqSectionBlock(
    section: FaqSection,
    openEntry: FaqEntry?,
    onEntryClick: (FaqEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    val entries = FaqEntry.of(section)

    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            Icon(
                imageVector = section.icon,
                contentDescription = null,
                tint = section.accent,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(section.titleRes).uppercase(),
                color = section.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
        Text(
            text = stringResource(section.introRes),
            color = TextSecondary,
            fontSize = 11.5.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        SectionCard(contentPadding = PaddingValues(horizontal = 16.dp)) {
            entries.forEachIndexed { index, entry ->
                FaqRow(
                    entry = entry,
                    expanded = entry == openEntry,
                    accent = section.accent,
                    onClick = { onEntryClick(entry) }
                )
                if (index != entries.lastIndex) {
                    RowDivider()
                }
            }
        }
    }
}

@Composable
private fun FaqRow(
    entry: FaqEntry,
    expanded: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val chevronTurn by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "faqChevron"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .animateContentSize()
            .padding(vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(entry.questionRes),
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (expanded) FontWeight.SemiBold else FontWeight.Normal,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = if (expanded) accent else TextSecondary,
                modifier = Modifier
                    .size(18.dp)
                    .rotate(chevronTurn)
            )
        }

        if (expanded) {
            Text(
                text = stringResource(entry.answerRes),
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                modifier = Modifier.padding(top = 8.dp, end = 28.dp)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 900)
@Composable
private fun FaqScreenPreview() {
    PukaarTheme {
        FaqScreen(onBack = {}, onEntryClick = {})
    }
}
