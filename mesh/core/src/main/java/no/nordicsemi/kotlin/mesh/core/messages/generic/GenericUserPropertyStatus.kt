package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.DevicePropertyCharacteristic
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.GenericPropertyAccess
import no.nordicsemi.kotlin.mesh.core.messages.GenericPropertyMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.encodePropertyStatus
import no.nordicsemi.kotlin.mesh.core.messages.getPropertyValue
import no.nordicsemi.kotlin.mesh.core.messages.propertyDescription

/**
 * This message is a response to a [GenericUserPropertyGet], [GenericUserPropertySet] and
 * [GenericUserPropertySetUnacknowledged] and contains the User Property state of a GenericUserPropertyServer
 * model.
 *
 * @property propertyId The User Property ID identifying the property.
 * @property access     The access rights of the property, or `null` when the property was not
 *                      found.
 * @property value      The value of the property, or `null` when the property was not found or
 *                      cannot be read.
 */
class GenericUserPropertyStatus(
    override val propertyId: UShort,
    val access: GenericPropertyAccess?,
    val value: DevicePropertyCharacteristic?,
) : ApplicationResponse, UnacknowledgedApplicationMessage, GenericPropertyMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() = encodePropertyStatus(propertyId, access, value)

    init {
        require(access != null || value == null) {
            "Property value requires the access rights to be set"
        }
    }

    /**
     * Convenience constructor for a property which has not been found.
     *
     * @param property The Device Property.
     */
    @Suppress("unused")
    constructor(property: DeviceProperty) : this(
        propertyId = property.id,
        access = null,
        value = null
    )

    /**
     * Convenience constructor.
     *
     * @param property The Device Property.
     * @param access   The access rights of the property.
     * @param value    The value of the property.
     */
    @Suppress("unused")
    constructor(
        property: DeviceProperty,
        access: GenericPropertyAccess,
        value: DevicePropertyCharacteristic?,
    ) : this(propertyId = property.id, access = access, value = value)

    override fun toString() = "GenericUserPropertyStatus(property: ${propertyId.propertyDescription()}, " +
            "access: ${access ?: "not found"}, value: ${value?.description ?: "none"})"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x4Eu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size >= 2 }
            ?.let { params ->
                val propertyId = params.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                when (params.size) {
                    2 -> GenericUserPropertyStatus(propertyId, access = null, value = null)
                    else -> GenericPropertyAccess.from(value = params[2].toUByte())?.let { access ->
                        when (params.size) {
                            3 -> GenericUserPropertyStatus(propertyId, access, value = null)
                            else -> params.getPropertyValue(propertyId, offset = 3)?.let {
                                GenericUserPropertyStatus(propertyId, access, value = it)
                            }
                        }
                    }
                }
            }
    }
}
