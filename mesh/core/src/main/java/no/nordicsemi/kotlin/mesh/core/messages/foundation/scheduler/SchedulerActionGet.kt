package no.nordicsemi.kotlin.mesh.core.messages.foundation.scheduler

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.SchedulerMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.SchedulerRegistryEntry

/**
 * A Scheduler Action Get message is an acknowledged message used to get the action defined by an
 * entry of the Schedule Register. The response to this message is a [SchedulerActionStatus].
 *
 * @property index Index of the Schedule Register entry, 0-15.
 */
class SchedulerActionGet(val index: UByte) : AcknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SchedulerActionStatus.opCode
    override val parameters: ByteArray
        get() = byteArrayOf(index.toByte())

    init {
        require(index <= SchedulerRegistryEntry.MAX_INDEX) { "Index must be in range 0..15!" }
    }

    override fun toString() = "SchedulerActionGet(index: $index)"

    companion object Initializer : SchedulerMessageInitializer {
        override val opCode = 0x8248u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 && it[0].toUByte() <= SchedulerRegistryEntry.MAX_INDEX }
            ?.let { SchedulerActionGet(index = it[0].toUByte()) }
    }
}
