@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package no.nordicsemi.kotlin.mesh.core.layers.lowertransport

import no.nordicsemi.kotlin.data.hasBitCleared
import no.nordicsemi.kotlin.data.hasBitSet
import no.nordicsemi.kotlin.data.toHexString
import no.nordicsemi.kotlin.mesh.core.exception.InvalidPdu
import no.nordicsemi.kotlin.mesh.core.layers.network.NetworkPdu
import no.nordicsemi.kotlin.mesh.core.layers.uppertransport.UpperTransportPdu
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import java.io.ByteArrayOutputStream
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * Data class defining an Access message.
 *
 * @property aid               6-bit Application key identifier. This field is set to null if the
 *                             message is signed with a Device Key instead.
 * @property sequence          Sequence number used to encode this message.
 * @property transportMicSize  Size of the transport MIC which is 4 or 8 bytes.
 * @constructor Creates an Access Message.
 */
internal open class AccessMessage(
    // Lower Transport PDU
    override val source: MeshAddress,
    override val destination: MeshAddress,
    override val networkKey: NetworkKey,
    override val ivIndex: UInt,
    override val upperTransportPdu: ByteArray,
    // Additional
    open val transportMicSize: UByte,
    open val sequence: UInt,
    open val aid: Byte? = null,
) : LowerTransportPdu {

    override val type = LowerTransportPduType.ACCESS_MESSAGE
    override val transportPdu: ByteArray
        get() {
            var octet0 = 0.toByte() // SEG = 0
            aid?.let {
                octet0 = octet0 or 0b01000000 // AKF = 1
                octet0 = octet0 or it
            }
            return byteArrayOf(octet0) + upperTransportPdu
        }

    /**
     * Creates an Access Message from the given Network PDU and Network Key.
     *
     * @param pdu          Network PDU containing the access message.
     * @param networkKey   Network key used to decode/encode the PDU.
     * @constructor Creates an Access Message.
     */
    internal constructor(
        pdu: UpperTransportPdu,
        networkKey: NetworkKey,
    ) : this(
        source = MeshAddress.create(pdu.source),
        destination = pdu.destination,
        networkKey = networkKey,
        ivIndex = pdu.ivIndex,
        upperTransportPdu = pdu.transportPdu,
        transportMicSize = pdu.transportMicSize,
        sequence = pdu.sequence,
        aid = pdu.aid
    )

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "$type (akf: ${
        aid?.let {
            "1, aid: ${
                it.toHexString(
                    format = HexFormat {
                        number.prefix = "0x"
                        upperCase = true
                    }
                )
            }"
        } ?: "0"
    }, szmic: ${if (transportMicSize == 4.toUByte()) 0 else 1}, " +
            "data: ${upperTransportPdu.toHexString(prefixOx = true)})"

    internal companion object {

        /**
         * Creates an Access Message from the given Network PDU.
         *
         * @param pdu Network PDU containing the access message.
         * @return an AccessMessage or null if the pdu is invalid.
         */
        fun init(pdu: NetworkPdu): AccessMessage {
            // Minimum length of an Access Message is 6 bytes:
            // * 1 byte for SEG | AKF | AID
            // * at least one byte of Upper Transport Layer payload
            // * 4 or 8 bytes of TransMIC
            require(pdu.transportPdu.size >= 6) { throw InvalidPdu() }

            // Make sure the SEG is 0, that is the message is unsegmented.
            require(pdu.transportPdu[0] hasBitCleared 7) { throw InvalidPdu() }

            val akf = pdu.transportPdu[0] hasBitSet 6
            val aid = if (akf) pdu.transportPdu[0] and 0x3F else null

            return AccessMessage(
                source = pdu.source,
                destination = pdu.destination,
                networkKey = pdu.key,
                ivIndex = pdu.ivIndex,
                upperTransportPdu = pdu.transportPdu.copyOfRange(1, pdu.transportPdu.size),
                // TransMIC is always 32-bits for unsegmented messages.
                transportMicSize = 4u,
                sequence = pdu.sequence,
                aid = aid
            )
        }

        /**
         * Creates an Access Message from the given list of Segmented Access Messages
         *
         * @param segments List of segmented access messages.
         * @return AccessMessage or null if the segments are invalid.
         */
        fun init(segments: List<SegmentedAccessMessage>): AccessMessage {
            require(segments.isNotEmpty()) { throw InvalidPdu() }
            val segment = segments.first()
            return AccessMessage(
                source = segment.source,
                destination = segment.destination,
                networkKey = segment.networkKey,
                ivIndex = segment.ivIndex,
                upperTransportPdu =  ByteArrayOutputStream().apply {
                    segments.forEach { write(it.upperTransportPdu) }
                }.toByteArray(),
                transportMicSize = segment.transportMicSize,
                sequence = segment.sequence,
                aid = segment.aid
            )
        }
    }
}
