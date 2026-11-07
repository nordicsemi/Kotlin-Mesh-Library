package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Requests the current Light Lightness Range state of a Light Lightness Server.
 *
 * The response to this message is a [LightLightnessRangeStatus].
 */
class LightLightnessRangeGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightLightnessRangeStatus.opCode
    override val parameters = null

    override fun toString() = "LightLightnessRangeGet"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8257u

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            LightLightnessRangeGet()
        else null
    }
}
