package com.keyora.keyboard.clipboard

import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * On-device clipboard history. Never uploaded.
 * Capture is skipped while [captureEnabled] is false (e.g. password fields).
 */
class ClipboardRepository(private val context: Context) {

    private val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    private val _clips = MutableStateFlow<List<String>>(emptyList())
    val clips: StateFlow<List<String>> = _clips.asStateFlow()

    @Volatile
    var captureEnabled: Boolean = true

    private val listener = ClipboardManager.OnPrimaryClipChangedListener {
        if (!captureEnabled) return@OnPrimaryClipChangedListener
        val text = clipboard.primaryClip
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.coerceToText(context)
            ?.toString()
            ?.trim()
            .orEmpty()
        if (text.isNotEmpty()) {
            addClip(text)
        }
    }

    fun start() {
        runCatching { clipboard.addPrimaryClipChangedListener(listener) }
        // Seed with current clip if any.
        if (captureEnabled) {
            val text = clipboard.primaryClip
                ?.takeIf { it.itemCount > 0 }
                ?.getItemAt(0)
                ?.coerceToText(context)
                ?.toString()
                ?.trim()
                .orEmpty()
            if (text.isNotEmpty()) addClip(text)
        }
    }

    fun stop() {
        runCatching { clipboard.removePrimaryClipChangedListener(listener) }
    }

    fun clear() {
        _clips.value = emptyList()
    }

    private fun addClip(text: String) {
        val current = _clips.value.toMutableList()
        current.removeAll { it == text }
        current.add(0, text.take(MAX_CLIP_CHARS))
        _clips.value = current.take(MAX_CLIPS)
    }

    companion object {
        private const val MAX_CLIPS = 20
        private const val MAX_CLIP_CHARS = 500
    }
}
