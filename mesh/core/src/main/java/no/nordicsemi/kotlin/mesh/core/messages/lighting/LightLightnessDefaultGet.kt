package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Requests the current Light Lightness Default state of a Light Lightness Server.
 *
 * The response to this message is a [LightLightnessDefaultStatus].
 */
class LightLightnessDefaultGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightLightnessDefaultStatus.opCode
    override val parameters = null

    override fun toString() = "LightLightnessDefaultGet"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8255u

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            LightLightnessDefaultGet()
        else null
    }
}
