package com.keyora.keyboard.theme

/**
 * Resolves keyboard theme for a package given global settings and per-app rules.
 *
 * Priority:
 * 1. Explicit per-app LIGHT / DARK
 * 2. Per-app SYSTEM_DEFAULT or missing rule → global theme
 * 3. Global SYSTEM_DEFAULT → system night mode
 */
class ThemeRuleManager {

    fun getThemeForPackage(
        packageName: String?,
        settings: ThemeSettings,
        systemTheme: ResolvedTheme
    ): ResolvedTheme {
        val rule = packageName?.let { settings.appRules[it] }
        val effectiveMode = when (rule) {
            KeyboardThemeMode.LIGHT, KeyboardThemeMode.DARK -> rule
            KeyboardThemeMode.SYSTEM_DEFAULT, null -> settings.globalTheme
        }
        return resolveMode(effectiveMode, systemTheme)
    }

    fun setThemeForPackage(
        settings: ThemeSettings,
        packageName: String,
        theme: KeyboardThemeMode
    ): ThemeSettings {
        val updated = settings.appRules.toMutableMap()
        updated[packageName] = theme
        return settings.copy(appRules = updated)
    }

    fun removeThemeForPackage(
        settings: ThemeSettings,
        packageName: String
    ): ThemeSettings {
        val updated = settings.appRules.toMutableMap()
        updated.remove(packageName)
        return settings.copy(appRules = updated)
    }

    fun getAllRules(settings: ThemeSettings): List<AppThemeRule> =
        settings.appRules
            .map { (pkg, theme) -> AppThemeRule(pkg, theme) }
            .sortedBy { it.packageName }

    private fun resolveMode(
        mode: KeyboardThemeMode,
        systemTheme: ResolvedTheme
    ): ResolvedTheme =
        when (mode) {
            KeyboardThemeMode.LIGHT -> ResolvedTheme.LIGHT
            KeyboardThemeMode.DARK -> ResolvedTheme.DARK
            KeyboardThemeMode.SYSTEM_DEFAULT -> systemTheme
        }
}
