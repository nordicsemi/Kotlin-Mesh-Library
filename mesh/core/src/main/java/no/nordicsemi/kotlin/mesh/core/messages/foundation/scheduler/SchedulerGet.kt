package no.nordicsemi.kotlin.mesh.core.messages.foundation.scheduler

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.SchedulerMessageInitializer

/**
 * A Scheduler Get message is an acknowledged message used to get the Schedule Register state of a
 * Scheduler Server. The response to this message is a [SchedulerStatus].
 */
class SchedulerGet : AcknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SchedulerStatus.opCode
    override val parameters = null

    override fun toString() = "SchedulerGet()"

    companion object Initializer : SchedulerMessageInitializer {
        override val opCode = 0x8249u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { SchedulerGet() }
    }
}
