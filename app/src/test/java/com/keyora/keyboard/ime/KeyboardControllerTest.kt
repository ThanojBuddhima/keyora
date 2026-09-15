package com.keyora.keyboard.ime

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InputHandlerTest {

    private val handler = InputHandler()

    @Test
    fun preferredLayout_numberClass() {
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        assertEquals(KeyboardLayout.NUMBERS, handler.preferredLayout(info))
    }

    @Test
    fun preferredLayout_textClass() {
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
        }
        assertEquals(KeyboardLayout.LETTERS, handler.preferredLayout(info))
    }

    @Test
    fun isPasswordField_detectsPasswordVariation() {
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        assertTrue(handler.isPasswordField(info))
    }

    @Test
    fun isPasswordField_normalText_false() {
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
        }
        assertFalse(handler.isPasswordField(info))
    }

    @Test
    fun enterLabel_search() {
        val info = EditorInfo().apply {
            imeOptions = EditorInfo.IME_ACTION_SEARCH
        }
        assertEquals("search", handler.enterLabel(info))
    }
}

@RunWith(RobolectricTestRunner::class)
class KeyboardControllerTest {

    @Test
    fun shift_cyclesOffAutoCaps() {
        val controller = KeyboardController()
        assertEquals(ShiftMode.OFF, controller.state.value.shiftMode)
        controller.onShift()
        assertEquals(ShiftMode.AUTO, controller.state.value.shiftMode)
        controller.onShift()
        assertEquals(ShiftMode.CAPS, controller.state.value.shiftMode)
        assertTrue(controller.state.value.lettersUppercase)
        controller.onCharacter("b")
        assertEquals(ShiftMode.CAPS, controller.state.value.shiftMode)
        controller.onShift()
        assertEquals(ShiftMode.OFF, controller.state.value.shiftMode)
    }

    @Test
    fun layoutSwitching() {
        val controller = KeyboardController()
        controller.switchToNumbers()
        assertEquals(KeyboardLayout.NUMBERS, controller.state.value.currentLayout)
        controller.switchToSymbols()
        assertEquals(KeyboardLayout.SYMBOLS, controller.state.value.currentLayout)
        controller.switchToLetters()
        assertEquals(KeyboardLayout.LETTERS, controller.state.value.currentLayout)
    }

    @Test
    fun onStartInput_disablesSuggestionsForPassword() {
        val controller = KeyboardController()
        val info = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            packageName = "com.example"
        }
        controller.onStartInput("com.example", info)
        assertTrue(controller.state.value.isPasswordField)
        assertFalse(controller.state.value.suggestionMode)
        assertEquals("com.example", controller.state.value.currentPackageName)
        assertEquals(ShiftMode.AUTO, controller.state.value.shiftMode)
    }
}
