package io.github.staernid.sunbeam.core.preferences

import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import kotlinx.coroutines.flow.Flow

/**
 * Access and mutation contract for typed stream and application settings.
 */
interface PreferencesRepository {
    /**
     * Flow of current stream settings, re-emitted on any preference change.
     */
    val settingsFlow: Flow<StreamSettings>

    /**
     * Reads a snapshot of current settings.
     */
    suspend fun getSettings(): StreamSettings

    /**
     * Atomically mutates settings using a transform lambda.
     */
    suspend fun updateSettings(transform: (StreamSettings) -> StreamSettings): StreamSettings

    /**
     * Replaces settings with the given [StreamSettings].
     */
    suspend fun setSettings(settings: StreamSettings)
}
