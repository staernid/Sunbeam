package io.github.staernid.sunbeam.core.stream.model

import java.security.cert.X509Certificate

data class StreamConfig(
    val hostAddress: String,
    val httpsPort: Int,
    val width: Int,
    val height: Int,
    val fps: Int,
    val bitrateKbps: Int,
    val packetSize: Int = 1024,
    val audioConfiguration: AudioChannelConfiguration = AudioChannelConfiguration.STEREO,
    val videoFormat: VideoFormat = VideoFormat.AUTO,
    val enableHdr: Boolean = false,
    val clientCert: X509Certificate? = null,
)

enum class AudioChannelConfiguration {
    STEREO,
    SURROUND_51,
    SURROUND_71,
}

enum class VideoFormat {
    AUTO,
    H264,
    HEVC,
    AV1,
}
