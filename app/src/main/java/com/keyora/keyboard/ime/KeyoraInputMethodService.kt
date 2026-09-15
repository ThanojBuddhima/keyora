package com.keyora.keyboard.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.keyora.keyboard.KeyoraApp
import com.keyora.keyboard.appdetector.AppContextDetector
import com.keyora.keyboard.enablement.ImeEnablement
import com.keyora.keyboard.ime.ui.KeyboardView
import com.keyora.keyboard.suggestions.PlaceholderSuggestionEngine
import com.keyora.keyboard.theme.ThemeManager
import com.keyora.keyboard.theme.ThemeSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class KeyoraInputMethodService : InputMethodService() {

    private val composeHost = ImeComposeHost()
    private val controller = KeyboardController()
    private val appDetector = AppContextDetector()
    private val suggestionEngine = PlaceholderSuggestionEngine()

    private lateinit var themeManager: ThemeManager
    private var serviceScope: CoroutineScope? = null
    private var composeView: ComposeView? = null
    private val typedBuffer = MutableStateFlow("")

    override fun onCreate() {
        super.onCreate()
        composeHost.onCreate()
        themeManager = ThemeManager(this)
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

        controller.bind(
            connectionProvider = { currentInputConnection },
            editorInfoProvider = { currentInputEditorInfo },
            onShowImePicker = { ImeEnablement.showInputMethodPicker(this) },
            onTyped = { event ->
                when (event) {
                    is KeyboardController.TypedEvent.Character ->
                        updateSuggestionBuffer(event.text)
                    KeyboardController.TypedEvent.Backspace ->
                        updateSuggestionBuffer(null, isBackspace = true)
                    KeyboardController.TypedEvent.SpaceOrEnter ->
                        updateSuggestionBuffer(" ")
                }
            }
        )

        val repo = (application as KeyoraApp).settingsRepository
        serviceScope?.launch {
            repo.themeSettings.collectLatest { settings: ThemeSettings ->
                themeManager.updateSettings(settings)
            }
        }
    }

    override fun onCreateInputView(): View {
        val view = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setViewTreeLifecycleOwner(composeHost)
            setViewTreeViewModelStoreOwner(composeHost)
            setViewTreeSavedStateRegistryOwner(composeHost)
            setContent {
                val keyboardState by controller.state.collectAsState()
                val theme by themeManager.resolvedTheme.collectAsState()
                val buffer by typedBuffer.collectAsState()
                val suggestions = suggestionEngine.suggestionsFor(
                    prefix = buffer,
                    enabled = keyboardState.suggestionMode
                )
                KeyboardView(
                    state = keyboardState,
                    theme = theme,
                    suggestions = suggestions,
                    controller = controller
                )
            }
        }
        composeView = view
        composeHost.onResume()
        return view
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        val packageName = appDetector.getCurrentPackageName(attribute)
        themeManager.onInputContextChanged(packageName)
        controller.onStartInput(packageName, attribute)
        typedBuffer.value = ""
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        composeHost.onResume()
        val packageName = appDetector.getCurrentPackageName(info ?: currentInputEditorInfo)
        themeManager.onInputContextChanged(packageName)
        controller.onStartInput(packageName, info ?: currentInputEditorInfo)
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        if (composeHost.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            composeHost.onPause()
        }
    }

    override fun onDestroy() {
        composeHost.onDestroy()
        serviceScope?.cancel()
        serviceScope = null
        composeView = null
        super.onDestroy()
    }

    /**
     * Lightweight buffer for placeholder suggestions only.
     * Never used for password fields (suggestionMode is false).
     */
    fun updateSuggestionBuffer(char: String?, isBackspace: Boolean = false) {
        val state = controller.state.value
        if (!state.suggestionMode) {
            typedBuffer.value = ""
            return
        }
        typedBuffer.value = when {
            isBackspace -> typedBuffer.value.dropLast(1)
            char == " " || char == "\n" -> ""
            char != null -> typedBuffer.value + char.lowercase()
            else -> typedBuffer.value
        }
    }
}
