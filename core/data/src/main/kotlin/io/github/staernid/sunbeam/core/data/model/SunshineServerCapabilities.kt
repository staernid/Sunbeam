@file:Suppress("MagicNumber")

package io.github.staernid.sunbeam.core.data.model

/**
 * Feature flags, codec capabilities, and metadata reported by official Sunshine servers in /serverinfo.
 */
data class SunshineServerCapabilities(
    val serverCodecModeSupport: Int = 0,
    val appVersion: String? = null,
    val gfeVersion: String? = null,
    val gpuType: String? = null,
    val maxLumaPixelsHevc: Long = 0L,
    val isSunshine: Boolean = true,
) {
    val supportsH264: Boolean get() = (serverCodecModeSupport and SCM_MASK_H264) != 0
    val supportsHevc: Boolean get() = (serverCodecModeSupport and SCM_MASK_HEVC) != 0
    val supportsAv1: Boolean get() = (serverCodecModeSupport and SCM_MASK_AV1) != 0
    val supports10Bit: Boolean get() = (serverCodecModeSupport and SCM_MASK_10BIT) != 0
    val supportsYuv444: Boolean get() = (serverCodecModeSupport and SCM_MASK_YUV444) != 0

    val supportsHevcMain10: Boolean get() = (serverCodecModeSupport and SCM_HEVC_MAIN10) != 0
    val supportsAv1Main10: Boolean get() = (serverCodecModeSupport and SCM_AV1_MAIN10) != 0

    companion object {
        const val SCM_H264 = 0x00000001
        const val SCM_HEVC = 0x00000100
        const val SCM_HEVC_MAIN10 = 0x00000200
        const val SCM_AV1_MAIN8 = 0x00010000
        const val SCM_AV1_MAIN10 = 0x00020000
        const val SCM_H264_HIGH8_444 = 0x00040000
        const val SCM_HEVC_REXT8_444 = 0x00080000
        const val SCM_HEVC_REXT10_444 = 0x00100000
        const val SCM_AV1_HIGH8_444 = 0x00200000
        const val SCM_AV1_HIGH10_444 = 0x00400000

        const val SCM_MASK_H264 = SCM_H264 or SCM_H264_HIGH8_444
        const val SCM_MASK_HEVC = SCM_HEVC or SCM_HEVC_MAIN10 or SCM_HEVC_REXT8_444 or SCM_HEVC_REXT10_444
        const val SCM_MASK_AV1 = SCM_AV1_MAIN8 or SCM_AV1_MAIN10 or SCM_AV1_HIGH8_444 or SCM_AV1_HIGH10_444
        const val SCM_MASK_10BIT = SCM_HEVC_MAIN10 or SCM_HEVC_REXT10_444 or SCM_AV1_MAIN10 or SCM_AV1_HIGH10_444
        const val SCM_MASK_YUV444 =
            SCM_H264_HIGH8_444 or SCM_HEVC_REXT8_444 or SCM_HEVC_REXT10_444 or
                SCM_AV1_HIGH8_444 or SCM_AV1_HIGH10_444

        val DEFAULT = SunshineServerCapabilities()
    }
}
