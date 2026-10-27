package io.github.staernid.sunbeam.core.preferences.display

import io.github.staernid.sunbeam.core.preferences.models.Resolution

enum class ResolutionCategory(val headerTitle: String) {
    NATIVE("DEVICE NATIVE"),
    STANDARD_16_9("STANDARD 16:9"),
    HANDHELD_16_10("HANDHELD 16:10"),
    CUSTOM("CUSTOM"),
}

data class ResolutionOption(
    val resolution: Resolution,
    val title: String,
    val subtitle: String? = null,
    val badge: String? = null,
    val category: ResolutionCategory,
)

object ResolutionProvider {
    val STANDARD_16_9 = listOf(
        ResolutionOption(
            resolution = Resolution.RES_4K,
            title = "4K UHD",
            subtitle = "3840x2160 • 16:9",
            category = ResolutionCategory.STANDARD_16_9,
        ),
        ResolutionOption(
            resolution = Resolution.RES_1440P,
            title = "1440p QHD",
            subtitle = "2560x1440 • 16:9",
            category = ResolutionCategory.STANDARD_16_9,
        ),
        ResolutionOption(
            resolution = Resolution.RES_1080P,
            title = "1080p FHD",
            subtitle = "1920x1080 • 16:9",
            category = ResolutionCategory.STANDARD_16_9,
        ),
        ResolutionOption(
            resolution = Resolution.RES_720P,
            title = "720p HD",
            subtitle = "1280x720 • 16:9",
            category = ResolutionCategory.STANDARD_16_9,
        ),
    )

    val HANDHELD_16_10 = listOf(
        ResolutionOption(
            resolution = Resolution.RES_1600P,
            title = "1600p",
            subtitle = "2560x1600 • 16:10",
            category = ResolutionCategory.HANDHELD_16_10,
        ),
        ResolutionOption(
            resolution = Resolution.RES_1200P,
            title = "1200p",
            subtitle = "1920x1200 • 16:10",
            category = ResolutionCategory.HANDHELD_16_10,
        ),
        ResolutionOption(
            resolution = Resolution.RES_800P,
            title = "800p",
            subtitle = "1280x800 • Steam Deck / 16:10",
            category = ResolutionCategory.HANDHELD_16_10,
        ),
    )

    fun getNativeOptions(displayInfo: DeviceDisplayInfo): List<ResolutionOption> {
        val options = mutableListOf<ResolutionOption>()

        val fullRes = displayInfo.fullResolution
        options.add(
            ResolutionOption(
                resolution = fullRes,
                title = "Native Fullscreen",
                subtitle = "Edge-to-edge panel coverage (${fullRes.aspectRatioLabel})",
                badge = "Zero Black Bars",
                category = ResolutionCategory.NATIVE,
            ),
        )

        if (displayInfo.hasCutoutInsets) {
            val avoidantRes = displayInfo.avoidantResolution
            if (avoidantRes != fullRes) {
                options.add(
                    ResolutionOption(
                        resolution = avoidantRes,
                        title = "Native Avoidant",
                        subtitle = "Padded inside camera punchhole & corners",
                        badge = "Punchhole Safe",
                        category = ResolutionCategory.NATIVE,
                    ),
                )
            }
        }

        val halfRes = displayInfo.halvedResolution
        options.add(
            ResolutionOption(
                resolution = halfRes,
                title = "Native Halved",
                subtitle = "Exact aspect match, 4x lighter decoding",
                badge = "Battery Saver",
                category = ResolutionCategory.NATIVE,
            ),
        )

        return options
    }
}
