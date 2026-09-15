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
import com.keyora.keyboard.ime.emoji.RecentEmojiStore
import com.keyora.keyboard.ime.ui.KeyboardRootView
import com.keyora.keyboard.settings.KeyboardHeightLevel
import com.keyora.keyboard.settings.SettingsRepository
import com.keyora.keyboard.theme.KeyboardThemeMode
import com.keyora.keyboard.theme.ResolvedTheme
import com.keyora.keyboard.theme.ThemeManager
import com.keyora.keyboard.theme.ThemeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class KeyoraInputMethodService : InputMethodService() {

    private val controller = KeyboardController()
    private val appDetector = AppContextDetector()

    private var themeManager: ThemeManager? = null
    private var serviceScope: CoroutineScope? = null
    private var rootView: KeyboardRootView? = null
    private var clipboardRepository: ClipboardRepository? = null
    private var recentEmojiStore: RecentEmojiStore? = null
    private var settingsRepository: SettingsRepository? = null
    private var heightLevel: KeyboardHeightLevel = KeyboardHeightLevel.MEDIUM

    override fun onCreate() {
        try {
            super.onCreate()
            themeManager = ThemeManager(this)
            serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
            settingsRepository = settingsRepositoryOrNull()
            clipboardRepository = ClipboardRepository(applicationContext)
            recentEmojiStore = RecentEmojiStore(applicationContext)

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

            val repo = settingsRepository
            if (repo != null) {
                serviceScope?.launch {
                    repo.themeSettings.collectLatest { settings: ThemeSettings ->
                        themeManager?.updateSettings(settings)
                        refreshChrome()
                    }
                }
                serviceScope?.launch {
                    repo.keyboardHeightLevel.collectLatest { level ->
                        heightLevel = level
                        refreshChrome()
                    }
                }
            }

            serviceScope?.launch {
                controller.state
                    .map { state ->
                        KeyboardVisualSnapshot(
                            currentLayout = state.currentLayout,
                            shiftMode = state.shiftMode,
                            autoShiftActive = state.autoShiftActive,
                            enterLabel = state.enterLabel,
                            isPasswordField = state.isPasswordField
                        )
                    }
                    .distinctUntilChanged()
                    .collectLatest { snapshot ->
                        rootView?.renderKeyboardState(controller.state.value)
                        if (snapshot.isPasswordField) {
                            refreshChrome()
                        }
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
            val recent = recentEmojiStore ?: RecentEmojiStore(applicationContext).also {
                recentEmojiStore = it
            }
            val root = KeyboardRootView(this)
            root.bind(
                controller = controller,
                clipboardRepository = clipboard,
                recentEmojiStore = recent,
                onCycleHeight = {
                    val next = heightLevel.next()
                    heightLevel = next
                    serviceScope?.launch {
                        settingsRepository?.setKeyboardHeightLevel(next)
                    }
                    refreshChrome()
                },
                onCycleTheme = { cycleThemeForCurrentApp() }
            )
            rootView = root
            refreshAll()
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
            val packageName = appDetector.getCurrentPackageName(attribute)
            themeManager?.onInputContextChanged(packageName)
            controller.onStartInput(packageName, attribute)
            clipboardRepository?.captureEnabled = !controller.state.value.isPasswordField
            refreshAll()
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
            refreshAll()
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
            recentEmojiStore = null
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

    private fun cycleThemeForCurrentApp() {
        val mgr = themeManager ?: return
        val pkg = mgr.currentPackage()
        val nextMode = when (mgr.resolvedTheme.value) {
            ResolvedTheme.LIGHT -> KeyboardThemeMode.DARK
            ResolvedTheme.DARK -> KeyboardThemeMode.LIGHT
        }
        serviceScope?.launch {
            val repo = settingsRepository ?: return@launch
            if (pkg.isNullOrBlank()) {
                repo.setGlobalTheme(nextMode)
            } else {
                repo.setThemeForPackage(pkg, nextMode)
            }
        }
    }

    private fun refreshAll() {
        refreshChrome()
        rootView?.renderKeyboardState(controller.state.value)
    }

    private fun refreshChrome() {
        val root = rootView ?: return
        val theme = themeManager?.resolvedTheme?.value ?: ResolvedTheme.LIGHT
        val state = controller.state.value
        root.updateChrome(
            theme = theme,
            heightLevel = heightLevel,
            passwordField = state.isPasswordField
        )
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

    private data class KeyboardVisualSnapshot(
        val currentLayout: KeyboardLayout,
        val shiftMode: ShiftMode,
        val autoShiftActive: Boolean,
        val enterLabel: String,
        val isPasswordField: Boolean
    )

    companion object {
        private const val TAG = "KeyoraIME"
        private const val BLUR_RADIUS_PX = 48
    }
}
