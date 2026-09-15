package com.keyora.keyboard.ime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardStateTest {

    @Test
    fun lettersUppercase_whenShiftOrCaps() {
        assertFalse(KeyboardState().lettersUppercase)
        assertTrue(KeyboardState(shiftEnabled = true).lettersUppercase)
        assertTrue(KeyboardState(capsLock = true).lettersUppercase)
    }
}
