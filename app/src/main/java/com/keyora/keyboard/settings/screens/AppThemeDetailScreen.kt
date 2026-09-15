package com.keyora.keyboard.settings.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.keyora.keyboard.theme.KeyboardThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThemeDetailScreen(
    packageName: String,
    label: String,
    current: KeyboardThemeMode,
    onBack: () -> Unit,
    onSave: (KeyboardThemeMode) -> Unit,
    onRemove: () -> Unit
) {
    var selected by remember(packageName, current) { mutableStateOf(current) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(label) },
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
            Text(text = packageName)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Theme")
            Spacer(modifier = Modifier.height(8.dp))
            KeyboardThemeMode.entries.forEach { mode ->
                ListItem(
                    headlineContent = { Text(mode.displayName()) },
                    leadingContent = {
                        RadioButton(selected = mode == selected, onClick = null)
                    },
                    modifier = Modifier.selectable(
                        selected = mode == selected,
                        onClick = { selected = mode },
                        role = Role.RadioButton
                    )
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onSave(selected) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onRemove,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Remove rule")
            }
        }
    }
}
