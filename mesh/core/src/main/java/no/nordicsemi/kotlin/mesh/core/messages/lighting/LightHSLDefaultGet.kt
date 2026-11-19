package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Requests the current Light HSL Default state of a Light HSL Server.
 *
 * The response to this message is a [LightHSLDefaultStatus].
 */
class LightHSLDefaultGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightHSLDefaultStatus.opCode
    override val parameters = null

    override fun toString() = "LightHSLDefaultGet"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x827Bu

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            LightHSLDefaultGet()
        else null
    }
}
