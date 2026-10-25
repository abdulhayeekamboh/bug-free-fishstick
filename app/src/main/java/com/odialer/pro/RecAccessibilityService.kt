package com.odialer.pro

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Intentionally empty. On Android 10+ an enabled accessibility service lets the app keep
 * microphone access while a call is in progress on many phones.
 */
class RecAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
