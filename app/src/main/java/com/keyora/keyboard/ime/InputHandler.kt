package com.keyora.keyboard.ime

import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.text.InputType

/**
 * Handles InputConnection operations. Safe when connection is null.
 */
class InputHandler {

    fun commitText(connection: InputConnection?, text: String) {
        connection?.commitText(text, 1)
    }

    fun backspace(connection: InputConnection?) {
        if (connection == null) return
        val selected = connection.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            connection.commitText("", 1)
        } else {
            connection.deleteSurroundingText(1, 0)
        }
    }

    fun enter(connection: InputConnection?, editorInfo: EditorInfo?) {
        if (connection == null) return
        // Always insert a newline (never Done/Search/Go editor actions).
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
        connection.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
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
