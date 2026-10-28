@file:Suppress("LongMethod")

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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StreamIntentParserTest {
    @Test
    fun applyStreamExtras_withNoExtras_doesNotModifyConfig() {
        val intent = Intent()
        val prefConfig = PreferenceConfiguration().apply {
            width = 1280
            height = 720
            fps = 60f
            bitrate = 10000
        }

        StreamIntentParser.applyStreamExtras(intent, prefConfig)

        assertEquals(1280, prefConfig.width)
        assertEquals(720, prefConfig.height)
        assertEquals(60f, prefConfig.fps, 0.01f)
        assertEquals(10000, prefConfig.bitrate)
    }

    @Test
    fun applyStreamExtras_withValidExtras_updatesAllFields() {
        val intent = Intent().apply {
            putExtra(StreamIntentParser.EXTRA_STREAM_WIDTH, 2560)
            putExtra(StreamIntentParser.EXTRA_STREAM_HEIGHT, 1440)
            putExtra(StreamIntentParser.EXTRA_STREAM_FPS, 120)
            putExtra(StreamIntentParser.EXTRA_STREAM_BITRATE, 40000)
        }
        val prefConfig = PreferenceConfiguration().apply {
            width = 1280
            height = 720
            fps = 60f
            bitrate = 10000
        }

        StreamIntentParser.applyStreamExtras(intent, prefConfig)

        assertEquals(2560, prefConfig.width)
        assertEquals(1440, prefConfig.height)
        assertEquals(120f, prefConfig.fps, 0.01f)
        assertEquals(40000, prefConfig.bitrate)
    }

    @Test
    fun applyStreamExtras_withInvalidValues_ignoresThem() {
        val intent = Intent().apply {
            putExtra(StreamIntentParser.EXTRA_STREAM_WIDTH, -1)
            putExtra(StreamIntentParser.EXTRA_STREAM_HEIGHT, 0)
            putExtra(StreamIntentParser.EXTRA_STREAM_FPS, -60)
            putExtra(StreamIntentParser.EXTRA_STREAM_BITRATE, 0)
        }
        val prefConfig = PreferenceConfiguration().apply {
            width = 1920
            height = 1080
            fps = 60f
            bitrate = 20000
        }

        StreamIntentParser.applyStreamExtras(intent, prefConfig)

        assertEquals(1920, prefConfig.width)
        assertEquals(1080, prefConfig.height)
        assertEquals(60f, prefConfig.fps, 0.01f)
        assertEquals(20000, prefConfig.bitrate)
    }

    @Test
    fun putAndApplyStreamSettingsExtras_roundTripsAllSettingsIntoPreferenceConfiguration() {
        val customSettings =
            StreamSettings(
                width = 3840,
                height = 2160,
                fps = 90,
                bitrateKbps = 80_000,
                meteredBitrateKbps = 15_000,
                videoScaleMode = VideoScaleMode.FILL,
                videoFormat = VideoFormatOption.FORCE_HEVC,
                enableHdr = true,
                fullRange = true,
                enableUltraLowLatency = true,
                framePacing = FramePacingMode.BALANCED,
                reduceRefreshRate = true,
                enforceDisplayMode = true,
                autoInvertVideoResolution = false,
                videoSuperResolution = true,
                unlockFps = true,
                audioConfig = AudioConfiguration.SURROUND_71,
                hostAudio = true,
                enableAudioFx = true,
                multiController = false,
                usbDriver = false,
                bindAllUsb = true,
                mouseEmulation = false,
                rememberMouseMode = true,
                absoluteMouseMode = true,
                mouseNavButtons = true,
                analogStickScrolling = AnalogStickScrolling.LEFT,
                onscreenController = true,
                hideOscWhenHasGamepad = false,
                onlyShowL3R3 = true,
                showGuideButton = false,
                vibrateOsc = false,
                vibrateFallback = true,
                vibrateFallbackStrength = 75,
                flipFaceButtons = true,
                gamepadTouchpadAsMouse = true,
                gamepadMotionSensors = false,
                gamepadMotionFallback = true,
                forceMotionSensorsFallback = true,
                enableRumble = false,
                enableBatteryReport = false,
                forceQwerty = false,
                backAsMeta = true,
                rightAltAsMeta = true,
                ignoreSynthEvents = true,
                backAsGuide = true,
                deadzonePercentage = 15,
                oscOpacity = 65,
                enableCommitText = true,
                trackpadSensitivityX = 140,
                trackpadSensitivityY = 130,
                trackpadDragDropVibration = true,
                trackpadDragDropThreshold = 300,
                trackpadSwapAxis = true,
                disableWarnings = true,
                enableLatencyToast = true,
                enablePerfOverlay = true,
                enablePerfLogging = true,
                enablePerfOverlayBottom = true,
                resumeWithoutConfirm = true,
                fullScreen = false,
                enableFloatingButton = true,
                showOverlayZoomToggleButton = true,
                enablePip = true,
            )

        val intent = Intent()
        StreamIntentParser.putStreamSettingsExtras(intent, customSettings)

        val prefConfig = PreferenceConfiguration()
        StreamIntentParser.applyStreamExtras(intent, prefConfig)

        assertEquals(3840, prefConfig.width)
        assertEquals(2160, prefConfig.height)
        assertEquals(90f, prefConfig.fps, 0.01f)
        assertEquals(80_000, prefConfig.bitrate)
        assertEquals(15_000, prefConfig.meteredBitrate)
        assertEquals(PreferenceConfiguration.ScaleMode.FILL, prefConfig.videoScaleMode)
        assertEquals(PreferenceConfiguration.FormatOption.FORCE_HEVC, prefConfig.videoFormat)
        assertTrue(prefConfig.enableHdr)
        assertTrue(prefConfig.fullRange)
        assertTrue(prefConfig.enableUltraLowLatency)
        assertEquals(PreferenceConfiguration.FRAME_PACING_BALANCED, prefConfig.framePacing)
        assertTrue(prefConfig.reduceRefreshRate)
        assertTrue(prefConfig.enforceDisplayMode)
        assertFalse(prefConfig.autoInvertVideoResolution)
        assertTrue(prefConfig.videoSr)
        assertTrue(prefConfig.unlockFps)
        assertEquals(MoonBridge.AudioConfiguration(8, 0x63F), prefConfig.audioConfiguration)
        assertTrue(prefConfig.playHostAudio)
        assertTrue(prefConfig.enableAudioFx)
        assertFalse(prefConfig.multiController)
        assertFalse(prefConfig.usbDriver)
        assertTrue(prefConfig.bindAllUsb)
        assertFalse(prefConfig.mouseEmulation)
        assertTrue(prefConfig.rememberMouseMode)
        assertTrue(prefConfig.absoluteMouseMode)
        assertTrue(prefConfig.mouseNavButtons)
        assertEquals(PreferenceConfiguration.AnalogStickForScrolling.LEFT, prefConfig.analogStickForScrolling)
        assertTrue(prefConfig.onscreenController)
        assertFalse(prefConfig.hideOSCWhenHasGamepad)
        assertTrue(prefConfig.onlyL3R3)
        assertFalse(prefConfig.showGuideButton)
        assertFalse(prefConfig.vibrateOsc)
        assertTrue(prefConfig.vibrateFallbackToDevice)
        assertEquals(75, prefConfig.vibrateFallbackToDeviceStrength)
        assertTrue(prefConfig.flipFaceButtons)
        assertTrue(prefConfig.gamepadTouchpadAsMouse)
        assertFalse(prefConfig.gamepadMotionSensors)
        assertTrue(prefConfig.gamepadMotionSensorsFallbackToDevice)
        assertTrue(prefConfig.forceMotionSensorsFallbackToDevice)
        assertFalse(prefConfig.enableRumble)
        assertFalse(prefConfig.enableBatteryReport)
        assertFalse(prefConfig.forceQwerty)
        assertTrue(prefConfig.backAsMeta)
        assertTrue(prefConfig.rightAltAsMeta)
        assertTrue(prefConfig.ignoreSynthEvents)
        assertTrue(prefConfig.backAsGuide)
        assertEquals(15, prefConfig.deadzonePercentage)
        assertEquals(65, prefConfig.oscOpacity)
        assertTrue(prefConfig.enableCommitText)
        assertEquals(140, prefConfig.trackpadSensitivityX)
        assertEquals(130, prefConfig.trackpadSensitivityY)
        assertTrue(prefConfig.trackpadDragDropVibration)
        assertEquals(300, prefConfig.trackpadDragDropThreshold)
        assertTrue(prefConfig.trackpadSwapAxis)
        assertTrue(prefConfig.disableWarnings)
        assertTrue(prefConfig.enableLatencyToast)
        assertTrue(prefConfig.enablePerfOverlay)
        assertTrue(prefConfig.enablePerfLogging)
        assertTrue(prefConfig.enablePerfOverlayBottom)
        assertTrue(prefConfig.resumeWithoutConfirm)
        assertFalse(prefConfig.fullScreen)
        assertTrue(prefConfig.enableFloatingButton)
        assertTrue(prefConfig.showOverlayZoomToggleButton)
        assertTrue(prefConfig.enablePip)
    }
}
