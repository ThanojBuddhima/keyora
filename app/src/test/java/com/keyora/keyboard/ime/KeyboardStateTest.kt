package com.keyora.keyboard.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardStateTest {

    @Test
    fun lettersUppercase_byShiftMode() {
        assertFalse(KeyboardState(shiftMode = ShiftMode.OFF).lettersUppercase)
        assertFalse(
            KeyboardState(shiftMode = ShiftMode.AUTO, autoShiftActive = false).lettersUppercase
        )
        assertTrue(
            KeyboardState(shiftMode = ShiftMode.AUTO, autoShiftActive = true).lettersUppercase
        )
        assertTrue(KeyboardState(shiftMode = ShiftMode.CAPS).lettersUppercase)
    }
}
