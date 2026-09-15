package com.pukaar.highalert

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Modifier
import com.pukaar.highalert.data.AlertRepository
import com.pukaar.highalert.ui.SampleAlertScreen
import com.pukaar.highalert.ui.theme.PukaarAlertTheme

/** Opens Ritik's sample SOS UI without firing the alarm (for design review). */
class SamplePreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PukaarAlertTheme {
                SampleAlertScreen(
                    alert = AlertRepository.sampleAlert(),
                    onBack = { finish() },
                    title = "Sample SOS Alert",
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                )
            }
        }
    }
}
