package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Sets the Light Lightness Default state of a Light Lightness Setup Server.
 *
 * The response to this message is a [LightLightnessDefaultStatus].
 *
 * @property lightness Value of the Light Lightness Default state.
 */
class LightLightnessDefaultSet(val lightness: UShort) : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightLightnessDefaultStatus.opCode
    override val parameters: ByteArray = lightness.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    override fun toString() = "LightLightnessDefaultSet(lightness: $lightness)"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8259u

        override fun init(parameters: ByteArray?) = parameters?.takeIf { it.size == 2 }?.let {
            LightLightnessDefaultSet(lightness = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN))
        }
    }
}
