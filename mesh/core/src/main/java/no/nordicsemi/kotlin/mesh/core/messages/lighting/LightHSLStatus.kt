package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TransitionStatusMessage
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime

/**
 * Reports the current Light HSL state of a Light HSL Server.
 *
 * This message is sent in response to a [LightHSLGet], [LightHSLSet] or [LightHSLSetUnacknowledged]
 * message, or it may be sent unsolicited when the
 * state changes.
 *
 * @property lightness     Present value of the Light HSL Lightness state.
 * @property hue           Present value of the Light HSL Hue state.
 * @property saturation    Present value of the Light HSL Saturation state.
 * @property remainingTime Time that an element will take to transition to the target state from the
 *                         present state, or null if no transition is in progress.
 */
class LightHSLStatus(
    val lightness: UShort,
    val hue: UShort,
    val saturation: UShort,
    override val remainingTime: TransitionTime?,
) : ApplicationResponse, TransitionStatusMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = when (remainingTime != null) {
        true -> lightness.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            hue.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            saturation.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            remainingTime.rawValue.toByteArray()

        else -> lightness.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            hue.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            saturation.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
    }

    /**
     * Convenience constructor to create a LightHSLStatus message without a remaining time.
     *
     * @param lightness  Present value of the Light HSL Lightness state.
     * @param hue        Present value of the Light HSL Hue state.
     * @param saturation Present value of the Light HSL Saturation state.
     */
    @Suppress("unused")
    constructor(
        lightness: UShort,
        hue: UShort,
        saturation: UShort
    ) : this(
        lightness = lightness,
        hue = hue,
        saturation = saturation,
        remainingTime = null
    )

    override fun toString() = "LightHSLStatus(lightness: $lightness, hue: $hue, " +
            "saturation: $saturation" +
            if (remainingTime != null) ", remainingTime: $remainingTime)" else ")"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8278u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 6 || it.size == 7 }
            ?.let { params ->
                LightHSLStatus(
                    lightness = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                    hue = params.getUShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN),
                    saturation = params.getUShort(offset = 4, order = ByteOrder.LITTLE_ENDIAN),
                    remainingTime = when (params.size) {
                        7 -> TransitionTime(rawValue = params[6].toUByte())
                        else -> null
                    }
                )
            }
    }
}
