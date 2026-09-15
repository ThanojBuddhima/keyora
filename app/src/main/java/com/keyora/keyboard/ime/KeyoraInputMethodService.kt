package com.keyora.keyboard.ime

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.FrameLayout
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
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
    private var inputContainer: FrameLayout? = null
    private var composeView: ComposeView? = null
    private val typedBuffer = MutableStateFlow("")

    override fun onCreate() {
        super.onCreate()
        composeHost.onCreate()
        composeHost.onResume()
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
        val density = resources.displayMetrics.density
        val minHeightPx = (260 * density).toInt()

        val container = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            minimumHeight = minHeightPx
        }

        val view = ComposeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            // Do NOT dispose on detach — IME views attach/detach often; disposing
            // leaves a blank keyboard the next time the field is focused.
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnLifecycleDestroyed(composeHost)
            )
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

        container.addView(view)
        inputContainer = container
        composeView = view
        composeHost.onResume()
        return container
    }

    override fun onEvaluateFullscreenMode(): Boolean = false

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
        // Ensure the input view is bound after show/hide cycles.
        inputContainer?.let { setInputView(it) }
        val packageName = appDetector.getCurrentPackageName(info ?: currentInputEditorInfo)
        themeManager.onInputContextChanged(packageName)
        controller.onStartInput(packageName, info ?: currentInputEditorInfo)
    }

    override fun onWindowShown() {
        super.onWindowShown()
        composeHost.onResume()
    }

    override fun onWindowHidden() {
        super.onWindowHidden()
        composeHost.onPause()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        composeHost.onPause()
    }

    override fun onDestroy() {
        composeHost.onDestroy()
        serviceScope?.cancel()
        serviceScope = null
        composeView = null
        inputContainer = null
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
