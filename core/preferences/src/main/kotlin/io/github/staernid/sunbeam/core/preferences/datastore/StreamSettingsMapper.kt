package io.github.staernid.sunbeam.core.preferences.datastore

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import io.github.staernid.sunbeam.core.preferences.models.AnalogStickScrolling
import io.github.staernid.sunbeam.core.preferences.models.AudioConfiguration
import io.github.staernid.sunbeam.core.preferences.models.FramePacingMode
import io.github.staernid.sunbeam.core.preferences.models.Resolution
import io.github.staernid.sunbeam.core.preferences.models.StreamSettings
import io.github.staernid.sunbeam.core.preferences.models.VideoFormatOption
import io.github.staernid.sunbeam.core.preferences.models.VideoScaleMode

/**
 * Maps DataStore [Preferences] to typed [StreamSettings] and vice versa.
 */
object StreamSettingsMapper {
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun fromPreferences(prefs: Preferences): StreamSettings {
        val defaultSettings = StreamSettings()

        val resStr = prefs[StreamPreferenceKeys.RESOLUTION]
        val parsedRes = Resolution.fromString(resStr)
        val width = parsedRes?.width ?: defaultSettings.width
        val height = parsedRes?.height ?: defaultSettings.height

        val fps = prefs[StreamPreferenceKeys.FPS]?.toIntOrNull() ?: defaultSettings.fps
        val bitrate = prefs[StreamPreferenceKeys.BITRATE_KBPS] ?: defaultSettings.bitrateKbps
        val meteredBitrate = prefs[StreamPreferenceKeys.METERED_BITRATE_KBPS] ?: defaultSettings.meteredBitrateKbps

        val scaleMode = VideoScaleMode.fromPrefValue(prefs[StreamPreferenceKeys.VIDEO_SCALE_MODE])
        val videoFormat = VideoFormatOption.fromPrefValue(prefs[StreamPreferenceKeys.VIDEO_FORMAT])
        val audioConfig = AudioConfiguration.fromPrefValue(prefs[StreamPreferenceKeys.AUDIO_CONFIG])
        val framePacing = FramePacingMode.fromPrefValue(prefs[StreamPreferenceKeys.FRAME_PACING])
        val analogScrolling = AnalogStickScrolling.fromPrefValue(prefs[StreamPreferenceKeys.ANALOG_SCROLLING])

        val customBitrateStr = prefs[StreamPreferenceKeys.CUSTOM_BITRATE]
        val customBitrate = customBitrateStr?.toIntOrNull()

        return StreamSettings(
            width = width,
            height = height,
            fps = fps,
            bitrateKbps = bitrate,
            meteredBitrateKbps = meteredBitrate,
            videoScaleMode = scaleMode,
            videoFormat = videoFormat,
            enableHdr = prefs[StreamPreferenceKeys.ENABLE_HDR] ?: defaultSettings.enableHdr,
            fullRange = prefs[StreamPreferenceKeys.FULL_RANGE] ?: defaultSettings.fullRange,
            enableUltraLowLatency =
                prefs[StreamPreferenceKeys.ULTRA_LOW_LATENCY]
                    ?: defaultSettings.enableUltraLowLatency,
            framePacing = framePacing,
            reduceRefreshRate = prefs[StreamPreferenceKeys.REDUCE_REFRESH_RATE] ?: defaultSettings.reduceRefreshRate,
            enforceDisplayMode = prefs[StreamPreferenceKeys.ENFORCE_DISPLAY_MODE] ?: defaultSettings.enforceDisplayMode,
            autoInvertVideoResolution =
                prefs[StreamPreferenceKeys.AUTO_INVERT_RESOLUTION]
                    ?: defaultSettings.autoInvertVideoResolution,
            videoSuperResolution = prefs[StreamPreferenceKeys.VIDEO_SR] ?: defaultSettings.videoSuperResolution,
            unlockFps = prefs[StreamPreferenceKeys.UNLOCK_FPS] ?: defaultSettings.unlockFps,
            customResolution = prefs[StreamPreferenceKeys.CUSTOM_RESOLUTION],
            customRefreshRate = prefs[StreamPreferenceKeys.CUSTOM_REFRESH_RATE],
            customBitrateKbps = customBitrate,
            audioConfig = audioConfig,
            hostAudio = prefs[StreamPreferenceKeys.HOST_AUDIO] ?: defaultSettings.hostAudio,
            enableAudioFx = prefs[StreamPreferenceKeys.ENABLE_AUDIO_FX] ?: defaultSettings.enableAudioFx,
            multiController = prefs[StreamPreferenceKeys.MULTI_CONTROLLER] ?: defaultSettings.multiController,
            usbDriver = prefs[StreamPreferenceKeys.USB_DRIVER] ?: defaultSettings.usbDriver,
            bindAllUsb = prefs[StreamPreferenceKeys.BIND_ALL_USB] ?: defaultSettings.bindAllUsb,
            mouseEmulation = prefs[StreamPreferenceKeys.MOUSE_EMULATION] ?: defaultSettings.mouseEmulation,
            rememberMouseMode = prefs[StreamPreferenceKeys.REMEMBER_MOUSE_MODE] ?: defaultSettings.rememberMouseMode,
            absoluteMouseMode = prefs[StreamPreferenceKeys.ABSOLUTE_MOUSE_MODE] ?: defaultSettings.absoluteMouseMode,
            mouseNavButtons = prefs[StreamPreferenceKeys.MOUSE_NAV_BUTTONS] ?: defaultSettings.mouseNavButtons,
            analogStickScrolling = analogScrolling,
            onscreenController = prefs[StreamPreferenceKeys.ONSCREEN_CONTROLLER] ?: defaultSettings.onscreenController,
            hideOscWhenHasGamepad =
                prefs[StreamPreferenceKeys.HIDE_OSC_WHEN_HAS_GAMEPAD]
                    ?: defaultSettings.hideOscWhenHasGamepad,
            onlyShowL3R3 = prefs[StreamPreferenceKeys.ONLY_SHOW_L3R3] ?: defaultSettings.onlyShowL3R3,
            showGuideButton = prefs[StreamPreferenceKeys.SHOW_GUIDE_BUTTON] ?: defaultSettings.showGuideButton,
            vibrateOsc = prefs[StreamPreferenceKeys.VIBRATE_OSC] ?: defaultSettings.vibrateOsc,
            vibrateFallback = prefs[StreamPreferenceKeys.VIBRATE_FALLBACK] ?: defaultSettings.vibrateFallback,
            vibrateFallbackStrength =
                prefs[StreamPreferenceKeys.VIBRATE_FALLBACK_STRENGTH]
                    ?: defaultSettings.vibrateFallbackStrength,
            flipFaceButtons = prefs[StreamPreferenceKeys.FLIP_FACE_BUTTONS] ?: defaultSettings.flipFaceButtons,
            gamepadTouchpadAsMouse =
                prefs[StreamPreferenceKeys.GAMEPAD_TOUCHPAD_AS_MOUSE]
                    ?: defaultSettings.gamepadTouchpadAsMouse,
            gamepadMotionSensors =
                prefs[StreamPreferenceKeys.GAMEPAD_MOTION_SENSORS]
                    ?: defaultSettings.gamepadMotionSensors,
            gamepadMotionFallback =
                prefs[StreamPreferenceKeys.GAMEPAD_MOTION_FALLBACK]
                    ?: defaultSettings.gamepadMotionFallback,
            forceMotionSensorsFallback =
                prefs[StreamPreferenceKeys.FORCE_MOTION_SENSORS_FALLBACK]
                    ?: defaultSettings.forceMotionSensorsFallback,
            enableRumble = prefs[StreamPreferenceKeys.ENABLE_RUMBLE] ?: defaultSettings.enableRumble,
            enableBatteryReport =
                prefs[StreamPreferenceKeys.ENABLE_BATTERY_REPORT]
                    ?: defaultSettings.enableBatteryReport,
            forceQwerty = prefs[StreamPreferenceKeys.FORCE_QWERTY] ?: defaultSettings.forceQwerty,
            backAsMeta = prefs[StreamPreferenceKeys.BACK_AS_META] ?: defaultSettings.backAsMeta,
            rightAltAsMeta = prefs[StreamPreferenceKeys.RIGHT_ALT_AS_META] ?: defaultSettings.rightAltAsMeta,
            ignoreSynthEvents = prefs[StreamPreferenceKeys.IGNORE_SYNTH_EVENTS] ?: defaultSettings.ignoreSynthEvents,
            backAsGuide = prefs[StreamPreferenceKeys.BACK_AS_GUIDE] ?: defaultSettings.backAsGuide,
            deadzonePercentage = prefs[StreamPreferenceKeys.DEADZONE] ?: defaultSettings.deadzonePercentage,
            oscOpacity = prefs[StreamPreferenceKeys.OSC_OPACITY] ?: defaultSettings.oscOpacity,
            onscreenKeyboardAlignMode =
                prefs[StreamPreferenceKeys.ONSCREEN_KEYBOARD_ALIGN_MODE]
                    ?: defaultSettings.onscreenKeyboardAlignMode,
            enableKeyboard = prefs[StreamPreferenceKeys.ENABLE_KEYBOARD] ?: defaultSettings.enableKeyboard,
            enableKeyboardVibrate =
                prefs[StreamPreferenceKeys.ENABLE_KEYBOARD_VIBRATE]
                    ?: defaultSettings.enableKeyboardVibrate,
            enableCommitText = prefs[StreamPreferenceKeys.ENABLE_COMMIT_TEXT] ?: defaultSettings.enableCommitText,
            smallIconMode = prefs[StreamPreferenceKeys.SMALL_ICON_MODE] ?: defaultSettings.smallIconMode,
            language = prefs[StreamPreferenceKeys.LANGUAGE] ?: defaultSettings.language,
            disableWarnings = prefs[StreamPreferenceKeys.DISABLE_WARNINGS] ?: defaultSettings.disableWarnings,
            enableLatencyToast = prefs[StreamPreferenceKeys.LATENCY_TOAST] ?: defaultSettings.enableLatencyToast,
            enablePerfOverlay = prefs[StreamPreferenceKeys.ENABLE_PERF_OVERLAY] ?: defaultSettings.enablePerfOverlay,
            enablePerfLogging = prefs[StreamPreferenceKeys.ENABLE_PERF_LOGGING] ?: defaultSettings.enablePerfLogging,
            resumeWithoutConfirm =
                prefs[StreamPreferenceKeys.RESUME_WITHOUT_CONFIRM]
                    ?: defaultSettings.resumeWithoutConfirm,
            fullScreen = prefs[StreamPreferenceKeys.FULL_SCREEN] ?: defaultSettings.fullScreen,
            enablePip = prefs[StreamPreferenceKeys.ENABLE_PIP] ?: defaultSettings.enablePip,
        )
    }

    @Suppress("LongMethod")
    fun toPreferences(
        settings: StreamSettings,
        prefs: MutablePreferences,
    ) {
        prefs[StreamPreferenceKeys.RESOLUTION] = "${settings.width}x${settings.height}"
        prefs[StreamPreferenceKeys.FPS] = settings.fps.toString()
        prefs[StreamPreferenceKeys.BITRATE_KBPS] = settings.bitrateKbps
        prefs[StreamPreferenceKeys.METERED_BITRATE_KBPS] = settings.meteredBitrateKbps
        prefs[StreamPreferenceKeys.VIDEO_SCALE_MODE] = settings.videoScaleMode.prefValue
        prefs[StreamPreferenceKeys.VIDEO_FORMAT] = settings.videoFormat.prefValue
        prefs[StreamPreferenceKeys.ENABLE_HDR] = settings.enableHdr
        prefs[StreamPreferenceKeys.FULL_RANGE] = settings.fullRange
        prefs[StreamPreferenceKeys.ULTRA_LOW_LATENCY] = settings.enableUltraLowLatency
        prefs[StreamPreferenceKeys.FRAME_PACING] = settings.framePacing.prefValue
        prefs[StreamPreferenceKeys.REDUCE_REFRESH_RATE] = settings.reduceRefreshRate
        prefs[StreamPreferenceKeys.ENFORCE_DISPLAY_MODE] = settings.enforceDisplayMode
        prefs[StreamPreferenceKeys.AUTO_INVERT_RESOLUTION] = settings.autoInvertVideoResolution
        prefs[StreamPreferenceKeys.VIDEO_SR] = settings.videoSuperResolution
        prefs[StreamPreferenceKeys.UNLOCK_FPS] = settings.unlockFps
        settings.customResolution?.let { prefs[StreamPreferenceKeys.CUSTOM_RESOLUTION] = it }
        settings.customRefreshRate?.let { prefs[StreamPreferenceKeys.CUSTOM_REFRESH_RATE] = it }
        settings.customBitrateKbps?.let { prefs[StreamPreferenceKeys.CUSTOM_BITRATE] = it.toString() }

        prefs[StreamPreferenceKeys.AUDIO_CONFIG] = settings.audioConfig.prefValue
        prefs[StreamPreferenceKeys.HOST_AUDIO] = settings.hostAudio
        prefs[StreamPreferenceKeys.ENABLE_AUDIO_FX] = settings.enableAudioFx

        prefs[StreamPreferenceKeys.MULTI_CONTROLLER] = settings.multiController
        prefs[StreamPreferenceKeys.USB_DRIVER] = settings.usbDriver
        prefs[StreamPreferenceKeys.BIND_ALL_USB] = settings.bindAllUsb
        prefs[StreamPreferenceKeys.MOUSE_EMULATION] = settings.mouseEmulation
        prefs[StreamPreferenceKeys.REMEMBER_MOUSE_MODE] = settings.rememberMouseMode
        prefs[StreamPreferenceKeys.ABSOLUTE_MOUSE_MODE] = settings.absoluteMouseMode
        prefs[StreamPreferenceKeys.MOUSE_NAV_BUTTONS] = settings.mouseNavButtons
        prefs[StreamPreferenceKeys.ANALOG_SCROLLING] = settings.analogStickScrolling.prefValue
        prefs[StreamPreferenceKeys.ONSCREEN_CONTROLLER] = settings.onscreenController
        prefs[StreamPreferenceKeys.HIDE_OSC_WHEN_HAS_GAMEPAD] = settings.hideOscWhenHasGamepad
        prefs[StreamPreferenceKeys.ONLY_SHOW_L3R3] = settings.onlyShowL3R3
        prefs[StreamPreferenceKeys.SHOW_GUIDE_BUTTON] = settings.showGuideButton
        prefs[StreamPreferenceKeys.VIBRATE_OSC] = settings.vibrateOsc
        prefs[StreamPreferenceKeys.VIBRATE_FALLBACK] = settings.vibrateFallback
        prefs[StreamPreferenceKeys.VIBRATE_FALLBACK_STRENGTH] = settings.vibrateFallbackStrength
        prefs[StreamPreferenceKeys.FLIP_FACE_BUTTONS] = settings.flipFaceButtons
        prefs[StreamPreferenceKeys.GAMEPAD_TOUCHPAD_AS_MOUSE] = settings.gamepadTouchpadAsMouse
        prefs[StreamPreferenceKeys.GAMEPAD_MOTION_SENSORS] = settings.gamepadMotionSensors
        prefs[StreamPreferenceKeys.GAMEPAD_MOTION_FALLBACK] = settings.gamepadMotionFallback
        prefs[StreamPreferenceKeys.FORCE_MOTION_SENSORS_FALLBACK] = settings.forceMotionSensorsFallback
        prefs[StreamPreferenceKeys.ENABLE_RUMBLE] = settings.enableRumble
        prefs[StreamPreferenceKeys.ENABLE_BATTERY_REPORT] = settings.enableBatteryReport
        prefs[StreamPreferenceKeys.FORCE_QWERTY] = settings.forceQwerty
        prefs[StreamPreferenceKeys.BACK_AS_META] = settings.backAsMeta
        prefs[StreamPreferenceKeys.RIGHT_ALT_AS_META] = settings.rightAltAsMeta
        prefs[StreamPreferenceKeys.IGNORE_SYNTH_EVENTS] = settings.ignoreSynthEvents
        prefs[StreamPreferenceKeys.BACK_AS_GUIDE] = settings.backAsGuide
        prefs[StreamPreferenceKeys.DEADZONE] = settings.deadzonePercentage
        prefs[StreamPreferenceKeys.OSC_OPACITY] = settings.oscOpacity
        prefs[StreamPreferenceKeys.ONSCREEN_KEYBOARD_ALIGN_MODE] = settings.onscreenKeyboardAlignMode
        prefs[StreamPreferenceKeys.ENABLE_KEYBOARD] = settings.enableKeyboard
        prefs[StreamPreferenceKeys.ENABLE_KEYBOARD_VIBRATE] = settings.enableKeyboardVibrate
        prefs[StreamPreferenceKeys.ENABLE_COMMIT_TEXT] = settings.enableCommitText

        prefs[StreamPreferenceKeys.SMALL_ICON_MODE] = settings.smallIconMode
        prefs[StreamPreferenceKeys.LANGUAGE] = settings.language
        prefs[StreamPreferenceKeys.DISABLE_WARNINGS] = settings.disableWarnings
        prefs[StreamPreferenceKeys.LATENCY_TOAST] = settings.enableLatencyToast
        prefs[StreamPreferenceKeys.ENABLE_PERF_OVERLAY] = settings.enablePerfOverlay
        prefs[StreamPreferenceKeys.ENABLE_PERF_LOGGING] = settings.enablePerfLogging
        prefs[StreamPreferenceKeys.RESUME_WITHOUT_CONFIRM] = settings.resumeWithoutConfirm
        prefs[StreamPreferenceKeys.FULL_SCREEN] = settings.fullScreen
        prefs[StreamPreferenceKeys.ENABLE_PIP] = settings.enablePip
    }
}
