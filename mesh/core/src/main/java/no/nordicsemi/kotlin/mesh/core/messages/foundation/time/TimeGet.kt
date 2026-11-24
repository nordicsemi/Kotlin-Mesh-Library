package no.nordicsemi.kotlin.mesh.core.messages.foundation.time

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.TimeMessageInitializer

/**
 * A Time Get message is an acknowledged message used to get the current Time state of an element.
 * The response to this message is a [TimeStatus].
 */
class TimeGet : AcknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = TimeStatus.opCode
    override val parameters = null

    override fun toString() = "TimeGet()"

    companion object Initializer : TimeMessageInitializer {
        override val opCode = 0x8237u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { TimeGet() }
    }
}
