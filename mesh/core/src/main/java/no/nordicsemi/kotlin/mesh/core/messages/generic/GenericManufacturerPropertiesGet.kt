package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * This message is used to request the list of Manufacturer Property IDs of a GenericManufacturerPropertyServer model.
 * Response received to the message is [GenericManufacturerPropertiesStatus].
 */
class GenericManufacturerPropertiesGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericManufacturerPropertiesStatus.opCode
    override val parameters = null

    override fun toString() = "GenericManufacturerPropertiesGet"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x822Au

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { GenericManufacturerPropertiesGet() }
    }
}
