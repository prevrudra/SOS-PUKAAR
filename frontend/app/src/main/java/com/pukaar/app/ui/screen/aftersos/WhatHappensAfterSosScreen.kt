package com.pukaar.app.ui.screen.aftersos

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
import com.pukaar.app.ui.theme.PukaarRed
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.TextSecondary

/** Menu tile: the sequence a single SOS press sets off. */
@Composable
fun WhatHappensAfterSosScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PukaarScreen(
        title = stringResource(R.string.after_sos_title),
        onBack = onBack,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.after_sos_intro),
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        SectionCard {
            val steps = listOf(
                R.string.after_sos_step1_title to R.string.after_sos_step1_body,
                R.string.after_sos_step2_title to R.string.after_sos_step2_body,
                R.string.after_sos_step3_title to R.string.after_sos_step3_body,
                R.string.after_sos_step4_title to R.string.after_sos_step4_body,
                R.string.after_sos_step5_title to R.string.after_sos_step5_body
            )
            steps.forEachIndexed { index, (title, body) ->
                StepRow(
                    number = index + 1,
                    title = stringResource(title),
                    description = stringResource(body),
                    accent = PukaarRed
                )
                if (index != steps.lastIndex) {
                    RowDivider()
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 760)
@Composable
private fun WhatHappensAfterSosScreenPreview() {
    PukaarTheme {
        WhatHappensAfterSosScreen(onBack = {})
    }
}
