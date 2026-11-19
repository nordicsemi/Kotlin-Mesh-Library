package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TransitionStatusMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime

/**
 * Reports the target Light HSL state of a Light HSL Server.
 *
 * This message is sent in response to a [LightHSLTargetGet] message, or it may be sent unsolicited
 * when the
 * state changes.
 *
 * @property targetLightness  Target value of the Light HSL Lightness state.
 * @property targetHue        Target value of the Light HSL Hue state.
 * @property targetSaturation Target value of the Light HSL Saturation state.
 * @property remainingTime    Time that an element will take to transition to the target state from
 *                            the present state, or null if no transition is in progress.
 */
class LightHSLTargetStatus(
    val targetLightness: UShort,
    val targetHue: UShort,
    val targetSaturation: UShort,
    override val remainingTime: TransitionTime?,
) : ApplicationResponse, TransitionStatusMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = when (remainingTime != null) {
        true -> targetLightness.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            targetHue.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            targetSaturation.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            remainingTime.rawValue.toByteArray()

        else -> targetLightness.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            targetHue.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            targetSaturation.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
    }

    /**
     * Convenience constructor to create a LightHSLTargetStatus message without a remaining time.
     *
     * @param targetLightness  Target value of the Light HSL Lightness state.
     * @param targetHue        Target value of the Light HSL Hue state.
     * @param targetSaturation Target value of the Light HSL Saturation state.
     */
    @Suppress("unused")
    constructor(
        targetLightness: UShort,
        targetHue: UShort,
        targetSaturation: UShort
    ) : this(
        targetLightness = targetLightness,
        targetHue = targetHue,
        targetSaturation = targetSaturation,
        remainingTime = null
    )

    override fun toString() = "LightHSLTargetStatus(targetLightness: $targetLightness, " +
            "targetHue: $targetHue, targetSaturation: $targetSaturation" +
            if (remainingTime != null) ", remainingTime: $remainingTime)" else ")"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x827Au

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 6 || it.size == 7 }
            ?.let { params ->
                LightHSLTargetStatus(
                    targetLightness = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                    targetHue = params.getUShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN),
                    targetSaturation = params.getUShort(
                        offset = 4,
                        order = ByteOrder.LITTLE_ENDIAN
                    ),
                    remainingTime = when (params.size) {
                        7 -> TransitionTime(rawValue = params[6].toUByte())
                        else -> null
                    }
                )
            }
    }
}
