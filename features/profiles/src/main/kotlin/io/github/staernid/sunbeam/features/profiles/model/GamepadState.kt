package io.github.staernid.sunbeam.features.profiles.model

/**
 * Information regarding a connected input controller.
 */
data class GamepadDeviceInfo(
    val id: Int,
    val name: String,
    val vendorId: Int = 0,
    val productId: Int = 0,
    val hasVibrator: Boolean = false,
    val hasSensors: Boolean = false,
)

/**
 * Standard gamepad buttons recognized by Moonlight / Sunshine clients.
 */
enum class GamepadButton {
    A,
    B,
    X,
    Y,
    DPAD_UP,
    DPAD_DOWN,
    DPAD_LEFT,
    DPAD_RIGHT,
    LB,
    RB,
    LSB,
    RSB,
    START,
    BACK,
    GUIDE,
}

/**
 * State representing live analog stick, trigger, button, and sensor values.
 */
data class GamepadDebugState(
    val devices: List<GamepadDeviceInfo> = emptyList(),
    val selectedDeviceId: Int? = null,
    val leftStickX: Float = 0f,
    val leftStickY: Float = 0f,
    val rightStickX: Float = 0f,
    val rightStickY: Float = 0f,
    val leftTrigger: Float = 0f,
    val rightTrigger: Float = 0f,
    val pressedButtons: Set<GamepadButton> = emptySet(),
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val rumbleStrength: Float = 0.8f,
    val isRumbling: Boolean = false,
)
