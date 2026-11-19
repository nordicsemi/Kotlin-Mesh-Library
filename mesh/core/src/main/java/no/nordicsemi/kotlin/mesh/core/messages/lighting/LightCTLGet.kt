package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Requests the current Light CTL state of a Light CTL Server.
 *
 * The response to this message is a [LightCTLStatus].
 */
class LightCTLGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightCTLStatus.opCode
    override val parameters = null

    override fun toString() = "LightCTLGet"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x825Du

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            LightCTLGet()
        else null
    }
}
