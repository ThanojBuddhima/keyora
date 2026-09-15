package com.keyora.keyboard.theme

import androidx.compose.ui.graphics.Color

/**
 * Compose color mirror of [KeyboardThemeTokens] (settings/preview only).
 */
data class KeyboardColors(
    val background: Color,
    val keyBackground: Color,
    val specialKeyBackground: Color,
    val keyText: Color,
    val specialKeyText: Color,
    val returnKeyBackground: Color,
    val returnKeyText: Color,
    val suggestionText: Color,
    val suggestionBackground: Color,
    val suggestionDivider: Color,
    val keyShadow: Color,
    val pressedOverlay: Color,
    val dockIcon: Color
) {
    companion object {
        val Light = KeyboardColors(
            background = Color(0xD9C5CCD4),
            keyBackground = Color(0xFFFFFFFF),
            specialKeyBackground = Color(0xB3AEB4BC),
            keyText = Color(0xFF000000),
            specialKeyText = Color(0xFF000000),
            returnKeyBackground = Color(0xB3AEB4BC),
            returnKeyText = Color(0xFF000000),
            suggestionText = Color(0xFF3C3C43),
            suggestionBackground = Color(0x00FFFFFF),
            suggestionDivider = Color(0x403C3C43),
            keyShadow = Color(0x33000000),
            pressedOverlay = Color(0x22000000),
            dockIcon = Color(0xFF000000)
        )

        val Dark = KeyboardColors(
            background = Color(0xB31C1C1E),
            keyBackground = Color(0x47FFFFFF),
            specialKeyBackground = Color(0x66000000),
            keyText = Color(0xFFFFFFFF),
            specialKeyText = Color(0xFFFFFFFF),
            returnKeyBackground = Color(0x66000000),
            returnKeyText = Color(0xFFFFFFFF),
            suggestionText = Color(0xFFE5E5EA),
            suggestionBackground = Color(0x00FFFFFF),
            suggestionDivider = Color(0x40E5E5EA),
            keyShadow = Color(0x66000000),
            pressedOverlay = Color(0x33FFFFFF),
            dockIcon = Color(0xFFFFFFFF)
        )

        fun forTheme(theme: ResolvedTheme): KeyboardColors =
            when (theme) {
                ResolvedTheme.LIGHT -> Light
                ResolvedTheme.DARK -> Dark
            }
    }
}
