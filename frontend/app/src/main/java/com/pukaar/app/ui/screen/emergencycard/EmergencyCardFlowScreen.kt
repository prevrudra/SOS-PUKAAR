package com.pukaar.app.ui.screen.emergencycard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.pukaar.app.R
import com.pukaar.app.ui.theme.PukaarTheme

/**
 * The five-step card builder, as one screen.
 *
 * One destination rather than five: the steps share a draft, and a nav graph that
 * split them would have to pass the whole thing back and forth on every Next. The
 * step being shown is remembered, so "Edit" from the review page and Back out of
 * a step both land where the user expects.
 *
 * The draft itself is hoisted to the caller — the flow can be left and re-entered
 * from the finished card's "Edit Card" without losing anything.
 */
@Composable
fun EmergencyCardFlowScreen(
    draft: EmergencyCardDraft,
    onDraftChange: (EmergencyCardDraft) -> Unit,
    onGenerate: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by rememberSaveable { mutableStateOf(EmergencyCardStep.PERSONAL) }

    // Back on the first step leaves the flow; anywhere else it is one step back.
    val goBack = {
        if (step.isFirst) onExit() else step = step.previous()
    }

    EmergencyCardScaffold(
        onBack = goBack,
        step = step,
        modifier = modifier,
        footer = {
            CardStepFooter(
                onBack = goBack,
                onNext = {
                    if (step.isLast) onGenerate() else step = step.next()
                },
                nextLabel = stringResource(
                    if (step.isLast) R.string.card_generate else R.string.action_next
                ),
                // Only step 1 can be incomplete; the rest are optional throughout.
                nextEnabled = step != EmergencyCardStep.PERSONAL || draft.canGenerate,
                showBack = !step.isFirst
            )
        }
    ) {
        when (step) {
            EmergencyCardStep.PERSONAL -> PersonalDetailsStep(
                details = draft.personal,
                onChange = { onDraftChange(draft.copy(personal = it)) }
            )

            EmergencyCardStep.IDENTIFICATION -> IdentificationStep(
                details = draft.identification,
                onChange = { onDraftChange(draft.copy(identification = it)) }
            )

            EmergencyCardStep.MEDICAL -> MedicalStep(
                details = draft.medical,
                onChange = { onDraftChange(draft.copy(medical = it)) }
            )

            EmergencyCardStep.TRAVEL -> TravelStep(
                details = draft.travel,
                onChange = { onDraftChange(draft.copy(travel = it)) }
            )

            EmergencyCardStep.PREVIEW -> ReviewStep(
                draft = draft,
                onEdit = { step = it }
            )
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun EmergencyCardFlowScreenPreview() {
    PukaarTheme {
        EmergencyCardFlowScreen(
            draft = SampleCardDraft,
            onDraftChange = {},
            onGenerate = {},
            onExit = {}
        )
    }
}
