package com.keyora.keyboard.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionEngineTest {

    private val engine = PlaceholderSuggestionEngine()

    @Test
    fun disabled_returnsEmpty() {
        assertTrue(engine.suggestionsFor("th", enabled = false).isEmpty())
    }

    @Test
    fun prefix_returnsMatches() {
        val result = engine.suggestionsFor("th", enabled = true)
        assertTrue(result.isNotEmpty())
        assertTrue(result.all { it.startsWith("th") })
        assertEquals(3, result.size.coerceAtMost(3))
    }

    @Test
    fun blank_returnsEmpty() {
        assertTrue(engine.suggestionsFor("  ", enabled = true).isEmpty())
    }
}
