package io.github.staernid.sunbeam.core.preferences.datastore

import android.content.SharedPreferences
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
            lastSelectedHostUuid =
                prefs[StreamPreferenceKeys.LAST_SELECTED_HOST_UUID]
                    ?: defaultSettings.lastSelectedHostUuid,
            oledBlackMode = prefs[StreamPreferenceKeys.OLED_BLACK_MODE] ?: defaultSettings.oledBlackMode,
            dynamicColor = prefs[StreamPreferenceKeys.DYNAMIC_COLOR] ?: defaultSettings.dynamicColor,
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
        prefs[StreamPreferenceKeys.OLED_BLACK_MODE] = settings.oledBlackMode
        prefs[StreamPreferenceKeys.DYNAMIC_COLOR] = settings.dynamicColor
        settings.lastSelectedHostUuid?.let {
            prefs[StreamPreferenceKeys.LAST_SELECTED_HOST_UUID] = it
        } ?: prefs.remove(StreamPreferenceKeys.LAST_SELECTED_HOST_UUID)
    }

    @Suppress("LongMethod")
    fun toSharedPreferences(
        settings: StreamSettings,
        prefs: SharedPreferences,
    ) {
        val editor =
            prefs.edit()
                .putString(StreamPreferenceKeys.RESOLUTION.name, "${settings.width}x${settings.height}")
                .putString(StreamPreferenceKeys.FPS.name, settings.fps.toString())
                .putInt(StreamPreferenceKeys.BITRATE_KBPS.name, settings.bitrateKbps)
                .putInt(StreamPreferenceKeys.METERED_BITRATE_KBPS.name, settings.meteredBitrateKbps)
                .putString(StreamPreferenceKeys.VIDEO_SCALE_MODE.name, settings.videoScaleMode.prefValue)
                .putString(StreamPreferenceKeys.VIDEO_FORMAT.name, settings.videoFormat.prefValue)
                .putBoolean(StreamPreferenceKeys.ENABLE_HDR.name, settings.enableHdr)
                .putBoolean(StreamPreferenceKeys.FULL_RANGE.name, settings.fullRange)
                .putBoolean(StreamPreferenceKeys.ULTRA_LOW_LATENCY.name, settings.enableUltraLowLatency)
                .putString(StreamPreferenceKeys.FRAME_PACING.name, settings.framePacing.prefValue)
                .putBoolean(StreamPreferenceKeys.REDUCE_REFRESH_RATE.name, settings.reduceRefreshRate)
                .putBoolean(StreamPreferenceKeys.ENFORCE_DISPLAY_MODE.name, settings.enforceDisplayMode)
                .putBoolean(StreamPreferenceKeys.AUTO_INVERT_RESOLUTION.name, settings.autoInvertVideoResolution)
                .putBoolean(StreamPreferenceKeys.VIDEO_SR.name, settings.videoSuperResolution)
                .putBoolean(StreamPreferenceKeys.UNLOCK_FPS.name, settings.unlockFps)
                .putString(StreamPreferenceKeys.AUDIO_CONFIG.name, settings.audioConfig.prefValue)
                .putBoolean(StreamPreferenceKeys.HOST_AUDIO.name, settings.hostAudio)
                .putBoolean(StreamPreferenceKeys.ENABLE_AUDIO_FX.name, settings.enableAudioFx)
                .putBoolean(StreamPreferenceKeys.MULTI_CONTROLLER.name, settings.multiController)
                .putBoolean(StreamPreferenceKeys.USB_DRIVER.name, settings.usbDriver)
                .putBoolean(StreamPreferenceKeys.BIND_ALL_USB.name, settings.bindAllUsb)
                .putBoolean(StreamPreferenceKeys.MOUSE_EMULATION.name, settings.mouseEmulation)
                .putBoolean(StreamPreferenceKeys.REMEMBER_MOUSE_MODE.name, settings.rememberMouseMode)
                .putBoolean(StreamPreferenceKeys.ABSOLUTE_MOUSE_MODE.name, settings.absoluteMouseMode)
                .putBoolean(StreamPreferenceKeys.MOUSE_NAV_BUTTONS.name, settings.mouseNavButtons)
                .putString(StreamPreferenceKeys.ANALOG_SCROLLING.name, settings.analogStickScrolling.prefValue)
                .putBoolean(StreamPreferenceKeys.ONSCREEN_CONTROLLER.name, settings.onscreenController)
                .putBoolean(StreamPreferenceKeys.HIDE_OSC_WHEN_HAS_GAMEPAD.name, settings.hideOscWhenHasGamepad)
                .putBoolean(StreamPreferenceKeys.ONLY_SHOW_L3R3.name, settings.onlyShowL3R3)
                .putBoolean(StreamPreferenceKeys.SHOW_GUIDE_BUTTON.name, settings.showGuideButton)
                .putBoolean(StreamPreferenceKeys.VIBRATE_OSC.name, settings.vibrateOsc)
                .putBoolean(StreamPreferenceKeys.VIBRATE_FALLBACK.name, settings.vibrateFallback)
                .putInt(StreamPreferenceKeys.VIBRATE_FALLBACK_STRENGTH.name, settings.vibrateFallbackStrength)
                .putBoolean(StreamPreferenceKeys.FLIP_FACE_BUTTONS.name, settings.flipFaceButtons)
                .putBoolean(StreamPreferenceKeys.GAMEPAD_TOUCHPAD_AS_MOUSE.name, settings.gamepadTouchpadAsMouse)
                .putBoolean(StreamPreferenceKeys.GAMEPAD_MOTION_SENSORS.name, settings.gamepadMotionSensors)
                .putBoolean(StreamPreferenceKeys.GAMEPAD_MOTION_FALLBACK.name, settings.gamepadMotionFallback)
                .putBoolean(
                    StreamPreferenceKeys.FORCE_MOTION_SENSORS_FALLBACK.name,
                    settings.forceMotionSensorsFallback,
                )
                .putBoolean(StreamPreferenceKeys.ENABLE_RUMBLE.name, settings.enableRumble)
                .putBoolean(StreamPreferenceKeys.ENABLE_BATTERY_REPORT.name, settings.enableBatteryReport)
                .putBoolean(StreamPreferenceKeys.FORCE_QWERTY.name, settings.forceQwerty)
                .putBoolean(StreamPreferenceKeys.BACK_AS_META.name, settings.backAsMeta)
                .putBoolean(StreamPreferenceKeys.RIGHT_ALT_AS_META.name, settings.rightAltAsMeta)
                .putBoolean(StreamPreferenceKeys.IGNORE_SYNTH_EVENTS.name, settings.ignoreSynthEvents)
                .putBoolean(StreamPreferenceKeys.BACK_AS_GUIDE.name, settings.backAsGuide)
                .putInt(StreamPreferenceKeys.DEADZONE.name, settings.deadzonePercentage)
                .putInt(StreamPreferenceKeys.OSC_OPACITY.name, settings.oscOpacity)
                .putString(StreamPreferenceKeys.ONSCREEN_KEYBOARD_ALIGN_MODE.name, settings.onscreenKeyboardAlignMode)
                .putBoolean(StreamPreferenceKeys.ENABLE_KEYBOARD.name, settings.enableKeyboard)
                .putBoolean(StreamPreferenceKeys.ENABLE_KEYBOARD_VIBRATE.name, settings.enableKeyboardVibrate)
                .putBoolean(StreamPreferenceKeys.ENABLE_COMMIT_TEXT.name, settings.enableCommitText)
                .putBoolean(StreamPreferenceKeys.SMALL_ICON_MODE.name, settings.smallIconMode)
                .putString(StreamPreferenceKeys.LANGUAGE.name, settings.language)
                .putBoolean(StreamPreferenceKeys.DISABLE_WARNINGS.name, settings.disableWarnings)
                .putBoolean(StreamPreferenceKeys.LATENCY_TOAST.name, settings.enableLatencyToast)
                .putBoolean(StreamPreferenceKeys.ENABLE_PERF_OVERLAY.name, settings.enablePerfOverlay)
                .putBoolean(StreamPreferenceKeys.ENABLE_PERF_LOGGING.name, settings.enablePerfLogging)
                .putBoolean(StreamPreferenceKeys.RESUME_WITHOUT_CONFIRM.name, settings.resumeWithoutConfirm)
                .putBoolean(StreamPreferenceKeys.FULL_SCREEN.name, settings.fullScreen)
                .putBoolean(StreamPreferenceKeys.ENABLE_PIP.name, settings.enablePip)
                .putBoolean(StreamPreferenceKeys.OLED_BLACK_MODE.name, settings.oledBlackMode)
                .putBoolean(StreamPreferenceKeys.DYNAMIC_COLOR.name, settings.dynamicColor)

        settings.customResolution?.let { editor.putString(StreamPreferenceKeys.CUSTOM_RESOLUTION.name, it) }
        settings.customRefreshRate?.let { editor.putString(StreamPreferenceKeys.CUSTOM_REFRESH_RATE.name, it) }
        settings.customBitrateKbps?.let { editor.putString(StreamPreferenceKeys.CUSTOM_BITRATE.name, it.toString()) }
        settings.lastSelectedHostUuid?.let {
            editor.putString(StreamPreferenceKeys.LAST_SELECTED_HOST_UUID.name, it)
        }
        editor.apply()
    }
}
