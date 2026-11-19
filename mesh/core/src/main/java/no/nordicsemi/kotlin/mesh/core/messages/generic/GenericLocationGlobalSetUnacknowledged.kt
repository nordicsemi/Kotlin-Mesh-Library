package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.Altitude
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.Latitude
import no.nordicsemi.kotlin.mesh.core.messages.Longitude
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.encodeGlobalLocation
import no.nordicsemi.kotlin.mesh.core.messages.getAltitude
import no.nordicsemi.kotlin.mesh.core.messages.getLatitude
import no.nordicsemi.kotlin.mesh.core.messages.getLongitude

/**
 * This is an unacknowledged message used to set the global location of a GenericLocationServer
 * model.
 *
 * @property latitude  Global latitude.
 * @property longitude Global longitude.
 * @property altitude  Global altitude.
 */
class GenericLocationGlobalSetUnacknowledged(
    val latitude: Latitude,
    val longitude: Longitude,
    val altitude: Altitude,
) : UnacknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() = encodeGlobalLocation(latitude, longitude, altitude)

    override fun toString() = "GenericLocationGlobalSetUnacknowledged(latitude: $latitude, " +
            "longitude: $longitude, altitude: $altitude)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x42u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 10 }
            ?.let { params ->
                GenericLocationGlobalSetUnacknowledged(
                    latitude = params.getLatitude(offset = 0),
                    longitude = params.getLongitude(offset = 4),
                    altitude = params.getAltitude(offset = 8)
                )
            }
    }
}
