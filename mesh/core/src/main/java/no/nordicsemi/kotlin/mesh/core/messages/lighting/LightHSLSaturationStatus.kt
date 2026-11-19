package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TransitionStatusMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime

/**
 * Reports the current Light HSL Saturation state of a Light HSL Server.
 *
 * This message is sent in response to a [LightHSLSaturationGet], [LightHSLSaturationSet] or
 * [LightHSLSaturationSetUnacknowledged] message, or it may be sent unsolicited when the
 * state changes.
 *
 * @property saturation       Present value of the Light HSL Saturation state.
 * @property targetSaturation Target value of the Light HSL Saturation state, or null if no
 *                            transition is in progress.
 * @property remainingTime    Time that an element will take to transition to the target state from
 *                            the present state, or null if no transition is in progress.
 */
class LightHSLSaturationStatus(
    val saturation: UShort,
    val targetSaturation: UShort?,
    override val remainingTime: TransitionTime?,
) : ApplicationResponse, TransitionStatusMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = when (targetSaturation != null && remainingTime != null) {
        true -> saturation.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                targetSaturation.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                remainingTime.rawValue.toByteArray()

        else -> saturation.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
    }

    /**
     * Convenience constructor to create a LightHSLSaturationStatus message without a target state.
     *
     * @param saturation Present value of the Light HSL Saturation state.
     */
    @Suppress("unused")
    constructor(
        saturation: UShort
    ) : this(
        saturation = saturation,
        targetSaturation = null,
        remainingTime = null
    )

    override fun toString() = "LightHSLSaturationStatus(saturation: $saturation" +
            if (targetSaturation != null && remainingTime != null) {
                ", targetSaturation: $targetSaturation, remainingTime: $remainingTime)"
            } else ")"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8275u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 || it.size == 5 }
            ?.let { params ->
                LightHSLSaturationStatus(
                    saturation = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                    targetSaturation = when (params.size) {
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
