package no.nordicsemi.kotlin.mesh.core.messages.lighting

/**
 * Minimum temperature of the Light CTL Temperature state in Kelvin.
 */
internal const val MIN_CTL_TEMPERATURE: UShort = 0x0320u

/**
 * Maximum temperature of the Light CTL Temperature state in Kelvin.
 */
internal const val MAX_CTL_TEMPERATURE: UShort = 0x4E20u

/**
 * Coerces a temperature value into the valid range of 800 K to 20000 K.
 */
internal fun UShort.coerceCtlTemperature(): UShort =
    coerceIn(MIN_CTL_TEMPERATURE, MAX_CTL_TEMPERATURE)
