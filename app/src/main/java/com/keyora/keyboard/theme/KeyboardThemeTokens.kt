package com.keyora.keyboard.theme

import androidx.annotation.ColorInt

/**
 * Android ColorInt tokens for the View-based IME (no Compose dependency).
 */
data class KeyboardThemeTokens(
    @ColorInt val background: Int,
    @ColorInt val keyBackground: Int,
    @ColorInt val specialKeyBackground: Int,
    @ColorInt val keyText: Int,
    @ColorInt val specialKeyText: Int,
    @ColorInt val returnKeyBackground: Int,
    @ColorInt val returnKeyText: Int
) {
    companion object {
        val Light = KeyboardThemeTokens(
            background = 0xFFD1D5DB.toInt(),
            keyBackground = 0xFFFFFFFF.toInt(),
            specialKeyBackground = 0xFFAEB3BE.toInt(),
            keyText = 0xFF1C1C1E.toInt(),
            specialKeyText = 0xFF1C1C1E.toInt(),
            returnKeyBackground = 0xFF007AFF.toInt(),
            returnKeyText = 0xFFFFFFFF.toInt()
        )

        val Dark = KeyboardThemeTokens(
            background = 0xFF1C1C1E.toInt(),
            keyBackground = 0xFF2C2C2E.toInt(),
            specialKeyBackground = 0xFF3A3A3C.toInt(),
            keyText = 0xFFF2F2F7.toInt(),
            specialKeyText = 0xFFF2F2F7.toInt(),
            returnKeyBackground = 0xFF0A84FF.toInt(),
            returnKeyText = 0xFFFFFFFF.toInt()
        )

        fun forTheme(theme: ResolvedTheme): KeyboardThemeTokens =
            when (theme) {
                ResolvedTheme.LIGHT -> Light
                ResolvedTheme.DARK -> Dark
            }
    }
}
