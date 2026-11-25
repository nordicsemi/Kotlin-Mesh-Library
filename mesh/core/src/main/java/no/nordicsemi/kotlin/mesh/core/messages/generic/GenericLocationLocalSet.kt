package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getShort
import no.nordicsemi.kotlin.mesh.core.messages.FloorNumber
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.LocationUncertainty
import no.nordicsemi.kotlin.mesh.core.messages.encodeLocalLocation
import no.nordicsemi.kotlin.mesh.core.messages.getFloorNumber
import no.nordicsemi.kotlin.mesh.core.messages.getLocationUncertainty

/**
 * This message is used to set the local location of a GenericLocationServer model. Response
 * received to the message is [GenericLocationLocalStatus].
 *
 * @property north       Local north position in decimeters.
 * @property east        Local east position in decimeters.
 * @property altitude    Local altitude in decimeters.
 * @property floor       Local floor number.
 * @property uncertainty Uncertainty of the local location.
 */
class GenericLocationLocalSet(
    val north: Short,
    val east: Short,
    val altitude: Short,
    val floor: FloorNumber,
    val uncertainty: LocationUncertainty,
) : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericLocationLocalStatus.opCode
    override val parameters: ByteArray
        get() = encodeLocalLocation(north, east, altitude, floor, uncertainty)

    override fun toString() = "GenericLocationLocalSet(north: $north, east: $east, " +
            "altitude: $altitude, floor: $floor, uncertainty: $uncertainty)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8228u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 9 }
            ?.let { params ->
                GenericLocationLocalSet(
                    north = params.getShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                    east = params.getShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN),
                    altitude = params.getShort(offset = 4, order = ByteOrder.LITTLE_ENDIAN),
                    floor = params.getFloorNumber(offset = 6),
                    uncertainty = params.getLocationUncertainty(offset = 7)
                )
            }
    }
}
