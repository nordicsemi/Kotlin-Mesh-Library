package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The SAR Transmitter states of a node as defined in the Bluetooth Mesh Protocol specification.
 * All values are the raw 4-bit state values (0..15).
 *
 * @property segmentIntervalStep                        SAR Segment Interval Step: (n + 1) * 10 ms.
 * @property unicastRetransmissionsCount                SAR Unicast Retransmissions Count.
 * @property unicastRetransmissionsWithoutProgressCount SAR Unicast Retransmissions Without Progress
 *                                                      Count.
 * @property unicastRetransmissionsIntervalStep         SAR Unicast Retransmissions Interval Step:
 *                                                      (n + 1) * 25 ms.
 * @property unicastRetransmissionsIntervalIncrement    SAR Unicast Retransmissions Interval
 *                                                      Increment: (n + 1) * 25 ms.
 * @property multicastRetransmissionsCount              SAR Multicast Retransmissions Count.
 * @property multicastRetransmissionsIntervalStep       SAR Multicast Retransmissions Interval Step:
 *                                                      (n + 1) * 25 ms.
 */
data class SarTransmitterState(
    val segmentIntervalStep: UByte,
    val unicastRetransmissionsCount: UByte,
    val unicastRetransmissionsWithoutProgressCount: UByte,
    val unicastRetransmissionsIntervalStep: UByte,
    val unicastRetransmissionsIntervalIncrement: UByte,
    val multicastRetransmissionsCount: UByte,
    val multicastRetransmissionsIntervalStep: UByte
) {
    init {
        require(segmentIntervalStep <= 0x0Fu) { "Segment interval step must be in range 0..15" }
        require(unicastRetransmissionsCount <= 0x0Fu) {
            "Unicast retransmissions count must be in range 0..15"
        }
        require(unicastRetransmissionsWithoutProgressCount <= 0x0Fu) {
            "Unicast retransmissions without progress count must be in range 0..15"
        }
        require(unicastRetransmissionsIntervalStep <= 0x0Fu) {
            "Unicast retransmissions interval step must be in range 0..15"
        }
        require(unicastRetransmissionsIntervalIncrement <= 0x0Fu) {
            "Unicast retransmissions interval increment must be in range 0..15"
        }
        require(multicastRetransmissionsCount <= 0x0Fu) {
            "Multicast retransmissions count must be in range 0..15"
        }
        require(multicastRetransmissionsIntervalStep <= 0x0Fu) {
            "Multicast retransmissions interval step must be in range 0..15"
        }
    }

    /** Interval between segment transmissions. */
    val segmentTransmissionInterval: Duration
        get() = ((segmentIntervalStep.toInt() + 1) * 10).milliseconds

    /** Interval step used between unicast retransmissions. */
    val unicastRetransmissionsInterval: Duration
        get() = ((unicastRetransmissionsIntervalStep.toInt() + 1) * 25).milliseconds

    /** Interval increment used between unicast retransmissions. */
    val unicastRetransmissionsIncrement: Duration
        get() = ((unicastRetransmissionsIntervalIncrement.toInt() + 1) * 25).milliseconds

    /** Interval between multicast retransmissions. */
    val multicastRetransmissionsInterval: Duration
        get() = ((multicastRetransmissionsIntervalStep.toInt() + 1) * 25).milliseconds

    /**
     * Encodes the state to the 4 byte parameters. Upper nibble of the last byte is RFU.
     */
    fun encode(): ByteArray = byteArrayOf(
        (segmentIntervalStep.toInt() or (unicastRetransmissionsCount.toInt() shl 4)).toByte(),
        (unicastRetransmissionsWithoutProgressCount.toInt() or
                (unicastRetransmissionsIntervalStep.toInt() shl 4)).toByte(),
        (unicastRetransmissionsIntervalIncrement.toInt() or
                (multicastRetransmissionsCount.toInt() shl 4)).toByte(),
        multicastRetransmissionsIntervalStep.toByte()
    )

    companion object {
        /** Number of bytes of the encoded state. */
        const val LENGTH = 4

        /**
         * Decodes the state from the given parameters, returning null if the length is not 4.
         */
        fun decode(parameters: ByteArray?): SarTransmitterState? =
            parameters?.takeIf { it.size == LENGTH }?.let { p ->
                val b = IntArray(LENGTH) { p[it].toInt() and 0xFF }
                SarTransmitterState(
                    segmentIntervalStep = (b[0] and 0x0F).toUByte(),
                    unicastRetransmissionsCount = ((b[0] shr 4) and 0x0F).toUByte(),
                    unicastRetransmissionsWithoutProgressCount = (b[1] and 0x0F).toUByte(),
                    unicastRetransmissionsIntervalStep = ((b[1] shr 4) and 0x0F).toUByte(),
                    unicastRetransmissionsIntervalIncrement = (b[2] and 0x0F).toUByte(),
                    multicastRetransmissionsCount = ((b[2] shr 4) and 0x0F).toUByte(),
                    multicastRetransmissionsIntervalStep = (b[3] and 0x0F).toUByte()
                )
            }
    }
}
