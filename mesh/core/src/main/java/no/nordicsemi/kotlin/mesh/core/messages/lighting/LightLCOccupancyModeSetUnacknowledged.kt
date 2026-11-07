package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage

/**
 * This message is used to set the Light LC Occupancy Mode state of a Light LC Server without
 * expecting a
 * response.
 *
 * @property occupancyMode Whether the controller may transition from a standby state when occupancy
 *                         is reported.
 */
class LightLCOccupancyModeSetUnacknowledged(
    val occupancyMode: Boolean,
) : UnacknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val parameters = byteArrayOf(if (occupancyMode) 0x01 else 0x00)

    override fun toString() = "LightLCOccupancyModeSetUnacknowledged(occupancyMode: $occupancyMode)"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8297u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 }
            ?.let { LightLCOccupancyModeSetUnacknowledged(occupancyMode = it[0] == 0x01.toByte()) }
    }
}
