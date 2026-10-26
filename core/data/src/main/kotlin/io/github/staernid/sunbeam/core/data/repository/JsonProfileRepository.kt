@file:Suppress("TooGenericExceptionCaught", "SwallowedException")

package io.github.staernid.sunbeam.core.data.repository

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.github.staernid.sunbeam.core.data.model.Profile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.util.UUID

/**
 * Profile repository backed by the legacy JSON storage format (filesDir/profiles/profiles.json)
 * to maintain 100% interoperability with existing profiles and customizations.
 */
class JsonProfileRepository(
    private val profilesFile: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ProfileRepository {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    private val _profilesFlow = MutableStateFlow<List<Profile>>(emptyList())
    override val profilesFlow: Flow<List<Profile>> = _profilesFlow.asStateFlow()

    private val _activeProfileFlow = MutableStateFlow<Profile?>(null)
    override val activeProfileFlow: Flow<Profile?> = _activeProfileFlow.asStateFlow()

    private var activeProfileId: UUID? = null

    suspend fun initialize() =
        withContext(ioDispatcher) {
            loadFromFile()
        }

    override suspend fun getAllProfiles(): List<Profile> =
        withContext(ioDispatcher) {
            _profilesFlow.value
        }

    override suspend fun getProfile(uuid: UUID): Profile? =
        withContext(ioDispatcher) {
            _profilesFlow.value.firstOrNull { it.uuid == uuid }
        }

    override suspend fun saveProfile(profile: Profile): Unit =
        withContext(ioDispatcher) {
            val current = _profilesFlow.value.toMutableList()
            val index = current.indexOfFirst { it.uuid == profile.uuid }
            val toSave = profile.copy(isActive = profile.uuid == activeProfileId)
            if (index >= 0) {
                current[index] = toSave
            } else {
                current.add(toSave)
            }
            _profilesFlow.value = current
            if (activeProfileId == profile.uuid) {
                _activeProfileFlow.value = toSave
            }
            saveToFile()
        }

    override suspend fun deleteProfile(uuid: UUID): Unit =
        withContext(ioDispatcher) {
            val current = _profilesFlow.value.filter { it.uuid != uuid }
            _profilesFlow.value = current
            if (activeProfileId == uuid) {
                activeProfileId = null
                _activeProfileFlow.value = null
            }
            saveToFile()
        }

    override suspend fun setActiveProfile(uuid: UUID?): Unit =
        withContext(ioDispatcher) {
            activeProfileId = uuid
            val updatedList = _profilesFlow.value.map { it.copy(isActive = it.uuid == uuid) }
            _profilesFlow.value = updatedList
            _activeProfileFlow.value = updatedList.firstOrNull { it.uuid == uuid }
            saveToFile()
        }

    private fun loadFromFile() {
        if (!profilesFile.exists()) {
            _profilesFlow.value = emptyList()
            _activeProfileFlow.value = null
            return
        }

        try {
            FileReader(profilesFile).use { reader ->
                val type = object : TypeToken<ProfilesData>() {}.type
                val data: ProfilesData? = gson.fromJson(reader, type)
                if (data?.profiles != null) {
                    activeProfileId = data.activeProfileId
                    val mapped =
                        data.profiles.map { raw ->
                            Profile(
                                uuid = raw.uuid,
                                name = raw.name,
                                createdUtc = raw.createdUtc,
                                modifiedUtc = raw.modifiedUtc,
                                options = raw.options ?: emptyMap(),
                                isActive = raw.uuid == activeProfileId,
                            )
                        }
                    _profilesFlow.value = mapped
                    _activeProfileFlow.value = mapped.firstOrNull { it.uuid == activeProfileId }
                }
            }
        } catch (e: Exception) {
            _profilesFlow.value = emptyList()
            _activeProfileFlow.value = null
        }
    }

    private fun saveToFile() {
        try {
            profilesFile.parentFile?.mkdirs()
            FileWriter(profilesFile).use { writer ->
                val rawProfiles =
                    _profilesFlow.value.map { profile ->
                        RawProfile(
                            uuid = profile.uuid,
                            name = profile.name,
                            createdUtc = profile.createdUtc,
                            modifiedUtc = profile.modifiedUtc,
                            options = profile.options,
                        )
                    }
                val data =
                    ProfilesData(
                        profiles = rawProfiles,
                        activeProfileId = activeProfileId,
                    )
                gson.toJson(data, writer)
            }
        } catch (e: Exception) {
            // Ignore write errors
        }
    }

    private data class ProfilesData(
        val profiles: List<RawProfile> = emptyList(),
        val activeProfileId: UUID? = null,
    )

    private data class RawProfile(
        val uuid: UUID,
        val name: String,
        val createdUtc: Long,
        val modifiedUtc: Long,
        val options: Map<String, Any>?,
    )

    companion object {
        const val PROFILES_DIR = "profiles"
        const val PROFILES_FILE = "profiles.json"
    }
}
