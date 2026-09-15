package com.keyora.keyboard.ime

enum class KeyboardLayout {
    LETTERS,
    NUMBERS,
    SYMBOLS
}

data class KeyboardState(
    val shiftEnabled: Boolean = false,
    val capsLock: Boolean = false,
    val currentLayout: KeyboardLayout = KeyboardLayout.LETTERS,
    val currentPackageName: String? = null,
    val language: String = "en",
    val suggestionMode: Boolean = true,
    val isPasswordField: Boolean = false,
    val enterLabel: String = "return"
) {
    val lettersUppercase: Boolean
        get() = capsLock || shiftEnabled
}
