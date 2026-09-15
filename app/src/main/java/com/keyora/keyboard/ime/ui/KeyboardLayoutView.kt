package com.keyora.keyboard.ime.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.graphics.drawable.StateListDrawable
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.keyora.keyboard.R
import com.keyora.keyboard.ime.KeyboardController
import com.keyora.keyboard.ime.KeyboardLayout
import com.keyora.keyboard.ime.KeyboardState
import com.keyora.keyboard.theme.KeyboardThemeTokens
import com.keyora.keyboard.theme.ResolvedTheme

/**
 * Classic View-based keyboard with fixed key heights and iOS-inspired floating glass keys.
 */
class KeyboardLayoutView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    private var controller: KeyboardController? = null
    private var tokens: KeyboardThemeTokens = KeyboardThemeTokens.Light
    private var keyboardState: KeyboardState = KeyboardState()
    private var pendingRows: List<List<KeySpec>> = emptyList()
    private var keyHeightDp: Int = 42
    private var rowGapDp: Int = 10

    private val keysContainer = LinearLayout(context).apply {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var repeatRunnable: Runnable? = null

    private enum class KeyKind {
        CHAR,
        SPECIAL,
        RETURN,
        SPACE,
        SPACER,
        BACKSPACE,
        SHIFT
    }

    private data class KeySpec(
        val label: String,
        val units: Float,
        val kind: KeyKind,
        val highlight: Boolean = false,
        val strongHighlight: Boolean = false,
        val iconRes: Int? = null,
        val onClick: (() -> Unit)? = null,
        val onLongClick: (() -> Boolean)? = null
    )

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setPadding(dp(HORIZONTAL_PAD_DP), dp(TOP_PAD_DP), dp(HORIZONTAL_PAD_DP), dp(BOTTOM_PAD_DP))
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        addView(keysContainer)
        rebuild()
    }

    fun bind(controller: KeyboardController) {
        this.controller = controller
        rebuild()
    }

    fun setKeyMetrics(keyHeightDp: Int, rowGapDp: Int) {
        if (this.keyHeightDp == keyHeightDp && this.rowGapDp == rowGapDp) return
        this.keyHeightDp = keyHeightDp
        this.rowGapDp = rowGapDp
        rebuild()
    }

    fun applyTheme(theme: ResolvedTheme) {
        val next = KeyboardThemeTokens.forTheme(theme)
        if (tokens == next) return
        tokens = next
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        rebuild()
    }

    fun renderState(state: KeyboardState) {
        if (!keyboardVisualStateChanged(keyboardState, state)) {
            keyboardState = state
            return
        }
        keyboardState = state
        rebuild()
    }

    fun update(theme: ResolvedTheme, state: KeyboardState) {
        val nextTokens = KeyboardThemeTokens.forTheme(theme)
        val themeChanged = tokens != nextTokens
        val stateChanged = keyboardVisualStateChanged(keyboardState, state)
        tokens = nextTokens
        keyboardState = state
        setBackgroundColor(android.graphics.Color.TRANSPARENT)
        if (themeChanged || stateChanged) {
            rebuild()
        }
    }

    private fun keyboardVisualStateChanged(old: KeyboardState, next: KeyboardState): Boolean {
        return old.currentLayout != next.currentLayout ||
            old.shiftMode != next.shiftMode ||
            old.autoShiftActive != next.autoShiftActive ||
            old.enterLabel != next.enterLabel ||
            old.lettersUppercase != next.lettersUppercase
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && w != oldw) {
            renderRows(pendingRows)
        }
    }

    override fun onDetachedFromWindow() {
        stopRepeat()
        super.onDetachedFromWindow()
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
            KeySpec(
                label = "",
                units = 1.5f,
                kind = KeyKind.SHIFT,
                highlight = keyboardState.shiftHighlighted,
                strongHighlight = keyboardState.shiftStrongHighlight,
                iconRes = R.drawable.ic_shift,
                onClick = { controller?.onShift() }
            )
        ) + charRow(listOf("z", "x", "c", "v", "b", "n", "m")) +
            listOf(backspaceKey()),
        bottomRow(leftLabel = "123", onLeft = { controller?.switchToNumbers() })
    )

    private fun numberRows(): List<List<KeySpec>> = listOf(
        charRow(listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"), literal = true),
        charRow(listOf("-", "/", ":", ";", "(", ")", "$", "&", "@", "\""), literal = true),
        listOf(special("#+=", 1.5f) { controller?.switchToSymbols() }) +
            charRow(listOf(".", ",", "?", "!", "'"), units = 1.4f, literal = true) +
            listOf(backspaceKey()),
        bottomRow(leftLabel = "ABC", onLeft = { controller?.switchToLetters() })
    )

    private fun symbolRows(): List<List<KeySpec>> = listOf(
        charRow(listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "="), literal = true),
        charRow(listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•"), literal = true),
        listOf(special("123", 1.5f) { controller?.switchToNumbers() }) +
            charRow(listOf(".", ",", "?", "!", "'"), units = 1.4f, literal = true) +
            listOf(backspaceKey()),
        bottomRow(leftLabel = "ABC", onLeft = { controller?.switchToLetters() })
    )

    private fun bottomRow(leftLabel: String, onLeft: () -> Unit): List<KeySpec> = listOf(
        special(leftLabel, 2.5f, onClick = onLeft),
        KeySpec(label = "", units = 5.0f, kind = KeyKind.SPACE, onClick = { controller?.onSpace() }),
        KeySpec(
            label = "",
            units = 2.5f,
            kind = KeyKind.RETURN,
            iconRes = R.drawable.ic_return,
            onClick = { controller?.onEnter() }
        )
    )

    private fun backspaceKey() = KeySpec(
        label = "",
        units = 1.5f,
        kind = KeyKind.BACKSPACE,
        iconRes = R.drawable.ic_backspace,
        onClick = { controller?.onBackspace() }
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
        val keyHeight = dp(keyHeightDp)

        if (contentWidth <= 0 || rows.isEmpty()) {
            rows.forEachIndexed { index, _ ->
                keysContainer.addView(
                    View(context).apply {
                        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, keyHeight).apply {
                            if (index < rows.lastIndex) bottomMargin = dp(rowGapDp)
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
                    if (rowIndex < rows.lastIndex) bottomMargin = dp(rowGapDp)
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

    private fun keyView(spec: KeySpec): View {
        val fill = when {
            spec.strongHighlight || (spec.kind == KeyKind.RETURN && spec.highlight) ->
                tokens.returnKeyBackground
            spec.highlight -> tokens.keyBackground
            spec.kind == KeyKind.RETURN -> tokens.returnKeyBackground
            spec.kind == KeyKind.SPECIAL ||
                spec.kind == KeyKind.BACKSPACE ||
                spec.kind == KeyKind.SHIFT -> tokens.specialKeyBackground
            else -> tokens.keyBackground
        }
        val fg = when {
            spec.strongHighlight -> tokens.returnKeyText
            spec.kind == KeyKind.RETURN -> tokens.returnKeyText
            spec.kind == KeyKind.SPECIAL ||
                spec.kind == KeyKind.BACKSPACE ||
                spec.kind == KeyKind.SHIFT -> tokens.specialKeyText
            else -> tokens.keyText
        }
        val elevated = spec.kind == KeyKind.CHAR || spec.kind == KeyKind.SPACE

        val container = FrameLayout(context).apply {
            isClickable = true
            isFocusable = true
            background = keyBackground(fill, elevated)
        }

        if (spec.iconRes != null) {
            container.addView(
                ImageView(context).apply {
                    val drawable = AppCompatResources.getDrawable(context, spec.iconRes)?.mutate()
                    if (drawable != null) {
                        DrawableCompat.setTint(drawable, fg)
                        setImageDrawable(drawable)
                    }
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    layoutParams = FrameLayout.LayoutParams(dp(22), dp(22), Gravity.CENTER)
                }
            )
        } else if (spec.kind != KeyKind.SPACE) {
            container.addView(
                TextView(context).apply {
                    text = spec.label
                    gravity = Gravity.CENTER
                    setTextColor(fg)
                    setTextSize(
                        TypedValue.COMPLEX_UNIT_SP,
                        when (spec.kind) {
                            KeyKind.SPECIAL, KeyKind.RETURN -> 15f
                            else -> 22f
                        }
                    )
                    typeface = Typeface.create("sans-serif", Typeface.NORMAL)
                    includeFontPadding = false
                    layoutParams = FrameLayout.LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.MATCH_PARENT
                    )
                }
            )
        }

        if (spec.kind == KeyKind.BACKSPACE) {
            container.setOnTouchListener { v, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        v.isPressed = true
                        controller?.onBackspace()
                        startRepeat { controller?.onBackspace() }
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        v.isPressed = false
                        stopRepeat()
                        true
                    }
                    else -> false
                }
            }
        } else {
            container.setOnClickListener { spec.onClick?.invoke() }
            if (spec.onLongClick != null) {
                container.setOnLongClickListener { spec.onLongClick.invoke() }
            }
        }
        return container
    }

    private fun startRepeat(action: () -> Unit) {
        stopRepeat()
        val runnable = object : Runnable {
            override fun run() {
                action()
                handler.postDelayed(this, REPEAT_INTERVAL_MS)
            }
        }
        repeatRunnable = runnable
        handler.postDelayed(runnable, REPEAT_INITIAL_DELAY_MS)
    }

    private fun stopRepeat() {
        repeatRunnable?.let { handler.removeCallbacks(it) }
        repeatRunnable = null
    }

    private fun keyBackground(fill: Int, elevated: Boolean): android.graphics.drawable.Drawable {
        val radius = dp(KEY_RADIUS_DP).toFloat()
        fun shape(color: Int) = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(color)
        }

        val normalFill = shape(fill)
        val pressedFill = shape(darken(fill))

        val content = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_pressed), pressedFill)
            addState(intArrayOf(), normalFill)
        }

        val withShadow = if (elevated) {
            val shadow = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = radius
                setColor(tokens.keyShadow)
            }
            LayerDrawable(arrayOf(shadow, content)).apply {
                val offset = dp(1)
                setLayerInset(0, 0, offset, 0, 0)
                setLayerInset(1, 0, 0, 0, offset)
            }
        } else {
            content
        }

        return RippleDrawable(
            ColorStateList.valueOf(0x33FFFFFF),
            withShadow,
            shape(0xFFFFFFFF.toInt())
        )
    }

    private fun darken(color: Int): Int {
        val a = color ushr 24
        val r = ((color shr 16) and 0xFF) * 88 / 100
        val g = ((color shr 8) and 0xFF) * 88 / 100
        val b = (color and 0xFF) * 88 / 100
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
        private const val KEY_GAP_DP = 6
        private const val KEY_RADIUS_DP = 5
        private const val HORIZONTAL_PAD_DP = 3
        private const val TOP_PAD_DP = 6
        private const val BOTTOM_PAD_DP = 2
        private const val REPEAT_INITIAL_DELAY_MS = 400L
        private const val REPEAT_INTERVAL_MS = 50L
    }
}
