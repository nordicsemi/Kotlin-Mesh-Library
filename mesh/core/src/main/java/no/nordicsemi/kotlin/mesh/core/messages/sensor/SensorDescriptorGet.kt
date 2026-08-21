package no.nordicsemi.kotlin.mesh.core.messages.sensor

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.SensorMessageInitializer
import java.nio.ByteOrder

/**
 * This message is used to request the Sensor Descriptor state of all sensors within an Element, or
 * of a single sensor. The response received to the message is [SensorDescriptorStatus].
 *
 * @property propertyId The Property ID of the sensor to get the descriptor of, or `null` to
 *                      request the descriptors of all sensors found on the Element.
 * @property property   The Device Property of the sensor, or `null` when no property was requested
 *                      or when the Property ID is not known.
 */
class SensorDescriptorGet(val propertyId: UShort?) : AcknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SensorDescriptorStatus.opCode
    override val parameters = propertyId?.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    val property: DeviceProperty?
        get() = propertyId?.let { DeviceProperty.from(id = it) }

    /**
     * Convenience constructor requesting the descriptors of all sensors found on the Element.
     */
    @Suppress("unused")
    constructor() : this(propertyId = null)

    /**
     * Convenience constructor requesting the descriptor of a single sensor.
     *
     * @param property The Device Property of the sensor.
     */
    @Suppress("unused")
    constructor(property: DeviceProperty) : this(propertyId = property.id)

    override fun toString() = "SensorDescriptorGet(property: ${
        property ?: propertyId
            ?.toHexString(
                format = HexFormat {
                    number.prefix = "0x"
                    upperCase = true
                }
            ) ?: "all"
    })"

    companion object Initializer : SensorMessageInitializer {
        override val opCode = 0x8230u

        override fun init(parameters: ByteArray?) = when (parameters?.size) {
            null, 0 -> SensorDescriptorGet(propertyId = null)
            2 -> SensorDescriptorGet(
                propertyId = parameters.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
            )

            else -> null
        }
    }
}
