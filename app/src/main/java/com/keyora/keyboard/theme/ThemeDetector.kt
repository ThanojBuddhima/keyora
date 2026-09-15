package com.keyora.keyboard.theme

import android.content.Context
import android.content.res.Configuration

/**
 * Abstraction for future automatic theme detection.
 * MVP uses rule-based / system detectors only — no screen scraping.
 */
fun interface ThemeDetector {
    fun detect(): ResolvedTheme?
}

class SystemThemeDetector(
    private val context: Context
) : ThemeDetector {
    override fun detect(): ResolvedTheme {
        val night = context.resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK
        return if (night == Configuration.UI_MODE_NIGHT_YES) {
            ResolvedTheme.DARK
        } else {
            ResolvedTheme.LIGHT
        }
    }
}

/**
 * Placeholder for a future implementation that could detect a host app's theme.
 * Not used in MVP — kept so the architecture remains extensible.
 */
class AppThemeDetector : ThemeDetector {
    override fun detect(): ResolvedTheme? = null
}

class ManualThemeDetector(
    private val mode: KeyboardThemeMode,
    private val systemThemeDetector: SystemThemeDetector
) : ThemeDetector {
    override fun detect(): ResolvedTheme? =
        when (mode) {
            KeyboardThemeMode.LIGHT -> ResolvedTheme.LIGHT
            KeyboardThemeMode.DARK -> ResolvedTheme.DARK
            KeyboardThemeMode.SYSTEM_DEFAULT -> systemThemeDetector.detect()
        }
}
