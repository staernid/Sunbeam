package io.github.staernid.sunbeam.features.settings

import io.github.staernid.sunbeam.core.preferences.PreferencesRepository
import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class FakePreferencesRepository(
    initialSettings: StreamSettings = StreamSettings(),
) : PreferencesRepository {
    private val settingsState = MutableStateFlow(initialSettings)

    override val settingsFlow: Flow<StreamSettings> = settingsState.asStateFlow()

    override suspend fun getSettings(): StreamSettings = settingsState.value

    override suspend fun updateSettings(transform: (StreamSettings) -> StreamSettings): StreamSettings {
        settingsState.update(transform)
        return settingsState.value
    }

    override suspend fun setSettings(settings: StreamSettings) {
        settingsState.value = settings
    }
}
