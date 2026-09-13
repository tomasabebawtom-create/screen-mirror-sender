package com.example.screenmirror

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.util.DisplayMetrics
import android.view.accessibility.AccessibilityEvent

class RemoteControlAccessibilityService : AccessibilityService() {

    companion object {
        var instance: RemoteControlAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    /** nx, ny are normalized (0..1) coordinates relative to the full screen. */
    fun performTap(nx: Float, ny: Float) {
        val dm: DisplayMetrics = resources.displayMetrics
        val path = Path().apply { moveTo(nx * dm.widthPixels, ny * dm.heightPixels) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }

    fun performSwipe(nx1: Float, ny1: Float, nx2: Float, ny2: Float, durationMs: Long) {
        val dm: DisplayMetrics = resources.displayMetrics
        val path = Path().apply {
            moveTo(nx1 * dm.widthPixels, ny1 * dm.heightPixels)
            lineTo(nx2 * dm.widthPixels, ny2 * dm.heightPixels)
        }
        val stroke = GestureDescription.StrokeDescription(path, 0, durationMs.coerceAtLeast(50))
        dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
    }
}
