package no.nordicsemi.kotlin.mesh.core.messages.foundation.scheduler

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.SchedulerMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.SchedulerRegistryEntry

/**
 * A Scheduler Action Set message is an acknowledged message used to set an entry of the Schedule
 * Register. The response to this message is a [SchedulerActionStatus].
 *
 * @property index  Index of the Schedule Register entry, 0-15.
 * @property entry  Entry to be stored.
 */
class SchedulerActionSet(
    val index: UByte,
    val entry: SchedulerRegistryEntry,
) : AcknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SchedulerActionStatus.opCode
    override val parameters: ByteArray
        get() = entry.toByteArray(index)

    init {
        require(index <= SchedulerRegistryEntry.MAX_INDEX) { "Index must be in range 0..15!" }
    }

    override fun toString() = "SchedulerActionSet(index: $index, entry: $entry)"

    companion object Initializer : SchedulerMessageInitializer {
        override val opCode = 0x60u

        override fun init(parameters: ByteArray?) = parameters
            ?.let { SchedulerRegistryEntry.from(it) }
            ?.let { (index, entry) -> SchedulerActionSet(index = index, entry = entry) }
    }
}
