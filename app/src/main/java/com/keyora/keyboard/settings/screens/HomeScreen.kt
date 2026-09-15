package com.keyora.keyboard.settings.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keyora.keyboard.settings.SettingsUiState
import com.keyora.keyboard.theme.KeyboardThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: SettingsUiState,
    onAppearance: () -> Unit,
    onAppThemes: () -> Unit,
    onLayout: () -> Unit,
    onLanguage: () -> Unit,
    onAbout: () -> Unit,
    onPrivacy: () -> Unit,
    onOpenSource: () -> Unit,
    onEnable: () -> Unit,
    onSelect: () -> Unit,
    onRefresh: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Keyora", fontWeight = FontWeight.SemiBold) })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Keyboard", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when {
                            state.imeSelected -> "Keyora is enabled and selected"
                            state.imeEnabled -> "Enabled — select Keyora to start typing"
                            else -> "Enable Keyora in system settings"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { onEnable(); onRefresh() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Enable Keyora")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onSelect(); onRefresh() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state.imeEnabled
                    ) {
                        Text("Select Keyora as Keyboard")
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            SectionLabel("Appearance")
            SettingsRow(
                icon = Icons.Outlined.Palette,
                title = "Theme",
                subtitle = state.themeSettings.globalTheme.displayName(),
                onClick = onAppearance
            )
            SettingsRow(
                icon = Icons.Outlined.Apps,
                title = "App Themes",
                subtitle = "Customize theme per application",
                onClick = onAppThemes
            )

            Spacer(modifier = Modifier.height(12.dp))
            SectionLabel("Keyboard")
            SettingsRow(
                icon = Icons.Outlined.Keyboard,
                title = "Keyboard Layout",
                subtitle = "QWERTY",
                onClick = onLayout
            )
            SettingsRow(
                icon = Icons.Outlined.Language,
                title = "Language",
                subtitle = "English",
                onClick = onLanguage
            )

            Spacer(modifier = Modifier.height(12.dp))
            SectionLabel("About")
            SettingsRow(Icons.Outlined.Info, "About", "Keyora 1.0.0", onAbout)
            SettingsRow(Icons.Outlined.Lock, "Privacy", "On-device by design", onPrivacy)
            SettingsRow(Icons.Outlined.Code, "Open Source", "Licenses & notices", onOpenSource)
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null)
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider()
}

fun KeyboardThemeMode.displayName(): String = when (this) {
    KeyboardThemeMode.LIGHT -> "Light"
    KeyboardThemeMode.DARK -> "Dark"
    KeyboardThemeMode.SYSTEM_DEFAULT -> "System Default"
}
