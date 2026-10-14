package com.ibc.procrastinapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.themeSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "theme_settings"
)

class ThemePreferences(private val dataStore: DataStore<Preferences>) {
    private val darkThemeKey = booleanPreferencesKey("dark_theme")

    // null conserva el tema del teléfono hasta la primera elección del usuario.
    val darkTheme: Flow<Boolean?> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[darkThemeKey] }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { preferences -> preferences[darkThemeKey] = enabled }
    }
}
