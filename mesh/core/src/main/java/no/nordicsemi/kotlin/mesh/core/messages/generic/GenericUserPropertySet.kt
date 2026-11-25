package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.DevicePropertyCharacteristic
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.GenericPropertyMessage
import no.nordicsemi.kotlin.mesh.core.messages.getPropertyValue
import no.nordicsemi.kotlin.mesh.core.messages.propertyDescription

/**
 * This message is used to set the User Property state of a GenericUserPropertyServer model. Response received to the message is [GenericUserPropertyStatus].
 *
 * @property propertyId The User Property ID identifying the property.
 * @property value      The new value of the property.
 */
class GenericUserPropertySet(
    override val propertyId: UShort,
    val value: DevicePropertyCharacteristic,
) : AcknowledgedApplicationMessage, GenericPropertyMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericUserPropertyStatus.opCode
    override val parameters: ByteArray
        get() = propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) + value.data

    /**
     * Convenience constructor.
     *
     * @param property The Device Property.
     * @param value    The new value of the property.
     */
    @Suppress("unused")
    constructor(property: DeviceProperty, value: DevicePropertyCharacteristic) : this(
        propertyId = property.id,
        value = value
    )

    override fun toString() = "GenericUserPropertySet(property: ${propertyId.propertyDescription()}, value: ${value.description})"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x4Cu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size >= 3 }
            ?.let { params ->
                val propertyId = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                params.getPropertyValue(propertyId, offset = 2)?.let {
                    GenericUserPropertySet(propertyId, value = it)
                }
            }
    }
}
