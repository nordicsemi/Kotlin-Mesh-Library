package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The SAR Receiver states of a node as defined in the Bluetooth Mesh Protocol specification. All
 * values are the raw state values.
 *
 * @property segmentsThreshold                  SAR Segments Threshold (5 bits, 0..31).
 * @property acknowledgmentDelayIncrement        SAR Acknowledgment Delay Increment (3 bits, 0..7):
 *                                               n + 1.5 segment intervals.
 * @property discardTimeout                      SAR Discard Timeout (4 bits, 0..15): (n + 1) * 5 s.
 * @property receiverSegmentIntervalStep         SAR Receiver Segment Interval Step (4 bits, 0..15):
 *                                               (n + 1) * 10 ms.
 * @property acknowledgmentRetransmissionsCount  SAR Acknowledgment Retransmissions Count (2 bits,
 *                                               0..3).
 */
data class SarReceiverState(
    val segmentsThreshold: UByte,
    val acknowledgmentDelayIncrement: UByte,
    val discardTimeout: UByte,
    val receiverSegmentIntervalStep: UByte,
    val acknowledgmentRetransmissionsCount: UByte
) {
    init {
        require(segmentsThreshold <= 0x1Fu) { "Segments threshold must be in range 0..31" }
        require(acknowledgmentDelayIncrement <= 0x07u) {
            "Acknowledgment delay increment must be in range 0..7"
        }
        require(discardTimeout <= 0x0Fu) { "Discard timeout must be in range 0..15" }
        require(receiverSegmentIntervalStep <= 0x0Fu) {
            "Receiver segment interval step must be in range 0..15"
        }
        require(acknowledgmentRetransmissionsCount <= 0x03u) {
            "Acknowledgment retransmissions count must be in range 0..3"
        }
    }

    /** Time after which an incomplete segmented message is discarded. */
    val discardTimeoutDuration: Duration
        get() = ((discardTimeout.toInt() + 1) * 5).seconds

    /** Interval between received segments. */
    val segmentReceptionInterval: Duration
        get() = ((receiverSegmentIntervalStep.toInt() + 1) * 10).milliseconds

    /**
     * Encodes the state to the 3 byte parameters. Upper 6 bits of the last byte are RFU.
     */
    fun encode(): ByteArray = byteArrayOf(
        (segmentsThreshold.toInt() or (acknowledgmentDelayIncrement.toInt() shl 5)).toByte(),
        (discardTimeout.toInt() or (receiverSegmentIntervalStep.toInt() shl 4)).toByte(),
        acknowledgmentRetransmissionsCount.toByte()
    )

    companion object {
        /** Number of bytes of the encoded state. */
        const val LENGTH = 3

        /**
         * Decodes the state from the given parameters, returning null if the length is not 3.
         */
        fun decode(parameters: ByteArray?): SarReceiverState? =
            parameters?.takeIf { it.size == LENGTH }?.let { p ->
                val b = IntArray(LENGTH) { p[it].toInt() and 0xFF }
                SarReceiverState(
                    segmentsThreshold = (b[0] and 0x1F).toUByte(),
                    acknowledgmentDelayIncrement = ((b[0] shr 5) and 0x07).toUByte(),
                    discardTimeout = (b[1] and 0x0F).toUByte(),
                    receiverSegmentIntervalStep = ((b[1] shr 4) and 0x0F).toUByte(),
                    acknowledgmentRetransmissionsCount = (b[2] and 0x03).toUByte()
                )
            }
    }
}
