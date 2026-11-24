package no.nordicsemi.kotlin.mesh.core.messages.foundation.scheduler

import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.SchedulerMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.SchedulerRegistryEntry
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage

/**
 * A Scheduler Action Status message is an unacknowledged message used to report an entry of the
 * Schedule Register. It is a response to [SchedulerActionGet] and [SchedulerActionSet] messages.
 *
 * @property index  Index of the Schedule Register entry, 0-15.
 * @property entry  Entry of the Schedule Register.
 */
class SchedulerActionStatus(
    val index: UByte,
    val entry: SchedulerRegistryEntry,
) : MeshResponse, UnacknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() = entry.toByteArray(index)

    init {
        require(index <= SchedulerRegistryEntry.MAX_INDEX) { "Index must be in range 0..15!" }
    }

    override fun toString() = "SchedulerActionStatus(index: $index, entry: $entry)"

    companion object Initializer : SchedulerMessageInitializer {
        override val opCode = 0x5Fu

        override fun init(parameters: ByteArray?) = parameters
            ?.let { SchedulerRegistryEntry.from(it) }
            ?.let { (index, entry) -> SchedulerActionStatus(index = index, entry = entry) }
    }
}
