package no.nordicsemi.kotlin.mesh.core.messages.foundation.time

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.TaiTime
import no.nordicsemi.kotlin.mesh.core.messages.TimeMessageInitializer

/**
 * A Time Set message is an acknowledged message used to set the Time state of an element. The
 * response to this message is a [TimeStatus].
 *
 * @property time Time to be set.
 */
class TimeSet(val time: TaiTime) : AcknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = TimeStatus.opCode
    override val parameters: ByteArray
        get() = time.toByteArray()

    override fun toString() = "TimeSet(time: $time)"

    companion object Initializer : TimeMessageInitializer {
        override val opCode = 0x5Cu

        override fun init(parameters: ByteArray?) = parameters
            ?.let { TaiTime.from(it) }
            ?.let { TimeSet(time = it) }
    }
}
