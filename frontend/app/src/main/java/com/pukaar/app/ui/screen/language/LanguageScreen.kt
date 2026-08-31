package com.pukaar.app.ui.screen.language

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pukaar.app.R
import com.pukaar.app.ui.component.ChoiceRow
import com.pukaar.app.ui.component.PrimaryButton
import com.pukaar.app.ui.component.PukaarScreen
import com.pukaar.app.ui.component.SectionCard
import com.pukaar.app.ui.theme.PukaarTheme

/** The languages Pukaar ships with. `tag` is the BCP-47 code used by the locale. */
enum class AppLanguage(@StringRes val labelRes: Int, val tag: String) {
    ENGLISH(R.string.language_english, "en"),
    HINDI(R.string.language_hindi, "hi"),
    PUNJABI(R.string.language_punjabi, "pa"),
    MARATHI(R.string.language_marathi, "mr")
}

/** Menu item 9. */
@Composable
fun LanguageScreen(
    onBack: () -> Unit,
    onSave: (AppLanguage) -> Unit,
    modifier: Modifier = Modifier,
    initialLanguage: AppLanguage = AppLanguage.ENGLISH
) {
    var selected by remember { mutableStateOf(initialLanguage) }

    PukaarScreen(
        title = stringResource(R.string.language_title),
        onBack = onBack,
        modifier = modifier,
        bottomBar = {
            PrimaryButton(
                text = stringResource(R.string.action_save),
                onClick = { onSave(selected) }
            )
        }
    ) {
        SectionCard {
            AppLanguage.entries.forEach { language ->
                ChoiceRow(
                    title = stringResource(language.labelRes),
                    selected = language == selected,
                    onSelect = { selected = language }
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000, heightDp = 700)
@Composable
private fun LanguageScreenPreview() {
    PukaarTheme {
        LanguageScreen(onBack = {}, onSave = {})
    }
}
