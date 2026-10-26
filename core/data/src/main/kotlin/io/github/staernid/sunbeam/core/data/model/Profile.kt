package io.github.staernid.sunbeam.core.data.model

import java.util.UUID

/**
 * Domain entity representing a custom settings and control overlay profile.
 */
data class Profile(
    val uuid: UUID = UUID.randomUUID(),
    val name: String,
    val createdUtc: Long = System.currentTimeMillis(),
    val modifiedUtc: Long = System.currentTimeMillis(),
    val options: Map<String, Any> = emptyMap(),
    val isActive: Boolean = false,
)
