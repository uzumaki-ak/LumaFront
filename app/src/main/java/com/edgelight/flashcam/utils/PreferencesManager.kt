package com.edgelight.flashcam.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * PreferencesManager - Enhanced with gradient support
 *
 * NEW:
 * - Gradient color storage
 * - Multiple color preferences
 * - Intensity settings
 */

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    companion object {
        private val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        private val GLOW_COLOR = intPreferencesKey("glow_color")
        private val GLOW_INTENSITY = intPreferencesKey("glow_intensity")

        // NEW: Gradient support
        private val IS_GRADIENT = booleanPreferencesKey("is_gradient")
        private val GRADIENT_COLORS = stringPreferencesKey("gradient_colors")
    }

    val serviceEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SERVICE_ENABLED] ?: false
    }

    val glowColorFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[GLOW_COLOR] ?: 0xFFFFF4E6.toInt()  // Default warm white
    }

    val glowIntensityFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[GLOW_INTENSITY] ?: 100  // Default 100%
    }

    val isGradientFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[IS_GRADIENT] ?: false
    }

    val gradientColorsFlow: Flow<List<Int>> = context.dataStore.data.map { preferences ->
        val colorsString = preferences[GRADIENT_COLORS] ?: ""
        if (colorsString.isEmpty()) {
            emptyList()
        } else {
            colorsString.split(",").map { it.toInt() }
        }
    }

    suspend fun setServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SERVICE_ENABLED] = enabled
        }
    }

    suspend fun setGlowColor(color: Int) {
        context.dataStore.edit { preferences ->
            preferences[GLOW_COLOR] = color
            preferences[IS_GRADIENT] = false  // Reset gradient when solid color selected
        }
    }

    suspend fun setGlowIntensity(intensity: Int) {
        context.dataStore.edit { preferences ->
            preferences[GLOW_INTENSITY] = intensity.coerceIn(0, 100)
        }
    }

    suspend fun setIsGradient(isGradient: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_GRADIENT] = isGradient
        }
    }

    suspend fun setGradientColors(colors: List<Int>) {
        context.dataStore.edit { preferences ->
            preferences[GRADIENT_COLORS] = colors.joinToString(",")
        }
    }
}