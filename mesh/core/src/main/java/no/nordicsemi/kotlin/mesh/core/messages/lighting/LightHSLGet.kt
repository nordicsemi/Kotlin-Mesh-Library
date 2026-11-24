package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Requests the current Light HSL state of a Light HSL Server.
 *
 * The response to this message is a [LightHSLStatus].
 */
class LightHSLGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightHSLStatus.opCode
    override val parameters = null

    override fun toString() = "LightHSLGet"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x826Du

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            LightHSLGet()
        else null
    }
}
