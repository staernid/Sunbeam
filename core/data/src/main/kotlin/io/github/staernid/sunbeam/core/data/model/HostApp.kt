package io.github.staernid.sunbeam.core.data.model

/**
 * Domain entity representing an application or game available on a streaming host.
 */
data class HostApp(
    val id: Int,
    val title: String,
    val uuid: String = "",
    val index: Int = 0,
    val isHdrSupported: Boolean = false,
    val isHidden: Boolean = false,
    val boxArtPath: String? = null,
)
