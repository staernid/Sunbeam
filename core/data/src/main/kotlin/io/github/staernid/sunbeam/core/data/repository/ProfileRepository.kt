package io.github.staernid.sunbeam.core.data.repository

import io.github.staernid.sunbeam.core.data.model.Profile
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Repository interface for custom configuration and overlay profile management.
 */
interface ProfileRepository {
    /**
     * Flow of all available profiles.
     */
    val profilesFlow: Flow<List<Profile>>

    /**
     * Flow of the currently active profile (or null if using default preferences).
     */
    val activeProfileFlow: Flow<Profile?>

    /**
     * Retrieves all profiles.
     */
    suspend fun getAllProfiles(): List<Profile>

    /**
     * Retrieves a profile by its UUID.
     */
    suspend fun getProfile(uuid: UUID): Profile?

    /**
     * Saves or updates a profile.
     */
    suspend fun saveProfile(profile: Profile)

    /**
     * Deletes a profile by its UUID.
     */
    suspend fun deleteProfile(uuid: UUID)

    /**
     * Sets the active profile (or null to clear).
     */
    suspend fun setActiveProfile(uuid: UUID?)
}
