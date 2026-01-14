package com.edgelight.flashcam.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * PreferencesManager - App settings storage
 *
 * PURPOSE: Save user preferences
 * - Enable/disable service
 * - Glow intensity
 * - Glow color
 * - Other settings (future)
 *
 * Uses DataStore for modern, safe storage
 */

// Extension property to get DataStore instance
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        // Preference keys
        private val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        private val GLOW_INTENSITY = intPreferencesKey("glow_intensity")
        private val GLOW_COLOR = intPreferencesKey("glow_color")
    }

    /**
     * Flow of service enabled state
     */
    val serviceEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SERVICE_ENABLED] ?: false
    }

    /**
     * Flow of glow intensity (0-100)
     */
    val glowIntensityFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[GLOW_INTENSITY] ?: 80  // Default: 80%
    }

    /**
     * Flow of glow color
     */
    val glowColorFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[GLOW_COLOR] ?: 0xFFFFF8E1.toInt()  // Default: Warm white
    }

    /**
     * Sets service enabled state
     */
    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SERVICE_ENABLED] = enabled
        }
    }

    /**
     * Sets glow intensity
     */
    suspend fun setGlowIntensity(intensity: Int) {
        context.dataStore.edit { preferences ->
            preferences[GLOW_INTENSITY] = intensity.coerceIn(0, 100)
        }
    }

    /**
     * Sets glow color
     */
    suspend fun setGlowColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[GLOW_COLOR] = color
        }
    }
}
