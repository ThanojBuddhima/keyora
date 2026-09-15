package com.keyora.keyboard.theme

import androidx.compose.ui.graphics.Color

data class KeyboardColors(
    val background: Color,
    val keyBackground: Color,
    val specialKeyBackground: Color,
    val keyText: Color,
    val specialKeyText: Color,
    val suggestionBackground: Color,
    val suggestionText: Color,
    val pressedOverlay: Color,
    val keyShadow: Color,
    val returnKeyBackground: Color,
    val returnKeyText: Color
) {
    companion object {
        val Light = KeyboardColors(
            background = Color(0xFFD1D5DB),
            keyBackground = Color(0xFFFFFFFF),
            specialKeyBackground = Color(0xFFAEB3BE),
            keyText = Color(0xFF1C1C1E),
            specialKeyText = Color(0xFF1C1C1E),
            suggestionBackground = Color(0xFFE5E7EB),
            suggestionText = Color(0xFF1C1C1E),
            pressedOverlay = Color(0x33000000),
            keyShadow = Color(0x33000000),
            returnKeyBackground = Color(0xFF007AFF),
            returnKeyText = Color(0xFFFFFFFF)
        )

        val Dark = KeyboardColors(
            background = Color(0xFF1C1C1E),
            keyBackground = Color(0xFF2C2C2E),
            specialKeyBackground = Color(0xFF3A3A3C),
            keyText = Color(0xFFF2F2F7),
            specialKeyText = Color(0xFFF2F2F7),
            suggestionBackground = Color(0xFF2C2C2E),
            suggestionText = Color(0xFFF2F2F7),
            pressedOverlay = Color(0x55FFFFFF),
            keyShadow = Color(0x66000000),
            returnKeyBackground = Color(0xFF0A84FF),
            returnKeyText = Color(0xFFFFFFFF)
        )

        fun forTheme(theme: ResolvedTheme): KeyboardColors =
            when (theme) {
                ResolvedTheme.LIGHT -> Light
                ResolvedTheme.DARK -> Dark
            }
    }
}
