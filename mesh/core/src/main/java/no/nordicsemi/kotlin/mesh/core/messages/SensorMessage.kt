@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import java.nio.ByteOrder
import kotlin.math.pow
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Base initializer for Sensor Messages.
 */
interface SensorMessageInitializer : BaseMeshMessageInitializer, HasOpCode

/**
 * A base interface for sensor property messages.
 *
 * @property propertyId 16-bit Property ID of the sensor.
 * @property property   Device Property describing the meaning and the format of the data reported
 *                      by the sensor, or `null` when the Property ID is not known to this library.
 */
interface SensorPropertyMessage : MeshMessage {

    val propertyId: UShort

    val property: DeviceProperty?
        get() = DeviceProperty.from(propertyId)
}

/**
 * Enumeration of sensor sampling functions.
 *
 * @property value The raw value of the sampling function.
 */
enum class SensorSamplingFunction(val value: UByte) {

    /** Sampling function is not made available. */
    UNSPECIFIED(0x00.toUByte()),

    /** The presented value is an instantaneous sample. */
    INSTANTANEOUS(0x01.toUByte()),

    /** Presented value is the arithmetic mean of multiple samples. */
    ARITHMETIC_MEAN(0x02.toUByte()),

    /** Presented value is the root mean square of multiple samples. */
    RMS(0x03.toUByte()),

    /** Presented value is the maximum of multiple samples. */
    MAXIMUM(0x04.toUByte()),

    /** Presented value is the minimum of multiple samples. */
    MINIMUM(0x05.toUByte()),

    /**
     * The Accumulated sampling function is intended to represent a cumulative moving average.
     *
     * The Sensor Measurement Period in this case would state the length of the period over which
     * a counted number of lightning strikes was detected.
     */
    ACCUMULATED(0x06.toUByte()),

    /**
     * The Count sampling function can be used for a discrete variable such as the number of
     * lightning discharges detected by a lightning detector.
     *
     * The measurement value would be a cumulative moving average value that was continually
     * updated with a frequency indicated by the Sensor Update Interval.
     */
    COUNT(0x07.toUByte());

    override fun toString() = when (this) {
        UNSPECIFIED -> "Unspecified"
        INSTANTANEOUS -> "Instantaneous"
        ARITHMETIC_MEAN -> "Arithmetic Mean"
        RMS -> "Root Mean Square"
        MAXIMUM -> "Maximum"
        MINIMUM -> "Minimum"
        ACCUMULATED -> "Accumulated"
        COUNT -> "Count"
    }

    companion object {

        /**
         * Returns the Sensor Sampling Function for the given value.
         *
         * @param value The raw value of the sampling function.
         * @return The Sensor Sampling Function, or `null` when the value is not known.
         */
        fun from(value: UByte): SensorSamplingFunction? = entries.find { it.value == value }
    }
}

/**
 * The Sensor Descriptor state represents the attributes describing the sensor data.
 *
 * This state does not change throughout the lifetime of an Element.
 *
 * @property propertyId             The 16-bit Property ID describing the meaning and the format of
 *                                  the data reported by the sensor.
 * @property positiveTolerance      A 12-bit value representing the magnitude of a possible
 *                                  positive error associated with the measurements that the
 *                                  sensor is reporting, where the error can be calculated as
 *                                  `100% * tolerance / 4095`. A tolerance of 0 is to be interpreted
 *                                  as "unspecified".
 * @property negativeTolerance      A 12-bit value representing the magnitude of a possible negative
 *                                  error associated with the measurements that the sensor is
 *                                  reporting, where the error can be calculated as
 *                                  100% * tolerance / 4095`. A tolerance of 0 is to be interpreted
 *                                  as "unspecified".
 * @property samplingFunction       The averaging operation or type of sampling function applied
 *                                  to the measured value.
 * @property measurementPeriodValue An 8-bit value `n` representing the averaging time span,
 *                                  accumulation time, or measurement period over which the
 *                                  measurement is taken, where the represented value is equal to
 *                                  `1.1^(n-64)` seconds. The value 0 indicates that the period is
 *                                  not available or is not applicable.
 * @property updateIntervalValue    An 8-bit value `n` determining the interval between internal
 *                                  refreshes of the measurement, where the represented value is
 *                                  equal to `1.1^(n-64)` seconds. The value 0 indicates that the
 *                                  interval is not available or is not applicable.
 */
class SensorDescriptor(
    val propertyId: UShort,
    val positiveTolerance: UShort,
    val negativeTolerance: UShort,
    val samplingFunction: SensorSamplingFunction,
    internal val measurementPeriodValue: UByte,
    internal val updateIntervalValue: UByte,
) {

    /**
     * Convenience constructor.
     *
     * @param property          The Device Property that describes the meaning and the format of
     *                          the data reported by the sensor.
     * @param positiveTolerance The magnitude of a possible positive error associated with the
     *                          measurements that the sensor is reporting. A tolerance of 0 is to
     *                          be interpreted as "unspecified".
     * @param negativeTolerance The magnitude of a possible negative error associated with the
     *                          measurements that the sensor is reporting. A tolerance of 0 is to
     *                          be interpreted as "unspecified".
     * @param samplingFunction  The averaging operation or type of sampling function applied to
     *                          the measured value.
     * @param measurementPeriod The averaging time span, accumulation time, or measurement period
     *                          over which the measurement is taken, as `1.1^(n-64)` seconds.
     *                          The value 0 indicates that the period is not applicable.
     * @param updateInterval    The interval of internal refreshing, as `1.1^(n-64)` seconds.
     *                          The value 0 indicates that the interval is not applicable.
     */
    constructor(
        property: DeviceProperty,
        positiveTolerance: UShort,
        negativeTolerance: UShort,
        samplingFunction: SensorSamplingFunction,
        measurementPeriod: UByte,
        updateInterval: UByte,
    ) : this(
        propertyId = property.id,
        positiveTolerance = positiveTolerance,
        negativeTolerance = negativeTolerance,
        samplingFunction = samplingFunction,
        measurementPeriodValue = measurementPeriod,
        updateIntervalValue = updateInterval,
    )

    init {
        require(positiveTolerance <= MAX_TOLERANCE) { "Positive tolerance must be a 12-bit value" }
        require(negativeTolerance <= MAX_TOLERANCE) { "Negative tolerance must be a 12-bit value" }
    }

    /**
     * The Device Property describing the meaning and the format of the data reported by the
     * sensor, or `null` when [propertyId] is not known to this library.
     */
    val property: DeviceProperty?
        get() = DeviceProperty.from(propertyId)

    /** Whether the positive tolerance is specified. */
    val isPositiveToleranceSpecified: Boolean
        get() = positiveTolerance > 0u

    /** Whether the negative tolerance is specified. */
    val isNegativeToleranceSpecified: Boolean
        get() = negativeTolerance > 0u

    /**
     * The measurement period, or `null` when the measurement period is not available or is not
     * applicable.
     *
     * For example, the measurement period can specify the length of the period used to obtain an
     * average reading.
     */
    val measurementPeriod: Duration?
        get() = measurementPeriodValue.toExponentialInterval()

    /**
     * The update interval, or `null` when the update interval is not available or is not
     * applicable.
     */
    val updateInterval: Duration?
        get() = updateIntervalValue.toExponentialInterval()

    /** The Sensor Descriptor encoded as it is sent over the mesh network. */
    internal val data: ByteArray
        get() {
            // Two 12-bit tolerance values are encoded in 3 octets.
            val tolerances = (negativeTolerance.toUInt() shl 12) or positiveTolerance.toUInt()
            return propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                    tolerances.toByteArray(order = ByteOrder.LITTLE_ENDIAN).copyOf(3) +
                    byteArrayOf(
                        samplingFunction.value.toByte(),
                        measurementPeriodValue.toByte(),
                        updateIntervalValue.toByte(),
                    )
        }

    override fun toString() = "SensorDescriptor(property: ${
        property ?: propertyId
            .toHexString(
                format = HexFormat {
                    number.prefix = "0x"
                    upperCase = true
                }
            )
    }, positiveTolerance: $positiveTolerance, negativeTolerance: $negativeTolerance, " +
            "samplingFunction: $samplingFunction, measurementPeriod: ${measurementPeriod ?: "N/A"}, " +
            "updateInterval: ${updateInterval ?: "N/A"})"

    override fun equals(other: Any?): Boolean = this === other ||
            (other is SensorDescriptor &&
                    propertyId == other.propertyId &&
                    positiveTolerance == other.positiveTolerance &&
                    negativeTolerance == other.negativeTolerance &&
                    samplingFunction == other.samplingFunction &&
                    measurementPeriodValue == other.measurementPeriodValue &&
                    updateIntervalValue == other.updateIntervalValue)

    override fun hashCode(): Int {
        var result = propertyId.hashCode()
        result = 31 * result + positiveTolerance.hashCode()
        result = 31 * result + negativeTolerance.hashCode()
        result = 31 * result + samplingFunction.hashCode()
        result = 31 * result + measurementPeriodValue.hashCode()
        result = 31 * result + updateIntervalValue.hashCode()
        return result
    }

    companion object {

        /** Length of an encoded Sensor Descriptor, in octets. */
        internal const val LENGTH = 8

        /** The maximum value of a 12-bit tolerance. */
        private val MAX_TOLERANCE = 0x0FFFu.toUShort()

        /**
         * Decodes a Sensor Descriptor from the given parameters.
         *
         * @param parameters The parameters of the message.
         * @param offset     The offset to read from.
         * @return The Sensor Descriptor, or `null` when the parameters are invalid.
         */
        internal fun from(parameters: ByteArray, offset: Int): SensorDescriptor? {
            if (parameters.size < offset + LENGTH) {
                return null
            }
            val samplingFunction = SensorSamplingFunction
                .from(parameters[offset + 5].toUByte()) ?: return null
            // Two 12-bit tolerance values are decoded from 3 octets.
            val positiveTolerance =
                ((parameters[offset + 3].toUInt() and 0x0Fu shl 8) or
                        parameters[offset + 2].toUByte().toUInt()).toUShort()
            val negativeTolerance =
                ((parameters[offset + 4].toUByte().toUInt() shl 4) or
                        (parameters[offset + 3].toUByte().toUInt() shr 4)).toUShort()
            return SensorDescriptor(
                propertyId = parameters.getUShort(offset = offset, order = ByteOrder.LITTLE_ENDIAN),
                positiveTolerance = positiveTolerance,
                negativeTolerance = negativeTolerance,
                samplingFunction = samplingFunction,
                measurementPeriodValue = parameters[offset + 6].toUByte(),
                updateIntervalValue = parameters[offset + 7].toUByte(),
            )
        }
    }
}

/**
 * The Sensor Cadence state controls the cadence of Sensor Status messages.
 *
 * It allows a Sensor Server to publish measured quantities using a Sensor Status message at
 * a higher cadence when a measured quantity is inside a configured range, or when it changes
 * more than a configured delta.
 *
 * @property fastCadencePeriodDivisor A 7-bit value controlling the increased cadence of
 *                                    publishing Sensor Status messages, represented as a `2^n`
 *                                    divisor of the Publish Period. For example, the value 0x04
 *                                    has a divisor of 16, and the value 0x00 a divisor of 1,
 *                                    meaning that the Publish Period does not change. The valid
 *                                    range is 0...15.
 * @property statusTriggerDelta       Controls the positive and negative change of a measured
 *                                    quantity that triggers a more rapid publication of a Sensor
 *                                    Status message. The value of
 *                                    [fastCadencePeriodDivisor] is used as a divider for the
 *                                    Publish Period when the change exceeds the delta.
 * @property statusMinIntervalValue   A 1-octet value controlling the minimum interval between
 *                                    publishing two consecutive Sensor Status messages,
 *                                    represented as `2^n` milliseconds. For example, the value
 *                                    0x0A represents an interval of 1024 ms. The valid range
 *                                    is 0...26.
 * @property fastCadenceLow           The lower boundary of a range of measured quantities when
 *                                    the publishing cadence is increased.
 * @property fastCadenceHigh          The upper boundary of a range of measured quantities when
 *                                    the publishing cadence is increased.
 */
class SensorCadence(
    val fastCadencePeriodDivisor: UByte,
    val statusTriggerDelta: StatusTriggerDelta,
    val statusMinIntervalValue: UByte,
    val fastCadenceLow: DevicePropertyCharacteristic,
    val fastCadenceHigh: DevicePropertyCharacteristic,
) {

    /**
     * The Status Trigger Delta controls the positive and negative change of a measured quantity
     * that triggers a more rapid publication of a Sensor Status message.
     */
    sealed class StatusTriggerDelta {

        /**
         * The delta type and unit are defined by the Format Type of the characteristic of the
         * Sensor Property.
         *
         * @property down The smallest decrease of the measured quantity which triggers a more
         *                rapid publication.
         * @property up   The smallest increase of the measured quantity which triggers a more
         *                rapid publication.
         */
        data class Values(
            val down: DevicePropertyCharacteristic,
            val up: DevicePropertyCharacteristic,
        ) : StatusTriggerDelta()

        /**
         * The unit is unitless and the value is represented as a percentage change with
         * a resolution of 0.01 percent.
         *
         * @property down The smallest decrease of the measured quantity, in 0.01 percent, which
         *                triggers a more rapid publication.
         * @property up   The smallest increase of the measured quantity, in 0.01 percent, which
         *                triggers a more rapid publication.
         */
        data class Percentage(val down: UShort, val up: UShort) : StatusTriggerDelta()

        /**
         * Defines the unit and format of the Status Trigger Delta fields, where 0x00 means that
         * the format is defined by the Format Type of the characteristic that the Sensor
         * Property ID state references, and 0x01 that the unit is unitless and the value is
         * represented as a percentage change with a resolution of 0.01 percent.
         */
        internal val type: UByte
            get() = when (this) {
                is Values -> 0x00.toUByte()
                is Percentage -> 0x01.toUByte()
            }

        /** The Status Trigger Delta encoded as it is sent over the mesh network. */
        internal val data: ByteArray
            get() = when (this) {
                is Values -> down.data + up.data
                is Percentage -> down.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                        up.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
            }
    }

    /**
     * Convenience constructor, which increases the publishing frequency when the measured value
     * is inside the given range, or changes by more than the given delta.
     *
     * @param divisor             Publish Period divisor, in range 0...15.
     * @param low                 Low threshold value of the characteristic which initiates
     *                            publishing with the Publish Period divisor.
     * @param high                High threshold value of the characteristic which initiates
     *                            publishing with the Publish Period divisor.
     * @param deltaDown           The smallest delta down which initiates fast publishing.
     * @param deltaUp             The smallest delta up which initiates fast publishing.
     * @param minIntervalExponent The minimum interval between publications, in `2^n`
     *                            milliseconds, in range 0...26.
     */
    constructor(
        divisor: UByte,
        low: DevicePropertyCharacteristic,
        high: DevicePropertyCharacteristic,
        deltaDown: DevicePropertyCharacteristic,
        deltaUp: DevicePropertyCharacteristic,
        minIntervalExponent: UByte,
    ) : this(
        fastCadencePeriodDivisor = divisor,
        statusTriggerDelta = StatusTriggerDelta.Values(down = deltaDown, up = deltaUp),
        statusMinIntervalValue = minOf(minIntervalExponent, MAX_MIN_INTERVAL_EXPONENT),
        fastCadenceLow = low,
        fastCadenceHigh = high,
    )

    /**
     * Convenience constructor, which increases the publishing frequency when the measured value
     * is inside the given range, or changes by more than the given percentage.
     *
     * @param divisor             Publish Period divisor, in range 0...15.
     * @param low                 Low threshold value of the characteristic which initiates
     *                            publishing with the Publish Period divisor.
     * @param high                High threshold value of the characteristic which initiates
     *                            publishing with the Publish Period divisor.
     * @param deltaDown           The smallest delta down which initiates fast publishing,
     *                            in 0.01 percent.
     * @param deltaUp             The smallest delta up which initiates fast publishing,
     *                            in 0.01 percent.
     * @param minIntervalExponent The minimum interval between publications, in `2^n`
     *                            milliseconds, in range 0...26.
     */
    constructor(
        divisor: UByte,
        low: DevicePropertyCharacteristic,
        high: DevicePropertyCharacteristic,
        deltaDown: UShort,
        deltaUp: UShort,
        minIntervalExponent: UByte,
    ) : this(
        fastCadencePeriodDivisor = divisor,
        statusTriggerDelta = StatusTriggerDelta.Percentage(down = deltaDown, up = deltaUp),
        statusMinIntervalValue = minOf(minIntervalExponent, MAX_MIN_INTERVAL_EXPONENT),
        fastCadenceLow = low,
        fastCadenceHigh = high,
    )

    init {
        require(fastCadencePeriodDivisor <= MAX_PERIOD_DIVISOR) {
            "Fast Cadence Period Divisor must be in range 0...15"
        }
        require(statusMinIntervalValue <= MAX_MIN_INTERVAL_EXPONENT) {
            "Status Min Interval must be in range 0...26"
        }
    }

    /**
     * The minimum interval between publishing two consecutive Sensor Status messages.
     */
    val statusMinInterval: Duration
        get() = (1L shl statusMinIntervalValue.toInt()).milliseconds

    /** The Sensor Cadence encoded as it is sent over the mesh network. */
    internal val data: ByteArray
        get() = byteArrayOf(
            ((fastCadencePeriodDivisor.toUInt() shl 1) or statusTriggerDelta.type.toUInt()).toByte()
        ) + statusTriggerDelta.data +
                byteArrayOf(statusMinIntervalValue.toByte()) +
                fastCadenceLow.data + fastCadenceHigh.data

    override fun toString() =
        "SensorCadence(fastCadencePeriodDivisor: $fastCadencePeriodDivisor, " +
                "statusTriggerDelta: $statusTriggerDelta, statusMinInterval: $statusMinInterval, " +
                "fastCadenceLow: ${fastCadenceLow.description}, " +
                "fastCadenceHigh: ${fastCadenceHigh.description})"

    override fun equals(other: Any?): Boolean = this === other ||
            (other is SensorCadence &&
                    fastCadencePeriodDivisor == other.fastCadencePeriodDivisor &&
                    statusTriggerDelta == other.statusTriggerDelta &&
                    statusMinIntervalValue == other.statusMinIntervalValue &&
                    fastCadenceLow == other.fastCadenceLow &&
                    fastCadenceHigh == other.fastCadenceHigh)

    override fun hashCode(): Int {
        var result = fastCadencePeriodDivisor.hashCode()
        result = 31 * result + statusTriggerDelta.hashCode()
        result = 31 * result + statusMinIntervalValue.hashCode()
        result = 31 * result + fastCadenceLow.hashCode()
        result = 31 * result + fastCadenceHigh.hashCode()
        return result
    }

    companion object {

        /** The maximum value of the Fast Cadence Period Divisor. */
        private val MAX_PERIOD_DIVISOR = 15.toUByte()

        /** The maximum value of the Status Min Interval. */
        private val MAX_MIN_INTERVAL_EXPONENT = 26.toUByte()

        /**
         * Decodes a Sensor Cadence of the given property from the given parameters.
         *
         * The length of the Fast Cadence Low, Fast Cadence High and, when the Status Trigger
         * Type is 0x00, the Status Trigger Delta Down and Up fields is not encoded in the
         * message. It is derived from the remaining number of octets, which must therefore
         * extend to the end of [parameters].
         *
         * @param property   The Device Property of the sensor, or `null` when the Property ID is
         *                   not known.
         * @param parameters The parameters of the message.
         * @param offset     The offset to read from.
         * @return The Sensor Cadence, or `null` when the parameters are invalid.
         */
        internal fun from(
            property: DeviceProperty?,
            parameters: ByteArray,
            offset: Int,
        ): SensorCadence? {
            // At least 6 octets are needed if the characteristic value is just 1 octet.
            if (parameters.size - offset < 6) {
                return null
            }
            val divisor = (parameters[offset].toUByte().toUInt() shr 1).toUByte()
            val remaining = parameters.size - offset - 1

            return when (parameters[offset].toUInt() and 0x01u) {
                // The Status Trigger Delta Down, Up, Fast Cadence Low and High fields all have
                // the same length. Status Min Interval takes 1 octet.
                0x00u -> {
                    if ((remaining - 1) % 4 != 0) {
                        return null
                    }
                    val length = (remaining - 1) / 4
                    if (property?.valueLength?.let { it != length } == true) {
                        return null
                    }
                    SensorCadence(
                        fastCadencePeriodDivisor = divisor,
                        statusTriggerDelta = StatusTriggerDelta.Values(
                            down = property.read(
                                data = parameters,
                                offset = offset + 1,
                                length = length
                            ),
                            up = property.read(
                                data = parameters,
                                offset = offset + 1 + length,
                                length = length
                            ),
                        ),
                        statusMinIntervalValue = parameters[offset + 1 + 2 * length].toUByte(),
                        fastCadenceLow = property.read(
                            data = parameters,
                            offset = offset + 2 + 2 * length,
                            length = length
                        ),
                        fastCadenceHigh = property.read(
                            data = parameters,
                            offset = offset + 2 + 3 * length,
                            length = length
                        ),
                    )
                }
                // The Status Trigger Delta Down and Up fields take 2 octets each, the Status Min
                // Interval 1 octet. The Fast Cadence Low and High fields have the same length.
                else -> {
                    if ((remaining - 5) % 2 != 0) {
                        return null
                    }
                    val length = (remaining - 5) / 2
                    if (property?.valueLength?.let { it != length } == true) {
                        return null
                    }
                    SensorCadence(
                        fastCadencePeriodDivisor = divisor,
                        statusTriggerDelta = StatusTriggerDelta.Percentage(
                            down = parameters
                                .getUShort(offset = offset + 1, order = ByteOrder.LITTLE_ENDIAN),
                            up = parameters
                                .getUShort(offset = offset + 3, order = ByteOrder.LITTLE_ENDIAN),
                        ),
                        statusMinIntervalValue = parameters[offset + 5].toUByte(),
                        fastCadenceLow = property.read(
                            data = parameters,
                            offset = offset + 6,
                            length = length
                        ),
                        fastCadenceHigh = property.read(
                            data = parameters,
                            offset = offset + 6 + length,
                            length = length
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Converts a value `n` encoded as `1.1^(n-64)` seconds to a [Duration], where 0 means that the
 * value is not available or is not applicable.
 */
private fun UByte.toExponentialInterval(): Duration? = when (this) {
    0x00.toUByte() -> null
    else -> 1.1.pow(toInt() - 64).seconds
}
