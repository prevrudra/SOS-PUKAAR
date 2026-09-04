package com.pukaar.app

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.pukaar.app.emergency.VolumeTriggerController
import com.pukaar.app.integration.PukaarAppNavHost
import com.pukaar.app.payment.RazorpayPaymentBridge
import com.pukaar.app.ui.theme.PukaarTheme
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener

class MainActivity : ComponentActivity(), PaymentResultWithDataListener {

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
        setIntent(intent)
        handleSosIntent(intent)
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (VolumeTriggerController.onVolumeUp(this)) return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun handleSosIntent(intent: Intent?) {
        if (intent?.action == PukaarApp.ACTION_SOS) {
            PukaarApp.instance.signalHardwareSos()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        RazorpayPaymentBridge.onPaymentSuccess(razorpayPaymentId, paymentData)
    }

    override fun onPaymentError(code: Int, description: String?, paymentData: PaymentData?) {
        RazorpayPaymentBridge.onPaymentError(code, description, paymentData)
    }
}
