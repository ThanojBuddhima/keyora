package com.keyora.keyboard.theme

enum class KeyboardThemeMode {
    LIGHT,
    DARK,
    SYSTEM_DEFAULT
}

enum class ResolvedTheme {
    LIGHT,
    DARK
}

data class AppThemeRule(
    val packageName: String,
    val theme: KeyboardThemeMode
)

data class ThemeSettings(
    val globalTheme: KeyboardThemeMode = KeyboardThemeMode.SYSTEM_DEFAULT,
    val appRules: Map<String, KeyboardThemeMode> = emptyMap()
)
