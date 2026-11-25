package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray

/**
 * Base interface for Generic Property messages that refer to a single Device Property.
 *
 * @property propertyId The Property ID identifying a device property.
 * @property property   The Device Property, or `null` when the Property ID is not known.
 */
interface GenericPropertyMessage : MeshMessage {

    val propertyId: UShort

    val property: DeviceProperty?
        get() = DeviceProperty.from(propertyId)
}

/**
 * The access rights of a Generic Property, as defined by the Generic User Access, Generic Admin
 * Access and Generic Manufacturer Access states.
 *
 * @property value The raw value of the access rights.
 */
enum class GenericPropertyAccess(val value: UByte) {

    /** The property cannot be accessed (for user access), or is not present. */
    PROHIBITED(0x00u),

    /** The property can be read. */
    READ(0x01u),

    /** The property can be written. */
    WRITE(0x02u),

    /** The property can be read and written. */
    READ_WRITE(0x03u);

    /** Returns true if the property can be read. */
    val canRead: Boolean get() = this == READ || this == READ_WRITE

    /** Returns true if the property can be written. */
    val canWrite: Boolean get() = this == WRITE || this == READ_WRITE

    override fun toString() = when (this) {
        PROHIBITED -> "Prohibited"
        READ -> "Read only"
        WRITE -> "Write only"
        READ_WRITE -> "Read / write"
    }

    companion object {

        /**
         * Returns the [GenericPropertyAccess] for the given raw value.
         *
         * @param value The raw value of the access rights.
         * @return The access rights, or `null` when the value is reserved for future use.
         */
        fun from(value: UByte): GenericPropertyAccess? = entries.find { it.value == value }
    }
}

/**
 * Formats a Property ID for log output, preferring the name of a known Device Property.
 */
internal fun UShort.propertyDescription(): String = DeviceProperty.from(this)?.toString()
    ?: toHexString(format = HexFormat {
        number.prefix = "0x"
        upperCase = true
    })

/** Encodes a list of Property IDs as consecutive little endian 16-bit values. */
internal fun List<UShort>.encodePropertyIds(): ByteArray = fold(byteArrayOf()) { data, id ->
    data + id.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
}

/**
 * Decodes a list of Property IDs, or returns `null` if the data is not a whole number of 16-bit
 * values.
 */
internal fun ByteArray.getPropertyIds(offset: Int = 0): List<UShort>? {
    val length = size - offset
    return if (length < 0 || length % 2 != 0) null
    else (offset until size step 2).map { getUShort(offset = it, order = ByteOrder.LITTLE_ENDIAN) }
}

/**
 * Reads a property value of the given length at the given offset.
 *
 * @return The value, or `null` if the length does not match the length defined for the known
 *         Device Property.
 */
internal fun ByteArray.getPropertyValue(
    propertyId: UShort,
    offset: Int,
): DevicePropertyCharacteristic? {
    val property = DeviceProperty.from(id = propertyId)
    val length = size - offset
    return when {
        property?.valueLength?.let { it != length } == true -> null
        else -> property.read(data = this, offset = offset, length = length)
    }
}

/**
 * The common content of Generic Manufacturer, Admin and User Property Status messages, which carry
 * the Property ID, optionally followed by the access rights and the value.
 *
 * The access rights and the value are omitted when the property is not found, and the value is
 * omitted when the property cannot be read.
 */
internal fun encodePropertyStatus(
    propertyId: UShort,
    access: GenericPropertyAccess?,
    value: DevicePropertyCharacteristic?,
): ByteArray = propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) + when {
    access == null -> byteArrayOf()
    value == null -> byteArrayOf(access.value.toByte())
    else -> byteArrayOf(access.value.toByte()) + value.data
}
