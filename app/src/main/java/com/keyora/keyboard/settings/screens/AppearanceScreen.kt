package com.keyora.keyboard.settings.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.keyora.keyboard.theme.KeyboardThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceScreen(
    current: KeyboardThemeMode,
    onBack: () -> Unit,
    onSelect: (KeyboardThemeMode) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
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
        ) {
            Text(
                text = "Keyboard Theme",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            KeyboardThemeMode.entries.forEach { mode ->
                ListItem(
                    headlineContent = { Text(mode.displayName()) },
                    leadingContent = {
                        RadioButton(selected = mode == current, onClick = null)
                    },
                    modifier = Modifier.selectable(
                        selected = mode == current,
                        onClick = { onSelect(mode) },
                        role = Role.RadioButton
                    )
                )
            }
        }
    }
}
