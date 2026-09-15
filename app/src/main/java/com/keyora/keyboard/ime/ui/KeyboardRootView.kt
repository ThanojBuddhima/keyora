package com.keyora.keyboard.ime.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.keyora.keyboard.R
import com.keyora.keyboard.clipboard.ClipboardRepository
import com.keyora.keyboard.ime.KeyboardController
import com.keyora.keyboard.ime.emoji.EmojiCatalog
import com.keyora.keyboard.settings.KeyboardHeightLevel
import com.keyora.keyboard.theme.KeyboardThemeTokens
import com.keyora.keyboard.theme.ResolvedTheme

/**
 * IME root: suggestion strip + keys + bottom emoji/clipboard dock (iOS-inspired chrome).
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
    private var suggestions: List<String> = emptyList()

    private val suggestionBar = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(SUGGESTION_HEIGHT_DP))
        setPadding(dp(8), 0, dp(8), 0)
    }

    private val panelHost = FrameLayout(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        visibility = GONE
    }

    val keyboardLayout = KeyboardLayoutView(context).apply {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    private val dock = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(DOCK_CONTENT_DP))
        setPadding(dp(16), dp(4), dp(16), dp(4))
    }

    init {
        orientation = VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        background = plateBackground(tokens.background)
        addView(suggestionBar)
        addView(panelHost)
        addView(keyboardLayout)
        addView(dock)
        applyBottomSafePadding()
        rebuildSuggestionBar()
        rebuildDock()

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
        rebuildDock()
    }

    fun update(
        theme: ResolvedTheme,
        heightLevel: KeyboardHeightLevel,
        passwordField: Boolean,
        suggestions: List<String> = emptyList()
    ) {
        this.tokens = KeyboardThemeTokens.forTheme(theme)
        this.heightLevel = heightLevel
        this.passwordField = passwordField
        this.suggestions = suggestions
        background = plateBackground(tokens.background)
        keyboardLayout.setKeyMetrics(heightLevel.keyHeightDp(), heightLevel.rowGapDp())
        keyboardLayout.applyTheme(theme)
        clipboardRepository?.captureEnabled = !passwordField
        if (passwordField && panel == Panel.CLIPBOARD) {
            showPanel(Panel.NONE)
        }
        rebuildSuggestionBar()
        rebuildDock()
        if (panel != Panel.NONE) renderPanel()
    }

    fun renderKeyboardState(state: com.keyora.keyboard.ime.KeyboardState) {
        keyboardLayout.renderState(state)
    }

    private fun applyBottomSafePadding() {
        setPadding(0, 0, 0, navInsetBottom + dp(EXTRA_BOTTOM_PAD_DP))
    }

    private fun rebuildSuggestionBar() {
        suggestionBar.removeAllViews()
        val show = !passwordField
        suggestionBar.visibility = if (show) VISIBLE else GONE
        if (!show) return

        val slots = List(3) { index -> suggestions.getOrNull(index).orEmpty() }
        slots.forEachIndexed { index, word ->
            if (index > 0) {
                suggestionBar.addView(
                    View(context).apply {
                        layoutParams = LayoutParams(dp(1), dp(18)).apply {
                            marginStart = dp(4)
                            marginEnd = dp(4)
                        }
                        setBackgroundColor(tokens.suggestionDivider)
                    }
                )
            }
            suggestionBar.addView(
                TextView(context).apply {
                    text = word
                    gravity = Gravity.CENTER
                    setTextColor(tokens.suggestionText)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                    maxLines = 1
                    layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1f)
                    isClickable = word.isNotBlank()
                    if (word.isNotBlank()) {
                        setOnClickListener {
                            controller?.commitSuggestion(word)
                        }
                    }
                }
            )
        }
    }

    private fun rebuildDock() {
        dock.removeAllViews()
        dock.addView(
            dockIcon(
                iconRes = R.drawable.ic_emoji,
                onClick = { togglePanel(Panel.EMOJI) }
            )
        )
        dock.addView(
            View(context).apply {
                layoutParams = LayoutParams(0, 1, 1f)
            }
        )
        dock.addView(
            dockIcon(
                iconRes = R.drawable.ic_clipboard,
                onClick = {
                    if (!passwordField) togglePanel(Panel.CLIPBOARD)
                },
                onLongClick = {
                    onCycleHeight?.invoke()
                    true
                }
            )
        )
    }

    private fun dockIcon(
        iconRes: Int,
        onClick: () -> Unit,
        onLongClick: (() -> Boolean)? = null
    ): ImageView {
        return ImageView(context).apply {
            val drawable = AppCompatResources.getDrawable(context, iconRes)?.mutate()
            if (drawable != null) {
                DrawableCompat.setTint(drawable, tokens.dockIcon)
                setImageDrawable(drawable)
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            layoutParams = LayoutParams(dp(36), dp(36))
            setPadding(dp(6), dp(6), dp(6), dp(6))
            isClickable = true
            isFocusable = true
            setOnClickListener { onClick() }
            if (onLongClick != null) {
                setOnLongClickListener { onLongClick() }
            }
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
            setTextColor(tokens.suggestionText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener {
                clipboardRepository?.clear()
                renderPanel()
            }
        })
        header.addView(TextView(context).apply {
            text = "ABC"
            setTextColor(tokens.suggestionText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            setOnClickListener {
                showPanel(Panel.NONE)
                controller?.switchToLetters()
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

    private fun plateBackground(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        setColor(color)
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
        private const val EXTRA_BOTTOM_PAD_DP = 8
        private const val PANEL_HEIGHT_DP = 220
        private const val SUGGESTION_HEIGHT_DP = 40
        private const val DOCK_CONTENT_DP = 36
    }
}
