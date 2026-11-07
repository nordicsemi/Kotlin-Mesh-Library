package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * This message is used to get the Light LC Occupancy Mode state of a Light LC Server. The response
 * received to
 * received to the message is [LightLCOccupancyModeStatus].
 */
class LightLCOccupancyModeGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightLCOccupancyModeStatus.opCode
    override val parameters = null

    override fun toString() = "LightLCOccupancyModeGet"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8295u

        override fun init(parameters: ByteArray?) = if (parameters == null)
            LightLCOccupancyModeGet()
        else null
    }
}
