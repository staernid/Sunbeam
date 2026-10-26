package io.github.staernid.sunbeam.core.stream.model

data class ControllerInput(
    val controllerNumber: Short,
    val activeGamepadMask: Short,
    val buttonFlags: Int = 0,
    val leftTrigger: Byte,
    val rightTrigger: Byte,
    val leftStickX: Short,
    val leftStickY: Short,
    val rightStickX: Short,
    val rightStickY: Short,
)
