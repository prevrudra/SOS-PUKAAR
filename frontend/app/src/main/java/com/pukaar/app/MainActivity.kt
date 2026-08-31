package com.pukaar.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pukaar.app.integration.PukaarAppNavHost
import com.pukaar.app.ui.theme.PukaarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        handleSosIntent(intent)
        setContent {
            PukaarTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    PukaarAppNavHost()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSosIntent(intent)
    }

    private fun handleSosIntent(intent: Intent?) {
        if (intent?.action == PukaarApp.ACTION_SOS) {
            PukaarApp.instance.signalHardwareSos()
        }
    }
}
