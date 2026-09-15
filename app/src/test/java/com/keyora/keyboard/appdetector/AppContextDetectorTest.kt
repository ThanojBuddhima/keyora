package com.keyora.keyboard.appdetector

import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppContextDetectorTest {

    private val detector = AppContextDetector()

    @Test
    fun returnsPackageName() {
        val info = EditorInfo().apply { packageName = "com.whatsapp" }
        assertEquals("com.whatsapp", detector.getCurrentPackageName(info))
    }

    @Test
    fun blankPackage_returnsNull() {
        val info = EditorInfo().apply { packageName = "  " }
        assertNull(detector.getCurrentPackageName(info))
    }

    @Test
    fun nullEditor_returnsNull() {
        assertNull(detector.getCurrentPackageName(null))
    }
}
