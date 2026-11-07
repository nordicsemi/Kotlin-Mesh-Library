package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Requests the Light LC Light OnOff state of a Light LC Server.
 *
 * The response received to the message is [LightLCLightOnOffStatus].
 */
class LightLCLightOnOffGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightLCLightOnOffStatus.opCode
    override val parameters = null

    override fun toString() = "LightLCLightOnOffGet"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8299u

        override fun init(parameters: ByteArray?) = if (parameters == null)
            LightLCLightOnOffGet()
        else null
    }
}