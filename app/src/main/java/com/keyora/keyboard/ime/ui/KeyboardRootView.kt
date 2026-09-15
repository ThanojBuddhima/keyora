package com.keyora.keyboard.ime.ui

import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.keyora.keyboard.clipboard.ClipboardRepository
import com.keyora.keyboard.ime.KeyboardController
import com.keyora.keyboard.ime.emoji.EmojiCatalog
import com.keyora.keyboard.settings.KeyboardHeightLevel
import com.keyora.keyboard.theme.KeyboardThemeTokens
import com.keyora.keyboard.theme.ResolvedTheme

/**
 * IME root: toolbar + optional panel + letter keyboard, with nav-bar safe padding.
 */
class KeyboardRootView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    enum class Panel { NONE, EMOJI, CLIPBOARD }

    private var controller: KeyboardController? = null
    private var clipboardRepository: ClipboardRepository? = null
    private var onCycleHeight: (() -> Unit)? = null
    private var tokens = KeyboardThemeTokens.Light
    private var heightLevel = KeyboardHeightLevel.MEDIUM
    private var panel = Panel.NONE
    private var navInsetBottom = 0
    private var passwordField = false

    private val toolbar = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(36))
        setPadding(dp(6), dp(4), dp(6), dp(4))
    }

    private val panelHost = FrameLayout(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        visibility = GONE
    }

    val keyboardLayout = KeyboardLayoutView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        setBackgroundColor(tokens.background)
        addView(toolbar)
        addView(panelHost)
        addView(keyboardLayout)
        applyBottomSafePadding()
        rebuildToolbar()

        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            navInsetBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            applyBottomSafePadding()
            insets
        }
        ViewCompat.requestApplyInsets(this)
    }

    fun bind(
        controller: KeyboardController,
        clipboardRepository: ClipboardRepository,
        onCycleHeight: () -> Unit
    ) {
        this.controller = controller
        this.clipboardRepository = clipboardRepository
        this.onCycleHeight = onCycleHeight
        keyboardLayout.bind(controller)
        rebuildToolbar()
    }

    fun update(
        theme: ResolvedTheme,
        heightLevel: KeyboardHeightLevel,
        passwordField: Boolean
    ) {
        this.tokens = KeyboardThemeTokens.forTheme(theme)
        this.heightLevel = heightLevel
        this.passwordField = passwordField
        setBackgroundColor(tokens.background)
        keyboardLayout.setKeyMetrics(heightLevel.keyHeightDp(), heightLevel.rowGapDp())
        keyboardLayout.applyTheme(theme)
        clipboardRepository?.captureEnabled = !passwordField
        if (passwordField && panel == Panel.CLIPBOARD) {
            showPanel(Panel.NONE)
        }
        rebuildToolbar()
        if (panel != Panel.NONE) renderPanel()
    }

    fun renderKeyboardState(state: com.keyora.keyboard.ime.KeyboardState) {
        keyboardLayout.renderState(state)
    }

    private fun applyBottomSafePadding() {
        val extra = dp(EXTRA_BOTTOM_PAD_DP)
        setPadding(0, 0, 0, navInsetBottom + extra)
    }

    private fun rebuildToolbar() {
        toolbar.removeAllViews()
        toolbar.setBackgroundColor(tokens.background)
        toolbar.addView(toolButton("☺") { togglePanel(Panel.EMOJI) })
        toolbar.addView(toolButton("CB") {
            if (!passwordField) togglePanel(Panel.CLIPBOARD)
        })
        toolbar.addView(toolButton("H ${heightLevel.label()}") {
            onCycleHeight?.invoke()
        })
        val spacer = View(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
        }
        toolbar.addView(spacer)
        toolbar.addView(toolButton("ABC") {
            showPanel(Panel.NONE)
            controller?.switchToLetters()
        })
    }

    private fun toolButton(label: String, onClick: () -> Unit): TextView {
        return TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            setTextColor(tokens.keyText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = rounded(tokens.keyBackground, dp(8))
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT).apply {
                marginEnd = dp(6)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun togglePanel(target: Panel) {
        showPanel(if (panel == target) Panel.NONE else target)
    }

    private fun showPanel(target: Panel) {
        panel = target
        if (panel == Panel.NONE) {
            panelHost.visibility = GONE
            panelHost.removeAllViews()
            keyboardLayout.visibility = VISIBLE
        } else {
            keyboardLayout.visibility = GONE
            panelHost.visibility = VISIBLE
            renderPanel()
        }
    }

    private fun renderPanel() {
        panelHost.removeAllViews()
        when (panel) {
            Panel.EMOJI -> panelHost.addView(buildEmojiPanel())
            Panel.CLIPBOARD -> panelHost.addView(buildClipboardPanel())
            Panel.NONE -> Unit
        }
    }

    private fun buildEmojiPanel(): View {
        val scroll = ScrollView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(PANEL_HEIGHT_DP)
            )
        }
        val grid = GridLayout(context).apply {
            columnCount = 8
            setPadding(dp(6), dp(4), dp(6), dp(4))
        }
        EmojiCatalog.all.forEach { emoji ->
            val cell = TextView(context).apply {
                text = emoji
                gravity = Gravity.CENTER
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                setPadding(dp(6), dp(8), dp(6), dp(8))
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    height = LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
                setOnClickListener { controller?.commitRawText(emoji) }
            }
            grid.addView(cell)
        }
        scroll.addView(grid)
        return scroll
    }

    private fun buildClipboardPanel(): View {
        val column = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(PANEL_HEIGHT_DP)
            )
            setPadding(dp(8), dp(4), dp(8), dp(4))
        }
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        header.addView(TextView(context).apply {
            text = "Clipboard"
            setTextColor(tokens.keyText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        })
        header.addView(TextView(context).apply {
            text = "Clear"
            setTextColor(tokens.returnKeyBackground)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener {
                clipboardRepository?.clear()
                renderPanel()
            }
        })
        column.addView(header)

        val clips = clipboardRepository?.clips?.value.orEmpty()
        if (clips.isEmpty()) {
            column.addView(TextView(context).apply {
                text = if (passwordField) {
                    "Clipboard disabled for password fields."
                } else {
                    "No recent clips yet. Copy text in any app."
                }
                setTextColor(tokens.specialKeyText)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                setPadding(0, dp(12), 0, 0)
            })
        } else {
            val scroll = ScrollView(context).apply {
                layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
            }
            val list = LinearLayout(context).apply { orientation = VERTICAL }
            clips.forEach { clip ->
                list.addView(TextView(context).apply {
                    text = clip
                    maxLines = 2
                    setTextColor(tokens.keyText)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    setPadding(dp(10), dp(10), dp(10), dp(10))
                    background = rounded(tokens.keyBackground, dp(8))
                    layoutParams = LayoutParams(
                        LayoutParams.MATCH_PARENT,
                        LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(6) }
                    setOnClickListener {
                        controller?.commitRawText(clip)
                        showPanel(Panel.NONE)
                    }
                })
            }
            scroll.addView(list)
            column.addView(scroll)
        }
        return column
    }

    private fun rounded(color: Int, radius: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = radius.toFloat()
        setColor(color)
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            resources.displayMetrics
        ).toInt()

    companion object {
        private const val EXTRA_BOTTOM_PAD_DP = 12
        private const val PANEL_HEIGHT_DP = 220
    }
}
