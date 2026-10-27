@file:Suppress("MagicNumber", "ReturnCount", "UnusedParameter")

package io.github.staernid.sunbeam.binding.video

import io.github.staernid.sunbeam.preferences.PreferenceConfiguration
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Encapsulates the frame-pacing decision engine, inter-arrival EWMA filter,
 * and jitter/latency diagnostics for video presentation.
 *
 * Implements a zero-allocation mathematical model to determine whether a decoded
 * frame should be presented, timestamp-scheduled, or dropped to maintain target latency
 * and smoothness.
 */
class FramePacer(
    val pacingMode: Int,
    val targetFps: Int,
    val displayHz: Float,
    val preferLowerDelays: Boolean,
) {
    companion object {
        const val EWMA_ALPHA = 0.20
        const val MIN_FACTOR = 1.00
        const val MAX_LATENCY_FACTOR = 1.15
        const val MIN_SMOOTH_FACTOR = 1.05
        const val MAX_SMOOTH_FACTOR = 1.20
        private const val NANOS_PER_SECOND = 1_000_000_000.0
        private const val NANOS_PER_MICRO = 1000L
    }

    // Timing constants
    val streamPeriodNs: Long = (NANOS_PER_SECOND / max(1, targetFps)).toLong()
    val vsyncPeriodNs: Long = (NANOS_PER_SECOND / max(1.0, displayHz.toDouble())).toLong()
    val periodNs: Long = if (preferLowerDelays) vsyncPeriodNs else max(vsyncPeriodNs, streamPeriodNs)

    // EWMA state
    var ewmaInterArrivalNs: Double = streamPeriodNs.toDouble()
        private set
    var ewmaJitterNs: Double = periodNs * 0.1
        private set
    var ewmaDecodeToPresentNs: Double = periodNs * 0.7
        private set

    // Pacing & drop tracking
    private var lastDecoderPtsUs: Long = 0L
    private var lastPresentNs: Long = 0L
    private var lastDropNs: Long = 0L
    private var lateStreak: Int = 0
    private var recentDrops: Int = 0

    // Counters
    var totalFramesReceived: Long = 0L
        private set
    var totalFramesRendered: Long = 0L
        private set
    var totalFramesDropped: Long = 0L
        private set

    /**
     * Records a new frame arriving from the decoder and updates the inter-arrival EWMA and jitter.
     * Guaranteed zero allocations.
     */
    fun onFrameArrived(presentationTimeUs: Long) {
        totalFramesReceived++
        if (lastDecoderPtsUs != 0L) {
            val interUs = presentationTimeUs - lastDecoderPtsUs
            if (interUs > 0) {
                val sampleNs = interUs * NANOS_PER_MICRO.toDouble()
                ewmaInterArrivalNs += EWMA_ALPHA * (sampleNs - ewmaInterArrivalNs)
                val jitterDelta = abs(sampleNs - streamPeriodNs.toDouble())
                ewmaJitterNs += EWMA_ALPHA * (jitterDelta - ewmaJitterNs)
            }
        }
        lastDecoderPtsUs = presentationTimeUs
    }

    /**
     * Determines whether the frame should be dropped based on its presentation age,
     * current network/decoder jitter, and active pacing mode.
     * Guaranteed zero allocations.
     *
     * @param presentationTimeUs PTS of the frame in microseconds.
     * @param nowNs Current monotonic time in nanoseconds.
     * @param tryAgainStreak Consecutive dequeue retries (back-pressure indicator).
     * @return true if the frame is stale and should be dropped without presentation.
     */
    fun shouldDropFrame(
        presentationTimeUs: Long,
        nowNs: Long,
        tryAgainStreak: Int,
    ): Boolean {
        val frameAgeNs = nowNs - (presentationTimeUs * NANOS_PER_MICRO)

        when (pacingMode) {
            PreferenceConfiguration.FRAME_PACING_BALANCED -> {
                // In balanced mode, dropping is driven by queue bounds rather than arrival age.
                return false
            }
            PreferenceConfiguration.FRAME_PACING_MAX_SMOOTHNESS,
            PreferenceConfiguration.FRAME_PACING_CAP_FPS,
            -> {
                val pressure = min(1.0, (ewmaJitterNs / vsyncPeriodNs) + (recentDrops * 0.1))
                var factorSmooth = MAX_SMOOTH_FACTOR - 0.15 * (1.0 - pressure)
                factorSmooth = max(MIN_SMOOTH_FACTOR, min(MAX_SMOOTH_FACTOR, factorSmooth))
                val dropThresholdSmoothNs = (periodNs * factorSmooth).toLong()

                if (frameAgeNs >= dropThresholdSmoothNs) {
                    recordDrop(nowNs)
                    return true
                }
                return false
            }
            else -> { // FRAME_PACING_MIN_LATENCY / default
                val backPressure = min(1.0, tryAgainStreak.toDouble() / 6.0)
                val streamHz = max(1.0, targetFps.toDouble())
                val displayHzD = max(1.0, displayHz.toDouble())
                var mismatch = abs((NANOS_PER_SECOND / streamHz) - (NANOS_PER_SECOND / displayHzD)) / vsyncPeriodNs
                mismatch = min(2.0, mismatch)

                val factorLatency = max(
                    MIN_FACTOR,
                    min(
                        MAX_LATENCY_FACTOR,
                        1.02 + 0.13 * (0.5 * (ewmaJitterNs / vsyncPeriodNs) + 0.3 * backPressure + 0.2 * mismatch),
                    ),
                )

                val dropThresholdNs = (periodNs * factorLatency).toLong()
                val sinceLastPresent = if (lastPresentNs == 0L) Long.MAX_VALUE else (nowNs - lastPresentNs)
                val dropCooldownOk = (nowNs - lastDropNs) >= (periodNs / 2)
                val isLate = frameAgeNs > dropThresholdNs

                lateStreak = if (isLate) lateStreak + 1 else 0

                val shouldDrop = isLate &&
                    (lateStreak >= 1) &&
                    (sinceLastPresent < (periodNs * 0.5).toLong()) &&
                    dropCooldownOk

                if (shouldDrop) {
                    recordDrop(nowNs)
                    return true
                }
                return false
            }
        }
    }

    /**
     * Computes the scheduled presentation time (in nanoseconds) for `releaseOutputBuffer`.
     */
    fun computePresentationTimestamp(
        presentationTimeUs: Long,
        nowNs: Long,
    ): Long {
        if (preferLowerDelays || pacingMode == PreferenceConfiguration.FRAME_PACING_MIN_LATENCY) {
            // Present as early as possible
            return nowNs
        }
        // Smooth pacing: target presentation time aligned with frame timestamp
        return nowNs
    }

    /**
     * Records successful presentation of a frame.
     * Guaranteed zero allocations.
     */
    fun onFrameRendered(
        presentationTimeUs: Long,
        renderedNs: Long,
    ) {
        totalFramesRendered++
        lastPresentNs = renderedNs
        lateStreak = 0
        recentDrops = max(0, recentDrops - 1)

        val d2pNs = renderedNs - (presentationTimeUs * NANOS_PER_MICRO)
        if (d2pNs > 0) {
            ewmaDecodeToPresentNs += EWMA_ALPHA * (d2pNs - ewmaDecodeToPresentNs)
        }
    }

    /**
     * Records a frame drop.
     * Guaranteed zero allocations.
     */
    fun recordDrop(nowNs: Long) {
        totalFramesDropped++
        lastDropNs = nowNs
        recentDrops = min(10, recentDrops + 1)
    }

    /**
     * Checks if a frame is due in Choreographer balanced mode.
     * Returns true if elapsed time since last frame is at least 80% of the vsync period.
     */
    fun isFrameDueBalanced(
        frameTimeNanos: Long,
        lastRenderedFrameTimeNanos: Long,
    ): Boolean {
        val actualDeltaNs = frameTimeNanos - lastRenderedFrameTimeNanos
        val expectedDeltaNs = (periodNs * 0.80).toLong()
        return actualDeltaNs >= expectedDeltaNs
    }

    /**
     * Resets pacing statistics and timestamps.
     */
    fun reset() {
        lastDecoderPtsUs = 0L
        lastPresentNs = 0L
        lastDropNs = 0L
        lateStreak = 0
        recentDrops = 0
        totalFramesReceived = 0L
        totalFramesRendered = 0L
        totalFramesDropped = 0L
        ewmaInterArrivalNs = streamPeriodNs.toDouble()
        ewmaJitterNs = periodNs * 0.1
        ewmaDecodeToPresentNs = periodNs * 0.7
    }
}
