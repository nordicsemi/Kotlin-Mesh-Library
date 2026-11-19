package no.nordicsemi.kotlin.mesh.core.messages.foundation.time

import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.TaiTime
import no.nordicsemi.kotlin.mesh.core.messages.TimeMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TimeZoneOffset

/**
 * A Time Zone Set message is an acknowledged message used to set the Time Zone Offset New and TAI
 * of Zone Change states. The response to this message is a [TimeZoneStatus].
 *
 * @property newOffset        Upcoming local time zone offset.
 * @property taiOfZoneChange  TAI seconds (40 bits) at which the zone offset changes.
 */
class TimeZoneSet(
    val newOffset: TimeZoneOffset,
    val taiOfZoneChange: ULong,
) : AcknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = TimeZoneStatus.opCode
    override val parameters: ByteArray
        get() = byteArrayOf(newOffset.rawValue.toByte()) + TaiTime.writeSeconds(taiOfZoneChange)

    init {
        require(taiOfZoneChange <= TaiTime.MAX_SECONDS) {
            "TAI of Zone Change must fit in 40 bits!"
        }
    }

    override fun toString() =
        "TimeZoneSet(newOffset: $newOffset, taiOfZoneChange: $taiOfZoneChange)"

    companion object Initializer : TimeMessageInitializer {
        override val opCode = 0x823Cu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 6 }
            ?.let {
                TimeZoneSet(
                    newOffset = TimeZoneOffset(it[0].toUByte()),
                    taiOfZoneChange = TaiTime.readSeconds(it, 1)
                )
            }
    }
}
