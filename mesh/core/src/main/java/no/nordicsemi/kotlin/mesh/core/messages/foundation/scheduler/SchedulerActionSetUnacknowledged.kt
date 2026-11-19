package no.nordicsemi.kotlin.mesh.core.messages.foundation.scheduler

import no.nordicsemi.kotlin.mesh.core.messages.SchedulerMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.SchedulerRegistryEntry
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage

/**
 * A Scheduler Action Set Unacknowledged message is an unacknowledged message used to set an entry
 * of the Schedule Register.
 *
 * @property index  Index of the Schedule Register entry, 0-15.
 * @property entry  Entry to be stored.
 */
class SchedulerActionSetUnacknowledged(
    val index: UByte,
    val entry: SchedulerRegistryEntry,
) : UnacknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() = entry.toByteArray(index)

    init {
        require(index <= SchedulerRegistryEntry.MAX_INDEX) { "Index must be in range 0..15!" }
    }

    override fun toString() = "SchedulerActionSetUnacknowledged(index: $index, entry: $entry)"

    companion object Initializer : SchedulerMessageInitializer {
        override val opCode = 0x61u

        override fun init(parameters: ByteArray?) = parameters
            ?.let { SchedulerRegistryEntry.from(it) }
            ?.let { (index, entry) ->
                SchedulerActionSetUnacknowledged(index = index, entry = entry)
            }
    }
}
