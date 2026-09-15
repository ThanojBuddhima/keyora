package com.keyora.keyboard.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keyora.keyboard.KeyoraApp
import com.keyora.keyboard.appdetector.InstalledAppInfo
import com.keyora.keyboard.appdetector.InstalledAppsRepository
import com.keyora.keyboard.enablement.ImeEnablement
import com.keyora.keyboard.theme.KeyboardThemeMode
import com.keyora.keyboard.theme.ThemeSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AppThemeListItem(
    val packageName: String,
    val label: String,
    val theme: KeyboardThemeMode
)

data class SettingsUiState(
    val themeSettings: ThemeSettings = ThemeSettings(),
    val onboardingComplete: Boolean = false,
    val imeEnabled: Boolean = false,
    val imeSelected: Boolean = false,
    val appThemeItems: List<AppThemeListItem> = emptyList(),
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val isLoaded: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as KeyoraApp).settingsRepository
    private val appsRepository = InstalledAppsRepository(application.packageManager)

    private val imeStatus = MutableStateFlow(
        ImeEnablement.isEnabled(application) to ImeEnablement.isSelected(application)
    )

    private val installedApps = MutableStateFlow(emptyList<InstalledAppInfo>())

    val uiState: StateFlow<SettingsUiState> = combine(
        repository.themeSettings,
        repository.onboardingComplete,
        imeStatus,
        installedApps
    ) { themes, onboarding, ime, apps ->
        val labels = apps.associate { it.packageName to it.label }
        SettingsUiState(
            themeSettings = themes,
            onboardingComplete = onboarding,
            imeEnabled = ime.first,
            imeSelected = ime.second,
            appThemeItems = themes.appRules.map { (pkg, mode) ->
                AppThemeListItem(
                    packageName = pkg,
                    label = labels[pkg] ?: appsRepository.getAppLabel(pkg),
                    theme = mode
                )
            }.sortedBy { it.label.lowercase() },
            installedApps = apps,
            isLoaded = true
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState()
    )

    init {
        refreshInstalledApps()
        refreshImeStatus()
    }

    fun refreshImeStatus() {
        val app = getApplication<Application>()
        imeStatus.value = ImeEnablement.isEnabled(app) to ImeEnablement.isSelected(app)
    }

    fun refreshInstalledApps() {
        viewModelScope.launch {
            installedApps.value = appsRepository.getLaunchableApps()
        }
    }

    fun setGlobalTheme(mode: KeyboardThemeMode) {
        viewModelScope.launch { repository.setGlobalTheme(mode) }
    }

    fun setThemeForPackage(packageName: String, mode: KeyboardThemeMode) {
        viewModelScope.launch { repository.setThemeForPackage(packageName, mode) }
    }

    fun removeThemeForPackage(packageName: String) {
        viewModelScope.launch { repository.removeThemeForPackage(packageName) }
    }

    fun completeOnboarding() {
        viewModelScope.launch { repository.setOnboardingComplete(true) }
    }

    fun openEnableSettings() {
        ImeEnablement.openInputMethodSettings(getApplication())
    }

    fun showPicker() {
        ImeEnablement.showInputMethodPicker(getApplication())
    }
}
