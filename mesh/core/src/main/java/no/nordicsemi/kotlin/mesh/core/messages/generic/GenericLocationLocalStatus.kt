package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.LocalLocationStatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage
import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getShort
import no.nordicsemi.kotlin.mesh.core.messages.FloorNumber
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.LocationUncertainty
import no.nordicsemi.kotlin.mesh.core.messages.encodeLocalLocation
import no.nordicsemi.kotlin.mesh.core.messages.getFloorNumber
import no.nordicsemi.kotlin.mesh.core.messages.getLocationUncertainty

/**
 * This message is a response to a [GenericLocationLocalGet] and [GenericLocationLocalSet] that
 * contains the local location of a GenericLocationServer.
 *
 * @property north       Local north position in decimeters.
 * @property east        Local east position in decimeters.
 * @property altitude    Local altitude in decimeters.
 * @property floor       Local floor number.
 * @property uncertainty Uncertainty of the local location.
 */
class GenericLocationLocalStatus(
    override val north: Short,
    override val east: Short,
    override val altitude: Short,
    override val floor: FloorNumber,
    override val uncertainty: LocationUncertainty,
) : ApplicationResponse, UnacknowledgedApplicationMessage, LocalLocationStatusMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() = encodeLocalLocation(north, east, altitude, floor, uncertainty)

    override fun toString() = "GenericLocationLocalStatus(north: $north, east: $east, " +
            "altitude: $altitude, floor: $floor, uncertainty: $uncertainty)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8227u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 9 }
            ?.let { params ->
                GenericLocationLocalStatus(
                    north = params.getShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                    east = params.getShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN),
                    altitude = params.getShort(offset = 4, order = ByteOrder.LITTLE_ENDIAN),
                    floor = params.getFloorNumber(offset = 6),
                    uncertainty = params.getLocationUncertainty(offset = 7)
                )
            }
    }
}
