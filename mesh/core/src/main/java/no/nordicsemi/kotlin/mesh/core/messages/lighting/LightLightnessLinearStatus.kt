package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TransitionStatusMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime

/**
 * Reports the current linear lightness state of a Light Lightness Server.
 *
 * This message is sent in response to a [LightLightnessLinearGet], [LightLightnessLinearSet] or [LightLightnessLinearSetUnacknowledged]
 * message, or it may be sent unsolicited when the state changes.
 *
 * @property lightness       Present value of the Light Lightness Linear state.
 * @property targetLightness Target value of the Light Lightness Linear state, or null if no transition is in
 *                           progress.
 * @property remainingTime   Time that an element will take to transition to the target state from
 *                           the present state, or null if no transition is in progress.
 */
class LightLightnessLinearStatus(
    val lightness: UShort,
    val targetLightness: UShort?,
    override val remainingTime: TransitionTime?,
) : ApplicationResponse, TransitionStatusMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = when (targetLightness != null && remainingTime != null) {
        true -> lightness.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                targetLightness.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                remainingTime.rawValue.toByteArray()

        else -> lightness.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
    }

    /**
     * Convenience constructor to create a LightLightnessLinearStatus message without a target state.
     *
     * @param lightness Present value of the Light Lightness Linear state.
     */
    @Suppress("unused")
    constructor(lightness: UShort) : this(
        lightness = lightness,
        targetLightness = null,
        remainingTime = null
    )

    override fun toString() = "LightLightnessLinearStatus(lightness: $lightness" +
            if (targetLightness != null && remainingTime != null) {
                ", targetLightness: $targetLightness, remainingTime: $remainingTime)"
            } else ")"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8252u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 || it.size == 5 }
            ?.let { params ->
                LightLightnessLinearStatus(
                    lightness = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                    targetLightness = when (params.size) {
                        5 -> params.getUShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN)
                        else -> null
                    },
                    remainingTime = when (params.size) {
                        5 -> TransitionTime(rawValue = params[4].toUByte())
                        else -> null
                    }
                )
            }
    }
}
