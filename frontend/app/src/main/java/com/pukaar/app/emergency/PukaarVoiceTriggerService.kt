package com.pukaar.app.emergency

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

/**
 * Placeholder for optional emergency phrase listening.
 * Continuous background mic use is heavily restricted by Android policy;
 * production must use on-device hotword / consented foreground listening with fallbacks.
 */
class PukaarVoiceTriggerService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("PUKAAR", "Voice trigger service started (device-dependent; fallback to App SOS required)")
        return START_STICKY
    }
}
