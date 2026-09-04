package com.pukaar.app.ui.screen.faq

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pukaar.app.R
import com.pukaar.app.ui.component.NavigationRow
import com.pukaar.app.ui.component.PukaarScreen
import com.pukaar.app.ui.component.RowDivider
import com.pukaar.app.ui.component.SectionCard
import com.pukaar.app.ui.theme.PukaarTheme

/** The questions listed on the FAQ screen, in order. */
enum class FaqEntry(@StringRes val questionRes: Int, @StringRes val answerRes: Int) {
    HOW_IT_WORKS(R.string.faq_q1, R.string.faq_a1),
    HOW_TO_TRIGGER(R.string.faq_q2, R.string.faq_a2),
    EMERGENCY_NUMBER(R.string.faq_q3, R.string.faq_a3),
    DATA_SAFETY(R.string.faq_q4, R.string.faq_a4),
    MORE(R.string.faq_q5, R.string.faq_a5)
}

/** Menu item 11. */
@Composable
fun FaqScreen(
    onBack: () -> Unit,
    onEntryClick: (FaqEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    PukaarScreen(
        title = stringResource(R.string.faq_title),
        onBack = onBack,
        modifier = modifier
    ) {
        SectionCard(contentPadding = PaddingValues(horizontal = 16.dp)) {
            FaqEntry.entries.forEachIndexed { index, entry ->
                NavigationRow(
                    title = stringResource(entry.questionRes),
                    onClick = { onEntryClick(entry) }
                )
                if (index != FaqEntry.entries.lastIndex) {
                    RowDivider()
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun FaqScreenPreview() {
    PukaarTheme {
        FaqScreen(onBack = {}, onEntryClick = {})
    }
}
