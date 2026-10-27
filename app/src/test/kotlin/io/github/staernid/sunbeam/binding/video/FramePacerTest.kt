package io.github.staernid.sunbeam.binding.video

import io.github.staernid.sunbeam.preferences.PreferenceConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FramePacerTest {
    @Test
    fun testInitialTimingParameters() {
        val pacer = FramePacer(
            pacingMode = PreferenceConfiguration.FRAME_PACING_MIN_LATENCY,
            targetFps = 60,
            displayHz = 60f,
            preferLowerDelays = false,
        )

        assertEquals(16_666_666L, pacer.streamPeriodNs)
        assertEquals(16_666_666L, pacer.vsyncPeriodNs)
        assertEquals(16_666_666L, pacer.periodNs)
    }

    @Test
    fun testAdaptivePeriodWithHighRefreshDisplay() {
        val pacer = FramePacer(
            pacingMode = PreferenceConfiguration.FRAME_PACING_MIN_LATENCY,
            targetFps = 60,
            displayHz = 120f,
            preferLowerDelays = false,
        )

        assertEquals(16_666_666L, pacer.streamPeriodNs)
        assertEquals(8_333_333L, pacer.vsyncPeriodNs)
        // With preferLowerDelays false, periodNs is max(vsync, stream) = 16_666_666L
        assertEquals(16_666_666L, pacer.periodNs)

        val pacerUll = FramePacer(
            pacingMode = PreferenceConfiguration.FRAME_PACING_MIN_LATENCY,
            targetFps = 60,
            displayHz = 120f,
            preferLowerDelays = true,
        )
        // With preferLowerDelays true, periodNs tracks vsync = 8_333_333L
        assertEquals(8_333_333L, pacerUll.periodNs)
    }

    @Test
    fun testInterArrivalAndJitterEwma() {
        val pacer = FramePacer(
            pacingMode = PreferenceConfiguration.FRAME_PACING_MIN_LATENCY,
            targetFps = 60,
            displayHz = 60f,
            preferLowerDelays = false,
        )

        // Initial jitter is periodNs * 0.1
        val initialJitter = pacer.ewmaJitterNs

        // First frame arriving at t=1000us
        pacer.onFrameArrived(1000L)
        assertEquals(1L, pacer.totalFramesReceived)

        // Second frame arrives exactly 16.666ms later (17666us) -> 0 jitter
        pacer.onFrameArrived(17666L)
        assertEquals(2L, pacer.totalFramesReceived)
        // Jitter should decrease towards 0
        assertTrue("Jitter should decrease when frames arrive on schedule", pacer.ewmaJitterNs < initialJitter)

        // Third frame arrives with big delay (e.g. +33.333ms, 51000us) -> jitter spikes
        pacer.onFrameArrived(51000L)
        assertTrue("Jitter should spike on high inter-arrival variance", pacer.ewmaJitterNs > 0.0)
    }

    @Test
    fun testLowLatencyDropDecision() {
        val pacer = FramePacer(
            pacingMode = PreferenceConfiguration.FRAME_PACING_MIN_LATENCY,
            targetFps = 60,
            displayHz = 60f,
            preferLowerDelays = false,
        )

        val basePtsUs = 100_000L
        val nowNs = basePtsUs * 1000L + 5_000_000L // 5ms age (well within 16.6ms)

        // Fresh frame should NOT be dropped
        assertFalse(pacer.shouldDropFrame(basePtsUs, nowNs, tryAgainStreak = 0))

        // Render this frame to establish lastPresentNs
        pacer.onFrameRendered(basePtsUs, nowNs)
        assertEquals(1L, pacer.totalFramesRendered)

        // Next frame arrives 2ms later (very close to last present) but PTS is stale by 30ms (> drop threshold)
        val stalePtsUs = basePtsUs - 30_000L
        val currentNs = nowNs + 2_000_000L // 2ms after last present

        // Stale frame arriving right after a presentation should be dropped to prevent queue backup
        val dropped = pacer.shouldDropFrame(stalePtsUs, currentNs, tryAgainStreak = 0)
        assertTrue("Late frame should be dropped", dropped)
        assertEquals(1L, pacer.totalFramesDropped)
    }

    @Test
    fun testBalancedFrameDueCalculation() {
        val pacer = FramePacer(
            pacingMode = PreferenceConfiguration.FRAME_PACING_BALANCED,
            targetFps = 60,
            displayHz = 60f,
            preferLowerDelays = false,
        )

        val lastRenderedNs = 1_000_000_000L
        val tooEarlyNs = lastRenderedNs + 10_000_000L // 10ms (less than 80% of 16.6ms = 13.3ms)
        assertFalse(pacer.isFrameDueBalanced(tooEarlyNs, lastRenderedNs))

        val readyNs = lastRenderedNs + 14_000_000L // 14ms (>= 13.3ms)
        assertTrue(pacer.isFrameDueBalanced(readyNs, lastRenderedNs))
    }

    @Test
    fun testResetClearsCounters() {
        val pacer = FramePacer(
            pacingMode = PreferenceConfiguration.FRAME_PACING_MIN_LATENCY,
            targetFps = 60,
            displayHz = 60f,
            preferLowerDelays = false,
        )

        pacer.onFrameArrived(1000L)
        pacer.onFrameRendered(1000L, 2_000_000L)
        pacer.recordDrop(3_000_000L)

        assertEquals(1L, pacer.totalFramesReceived)
        assertEquals(1L, pacer.totalFramesRendered)
        assertEquals(1L, pacer.totalFramesDropped)

        pacer.reset()

        assertEquals(0L, pacer.totalFramesReceived)
        assertEquals(0L, pacer.totalFramesRendered)
        assertEquals(0L, pacer.totalFramesDropped)
    }
}
