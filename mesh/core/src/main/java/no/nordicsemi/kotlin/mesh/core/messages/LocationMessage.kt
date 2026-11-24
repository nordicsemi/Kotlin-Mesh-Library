package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getInt
import no.nordicsemi.kotlin.data.getShort
import no.nordicsemi.kotlin.data.toByteArray
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

/**
 * Base interface for global location status messages.
 *
 * @property latitude  Global latitude.
 * @property longitude Global longitude.
 * @property altitude  Global altitude.
 */
interface LocationStatusMessage : MeshMessage {
    val latitude: Latitude
    val longitude: Longitude
    val altitude: Altitude
}

/**
 * Encodes the global location fields as 10 bytes: latitude (4), longitude (4) and altitude (2),
 * all little endian.
 */
internal fun encodeGlobalLocation(
    latitude: Latitude,
    longitude: Longitude,
    altitude: Altitude,
): ByteArray = latitude.raw.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
        longitude.raw.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
        altitude.raw.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

private const val MAX_RAW_COORDINATE = Int.MAX_VALUE.toDouble() // 2^31 - 1

private fun coordinateToRaw(degrees: Double, range: Double): Int = max(
    a = Int.MIN_VALUE + 1L,
    b = min(
        a = Int.MAX_VALUE.toLong(),
        b = floor(degrees / range * MAX_RAW_COORDINATE).toLong()
    )
).toInt()

/**
 * Global latitude, in the range -90 to 90 degrees, as defined in the Generic Location state.
 *
 * @property raw Raw 32-bit value. [NOT_CONFIGURED] (0x80000000) means the latitude is not
 *               configured.
 */
@JvmInline
value class Latitude(val raw: Int) {

    /** Returns true if the latitude is configured. */
    val isConfigured: Boolean get() = raw != NOT_CONFIGURED

    /** Latitude in degrees (WGS84) or null if not configured. */
    val degrees: Double?
        get() = if (isConfigured) raw / MAX_RAW_COORDINATE * 90.0 else null

    override fun toString() = degrees?.let { "Latitude($it)" } ?: "Latitude(not configured)"

    companion object {
        /** Raw value indicating that the latitude is not configured. */
        const val NOT_CONFIGURED = Int.MIN_VALUE

        /** Latitude that is not configured. */
        val notConfigured = Latitude(NOT_CONFIGURED)

        /**
         * Creates a [Latitude] from degrees.
         *
         * @param degrees WGS84 latitude between -90 and 90.
         * @return Latitude or null if the value is out of range.
         */
        fun fromDegrees(degrees: Double): Latitude? =
            if (degrees in -90.0..90.0) Latitude(coordinateToRaw(degrees, 90.0)) else null
    }
}

/**
 * Global longitude, in the range -180 to 180 degrees, as defined in the Generic Location state.
 *
 * @property raw Raw 32-bit value. [NOT_CONFIGURED] (0x80000000) means the longitude is not
 *               configured.
 */
@JvmInline
value class Longitude(val raw: Int) {

    /** Returns true if the longitude is configured. */
    val isConfigured: Boolean get() = raw != NOT_CONFIGURED

    /** Longitude in degrees (WGS84) or null if not configured. */
    val degrees: Double?
        get() = if (isConfigured) raw / MAX_RAW_COORDINATE * 180.0 else null

    override fun toString() = degrees?.let { "Longitude($it)" } ?: "Longitude(not configured)"

    companion object {
        /** Raw value indicating that the longitude is not configured. */
        const val NOT_CONFIGURED = Int.MIN_VALUE

        /** Longitude that is not configured. */
        val notConfigured = Longitude(NOT_CONFIGURED)

        /**
         * Creates a [Longitude] from degrees.
         *
         * @param degrees WGS84 longitude between -180 and 180.
         * @return Longitude or null if the value is out of range.
         */
        fun fromDegrees(degrees: Double): Longitude? =
            if (degrees in -180.0..180.0) Longitude(coordinateToRaw(degrees, 180.0)) else null
    }
}

/**
 * Global altitude in meters relative to the WGS84 datum.
 *
 * @property raw Raw 16-bit value. [NOT_CONFIGURED] (0x7FFF) means the altitude is not configured
 *               and [TOO_LARGE] (0x7FFE) means the altitude is greater than or equal to 32766 m.
 */
@JvmInline
value class Altitude(val raw: Short) {

    /** Returns true if the altitude is configured and fits the 16-bit range. */
    val isConfigured: Boolean get() = raw != NOT_CONFIGURED && raw != TOO_LARGE

    /** Returns true if the altitude is 32766 m or more. */
    val isTooLarge: Boolean get() = raw == TOO_LARGE

    /** Altitude in meters or null if not configured or too large. */
    val meters: Short? get() = if (isConfigured) raw else null

    override fun toString() = when (raw) {
        NOT_CONFIGURED -> "Altitude(not configured)"
        TOO_LARGE -> "Altitude(too large)"
        else -> "Altitude($raw m)"
    }

    companion object {
        /** Raw value indicating that the altitude is not configured. */
        const val NOT_CONFIGURED: Short = 0x7FFF

        /** Raw value indicating that the altitude is greater than or equal to 32766 m. */
        const val TOO_LARGE: Short = 0x7FFE

        /** Altitude that is not configured. */
        val notConfigured = Altitude(NOT_CONFIGURED)

        /** Altitude that is too large to be represented. */
        val tooLarge = Altitude(TOO_LARGE)
    }
}

internal fun ByteArray.getLatitude(offset: Int) =
    Latitude(getInt(offset = offset, order = ByteOrder.LITTLE_ENDIAN))

internal fun ByteArray.getLongitude(offset: Int) =
    Longitude(getInt(offset = offset, order = ByteOrder.LITTLE_ENDIAN))

internal fun ByteArray.getAltitude(offset: Int) =
    Altitude(getShort(offset = offset, order = ByteOrder.LITTLE_ENDIAN))
