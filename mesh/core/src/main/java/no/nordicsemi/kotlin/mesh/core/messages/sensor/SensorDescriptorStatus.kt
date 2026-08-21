package no.nordicsemi.kotlin.mesh.core.messages.sensor

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.SensorDescriptor
import no.nordicsemi.kotlin.mesh.core.messages.SensorMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import java.nio.ByteOrder

/**
 * This message is a response to a [SensorDescriptorGet] and contains the Sensor Descriptor states
 * of one or more sensors within an Element.
 *
 * @property result The result received.
 */
class SensorDescriptorStatus(
    val result: Result,
) : MeshResponse, UnacknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val parameters = when (result) {
        is Result.PropertyNotFound -> result.propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN)
        is Result.Descriptors -> result.descriptors
            .fold(byteArrayOf()) { data, descriptor -> data + descriptor.data }
    }

    /**
     * The result returned in a Sensor Descriptor Status message.
     */
    sealed class Result {

        /**
         * The list of Sensor Descriptors returned in the response.
         *
         * When [SensorDescriptorGet] was sent with [SensorDescriptorGet.propertyId] set to `null`,
         * this list contains the descriptors of all sensors found on the target Element.
         * Otherwise, when the requested property was found, it contains only the requested
         * descriptor.
         *
         * @property descriptors The Sensor Descriptors.
         */
        data class Descriptors(val descriptors: List<SensorDescriptor>) : Result()

        /**
         * The requested property was not found on the Element.
         *
         * @property propertyId Property ID which was not found.
         * @property property   Device Property which was not found, or `null` when the Property ID
         *                      is not known.
         */
        data class PropertyNotFound(val propertyId: UShort) : Result() {

            val property: DeviceProperty?
                get() = DeviceProperty.from(id = propertyId)

            override fun toString() = "PropertyNotFound(${
                property ?: propertyId.toHexString(
                    format = HexFormat {
                        number.prefix = "0x"
                        upperCase = true
                    }
                )
            })"
        }
    }

    /**
     * Convenience constructor for a list of Sensor Descriptors, which must not be empty.
     *
     * @param descriptors The Sensor Descriptors.
     */
    @Suppress("unused")
    constructor(descriptors: List<SensorDescriptor>) : this(Result.Descriptors(descriptors))

    /**
     * Convenience constructor for a property which has not been found on the Element.
     *
     * @param property The requested Device Property which has not been found.
     */
    @Suppress("unused")
    constructor(property: DeviceProperty) : this(Result.PropertyNotFound(property.id))

    init {
        require(result !is Result.Descriptors || result.descriptors.isNotEmpty()) {
            "At least one descriptor is required"
        }
    }

    override fun toString() = "SensorDescriptorStatus(result: $result)"

    companion object Initializer : SensorMessageInitializer {
        override val opCode = 0x51u

        override fun init(parameters: ByteArray?) = when {
            parameters == null -> null
            parameters.size == 2 -> SensorDescriptorStatus(
                Result.PropertyNotFound(
                    propertyId = parameters.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                )
            )

            parameters.isNotEmpty() && parameters.size % SensorDescriptor.LENGTH == 0 ->
                (parameters.indices step SensorDescriptor.LENGTH)
                    .mapNotNull { SensorDescriptor.from(parameters = parameters, offset = it) }
                    .takeIf { it.isNotEmpty() }
                    ?.let { SensorDescriptorStatus(result = Result.Descriptors(it)) }

            else -> null
        }
    }
}
