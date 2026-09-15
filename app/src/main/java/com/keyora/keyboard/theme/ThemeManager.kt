package com.keyora.keyboard.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ThemeManager(
    context: Context,
    private val ruleManager: ThemeRuleManager = ThemeRuleManager()
) {
    private val systemThemeDetector = SystemThemeDetector(context.applicationContext)
    private val _resolvedTheme = MutableStateFlow(systemThemeDetector.detect())
    val resolvedTheme: StateFlow<ResolvedTheme> = _resolvedTheme.asStateFlow()

    private var settings: ThemeSettings = ThemeSettings()
    private var currentPackageName: String? = null

    fun updateSettings(newSettings: ThemeSettings) {
        settings = newSettings
        recompute()
    }

    fun onInputContextChanged(packageName: String?) {
        currentPackageName = packageName
        recompute()
    }

    fun currentPackage(): String? = currentPackageName

    fun currentSettings(): ThemeSettings = settings

    fun colors(): KeyboardColors = KeyboardColors.forTheme(_resolvedTheme.value)

    private fun recompute() {
        _resolvedTheme.value = ruleManager.getThemeForPackage(
            packageName = currentPackageName,
            settings = settings,
            systemTheme = systemThemeDetector.detect()
        )
    }
}
