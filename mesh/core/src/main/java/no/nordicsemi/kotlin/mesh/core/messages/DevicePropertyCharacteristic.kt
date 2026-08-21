@file:Suppress("unused", "MemberVisibilityCanBePrivate")
@file:OptIn(ExperimentalTime::class)

package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.DevicePropertyCharacteristic.Companion.INVALID
import no.nordicsemi.kotlin.mesh.core.messages.DevicePropertyCharacteristic.Companion.UNKNOWN
import java.math.BigDecimal
import java.math.RoundingMode
import java.nio.ByteOrder
import kotlin.math.ln
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Represents a valid or an invalid decimal value.
 *
 * Some characteristics use one of the highest values of their range to indicate that the value
 * is not valid, and the highest one to indicate that the value is not known. The latter is
 * represented by `null`.
 */
sealed class ValidDecimal {

    /**
     * The value is valid.
     *
     * @property value The decimal value.
     */
    data class Valid(val value: BigDecimal) : ValidDecimal()

    /**
     * The value is invalid.
     */
    data object Invalid : ValidDecimal()
}

/**
 * The time represented by the value `1.1^(N-64)` in seconds, with `N` being the raw 8-bit value.
 */
sealed class TimeExponential {

    /**
     * Approximate value of the time in seconds, encoded as `1.1^(N-64)`.
     *
     * @property n The raw 8-bit value.
     */
    data class RawValue(val n: UByte) : TimeExponential()

    /**
     * The total lifetime of the device.
     */
    data object DeviceLifetime : TimeExponential()

    /**
     * Approximate time interval calculated from the raw value, or `null` for [DeviceLifetime].
     *
     * As the time is encoded as `1.1^(N-64)`, the returned value may differ from the one used to
     * create this object.
     */
    val interval: Duration?
        get() = when (this) {
            is DeviceLifetime -> null
            is RawValue -> when (val exponent = n.toInt() - 64) {
                // Special case for 0 seconds.
                -64 -> Duration.ZERO
                else -> 1.1.pow(exponent).seconds
            }
        }

    override fun toString() = interval?.toString() ?: "Total device lifetime"

    companion object {

        /**
         * Returns the [TimeExponential] for the given raw value.
         *
         * @param rawValue The raw 8-bit value, where 0xFE means the device lifetime and 0xFF that
         *                 the value is not known.
         * @return The [TimeExponential], or `null` when the value is not known.
         */
        fun from(rawValue: UByte): TimeExponential? = when (rawValue) {
            0xFE.toUByte() -> DeviceLifetime
            0xFF.toUByte() -> null
            else -> RawValue(rawValue)
        }

        /**
         * Creates a [TimeExponential] from the given interval.
         *
         * As the time is encoded as `1.1^(N-64)` the value will be rounded to the nearest
         * possible one.
         *
         * @param interval The time interval.
         * @return The [TimeExponential].
         */
        fun interval(interval: Duration): TimeExponential {
            val seconds = interval.inWholeMilliseconds / 1000.0
            return when {
                seconds <= 0.0 -> RawValue(0u)
                seconds > 66560641 -> RawValue(0xFDu)
                else -> {
                    val n = (ln(seconds) / ln(1.1) + 64).toInt()
                    RawValue(if (n > 0) n.toUByte() else 0u)
                }
            }
        }
    }
}

/**
 * A representation of a Device Property characteristic.
 *
 * The unit of a characteristic is specified in the documentation of each type. For example,
 * [ElectricCurrent] is expressed in Amperes with a resolution of 0.01 A.
 *
 * #### Encoding sample
 * ```kotlin
 * // The value will be encoded as 0xD204 (12.34).
 * val characteristic = DevicePropertyCharacteristic.ElectricCurrent(BigDecimal("12.345"))
 * ```
 * #### Decoding sample
 * ```kotlin
 * val characteristic = DeviceProperty.PRESENT_INPUT_CURRENT.read(data, offset = 0, length = 2)
 * if (characteristic is DevicePropertyCharacteristic.ElectricCurrent) {
 *     println(characteristic.description) // -> "12.34 A"
 * }
 * ```
 */
sealed class DevicePropertyCharacteristic {

    /**
     * The integral of Apparent Power over a time interval, represented in units of kVAh
     * (kilo-volt-ampere-hour).
     *
     * Unit is kilo-volt-ampere-hour with a resolution of 1 volt-ampere-hour.
     *
     * @property value The value, or `null` when it is not known.
     */
    data class ApparentEnergy32(val value: ValidDecimal?) : DevicePropertyCharacteristic()

    /**
     * Apparent power is the product of the quadratic mean values of voltage and current.
     *
     * Unit is volt-ampere (VA) with a resolution of 0.1 VA.
     *
     * @property value The value, or `null` when it is not known.
     */
    data class ApparentPower(val value: ValidDecimal?) : DevicePropertyCharacteristic()

    /**
     * An electric current averaged over a sensing duration.
     *
     * Unit is Ampere with a resolution of 0.01 A.
     *
     * @property value           The value, or `null` when it is not known.
     * @property sensingDuration The duration the value was averaged over, or `null` when it is
     *                           not known.
     */
    data class AverageCurrent(
        val value: BigDecimal?,
        val sensingDuration: TimeExponential?,
    ) : DevicePropertyCharacteristic()

    /**
     * A voltage averaged over a sensing duration.
     *
     * Unit is Volt with a resolution of 1/64 V.
     *
     * @property value           The value, or `null` when it is not known.
     * @property sensingDuration The duration the value was averaged over, or `null` when it is
     *                           not known.
     */
    data class AverageVoltage(
        val value: BigDecimal?,
        val sensingDuration: TimeExponential?,
    ) : DevicePropertyCharacteristic()

    /**
     * A boolean value.
     *
     * @property value The value.
     */
    data class Bool(val value: Boolean) : DevicePropertyCharacteristic()

    /**
     * A unitless 16-bit count.
     *
     * @property value The value, or `null` when it is not known.
     */
    data class Count16(val value: UShort?) : DevicePropertyCharacteristic()

    /**
     * A unitless 24-bit count.
     *
     * @property value The value, or `null` when it is not known.
     */
    data class Count24(val value: UInt?) : DevicePropertyCharacteristic()

    /**
     * A unitless coefficient, encoded as an IEEE 754 32-bit floating point value.
     *
     * @property value The value.
     */
    data class Coefficient(val value: Float) : DevicePropertyCharacteristic()

    /**
     * A concentration of carbon dioxide.
     *
     * Unit is parts per million (ppm), where 0xFFFE means 65534 ppm or more.
     *
     * @property value The value, or `null` when it is not known.
     */
    data class Co2Concentration(val value: UShort?) : DevicePropertyCharacteristic()

    /**
     * A date, encoded as the number of days elapsed since the UTC epoch.
     *
     * @property value The date, or `null` when it is not known.
     */
    data class DateUtc(val value: Instant?) : DevicePropertyCharacteristic()

    /**
     * An electric current.
     *
     * Unit is Ampere with a resolution of 0.01 A.
     *
     * @property value The value, or `null` when it is not known.
     */
    data class ElectricCurrent(val value: BigDecimal?) : DevicePropertyCharacteristic()

    /**
     * Energy consumption.
     *
     * Unit is kilowatt-hour (kWh) with a resolution of 1 kWh.
     *
     * @property value The value, or `null` when it is not known.
     */
    data class Energy(val value: UInt?) : DevicePropertyCharacteristic()

    /**
     * Energy consumption with a higher resolution.
     *
     * Unit is kilowatt-hour (kWh) with a resolution of 0.001 kWh.
     *
     * @property value The value, or `null` when it is not known.
     */
    data class Energy32(val value: ValidDecimal?) : DevicePropertyCharacteristic()

    /**
     * Statistics of an event.
     *
     * @property count                     Number of times the event occurred, or `null` when it
     *                                     is not known.
     * @property averageEventDuration      Average duration of the event in seconds, or `null`
     *                                     when it is not known.
     * @property timeElapsedSinceLastEvent Time elapsed since the last event, or `null` when it
     *                                     is not known.
     * @property sensingDuration           Duration of the sensing period, or `null` when it is
     *                                     not known.
     */
    data class EventStatistics(
        val count: UShort?,
        val averageEventDuration: UShort?,
        val timeElapsedSinceLastEvent: TimeExponential?,
        val sensingDuration: TimeExponential?,
    ) : DevicePropertyCharacteristic()

    /**
     * A UTF-8 string of a fixed length of 8 bytes.
     *
     * @property value The value.
     */
    data class FixedString8(val value: String) : DevicePropertyCharacteristic()

    /**
     * A UTF-8 string of a fixed length of 16 bytes.
     *
     * @property value The value.
     */
    data class FixedString16(val value: String) : DevicePropertyCharacteristic()

    /**
     * A UTF-8 string of a fixed length of 24 bytes.
     *
     * @property value The value.
     */
    data class FixedString24(val value: String) : DevicePropertyCharacteristic()

    /**
     * A UTF-8 string of a fixed length of 36 bytes.
     *
     * @property value The value.
     */
    data class FixedString36(val value: String) : DevicePropertyCharacteristic()

    /**
     * A UTF-8 string of a fixed length of 64 bytes.
     *
     * @property value The value.
     */
    data class FixedString64(val value: String) : DevicePropertyCharacteristic()

    /**
     * A relative humidity.
     *
     * Unit is percent with a resolution of 0.01%.
     *
     * @property value The value, or `null` when it is not known.
     */
    data class Humidity(val value: BigDecimal?) : DevicePropertyCharacteristic()

    /**
     * An illuminance.
     *
     * Unit is lux with a resolution of 0.01 lux.
     *
     * @property value The value, or `null` when it is not known.
     */
    data class Illuminance(val value: BigDecimal?) : DevicePropertyCharacteristic()

    /**
     * A percentage, encoded on a single octet.
     *
     * Unit is percent with a resolution of 0.5%.
     *
     * @property value The value, or `null` when it is not known.
     */
    data class Percentage8(val value: BigDecimal?) : DevicePropertyCharacteristic()

    /**
     * A perceived lightness.
     *
     * @property value The value.
     */
    data class PerceivedLightness(val value: UShort) : DevicePropertyCharacteristic()

    /**
     * A power.
     *
     * Unit is Watt with a resolution of 0.1 W.
     *
     * @property value The value, or `null` when it is not known.
     */
    data class Power(val value: BigDecimal?) : DevicePropertyCharacteristic()

    /**
     * A pressure.
     *
     * Unit is Pascal with a resolution of 0.1 Pa.
     *
     * @property value The value.
     */
    data class Pressure(val value: BigDecimal) : DevicePropertyCharacteristic()

    /**
     * A rainfall.
     *
     * Unit is millimeter with a resolution of 1 mm.
     *
     * @property value The value.
     */
    data class Rainfall(val value: UShort) : DevicePropertyCharacteristic()

    /**
     * A temperature, encoded on two octets.
     *
     * Unit is degree Celsius with a resolution of 0.01 &deg;C.
     *
     * @property value The value, or `null` when it is not known.
     */
    data class Temperature(val value: BigDecimal?) : DevicePropertyCharacteristic()

    /**
     * A temperature, encoded on a single octet.
     *
     * Unit is degree Celsius with a resolution of 0.5 &deg;C.
     *
     * @property value The value, or `null` when it is not known.
     */
    data class Temperature8(val value: BigDecimal?) : DevicePropertyCharacteristic()

    /**
     * A time period expressed in hours, encoded on three octets.
     *
     * @property value The number of hours, or `null` when it is not known.
     */
    data class TimeHour24(val value: UInt?) : DevicePropertyCharacteristic()

    /**
     * A time period expressed in milliseconds, encoded on three octets.
     *
     * @property value The number of milliseconds, or `null` when it is not known.
     */
    data class TimeMillisecond24(val value: UInt?) : DevicePropertyCharacteristic()

    /**
     * A time period expressed in seconds, encoded on two octets.
     *
     * @property value The number of seconds, or `null` when it is not known.
     */
    data class TimeSecond16(val value: UShort?) : DevicePropertyCharacteristic()

    /**
     * A time period expressed in seconds, encoded on four octets.
     *
     * @property value The number of seconds, or `null` when it is not known.
     */
    data class TimeSecond32(val value: UInt?) : DevicePropertyCharacteristic()

    /**
     * A UV index.
     *
     * @property value The value.
     */
    data class UvIndex(val value: UByte) : DevicePropertyCharacteristic()

    /**
     * A concentration of volatile organic compounds.
     *
     * Unit is parts per billion (ppb), where 0xFFFE means 65534 ppb or more.
     *
     * @property value The value, or `null` when it is not known.
     */
    data class VocConcentration(val value: UShort?) : DevicePropertyCharacteristic()

    /**
     * A voltage.
     *
     * Unit is Volt with a resolution of 1/64 V.
     *
     * @property value The value, or `null` when it is not known.
     */
    data class Voltage(val value: BigDecimal?) : DevicePropertyCharacteristic()

    /**
     * A characteristic which is not supported by this library, returned as raw data.
     *
     * @property value The raw value.
     */
    class Other(val value: ByteArray) : DevicePropertyCharacteristic() {

        override fun equals(other: Any?): Boolean =
            this === other || (other is Other && value.contentEquals(other.value))

        override fun hashCode() = value.contentHashCode()

        override fun toString() = "Other(value: ${value.toHex()})"
    }

    /**
     * The characteristic value encoded as it is sent over the mesh network.
     */
    internal val data: ByteArray
        get() = when (this) {
            // Bool:
            is Bool -> byteArrayOf(if (value) 0x01 else 0x00)

            is UvIndex -> byteArrayOf(value.toByte())

            // Event Statistics:
            is EventStatistics ->
                Count16(count).data +
                    TimeSecond16(averageEventDuration).data +
                    timeElapsedSinceLastEvent.toData() +
                    sensingDuration.toData()

            // BigDecimal? as UInt8 with 0xFF as unknown:
            is Percentage8 -> value.toData(
                length = 1,
                range = ZERO..HUNDRED,
                resolution = HALF,
                unknownValue = 0xFF,
            )

            // BigDecimal? as Int8 with 0x7F as unknown (see Errata 15863):
            is Temperature8 -> value.toData(
                length = 1,
                range = BigDecimal("-64.0")..BigDecimal("63.0"),
                resolution = HALF,
                unknownValue = 0x7F,
            )

            // UShort? with 0xFFFF as unknown, and 0xFFFE as greater than 65534:
            is Count16 -> value.toData(length = 2, unknownValue = 0xFFFF)
            is TimeSecond16 -> value.toData(length = 2, unknownValue = 0xFFFF)
            is Co2Concentration -> value.toData(length = 2, unknownValue = 0xFFFF)
            is VocConcentration -> value.toData(length = 2, unknownValue = 0xFFFF)

            // UShort:
            is PerceivedLightness -> value.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
            is Rainfall -> value.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

            // BigDecimal? as UInt16 with 0xFFFF as unknown:
            is Humidity -> value.toData(
                length = 2,
                range = ZERO..HUNDRED,
                resolution = CENTI,
                unknownValue = 0xFFFF,
            )
            is ElectricCurrent -> value.toData(
                length = 2,
                range = ZERO..MAX_CURRENT,
                resolution = CENTI,
                unknownValue = 0xFFFF,
            )
            is AverageCurrent -> value.toData(
                length = 2,
                range = ZERO..MAX_CURRENT,
                resolution = CENTI,
                unknownValue = 0xFFFF,
            ) + sensingDuration.toData()
            is Voltage -> value.toData(
                length = 2,
                range = ZERO..MAX_VOLTAGE,
                resolution = VOLTAGE_RESOLUTION,
                unknownValue = 0xFFFF,
            )
            is AverageVoltage -> value.toData(
                length = 2,
                range = ZERO..MAX_VOLTAGE,
                resolution = VOLTAGE_RESOLUTION,
                unknownValue = 0xFFFF,
            ) + sensingDuration.toData()

            // BigDecimal? as Int16 with 0x8000 as unknown:
            is Temperature -> value.toData(
                length = 2,
                range = BigDecimal("-273.15")..BigDecimal("327.67"),
                resolution = CENTI,
                unknownValue = 0x8000,
            )

            // UInt? as UInt24 with 0xFFFFFF as unknown:
            is Count24 -> value.toData(length = 3, unknownValue = 0xFFFFFF)
            is TimeHour24 -> value.toData(length = 3, unknownValue = 0xFFFFFF)
            is TimeMillisecond24 -> value.toData(length = 3, unknownValue = 0xFFFFFF)
            is Energy -> value.toData(length = 3, unknownValue = 0xFFFFFF)

            // BigDecimal? as UInt24 with 0xFFFFFF as unknown:
            is Illuminance -> value.toData(
                length = 3,
                range = ZERO..BigDecimal("167772.14"),
                resolution = CENTI,
                unknownValue = 0xFFFFFF,
            )
            is Power -> value.toData(
                length = 3,
                range = ZERO..BigDecimal("1677721.4"),
                resolution = DECI,
                unknownValue = 0xFFFFFF,
            )

            // Instant? as UInt24 with 0x000000 as unknown:
            is DateUtc -> when (value) {
                null -> byteArrayOf(0x00, 0x00, 0x00)
                else -> (value.epochSeconds / SECONDS_PER_DAY).toData(length = 3)
            }

            // BigDecimal as UInt32:
            is Pressure -> value.toData(
                length = 4,
                range = ZERO..BigDecimal(UInt.MAX_VALUE.toLong()),
                resolution = DECI,
            )

            // ValidDecimal? as UInt24 with 0xFFFFFE as invalid and 0xFFFFFF as unknown:
            is ApparentPower -> value.toData(
                length = 3,
                range = ZERO..BigDecimal("1677721.3"),
                resolution = DECI,
                invalidValue = 0xFFFFFE,
                unknownValue = 0xFFFFFF,
            )

            // ValidDecimal? as UInt32 with 0xFFFFFFFE as invalid and 0xFFFFFFFF as unknown:
            is Energy32 -> value.toData(
                length = 4,
                range = ZERO..MAX_ENERGY_32,
                resolution = MILLI,
                invalidValue = 0xFFFFFFFE,
                unknownValue = 0xFFFFFFFF,
            )
            is ApparentEnergy32 -> value.toData(
                length = 4,
                range = ZERO..MAX_ENERGY_32,
                resolution = MILLI,
                invalidValue = 0xFFFFFFFE,
                unknownValue = 0xFFFFFFFF,
            )

            // UInt? with 0xFFFFFFFF as unknown:
            is TimeSecond32 -> value.toData(length = 4, unknownValue = 0xFFFFFFFF)

            // Float32 (IEEE 754):
            is Coefficient -> value.toRawBits().toUInt().toByteArray(order = ByteOrder.LITTLE_ENDIAN)

            // String, padded with spaces to the required number of bytes:
            is FixedString8 -> value.toData(length = 8)
            is FixedString16 -> value.toData(length = 16)
            is FixedString24 -> value.toData(length = 24)
            is FixedString36 -> value.toData(length = 36)
            is FixedString64 -> value.toData(length = 64)

            // Other:
            is Other -> value
        }

    /**
     * The value of the characteristic formatted for display, including its unit.
     *
     * Values which are not known are reported as [UNKNOWN], invalid ones as [INVALID].
     */
    val description: String
        get() = when (this) {
            // Bool:
            is Bool -> if (value) "True" else "False"

            is UvIndex -> "$value"

            // Event Statistics:
            is EventStatistics -> "${Count16(count).description} events, " +
                "avg. event duration: ${TimeSecond16(averageEventDuration).description}, " +
                "time elapsed since last event: ${timeElapsedSinceLastEvent ?: "unknown"}, " +
                "sensing duration: ${sensingDuration ?: "unknown"}"

            // BigDecimal:
            is Pressure -> value.format(ZERO..BigDecimal(UInt.MAX_VALUE.toLong() / 10), " Pa")

            // BigDecimal?:
            is Percentage8 -> value?.format(ZERO..HUNDRED, "%") ?: UNKNOWN
            is Humidity -> value?.format(ZERO..HUNDRED, "%") ?: UNKNOWN
            is Temperature8 -> value?.format(BigDecimal("-64")..BigDecimal("63"), "°C") ?: UNKNOWN
            is ElectricCurrent -> value?.format(ZERO..MAX_CURRENT, " A") ?: UNKNOWN
            is AverageCurrent -> value
                ?.let { "${ElectricCurrent(it).description} over ${sensingDuration ?: "an unknown time"}" }
                ?: UNKNOWN
            is Illuminance -> value?.format(ZERO..BigDecimal("167772.13"), " lux") ?: UNKNOWN
            is Power -> value?.format(ZERO..BigDecimal("1677721.4"), " W") ?: UNKNOWN
            is Temperature -> value
                ?.format(BigDecimal("-273.15")..BigDecimal("327.67"), "°C") ?: UNKNOWN
            is Voltage -> when {
                value == null -> UNKNOWN
                value.signum() <= 0 -> "0 V or lower"
                value >= MAX_VOLTAGE -> "1022 V or higher"
                else -> value.format(ZERO..MAX_VOLTAGE, " V")
            }
            is AverageVoltage -> value
                ?.let { "${Voltage(it).description} over ${sensingDuration ?: "an unknown time"}" }
                ?: UNKNOWN

            // UShort:
            is PerceivedLightness -> "$value"
            is Rainfall -> "$value mm"

            // UShort?:
            is Count16 -> value?.toString() ?: UNKNOWN // unitless
            is TimeSecond16 -> value?.toInt()?.seconds?.toString() ?: UNKNOWN
            is Co2Concentration -> when {
                value == null -> UNKNOWN
                value == UNKNOWN_16.dec() -> "65534 ppm or more"
                else -> "$value ppm"
            }
            is VocConcentration -> when {
                value == null -> UNKNOWN
                value == UNKNOWN_16.dec() -> "65534 ppb or more"
                else -> "$value ppb"
            }

            // UInt? as UInt24?:
            is Count24 -> value?.let { "${minOf(it, MAX_24)}" } ?: UNKNOWN // unitless
            is TimeHour24 -> value?.let { minOf(it, MAX_24).toInt().hours.toString() } ?: UNKNOWN
            is TimeMillisecond24 -> value
                ?.let { minOf(it, MAX_24).toInt().milliseconds.toString() } ?: UNKNOWN
            is Energy -> value?.let { "${minOf(it, MAX_24)} kWh" } ?: UNKNOWN

            // UInt?:
            is TimeSecond32 -> value?.toLong()?.seconds?.toString() ?: UNKNOWN

            // ValidDecimal?:
            is Energy32 -> value.format(ZERO..BigDecimal(UInt.MAX_VALUE.toLong()), " kWh")
            is ApparentEnergy32 -> value.format(ZERO..BigDecimal(UInt.MAX_VALUE.toLong()), " kVAh")
            is ApparentPower -> value.format(ZERO..BigDecimal("1677721.3"), " VA")

            // Instant?:
            is DateUtc -> value?.toString() ?: UNKNOWN

            // Float32 (IEEE 754):
            is Coefficient -> "$value" // unitless

            // String:
            is FixedString8 -> value
            is FixedString16 -> value
            is FixedString24 -> value
            is FixedString36 -> value
            is FixedString64 -> value

            // Other:
            is Other -> value.toHex()
        }

    internal companion object {

        /** Text used when the characteristic value is not known. */
        const val UNKNOWN = "Value is not known"

        /** Text used when the characteristic value is not valid. */
        const val INVALID = "Value is not valid"

        internal val ZERO: BigDecimal = BigDecimal.ZERO
        internal val HUNDRED: BigDecimal = BigDecimal("100")
        internal val HALF: BigDecimal = BigDecimal("0.5")
        internal val DECI: BigDecimal = BigDecimal("0.1")
        internal val CENTI: BigDecimal = BigDecimal("0.01")
        internal val MILLI: BigDecimal = BigDecimal("0.001")

        /** Resolution of a voltage value, 1/64 V. */
        internal val VOLTAGE_RESOLUTION: BigDecimal = BigDecimal("0.015625")

        internal val MAX_CURRENT: BigDecimal = BigDecimal("655.34")
        internal val MAX_VOLTAGE: BigDecimal = BigDecimal("1022")
        internal val MAX_ENERGY_32: BigDecimal = BigDecimal("4294967.293")

        private const val SECONDS_PER_DAY = 86_400L
        private val UNKNOWN_16 = 0xFFFF.toUShort()
        private val MAX_24 = 0xFFFFFEu
    }
}

// MARK: - Helper extensions - encoding

/**
 * Converts an optional [TimeExponential] to its 1-octet representation, where 0xFE means the
 * device lifetime and 0xFF that the value is not known.
 */
private fun TimeExponential?.toData(): ByteArray = when (this) {
    null -> byteArrayOf(0xFF.toByte())
    is TimeExponential.DeviceLifetime -> byteArrayOf(0xFE.toByte())
    is TimeExponential.RawValue -> byteArrayOf(n.toByte())
}

/**
 * Returns the given number of the least significant octets of the value, in little endian order.
 */
private fun Long.toData(length: Int): ByteArray =
    ByteArray(length) { i -> (this shr (8 * i)).toByte() }

/**
 * Returns the value as data of the given length, or the unknown value when it is `null`.
 */
private fun UShort?.toData(length: Int, unknownValue: Long): ByteArray =
    (this?.toLong() ?: unknownValue).toData(length)

/**
 * Returns the value as data of the given length, or the unknown value when it is `null`.
 */
private fun UInt?.toData(length: Int, unknownValue: Long): ByteArray =
    (this?.toLong() ?: unknownValue).toData(length)

/**
 * Returns the value as data of the given length, or the unknown value when it is `null`.
 *
 * @param length       Resulting number of octets.
 * @param range        The range the value is to be clamped to.
 * @param resolution   The conversion resolution.
 * @param unknownValue The value used when the value is not known.
 */
private fun BigDecimal?.toData(
    length: Int,
    range: ClosedRange<BigDecimal>,
    resolution: BigDecimal,
    unknownValue: Long,
): ByteArray = when (this) {
    null -> unknownValue.toData(length)
    else -> coerceIn(range.start, range.endInclusive)
        .divide(resolution, 10, RoundingMode.HALF_UP)
        .setScale(0, RoundingMode.DOWN)
        .toLong()
        .toData(length)
}

/**
 * Returns the value as data of the given length.
 */
private fun BigDecimal.toData(
    length: Int,
    range: ClosedRange<BigDecimal>,
    resolution: BigDecimal,
): ByteArray = (this as BigDecimal?).toData(length, range, resolution, unknownValue = 0L)

/**
 * Returns the value as data of the given length, or the invalid or unknown value respectively.
 */
private fun ValidDecimal?.toData(
    length: Int,
    range: ClosedRange<BigDecimal>,
    resolution: BigDecimal,
    invalidValue: Long,
    unknownValue: Long,
): ByteArray = when (this) {
    null -> unknownValue.toData(length)
    is ValidDecimal.Invalid -> invalidValue.toData(length)
    is ValidDecimal.Valid -> value.toData(length, range, resolution)
}

/**
 * Returns the string as UTF-8 data of exactly the given number of octets, padded with spaces
 * or truncated when needed.
 */
private fun String.toData(length: Int): ByteArray =
    padEnd(length, ' ').toByteArray(Charsets.UTF_8).copyOf(length)

// MARK: - Helper extensions - formatting

/**
 * Formats the value clamped to the given range, with 1 to 3 fraction digits and the given unit.
 */
private fun BigDecimal.format(range: ClosedRange<BigDecimal>, unit: String): String {
    val value = coerceIn(range.start, range.endInclusive)
        .setScale(3, RoundingMode.HALF_UP)
        .stripTrailingZeros()
    val scale = if (value.scale() < 1) 1 else value.scale()
    return value.setScale(scale, RoundingMode.HALF_UP).toPlainString() + unit
}

/**
 * Formats an optional [ValidDecimal] value.
 */
private fun ValidDecimal?.format(range: ClosedRange<BigDecimal>, unit: String): String =
    when (this) {
        null -> DevicePropertyCharacteristic.UNKNOWN
        is ValidDecimal.Invalid -> DevicePropertyCharacteristic.INVALID
        is ValidDecimal.Valid -> value.format(range, unit)
    }

/**
 * Returns the data as an uppercase hexadecimal string, with `0x` prefix.
 */
private fun ByteArray.toHex(): String =
    if (isEmpty()) "" else joinToString(separator = "", prefix = "0x") { "%02X".format(it) }
