@file:Suppress("unused")
@file:OptIn(ExperimentalStdlibApi::class)

package no.nordicsemi.kotlin.mesh.core.layers.network

import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getUInt
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.hasBitSet
import no.nordicsemi.kotlin.data.shl
import no.nordicsemi.kotlin.data.shr
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.data.toHexString
import no.nordicsemi.kotlin.data.ushr
import no.nordicsemi.kotlin.mesh.bearer.PduType
import no.nordicsemi.kotlin.mesh.core.layers.lowertransport.LowerTransportPdu
import no.nordicsemi.kotlin.mesh.core.layers.lowertransport.LowerTransportPduType
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.NetworkKeyDerivatives
import no.nordicsemi.kotlin.mesh.crypto.Crypto
import java.nio.ByteOrder
import kotlin.experimental.and
import kotlin.experimental.or

/**
 * Defines a Network PDU received/sent by a node.
 *
 * @property pdu                             Raw PDU data.
 * @property key                             Network key used to decode/encode the PDU.
 * @property ivIndex                         IV Index used to decode/encode the PDU.
 * @property type                            PDU type.
 * @property ttl                             Time to live.
 * @property sequence                        Sequence number of the message.
 * @property source                          Source address of the message.
 * @property destination                     Destination address of the message.
 * @property transportPdu                    Transport protocol data unit that's guaranteed to have
 *                                           1 to 16 bytes.
 * @property ivi                             Raw data of the upper transport layer PDU.
 * @property nid                             Indicates if the message is a control message.
 * @property isSegmented                     Indicates if the message is segmented.
 * @property isSegmentAcknowledgementMessage Indicates if the message is a segment acknowledgement
 *                                           message.
 * @property sequenceZero                    SeqZero field of the message. The message must be
 *                                           either a Segment Access message, Segmented Control
 *                                           message or Segment Acknowledgement message otherwise
 *                                           null
 * @property messageSequence                 24-bit message sequence number used to transmit the
 *                                           first segment of a segmented message, or the 24-bit
 *                                           sequence number of an unsegmented message. This should
 *                                           be prefixed with the 32-bit IV Index to form the
 *                                           SeqAuth. If the Seq is 0x647262 and SeqZero is 0x1849,
 *                                           the message sequence is 0x6451849. See Bluetooth Mesh
 *                                           Profile 1.0.1 section 3.5.3.1
 * @constructor Creates a Network PDU.
 */
internal class NetworkPdu internal constructor(
    val pdu: ByteArray,
    val key: NetworkKey,
    val ivIndex: UInt,
    val ivi: Byte,
    val nid: Byte,
    val type: LowerTransportPduType,
    val ttl: UByte,
    val sequence: UInt,
    val source: MeshAddress,
    val destination: MeshAddress,
    val transportPdu: ByteArray,
) {
    val isSegmented: Boolean
        get() = transportPdu[0] hasBitSet 7 && transportPdu.size > 4

    private val isSegmentAcknowledgementMessage: Boolean
        get() = transportPdu[0] == 0x00.toByte() && transportPdu.size == 7

    val sequenceZero: UShort?
        get() = if (isSegmented || isSegmentAcknowledgementMessage) {
            ((transportPdu[1] and 0x7F).toUShort() shl 6) or (transportPdu[2] shr 2).toUShort()
        } else null

    private val messageSequence: UInt
        get() = if (isSegmented) {
            val sequenceZero = (transportPdu[1].toUShort() and 0x7Fu shl 6) or
                    (transportPdu[2].toUShort() shr 2)
            if ((sequence and 0x1FFFFu) < sequenceZero) {
                (sequence and 0xFFE000u) + sequenceZero.toUInt() - (0x1FFF + 1).toUInt()
            } else {
                (sequence and 0xFFE000u) + sequenceZero.toUInt()
            }
        } else sequence

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String {
        val micSize = type.netMicSize
        val encryptedDataSie = pdu.size - micSize - 9
        val encryptedData = pdu.copyOfRange(fromIndex = 9, toIndex = 9 + encryptedDataSie)
        val mic = pdu.copyOfRange(fromIndex = 9 + encryptedDataSie, pdu.size)
        return "NetworkPdu (ivi: $ivi, nid: ${
            nid.toHexString(
                format = HexFormat {
                    number.prefix = "0x"
                    upperCase = true
                }
            )
        }, ctl: ${type.rawValue}, ttl: $ttl, seq: $sequence, src: ${
            source.address.toHexString(
                format = HexFormat {
                    number.prefix = "0x"
                    upperCase = true
                }
            )
        }, dst: ${
            destination.address.toHexString(
                format = HexFormat {
                    number.prefix = "0x"
                    upperCase = true
                }
            )
        }, transportPdu: 0x${
            encryptedData.toHexString(format = HexFormat.UpperCase)
        }, netMic: 0x${
            mic.toHexString(format = HexFormat.UpperCase)
        })"
    }
}

/**
 * Helper object for encoding and decoding Network PDUs
 */
internal object NetworkPduDecoder {

    /**
     * Creates a Network PDU object from the received PDU. The initiator tries to de-obfuscate and
     * decrypt the data using given Network Key and IV Index.
     *
     * @param pdu           PDU received from the mesh network
     * @param pduType       Type of the PDU. This could be either a Network or a Proxy Configuration
     *                      PDU.
     * @param meshNetwork   Mesh network.
     * @return Network PDU or null if unable to decode the PDU.
     */
    fun decode(pdu: ByteArray, pduType: PduType, meshNetwork: MeshNetwork): NetworkPdu? {
        require(pduType == PduType.NETWORK_PDU || pduType == PduType.PROXY_CONFIGURATION) {
            return null
        }
        require(pdu.size >= 14) {
            return null
        }
        for (networkKey in meshNetwork.networkKeys) {
            decode(
                pdu = pdu,
                pduType = pduType,
                networkKey = networkKey,
                ivIndex = meshNetwork.ivIndex
            )?.let {
                return it
            }
        }
        return null
    }

    /**
     * Creates a Network PDU object from the received PDU. The initiator tries to de-obfuscate and
     * decrypt the data using given Network Key and IV Index.
     *
     * @param pdu           PDU received from the mesh network
     * @param pduType       Type of the PDU. This could be either a Network or a Proxy Configuration
     *                      PDU.
     * @param networkKey    Network key to be used to decrypt the PDU.
     * @param ivIndex       IV Index of the mesh network.
     * @return Network PDU or null if unable to decode the PDU.
     */
    private fun decode(
        pdu: ByteArray,
        pduType: PduType,
        networkKey: NetworkKey,
        ivIndex: IvIndex,
    ): NetworkPdu? {
        // The first byte is not obfuscated.
        val ivi: Byte = pdu[0] ushr 7
        val nid: Byte = pdu[0] and 0x7F
        val keySets = mutableListOf<NetworkKeyDerivatives>()
        // The NID must match.
        // If the Key Refresh procedure is in place, the received packet might have been encrypted
        // using an old key. We have to try both.
        networkKey.derivatives.takeIf { nid == it.nid }?.let { keySets += it }
        networkKey.oldDerivatives?.takeIf { nid == it.nid }?.let { keySets += it }
        require(keySets.isNotEmpty()) { return null }

        // IVI should match the LSB bit of current IV Index.
        // If it doesn't, the PDU will be deobfuscated and decoded with IV Index decremented by 1.
        // See: Bluetooth Mesh Profile 1.0.1 Specification, chapter: 3.10.5.
        val currentIvIndex = ivIndex.index(ivi = ivi)

        for (keys in keySets) {
            // 6 bytes following IVI
            val obfuscatedData = pdu.copyOfRange(fromIndex = 1, toIndex = 7)
            // 7 bytes of encrypted data
            val random = pdu.copyOfRange(fromIndex = 7, toIndex = 14)

            val deobfuscatedData = Crypto.obfuscate(
                data = obfuscatedData,
                random = random,
                ivIndex = currentIvIndex,
                privacyKey = keys.privacyKey
            )

            // First validation: Control messages have a NetMIC of size 64 bits.
            val ctl = deobfuscatedData[0] ushr 7
            val type = LowerTransportPduType.from(type = ctl)!!
            if (type != LowerTransportPduType.ACCESS_MESSAGE && pdu.size < 18) continue

            val ttl = (deobfuscatedData[0] and 0x7F).toUByte()

            // Multiple octet values use Big Endian.
            val sequence = deobfuscatedData.getUInt(
                offset = 1,
                format = IntFormat.UINT24,
                order = ByteOrder.BIG_ENDIAN
            )

            val src = deobfuscatedData.getUShort(offset = 4)

            val micOffset = pdu.size - type.netMicSize
            val destAndTransportPdu = pdu.copyOfRange(fromIndex = 7, toIndex = pdu.size)
            val mic = pdu.copyOfRange(fromIndex = micOffset, toIndex = pdu.size)

            val nonce = pduType.nonceId.toByteArray() +
                    deobfuscatedData +
                    byteArrayOf(0x00, 0x00) +
                    currentIvIndex.toByteArray()

            // Pad
            if (pduType == PduType.PROXY_CONFIGURATION) nonce[1] = 0x00

            try {
                val decryptedData = Crypto.decrypt(
                    data = destAndTransportPdu,
                    key = keys.encryptionKey,
                    nonce = nonce,
                    micSize = mic.size
                ) ?: continue

                val dst = decryptedData.getUShort(offset = 0)

                return NetworkPdu(
                    pdu = pdu,
                    key = networkKey,
                    ivIndex = currentIvIndex,
                    ivi = ivi,
                    nid = nid,
                    type = type,
                    ttl = ttl,
                    sequence = sequence,
                    source = MeshAddress.create(address = src),
                    destination = MeshAddress.create(address = dst),
                    transportPdu = decryptedData.copyOfRange(
                        fromIndex = 2, toIndex = decryptedData.size
                    )
                )
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    /**
     * Creates the Network PDU. This method encrypts and deobfuscates data that are to be send to
     * the mesh network.
     *
     * @param lowerTransportPdu  Network pdu to be decoded.
     * @param pduType            Pdu type.
     * @param sequence           Sequence number of the pdu.
     * @param ttl                TTL of the pdu.
     * @return De-obfuscated and decoded the Network Pdu, or null if the PDU was not signed with any
     *         of the Network Keys, the IV Index was not valid or the PDU was invalid.
     */
    fun encode(
        lowerTransportPdu: LowerTransportPdu,
        pduType: PduType,
        sequence: UInt,
        ttl: UByte,
    ): NetworkPdu {
        require(pduType == PduType.NETWORK_PDU || pduType == PduType.PROXY_CONFIGURATION) {
            throw IllegalArgumentException(
                "Only ${PduType.NETWORK_PDU} and ${PduType.PROXY_CONFIGURATION} can be encoded " +
                        "into a NetworkPdu"
            )
        }
        val networkKey = lowerTransportPdu.networkKey
        val keys = networkKey.transmitKeys
        val ivIndex = lowerTransportPdu.ivIndex
        val ivi = (ivIndex and 0x1u).toByte()
        val nid = keys.nid
        val type = lowerTransportPdu.type
        val source = lowerTransportPdu.source.address
        val destination = lowerTransportPdu.destination.address
        val transportPdu = lowerTransportPdu.transportPdu

        val iviNid = (ivi shl 7) or nid
        val ctlTtl = (type.rawValue shl 7) or ttl.toByte()

        val seq = sequence.toByteArray().copyOfRange(fromIndex = 1, toIndex = 4)
        val deobfuscatedData = byteArrayOf(ctlTtl) + seq + source.toByteArray()
        val decryptedData = destination.toByteArray() + transportPdu

        val nonce = byteArrayOf(pduType.nonceId.toByte()) +
                deobfuscatedData +
                byteArrayOf(0x00, 0x00) +
                ivIndex.toByteArray()
        // Pad
        if (pduType == PduType.PROXY_CONFIGURATION) nonce[1] = 0x00

        val encryptedData = Crypto.encrypt(
            data = decryptedData,
            key = keys.encryptionKey,
            nonce = nonce,
            micSize = type.netMicSize
        )
        val obfuscatedData = Crypto.obfuscate(
            data = deobfuscatedData,
            random = encryptedData,
            ivIndex = ivIndex,
            privacyKey = keys.privacyKey
        )
        val pdu = byteArrayOf(iviNid) + obfuscatedData + encryptedData
        return NetworkPdu(
            pdu = pdu,
            key = networkKey,
            ivIndex = ivIndex,
            ivi = ivi,
            nid = nid,
            type = type,
            ttl = ttl,
            sequence = sequence,
            source = lowerTransportPdu.source,
            destination = lowerTransportPdu.destination,
            transportPdu = transportPdu
        )
    }
}