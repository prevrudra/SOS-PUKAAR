package com.pukaar.app.ui.screen.elderlyworks

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.PukaarScreen
import com.pukaar.app.ui.component.RowDivider
import com.pukaar.app.ui.component.SectionCard
import com.pukaar.app.ui.component.StepRow
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.TextSecondary

/**
 * Menu item 13. An explainer, not a settings screen — the switches themselves
 * live on [com.pukaar.app.ui.screen.elderlyhelp.ElderlyHelpScreen].
 */
@Composable
fun HowElderlyHelpWorksScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PukaarScreen(
        title = stringResource(R.string.elderly_works_title),
        onBack = onBack,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.elderly_works_intro),
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        SectionCard {
            StepRow(
                number = 1,
                title = stringResource(R.string.elderly_works_step1_title),
                description = stringResource(R.string.elderly_works_step1_body)
            )
            RowDivider()
            StepRow(
                number = 2,
                title = stringResource(R.string.elderly_works_step2_title),
                description = stringResource(R.string.elderly_works_step2_body)
            )
            RowDivider()
            StepRow(
                number = 3,
                title = stringResource(R.string.elderly_works_step3_title),
                description = stringResource(R.string.elderly_works_step3_body)
            )
            RowDivider()
            StepRow(
                number = 4,
                title = stringResource(R.string.elderly_works_step4_title),
                description = stringResource(R.string.elderly_works_step4_body)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun HowElderlyHelpWorksScreenPreview() {
    PukaarTheme {
        HowElderlyHelpWorksScreen(onBack = {})
    }
}
