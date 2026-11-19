package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * This message is used to request the global location of a GenericLocationServer model. Response
 * received to the message is [GenericLocationGlobalStatus].
 */
class GenericLocationGlobalGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericLocationGlobalStatus.opCode
    override val parameters = null

    override fun toString() = "GenericLocationGlobalGet"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8225u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { GenericLocationGlobalGet() }
    }
}
