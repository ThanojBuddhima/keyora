package com.keyora.keyboard.ime.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
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
import com.keyora.keyboard.ime.emoji.RecentEmojiStore
import com.keyora.keyboard.settings.KeyboardHeightLevel
import com.keyora.keyboard.settings.SettingsActivity
import com.keyora.keyboard.theme.KeyboardThemeTokens
import com.keyora.keyboard.theme.ResolvedTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * IME root: top utility toolbar + keys (or emoji/clipboard panel).
 */
class KeyboardRootView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    enum class Panel { NONE, EMOJI, CLIPBOARD }

    private var controller: KeyboardController? = null
    private var clipboardRepository: ClipboardRepository? = null
    private var recentEmojiStore: RecentEmojiStore? = null
    private var onCycleHeight: (() -> Unit)? = null
    private var onCycleTheme: (() -> Unit)? = null
    private var tokens = KeyboardThemeTokens.Light
    private var resolvedTheme: ResolvedTheme = ResolvedTheme.LIGHT
    private var heightLevel = KeyboardHeightLevel.MEDIUM
    private var panel = Panel.NONE
    private var navInsetBottom = 0
    private var passwordField = false
    private var chromeApplied = false
    private var emojiTabId: String = TAB_RECENT
    private var recentEmojis: List<String> = emptyList()
    private val viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val emojiTabOrder: List<String> =
        listOf(TAB_RECENT) + EmojiCatalog.categories.map { it.id }

    private val toolbar = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dp(TOOLBAR_HEIGHT_DP))
        setPadding(dp(10), dp(2), dp(10), dp(2))
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
        background = plateBackground(tokens.background)
        addView(toolbar)
        addView(panelHost)
        addView(keyboardLayout)
        applyBottomSafePadding()
        rebuildToolbar()

        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            val nav = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures()).bottom
            navInsetBottom = maxOf(nav, gestures)
            applyBottomSafePadding()
            insets
        }
        ViewCompat.requestApplyInsets(this)
    }

    fun bind(
        controller: KeyboardController,
        clipboardRepository: ClipboardRepository,
        recentEmojiStore: RecentEmojiStore,
        onCycleHeight: () -> Unit,
        onCycleTheme: () -> Unit
    ) {
        this.controller = controller
        this.clipboardRepository = clipboardRepository
        this.recentEmojiStore = recentEmojiStore
        this.onCycleHeight = onCycleHeight
        this.onCycleTheme = onCycleTheme
        keyboardLayout.bind(controller)
        rebuildToolbar()
        viewScope.launch {
            recentEmojiStore.recent.collect { list ->
                recentEmojis = list
                // Only rebuild when Recent tab is visible — avoids wiping inserts mid-tap.
                if (panel == Panel.EMOJI && emojiTabId == TAB_RECENT) {
                    renderPanel()
                }
            }
        }
    }

    fun updateChrome(
        theme: ResolvedTheme,
        heightLevel: KeyboardHeightLevel,
        passwordField: Boolean
    ) {
        val themeChanged = !chromeApplied || resolvedTheme != theme
        val heightChanged = !chromeApplied || this.heightLevel != heightLevel
        val passwordChanged = !chromeApplied || this.passwordField != passwordField
        if (!themeChanged && !heightChanged && !passwordChanged) return

        resolvedTheme = theme
        tokens = KeyboardThemeTokens.forTheme(theme)
        this.heightLevel = heightLevel
        this.passwordField = passwordField
        chromeApplied = true

        if (themeChanged) {
            background = plateBackground(tokens.background)
            keyboardLayout.applyTheme(theme)
        }
        if (heightChanged) {
            keyboardLayout.setKeyMetrics(heightLevel.keyHeightDp(), heightLevel.rowGapDp())
        }
        clipboardRepository?.captureEnabled = !passwordField
        if (passwordField && panel == Panel.CLIPBOARD) {
            showPanel(Panel.NONE)
        }
        if (themeChanged || passwordChanged) {
            rebuildToolbar()
        }
        if (panel != Panel.NONE && (themeChanged || passwordChanged)) {
            renderPanel()
        }
    }

    fun renderKeyboardState(state: com.keyora.keyboard.ime.KeyboardState) {
        keyboardLayout.renderState(state)
    }

    override fun onDetachedFromWindow() {
        viewScope.cancel()
        super.onDetachedFromWindow()
    }

    private fun applyBottomSafePadding() {
        setPadding(0, 0, 0, navInsetBottom + dp(EXTRA_BOTTOM_PAD_DP))
    }

    private fun rebuildToolbar() {
        toolbar.removeAllViews()
        toolbar.addView(
            toolbarIcon(
                iconRes = R.drawable.ic_emoji,
                onClick = { togglePanel(Panel.EMOJI) }
            )
        )
        toolbar.addView(
            toolbarIcon(
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
        toolbar.addView(
            View(context).apply {
                layoutParams = LayoutParams(0, 1, 1f)
            }
        )
        toolbar.addView(
            toolbarIcon(
                iconRes = R.drawable.ic_theme,
                onClick = { onCycleTheme?.invoke() }
            )
        )
        toolbar.addView(
            toolbarIcon(
                iconRes = R.drawable.ic_settings,
                onClick = { openSettings() }
            )
        )
    }

    private fun openSettings() {
        try {
            context.startActivity(
                Intent(context, SettingsActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Throwable) {
            // Ignore if activity cannot be launched from IME context.
        }
    }

    private fun toolbarIcon(
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
            layoutParams = LayoutParams(dp(40), dp(40)).apply {
                marginEnd = dp(4)
            }
            setPadding(dp(8), dp(8), dp(8), dp(8))
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
        val column = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                dp(PANEL_HEIGHT_DP)
            )
        }

        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(4), dp(2), dp(4), dp(2))
        }
        val tabsScroll = HorizontalScrollView(context).apply {
            isHorizontalScrollBarEnabled = false
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        val tabsRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        tabsRow.addView(categoryChip(TAB_RECENT, "Recent", selected = emojiTabId == TAB_RECENT))
        EmojiCatalog.categories.forEach { category ->
            tabsRow.addView(
                categoryChip(
                    id = category.id,
                    label = category.label,
                    selected = emojiTabId == category.id
                )
            )
        }
        tabsScroll.addView(tabsRow)
        header.addView(tabsScroll)
        header.addView(TextView(context).apply {
            text = "ABC"
            setTextColor(tokens.suggestionText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener {
                showPanel(Panel.NONE)
                controller?.switchToLetters()
            }
        })
        column.addView(header)

        val scroll = ScrollView(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val grid = GridLayout(context).apply {
            columnCount = 8
            setPadding(dp(6), dp(4), dp(6), dp(4))
        }
        val emojis = emojiListForTab(emojiTabId)
        if (emojis.isEmpty()) {
            grid.addView(
                TextView(context).apply {
                    text = if (emojiTabId == TAB_RECENT) {
                        "No recent emoji yet."
                    } else {
                        "No emoji in this category."
                    }
                    setTextColor(tokens.specialKeyText)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setPadding(dp(8), dp(16), dp(8), dp(8))
                    layoutParams = GridLayout.LayoutParams().apply {
                        width = LayoutParams.MATCH_PARENT
                        columnSpec = GridLayout.spec(0, 8)
                    }
                }
            )
        } else {
            emojis.forEach { emoji ->
                grid.addView(
                    TextView(context).apply {
                        text = emoji
                        gravity = Gravity.CENTER
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                        setPadding(dp(6), dp(8), dp(6), dp(8))
                        layoutParams = GridLayout.LayoutParams().apply {
                            width = 0
                            height = LayoutParams.WRAP_CONTENT
                            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                        }
                        setOnClickListener { commitEmoji(emoji) }
                    }
                )
            }
        }
        scroll.addView(grid)
        attachCategorySwipe(scroll)
        column.addView(scroll)
        return column
    }

    private fun attachCategorySwipe(target: View) {
        val detector = GestureDetector(
            context,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onDown(e: MotionEvent): Boolean = true

                override fun onFling(
                    e1: MotionEvent?,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    if (abs(velocityX) <= abs(velocityY) || abs(velocityX) < 800f) return false
                    if (velocityX < 0) switchEmojiTab(1) else switchEmojiTab(-1)
                    return true
                }
            }
        )
        // Observe touches for horizontal flings without blocking vertical ScrollView.
        target.setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            false
        }
    }

    private fun switchEmojiTab(delta: Int) {
        val index = emojiTabOrder.indexOf(emojiTabId).coerceAtLeast(0)
        val next = (index + delta).coerceIn(0, emojiTabOrder.lastIndex)
        if (next == index) return
        emojiTabId = emojiTabOrder[next]
        renderPanel()
    }

    private fun categoryChip(id: String, label: String, selected: Boolean): TextView {
        return TextView(context).apply {
            text = label
            gravity = Gravity.CENTER
            setTextColor(if (selected) tokens.keyText else tokens.suggestionText)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, if (label.length <= 2) 18f else 13f)
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = if (selected) {
                rounded(tokens.keyBackground, dp(14))
            } else {
                null
            }
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginEnd = dp(4)
            }
            setOnClickListener {
                emojiTabId = id
                renderPanel()
            }
        }
    }

    private fun emojiListForTab(tabId: String): List<String> {
        if (tabId == TAB_RECENT) return recentEmojis
        return EmojiCatalog.categories.firstOrNull { it.id == tabId }?.emojis.orEmpty()
    }

    private fun commitEmoji(emoji: String) {
        controller?.commitRawText(emoji)
        // Optimistic local MRU so Recent updates without fighting the insert.
        recentEmojis = (listOf(emoji) + recentEmojis.filter { it != emoji }).take(32)
        viewScope.launch {
            recentEmojiStore?.record(emoji)
        }
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
        private const val PANEL_HEIGHT_DP = 240
        private const val TOOLBAR_HEIGHT_DP = 40
        private const val TAB_RECENT = "recent"
    }
}
