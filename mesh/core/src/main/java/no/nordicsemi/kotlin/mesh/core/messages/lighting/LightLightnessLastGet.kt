package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Requests the current Light Lightness Last state of a Light Lightness Server.
 *
 * The response to this message is a [LightLightnessLastStatus].
 */
class LightLightnessLastGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightLightnessLastStatus.opCode
    override val parameters = null

    override fun toString() = "LightLightnessLastGet"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8253u

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            LightLightnessLastGet()
        else null
    }
}
