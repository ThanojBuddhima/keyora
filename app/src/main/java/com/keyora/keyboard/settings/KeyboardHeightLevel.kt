package com.keyora.keyboard.settings

enum class KeyboardHeightLevel {
    SMALL,
    MEDIUM,
    LARGE;

    fun keyHeightDp(): Int = when (this) {
        SMALL -> 36
        MEDIUM -> 42
        LARGE -> 50
    }

    fun rowGapDp(): Int = when (this) {
        SMALL -> 8
        MEDIUM -> 10
        LARGE -> 12
    }

    fun next(): KeyboardHeightLevel = when (this) {
        SMALL -> MEDIUM
        MEDIUM -> LARGE
        LARGE -> SMALL
    }

    fun label(): String = when (this) {
        SMALL -> "S"
        MEDIUM -> "M"
        LARGE -> "L"
    }
}
