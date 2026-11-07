package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * This message is used to get the Light LC Mode state of a Light LC Server. The response
 * received to the message is [LightLCModeStatus].
 */
class LightLCModeGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightLCModeStatus.opCode
    override val parameters = null

    override fun toString() = "LightLCModeGet"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8291u

        override fun init(parameters: ByteArray?) = if (parameters == null)
            LightLCModeGet()
        else null
    }
}
