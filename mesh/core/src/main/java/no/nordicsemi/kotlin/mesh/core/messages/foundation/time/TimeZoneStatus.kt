package no.nordicsemi.kotlin.mesh.core.messages.foundation.time

import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.TaiTime
import no.nordicsemi.kotlin.mesh.core.messages.TimeMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TimeZoneOffset
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage

/**
 * A Time Zone Status message is an unacknowledged message used to report the Time Zone Offset
 * Current, Time Zone Offset New and TAI of Zone Change states. It is a response to [TimeZoneGet]
 * and [TimeZoneSet] messages.
 *
 * @property currentOffset    Current local time zone offset.
 * @property newOffset        Upcoming local time zone offset.
 * @property taiOfZoneChange  TAI seconds (40 bits) at which the zone offset changes.
 */
class TimeZoneStatus(
    val currentOffset: TimeZoneOffset,
    val newOffset: TimeZoneOffset,
    val taiOfZoneChange: ULong,
) : MeshResponse, UnacknowledgedMeshMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() = byteArrayOf(currentOffset.rawValue.toByte(), newOffset.rawValue.toByte()) +
                TaiTime.writeSeconds(taiOfZoneChange)

    init {
        require(taiOfZoneChange <= TaiTime.MAX_SECONDS) {
            "TAI of Zone Change must fit in 40 bits!"
        }
    }

    override fun toString() = "TimeZoneStatus(currentOffset: $currentOffset, " +
            "newOffset: $newOffset, taiOfZoneChange: $taiOfZoneChange)"

    companion object Initializer : TimeMessageInitializer {
        override val opCode = 0x823Du

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 7 }
            ?.let {
                TimeZoneStatus(
                    currentOffset = TimeZoneOffset(it[0].toUByte()),
                    newOffset = TimeZoneOffset(it[1].toUByte()),
                    taiOfZoneChange = TaiTime.readSeconds(it, 2)
                )
            }
    }
}
