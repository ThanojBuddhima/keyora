package com.keyora.keyboard.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeRuleManagerTest {

    private val manager = ThemeRuleManager()
    private val base = ThemeSettings(
        globalTheme = KeyboardThemeMode.SYSTEM_DEFAULT,
        appRules = mapOf(
            "com.whatsapp" to KeyboardThemeMode.DARK,
            "com.android.chrome" to KeyboardThemeMode.LIGHT
        )
    )

    @Test
    fun noRule_usesGlobalThenSystem() {
        val result = manager.getThemeForPackage(
            packageName = "com.test",
            settings = base,
            systemTheme = ResolvedTheme.LIGHT
        )
        assertEquals(ResolvedTheme.LIGHT, result)
    }

    @Test
    fun whatsapp_isDark() {
        val result = manager.getThemeForPackage(
            packageName = "com.whatsapp",
            settings = base,
            systemTheme = ResolvedTheme.LIGHT
        )
        assertEquals(ResolvedTheme.DARK, result)
    }

    @Test
    fun chrome_isLight() {
        val result = manager.getThemeForPackage(
            packageName = "com.android.chrome",
            settings = base,
            systemTheme = ResolvedTheme.DARK
        )
        assertEquals(ResolvedTheme.LIGHT, result)
    }

    @Test
    fun removeRule_returnsGlobalTheme() {
        val updated = manager.removeThemeForPackage(base, "com.whatsapp")
        val result = manager.getThemeForPackage(
            packageName = "com.whatsapp",
            settings = updated,
            systemTheme = ResolvedTheme.LIGHT
        )
        assertEquals(ResolvedTheme.LIGHT, result)
    }

    @Test
    fun setThemeForPackage_addsRule() {
        val updated = manager.setThemeForPackage(
            base,
            "org.telegram.messenger",
            KeyboardThemeMode.DARK
        )
        assertEquals(KeyboardThemeMode.DARK, updated.appRules["org.telegram.messenger"])
        assertEquals(
            ResolvedTheme.DARK,
            manager.getThemeForPackage("org.telegram.messenger", updated, ResolvedTheme.LIGHT)
        )
    }

    @Test
    fun globalDark_appliesWhenRuleIsSystemDefault() {
        val settings = ThemeSettings(
            globalTheme = KeyboardThemeMode.DARK,
            appRules = mapOf("com.gmail" to KeyboardThemeMode.SYSTEM_DEFAULT)
        )
        val result = manager.getThemeForPackage("com.gmail", settings, ResolvedTheme.LIGHT)
        assertEquals(ResolvedTheme.DARK, result)
    }

    @Test
    fun nullPackage_fallsBackToGlobal() {
        val settings = ThemeSettings(globalTheme = KeyboardThemeMode.LIGHT)
        val result = manager.getThemeForPackage(null, settings, ResolvedTheme.DARK)
        assertEquals(ResolvedTheme.LIGHT, result)
    }

    @Test
    fun getAllRules_sorted() {
        val rules = manager.getAllRules(base)
        assertEquals(2, rules.size)
        assertTrue(rules[0].packageName <= rules[1].packageName)
    }
}
