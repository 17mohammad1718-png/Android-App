package com.dataguard.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dataguard.app.domain.model.AppSettings
import com.dataguard.app.domain.repository.SettingsRepository
import com.dataguard.app.presentation.navigation.AppNavHost
import com.dataguard.app.presentation.theme.DataGuardTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = AppSettings(),
            )
            DataGuardTheme(themeMode = settings.themeMode) {
                AppNavHost()
            }
        }
    }
}
