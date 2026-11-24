package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Reports the Light HSL Default state of a Light HSL Server.
 *
 * This message is sent in response to a [LightHSLDefaultGet], [LightHSLDefaultSet] or
 * [LightHSLDefaultSetUnacknowledged] message.
 *
 * @property lightness  Value of the Light HSL Lightness Default state.
 * @property hue        Value of the Light HSL Hue Default state.
 * @property saturation Value of the Light HSL Saturation Default state.
 */
class LightHSLDefaultStatus(
    val lightness: UShort,
    val hue: UShort,
    val saturation: UShort,
) : ApplicationResponse {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = lightness.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            hue.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            saturation.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    override fun toString() = "LightHSLDefaultStatus(lightness: $lightness, hue: $hue, " +
            "saturation: $saturation)"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x827Cu

        override fun init(parameters: ByteArray?) = parameters?.takeIf { it.size == 6 }?.let {
            LightHSLDefaultStatus(
                lightness = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                hue = it.getUShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN),
                saturation = it.getUShort(offset = 4, order = ByteOrder.LITTLE_ENDIAN)
            )
        }
    }
}
