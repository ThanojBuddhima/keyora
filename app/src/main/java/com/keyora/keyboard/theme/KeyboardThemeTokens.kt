package com.keyora.keyboard.theme

import androidx.annotation.ColorInt

/**
 * iOS-inspired translucent glass tokens for the View IME.
 * Alpha channels enable frosted look with window blur (API 31+) or fallback translucency.
 */
data class KeyboardThemeTokens(
    @ColorInt val background: Int,
    @ColorInt val keyBackground: Int,
    @ColorInt val specialKeyBackground: Int,
    @ColorInt val keyText: Int,
    @ColorInt val specialKeyText: Int,
    @ColorInt val returnKeyBackground: Int,
    @ColorInt val returnKeyText: Int,
    @ColorInt val suggestionText: Int,
    @ColorInt val suggestionDivider: Int,
    @ColorInt val dockIcon: Int,
    @ColorInt val keyShadow: Int
) {
    companion object {
        val Light = KeyboardThemeTokens(
            background = 0xD9C5CCD4.toInt(),
            keyBackground = 0xFFFFFFFF.toInt(),
            specialKeyBackground = 0xB3AEB4BC.toInt(),
            keyText = 0xFF000000.toInt(),
            specialKeyText = 0xFF000000.toInt(),
            returnKeyBackground = 0xB3AEB4BC.toInt(),
            returnKeyText = 0xFF000000.toInt(),
            suggestionText = 0xFF3C3C43.toInt(),
            suggestionDivider = 0x403C3C43.toInt(),
            dockIcon = 0xFF000000.toInt(),
            keyShadow = 0x33000000.toInt()
        )

        val Dark = KeyboardThemeTokens(
            background = 0xB31C1C1E.toInt(),
            keyBackground = 0x47FFFFFF.toInt(),
            specialKeyBackground = 0x66000000.toInt(),
            keyText = 0xFFFFFFFF.toInt(),
            specialKeyText = 0xFFFFFFFF.toInt(),
            returnKeyBackground = 0x66000000.toInt(),
            returnKeyText = 0xFFFFFFFF.toInt(),
            suggestionText = 0xFFE5E5EA.toInt(),
            suggestionDivider = 0x40E5E5EA.toInt(),
            dockIcon = 0xFFFFFFFF.toInt(),
            keyShadow = 0x66000000.toInt()
        )

        fun forTheme(theme: ResolvedTheme): KeyboardThemeTokens =
            when (theme) {
                ResolvedTheme.LIGHT -> Light
                ResolvedTheme.DARK -> Dark
            }
    }
}
