package com.keyora.keyboard.settings

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.keyora.keyboard.onboarding.OnboardingScreen
import com.keyora.keyboard.settings.screens.AboutScreen
import com.keyora.keyboard.settings.screens.AppPickerScreen
import com.keyora.keyboard.settings.screens.AppThemeDetailScreen
import com.keyora.keyboard.settings.screens.AppThemesScreen
import com.keyora.keyboard.settings.screens.AppearanceScreen
import com.keyora.keyboard.settings.screens.HomeScreen
import com.keyora.keyboard.settings.screens.KeyboardLayoutScreen
import com.keyora.keyboard.settings.screens.LanguageScreen
import com.keyora.keyboard.settings.screens.OpenSourceScreen
import com.keyora.keyboard.settings.screens.PrivacyScreen
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.refreshImeStatus()
            }
        }
        setContent {
            KeyoraSettingsTheme {
                val state by viewModel.uiState.collectAsState()
                if (!state.isLoaded) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    KeyoraNavHost(
                        state = state,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyoraSettingsTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) {
        darkColorScheme(primary = Color(0xFF0A84FF))
    } else {
        lightColorScheme(primary = Color(0xFF007AFF))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

object Routes {
    const val Onboarding = "onboarding"
    const val Home = "home"
    const val Appearance = "appearance"
    const val AppThemes = "app_themes"
    const val AppPicker = "app_picker"
    const val AppThemeDetail = "app_theme_detail?pkg={pkg}"
    const val Layout = "layout"
    const val Language = "language"
    const val About = "about"
    const val Privacy = "privacy"
    const val OpenSource = "open_source"

    fun appThemeDetail(packageName: String): String =
        "app_theme_detail?pkg=${Uri.encode(packageName)}"
}

@Composable
fun KeyoraNavHost(
    state: SettingsUiState,
    viewModel: SettingsViewModel
) {
    val navController = rememberNavController()
    val start = if (state.onboardingComplete) Routes.Home else Routes.Onboarding

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.Onboarding) {
            OnboardingScreen(
                imeEnabled = state.imeEnabled,
                imeSelected = state.imeSelected,
                onEnable = viewModel::openEnableSettings,
                onSelect = viewModel::showPicker,
                onRefresh = viewModel::refreshImeStatus,
                onFinished = {
                    viewModel.completeOnboarding()
                    navController.navigate(Routes.Home) {
                        popUpTo(Routes.Onboarding) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.Home) {
            HomeScreen(
                state = state,
                onAppearance = { navController.navigate(Routes.Appearance) },
                onAppThemes = { navController.navigate(Routes.AppThemes) },
                onLayout = { navController.navigate(Routes.Layout) },
                onLanguage = { navController.navigate(Routes.Language) },
                onAbout = { navController.navigate(Routes.About) },
                onPrivacy = { navController.navigate(Routes.Privacy) },
                onOpenSource = { navController.navigate(Routes.OpenSource) },
                onEnable = viewModel::openEnableSettings,
                onSelect = viewModel::showPicker,
                onRefresh = viewModel::refreshImeStatus
            )
        }
        composable(Routes.Appearance) {
            AppearanceScreen(
                current = state.themeSettings.globalTheme,
                onBack = { navController.popBackStack() },
                onSelect = viewModel::setGlobalTheme
            )
        }
        composable(Routes.AppThemes) {
            AppThemesScreen(
                items = state.appThemeItems,
                onBack = { navController.popBackStack() },
                onAdd = {
                    viewModel.refreshInstalledApps()
                    navController.navigate(Routes.AppPicker)
                },
                onOpen = { pkg -> navController.navigate(Routes.appThemeDetail(pkg)) },
                onRemove = viewModel::removeThemeForPackage
            )
        }
        composable(Routes.AppPicker) {
            AppPickerScreen(
                apps = state.installedApps,
                existingRules = state.themeSettings.appRules.keys,
                onBack = { navController.popBackStack() },
                onPick = { pkg ->
                    navController.navigate(Routes.appThemeDetail(pkg)) {
                        popUpTo(Routes.AppThemes)
                    }
                }
            )
        }
        composable(
            route = Routes.AppThemeDetail,
            arguments = listOf(navArgument("pkg") { type = NavType.StringType })
        ) { entry ->
            val packageName = Uri.decode(entry.arguments?.getString("pkg").orEmpty())
            val current = state.themeSettings.appRules[packageName]
                ?: com.keyora.keyboard.theme.KeyboardThemeMode.SYSTEM_DEFAULT
            val label = state.installedApps.find { it.packageName == packageName }?.label
                ?: state.appThemeItems.find { it.packageName == packageName }?.label
                ?: packageName
            AppThemeDetailScreen(
                packageName = packageName,
                label = label,
                current = current,
                onBack = { navController.popBackStack() },
                onSave = { mode ->
                    viewModel.setThemeForPackage(packageName, mode)
                    navController.popBackStack(Routes.AppThemes, inclusive = false)
                },
                onRemove = {
                    viewModel.removeThemeForPackage(packageName)
                    navController.popBackStack(Routes.AppThemes, inclusive = false)
                }
            )
        }
        composable(Routes.Layout) {
            KeyboardLayoutScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Language) {
            LanguageScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.About) {
            AboutScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.Privacy) {
            PrivacyScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.OpenSource) {
            OpenSourceScreen(onBack = { navController.popBackStack() })
        }
    }
}
