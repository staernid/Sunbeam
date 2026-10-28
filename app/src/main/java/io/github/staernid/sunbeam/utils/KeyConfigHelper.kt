package io.github.staernid.sunbeam.utils

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object KeyConfigHelper {
    private val jsonParser =
        Json {
            ignoreUnknownKeys = true
        }

    @Serializable
    data class ShortcutFile(
        @JvmField val data: List<Shortcut> = emptyList(),
    )

    @Serializable
    data class Shortcut(
        @JvmField val id: String? = null,
        @JvmField val name: String? = null,
        @JvmField val sticky: Boolean = false,
        @JvmField val keys: List<String> = emptyList(),
    )

    @JvmStatic
    fun parseShortcutFile(json: String?): ShortcutFile? {
        if (json.isNullOrBlank() || json.trim() == "null") {
            return null
        }
        return jsonParser.decodeFromString<ShortcutFile>(json)
    }
}
