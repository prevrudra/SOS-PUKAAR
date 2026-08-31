package com.pukaar.app.ui.screen.legal

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pukaar.app.R
import com.pukaar.app.ui.component.InfoRow
import com.pukaar.app.ui.component.PukaarScreen
import com.pukaar.app.ui.component.RowDivider
import com.pukaar.app.ui.component.SectionCard
import com.pukaar.app.ui.theme.PukaarTheme
import com.pukaar.app.ui.theme.TextSecondary

/** Menu tile: the terms, in short. */
@Composable
fun LegalTermsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PukaarScreen(
        title = stringResource(R.string.legal_title),
        onBack = onBack,
        modifier = modifier
    ) {
        Text(
            text = stringResource(R.string.legal_intro),
            color = TextSecondary,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        SectionCard(modifier = Modifier.verticalScroll(rememberScrollState())) {
            val entries = listOf(
                R.string.legal_service_title to R.string.legal_service_body,
                R.string.legal_reliability_title to R.string.legal_reliability_body,
                R.string.legal_account_title to R.string.legal_account_body,
                R.string.legal_payment_title to R.string.legal_payment_body
            )
            entries.forEachIndexed { index, (title, body) ->
                InfoRow(title = stringResource(title), body = stringResource(body))
                if (index != entries.lastIndex) {
                    RowDivider()
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 760)
@Composable
private fun LegalTermsScreenPreview() {
    PukaarTheme {
        LegalTermsScreen(onBack = {})
    }
}
