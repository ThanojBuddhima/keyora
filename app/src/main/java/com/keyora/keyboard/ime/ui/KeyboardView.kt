package com.keyora.keyboard.ime.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.KeyboardCapslock
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keyora.keyboard.ime.KeyboardController
import com.keyora.keyboard.ime.KeyboardLayout
import com.keyora.keyboard.ime.KeyboardState
import com.keyora.keyboard.theme.KeyboardColors
import com.keyora.keyboard.theme.ResolvedTheme

private val ACCENTS = mapOf(
    "a" to listOf("à", "á", "â", "ä", "æ", "ã", "å", "ā"),
    "e" to listOf("è", "é", "ê", "ë", "ē", "ė", "ę"),
    "i" to listOf("î", "ï", "í", "ī", "į", "ì"),
    "o" to listOf("ô", "ö", "ò", "ó", "œ", "ø", "ō", "õ"),
    "u" to listOf("û", "ü", "ù", "ú", "ū"),
    "c" to listOf("ç", "ć", "č"),
    "n" to listOf("ñ", "ń"),
    "s" to listOf("ś", "š"),
    "y" to listOf("ÿ")
)

@Composable
fun KeyboardView(
    state: KeyboardState,
    theme: ResolvedTheme,
    suggestions: List<String>,
    controller: KeyboardController,
    modifier: Modifier = Modifier
) {
    val colors = KeyboardColors.forTheme(theme)
    val animatedBg by animateColorAsState(
        targetValue = colors.background,
        animationSpec = tween(180),
        label = "keyboardBg"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .background(animatedBg)
            .padding(bottom = 8.dp, top = 4.dp)
    ) {
        SuggestionBar(
            suggestions = suggestions,
            colors = colors,
            enabled = state.suggestionMode,
            onSuggestion = controller::commitSuggestion
        )
        when (state.currentLayout) {
            KeyboardLayout.LETTERS -> LetterKeyboard(state, colors, controller)
            KeyboardLayout.NUMBERS -> NumberKeyboard(colors, controller)
            KeyboardLayout.SYMBOLS -> SymbolKeyboard(colors, controller)
        }
    }
}

@Composable
fun SuggestionBar(
    suggestions: List<String>,
    colors: KeyboardColors,
    enabled: Boolean,
    onSuggestion: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!enabled || suggestions.isEmpty()) {
            Spacer(modifier = Modifier.weight(1f))
        } else {
        suggestions.forEach { word ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.suggestionBackground)
                    .pointerInput(word) {
                        detectTapGestures(onTap = { onSuggestion(word) })
                    }
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = word,
                    color = colors.suggestionText,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }
        }
        }
    }
}

@Composable
fun LetterKeyboard(
    state: KeyboardState,
    colors: KeyboardColors,
    controller: KeyboardController
) {
    val row1 = listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
    val row2 = listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
    val row3 = listOf("z", "x", "c", "v", "b", "n", "m")

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val keyWidth = maxWidth / 10f
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeyRow(row1, keyWidth, state, colors, controller)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Spacer(modifier = Modifier.width(keyWidth * 0.35f))
                row2.forEach { key ->
                    CharacterKey(
                        label = displayLetter(key, state),
                        width = keyWidth,
                        colors = colors,
                        accents = ACCENTS[key],
                        uppercase = state.lettersUppercase,
                        onTap = { controller.onCharacter(key) },
                        onAccent = { controller.onCharacter(it) }
                    )
                }
                Spacer(modifier = Modifier.width(keyWidth * 0.35f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpecialKey(
                    width = keyWidth * 1.4f,
                    colors = colors,
                    onTap = { controller.onShift() },
                    onLongPress = { controller.onShiftLongPress() }
                ) {
                    Icon(
                        imageVector = if (state.capsLock) {
                            Icons.Outlined.KeyboardCapslock
                        } else {
                            Icons.Outlined.KeyboardArrowUp
                        },
                        contentDescription = "Shift",
                        tint = if (state.shiftEnabled || state.capsLock) {
                            colors.returnKeyBackground
                        } else {
                            colors.specialKeyText
                        }
                    )
                }
                row3.forEach { key ->
                    CharacterKey(
                        label = displayLetter(key, state),
                        width = keyWidth,
                        colors = colors,
                        accents = ACCENTS[key],
                        uppercase = state.lettersUppercase,
                        onTap = { controller.onCharacter(key) },
                        onAccent = { controller.onCharacter(it) }
                    )
                }
                SpecialKey(
                    width = keyWidth * 1.4f,
                    colors = colors,
                    onTap = { controller.onBackspace() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                        contentDescription = "Backspace",
                        tint = colors.specialKeyText
                    )
                }
            }
            BottomRow(
                colors = colors,
                enterLabel = state.enterLabel,
                leftLabel = "123",
                onLeft = { controller.switchToNumbers() },
                onSpace = { controller.onSpace() },
                onEnter = { controller.onEnter() },
                onGlobe = { controller.onGlobe() }
            )
        }
    }
}

@Composable
fun NumberKeyboard(
    colors: KeyboardColors,
    controller: KeyboardController
) {
    val row1 = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    val row2 = listOf("-", "/", ":", ";", "(", ")", "$", "&", "@", "\"")
    val row3 = listOf(".", ",", "?", "!", "'")

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val keyWidth = maxWidth / 10f
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeyRow(row1, keyWidth, KeyboardState(), colors, controller, forceLiteral = true)
            KeyRow(row2, keyWidth, KeyboardState(), colors, controller, forceLiteral = true)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                SpecialKey(
                    width = keyWidth * 1.5f,
                    colors = colors,
                    onTap = { controller.switchToSymbols() }
                ) {
                    KeyLabel("#+=", colors.specialKeyText)
                }
                row3.forEach { key ->
                    CharacterKey(
                        label = key,
                        width = keyWidth * 1.2f,
                        colors = colors,
                        onTap = { controller.onCharacter(key) }
                    )
                }
                SpecialKey(
                    width = keyWidth * 1.5f,
                    colors = colors,
                    onTap = { controller.onBackspace() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                        contentDescription = "Backspace",
                        tint = colors.specialKeyText
                    )
                }
            }
            BottomRow(
                colors = colors,
                enterLabel = "return",
                leftLabel = "ABC",
                onLeft = { controller.switchToLetters() },
                onSpace = { controller.onSpace() },
                onEnter = { controller.onEnter() },
                onGlobe = { controller.onGlobe() }
            )
        }
    }
}

@Composable
fun SymbolKeyboard(
    colors: KeyboardColors,
    controller: KeyboardController
) {
    val row1 = listOf("[", "]", "{", "}", "#", "%", "^", "*", "+", "=")
    val row2 = listOf("_", "\\", "|", "~", "<", ">", "€", "£", "¥", "•")
    val row3 = listOf(".", ",", "?", "!", "'")

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val keyWidth = maxWidth / 10f
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            KeyRow(row1, keyWidth, KeyboardState(), colors, controller, forceLiteral = true)
            KeyRow(row2, keyWidth, KeyboardState(), colors, controller, forceLiteral = true)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                SpecialKey(
                    width = keyWidth * 1.5f,
                    colors = colors,
                    onTap = { controller.switchToNumbers() }
                ) {
                    KeyLabel("123", colors.specialKeyText)
                }
                row3.forEach { key ->
                    CharacterKey(
                        label = key,
                        width = keyWidth * 1.2f,
                        colors = colors,
                        onTap = { controller.onCharacter(key) }
                    )
                }
                SpecialKey(
                    width = keyWidth * 1.5f,
                    colors = colors,
                    onTap = { controller.onBackspace() }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                        contentDescription = "Backspace",
                        tint = colors.specialKeyText
                    )
                }
            }
            BottomRow(
                colors = colors,
                enterLabel = "return",
                leftLabel = "ABC",
                onLeft = { controller.switchToLetters() },
                onSpace = { controller.onSpace() },
                onEnter = { controller.onEnter() },
                onGlobe = { controller.onGlobe() }
            )
        }
    }
}

@Composable
private fun KeyRow(
    keys: List<String>,
    keyWidth: Dp,
    state: KeyboardState,
    colors: KeyboardColors,
    controller: KeyboardController,
    forceLiteral: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        keys.forEach { key ->
            val label = if (forceLiteral) key else displayLetter(key, state)
            CharacterKey(
                label = label,
                width = keyWidth,
                colors = colors,
                accents = if (forceLiteral) null else ACCENTS[key],
                uppercase = !forceLiteral && state.lettersUppercase,
                onTap = { controller.onCharacter(key) },
                onAccent = { controller.onCharacter(it) }
            )
        }
    }
}

@Composable
private fun BottomRow(
    colors: KeyboardColors,
    enterLabel: String,
    leftLabel: String,
    onLeft: () -> Unit,
    onSpace: () -> Unit,
    onEnter: () -> Unit,
    onGlobe: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val unit = maxWidth / 10f
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpecialKey(width = unit * 1.3f, colors = colors, onTap = onLeft) {
                KeyLabel(leftLabel, colors.specialKeyText, 13.sp)
            }
            SpecialKey(width = unit * 1.2f, colors = colors, onTap = onGlobe) {
                Icon(
                    imageVector = Icons.Outlined.Language,
                    contentDescription = "Languages",
                    tint = colors.specialKeyText
                )
            }
            CharacterKey(
                label = "space",
                width = unit * 4.8f,
                colors = colors,
                isSpecial = false,
                fontSize = 14.sp,
                onTap = onSpace
            )
            SpecialKey(
                width = unit * 2.2f,
                colors = colors,
                background = colors.returnKeyBackground,
                onTap = onEnter
            ) {
                KeyLabel(enterLabel, colors.returnKeyText, 13.sp)
            }
        }
    }
}

@Composable
fun CharacterKey(
    label: String,
    width: Dp,
    colors: KeyboardColors,
    accents: List<String>? = null,
    uppercase: Boolean = false,
    isSpecial: Boolean = false,
    fontSize: androidx.compose.ui.unit.TextUnit = 20.sp,
    onTap: () -> Unit,
    onAccent: ((String) -> Unit)? = null
) {
    var pressed by remember { mutableStateOf(false) }
    var showAccents by remember { mutableStateOf(false) }
    val bg = if (isSpecial) colors.specialKeyBackground else colors.keyBackground
    val fg = if (isSpecial) colors.specialKeyText else colors.keyText

    Box(contentAlignment = Alignment.BottomCenter) {
        if (showAccents && !accents.isNullOrEmpty()) {
            AccentPopup(
                accents = accents,
                uppercase = uppercase,
                colors = colors,
                onSelect = {
                    showAccents = false
                    onAccent?.invoke(if (uppercase) it.uppercase() else it)
                },
                onDismiss = { showAccents = false }
            )
        }
        Box(
            modifier = Modifier
                .width(width)
                .height(46.dp)
                .padding(horizontal = 2.dp)
                .shadow(1.dp, RoundedCornerShape(8.dp), ambientColor = colors.keyShadow)
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .then(
                    if (pressed) Modifier.background(colors.pressedOverlay) else Modifier
                )
                .pointerInput(label, accents) {
                    detectTapGestures(
                        onPress = {
                            pressed = true
                            try {
                                awaitRelease()
                            } finally {
                                pressed = false
                            }
                        },
                        onTap = { onTap() },
                        onLongPress = {
                            if (!accents.isNullOrEmpty()) {
                                showAccents = true
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                color = fg,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SpecialKey(
    width: Dp,
    colors: KeyboardColors,
    background: Color = colors.specialKeyBackground,
    onTap: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .width(width)
            .height(46.dp)
            .padding(horizontal = 2.dp)
            .shadow(1.dp, RoundedCornerShape(8.dp), ambientColor = colors.keyShadow)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .then(if (pressed) Modifier.background(colors.pressedOverlay) else Modifier)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        try {
                            awaitRelease()
                        } finally {
                            pressed = false
                        }
                    },
                    onTap = { onTap() },
                    onLongPress = { onLongPress?.invoke() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun KeyLabel(
    text: String,
    color: Color,
    size: androidx.compose.ui.unit.TextUnit = 16.sp
) {
    Text(
        text = text,
        color = color,
        fontSize = size,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun AccentPopup(
    accents: List<String>,
    uppercase: Boolean,
    colors: KeyboardColors,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(bottom = 52.dp)
            .shadow(4.dp, RoundedCornerShape(10.dp))
            .clip(RoundedCornerShape(10.dp))
            .background(colors.keyBackground)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        accents.take(6).forEach { accent ->
            val shown = if (uppercase) accent.uppercase() else accent
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.suggestionBackground)
                    .pointerInput(accent) {
                        detectTapGestures(onTap = { onSelect(accent) })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = shown, color = colors.keyText, fontSize = 18.sp)
            }
        }
    }
}

private fun displayLetter(key: String, state: KeyboardState): String =
    if (state.lettersUppercase) key.uppercase() else key
