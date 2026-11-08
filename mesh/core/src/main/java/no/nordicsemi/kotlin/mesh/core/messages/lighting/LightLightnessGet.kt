package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Requests the current Lightness state of a Light Lightness Server.
 *
 * The response to this message is a [LightLightnessStatus].
 */
class LightLightnessGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightLightnessStatus.opCode
    override val parameters = null

    override fun toString() = "LightLightnessGet"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x824Bu

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            LightLightnessGet()
        else null
    }
}
