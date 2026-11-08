package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Reports the current Light Lightness Default state of a Light Lightness Server.
 *
 * This message is sent in response to a [LightLightnessDefaultGet], [LightLightnessDefaultSet] or [LightLightnessDefaultSetUnacknowledged]
 * message.
 *
 * @property lightness Value of the Light Lightness Default state.
 */
class LightLightnessDefaultStatus(val lightness: UShort) : ApplicationResponse {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = lightness.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    override fun toString() = "LightLightnessDefaultStatus(lightness: $lightness)"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8256u

        override fun init(parameters: ByteArray?) = parameters?.takeIf { it.size == 2 }?.let {
            LightLightnessDefaultStatus(lightness = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN))
        }
    }
}
