package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Requests the current target Light HSL state of a Light HSL Server.
 *
 * The response to this message is a [LightHSLTargetStatus].
 */
class LightHSLTargetGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightHSLTargetStatus.opCode
    override val parameters = null

    override fun toString() = "LightHSLTargetGet"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8279u

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            LightHSLTargetGet()
        else null
    }
}
