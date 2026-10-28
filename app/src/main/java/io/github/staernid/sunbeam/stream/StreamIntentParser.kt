@file:Suppress("LongMethod", "CyclomaticComplexMethod")

package io.github.staernid.sunbeam.stream

import android.content.Intent
import io.github.staernid.sunbeam.core.preferences.models.AnalogStickScrolling
import io.github.staernid.sunbeam.core.preferences.models.AudioConfiguration
import io.github.staernid.sunbeam.core.preferences.models.FramePacingMode
import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import io.github.staernid.sunbeam.core.preferences.models.VideoFormatOption
import io.github.staernid.sunbeam.core.preferences.models.VideoScaleMode
import io.github.staernid.sunbeam.nvstream.jni.MoonBridge
import io.github.staernid.sunbeam.preferences.PreferenceConfiguration

/**
 * Bridges [StreamSettings] from DataStore and launching [Intent] extras into legacy [PreferenceConfiguration].
 */
object StreamIntentParser {
    const val EXTRA_HAS_STREAM_SETTINGS = "HasStreamSettings"
    const val EXTRA_STREAM_WIDTH = "StreamWidth"
    const val EXTRA_STREAM_HEIGHT = "StreamHeight"
    const val EXTRA_STREAM_FPS = "StreamFps"
    const val EXTRA_STREAM_BITRATE = "StreamBitrate"

    private const val DEFAULT_METERED_BITRATE_DIVISOR = 4
    private const val STEREO_CHANNELS = 2
    private const val STEREO_MASK = 0x3
    private const val SURROUND_51_CHANNELS = 6
    private const val SURROUND_51_MASK = 0x3F
    private const val SURROUND_71_CHANNELS = 8
    private const val SURROUND_71_MASK = 0x63F

    private const val EXTRA_METERED_BITRATE = "StreamMeteredBitrate"
    private const val EXTRA_VIDEO_SCALE_MODE = "StreamVideoScaleMode"
    private const val EXTRA_VIDEO_FORMAT = "StreamVideoFormat"
    private const val EXTRA_ENABLE_HDR = "StreamEnableHdr"
    private const val EXTRA_FULL_RANGE = "StreamFullRange"
    private const val EXTRA_ULTRA_LOW_LATENCY = "StreamUltraLowLatency"
    private const val EXTRA_FRAME_PACING = "StreamFramePacing"
    private const val EXTRA_REDUCE_REFRESH_RATE = "StreamReduceRefreshRate"
    private const val EXTRA_ENFORCE_DISPLAY_MODE = "StreamEnforceDisplayMode"
    private const val EXTRA_AUTO_INVERT_RES = "StreamAutoInvertRes"
    private const val EXTRA_VIDEO_SR = "StreamVideoSr"
    private const val EXTRA_UNLOCK_FPS = "StreamUnlockFps"
    private const val EXTRA_CUSTOM_RES = "StreamCustomRes"
    private const val EXTRA_CUSTOM_REFRESH_RATE = "StreamCustomRefreshRate"
    private const val EXTRA_AUDIO_CONFIG = "StreamAudioConfig"
    private const val EXTRA_HOST_AUDIO = "StreamHostAudio"
    private const val EXTRA_AUDIO_FX = "StreamAudioFx"
    private const val EXTRA_MULTI_CONTROLLER = "StreamMultiController"
    private const val EXTRA_USB_DRIVER = "StreamUsbDriver"
    private const val EXTRA_BIND_ALL_USB = "StreamBindAllUsb"
    private const val EXTRA_MOUSE_EMULATION = "StreamMouseEmulation"
    private const val EXTRA_REMEMBER_MOUSE_MODE = "StreamRememberMouseMode"
    private const val EXTRA_ABSOLUTE_MOUSE_MODE = "StreamAbsoluteMouseMode"
    private const val EXTRA_MOUSE_NAV_BUTTONS = "StreamMouseNavButtons"
    private const val EXTRA_ANALOG_SCROLLING = "StreamAnalogScrolling"
    private const val EXTRA_ONSCREEN_CONTROLLER = "StreamOnscreenController"
    private const val EXTRA_HIDE_OSC_GAMEPAD = "StreamHideOscGamepad"
    private const val EXTRA_ONLY_L3_R3 = "StreamOnlyL3R3"
    private const val EXTRA_SHOW_GUIDE_BUTTON = "StreamShowGuideButton"
    private const val EXTRA_VIBRATE_OSC = "StreamVibrateOsc"
    private const val EXTRA_VIBRATE_FALLBACK = "StreamVibrateFallback"
    private const val EXTRA_VIBRATE_FALLBACK_STRENGTH = "StreamVibrateFallbackStrength"
    private const val EXTRA_FLIP_FACE_BUTTONS = "StreamFlipFaceButtons"
    private const val EXTRA_GAMEPAD_TOUCHPAD_MOUSE = "StreamGamepadTouchpadMouse"
    private const val EXTRA_GAMEPAD_MOTION_SENSORS = "StreamGamepadMotionSensors"
    private const val EXTRA_GAMEPAD_MOTION_FALLBACK = "StreamGamepadMotionFallback"
    private const val EXTRA_FORCE_MOTION_FALLBACK = "StreamForceMotionFallback"
    private const val EXTRA_ENABLE_RUMBLE = "StreamEnableRumble"
    private const val EXTRA_ENABLE_BATTERY_REPORT = "StreamEnableBatteryReport"
    private const val EXTRA_FORCE_QWERTY = "StreamForceQwerty"
    private const val EXTRA_BACK_AS_META = "StreamBackAsMeta"
    private const val EXTRA_RIGHT_ALT_AS_META = "StreamRightAltAsMeta"
    private const val EXTRA_IGNORE_SYNTH_EVENTS = "StreamIgnoreSynthEvents"
    private const val EXTRA_BACK_AS_GUIDE = "StreamBackAsGuide"
    private const val EXTRA_DEADZONE_PERCENTAGE = "StreamDeadzonePercentage"
    private const val EXTRA_OSC_OPACITY = "StreamOscOpacity"
    private const val EXTRA_KEYBOARD_ALIGN_MODE = "StreamKeyboardAlignMode"
    private const val EXTRA_ENABLE_KEYBOARD = "StreamEnableKeyboard"
    private const val EXTRA_ENABLE_KEYBOARD_VIBRATE = "StreamEnableKeyboardVibrate"
    private const val EXTRA_ENABLE_COMMIT_TEXT = "StreamEnableCommitText"
    private const val EXTRA_TRACKPAD_SENS_X = "StreamTrackpadSensX"
    private const val EXTRA_TRACKPAD_SENS_Y = "StreamTrackpadSensY"
    private const val EXTRA_TRACKPAD_DRAG_VIBRATE = "StreamTrackpadDragVibrate"
    private const val EXTRA_TRACKPAD_DRAG_THRESHOLD = "StreamTrackpadDragThreshold"
    private const val EXTRA_TRACKPAD_SWAP_AXIS = "StreamTrackpadSwapAxis"
    private const val EXTRA_SMALL_ICON_MODE = "StreamSmallIconMode"
    private const val EXTRA_LANGUAGE = "StreamLanguage"
    private const val EXTRA_DISABLE_WARNINGS = "StreamDisableWarnings"
    private const val EXTRA_LATENCY_TOAST = "StreamLatencyToast"
    private const val EXTRA_PERF_OVERLAY = "StreamPerfOverlay"
    private const val EXTRA_PERF_LOGGING = "StreamPerfLogging"
    private const val EXTRA_PERF_OVERLAY_BOTTOM = "StreamPerfOverlayBottom"
    private const val EXTRA_RESUME_WITHOUT_CONFIRM = "StreamResumeWithoutConfirm"
    private const val EXTRA_FULL_SCREEN = "StreamFullScreen"
    private const val EXTRA_FLOATING_BUTTON = "StreamFloatingButton"
    private const val EXTRA_OVERLAY_ZOOM_BUTTON = "StreamOverlayZoomButton"
    private const val EXTRA_ENABLE_PIP = "StreamEnablePip"

    @JvmStatic
    fun putStreamSettingsExtras(
        intent: Intent,
        settings: StreamSettings,
    ) {
        intent.putExtra(EXTRA_HAS_STREAM_SETTINGS, true)
        intent.putExtra(EXTRA_STREAM_WIDTH, settings.width)
        intent.putExtra(EXTRA_STREAM_HEIGHT, settings.height)
        intent.putExtra(EXTRA_STREAM_FPS, settings.fps)
        intent.putExtra(EXTRA_STREAM_BITRATE, settings.bitrateKbps)
        intent.putExtra(EXTRA_METERED_BITRATE, settings.meteredBitrateKbps)
        intent.putExtra(EXTRA_VIDEO_SCALE_MODE, settings.videoScaleMode.name)
        intent.putExtra(EXTRA_VIDEO_FORMAT, settings.videoFormat.name)
        intent.putExtra(EXTRA_ENABLE_HDR, settings.enableHdr)
        intent.putExtra(EXTRA_FULL_RANGE, settings.fullRange)
        intent.putExtra(EXTRA_ULTRA_LOW_LATENCY, settings.enableUltraLowLatency)
        intent.putExtra(EXTRA_FRAME_PACING, settings.framePacing.name)
        intent.putExtra(EXTRA_REDUCE_REFRESH_RATE, settings.reduceRefreshRate)
        intent.putExtra(EXTRA_ENFORCE_DISPLAY_MODE, settings.enforceDisplayMode)
        intent.putExtra(EXTRA_AUTO_INVERT_RES, settings.autoInvertVideoResolution)
        intent.putExtra(EXTRA_VIDEO_SR, settings.videoSuperResolution)
        intent.putExtra(EXTRA_UNLOCK_FPS, settings.unlockFps)
        intent.putExtra(EXTRA_CUSTOM_RES, settings.customResolution)
        intent.putExtra(EXTRA_CUSTOM_REFRESH_RATE, settings.customRefreshRate)
        intent.putExtra(EXTRA_AUDIO_CONFIG, settings.audioConfig.name)
        intent.putExtra(EXTRA_HOST_AUDIO, settings.hostAudio)
        intent.putExtra(EXTRA_AUDIO_FX, settings.enableAudioFx)
        intent.putExtra(EXTRA_MULTI_CONTROLLER, settings.multiController)
        intent.putExtra(EXTRA_USB_DRIVER, settings.usbDriver)
        intent.putExtra(EXTRA_BIND_ALL_USB, settings.bindAllUsb)
        intent.putExtra(EXTRA_MOUSE_EMULATION, settings.mouseEmulation)
        intent.putExtra(EXTRA_REMEMBER_MOUSE_MODE, settings.rememberMouseMode)
        intent.putExtra(EXTRA_ABSOLUTE_MOUSE_MODE, settings.absoluteMouseMode)
        intent.putExtra(EXTRA_MOUSE_NAV_BUTTONS, settings.mouseNavButtons)
        intent.putExtra(EXTRA_ANALOG_SCROLLING, settings.analogStickScrolling.name)
        intent.putExtra(EXTRA_ONSCREEN_CONTROLLER, settings.onscreenController)
        intent.putExtra(EXTRA_HIDE_OSC_GAMEPAD, settings.hideOscWhenHasGamepad)
        intent.putExtra(EXTRA_ONLY_L3_R3, settings.onlyShowL3R3)
        intent.putExtra(EXTRA_SHOW_GUIDE_BUTTON, settings.showGuideButton)
        intent.putExtra(EXTRA_VIBRATE_OSC, settings.vibrateOsc)
        intent.putExtra(EXTRA_VIBRATE_FALLBACK, settings.vibrateFallback)
        intent.putExtra(EXTRA_VIBRATE_FALLBACK_STRENGTH, settings.vibrateFallbackStrength)
        intent.putExtra(EXTRA_FLIP_FACE_BUTTONS, settings.flipFaceButtons)
        intent.putExtra(EXTRA_GAMEPAD_TOUCHPAD_MOUSE, settings.gamepadTouchpadAsMouse)
        intent.putExtra(EXTRA_GAMEPAD_MOTION_SENSORS, settings.gamepadMotionSensors)
        intent.putExtra(EXTRA_GAMEPAD_MOTION_FALLBACK, settings.gamepadMotionFallback)
        intent.putExtra(EXTRA_FORCE_MOTION_FALLBACK, settings.forceMotionSensorsFallback)
        intent.putExtra(EXTRA_ENABLE_RUMBLE, settings.enableRumble)
        intent.putExtra(EXTRA_ENABLE_BATTERY_REPORT, settings.enableBatteryReport)
        intent.putExtra(EXTRA_FORCE_QWERTY, settings.forceQwerty)
        intent.putExtra(EXTRA_BACK_AS_META, settings.backAsMeta)
        intent.putExtra(EXTRA_RIGHT_ALT_AS_META, settings.rightAltAsMeta)
        intent.putExtra(EXTRA_IGNORE_SYNTH_EVENTS, settings.ignoreSynthEvents)
        intent.putExtra(EXTRA_BACK_AS_GUIDE, settings.backAsGuide)
        intent.putExtra(EXTRA_DEADZONE_PERCENTAGE, settings.deadzonePercentage)
        intent.putExtra(EXTRA_OSC_OPACITY, settings.oscOpacity)
        intent.putExtra(EXTRA_KEYBOARD_ALIGN_MODE, settings.onscreenKeyboardAlignMode)
        intent.putExtra(EXTRA_ENABLE_KEYBOARD, settings.enableKeyboard)
        intent.putExtra(EXTRA_ENABLE_KEYBOARD_VIBRATE, settings.enableKeyboardVibrate)
        intent.putExtra(EXTRA_ENABLE_COMMIT_TEXT, settings.enableCommitText)
        intent.putExtra(EXTRA_TRACKPAD_SENS_X, settings.trackpadSensitivityX)
        intent.putExtra(EXTRA_TRACKPAD_SENS_Y, settings.trackpadSensitivityY)
        intent.putExtra(EXTRA_TRACKPAD_DRAG_VIBRATE, settings.trackpadDragDropVibration)
        intent.putExtra(EXTRA_TRACKPAD_DRAG_THRESHOLD, settings.trackpadDragDropThreshold)
        intent.putExtra(EXTRA_TRACKPAD_SWAP_AXIS, settings.trackpadSwapAxis)
        intent.putExtra(EXTRA_SMALL_ICON_MODE, settings.smallIconMode)
        intent.putExtra(EXTRA_LANGUAGE, settings.language)
        intent.putExtra(EXTRA_DISABLE_WARNINGS, settings.disableWarnings)
        intent.putExtra(EXTRA_LATENCY_TOAST, settings.enableLatencyToast)
        intent.putExtra(EXTRA_PERF_OVERLAY, settings.enablePerfOverlay)
        intent.putExtra(EXTRA_PERF_LOGGING, settings.enablePerfLogging)
        intent.putExtra(EXTRA_PERF_OVERLAY_BOTTOM, settings.enablePerfOverlayBottom)
        intent.putExtra(EXTRA_RESUME_WITHOUT_CONFIRM, settings.resumeWithoutConfirm)
        intent.putExtra(EXTRA_FULL_SCREEN, settings.fullScreen)
        intent.putExtra(EXTRA_FLOATING_BUTTON, settings.enableFloatingButton)
        intent.putExtra(EXTRA_OVERLAY_ZOOM_BUTTON, settings.showOverlayZoomToggleButton)
        intent.putExtra(EXTRA_ENABLE_PIP, settings.enablePip)
    }

    @JvmStatic
    fun readStreamSettingsFromExtras(intent: Intent): StreamSettings? {
        if (!intent.getBooleanExtra(EXTRA_HAS_STREAM_SETTINGS, false)) return null
        val defaults = StreamSettings()

        val scaleMode =
            intent.getStringExtra(EXTRA_VIDEO_SCALE_MODE)?.let {
                runCatching { VideoScaleMode.valueOf(it) }.getOrNull()
            } ?: defaults.videoScaleMode

        val videoFormat =
            intent.getStringExtra(EXTRA_VIDEO_FORMAT)?.let {
                runCatching { VideoFormatOption.valueOf(it) }.getOrNull()
            } ?: defaults.videoFormat

        val framePacing =
            intent.getStringExtra(EXTRA_FRAME_PACING)?.let {
                runCatching { FramePacingMode.valueOf(it) }.getOrNull()
            } ?: defaults.framePacing

        val audioConfig =
            intent.getStringExtra(EXTRA_AUDIO_CONFIG)?.let {
                runCatching { AudioConfiguration.valueOf(it) }.getOrNull()
            } ?: defaults.audioConfig

        val analogScrolling =
            intent.getStringExtra(EXTRA_ANALOG_SCROLLING)?.let {
                runCatching { AnalogStickScrolling.valueOf(it) }.getOrNull()
            } ?: defaults.analogStickScrolling

        return StreamSettings(
            width = intent.getIntExtra(EXTRA_STREAM_WIDTH, defaults.width),
            height = intent.getIntExtra(EXTRA_STREAM_HEIGHT, defaults.height),
            fps = intent.getIntExtra(EXTRA_STREAM_FPS, defaults.fps),
            bitrateKbps = intent.getIntExtra(EXTRA_STREAM_BITRATE, defaults.bitrateKbps),
            meteredBitrateKbps = intent.getIntExtra(EXTRA_METERED_BITRATE, defaults.meteredBitrateKbps),
            videoScaleMode = scaleMode,
            videoFormat = videoFormat,
            enableHdr = intent.getBooleanExtra(EXTRA_ENABLE_HDR, defaults.enableHdr),
            fullRange = intent.getBooleanExtra(EXTRA_FULL_RANGE, defaults.fullRange),
            enableUltraLowLatency =
                intent.getBooleanExtra(EXTRA_ULTRA_LOW_LATENCY, defaults.enableUltraLowLatency),
            framePacing = framePacing,
            reduceRefreshRate = intent.getBooleanExtra(EXTRA_REDUCE_REFRESH_RATE, defaults.reduceRefreshRate),
            enforceDisplayMode = intent.getBooleanExtra(EXTRA_ENFORCE_DISPLAY_MODE, defaults.enforceDisplayMode),
            autoInvertVideoResolution =
                intent.getBooleanExtra(EXTRA_AUTO_INVERT_RES, defaults.autoInvertVideoResolution),
            videoSuperResolution = intent.getBooleanExtra(EXTRA_VIDEO_SR, defaults.videoSuperResolution),
            unlockFps = intent.getBooleanExtra(EXTRA_UNLOCK_FPS, defaults.unlockFps),
            customResolution = intent.getStringExtra(EXTRA_CUSTOM_RES),
            customRefreshRate = intent.getStringExtra(EXTRA_CUSTOM_REFRESH_RATE),
            audioConfig = audioConfig,
            hostAudio = intent.getBooleanExtra(EXTRA_HOST_AUDIO, defaults.hostAudio),
            enableAudioFx = intent.getBooleanExtra(EXTRA_AUDIO_FX, defaults.enableAudioFx),
            multiController = intent.getBooleanExtra(EXTRA_MULTI_CONTROLLER, defaults.multiController),
            usbDriver = intent.getBooleanExtra(EXTRA_USB_DRIVER, defaults.usbDriver),
            bindAllUsb = intent.getBooleanExtra(EXTRA_BIND_ALL_USB, defaults.bindAllUsb),
            mouseEmulation = intent.getBooleanExtra(EXTRA_MOUSE_EMULATION, defaults.mouseEmulation),
            rememberMouseMode = intent.getBooleanExtra(EXTRA_REMEMBER_MOUSE_MODE, defaults.rememberMouseMode),
            absoluteMouseMode = intent.getBooleanExtra(EXTRA_ABSOLUTE_MOUSE_MODE, defaults.absoluteMouseMode),
            mouseNavButtons = intent.getBooleanExtra(EXTRA_MOUSE_NAV_BUTTONS, defaults.mouseNavButtons),
            analogStickScrolling = analogScrolling,
            onscreenController = intent.getBooleanExtra(EXTRA_ONSCREEN_CONTROLLER, defaults.onscreenController),
            hideOscWhenHasGamepad =
                intent.getBooleanExtra(EXTRA_HIDE_OSC_GAMEPAD, defaults.hideOscWhenHasGamepad),
            onlyShowL3R3 = intent.getBooleanExtra(EXTRA_ONLY_L3_R3, defaults.onlyShowL3R3),
            showGuideButton = intent.getBooleanExtra(EXTRA_SHOW_GUIDE_BUTTON, defaults.showGuideButton),
            vibrateOsc = intent.getBooleanExtra(EXTRA_VIBRATE_OSC, defaults.vibrateOsc),
            vibrateFallback = intent.getBooleanExtra(EXTRA_VIBRATE_FALLBACK, defaults.vibrateFallback),
            vibrateFallbackStrength =
                intent.getIntExtra(EXTRA_VIBRATE_FALLBACK_STRENGTH, defaults.vibrateFallbackStrength),
            flipFaceButtons = intent.getBooleanExtra(EXTRA_FLIP_FACE_BUTTONS, defaults.flipFaceButtons),
            gamepadTouchpadAsMouse =
                intent.getBooleanExtra(EXTRA_GAMEPAD_TOUCHPAD_MOUSE, defaults.gamepadTouchpadAsMouse),
            gamepadMotionSensors =
                intent.getBooleanExtra(EXTRA_GAMEPAD_MOTION_SENSORS, defaults.gamepadMotionSensors),
            gamepadMotionFallback =
                intent.getBooleanExtra(EXTRA_GAMEPAD_MOTION_FALLBACK, defaults.gamepadMotionFallback),
            forceMotionSensorsFallback =
                intent.getBooleanExtra(EXTRA_FORCE_MOTION_FALLBACK, defaults.forceMotionSensorsFallback),
            enableRumble = intent.getBooleanExtra(EXTRA_ENABLE_RUMBLE, defaults.enableRumble),
            enableBatteryReport = intent.getBooleanExtra(EXTRA_ENABLE_BATTERY_REPORT, defaults.enableBatteryReport),
            forceQwerty = intent.getBooleanExtra(EXTRA_FORCE_QWERTY, defaults.forceQwerty),
            backAsMeta = intent.getBooleanExtra(EXTRA_BACK_AS_META, defaults.backAsMeta),
            rightAltAsMeta = intent.getBooleanExtra(EXTRA_RIGHT_ALT_AS_META, defaults.rightAltAsMeta),
            ignoreSynthEvents = intent.getBooleanExtra(EXTRA_IGNORE_SYNTH_EVENTS, defaults.ignoreSynthEvents),
            backAsGuide = intent.getBooleanExtra(EXTRA_BACK_AS_GUIDE, defaults.backAsGuide),
            deadzonePercentage = intent.getIntExtra(EXTRA_DEADZONE_PERCENTAGE, defaults.deadzonePercentage),
            oscOpacity = intent.getIntExtra(EXTRA_OSC_OPACITY, defaults.oscOpacity),
            onscreenKeyboardAlignMode =
                intent.getStringExtra(EXTRA_KEYBOARD_ALIGN_MODE) ?: defaults.onscreenKeyboardAlignMode,
            enableKeyboard = intent.getBooleanExtra(EXTRA_ENABLE_KEYBOARD, defaults.enableKeyboard),
            enableKeyboardVibrate =
                intent.getBooleanExtra(EXTRA_ENABLE_KEYBOARD_VIBRATE, defaults.enableKeyboardVibrate),
            enableCommitText = intent.getBooleanExtra(EXTRA_ENABLE_COMMIT_TEXT, defaults.enableCommitText),
            trackpadSensitivityX = intent.getIntExtra(EXTRA_TRACKPAD_SENS_X, defaults.trackpadSensitivityX),
            trackpadSensitivityY = intent.getIntExtra(EXTRA_TRACKPAD_SENS_Y, defaults.trackpadSensitivityY),
            trackpadDragDropVibration =
                intent.getBooleanExtra(EXTRA_TRACKPAD_DRAG_VIBRATE, defaults.trackpadDragDropVibration),
            trackpadDragDropThreshold =
                intent.getIntExtra(EXTRA_TRACKPAD_DRAG_THRESHOLD, defaults.trackpadDragDropThreshold),
            trackpadSwapAxis = intent.getBooleanExtra(EXTRA_TRACKPAD_SWAP_AXIS, defaults.trackpadSwapAxis),
            smallIconMode = intent.getBooleanExtra(EXTRA_SMALL_ICON_MODE, defaults.smallIconMode),
            language = intent.getStringExtra(EXTRA_LANGUAGE) ?: defaults.language,
            disableWarnings = intent.getBooleanExtra(EXTRA_DISABLE_WARNINGS, defaults.disableWarnings),
            enableLatencyToast = intent.getBooleanExtra(EXTRA_LATENCY_TOAST, defaults.enableLatencyToast),
            enablePerfOverlay = intent.getBooleanExtra(EXTRA_PERF_OVERLAY, defaults.enablePerfOverlay),
            enablePerfLogging = intent.getBooleanExtra(EXTRA_PERF_LOGGING, defaults.enablePerfLogging),
            enablePerfOverlayBottom =
                intent.getBooleanExtra(EXTRA_PERF_OVERLAY_BOTTOM, defaults.enablePerfOverlayBottom),
            resumeWithoutConfirm = intent.getBooleanExtra(EXTRA_RESUME_WITHOUT_CONFIRM, defaults.resumeWithoutConfirm),
            fullScreen = intent.getBooleanExtra(EXTRA_FULL_SCREEN, defaults.fullScreen),
            enableFloatingButton = intent.getBooleanExtra(EXTRA_FLOATING_BUTTON, defaults.enableFloatingButton),
            showOverlayZoomToggleButton =
                intent.getBooleanExtra(EXTRA_OVERLAY_ZOOM_BUTTON, defaults.showOverlayZoomToggleButton),
            enablePip = intent.getBooleanExtra(EXTRA_ENABLE_PIP, defaults.enablePip),
        )
    }

    @JvmStatic
    fun applyStreamSettings(
        prefConfig: PreferenceConfiguration,
        settings: StreamSettings,
    ) {
        prefConfig.width = settings.width
        prefConfig.height = settings.height
        prefConfig.fps = settings.fps.toFloat()
        prefConfig.bitrate = settings.bitrateKbps
        prefConfig.meteredBitrate =
            if (settings.meteredBitrateKbps > 0) {
                settings.meteredBitrateKbps
            } else {
                settings.bitrateKbps / DEFAULT_METERED_BITRATE_DIVISOR
            }

        prefConfig.videoScaleMode =
            when (settings.videoScaleMode) {
                VideoScaleMode.FIT -> PreferenceConfiguration.ScaleMode.FIT
                VideoScaleMode.FILL -> PreferenceConfiguration.ScaleMode.FILL
                VideoScaleMode.STRETCH -> PreferenceConfiguration.ScaleMode.STRETCH
            }

        prefConfig.videoFormat =
            when (settings.videoFormat) {
                VideoFormatOption.AUTO -> PreferenceConfiguration.FormatOption.AUTO
                VideoFormatOption.FORCE_AV1 -> PreferenceConfiguration.FormatOption.FORCE_AV1
                VideoFormatOption.FORCE_HEVC -> PreferenceConfiguration.FormatOption.FORCE_HEVC
                VideoFormatOption.FORCE_H264 -> PreferenceConfiguration.FormatOption.FORCE_H264
            }

        prefConfig.framePacing =
            when (settings.framePacing) {
                FramePacingMode.MIN_LATENCY -> PreferenceConfiguration.FRAME_PACING_MIN_LATENCY
                FramePacingMode.BALANCED -> PreferenceConfiguration.FRAME_PACING_BALANCED
                FramePacingMode.CAP_FPS -> PreferenceConfiguration.FRAME_PACING_CAP_FPS
                FramePacingMode.MAX_SMOOTHNESS -> PreferenceConfiguration.FRAME_PACING_MAX_SMOOTHNESS
            }

        prefConfig.audioConfiguration =
            when (settings.audioConfig) {
                AudioConfiguration.STEREO ->
                    MoonBridge.AudioConfiguration(STEREO_CHANNELS, STEREO_MASK)
                AudioConfiguration.SURROUND_51 ->
                    MoonBridge.AudioConfiguration(SURROUND_51_CHANNELS, SURROUND_51_MASK)
                AudioConfiguration.SURROUND_71 ->
                    MoonBridge.AudioConfiguration(SURROUND_71_CHANNELS, SURROUND_71_MASK)
            }

        prefConfig.analogStickForScrolling =
            when (settings.analogStickScrolling) {
                AnalogStickScrolling.NONE -> PreferenceConfiguration.AnalogStickForScrolling.NONE
                AnalogStickScrolling.RIGHT -> PreferenceConfiguration.AnalogStickForScrolling.RIGHT
                AnalogStickScrolling.LEFT -> PreferenceConfiguration.AnalogStickForScrolling.LEFT
            }

        prefConfig.enableHdr = settings.enableHdr && !PreferenceConfiguration.isShieldAtvFirmwareWithBrokenHdr()
        prefConfig.fullRange = settings.fullRange
        prefConfig.enableUltraLowLatency = settings.enableUltraLowLatency
        prefConfig.reduceRefreshRate = settings.reduceRefreshRate
        prefConfig.enforceDisplayMode = settings.enforceDisplayMode
        prefConfig.autoInvertVideoResolution = settings.autoInvertVideoResolution
        prefConfig.videoSr = settings.videoSuperResolution
        prefConfig.unlockFps = settings.unlockFps
        prefConfig.customResolution = settings.customResolution
        prefConfig.customRefreshRate = settings.customRefreshRate

        prefConfig.playHostAudio = settings.hostAudio
        prefConfig.enableAudioFx = settings.enableAudioFx

        prefConfig.multiController = settings.multiController
        prefConfig.usbDriver = settings.usbDriver
        prefConfig.bindAllUsb = settings.bindAllUsb
        prefConfig.mouseEmulation = settings.mouseEmulation
        prefConfig.rememberMouseMode = settings.rememberMouseMode
        prefConfig.absoluteMouseMode = settings.absoluteMouseMode
        prefConfig.mouseNavButtons = settings.mouseNavButtons
        prefConfig.onscreenController = settings.onscreenController
        prefConfig.hideOSCWhenHasGamepad = settings.hideOscWhenHasGamepad
        prefConfig.onlyL3R3 = settings.onlyShowL3R3
        prefConfig.showGuideButton = settings.showGuideButton
        prefConfig.vibrateOsc = settings.vibrateOsc
        prefConfig.vibrateFallbackToDevice = settings.vibrateFallback
        prefConfig.vibrateFallbackToDeviceStrength = settings.vibrateFallbackStrength
        prefConfig.flipFaceButtons = settings.flipFaceButtons
        prefConfig.gamepadTouchpadAsMouse = settings.gamepadTouchpadAsMouse
        prefConfig.gamepadMotionSensors = settings.gamepadMotionSensors
        prefConfig.gamepadMotionSensorsFallbackToDevice = settings.gamepadMotionFallback
        prefConfig.forceMotionSensorsFallbackToDevice = settings.forceMotionSensorsFallback
        prefConfig.enableRumble = settings.enableRumble
        prefConfig.enableBatteryReport = settings.enableBatteryReport
        prefConfig.forceQwerty = settings.forceQwerty
        prefConfig.backAsMeta = settings.backAsMeta
        prefConfig.rightAltAsMeta = settings.rightAltAsMeta
        prefConfig.ignoreSynthEvents = settings.ignoreSynthEvents
        prefConfig.backAsGuide = settings.backAsGuide
        prefConfig.deadzonePercentage = settings.deadzonePercentage
        prefConfig.oscOpacity = settings.oscOpacity
        prefConfig.onscreenKeyboardAlignMode = settings.onscreenKeyboardAlignMode
        prefConfig.enableKeyboard = settings.enableKeyboard
        prefConfig.enableKeyboardVibrate = settings.enableKeyboardVibrate
        prefConfig.enableCommitText = settings.enableCommitText

        prefConfig.trackpadSensitivityX = settings.trackpadSensitivityX
        prefConfig.trackpadSensitivityY = settings.trackpadSensitivityY
        prefConfig.trackpadDragDropVibration = settings.trackpadDragDropVibration
        prefConfig.trackpadDragDropThreshold = settings.trackpadDragDropThreshold
        prefConfig.trackpadSwapAxis = settings.trackpadSwapAxis

        prefConfig.smallIconMode = settings.smallIconMode
        prefConfig.language = settings.language
        prefConfig.disableWarnings = settings.disableWarnings
        prefConfig.enableLatencyToast = settings.enableLatencyToast
        prefConfig.enablePerfOverlay = settings.enablePerfOverlay
        prefConfig.enablePerfLogging = settings.enablePerfLogging
        prefConfig.enablePerfOverlayBottom = settings.enablePerfOverlayBottom
        prefConfig.resumeWithoutConfirm = settings.resumeWithoutConfirm
        prefConfig.fullScreen = settings.fullScreen
        prefConfig.enableFloatingButton = settings.enableFloatingButton
        prefConfig.showOverlayZoomToggleButton = settings.showOverlayZoomToggleButton
        prefConfig.enablePip = settings.enablePip
    }

    @JvmStatic
    fun applyStreamExtras(
        intent: Intent?,
        prefConfig: PreferenceConfiguration?,
    ) {
        if (intent == null || prefConfig == null) return

        readStreamSettingsFromExtras(intent)?.let { settings ->
            applyStreamSettings(prefConfig, settings)
        }

        val width = intent.getIntExtra(EXTRA_STREAM_WIDTH, 0)
        val height = intent.getIntExtra(EXTRA_STREAM_HEIGHT, 0)
        if (width > 0 && height > 0) {
            prefConfig.width = width
            prefConfig.height = height
        }

        val fps = intent.getIntExtra(EXTRA_STREAM_FPS, 0)
        if (fps > 0) {
            prefConfig.fps = fps.toFloat()
        }

        val bitrate = intent.getIntExtra(EXTRA_STREAM_BITRATE, 0)
        if (bitrate > 0) {
            prefConfig.bitrate = bitrate
        }
    }
}
