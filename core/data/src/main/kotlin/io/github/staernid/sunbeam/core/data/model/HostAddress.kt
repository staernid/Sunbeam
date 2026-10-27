package io.github.staernid.sunbeam.core.data.model

/**
 * IP/host address and port combination for contacting a stream host.
 */
data class HostAddress(
    val address: String,
    val port: Int = DEFAULT_PORT,
) {
    override fun toString(): String = "$address:$port"

    companion object {
        const val DEFAULT_PORT = 47989
    }
}
