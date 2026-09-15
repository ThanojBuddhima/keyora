package com.keyora.keyboard.ime

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import com.keyora.keyboard.KeyoraApp
import com.keyora.keyboard.appdetector.AppContextDetector
import com.keyora.keyboard.enablement.ImeEnablement
import com.keyora.keyboard.ime.ui.KeyboardLayoutView
import com.keyora.keyboard.settings.SettingsRepository
import com.keyora.keyboard.theme.ThemeManager
import com.keyora.keyboard.theme.ThemeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KeyoraInputMethodService : InputMethodService() {

    private val controller = KeyboardController()
    private val appDetector = AppContextDetector()

    private var themeManager: ThemeManager? = null
    private var serviceScope: CoroutineScope? = null
    private var keyboardView: KeyboardLayoutView? = null

    override fun onCreate() {
        try {
            super.onCreate()
            themeManager = ThemeManager(this)
            serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

            controller.bind(
                connectionProvider = { currentInputConnection },
                editorInfoProvider = { currentInputEditorInfo },
                onShowImePicker = {
                    try {
                        ImeEnablement.showInputMethodPicker(this)
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed to show IME picker", t)
                    }
                }
            )

            val repo = settingsRepositoryOrNull()
            if (repo != null) {
                serviceScope?.launch {
                    repo.themeSettings.collectLatest { settings: ThemeSettings ->
                        themeManager?.updateSettings(settings)
                        refreshKeyboardUi()
                    }
                }
            }

            serviceScope?.launch {
                controller.state.collectLatest {
                    refreshKeyboardUi()
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "onCreate failed", t)
        }
    }

    override fun onCreateInputView(): View {
        return try {
            val view = KeyboardLayoutView(this)
            view.bind(controller)
            themeManager?.let { view.applyTheme(it.resolvedTheme.value) }
            view.renderState(controller.state.value)
            keyboardView = view
            view
        } catch (t: Throwable) {
            Log.e(TAG, "onCreateInputView failed", t)
            // Absolute fallback so the IME still "starts" with a visible bar.
            View(this).apply {
                setBackgroundColor(0xFFD1D5DB.toInt())
                minimumHeight = (260 * resources.displayMetrics.density).toInt()
            }
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        try {
            super.onStartInput(attribute, restarting)
            val packageName = appDetector.getCurrentPackageName(attribute)
            themeManager?.onInputContextChanged(packageName)
            controller.onStartInput(packageName, attribute)
            refreshKeyboardUi()
        } catch (t: Throwable) {
            Log.e(TAG, "onStartInput failed", t)
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        try {
            super.onStartInputView(info, restarting)
            val packageName = appDetector.getCurrentPackageName(info ?: currentInputEditorInfo)
            themeManager?.onInputContextChanged(packageName)
            controller.onStartInput(packageName, info ?: currentInputEditorInfo)
            keyboardView?.let { setInputView(it) }
            refreshKeyboardUi()
        } catch (t: Throwable) {
            Log.e(TAG, "onStartInputView failed", t)
        }
    }

    override fun onDestroy() {
        try {
            serviceScope?.cancel()
            serviceScope = null
            keyboardView = null
            themeManager = null
        } catch (t: Throwable) {
            Log.e(TAG, "onDestroy cleanup failed", t)
        }
        super.onDestroy()
    }

    private fun refreshKeyboardUi() {
        val view = keyboardView ?: return
        val theme = themeManager?.resolvedTheme?.value ?: return
        view.update(theme, controller.state.value)
    }

    private fun settingsRepositoryOrNull(): SettingsRepository? {
        return try {
            (application as? KeyoraApp)?.settingsRepository
                ?: SettingsRepository(applicationContext)
        } catch (t: Throwable) {
            Log.e(TAG, "SettingsRepository unavailable", t)
            null
        }
    }

    companion object {
        private const val TAG = "KeyoraIME"
    }
}
