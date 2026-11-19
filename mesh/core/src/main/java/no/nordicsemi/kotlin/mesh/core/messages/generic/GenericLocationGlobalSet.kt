package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.Altitude
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.Latitude
import no.nordicsemi.kotlin.mesh.core.messages.Longitude
import no.nordicsemi.kotlin.mesh.core.messages.encodeGlobalLocation
import no.nordicsemi.kotlin.mesh.core.messages.getAltitude
import no.nordicsemi.kotlin.mesh.core.messages.getLatitude
import no.nordicsemi.kotlin.mesh.core.messages.getLongitude

/**
 * This message is used to set the global location of a GenericLocationServer model. Response
 * received to the message is [GenericLocationGlobalStatus].
 *
 * @property latitude  Global latitude.
 * @property longitude Global longitude.
 * @property altitude  Global altitude.
 */
class GenericLocationGlobalSet(
    val latitude: Latitude,
    val longitude: Longitude,
    val altitude: Altitude,
) : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericLocationGlobalStatus.opCode
    override val parameters: ByteArray
        get() = encodeGlobalLocation(latitude, longitude, altitude)

    override fun toString() = "GenericLocationGlobalSet(latitude: $latitude, " +
            "longitude: $longitude, altitude: $altitude)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x41u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 10 }
            ?.let { params ->
                GenericLocationGlobalSet(
                    latitude = params.getLatitude(offset = 0),
                    longitude = params.getLongitude(offset = 4),
                    altitude = params.getAltitude(offset = 8)
                )
            }
    }
}
