package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * This message is used to request the local location of a GenericLocationServer model. Response
 * received to the message is [GenericLocationLocalStatus].
 */
class GenericLocationLocalGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericLocationLocalStatus.opCode
    override val parameters = null

    override fun toString() = "GenericLocationLocalGet"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8226u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { GenericLocationLocalGet() }
    }
}
