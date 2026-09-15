package com.keyora.keyboard.settings.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.keyora.keyboard.appdetector.InstalledAppInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerScreen(
    apps: List<InstalledAppInfo>,
    existingRules: Set<String>,
    onBack: () -> Unit,
    onPick: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add App") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(apps, key = { it.packageName }) { app ->
                val bitmap = remember(app.packageName) {
                    runCatching {
                        app.icon?.toBitmap(96, 96, Bitmap.Config.ARGB_8888)?.asImageBitmap()
                    }.getOrNull()
                }
                ListItem(
                    headlineContent = { Text(app.label) },
                    supportingContent = {
                        Text(
                            if (app.packageName in existingRules) {
                                "${app.packageName} · already configured"
                            } else {
                                app.packageName
                            }
                        )
                    },
                    leadingContent = {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                        } else {
                            Icon(Icons.Outlined.Android, contentDescription = null)
                        }
                    },
                    modifier = Modifier.clickable { onPick(app.packageName) }
                )
            }
        }
    }
}
