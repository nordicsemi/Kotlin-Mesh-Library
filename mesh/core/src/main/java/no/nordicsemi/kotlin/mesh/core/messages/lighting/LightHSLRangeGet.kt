package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Requests the current Light HSL Range state of a Light HSL Server.
 *
 * The response to this message is a [LightHSLRangeStatus].
 */
class LightHSLRangeGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightHSLRangeStatus.opCode
    override val parameters = null

    override fun toString() = "LightHSLRangeGet"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x827Du

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            LightHSLRangeGet()
        else null
    }
}
