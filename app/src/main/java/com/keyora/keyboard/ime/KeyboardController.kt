package com.keyora.keyboard.ime

import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class KeyboardController(
    private val inputHandler: InputHandler = InputHandler()
) {
    private val _state = MutableStateFlow(KeyboardState())
    val state: StateFlow<KeyboardState> = _state.asStateFlow()

    private var connectionProvider: (() -> InputConnection?)? = null
    private var editorInfoProvider: (() -> EditorInfo?)? = null
    private var onShowImePicker: (() -> Unit)? = null
    private var onTyped: ((event: TypedEvent) -> Unit)? = null
    private var lastShiftTapMs: Long = 0L

    sealed class TypedEvent {
        data class Character(val text: String) : TypedEvent()
        data object Backspace : TypedEvent()
        data object SpaceOrEnter : TypedEvent()
    }

    fun bind(
        connectionProvider: () -> InputConnection?,
        editorInfoProvider: () -> EditorInfo?,
        onShowImePicker: () -> Unit,
        onTyped: ((TypedEvent) -> Unit)? = null
    ) {
        this.connectionProvider = connectionProvider
        this.editorInfoProvider = editorInfoProvider
        this.onShowImePicker = onShowImePicker
        this.onTyped = onTyped
    }

    fun onStartInput(packageName: String?, editorInfo: EditorInfo?) {
        val password = inputHandler.isPasswordField(editorInfo)
        _state.update {
            it.copy(
                currentPackageName = packageName,
                isPasswordField = password,
                suggestionMode = !password,
                currentLayout = inputHandler.preferredLayout(editorInfo),
                enterLabel = inputHandler.enterLabel(editorInfo),
                shiftEnabled = false,
                capsLock = false
            )
        }
    }

    fun onCharacter(char: String) {
        val current = _state.value
        val text = if (current.currentLayout == KeyboardLayout.LETTERS && current.lettersUppercase) {
            char.uppercase()
        } else {
            char
        }
        inputHandler.commitText(connectionProvider?.invoke(), text)
        onTyped?.invoke(TypedEvent.Character(text))
        if (current.shiftEnabled && !current.capsLock) {
            _state.update { it.copy(shiftEnabled = false) }
        }
    }

    fun onBackspace() {
        inputHandler.backspace(connectionProvider?.invoke())
        onTyped?.invoke(TypedEvent.Backspace)
    }

    fun onEnter() {
        inputHandler.enter(connectionProvider?.invoke(), editorInfoProvider?.invoke())
        onTyped?.invoke(TypedEvent.SpaceOrEnter)
    }

    fun onSpace() {
        inputHandler.space(connectionProvider?.invoke())
        onTyped?.invoke(TypedEvent.SpaceOrEnter)
    }

    fun onShift() {
        val now = System.currentTimeMillis()
        val doubleTap = now - lastShiftTapMs < 350
        lastShiftTapMs = now
        _state.update { current ->
            when {
                doubleTap -> current.copy(capsLock = true, shiftEnabled = true)
                current.capsLock -> current.copy(capsLock = false, shiftEnabled = false)
                else -> current.copy(shiftEnabled = !current.shiftEnabled)
            }
        }
    }

    fun onShiftLongPress() {
        _state.update { it.copy(capsLock = true, shiftEnabled = true) }
    }

    fun switchToNumbers() {
        _state.update { it.copy(currentLayout = KeyboardLayout.NUMBERS) }
    }

    fun switchToSymbols() {
        _state.update { it.copy(currentLayout = KeyboardLayout.SYMBOLS) }
    }

    fun switchToLetters() {
        _state.update { it.copy(currentLayout = KeyboardLayout.LETTERS) }
    }

    fun onGlobe() {
        onShowImePicker?.invoke()
    }

    fun commitSuggestion(word: String) {
        inputHandler.commitText(connectionProvider?.invoke(), "$word ")
        onTyped?.invoke(TypedEvent.SpaceOrEnter)
    }

    fun commitRawText(text: String) {
        inputHandler.commitText(connectionProvider?.invoke(), text)
    }
}
