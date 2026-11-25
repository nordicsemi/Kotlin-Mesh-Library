package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getInt
import no.nordicsemi.kotlin.data.getShort
import no.nordicsemi.kotlin.data.getUShort
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

/**
 * Base interface for local location status messages.
 *
 * @property north       Local north position in decimeters. [LOCAL_NOT_CONFIGURED] (0x8000) means
 *                       the position is not configured.
 * @property east        Local east position in decimeters. [LOCAL_NOT_CONFIGURED] (0x8000) means
 *                       the position is not configured.
 * @property altitude    Local altitude in decimeters. [Altitude.NOT_CONFIGURED] (0x7FFF) means the
 *                       altitude is not configured and [Altitude.TOO_LARGE] (0x7FFE) means the
 *                       altitude is too large to be represented.
 * @property floor       Local floor number.
 * @property uncertainty Uncertainty of the local location.
 */
interface LocalLocationStatusMessage : MeshMessage {
    val north: Short
    val east: Short
    val altitude: Short
    val floor: FloorNumber
    val uncertainty: LocationUncertainty

    companion object {
        /** Raw value indicating that the local north or east is not configured. */
        const val LOCAL_NOT_CONFIGURED: Short = Short.MIN_VALUE // 0x8000
    }
}

/**
 * Encodes the local location fields as 9 bytes: north (2), east (2), altitude (2), floor number (1)
 * and uncertainty (2), all little endian.
 */
internal fun encodeLocalLocation(
    north: Short,
    east: Short,
    altitude: Short,
    floor: FloorNumber,
    uncertainty: LocationUncertainty,
): ByteArray = north.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
        east.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
        altitude.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
        byteArrayOf(floor.raw.toByte()) +
        uncertainty.raw.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

/**
 * Local floor number as defined in the Generic Location Local state.
 *
 * @property raw Raw 8-bit value. 0x00 to 0xFB represent floors -20 to 231, [GROUND_FLOOR_0] (0xFC)
 *               and [GROUND_FLOOR_1] (0xFD) represent the ground floor, depending on whether the
 *               ground floor is numbered 0 or 1 and [UNKNOWN] (0xFF) means the floor is not known.
 */
@JvmInline
value class FloorNumber(val raw: UByte) {

    /** Returns true if the floor is known. */
    val isKnown: Boolean get() = raw != UNKNOWN_RAW

    /** Returns true if this is the ground floor. */
    val isGroundFloor: Boolean get() = raw == GROUND_FLOOR_0_RAW || raw == GROUND_FLOOR_1_RAW

    /**
     * Floor number, or null if the floor is not known or the raw value is reserved for future use.
     * The ground floor is 0 or 1 depending on [raw].
     */
    val number: Int?
        get() = when {
            raw == GROUND_FLOOR_0_RAW -> 0
            raw == GROUND_FLOOR_1_RAW -> 1
            raw <= MAX_FLOOR_RAW -> raw.toInt() - FLOOR_OFFSET
            else -> null
        }

    override fun toString() = when {
        raw == UNKNOWN_RAW -> "FloorNumber(unknown)"
        raw == GROUND_FLOOR_0_RAW -> "FloorNumber(ground floor, 0)"
        raw == GROUND_FLOOR_1_RAW -> "FloorNumber(ground floor, 1)"
        else -> number?.let { "FloorNumber($it)" } ?: "FloorNumber(reserved, 0x${raw.toString(16)})"
    }

    companion object {
        private const val FLOOR_OFFSET = 20
        private val MAX_FLOOR_RAW: UByte = 0xFBu
        private val GROUND_FLOOR_0_RAW: UByte = 0xFCu
        private val GROUND_FLOOR_1_RAW: UByte = 0xFDu
        private val UNKNOWN_RAW: UByte = 0xFFu

        /** Lowest floor number that can be represented. */
        const val MIN = -20

        /** Highest floor number that can be represented. */
        const val MAX = 231

        /** Ground floor, where the ground floor is referred to as floor 0. */
        val GROUND_FLOOR_0 = FloorNumber(GROUND_FLOOR_0_RAW)

        /** Ground floor, where the ground floor is referred to as floor 1. */
        val GROUND_FLOOR_1 = FloorNumber(GROUND_FLOOR_1_RAW)

        /** Floor that is not known. */
        val UNKNOWN = FloorNumber(UNKNOWN_RAW)

        /**
         * Creates a [FloorNumber] from a floor number between [MIN] and [MAX].
         *
         * @return The floor or null if the number is out of range.
         */
        fun of(number: Int): FloorNumber? =
            if (number in MIN..MAX) FloorNumber((number + FLOOR_OFFSET).toUByte()) else null
    }
}

/**
 * Uncertainty of a local location, which describes whether the device is mobile, how often the
 * location is updated and how precise it is.
 *
 * @property raw Raw 16-bit value. Bit 0 is set for a mobile device, bits 8 to 11 hold the update
 *               time and bits 12 to 15 hold the update precision.
 */
@JvmInline
value class LocationUncertainty(val raw: UShort) {

    /** Returns true if the device is mobile, or false if it is stationary. */
    val isMobile: Boolean get() = raw.toInt() and 0x01 != 0

    /** Raw 4-bit update time. The time is `2^(value - 3)` seconds. 0xF means not known. */
    val updateTimeRaw: Int get() = (raw.toInt() shr 8) and 0x0F

    /** Raw 4-bit update precision. The precision is `2^(value - 3)` meters. 0xF means not known. */
    val precisionRaw: Int get() = (raw.toInt() shr 12) and 0x0F

    /** Time between location updates in milliseconds, or null if not known. */
    val updateTimeMillis: Long? get() = updateTimeRaw.takeIf { it != 0x0F }?.let { 125L shl it }

    /** Precision of the location in millimeters, or null if not known. */
    val precisionMillimeters: Long? get() = precisionRaw.takeIf { it != 0x0F }?.let { 125L shl it }

    override fun toString() = "LocationUncertainty(" +
            "mobile: $isMobile, updateTime: ${updateTimeMillis?.let { "$it ms" } ?: "unknown"}, " +
            "precision: ${precisionMillimeters?.let { "$it mm" } ?: "unknown"})"

    companion object {
        /**
         * Creates a [LocationUncertainty] from the raw 4-bit update time and precision values.
         *
         * @param isMobile      True if the device is mobile.
         * @param updateTimeRaw Raw 4-bit update time, 0 to 15 where 15 means not known.
         * @param precisionRaw  Raw 4-bit precision, 0 to 15 where 15 means not known.
         */
        fun of(isMobile: Boolean, updateTimeRaw: Int = 0x0F, precisionRaw: Int = 0x0F) =
            LocationUncertainty(
                raw = ((if (isMobile) 1 else 0) or
                        ((updateTimeRaw and 0x0F) shl 8) or
                        ((precisionRaw and 0x0F) shl 12)).toUShort()
            )
    }
}

internal fun ByteArray.getFloorNumber(offset: Int) = FloorNumber(raw = this[offset].toUByte())

internal fun ByteArray.getLocationUncertainty(offset: Int) =
    LocationUncertainty(raw = getUShort(offset = offset, order = ByteOrder.LITTLE_ENDIAN))
