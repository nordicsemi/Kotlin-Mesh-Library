package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Sets the Light Lightness Default state of a Light Lightness Setup Server without requesting a response.
 *
 * This is an unacknowledged message, so no response is expected.
 *
 * @property lightness Value of the Light Lightness Default state.
 */
class LightLightnessDefaultSetUnacknowledged(val lightness: UShort) : UnacknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = lightness.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    override fun toString() = "LightLightnessDefaultSetUnacknowledged(lightness: $lightness)"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x825Au

        override fun init(parameters: ByteArray?) = parameters?.takeIf { it.size == 2 }?.let {
            LightLightnessDefaultSetUnacknowledged(lightness = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN))
        }
    }
}
