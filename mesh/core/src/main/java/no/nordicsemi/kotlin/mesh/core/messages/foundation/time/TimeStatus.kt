package no.nordicsemi.kotlin.mesh.core.messages.foundation.time

import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.TaiTime
import no.nordicsemi.kotlin.mesh.core.messages.TimeMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage

/**
 * A Time Status message is an unacknowledged message used to report the Time state of an element.
 * It is a response to [TimeGet] and [TimeSet] messages.
 *
 * If the TAI seconds is 0 (unknown time), only the 5 bytes of the TAI seconds are sent, as the
 * remaining fields shall be omitted.
 *
 * @property time Current time.
 */
class TimeStatus(val time: TaiTime) : MeshResponse, UnacknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() = if (time.isKnown) time.toByteArray() else ByteArray(5)

    override fun toString() = "TimeStatus(time: $time)"

    companion object Initializer : TimeMessageInitializer {
        override val opCode = 0x5Du

        override fun init(parameters: ByteArray?) = when (parameters?.size) {
            5 -> TaiTime.readSeconds(parameters, 0)
                .takeIf { it == 0uL }
                ?.let { TimeStatus(time = TaiTime()) }

            10 -> TaiTime.from(parameters)?.let { TimeStatus(time = it) }
            else -> null
        }
    }
}
