package no.nordicsemi.kotlin.mesh.core.messages.foundation.time

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.TimeMessageInitializer

/**
 * A Time Zone Get message is an acknowledged message used to get the Time Zone Offset Current,
 * Time Zone Offset New and TAI of Zone Change states. The response is a [TimeZoneStatus].
 */
class TimeZoneGet : AcknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = TimeZoneStatus.opCode
    override val parameters = null

    override fun toString() = "TimeZoneGet()"

    companion object Initializer : TimeMessageInitializer {
        override val opCode = 0x823Bu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.isEmpty() }
            ?.let { TimeZoneGet() }
    }
}
