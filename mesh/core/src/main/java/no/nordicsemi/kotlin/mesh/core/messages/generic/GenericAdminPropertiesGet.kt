package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * This message is used to request the list of Admin Property IDs of a GenericAdminPropertyServer model.
 * Response received to the message is [GenericAdminPropertiesStatus].
 */
class GenericAdminPropertiesGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericAdminPropertiesStatus.opCode
    override val parameters = null

    override fun toString() = "GenericAdminPropertiesGet"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x822Cu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { GenericAdminPropertiesGet() }
    }
}
