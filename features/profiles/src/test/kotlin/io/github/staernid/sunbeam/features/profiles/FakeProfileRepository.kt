package io.github.staernid.sunbeam.features.profiles

import io.github.staernid.sunbeam.core.data.model.Profile
import io.github.staernid.sunbeam.core.data.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class FakeProfileRepository(
    initialProfiles: List<Profile> = emptyList(),
    initialActiveId: UUID? = null,
) : ProfileRepository {
    private val _profilesFlow = MutableStateFlow(initialProfiles)
    override val profilesFlow: Flow<List<Profile>> = _profilesFlow.asStateFlow()

    private val _activeProfileFlow = MutableStateFlow(initialProfiles.firstOrNull { it.uuid == initialActiveId })
    override val activeProfileFlow: Flow<Profile?> = _activeProfileFlow.asStateFlow()

    private var activeId: UUID? = initialActiveId

    override suspend fun getAllProfiles(): List<Profile> = _profilesFlow.value

    override suspend fun getProfile(uuid: UUID): Profile? =
        _profilesFlow.value.firstOrNull { it.uuid == uuid }

    override suspend fun saveProfile(profile: Profile) {
        val current = _profilesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.uuid == profile.uuid }
        val toSave = profile.copy(isActive = profile.uuid == activeId)
        if (index >= 0) {
            current[index] = toSave
        } else {
            current.add(toSave)
        }
        _profilesFlow.value = current
        if (activeId == profile.uuid) {
            _activeProfileFlow.value = toSave
        }
    }

    override suspend fun deleteProfile(uuid: UUID) {
        val current = _profilesFlow.value.filter { it.uuid != uuid }
        _profilesFlow.value = current
        if (activeId == uuid) {
            activeId = null
            _activeProfileFlow.value = null
        }
    }

    override suspend fun setActiveProfile(uuid: UUID?) {
        activeId = uuid
        val updated = _profilesFlow.value.map { it.copy(isActive = it.uuid == uuid) }
        _profilesFlow.value = updated
        _activeProfileFlow.value = updated.firstOrNull { it.uuid == uuid }
    }
}
