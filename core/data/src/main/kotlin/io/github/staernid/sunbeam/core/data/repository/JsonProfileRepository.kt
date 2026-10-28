@file:Suppress("TooGenericExceptionCaught", "SwallowedException")

package io.github.staernid.sunbeam.core.data.repository

import io.github.staernid.sunbeam.core.data.model.Profile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import java.io.File
import java.util.UUID

/**
 * Profile repository backed by the legacy JSON storage format (filesDir/profiles/profiles.json)
 * to maintain 100% interoperability with existing profiles and customizations.
 */
class JsonProfileRepository(
    private val profilesFile: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ProfileRepository {
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
            val rawText = profilesFile.readText()
            if (rawText.isBlank()) {
                _profilesFlow.value = emptyList()
                _activeProfileFlow.value = null
                return
            }
            val (mapped, loadedActiveId) = decodeProfilesJson(rawText)
            activeProfileId = loadedActiveId
            _profilesFlow.value = mapped
            _activeProfileFlow.value = mapped.firstOrNull { it.uuid == activeProfileId }
        } catch (e: Exception) {
            _profilesFlow.value = emptyList()
            _activeProfileFlow.value = null
        }
    }

    private fun saveToFile() {
        try {
            profilesFile.parentFile?.mkdirs()
            val encoded = encodeProfilesJson(_profilesFlow.value, activeProfileId)
            profilesFile.writeText(encoded)
        } catch (e: Exception) {
            // Ignore write errors
        }
    }

    @Serializable
    private data class ProfilesData(
        val profiles: List<RawProfile> = emptyList(),
        val activeProfileId: String? = null,
    )

    @Serializable
    private data class RawProfile(
        val uuid: String,
        val name: String,
        val createdUtc: Long = 0L,
        val modifiedUtc: Long = 0L,
        val options: Map<String, JsonElement>? = null,
    )

    companion object {
        const val PROFILES_DIR = "profiles"
        const val PROFILES_FILE = "profiles.json"

        private val json =
            Json {
                ignoreUnknownKeys = true
                prettyPrint = true
            }

        @JvmStatic
        fun decodeProfilesJson(rawJson: String): Pair<List<Profile>, UUID?> {
            val data = json.decodeFromString<ProfilesData>(rawJson)
            val parsedActiveId = data.activeProfileId?.let { UUID.fromString(it) }
            val mapped =
                data.profiles.map { raw ->
                    val parsedUuid = UUID.fromString(raw.uuid)
                    val parsedOptions =
                        raw.options
                            ?.mapNotNull { (key, value) ->
                                value.toPrimitiveOrCollection()?.let { key to it }
                            }?.toMap()
                            ?: emptyMap()
                    Profile(
                        uuid = parsedUuid,
                        name = raw.name,
                        createdUtc = raw.createdUtc,
                        modifiedUtc = raw.modifiedUtc,
                        options = parsedOptions,
                        isActive = parsedUuid == parsedActiveId,
                    )
                }
            return mapped to parsedActiveId
        }

        @JvmStatic
        fun encodeProfilesJson(
            profiles: List<Profile>,
            activeProfileId: UUID?,
        ): String {
            val rawProfiles =
                profiles.map { profile ->
                    RawProfile(
                        uuid = profile.uuid.toString(),
                        name = profile.name,
                        createdUtc = profile.createdUtc,
                        modifiedUtc = profile.modifiedUtc,
                        options = profile.options.mapValues { (_, v) -> v.toJsonElement() },
                    )
                }
            val data =
                ProfilesData(
                    profiles = rawProfiles,
                    activeProfileId = activeProfileId?.toString(),
                )
            return json.encodeToString(data)
        }

        private fun Any?.toJsonElement(): JsonElement =
            when (this) {
                null -> JsonNull
                is JsonElement -> this
                is Boolean -> JsonPrimitive(this)
                is Number -> JsonPrimitive(this)
                is String -> JsonPrimitive(this)
                is Iterable<*> -> JsonArray(this.map { it.toJsonElement() })
                is Array<*> -> JsonArray(this.map { it.toJsonElement() })
                is Map<*, *> ->
                    JsonObject(
                        this.entries.associate { (k, v) -> k.toString() to v.toJsonElement() },
                    )
                else -> JsonPrimitive(this.toString())
            }

        private fun JsonElement.toPrimitiveOrCollection(): Any? =
            when (this) {
                is JsonNull -> null
                is JsonPrimitive -> {
                    if (isString) {
                        content
                    } else {
                        booleanOrNull ?: longOrNull ?: doubleOrNull ?: content
                    }
                }
                is JsonArray -> mapNotNull { it.toPrimitiveOrCollection()?.toString() }.toSet()
                is JsonObject ->
                    entries
                        .mapNotNull { (k, v) ->
                            v.toPrimitiveOrCollection()?.let { k to it }
                        }.toMap()
            }
    }
}
