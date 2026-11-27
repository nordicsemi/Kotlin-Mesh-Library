package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * This message is used to request the list of User Property IDs of a GenericUserPropertyServer model.
 * Response received to the message is [GenericUserPropertiesStatus].
 */
class GenericUserPropertiesGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericUserPropertiesStatus.opCode
    override val parameters = null

    override fun toString() = "GenericUserPropertiesGet"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x822Eu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { GenericUserPropertiesGet() }
    }
}
