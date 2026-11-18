package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * Retrieves the Generic Default Transition Time state of a server.
 *
 * The response to this message is a [GenericDefaultTransitionTimeStatus].
 */
class GenericDefaultTransitionTimeGet : AcknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = GenericDefaultTransitionTimeStatus.opCode
    override val parameters = null

    override fun toString() = "GenericDefaultTransitionTimeGet"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x820Du

        override fun init(parameters: ByteArray?) = if (parameters == null || parameters.isEmpty())
            GenericDefaultTransitionTimeGet()
        else null
    }
}
