package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage

/**
 * This message is a response to a [LightLCOccupancyModeGet], [LightLCOccupancyModeSet] and
 * [LightLCOccupancyModeSetUnacknowledged]
 * and contains the present value of the Light LC Occupancy Mode state.
 *
 * @property occupancyMode Whether the controller may transition from a standby state when occupancy
 *                         is reported.
 */
class LightLCOccupancyModeStatus(
    val occupancyMode: Boolean,
) : ApplicationResponse, UnacknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val parameters = byteArrayOf(if (occupancyMode) 0x01 else 0x00)

    override fun toString() = "LightLCOccupancyModeStatus(occupancyMode: $occupancyMode)"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8298u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 }
            ?.let { LightLCOccupancyModeStatus(occupancyMode = it[0] == 0x01.toByte()) }
    }
}
