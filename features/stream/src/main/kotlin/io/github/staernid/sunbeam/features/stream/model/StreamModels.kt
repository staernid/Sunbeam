@file:Suppress("MagicNumber")

package io.github.staernid.sunbeam.features.stream.model

import java.util.Locale

/**
 * Real-time streaming performance metrics for the in-stream HUD.
 */
data class PerformanceStats(
    val fps: Float = 0f,
    val networkLatencyMs: Int = 0,
    val decodeLatencyMs: Float = 0f,
    val packetLossPercentage: Float = 0f,
    val bandwidthKbps: Int = 0,
    val videoFormat: String = "",
    val decoderName: String = "",
    val customDetails: String? = null,
) {
    val formattedFps: String
        get() = String.format(Locale.US, "%.1f", fps)

    val formattedDecodeLatency: String
        get() = String.format(Locale.US, "%.1f ms", decodeLatencyMs)

    val formattedPacketLoss: String
        get() = String.format(Locale.US, "%.1f%%", packetLossPercentage)

    val formattedBandwidth: String
        get() = if (bandwidthKbps >= 1000) {
            String.format(Locale.US, "%.1f Mbps", bandwidthKbps / 1000f)
        } else {
            "$bandwidthKbps kbps"
        }
}

/**
 * Display modes for the performance HUD overlay.
 */
enum class PerfOverlayMode {
    NONE,
    LITE,
    EXPANDED,
}

/**
 * Screen anchor position for the performance HUD overlay.
 */
enum class PerfOverlayPosition {
    TOP_CENTER,
    TOP_START,
    BOTTOM_CENTER,
}

/**
 * Touchpad / mouse input emulation mode matching [PointerInputHandler] mode indices.
 */
enum class StreamMouseMode(
    val modeId: Int,
    val title: String,
    val shortLabel: String,
    val description: String,
) {
    MULTI_TOUCH(
        modeId = 0,
        title = "Direct Touch",
        shortLabel = "Direct Touch",
        description = "Touchscreen maps directly to multi-touch and cursor",
    ),
    ABSOLUTE_TOUCH(
        modeId = 1,
        title = "Touch Mouse",
        shortLabel = "Touch Mouse",
        description = "Tap and drag cursor directly at touch point",
    ),
    TRACKPAD_NATURAL(
        modeId = 2,
        title = "Trackpad (Natural)",
        shortLabel = "Trackpad",
        description = "Two-finger scroll, natural inertia, tap to click",
    ),
    TRACKPAD_GAMING(
        modeId = 3,
        title = "Trackpad (Gaming)",
        shortLabel = "Gaming Pad",
        description = "Relative cursor movement with long-press drag",
    ),
    DISABLED(
        modeId = 4,
        title = "Touch Off",
        shortLabel = "Touch Off",
        description = "Ignore touch input (controller or stylus only)",
    ),
    ;

    companion object {
        fun fromModeId(modeId: Int): StreamMouseMode =
            entries.firstOrNull { it.modeId == modeId } ?: MULTI_TOUCH
    }
}

/**
 * Active modal dialog overlay in the stream view.
 */
enum class StreamDialog {
    NONE,
    MAIN_MENU,
    CONFIRM_QUIT,
    MOUSE_MODE_PICKER,
    SPECIAL_KEYS_PICKER,
}

/**
 * Host keyboard shortcuts and special key combinations.
 */
data class StreamShortcut(
    val id: String,
    val label: String,
    val keyCodes: ShortArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is StreamShortcut) return false
        return id == other.id && label == other.label && keyCodes.contentEquals(other.keyCodes)
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + keyCodes.contentHashCode()
        return result
    }

    companion object {
        const val VK_RETURN: Short = 13
        const val VK_ESCAPE: Short = 27
        const val VK_TAB: Short = 9
        const val VK_D: Short = 68
        const val VK_G: Short = 71
        const val VK_V: Short = 86
        const val VK_B: Short = 66
        const val VK_F4: Short = 115
        const val VK_F11: Short = 122
        const val VK_LWIN: Short = 91
        const val VK_LSHIFT: Short = 160
        const val VK_LCONTROL: Short = 162
        const val VK_LMENU: Short = 164 // Alt

        val defaultShortcuts: List<StreamShortcut> = listOf(
            StreamShortcut("esc", "Escape", shortArrayOf(VK_ESCAPE)),
            StreamShortcut("f11", "Fullscreen (F11)", shortArrayOf(VK_F11)),
            StreamShortcut("alt_f4", "Close App (Alt + F4)", shortArrayOf(VK_LMENU, VK_F4)),
            StreamShortcut("alt_enter", "Alt + Enter", shortArrayOf(VK_LMENU, VK_RETURN)),
            StreamShortcut("ctrl_v", "Paste (Ctrl + V)", shortArrayOf(VK_LCONTROL, VK_V)),
            StreamShortcut("win", "Windows Key", shortArrayOf(VK_LWIN)),
            StreamShortcut("win_d", "Desktop (Win + D)", shortArrayOf(VK_LWIN, VK_D)),
            StreamShortcut("win_g", "Game Bar (Win + G)", shortArrayOf(VK_LWIN, VK_G)),
            StreamShortcut(
                "task_mgr",
                "Task Manager (Ctrl + Shift + Esc)",
                shortArrayOf(VK_LCONTROL, VK_LSHIFT, VK_ESCAPE),
            ),
            StreamShortcut("ctrl_alt_tab", "Ctrl + Alt + Tab", shortArrayOf(VK_LCONTROL, VK_LMENU, VK_TAB)),
            StreamShortcut("shift_tab", "Steam Overlay (Shift + Tab)", shortArrayOf(VK_LSHIFT, VK_TAB)),
            StreamShortcut("win_alt_b", "HDR Toggle (Win + Alt + B)", shortArrayOf(VK_LWIN, VK_LMENU, VK_B)),
        )
    }
}
