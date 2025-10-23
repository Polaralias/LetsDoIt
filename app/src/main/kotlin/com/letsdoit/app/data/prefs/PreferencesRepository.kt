package com.letsdoit.app.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.letsdoit.app.ui.theme.AccentPack
import com.letsdoit.app.ui.theme.CardShapeFamily
import com.letsdoit.app.ui.theme.PaletteFamily
import com.letsdoit.app.ui.theme.ThemeConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class PreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val paletteKey = stringPreferencesKey("palette_family")
    private val accentKey = stringPreferencesKey("accent_pack")
    private val shapeKey = stringPreferencesKey("card_shape")

    val themeConfig: Flow<ThemeConfig> = dataStore.data.map { preferences ->
        ThemeConfig(
            paletteFamily = preferences[paletteKey]?.let { PaletteFamily.valueOf(it) } ?: ThemeConfig.Default.paletteFamily,
            accentPack = preferences[accentKey]?.let { AccentPack.valueOf(it) } ?: ThemeConfig.Default.accentPack,
            cardShapeFamily = preferences[shapeKey]?.let { CardShapeFamily.valueOf(it) } ?: ThemeConfig.Default.cardShapeFamily
        )
    }

    suspend fun updateTheme(config: ThemeConfig) {
        dataStore.edit { preferences ->
            preferences[paletteKey] = config.paletteFamily.name
            preferences[accentKey] = config.accentPack.name
            preferences[shapeKey] = config.cardShapeFamily.name
        }
    }
}
