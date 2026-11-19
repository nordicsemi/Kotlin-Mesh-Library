package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getShort
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TransitionStatusMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime

/**
 * Reports the current Light CTL Temperature and Delta UV state of a Light CTL Temperature Server.
 *
 * This message is sent in response to a [LightCTLTemperatureGet], [LightCTLTemperatureSet] or
 * [LightCTLTemperatureSetUnacknowledged] message, or it may be sent unsolicited when the state
 * changes.
 *
 * @property temperature       Temperature in Kelvin, coerced to 800 to 20000.
 * @property deltaUV           Delta UV value (signed).
 * @property targetTemperature Target temperature, or null if no transition is in progress.
 * @property targetDeltaUV     Target Delta UV, or null if no transition is in progress.
 * @property remainingTime     Time that an element will take to transition to the target state from
 *                            the present state, or null if no transition is in progress.
 */
class LightCTLTemperatureStatus(
    temperature: UShort,
    val deltaUV: Short,
    targetTemperature: UShort?,
    val targetDeltaUV: Short?,
    override val remainingTime: TransitionTime?,
) : ApplicationResponse, TransitionStatusMessage {
    val temperature: UShort = temperature.coerceCtlTemperature()
    val targetTemperature: UShort? = targetTemperature?.coerceCtlTemperature()
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = when (this.targetTemperature != null && targetDeltaUV != null && remainingTime != null) {
        true -> this.temperature.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                deltaUV.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                this.targetTemperature.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                targetDeltaUV.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                remainingTime.rawValue.toByteArray()

        else -> this.temperature.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                deltaUV.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
    }

    /**
     * Convenience constructor to create a LightCTLTemperatureStatus message without a target state.
     */
    @Suppress("unused")
    constructor(temperature: UShort, deltaUV: Short) : this(
        temperature = temperature,
        deltaUV = deltaUV,
        targetTemperature = null,
        targetDeltaUV = null,
        remainingTime = null
    )

    override fun toString() = "LightCTLTemperatureStatus(temperature: $temperature, deltaUV: $deltaUV" +
            if (targetTemperature != null && targetDeltaUV != null && remainingTime != null) {
                ", targetTemperature: $targetTemperature, targetDeltaUV: $targetDeltaUV, remainingTime: $remainingTime)"
            } else ")"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8266u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 4 || it.size == 9 }
            ?.let { params ->
                LightCTLTemperatureStatus(
                    temperature = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                    deltaUV = params.getShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN),
                    targetTemperature = if (params.size == 9) params.getUShort(offset = 4, order = ByteOrder.LITTLE_ENDIAN) else null,
                    targetDeltaUV = if (params.size == 9) params.getShort(offset = 6, order = ByteOrder.LITTLE_ENDIAN) else null,
                    remainingTime = if (params.size == 9) {
                        TransitionTime(rawValue = params[8].toUByte())
                    } else null
                )
            }
    }
}
