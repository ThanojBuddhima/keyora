package com.keyora.keyboard.suggestions

/**
 * Modular suggestion engine. MVP provides basic on-device placeholders only.
 * Never send typed text to a server.
 */
interface SuggestionEngine {
    fun suggestionsFor(prefix: String, enabled: Boolean): List<String>
}

class PlaceholderSuggestionEngine : SuggestionEngine {

    private val dictionary = listOf(
        "the", "to", "and", "a", "of", "in", "is", "it", "you", "that",
        "for", "on", "with", "as", "are", "this", "be", "or", "have", "from",
        "hello", "thanks", "please", "yes", "no", "okay", "good", "great",
        "tomorrow", "today", "meeting", "message", "email", "phone", "keyora"
    )

    override fun suggestionsFor(prefix: String, enabled: Boolean): List<String> {
        if (!enabled || prefix.isBlank()) return emptyList()
        val lower = prefix.lowercase()
        return dictionary
            .filter { it.startsWith(lower) && it != lower }
            .take(3)
    }
}
