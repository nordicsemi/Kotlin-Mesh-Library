package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.messages.DevicePropertyCharacteristic.*
import no.nordicsemi.kotlin.mesh.core.messages.DevicePropertyCharacteristic.Companion.HUNDRED
import no.nordicsemi.kotlin.mesh.core.messages.DevicePropertyCharacteristic.Companion.MAX_CURRENT
import no.nordicsemi.kotlin.mesh.core.messages.DevicePropertyCharacteristic.Companion.MAX_VOLTAGE
import no.nordicsemi.kotlin.mesh.core.messages.DevicePropertyCharacteristic.Companion.ZERO
import java.math.BigDecimal
import kotlin.time.Duration.Companion.seconds

/** The kind of value a [PropertyField] accepts. */
enum class PropertyFieldType {
    /** A number which may have a fraction, such as `100.5`. */
    DECIMAL,

    /** A number without a fraction, such as `3`. */
    WHOLE_NUMBER,

    /** Either `true` or `false`. */
    BOOLEAN,
}

/**
 * One value a user enters to create the characteristic of a Device Property.
 *
 * @property name  Name of the value, for example "Value" or "Sensing duration".
 * @property unit  Unit of the value, or `null` when it has none.
 * @property type  The kind of value.
 * @property range The lowest and highest accepted value, or `null` for a boolean.
 */
data class PropertyField(
    val name: String,
    val unit: String?,
    val type: PropertyFieldType,
    val range: ClosedRange<BigDecimal>? = null,
)

/**
 * The values to enter to create the characteristic of this property, in the order they are
 * passed to [characteristicOf], or `null` when the characteristic cannot be created from typed
 * values. The raw bytes of such a property are set using [DevicePropertyCharacteristic.Other].
 */
val DeviceProperty.inputFields: List<PropertyField>?
    get() = template()?.spec()?.fields

/**
 * Creates the characteristic of this property from typed values, so that the value does not have
 * to be given as raw bytes. The library encodes it when the message is sent.
 *
 * ```kotlin
 * // Average Input Current of 100 A, averaged over 0 seconds.
 * DeviceProperty.AVERAGE_INPUT_CURRENT.characteristicOf(listOf("100", "0"))
 * ```
 *
 * @param values One value per [inputFields], in the same order. A boolean is `true` or `false`.
 * @return The characteristic, or `null` when the property is not supported by [inputFields], when
 *         the number of values does not match, or when a value is not a number in its range.
 */
fun DeviceProperty.characteristicOf(values: List<String>): DevicePropertyCharacteristic? {
    val spec = template()?.spec() ?: return null
    return if (values.size == spec.fields.size) spec.build(values) else null
}

/**
 * The characteristic of this property decoded from zeroes. Its type tells what the property is
 * made of, which keeps this file consistent with how the property is decoded.
 */
private fun DeviceProperty.template(): DevicePropertyCharacteristic? {
    val length = valueLength ?: return null
    return runCatching { read(data = ByteArray(length), offset = 0, length = length) }.getOrNull()
}

private class Spec(
    val fields: List<PropertyField>,
    val build: (List<String>) -> DevicePropertyCharacteristic?,
)

private const val VALUE = "Value"
private val MAX_SECONDS = BigDecimal("66560641")
private val MAX_24 = BigDecimal(0xFFFFFE)
private val MAX_16 = BigDecimal(0xFFFE)

private fun DevicePropertyCharacteristic.spec(): Spec? = when (this) {
    is Bool -> Spec(listOf(PropertyField(VALUE, null, PropertyFieldType.BOOLEAN))) {
        when (it[0].trim().lowercase()) {
            "true" -> Bool(true)
            "false" -> Bool(false)
            else -> null
        }
    }

    is UvIndex -> whole(unit = null, max = BigDecimal(0xFF)) { UvIndex(it.toInt().toUByte()) }

    is Percentage8 -> decimal("%", ZERO..HUNDRED) { Percentage8(it) }
    is Humidity -> decimal("%", ZERO..HUNDRED) { Humidity(it) }
    is Temperature8 -> decimal("°C", BigDecimal("-64")..BigDecimal("63")) { Temperature8(it) }
    is Temperature ->
        decimal("°C", BigDecimal("-273.15")..BigDecimal("327.67")) { Temperature(it) }
    is ElectricCurrent -> decimal("A", ZERO..MAX_CURRENT) { ElectricCurrent(it) }
    is Voltage -> decimal("V", ZERO..MAX_VOLTAGE) { Voltage(it) }
    is Illuminance -> decimal("lx", ZERO..BigDecimal("167772.14")) { Illuminance(it) }
    is Power -> decimal("W", ZERO..BigDecimal("1677721.4")) { Power(it) }

    is AverageCurrent -> average("A", ZERO..MAX_CURRENT) { value, time ->
        AverageCurrent(value, time)
    }
    is AverageVoltage -> average("V", ZERO..MAX_VOLTAGE) { value, time ->
        AverageVoltage(value, time)
    }

    is Count16 -> whole(unit = null, max = MAX_16) { Count16(it.toInt().toUShort()) }
    is TimeSecond16 -> whole("s", MAX_16) { TimeSecond16(it.toInt().toUShort()) }
    is Co2Concentration -> whole("ppm", MAX_16) { Co2Concentration(it.toInt().toUShort()) }
    is VocConcentration -> whole("ppb", MAX_16) { VocConcentration(it.toInt().toUShort()) }
    is Rainfall -> whole("mm", BigDecimal(0xFFFF)) { Rainfall(it.toInt().toUShort()) }
    is PerceivedLightness ->
        whole(unit = null, max = BigDecimal(0xFFFF)) { PerceivedLightness(it.toInt().toUShort()) }

    is Count24 -> whole(unit = null, max = MAX_24) { Count24(it.toLong().toUInt()) }
    is TimeHour24 -> whole("h", MAX_24) { TimeHour24(it.toLong().toUInt()) }
    is TimeMillisecond24 -> whole("ms", MAX_24) { TimeMillisecond24(it.toLong().toUInt()) }
    is Energy -> whole("kWh", MAX_24) { Energy(it.toLong().toUInt()) }

    else -> null
}

private fun decimal(
    unit: String,
    range: ClosedRange<BigDecimal>,
    create: (BigDecimal) -> DevicePropertyCharacteristic,
) = Spec(listOf(PropertyField(VALUE, unit, PropertyFieldType.DECIMAL, range))) {
    it[0].toDecimal(range)?.let(create)
}

private fun whole(
    unit: String?,
    max: BigDecimal,
    create: (BigDecimal) -> DevicePropertyCharacteristic,
): Spec {
    val range = ZERO..max
    return Spec(listOf(PropertyField(VALUE, unit, PropertyFieldType.WHOLE_NUMBER, range))) {
        it[0].toWhole(range)?.let(create)
    }
}

/** A value averaged over a sensing duration, which is given in seconds. */
private fun average(
    unit: String,
    range: ClosedRange<BigDecimal>,
    create: (BigDecimal, TimeExponential) -> DevicePropertyCharacteristic,
): Spec {
    val seconds = ZERO..MAX_SECONDS
    return Spec(
        listOf(
            PropertyField(VALUE, unit, PropertyFieldType.DECIMAL, range),
            PropertyField("Sensing duration", "s", PropertyFieldType.DECIMAL, seconds),
        )
    ) {
        val value = it[0].toDecimal(range)
        val duration = it[1].toDecimal(seconds)
        if (value == null || duration == null) null
        else create(value, TimeExponential.interval(duration.toDouble().seconds))
    }
}

private fun String.toDecimal(range: ClosedRange<BigDecimal>): BigDecimal? =
    trim().toBigDecimalOrNull()?.takeIf { it in range }

private fun String.toWhole(range: ClosedRange<BigDecimal>): BigDecimal? =
    toDecimal(range)?.takeIf { it.stripTrailingZeros().scale() <= 0 }
