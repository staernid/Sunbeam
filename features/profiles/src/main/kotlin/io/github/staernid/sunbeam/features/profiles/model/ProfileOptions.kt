@file:Suppress("LongParameterList")

package io.github.staernid.sunbeam.features.profiles.model

import io.github.staernid.sunbeam.core.data.model.Profile
import java.util.UUID

/**
 * Extension helpers to map [Profile.options] map keys to strongly-typed stream and control settings.
 */
object ProfileOptions {
    const val KEY_WIDTH = "width"
    const val KEY_HEIGHT = "height"
    const val KEY_FPS = "fps"
    const val KEY_BITRATE_KBPS = "bitrate_kbps"
    const val KEY_STRETCH = "stretch"
    const val KEY_OSC_ENABLED = "osc_enabled"
    const val KEY_OSC_OPACITY = "osc_opacity"

    const val DEFAULT_WIDTH = 1920
    const val DEFAULT_HEIGHT = 1080
    const val DEFAULT_FPS = 60
    const val DEFAULT_BITRATE_KBPS = 20_000
    const val DEFAULT_STRETCH = false
    const val DEFAULT_OSC_ENABLED = false
    const val DEFAULT_OSC_OPACITY = 0.7f
}

val Profile.width: Int
    get() = (options[ProfileOptions.KEY_WIDTH] as? Number)?.toInt() ?: ProfileOptions.DEFAULT_WIDTH

val Profile.height: Int
    get() = (options[ProfileOptions.KEY_HEIGHT] as? Number)?.toInt() ?: ProfileOptions.DEFAULT_HEIGHT

val Profile.fps: Int
    get() = (options[ProfileOptions.KEY_FPS] as? Number)?.toInt() ?: ProfileOptions.DEFAULT_FPS

val Profile.bitrateKbps: Int
    get() = (options[ProfileOptions.KEY_BITRATE_KBPS] as? Number)?.toInt() ?: ProfileOptions.DEFAULT_BITRATE_KBPS

val Profile.stretchMode: Boolean
    get() = (options[ProfileOptions.KEY_STRETCH] as? Boolean) ?: ProfileOptions.DEFAULT_STRETCH

val Profile.oscEnabled: Boolean
    get() = (options[ProfileOptions.KEY_OSC_ENABLED] as? Boolean) ?: ProfileOptions.DEFAULT_OSC_ENABLED

val Profile.oscOpacity: Float
    get() = (options[ProfileOptions.KEY_OSC_OPACITY] as? Number)?.toFloat() ?: ProfileOptions.DEFAULT_OSC_OPACITY

/**
 * Creates a new [Profile] with updated configuration options.
 */
fun Profile.copyWithOptions(
    name: String = this.name,
    width: Int = this.width,
    height: Int = this.height,
    fps: Int = this.fps,
    bitrateKbps: Int = this.bitrateKbps,
    stretchMode: Boolean = this.stretchMode,
    oscEnabled: Boolean = this.oscEnabled,
    oscOpacity: Float = this.oscOpacity,
    modifiedUtc: Long = System.currentTimeMillis(),
): Profile {
    val newOptions = options.toMutableMap().apply {
        put(ProfileOptions.KEY_WIDTH, width)
        put(ProfileOptions.KEY_HEIGHT, height)
        put(ProfileOptions.KEY_FPS, fps)
        put(ProfileOptions.KEY_BITRATE_KBPS, bitrateKbps)
        put(ProfileOptions.KEY_STRETCH, stretchMode)
        put(ProfileOptions.KEY_OSC_ENABLED, oscEnabled)
        put(ProfileOptions.KEY_OSC_OPACITY, oscOpacity)
    }
    return copy(
        name = name,
        modifiedUtc = modifiedUtc,
        options = newOptions,
    )
}

/**
 * Factory to construct a fresh [Profile].
 */
fun createNewProfile(
    name: String,
    width: Int = ProfileOptions.DEFAULT_WIDTH,
    height: Int = ProfileOptions.DEFAULT_HEIGHT,
    fps: Int = ProfileOptions.DEFAULT_FPS,
    bitrateKbps: Int = ProfileOptions.DEFAULT_BITRATE_KBPS,
    stretchMode: Boolean = ProfileOptions.DEFAULT_STRETCH,
    oscEnabled: Boolean = ProfileOptions.DEFAULT_OSC_ENABLED,
    oscOpacity: Float = ProfileOptions.DEFAULT_OSC_OPACITY,
): Profile {
    val options = mapOf(
        ProfileOptions.KEY_WIDTH to width,
        ProfileOptions.KEY_HEIGHT to height,
        ProfileOptions.KEY_FPS to fps,
        ProfileOptions.KEY_BITRATE_KBPS to bitrateKbps,
        ProfileOptions.KEY_STRETCH to stretchMode,
        ProfileOptions.KEY_OSC_ENABLED to oscEnabled,
        ProfileOptions.KEY_OSC_OPACITY to oscOpacity,
    )
    val now = System.currentTimeMillis()
    return Profile(
        uuid = UUID.randomUUID(),
        name = name,
        createdUtc = now,
        modifiedUtc = now,
        options = options,
        isActive = false,
    )
}
