package io.github.staernid.sunbeam.core.data.model

/**
 * Domain entity representing a streaming host PC.
 */
data class Host(
    val uuid: String,
    val name: String,
    val localAddress: HostAddress? = null,
    val remoteAddress: HostAddress? = null,
    val manualAddress: HostAddress? = null,
    val ipv6Address: HostAddress? = null,
    val macAddress: String? = null,
    val serverCertDer: ByteArray? = null,
    val state: HostState = HostState.UNKNOWN,
    val pairState: PairState = PairState.NOT_PAIRED,
    val currentGameId: Int = 0,
    val currentGameUuid: String? = null,
    val capabilities: SunshineServerCapabilities = SunshineServerCapabilities.DEFAULT,
    val httpsPort: Int = 0,
) {
    @Suppress("CyclomaticComplexMethod")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Host) return false

        if (uuid != other.uuid) return false
        if (name != other.name) return false
        if (localAddress != other.localAddress) return false
        if (remoteAddress != other.remoteAddress) return false
        if (manualAddress != other.manualAddress) return false
        if (ipv6Address != other.ipv6Address) return false
        if (macAddress != other.macAddress) return false
        if (serverCertDer != null) {
            if (other.serverCertDer == null) return false
            if (!serverCertDer.contentEquals(other.serverCertDer)) return false
        } else if (other.serverCertDer != null) {
            return false
        }
        if (state != other.state) return false
        if (pairState != other.pairState) return false
        if (currentGameId != other.currentGameId) return false
        if (currentGameUuid != other.currentGameUuid) return false
        if (capabilities != other.capabilities) return false
        if (httpsPort != other.httpsPort) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (localAddress?.hashCode() ?: 0)
        result = 31 * result + (remoteAddress?.hashCode() ?: 0)
        result = 31 * result + (manualAddress?.hashCode() ?: 0)
        result = 31 * result + (ipv6Address?.hashCode() ?: 0)
        result = 31 * result + (macAddress?.hashCode() ?: 0)
        result = 31 * result + (serverCertDer?.contentHashCode() ?: 0)
        result = 31 * result + state.hashCode()
        result = 31 * result + pairState.hashCode()
        result = 31 * result + currentGameId
        result = 31 * result + (currentGameUuid?.hashCode() ?: 0)
        result = 31 * result + capabilities.hashCode()
        result = 31 * result + httpsPort
        return result
    }
}
