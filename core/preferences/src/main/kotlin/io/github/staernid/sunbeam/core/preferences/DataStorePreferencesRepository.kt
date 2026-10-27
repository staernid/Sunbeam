package io.github.staernid.sunbeam.core.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import io.github.staernid.sunbeam.core.preferences.datastore.StreamSettingsMapper
import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class DataStorePreferencesRepository(
    private val dataStore: DataStore<Preferences>,
    private val sharedPreferences: SharedPreferences? = null,
) : PreferencesRepository {
    override val settingsFlow: Flow<StreamSettings> =
        dataStore.data.map { preferences ->
            StreamSettingsMapper.fromPreferences(preferences)
        }

    override suspend fun getSettings(): StreamSettings {
        return settingsFlow.first()
    }

    override suspend fun updateSettings(transform: (StreamSettings) -> StreamSettings): StreamSettings {
        var result = StreamSettings()
        dataStore.edit { preferences ->
            val current = StreamSettingsMapper.fromPreferences(preferences)
            val updated = transform(current)
            StreamSettingsMapper.toPreferences(updated, preferences)
            result = updated
        }
        sharedPreferences?.let { prefs ->
            StreamSettingsMapper.toSharedPreferences(result, prefs)
        }
        return result
    }

    override suspend fun setSettings(settings: StreamSettings) {
        dataStore.edit { preferences ->
            StreamSettingsMapper.toPreferences(settings, preferences)
        }
        sharedPreferences?.let { prefs ->
            StreamSettingsMapper.toSharedPreferences(settings, prefs)
        }
    }

    companion object {
        const val DATASTORE_FILE_NAME = "sunbeam_settings"

        /**
         * Creates a [DataStorePreferencesRepository] wired with automatic [SharedPreferencesMigration]
         * from legacy SharedPreferences keys and bidirectional synchronization.
         */
        fun create(
            context: Context,
            dataStoreName: String = DATASTORE_FILE_NAME,
            legacySharedPrefsName: String? = null,
            scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        ): DataStorePreferencesRepository {
            val prefsName = legacySharedPrefsName ?: "${context.packageName}_preferences"
            val sharedPrefs = context.getSharedPreferences(prefsName, Context.MODE_PRIVATE)
            val migrations = listOf(SharedPreferencesMigration(context, prefsName))

            val dataStore =
                PreferenceDataStoreFactory.create(
                    migrations = migrations,
                    scope = scope,
                    produceFile = { context.preferencesDataStoreFile(dataStoreName) },
                )
            val repository = DataStorePreferencesRepository(dataStore, sharedPrefs)
            scope.launch {
                val current = repository.getSettings()
                StreamSettingsMapper.toSharedPreferences(current, sharedPrefs)
            }
            return repository
        }
    }
}
