package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TransitionStatusMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime

/**
 * Reports the current Light HSL Hue state of a Light HSL Server.
 *
 * This message is sent in response to a [LightHSLHueGet], [LightHSLHueSet] or
 * [LightHSLHueSetUnacknowledged] message, or it may be sent unsolicited when the
 * state changes.
 *
 * @property hue           Present value of the Light HSL Hue state.
 * @property targetHue     Target value of the Light HSL Hue state, or null if no transition is in
 *                         progress.
 * @property remainingTime Time that an element will take to transition to the target state from the
 *                         present state, or null if no transition is in progress.
 */
class LightHSLHueStatus(
    val hue: UShort,
    val targetHue: UShort?,
    override val remainingTime: TransitionTime?,
) : ApplicationResponse, TransitionStatusMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = when (targetHue != null && remainingTime != null) {
        true -> hue.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                targetHue.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                remainingTime.rawValue.toByteArray()

        else -> hue.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
    }

    /**
     * Convenience constructor to create a LightHSLHueStatus message without a target state.
     *
     * @param hue Present value of the Light HSL Hue state.
     */
    @Suppress("unused")
    constructor(hue: UShort) : this(hue = hue, targetHue = null, remainingTime = null)

    override fun toString() = "LightHSLHueStatus(hue: $hue" +
            if (targetHue != null && remainingTime != null) {
                ", targetHue: $targetHue, remainingTime: $remainingTime)"
            } else ")"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8271u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 || it.size == 5 }
            ?.let { params ->
                LightHSLHueStatus(
                    hue = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                    targetHue = when (params.size) {
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
