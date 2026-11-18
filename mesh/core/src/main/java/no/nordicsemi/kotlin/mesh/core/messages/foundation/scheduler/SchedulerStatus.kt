package no.nordicsemi.kotlin.mesh.core.messages.foundation.scheduler

import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.SchedulerMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage

/**
 * A Scheduler Status message is an unacknowledged message used to report the Schedule Register
 * state of a Scheduler Server. It is a response to a [SchedulerGet] message.
 *
 * @property schedules  Bitmap of the entries of the Schedule Register that are in use, bit n
 *                      corresponds to the entry with index n.
 */
class SchedulerStatus(val schedules: UShort) : MeshResponse, UnacknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() = byteArrayOf(schedules.toByte(), (schedules.toInt() shr 8).toByte())

    /**
     * Returns true if the entry with the given index (0-15) is in use.
     */
    fun isEntryInUse(index: Int) = index in 0..15 && (schedules.toInt() shr index) and 1 == 1

    override fun toString() = "SchedulerStatus(schedules: 0x${
        schedules.toString(radix = 16).padStart(4, '0').uppercase()
    })"

    companion object Initializer : SchedulerMessageInitializer {
        override val opCode = 0x824Au

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 2 }
            ?.let {
                SchedulerStatus(
                    schedules = ((it[0].toInt() and 0xFF) or ((it[1].toInt() and 0xFF) shl 8))
                        .toUShort()
                )
            }
    }
}
