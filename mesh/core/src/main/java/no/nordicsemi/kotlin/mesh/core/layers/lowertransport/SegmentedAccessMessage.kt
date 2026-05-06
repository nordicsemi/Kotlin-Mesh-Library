@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.layers.lowertransport

import no.nordicsemi.kotlin.data.hasBitSet
import no.nordicsemi.kotlin.data.shl
import no.nordicsemi.kotlin.data.shr
import no.nordicsemi.kotlin.data.ushr
import no.nordicsemi.kotlin.mesh.core.exception.InvalidPdu
import no.nordicsemi.kotlin.mesh.core.layers.network.NetworkPdu
import no.nordicsemi.kotlin.mesh.core.layers.uppertransport.UpperTransportPdu
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.min

/**
 * Data class defining a Segmented Access Message.
 *
 * @property aid                Application Key identifier.
 * @property sequence           Sequence number of the message.
 * @property transportMicSize   Size of the transport MIC which is 4 or 8 bytes.
 */
internal class SegmentedAccessMessage(
    // Access Message
    override val source: MeshAddress,
    override val destination: MeshAddress,
    override val networkKey: NetworkKey,
    override val ivIndex: UInt,
    override val upperTransportPdu: ByteArray,
    override val transportMicSize: UByte,
    override val sequence: UInt,
    override val aid: Byte? = null,
    // Segmented Message
    override val message: MeshMessage? = null,
    override val userInitialized: Boolean = false,
    override val sequenceZero: UShort,
    override val segmentOffset: UByte,
    override val lastSegmentNumber: UByte,
) : AccessMessage(
    source = source,
    destination = destination,
    networkKey = networkKey,
    ivIndex = ivIndex,
    upperTransportPdu = upperTransportPdu,
    transportMicSize = transportMicSize,
    sequence = sequence
), SegmentedMessage {

    override val transportPdu: ByteArray
        get() {
            var octet0 = 0x80.toByte() // SEG = 1
            aid?.let { aid ->
                octet0 = octet0 or 0b01000000 // AKF = 1
                octet0 = octet0 or aid
            }
            val octet1 = (transportMicSize and 0x08u shl 4).toByte() or // 8 -> 0x80, 4 -> 0x00
                    (sequenceZero shr 6).toByte()
            val octet2 = (sequenceZero shl 2).toByte() or
                    (segmentOffset shr 3).toByte()
            val octet3 = (segmentOffset and 0x07u shl 5).toByte() or
                    (lastSegmentNumber and 0x1Fu).toByte()
            return byteArrayOf(octet0, octet1, octet2, octet3) + upperTransportPdu
        }

    override val type = LowerTransportPduType.ACCESS_MESSAGE


    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "Segmented $type " +
            "(akf: ${
                aid?.let {
                    "1, aid: ${
                        it.toHexString(
                            format = HexFormat {
                                upperCase = true
                                number.prefix = "0x"
                            }
                        )
                    }"
                } ?: "0"
            }, " +
            "szmic: ${if (transportMicSize == 4.toUByte()) 0 else 1}, " +
            "seqZero: $sequenceZero, segO: $segmentOffset, segN: $lastSegmentNumber " +
            "data: 0x${upperTransportPdu.toHexString(format = HexFormat.UpperCase)})"

    internal companion object {

        /**
         * Creates a segment of an [AccessMessage] from a network from a given Network PDU that
         * contains a segmented access message. If the PDU is invalid the method will throw an
         * [InvalidPdu] exception.
         *
         * @param pdu Network pdu to be decoded.
         * @return SegmentedAccessMessage or null otherwise.
         * @throws InvalidPdu If the PDU is invalid.
         */
        fun init(pdu: NetworkPdu): SegmentedAccessMessage? {
            val data = pdu.transportPdu
            // Minimum length of a Access Message is 6 bytes:
            // * 1 byte for SEG | AKF | AID
            // * 3 bytes for SZMIC | SeqZero | SegO | SegN
            // * At least 1 byte of segment payload
            require(data.size >= 5) { return null }

            // Make sure the SEG is 0, that is the message is segmented.
            require(data[0] hasBitSet 7) { return null }

            val akf = data[0] hasBitSet 6
            val aid = if (akf) data[0] and 0x3F else null

            val szmic = data[1] hasBitSet 7
            val transportMicSize: UByte = if (szmic) 8u else 4u
            val sequenceZero = ((data[1] and 0x7F).toUShort() shl 6) or
                    (data[2] ushr 2).toUShort()
            val segmentOffset = ((data[2].toUByte() and 0x03u) shl 3) or
                    ((data[3].toUByte() and 0xE0u) shr 5)
            val lastSegmentNumber = data[3].toUByte() and 0x1Fu

            // Make sure SegO is less than or equal to SegN.
            require(segmentOffset <= lastSegmentNumber) { return null }

            val upperTransportPdu = data.copyOfRange(
                fromIndex = 4,
                toIndex = data.size
            )
            val sequence = (pdu.sequence and 0xFFE000u) or sequenceZero.toUInt()
            return SegmentedAccessMessage(
                source = pdu.source,
                destination = pdu.destination,
                networkKey = pdu.key,
                ivIndex = pdu.ivIndex,
                upperTransportPdu = upperTransportPdu,
                transportMicSize = transportMicSize,
                sequence = sequence,
                aid = aid,
                sequenceZero = sequenceZero,
                segmentOffset = segmentOffset,
                lastSegmentNumber = lastSegmentNumber,
            )
        }

        /**
         * Creates a SegmentedAccessMessage from the given Upper Transport PDU, network key and
         * offset.
         *
         * @param pdu        Upper transport PDU.
         * @param networkKey Network key to be used to encrypt the message.
         * @param offset     Offset of the segment.
         * @return SegmentedAccessMessage
         */
        fun init(
            pdu: UpperTransportPdu,
            networkKey: NetworkKey,
            offset: UByte,
        ): SegmentedAccessMessage {
            val lowerBound = offset.toInt() * 12
            val upperBound = min(a = pdu.transportPdu.size, b = (offset.toInt() + 1) * 12)
            val segment = pdu.transportPdu.copyOfRange(fromIndex = lowerBound, toIndex = upperBound)
            return SegmentedAccessMessage(
                message = pdu.message,
                aid = pdu.aid,
                source = MeshAddress.create(address = pdu.source),
                destination = pdu.destination,
                networkKey = networkKey,
                ivIndex = pdu.ivIndex,
                transportMicSize = pdu.transportMicSize,
                sequence = pdu.sequence,
                sequenceZero = (pdu.sequence and 0x1FFFu).toUShort(),
                segmentOffset = offset,
                lastSegmentNumber = (((pdu.transportPdu.size + 11) / 12) - 1).toUByte(),
                upperTransportPdu = segment,
                userInitialized = pdu.userInitiated,
            )
        }
    }
}
