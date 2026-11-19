package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.Altitude
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.Latitude
import no.nordicsemi.kotlin.mesh.core.messages.LocationStatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.Longitude
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.encodeGlobalLocation
import no.nordicsemi.kotlin.mesh.core.messages.getAltitude
import no.nordicsemi.kotlin.mesh.core.messages.getLatitude
import no.nordicsemi.kotlin.mesh.core.messages.getLongitude

/**
 * This message is a response to a [GenericLocationGlobalGet] and [GenericLocationGlobalSet] that
 * contains the global location of a GenericLocationServer.
 *
 * @property latitude  Global latitude.
 * @property longitude Global longitude.
 * @property altitude  Global altitude.
 */
class GenericLocationGlobalStatus(
    override val latitude: Latitude,
    override val longitude: Longitude,
    override val altitude: Altitude,
) : ApplicationResponse, UnacknowledgedApplicationMessage, LocationStatusMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() = encodeGlobalLocation(latitude, longitude, altitude)

    override fun toString() = "GenericLocationGlobalStatus(latitude: $latitude, " +
            "longitude: $longitude, altitude: $altitude)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x40u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 10 }
            ?.let { params ->
                GenericLocationGlobalStatus(
                    latitude = params.getLatitude(offset = 0),
                    longitude = params.getLongitude(offset = 4),
                    altitude = params.getAltitude(offset = 8)
                )
            }
    }
}
