package com.keyora.keyboard.ime.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import com.keyora.keyboard.ime.KeyboardController
import com.keyora.keyboard.ime.KeyboardLayout
import com.keyora.keyboard.ime.KeyboardState
import com.keyora.keyboard.theme.KeyboardThemeTokens
import com.keyora.keyboard.theme.ResolvedTheme

/**
 * Classic View-based keyboard for InputMethodService.
 * Avoids Compose-in-IME lifecycle/crash issues on real devices.
 */
class KeyboardLayoutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var controller: KeyboardController? = null
    private var tokens: KeyboardThemeTokens = KeyboardThemeTokens.Light
    private var keyboardState: KeyboardState = KeyboardState()

    private val keysContainer = LinearLayout(context).apply {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    init {
        orientation = VERTICAL
        val heightPx = dp(280)
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, heightPx)
        minimumHeight = heightPx
        setPadding(dp(4), dp(6), dp(4), dp(10))
        addView(keysContainer)
        rebuild()
    }

    fun bind(controller: KeyboardController) {
        this.controller = controller
        rebuild()
    }

    fun applyTheme(theme: ResolvedTheme) {
        tokens = KeyboardThemeTokens.forTheme(theme)
        setBackgroundColor(tokens.background)
        rebuild()
    }

    fun renderState(state: KeyboardState) {
        keyboardState = state
        rebuild()
    }

    fun update(theme: ResolvedTheme, state: KeyboardState) {
        tokens = KeyboardThemeTokens.forTheme(theme)
        keyboardState = state
        setBackgroundColor(tokens.background)
        rebuild()
    }

    private fun rebuild() {
        keysContainer.removeAllViews()
        when (keyboardState.currentLayout) {
            KeyboardLayout.LETTERS -> buildLetters()
            KeyboardLayout.NUMBERS -> buildNumbers()
            KeyboardLayout.SYMBOLS -> buildSymbols()
        }
        requestLayout()
        invalidate()
    }

    private fun buildLetters() {
        addCharRow(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"), weight = 1f)
        addCharRow(listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"), weight = 1f, sidePad = 0.35f)
        addRow {
            addSpecialKey(
                label = if (keyboardState.capsLock) "⇪" else "⇧",
                weight = 1.4f,
                highlight = keyboardState.shiftEnabled || keyboardState.capsLock,
                onLongClick = {
                    controller?.onShiftLongPress()
                    true
                }
            ) { controller?.onShift() }
            listOf("z", "x", "c", "v", "b", "n", "m").forEach { ch ->
                addCharKey(ch, weight = 1f)
            }
            addSpecialKey("⌫", weight = 1.4f) { controller?.onBackspace() }
        }
        addBottomRow(leftLabel = "123", onLeft = { controller?.switchToNumbers() })
    }

    private fun buildNumbers() {
        addCharRow(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), literal = true)
        addCharRow(listOf("-", "/", ":", ";", "(", ")", "$", "&", "@", "\""), literal = true)
        addRow {
            addSpecialKey("#+=", weight = 1.5f) { controller?.switchToSymbols() }
            listOf(".", ",", "?", "!", "'").forEach { ch ->
                addCharKey(ch, weight = 1.2f, literal = true)
            }
            addSpecialKey("⌫", weight = 1.5f) { controller?.onBackspace() }
        }
        addBottomRow(leftLabel = "ABC", onLeft = { controller?.switchToLetters() })
    }

    private fun buildSymbols() {
        addCharRow(listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "="), literal = true)
        addCharRow(listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•"), literal = true)
        addRow {
            addSpecialKey("123", weight = 1.5f) { controller?.switchToNumbers() }
            listOf(".", ",", "?", "!", "'").forEach { ch ->
                addCharKey(ch, weight = 1.2f, literal = true)
            }
            addSpecialKey("⌫", weight = 1.5f) { controller?.onBackspace() }
        }
        addBottomRow(leftLabel = "ABC", onLeft = { controller?.switchToLetters() })
    }

    private fun addBottomRow(leftLabel: String, onLeft: () -> Unit) {
        addRow {
            addSpecialKey(leftLabel, weight = 1.3f, onClick = onLeft)
            addSpecialKey("🌐", weight = 1.2f) { controller?.onGlobe() }
            addCharKey("space", weight = 4.8f, literal = true, displayOverride = "space") {
                controller?.onSpace()
            }
            addSpecialKey(
                label = keyboardState.enterLabel,
                weight = 2.2f,
                background = tokens.returnKeyBackground,
                textColor = tokens.returnKeyText
            ) { controller?.onEnter() }
        }
    }

    private fun addCharRow(
        chars: List<String>,
        weight: Float = 1f,
        sidePad: Float = 0f,
        literal: Boolean = false
    ) {
        addRow {
            if (sidePad > 0f) {
                addView(View(context).apply {
                    layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, sidePad)
                })
            }
            chars.forEach { ch -> addCharKey(ch, weight = weight, literal = literal) }
            if (sidePad > 0f) {
                addView(View(context).apply {
                    layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, sidePad)
                })
            }
        }
    }

    private fun addRow(build: LinearLayout.() -> Unit) {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).apply {
                topMargin = dp(3)
                bottomMargin = dp(3)
            }
        }
        row.build()
        keysContainer.addView(row)
    }

    private fun LinearLayout.addCharKey(
        char: String,
        weight: Float,
        literal: Boolean = false,
        displayOverride: String? = null,
        onClick: (() -> Unit)? = null
    ) {
        val display = displayOverride ?: if (!literal && keyboardState.lettersUppercase) {
            char.uppercase()
        } else {
            char
        }
        val button = keyButton(
            label = display,
            background = tokens.keyBackground,
            textColor = tokens.keyText,
            textSizeSp = if (display == "space") 14f else 18f
        )
        button.setOnClickListener {
            onClick?.invoke() ?: controller?.onCharacter(char)
            // Labels update after shift toggles off on character press.
            post { /* state refreshed by service observer */ }
        }
        addView(button, LayoutParams(0, LayoutParams.MATCH_PARENT, weight).apply {
            marginStart = dp(2)
            marginEnd = dp(2)
        })
    }

    private fun LinearLayout.addSpecialKey(
        label: String,
        weight: Float,
        background: Int = tokens.specialKeyBackground,
        textColor: Int = tokens.specialKeyText,
        highlight: Boolean = false,
        onLongClick: (() -> Boolean)? = null,
        onClick: () -> Unit
    ) {
        val button = keyButton(
            label = label,
            background = if (highlight) tokens.returnKeyBackground else background,
            textColor = if (highlight) tokens.returnKeyText else textColor,
            textSizeSp = 14f
        )
        button.setOnClickListener { onClick() }
        if (onLongClick != null) {
            button.setOnLongClickListener { onLongClick() }
        }
        addView(button, LayoutParams(0, LayoutParams.MATCH_PARENT, weight).apply {
            marginStart = dp(2)
            marginEnd = dp(2)
        })
    }

    private fun keyButton(
        label: String,
        background: Int,
        textColor: Int,
        textSizeSp: Float
    ): Button {
        return Button(context, null, android.R.attr.borderlessButtonStyle).apply {
            text = label
            isAllCaps = false
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 0)
            minHeight = 0
            minimumHeight = 0
            this.background = roundedKeyDrawable(background)
            elevation = dp(1).toFloat()
        }
    }

    private fun roundedKeyDrawable(fill: Int): StateListDrawable {
        fun shape(color: Int) = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(8).toFloat()
            setColor(color)
        }
        return StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), shape(darken(fill)))
            addState(intArrayOf(), shape(fill))
        }
    }

    private fun darken(color: Int): Int {
        val a = color ushr 24
        val r = ((color shr 16) and 0xFF) * 85 / 100
        val g = ((color shr 8) and 0xFF) * 85 / 100
        val b = (color and 0xFF) * 85 / 100
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()
}
