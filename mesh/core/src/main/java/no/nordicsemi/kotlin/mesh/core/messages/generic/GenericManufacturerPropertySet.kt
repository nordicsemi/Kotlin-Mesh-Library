package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.GenericPropertyAccess
import no.nordicsemi.kotlin.mesh.core.messages.GenericPropertyMessage
import no.nordicsemi.kotlin.mesh.core.messages.propertyDescription

/**
 * This message is used to set the Manufacturer Property state of a GenericManufacturerPropertyServer
 * model. Response received to the message is [GenericManufacturerPropertyStatus].
 *
 * @property propertyId The Manufacturer Property ID identifying the property.
 * @property access     The user access rights, which can be [GenericPropertyAccess.PROHIBITED] or
 *                     [GenericPropertyAccess.READ].
 */
class GenericManufacturerPropertySet(
    override val propertyId: UShort,
    val access: GenericPropertyAccess,
) : AcknowledgedApplicationMessage, GenericPropertyMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericManufacturerPropertyStatus.opCode
    override val parameters: ByteArray
        get() = propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                byteArrayOf(access.value.toByte())

    init {
        require(access == GenericPropertyAccess.PROHIBITED || access == GenericPropertyAccess.READ) {
            "Manufacturer property access can be only Prohibited or Read"
        }
    }

    /**
     * Convenience constructor.
     *
     * @param property The Device Property.
     * @param access   The user access rights.
     */
    @Suppress("unused")
    constructor(property: DeviceProperty, access: GenericPropertyAccess) : this(
        propertyId = property.id,
        access = access
    )

    override fun toString() = "GenericManufacturerPropertySet(property: ${propertyId.propertyDescription()}, access: $access)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x44u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 3 }
            ?.let { params ->
                GenericPropertyAccess.from(value = params[2].toUByte())
                    ?.takeIf { it == GenericPropertyAccess.PROHIBITED || it == GenericPropertyAccess.READ }
                    ?.let { access ->
                        GenericManufacturerPropertySet(
                            propertyId = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                            access = access
                        )
                    }
            }
    }
}
