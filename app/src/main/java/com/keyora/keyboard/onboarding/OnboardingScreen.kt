package com.keyora.keyboard.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    imeEnabled: Boolean,
    imeSelected: Boolean,
    onEnable: () -> Unit,
    onSelect: () -> Unit,
    onRefresh: () -> Unit,
    onFinished: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> OnboardingPage(
                        title = "Meet Keyora",
                        body = "An elegant, privacy-first keyboard for Android."
                    )
                    1 -> OnboardingPage(
                        title = "Personalize Every App",
                        body = "Choose a different keyboard theme for each app."
                    )
                    2 -> OnboardingPage(
                        title = "Private by Design",
                        body = "Your typed text stays on your device."
                    )
                    else -> EnablePage(
                        imeEnabled = imeEnabled,
                        imeSelected = imeSelected,
                        onEnable = {
                            onEnable()
                            onRefresh()
                        },
                        onSelect = {
                            onSelect()
                            onRefresh()
                        },
                        onRefresh = onRefresh
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (pagerState.currentPage < 3) {
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Continue")
                }
            } else {
                Button(
                    onClick = onFinished,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = imeEnabled
                ) {
                    Text(if (imeSelected) "Get started" else "Continue to settings")
                }
                if (!imeEnabled) {
                    Text(
                        text = "Enable Keyora in system settings to continue.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingPage(title: String, body: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EnablePage(
    imeEnabled: Boolean,
    imeSelected: Boolean,
    onEnable: () -> Unit,
    onSelect: () -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Enable Keyora",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Turn on Keyora in Android keyboard settings, then select it as your keyboard. Keyora never changes the default keyboard without your action.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onEnable, modifier = Modifier.fillMaxWidth()) {
            Text(if (imeEnabled) "Enabled — open settings again" else "Enable Keyora")
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onSelect,
            modifier = Modifier.fillMaxWidth(),
            enabled = imeEnabled
        ) {
            Text(if (imeSelected) "Selected as keyboard" else "Select Keyora as Keyboard")
        }
        TextButton(onClick = onRefresh) {
            Text("Refresh status")
        }
        Text(
            text = "Status: ${if (imeEnabled) "enabled" else "not enabled"} · ${if (imeSelected) "selected" else "not selected"}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
