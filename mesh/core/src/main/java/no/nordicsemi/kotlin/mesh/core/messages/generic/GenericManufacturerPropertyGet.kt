package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.GenericPropertyMessage
import no.nordicsemi.kotlin.mesh.core.messages.propertyDescription

/**
 * This message is used to request the Manufacturer Property state of a GenericManufacturerPropertyServer model.
 * Response received to the message is [GenericManufacturerPropertyStatus].
 *
 * @property propertyId The Manufacturer Property ID identifying the property.
 */
class GenericManufacturerPropertyGet(
    override val propertyId: UShort,
) : AcknowledgedApplicationMessage, GenericPropertyMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericManufacturerPropertyStatus.opCode
    override val parameters: ByteArray
        get() = propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    /**
     * Convenience constructor.
     *
     * @param property The Device Property.
     */
    @Suppress("unused")
    constructor(property: DeviceProperty) : this(propertyId = property.id)

    override fun toString() = "GenericManufacturerPropertyGet(property: ${propertyId.propertyDescription()})"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x822Bu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let {
                GenericManufacturerPropertyGet(
                    propertyId = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                )
            }
    }
}
