package com.keyora.keyboard.settings.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.keyora.keyboard.settings.AppThemeListItem
import com.keyora.keyboard.theme.KeyboardThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThemesScreen(
    items: List<AppThemeListItem>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Themes") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Filled.Add, contentDescription = "Add App")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Text(
                text = "Choose how Keyora looks in each app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp)
            )
            if (items.isEmpty()) {
                Text(
                    text = "No app rules yet. Tap + to add an app.",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(items, key = { it.packageName }) { item ->
                        ListItem(
                            headlineContent = { Text(item.label) },
                            supportingContent = {
                                Text(
                                    when (item.theme) {
                                        KeyboardThemeMode.DARK -> "Dark"
                                        KeyboardThemeMode.LIGHT -> "Light"
                                        KeyboardThemeMode.SYSTEM_DEFAULT -> "System"
                                    }
                                )
                            },
                            trailingContent = {
                                IconButton(onClick = { onRemove(item.packageName) }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = "Remove")
                                }
                            },
                            modifier = Modifier.clickable { onOpen(item.packageName) }
                        )
                    }
                }
            }
        }
    }
}
