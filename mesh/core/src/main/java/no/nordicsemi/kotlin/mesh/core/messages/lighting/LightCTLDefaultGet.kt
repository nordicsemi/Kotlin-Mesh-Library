package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Requests the Light CTL Default state of a Light CTL Server.
 *
 * The response to this message is a [LightCTLDefaultStatus].
 */
class LightCTLDefaultGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightCTLDefaultStatus.opCode
    override val parameters = null

    override fun toString() = "LightCTLDefaultGet"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8267u

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            LightCTLDefaultGet()
        else null
    }
}
