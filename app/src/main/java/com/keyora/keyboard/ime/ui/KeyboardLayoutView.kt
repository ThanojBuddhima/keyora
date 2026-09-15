package com.keyora.keyboard.ime.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.keyora.keyboard.ime.KeyboardController
import com.keyora.keyboard.ime.KeyboardLayout
import com.keyora.keyboard.ime.KeyboardState
import com.keyora.keyboard.theme.KeyboardThemeTokens
import com.keyora.keyboard.theme.ResolvedTheme

/**
 * Classic View-based keyboard with fixed key heights (not vertically stretched).
 * Uses a 10-unit width grid so rows align like a standard phone keyboard.
 */
class KeyboardLayoutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var controller: KeyboardController? = null
    private var tokens: KeyboardThemeTokens = KeyboardThemeTokens.Light
    private var keyboardState: KeyboardState = KeyboardState()
    private var pendingRows: List<List<KeySpec>> = emptyList()

    private val keysContainer = LinearLayout(context).apply {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    private enum class KeyKind {
        CHAR,
        SPECIAL,
        RETURN,
        SPACE,
        SPACER
    }

    private data class KeySpec(
        val label: String,
        val units: Float,
        val kind: KeyKind,
        val highlight: Boolean = false,
        val onClick: (() -> Unit)? = null,
        val onLongClick: (() -> Boolean)? = null
    )

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setPadding(dp(HORIZONTAL_PAD_DP), dp(TOP_PAD_DP), dp(HORIZONTAL_PAD_DP), dp(BOTTOM_PAD_DP))
        setBackgroundColor(tokens.background)
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

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && w != oldw) {
            renderRows(pendingRows)
        }
    }

    private fun rebuild() {
        pendingRows = when (keyboardState.currentLayout) {
            KeyboardLayout.LETTERS -> letterRows()
            KeyboardLayout.NUMBERS -> numberRows()
            KeyboardLayout.SYMBOLS -> symbolRows()
        }
        renderRows(pendingRows)
        requestLayout()
        invalidate()
    }

    private fun letterRows(): List<List<KeySpec>> = listOf(
        charRow(listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")),
        listOf(spacer(0.5f)) +
            charRow(listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")) +
            listOf(spacer(0.5f)),
        listOf(
            special(
                label = if (keyboardState.capsLock) "⇪" else "⇧",
                units = 1.5f,
                highlight = keyboardState.shiftEnabled || keyboardState.capsLock,
                onClick = { controller?.onShift() },
                onLongClick = {
                    controller?.onShiftLongPress()
                    true
                }
            )
        ) + charRow(listOf("z", "x", "c", "v", "b", "n", "m")) +
            listOf(special("⌫", 1.5f) { controller?.onBackspace() }),
        bottomRow(leftLabel = "123", onLeft = { controller?.switchToNumbers() })
    )

    private fun numberRows(): List<List<KeySpec>> = listOf(
        charRow(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), literal = true),
        charRow(listOf("-", "/", ":", ";", "(", ")", "$", "&", "@", "\""), literal = true),
        listOf(special("#+=", 1.5f) { controller?.switchToSymbols() }) +
            charRow(listOf(".", ",", "?", "!", "'"), units = 1.4f, literal = true) +
            listOf(special("⌫", 1.5f) { controller?.onBackspace() }),
        bottomRow(leftLabel = "ABC", onLeft = { controller?.switchToLetters() })
    )

    private fun symbolRows(): List<List<KeySpec>> = listOf(
        charRow(listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "="), literal = true),
        charRow(listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•"), literal = true),
        listOf(special("123", 1.5f) { controller?.switchToNumbers() }) +
            charRow(listOf(".", ",", "?", "!", "'"), units = 1.4f, literal = true) +
            listOf(special("⌫", 1.5f) { controller?.onBackspace() }),
        bottomRow(leftLabel = "ABC", onLeft = { controller?.switchToLetters() })
    )

    private fun bottomRow(leftLabel: String, onLeft: () -> Unit): List<KeySpec> = listOf(
        special(leftLabel, 1.25f, onClick = onLeft),
        special("EN", 1.25f) { controller?.onGlobe() },
        KeySpec(label = "", units = 5.0f, kind = KeyKind.SPACE, onClick = { controller?.onSpace() }),
        KeySpec(
            label = keyboardState.enterLabel,
            units = 2.5f,
            kind = KeyKind.RETURN,
            onClick = { controller?.onEnter() }
        )
    )

    private fun charRow(
        chars: List<String>,
        units: Float = 1f,
        literal: Boolean = false
    ): List<KeySpec> = chars.map { ch ->
        val label = if (!literal && keyboardState.lettersUppercase) ch.uppercase() else ch
        KeySpec(
            label = label,
            units = units,
            kind = KeyKind.CHAR,
            onClick = { controller?.onCharacter(ch) }
        )
    }

    private fun special(
        label: String,
        units: Float,
        highlight: Boolean = false,
        onLongClick: (() -> Boolean)? = null,
        onClick: () -> Unit
    ) = KeySpec(
        label = label,
        units = units,
        kind = KeyKind.SPECIAL,
        highlight = highlight,
        onClick = onClick,
        onLongClick = onLongClick
    )

    private fun spacer(units: Float) =
        KeySpec(label = "", units = units, kind = KeyKind.SPACER)

    private fun renderRows(rows: List<List<KeySpec>>) {
        keysContainer.removeAllViews()
        val contentWidth = (width - paddingLeft - paddingRight).coerceAtLeast(0)
        val keyHeight = dp(KEY_HEIGHT_DP)

        if (contentWidth <= 0 || rows.isEmpty()) {
            rows.forEachIndexed { index, _ ->
                keysContainer.addView(
                    View(context).apply {
                        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, keyHeight).apply {
                            if (index < rows.lastIndex) bottomMargin = dp(ROW_GAP_DP)
                        }
                    }
                )
            }
            return
        }

        val gap = dp(KEY_GAP_DP)

        rows.forEachIndexed { rowIndex, specs ->
            val row = LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, keyHeight).apply {
                    if (rowIndex < rows.lastIndex) bottomMargin = dp(ROW_GAP_DP)
                }
            }

            val keyCount = specs.count { it.kind != KeyKind.SPACER }
            val gapCount = (keyCount - 1).coerceAtLeast(0)
            val usableWidth = (contentWidth - gap * gapCount).coerceAtLeast(0)
            val unit = usableWidth / TOTAL_UNITS
            var keysPlaced = 0

            specs.forEach { spec ->
                val widthPx = (spec.units * unit).toInt().coerceAtLeast(1)
                if (spec.kind == KeyKind.SPACER) {
                    row.addView(View(context), LayoutParams(widthPx, keyHeight))
                } else {
                    row.addView(keyView(spec), LayoutParams(widthPx, keyHeight))
                    keysPlaced++
                    if (keysPlaced < keyCount) {
                        row.addView(View(context), LayoutParams(gap, keyHeight))
                    }
                }
            }
            keysContainer.addView(row)
        }
    }

    private fun keyView(spec: KeySpec): TextView {
        val bg = when {
            spec.kind == KeyKind.RETURN || spec.highlight -> tokens.returnKeyBackground
            spec.kind == KeyKind.SPECIAL -> tokens.specialKeyBackground
            else -> tokens.keyBackground
        }
        val fg = when {
            spec.kind == KeyKind.RETURN || spec.highlight -> tokens.returnKeyText
            spec.kind == KeyKind.SPECIAL -> tokens.specialKeyText
            else -> tokens.keyText
        }

        return TextView(context).apply {
            text = if (spec.kind == KeyKind.SPACE) "" else spec.label
            gravity = Gravity.CENTER
            setTextColor(fg)
            setTextSize(
                TypedValue.COMPLEX_UNIT_SP,
                when (spec.kind) {
                    KeyKind.SPECIAL, KeyKind.RETURN -> 13f
                    else -> 18f
                }
            )
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            includeFontPadding = false
            setPadding(0, 0, 0, 0)
            minHeight = 0
            minimumHeight = 0
            minWidth = 0
            minimumWidth = 0
            isClickable = true
            isFocusable = true
            background = roundedKeyDrawable(bg)
            setOnClickListener { spec.onClick?.invoke() }
            if (spec.onLongClick != null) {
                setOnLongClickListener { spec.onLongClick.invoke() }
            }
        }
    }

    private fun roundedKeyDrawable(fill: Int): StateListDrawable {
        fun shape(color: Int) = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(KEY_RADIUS_DP).toFloat()
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

    companion object {
        private const val TOTAL_UNITS = 10f
        private const val KEY_HEIGHT_DP = 42
        private const val KEY_GAP_DP = 5
        private const val ROW_GAP_DP = 10
        private const val KEY_RADIUS_DP = 8
        private const val HORIZONTAL_PAD_DP = 3
        private const val TOP_PAD_DP = 8
        private const val BOTTOM_PAD_DP = 6
    }
}
