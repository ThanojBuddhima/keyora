package com.keyora.keyboard.ime

enum class KeyboardLayout {
    LETTERS,
    NUMBERS,
    SYMBOLS
}

enum class ShiftMode {
    OFF,
    AUTO,
    CAPS
}

data class KeyboardState(
    val shiftMode: ShiftMode = ShiftMode.OFF,
    val autoShiftActive: Boolean = false,
    val currentLayout: KeyboardLayout = KeyboardLayout.LETTERS,
    val currentPackageName: String? = null,
    val language: String = "en",
    val suggestionMode: Boolean = true,
    val isPasswordField: Boolean = false,
    val enterLabel: String = "return"
) {
    val lettersUppercase: Boolean
        get() = when (shiftMode) {
            ShiftMode.OFF -> false
            ShiftMode.CAPS -> true
            ShiftMode.AUTO -> autoShiftActive
        }

    val shiftHighlighted: Boolean
        get() = shiftMode == ShiftMode.AUTO || shiftMode == ShiftMode.CAPS

    val shiftStrongHighlight: Boolean
        get() = shiftMode == ShiftMode.CAPS
}
