@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.layers.lowertransport

import no.nordicsemi.kotlin.data.hasBitCleared
import no.nordicsemi.kotlin.mesh.core.exception.InvalidPdu
import no.nordicsemi.kotlin.mesh.core.layers.network.NetworkPdu
import no.nordicsemi.kotlin.mesh.core.layers.uppertransport.HeartbeatMessage
import no.nordicsemi.kotlin.mesh.core.messages.proxy.ProxyConfigurationMessage
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.UnassignedAddress

/**
 * Data class defining a Control Message.
 *
 * @property opCode  Message Op Code.
 * @constructor Creates a Control Message.
 */
internal open class ControlMessage(
    // Control Message
    open val opCode: UByte,
    // Lower Transport PDU
    override val source: MeshAddress,
    override val destination: MeshAddress,
    override val networkKey: NetworkKey,
    override val ivIndex: UInt,
    override val upperTransportPdu: ByteArray,
    // Received TTL, used only for received Heartbeat messages
    val ttl: UByte = 0u,
) : LowerTransportPdu {

    override val transportPdu: ByteArray
        get() = byteArrayOf() + opCode.toByte() + upperTransportPdu

    override val type = LowerTransportPduType.CONTROL_MESSAGE

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String = "ControlMessage(" +
            "opCode: ${
                opCode.toHexString(
                    format = HexFormat {
                        number.prefix = "0x"
                        upperCase = true
                    }
                )
            }, " +
            "source: $source, destination: $destination, " +
            "networkKey: ${networkKey.name}, ivIndex: $ivIndex, ttl: $ttl)"

    /**
     * Creates a Control Message.
     *
     * @param heartbeatMessage   Heartbeat message.
     * @param networkKey         Network key.
     * @constructor Creates a Control Message.
     */
    constructor(
        heartbeatMessage: HeartbeatMessage,
        networkKey: NetworkKey,
    ) : this(
        opCode = HeartbeatMessage.OP_CODE,
        upperTransportPdu = heartbeatMessage.transportPdu,
        source = heartbeatMessage.source,
        destination = heartbeatMessage.destination,
        networkKey = networkKey,
        ivIndex = heartbeatMessage.ivIndex,
    )

    internal companion object {

        /**
         * Creates a Control Message using the given NetworkPdu.
         *
         * @param pdu The network pdu to be decoded.
         * @return ControlMessage or null if the pdu could not be decoded.
         */
        fun init(pdu: NetworkPdu): ControlMessage {
            // Minimum length of a Control Message is 2 bytes:
            // * 1 byte for SEG | AKF | AID
            // * at least one byte of Upper Transport Control PDU
            require(pdu.transportPdu.size >= 2) { throw InvalidPdu() }

            // Make sure the SEG is 0, that is the message is unsegmented.
            require(pdu.transportPdu[0] hasBitCleared 7) { throw InvalidPdu() }

            return ControlMessage(
                opCode = pdu.transportPdu[0].toUByte() and 0x7Fu,
                upperTransportPdu = pdu.transportPdu.drop(1).toByteArray(),
                source = pdu.source,
                destination = pdu.destination,
                networkKey = pdu.key,
                ivIndex = pdu.ivIndex,
                ttl = pdu.ttl,
            )
        }

        /**
         * Creates a Control Message from a given array of message segments.
         *
         * @param segments List of [SegmentedControlMessage] to be decoded.
         * @return a ControlMessage.
         */
        fun init(segments: List<SegmentedControlMessage>): ControlMessage {
            require(segments.isNotEmpty()) { throw InvalidPdu() }

            val pdu = segments.fold(byteArrayOf()) { acc, seg -> acc + seg.upperTransportPdu }
            return segments.first().run {
                ControlMessage(
                    opCode = opCode,
                    upperTransportPdu = pdu,
                    source = source,
                    destination = destination,
                    networkKey = networkKey,
                    ivIndex = ivIndex,
                )
            }
        }

        /**
         * Creates a Control Message from the given Proxy configuration message. The source should be
         * set to the local Node address. The given Network Key should be known to the Proxy node.
         *
         * @param message      Proxy configuration message.
         * @param source       Source address of the message.
         * @param networkKey   Network key to be used to encrypt the message.
         * @param ivIndex      Current IV Index of the network.
         *
         * @return decoded ControlMessage.
         */
        fun init(
            message: ProxyConfigurationMessage,
            source: MeshAddress,
            networkKey: NetworkKey,
            ivIndex: IvIndex,
        ) = ControlMessage(
            opCode = message.opCode,
            upperTransportPdu = message.parameters ?: byteArrayOf(),
            source = source,
            destination = UnassignedAddress,
            networkKey = networkKey,
            ivIndex = ivIndex.transmitIvIndex,
        )
    }
}

