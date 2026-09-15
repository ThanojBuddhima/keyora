package com.keyora.keyboard.appdetector

import android.view.inputmethod.EditorInfo

/**
 * Detects the package currently requesting text input via EditorInfo.
 * Does not use AccessibilityService.
 */
class AppContextDetector {
    fun getCurrentPackageName(editorInfo: EditorInfo?): String? {
        val packageName = editorInfo?.packageName?.trim().orEmpty()
        return packageName.ifEmpty { null }
    }
}
