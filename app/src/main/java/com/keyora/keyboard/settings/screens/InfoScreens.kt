package com.keyora.keyboard.settings.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyboardLayoutScreen(onBack: () -> Unit) {
    SimpleInfoScreen(
        title = "Keyboard Layout",
        body = "Keyora currently supports English QWERTY. Additional layouts are planned for a future release.",
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(onBack: () -> Unit) {
    SimpleInfoScreen(
        title = "Language",
        body = "English is available in this version. Multilingual support is on the roadmap.",
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    SimpleInfoScreen(
        title = "About",
        body = "Keyora 1.0.0\n\nAn elegant, privacy-first Android keyboard with per-app Light and Dark themes.\n\nDesign is inspired by modern minimal keyboards — Keyora does not use Apple proprietary assets, icons, or branding.",
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    SimpleInfoScreen(
        title = "Privacy",
        body = "Keyora is privacy-first.\n\n• No cloud processing\n• No analytics in this version\n• No advertising\n• Typed text is never sent to external servers\n• Passwords are not stored or used for suggestions\n• Suggestions are disabled for password fields\n• No AccessibilityService\n• No unnecessary permissions\n\nKeyora does not currently detect whether another app is using Light Mode or Dark Mode internally. Instead, you configure the keyboard theme per application.",
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenSourceScreen(onBack: () -> Unit) {
    SimpleInfoScreen(
        title = "Open Source",
        body = "Keyora is built with AndroidX, Jetpack Compose, Material 3, Kotlin Coroutines, and DataStore.\n\nThese libraries are licensed under Apache License 2.0.",
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleInfoScreen(
    title: String,
    body: String,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(text = body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
