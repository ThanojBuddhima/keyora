package com.keyora.keyboard

import android.app.Application
import com.keyora.keyboard.settings.SettingsRepository

class KeyoraApp : Application() {
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
    }
}
