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
        val needsCap = shouldAutoCapitalize(connectionProvider?.invoke())
        _state.update {
            it.copy(
                currentPackageName = packageName,
                isPasswordField = password,
                suggestionMode = !password,
                currentLayout = inputHandler.preferredLayout(editorInfo),
                enterLabel = "return",
                shiftMode = ShiftMode.AUTO,
                autoShiftActive = needsCap
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
        when (current.shiftMode) {
            ShiftMode.AUTO -> _state.update { it.copy(autoShiftActive = false) }
            ShiftMode.OFF, ShiftMode.CAPS -> Unit
        }
    }

    fun onBackspace() {
        inputHandler.backspace(connectionProvider?.invoke())
        onTyped?.invoke(TypedEvent.Backspace)
        if (_state.value.shiftMode == ShiftMode.AUTO) {
            _state.update {
                it.copy(autoShiftActive = shouldAutoCapitalize(connectionProvider?.invoke()))
            }
        }
    }

    fun onEnter() {
        inputHandler.enter(connectionProvider?.invoke(), editorInfoProvider?.invoke())
        onTyped?.invoke(TypedEvent.SpaceOrEnter)
        if (_state.value.shiftMode == ShiftMode.AUTO) {
            _state.update { it.copy(autoShiftActive = true) }
        }
    }

    fun onSpace() {
        inputHandler.space(connectionProvider?.invoke())
        onTyped?.invoke(TypedEvent.SpaceOrEnter)
        if (_state.value.shiftMode == ShiftMode.AUTO) {
            _state.update {
                it.copy(autoShiftActive = shouldAutoCapitalize(connectionProvider?.invoke()))
            }
        }
    }

    fun onShift() {
        _state.update { current ->
            when (current.shiftMode) {
                ShiftMode.OFF -> current.copy(
                    shiftMode = ShiftMode.AUTO,
                    autoShiftActive = shouldAutoCapitalize(connectionProvider?.invoke())
                )
                ShiftMode.AUTO -> current.copy(
                    shiftMode = ShiftMode.CAPS,
                    autoShiftActive = false
                )
                ShiftMode.CAPS -> current.copy(
                    shiftMode = ShiftMode.OFF,
                    autoShiftActive = false
                )
            }
        }
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
        if (_state.value.shiftMode == ShiftMode.AUTO) {
            _state.update {
                it.copy(autoShiftActive = shouldAutoCapitalize(connectionProvider?.invoke()))
            }
        }
    }

    fun commitRawText(text: String) {
        inputHandler.commitText(connectionProvider?.invoke(), text)
    }

    private fun shouldAutoCapitalize(connection: InputConnection?): Boolean {
        if (connection == null) return true
        val before = connection.getTextBeforeCursor(64, 0)?.toString().orEmpty()
        if (before.isBlank()) return true
        val trimmed = before.trimEnd()
        if (trimmed.isEmpty()) return true
        val last = trimmed.last()
        if (last == '\n' || last == '\r') return true
        if (last == '.' || last == '!' || last == '?') {
            val afterTerminator = before.length > trimmed.length
            return afterTerminator || before.endsWith(" ") || before.endsWith("\n")
        }
        return false
    }
}
