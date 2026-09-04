package com.pukaar.app.emergency

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

/**
 * Listens for triple volume-up when PUKAAR is closed or in the background.
 * User must enable this once in Android Accessibility settings.
 */
class VolumeSosAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            if (VolumeTriggerController.onVolumeUp(this)) {
                return true
            }
        }
        return super.onKeyEvent(event)
    }
}
