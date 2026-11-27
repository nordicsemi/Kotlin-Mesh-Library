package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.propertyDescription

/**
 * This message is used to request the list of Client Property IDs of a GenericClientPropertyServer
 * model, starting from the given Client Property ID. Response received to the message is
 * [GenericClientPropertiesStatus].
 *
 * @property propertyId The Client Property ID from which the list should start. The list contains
 *                      the Property IDs that are greater than or equal to this value.
 */
class GenericClientPropertiesGet(
    val propertyId: UShort,
) : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericClientPropertiesStatus.opCode
    override val parameters: ByteArray
        get() = propertyId.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    override fun toString() = "GenericClientPropertiesGet(propertyId: ${propertyId.propertyDescription()})"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x4Fu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let {
                GenericClientPropertiesGet(
                    propertyId = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN)
                )
            }
    }
}
