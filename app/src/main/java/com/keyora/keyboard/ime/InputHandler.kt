package com.keyora.keyboard.ime

import android.os.Build
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.text.InputType

/**
 * Handles InputConnection operations. Safe when connection is null.
 */
class InputHandler {

    fun commitText(connection: InputConnection?, text: String) {
        if (connection == null || text.isEmpty()) return
        connection.beginBatchEdit()
        try {
            connection.finishComposingText()
            connection.commitText(text, 1)
        } finally {
            connection.endBatchEdit()
        }
    }

    fun backspace(connection: InputConnection?) {
        if (connection == null) return
        connection.beginBatchEdit()
        try {
            connection.finishComposingText()
            val selected = connection.getSelectedText(0)
            if (!selected.isNullOrEmpty()) {
                connection.commitText("", 1)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connection.deleteSurroundingTextInCodePoints(1, 0)
            } else {
                // Delete a full UTF-16 code unit pair when needed for emoji.
                val before = connection.getTextBeforeCursor(2, 0)?.toString().orEmpty()
                val deleteCount = when {
                    before.length >= 2 &&
                        Character.isSurrogatePair(before[before.length - 2], before[before.length - 1]) -> 2
                    before.isNotEmpty() -> 1
                    else -> 1
                }
                connection.deleteSurroundingText(deleteCount, 0)
            }
        } finally {
            connection.endBatchEdit()
        }
    }

    fun enter(connection: InputConnection?, editorInfo: EditorInfo?) {
        if (connection == null) return
        // Insert a real newline — never KEYCODE_ENTER (that often sends/clears chat fields).
        commitText(connection, "\n")
    }

    fun space(connection: InputConnection?) {
        commitText(connection, " ")
    }

    fun isPasswordField(editorInfo: EditorInfo?): Boolean {
        if (editorInfo == null) return false
        val variation = editorInfo.inputType and InputType.TYPE_MASK_VARIATION
        val classType = editorInfo.inputType and InputType.TYPE_MASK_CLASS
        if (classType != InputType.TYPE_CLASS_TEXT) return false
        return variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
    }

    fun preferredLayout(editorInfo: EditorInfo?): KeyboardLayout {
        if (editorInfo == null) return KeyboardLayout.LETTERS
        return when (editorInfo.inputType and InputType.TYPE_MASK_CLASS) {
            InputType.TYPE_CLASS_NUMBER,
            InputType.TYPE_CLASS_PHONE,
            InputType.TYPE_CLASS_DATETIME -> KeyboardLayout.NUMBERS
            else -> KeyboardLayout.LETTERS
        }
    }

    fun enterLabel(editorInfo: EditorInfo?): String {
        return when (editorInfo?.imeOptions?.and(EditorInfo.IME_MASK_ACTION)) {
            EditorInfo.IME_ACTION_GO -> "go"
            EditorInfo.IME_ACTION_SEARCH -> "search"
            EditorInfo.IME_ACTION_SEND -> "send"
            EditorInfo.IME_ACTION_NEXT -> "next"
            EditorInfo.IME_ACTION_DONE -> "done"
            else -> "return"
        }
    }
}
