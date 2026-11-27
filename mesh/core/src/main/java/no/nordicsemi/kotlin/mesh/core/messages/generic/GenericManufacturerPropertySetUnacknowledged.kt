package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.GenericPropertyAccess
import no.nordicsemi.kotlin.mesh.core.messages.GenericPropertyMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.propertyDescription

/**
 * This is an unacknowledged message used to set the Manufacturer Property state of a
 * GenericManufacturerPropertyServer model.
 *
 * @property propertyId The Manufacturer Property ID identifying the property.
 * @property access     The user access rights, which can be [GenericPropertyAccess.PROHIBITED] or
 *                     [GenericPropertyAccess.READ].
 */
class GenericManufacturerPropertySetUnacknowledged(
    override val propertyId: UShort,
    val access: GenericPropertyAccess,
) : UnacknowledgedApplicationMessage, GenericPropertyMessage {
    override val opCode = Initializer.opCode
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

    override fun toString() = "GenericManufacturerPropertySetUnacknowledged(property: ${propertyId.propertyDescription()}, access: $access)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x45u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 3 }
            ?.let { params ->
                GenericPropertyAccess.from(value = params[2].toUByte())
                    ?.takeIf { it == GenericPropertyAccess.PROHIBITED || it == GenericPropertyAccess.READ }
                    ?.let { access ->
                        GenericManufacturerPropertySetUnacknowledged(
                            propertyId = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                            access = access
                        )
                    }
            }
    }
}
