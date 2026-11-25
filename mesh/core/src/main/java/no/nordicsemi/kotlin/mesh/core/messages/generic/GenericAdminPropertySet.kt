package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.DevicePropertyCharacteristic
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.GenericPropertyAccess
import no.nordicsemi.kotlin.mesh.core.messages.GenericPropertyMessage
import no.nordicsemi.kotlin.mesh.core.messages.getPropertyValue
import no.nordicsemi.kotlin.mesh.core.messages.propertyDescription

/**
 * This message is used to set the Admin Property state of a GenericAdminPropertyServer model. Response received to the message is [GenericAdminPropertyStatus].
 *
 * @property propertyId The Admin Property ID identifying the property.
 * @property access     The admin access rights.
 * @property value      The new value of the property, or `null` if the value is not included.
 */
class GenericAdminPropertySet(
    override val propertyId: UShort,
    val access: GenericPropertyAccess,
    val value: DevicePropertyCharacteristic?,
) : AcknowledgedApplicationMessage, GenericPropertyMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericAdminPropertyStatus.opCode
    override val parameters: ByteArray
        get() = propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                byteArrayOf(access.value.toByte()) +
                (value?.data ?: byteArrayOf())

    /**
     * Convenience constructor.
     *
     * @param property The Device Property.
     * @param access   The admin access rights.
     * @param value    The new value of the property.
     */
    @Suppress("unused")
    constructor(
        property: DeviceProperty,
        access: GenericPropertyAccess,
        value: DevicePropertyCharacteristic?,
    ) : this(propertyId = property.id, access = access, value = value)

    override fun toString() = "GenericAdminPropertySet(property: ${propertyId.propertyDescription()}, access: $access, value: ${value?.description ?: "none"})"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x48u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size >= 3 }
            ?.let { params ->
                val propertyId = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                GenericPropertyAccess.from(value = params[2].toUByte())?.let { access ->
                    when (params.size) {
                        3 -> GenericAdminPropertySet(propertyId, access, value = null)
                        else -> params.getPropertyValue(propertyId, offset = 3)?.let {
                            GenericAdminPropertySet(propertyId, access, value = it)
                        }
                    }
                }
            }
    }
}
