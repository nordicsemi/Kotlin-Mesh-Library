package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Requests the current Lightness Linear state of a Light Lightness Server.
 *
 * The response to this message is a [LightLightnessLinearStatus].
 */
class LightLightnessLinearGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightLightnessLinearStatus.opCode
    override val parameters = null

    override fun toString() = "LightLightnessLinearGet"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x824Fu

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            LightLightnessLinearGet()
        else null
    }
}
