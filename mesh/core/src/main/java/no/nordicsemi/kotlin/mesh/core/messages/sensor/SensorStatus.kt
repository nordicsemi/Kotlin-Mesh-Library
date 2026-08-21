package no.nordicsemi.kotlin.mesh.core.messages.sensor

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.DevicePropertyCharacteristic
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.SensorMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.read
import java.nio.ByteOrder

/**
 * A Device Property with its corresponding characteristic.
 *
 * @property propertyId The Property ID of the sensor.
 * @property value      The value reported by the sensor.
 * @property property   The Device Property of the sensor, or `null` when the Property ID is not
 *                      known.
 */
data class SensorValue(val propertyId: UShort, val value: DevicePropertyCharacteristic) {

    val property: DeviceProperty?
        get() = DeviceProperty.from(propertyId)

    /**
     * Convenience constructor.
     *
     * @param property The Device Property of the sensor.
     * @param value    The value reported by the sensor.
     */
    constructor(property: DeviceProperty, value: DevicePropertyCharacteristic) : this(
        propertyId = property.id,
        value = value,
    )

    override fun toString() = "${property ?: propertyId}: ${value.description}"
}

/**
 * This message is a response to a [SensorGet] and contains the Sensor Data state of one or more
 * sensors within an Element.
 *
 * Each value is marshalled using Format A when the Property ID is lower than 2048 and the value
 * is 1 to 16 octets long, and using Format B otherwise.
 *
 * @property values The sensor values. The maximum length of a single value is 128 octets.
 */
class SensorStatus(val values: List<SensorValue>) : MeshResponse, UnacknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val parameters = values.fold(byteArrayOf()) { data, value -> data + marshal(value) }

    /**
     * Convenience constructor for a single sensor value.
     *
     * @param value The sensor value. The maximum length of the value is 128 octets.
     */
    @Suppress("unused")
    constructor(value: SensorValue) : this(listOf(value))

    override fun toString() = "SensorStatus(values: ${values.joinToString()})"

    companion object Initializer : SensorMessageInitializer {
        override val opCode = 0x52u

        /** Property IDs lower than this value can be marshalled using Format A. */
        private const val MAX_FORMAT_A_PROPERTY_ID = 2048u

        /** The maximum length of a single sensor value, in octets. */
        private const val MAX_VALUE_LENGTH = 128

        /** Length reported in Format B when the value is 0 octets long. */
        private const val ZERO_LENGTH = 0x7F

        override fun init(parameters: ByteArray?) = parameters?.let { SensorStatus(unmarshal(it)) }

        /**
         * Marshals the given sensor value using Format A or Format B, depending on the Property ID
         * and the length of the value.
         *
         * @param value The sensor value to be marshalled.
         * @return The marshalled data.
         */
        private fun marshal(value: SensorValue): ByteArray {
            val data = value.value.data.let { it.copyOf(minOf(it.size, MAX_VALUE_LENGTH)) }
            val id = value.propertyId.toUInt()

            // Format A can be used when the Property ID is lower than 2048 and the value is
            // 1 to 16 octets long. Otherwise Format B is used.
            return if (id < MAX_FORMAT_A_PROPERTY_ID && data.size in 1..16) {
                // Format A: 1 bit format, 4 bits length - 1, 11 bits Property ID.
                val length = (data.size - 1).toUInt()
                byteArrayOf(
                    ((length shl 1) or ((id and 0x07u) shl 5)).toByte(),
                    (id shr 3).toByte(),
                ) + data
            } else {
                // Format B: 1 bit format, 7 bits length - 1, 16 bits Property ID.
                val length = if (data.isEmpty()) ZERO_LENGTH.toUInt() else (data.size - 1).toUInt()
                byteArrayOf((0x01u or (length shl 1)).toByte()) +
                        value.propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                        data
            }
        }

        /**
         * Unmarshals the received data. Values which cannot be parsed are skipped.
         *
         * @param data The received data.
         * @return The list of sensor values.
         */
        private fun unmarshal(data: ByteArray): List<SensorValue> {
            val values = mutableListOf<SensorValue>()
            var offset = 0
            while (offset < data.size) {
                if (data[offset].toInt() and 0x01 == 0x00) {
                    // Format A: the MPID takes 2 octets and the value 1 to 16 octets.
                    val length = ((data[offset].toInt() shr 1) and 0x0F) + 1
                    if (data.size < offset + 2 + length) {
                        break
                    }
                    val propertyId = ((data[offset].toUByte().toUInt() shr 5) or
                            (data[offset + 1].toUByte().toUInt() shl 3)).toUShort()
                    val property = DeviceProperty.from(propertyId)
                    if (property?.valueLength?.let { it != length } != true) {
                        values.add(
                            SensorValue(
                                propertyId = propertyId,
                                value = property.read(data, offset + 2, length),
                            )
                        )
                    }
                    offset += 2 + length
                } else {
                    // Format B: the MPID takes 3 octets and the value 0 to 128 octets.
                    val reported = data[offset].toUByte().toInt() shr 1
                    val length = if (reported == ZERO_LENGTH) 0 else reported + 1
                    if (data.size < offset + 3 + length) {
                        break
                    }
                    val propertyId = data
                        .getUShort(offset = offset + 1, order = ByteOrder.LITTLE_ENDIAN)
                    val property = DeviceProperty.from(propertyId)
                    if (length == 0 || property?.valueLength?.let { it != length } != true) {
                        values.add(
                            SensorValue(
                                propertyId = propertyId,
                                value = property.read(data, offset + 3, length),
                            )
                        )
                    }
                    offset += 3 + length
                }
            }
            return values
        }
    }
}

/** Returns the data as an uppercase hexadecimal string, with a `0x` prefix. */
internal fun ByteArray.toHex(): String =
    if (isEmpty()) "" else joinToString(separator = "", prefix = "0x") { "%02X".format(it) }
