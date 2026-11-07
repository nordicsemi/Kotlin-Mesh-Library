package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * This message is used to set the Light LC Occupancy Mode state of a Light LC Server. The response received to
 * the message is [LightLCOccupancyModeStatus].
 *
 * @property occupancyMode Whether the controller may transition from a standby state when occupancy is reported.
 */
class LightLCOccupancyModeSet(
    val occupancyMode: Boolean,
) : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = LightLCOccupancyModeStatus.opCode
    override val parameters = byteArrayOf(if (occupancyMode) 0x01 else 0x00)

    override fun toString() = "LightLCOccupancyModeSet(occupancyMode: $occupancyMode)"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8296u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 }
            ?.let { LightLCOccupancyModeSet(occupancyMode = it[0] == 0x01.toByte()) }
    }
}
