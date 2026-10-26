package io.github.staernid.sunbeam.core.data.model

/**
 * IP/host address and port combination for contacting a stream host.
 */
data class HostAddress(
    val address: String,
    val port: Int,
) {
    override fun toString(): String = "$address:$port"
}
