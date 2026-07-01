package com.example.stocktracker.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AppPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val darkThemeKey = booleanPreferencesKey("is_dark_theme")
    private val marketFetchedAtKey = longPreferencesKey("market_last_fetched_at")

    fun observeIsDarkTheme(): Flow<Boolean> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[darkThemeKey] ?: true }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { it[darkThemeKey] = enabled }
    }

    suspend fun getMarketLastFetchedAt(): Long = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { it[marketFetchedAtKey] ?: 0L }
        .first()

    suspend fun setMarketLastFetchedAt(timeMs: Long) {
        dataStore.edit { it[marketFetchedAtKey] = timeMs }
    }
}
