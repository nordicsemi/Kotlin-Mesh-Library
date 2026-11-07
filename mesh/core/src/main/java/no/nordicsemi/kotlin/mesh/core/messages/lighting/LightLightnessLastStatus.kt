package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Reports the current Light Lightness Last state of a Light Lightness Server.
 *
 * This message is sent in response to a [LightLightnessLastGet] message.
 *
 * @property lightness Last non-zero value of the Light Lightness state.
 */
class LightLightnessLastStatus(val lightness: UShort) : ApplicationResponse {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray = lightness.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    override fun toString() = "LightLightnessLastStatus(lightness: $lightness)"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8254u

        override fun init(parameters: ByteArray?) = parameters?.takeIf { it.size == 2 }?.let {
            LightLightnessLastStatus(lightness = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN))
        }
    }
}
