package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TransitionStatusMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime

/**
 * Reports the current Light CTL state of a Light CTL Server.
 *
 * This message is sent in response to a [LightCTLGet], [LightCTLSet] or
 * [LightCTLSetUnacknowledged] message, or it may be sent unsolicited when the state changes.
 *
 * @property lightness         Lightness value.
 * @property temperature       Temperature in Kelvin, coerced to 800 to 20000.
 * @property targetLightness   Target lightness, or null if no transition is in progress.
 * @property targetTemperature Target temperature, or null if no transition is in progress.
 * @property remainingTime     Time that an element will take to transition to the target state from
 *                            the present state, or null if no transition is in progress.
 */
class LightCTLStatus(
    val lightness: UShort,
    temperature: UShort,
    val targetLightness: UShort?,
    targetTemperature: UShort?,
    override val remainingTime: TransitionTime?,
) : ApplicationResponse, TransitionStatusMessage {
    val temperature: UShort = temperature.coerceCtlTemperature()
    val targetTemperature: UShort? = targetTemperature?.coerceCtlTemperature()
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = when (this.targetTemperature != null && targetLightness != null && remainingTime != null) {
        true -> lightness.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                this.temperature.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                targetLightness.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                this.targetTemperature.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                remainingTime.rawValue.toByteArray()

        else -> lightness.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                this.temperature.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
    }

    /**
     * Convenience constructor to create a LightCTLStatus message without a target state.
     */
    @Suppress("unused")
    constructor(lightness: UShort, temperature: UShort) : this(
        lightness = lightness,
        temperature = temperature,
        targetLightness = null,
        targetTemperature = null,
        remainingTime = null
    )

    override fun toString() = "LightCTLStatus(lightness: $lightness, temperature: $temperature" +
            if (targetLightness != null && targetTemperature != null && remainingTime != null) {
                ", targetLightness: $targetLightness, targetTemperature: $targetTemperature, remainingTime: $remainingTime)"
            } else ")"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8260u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 4 || it.size == 9 }
            ?.let { params ->
                LightCTLStatus(
                    lightness = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                    temperature = params.getUShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN),
                    targetLightness = if (params.size == 9) params.getUShort(offset = 4, order = ByteOrder.LITTLE_ENDIAN) else null,
                    targetTemperature = if (params.size == 9) params.getUShort(offset = 6, order = ByteOrder.LITTLE_ENDIAN) else null,
                    remainingTime = if (params.size == 9) {
                        TransitionTime(rawValue = params[8].toUByte())
                    } else null
                )
            }
    }
}
