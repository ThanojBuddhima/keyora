package com.keyora.keyboard.ime

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import com.keyora.keyboard.KeyoraApp
import com.keyora.keyboard.appdetector.AppContextDetector
import com.keyora.keyboard.clipboard.ClipboardRepository
import com.keyora.keyboard.enablement.ImeEnablement
import com.keyora.keyboard.ime.ui.KeyboardRootView
import com.keyora.keyboard.settings.KeyboardHeightLevel
import com.keyora.keyboard.settings.SettingsRepository
import com.keyora.keyboard.suggestions.PlaceholderSuggestionEngine
import com.keyora.keyboard.suggestions.SuggestionEngine
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
    private val suggestionEngine: SuggestionEngine = PlaceholderSuggestionEngine()

    private var themeManager: ThemeManager? = null
    private var serviceScope: CoroutineScope? = null
    private var rootView: KeyboardRootView? = null
    private var clipboardRepository: ClipboardRepository? = null
    private var settingsRepository: SettingsRepository? = null
    private var heightLevel: KeyboardHeightLevel = KeyboardHeightLevel.MEDIUM
    private val wordBuffer = StringBuilder()

    override fun onCreate() {
        try {
            super.onCreate()
            themeManager = ThemeManager(this)
            serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            settingsRepository = settingsRepositoryOrNull()
            clipboardRepository = ClipboardRepository(applicationContext)

            controller.bind(
                connectionProvider = { currentInputConnection },
                editorInfoProvider = { currentInputEditorInfo },
                onShowImePicker = {
                    try {
                        ImeEnablement.showInputMethodPicker(this)
                    } catch (t: Throwable) {
                        Log.e(TAG, "Failed to show IME picker", t)
                    }
                },
                onTyped = { event ->
                    when (event) {
                        is KeyboardController.TypedEvent.Character -> {
                            if (event.text.any { it.isLetterOrDigit() || it == '\'' }) {
                                wordBuffer.append(event.text)
                            } else {
                                wordBuffer.clear()
                            }
                        }
                        KeyboardController.TypedEvent.Backspace -> {
                            if (wordBuffer.isNotEmpty()) {
                                wordBuffer.deleteCharAt(wordBuffer.lastIndex)
                            }
                        }
                        KeyboardController.TypedEvent.SpaceOrEnter -> wordBuffer.clear()
                    }
                    refreshKeyboardUi()
                }
            )

            val repo = settingsRepository
            if (repo != null) {
                serviceScope?.launch {
                    repo.themeSettings.collectLatest { settings: ThemeSettings ->
                        themeManager?.updateSettings(settings)
                        refreshKeyboardUi()
                    }
                }
                serviceScope?.launch {
                    repo.keyboardHeightLevel.collectLatest { level ->
                        heightLevel = level
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
            val clipboard = clipboardRepository ?: ClipboardRepository(applicationContext).also {
                clipboardRepository = it
            }
            val root = KeyboardRootView(this)
            root.bind(
                controller = controller,
                clipboardRepository = clipboard,
                onCycleHeight = {
                    val next = heightLevel.next()
                    heightLevel = next
                    serviceScope?.launch {
                        settingsRepository?.setKeyboardHeightLevel(next)
                    }
                    refreshKeyboardUi()
                }
            )
            rootView = root
            refreshKeyboardUi()
            root
        } catch (t: Throwable) {
            Log.e(TAG, "onCreateInputView failed", t)
            View(this).apply {
                setBackgroundColor(0xD9C5CCD4.toInt())
                minimumHeight = (220 * resources.displayMetrics.density).toInt()
            }
        }
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

    override fun onWindowShown() {
        super.onWindowShown()
        applyGlassWindow()
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        try {
            super.onStartInput(attribute, restarting)
            wordBuffer.clear()
            val packageName = appDetector.getCurrentPackageName(attribute)
            themeManager?.onInputContextChanged(packageName)
            controller.onStartInput(packageName, attribute)
            clipboardRepository?.captureEnabled = !controller.state.value.isPasswordField
            refreshKeyboardUi()
        } catch (t: Throwable) {
            Log.e(TAG, "onStartInput failed", t)
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        try {
            super.onStartInputView(info, restarting)
            applyGlassWindow()
            clipboardRepository?.start()
            val packageName = appDetector.getCurrentPackageName(info ?: currentInputEditorInfo)
            themeManager?.onInputContextChanged(packageName)
            controller.onStartInput(packageName, info ?: currentInputEditorInfo)
            rootView?.let { setInputView(it) }
            refreshKeyboardUi()
        } catch (t: Throwable) {
            Log.e(TAG, "onStartInputView failed", t)
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        clipboardRepository?.stop()
        super.onFinishInputView(finishingInput)
    }

    override fun onDestroy() {
        try {
            clipboardRepository?.stop()
            serviceScope?.cancel()
            serviceScope = null
            rootView = null
            themeManager = null
            clipboardRepository = null
            settingsRepository = null
        } catch (t: Throwable) {
            Log.e(TAG, "onDestroy cleanup failed", t)
        }
        super.onDestroy()
    }

    private fun applyGlassWindow() {
        val w = window?.window ?: return
        try {
            w.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            w.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            val attrs = w.attributes
            attrs.dimAmount = 0f
            w.attributes = attrs
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                w.setBackgroundBlurRadius(BLUR_RADIUS_PX)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Glass window setup failed", t)
        }
    }

    private fun refreshKeyboardUi() {
        val root = rootView ?: return
        val theme = themeManager?.resolvedTheme?.value ?: return
        val state = controller.state.value
        val suggestions = suggestionEngine.suggestionsFor(
            prefix = wordBuffer.toString(),
            enabled = state.suggestionMode && !state.isPasswordField
        )
        root.update(
            theme = theme,
            heightLevel = heightLevel,
            passwordField = state.isPasswordField,
            suggestions = suggestions
        )
        root.renderKeyboardState(state)
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
        private const val BLUR_RADIUS_PX = 48
    }
}
