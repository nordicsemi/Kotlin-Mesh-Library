package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.encodePropertyIds
import no.nordicsemi.kotlin.mesh.core.messages.getPropertyIds

/**
 * This message is a response to a [GenericAdminPropertiesGet] and contains the list of Admin Property IDs
 * of a GenericAdminPropertyServer model.
 *
 * @property propertyIds The Admin Property IDs. The list is empty if the server has no properties.
 * @property properties  The Device Properties, where unknown Property IDs are reported as `null`.
 */
class GenericAdminPropertiesStatus(
    val propertyIds: List<UShort>,
) : ApplicationResponse, UnacknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() = propertyIds.encodePropertyIds()

    val properties: List<DeviceProperty?>
        get() = propertyIds.map { DeviceProperty.from(id = it) }

    override fun toString() = "GenericAdminPropertiesStatus(propertyIds: ${propertyIds.joinToString { id ->
        id.toHexString(format = HexFormat {
            number.prefix = "0x"
            upperCase = true
        })
    }})"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x47u

        override fun init(parameters: ByteArray?) = parameters
            ?.getPropertyIds()
            ?.let { GenericAdminPropertiesStatus(propertyIds = it) }
    }
}
