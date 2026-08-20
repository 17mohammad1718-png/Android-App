package com.dataguard.app.data.settings

import android.content.Context
import com.dataguard.app.domain.model.AppSettings
import com.dataguard.app.domain.model.DisplayUnit
import com.dataguard.app.domain.model.ThemeMode
import com.dataguard.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple, local-first settings backed by SharedPreferences. A full DataStore
 * migration can be done later if settings grow beyond a couple of toggles.
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    override val settings: Flow<AppSettings> = _settings.asStateFlow()

    private fun read(): AppSettings = AppSettings(
        themeMode = enumValueOrDefault(prefs.getString(KEY_THEME, null), ThemeMode.SYSTEM),
        displayUnit = enumValueOrDefault(prefs.getString(KEY_UNIT, null), DisplayUnit.AUTO),
    )

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME, mode.name).apply()
        _settings.value = read()
    }

    override suspend fun setDisplayUnit(unit: DisplayUnit) {
        prefs.edit().putString(KEY_UNIT, unit.name).apply()
        _settings.value = read()
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String?, default: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: default

    private companion object {
        const val KEY_THEME = "theme_mode"
        const val KEY_UNIT = "display_unit"
    }
}
