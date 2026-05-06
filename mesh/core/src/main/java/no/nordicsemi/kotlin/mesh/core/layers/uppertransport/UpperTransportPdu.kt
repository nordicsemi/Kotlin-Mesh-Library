package no.nordicsemi.kotlin.mesh.core.layers.uppertransport

import no.nordicsemi.kotlin.data.shl
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.layers.AccessKeySet
import no.nordicsemi.kotlin.mesh.core.layers.DeviceKeySet
import no.nordicsemi.kotlin.mesh.core.layers.KeySet
import no.nordicsemi.kotlin.mesh.core.layers.access.AccessPdu
import no.nordicsemi.kotlin.mesh.core.layers.lowertransport.AccessMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessageSecurity
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import no.nordicsemi.kotlin.mesh.core.model.boundTo
import no.nordicsemi.kotlin.mesh.crypto.Crypto
import kotlin.uuid.ExperimentalUuidApi

/**
 * UpperTransportPdu defines the credentials used to encrypt a message.
 *
 * @property source           Source address of the message.
 * @property destination      Destination address of the message.
 * @property aid              6-bit Application key identifier, or 'nil' if a Device Key was used.
 * @property transportMicSize Size of the transport MIC which is 4 or 8 bytes.
 * @property transportPdu     Raw data of the lower transport layer PDU.
 * @property accessPdu        Raw data of the upper transport layer PDU.
 * @property sequence         Sequence number used to encode this message.
 * @property ivIndex          IV Index used to encode this message.
 * @property message          Mesh message that was sent or null if the message was received.
 * @property userInitiated    Flag indicating whether the message was user initiated.
 * @constructor Creates an UpperTransportPdu.
 */
internal class UpperTransportPdu(
    val source: Address,
    val destination: MeshAddress,
    val aid: Byte?,
    val transportMicSize: UByte,
    val transportPdu: ByteArray,
    val accessPdu: ByteArray,
    val sequence: UInt,
    val ivIndex: UInt,
    val message: MeshMessage?,
    val userInitiated: Boolean,
) {

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString(): String {
        val micSize = transportMicSize.toInt()
        val encryptedDataSize = transportPdu.size - micSize
        val encryptedData = transportPdu.sliceArray(
            0 until encryptedDataSize
        )
        val mic = transportPdu.sliceArray(
            encryptedDataSize until encryptedDataSize + micSize
        )
        return "Upper transport PDU (encrypted data: " +
                "0x${encryptedData.toHexString(format = HexFormat.UpperCase)}, " +
                "transMIC: 0x${mic.toHexString(format = HexFormat.UpperCase)})"
    }

    internal companion object {

        /**
         * Constructs an UpperTransportPdu from a given AccessMessage.
         *
         * @param message       AccessMessage to be decoded from.
         * @param key           Key to be used for decryption.
         * @param virtualGroup  Virtual group address if the message is a virtual group message.
         * @return an UpperTransportPdu or null if the pdu could not be decoded.
         */
        @OptIn(ExperimentalUuidApi::class)
        fun init(message: AccessMessage, key: ByteArray, virtualGroup: Group?): UpperTransportPdu? {
            val micSize = message.transportMicSize.toInt()
            val encryptedData = message.upperTransportPdu

            // The nonce type is 0x01 for messages signed with Application Key and 0x02 for messages
            // signed with Device Key (Configuration Messages).
            val type = if (message.aid != null) 0x01 else 0x02
            // ASZMIC is set to 1 for messages sent with high security(64-bit TransMIC). This is
            // allowed only for Segmented Access Messages.
            val aszmic: Byte = if (micSize == 4) 0 else 1
            val seq = message.sequence.toByteArray().let {
                it.copyOfRange(fromIndex = 1, toIndex = it.size)
            }

            val nonce = byteArrayOf(type.toByte(), aszmic shl 7) + seq +
                    message.source.address.toByteArray() +
                    message.destination.address.toByteArray() +
                    message.ivIndex.toByteArray()

            val decryptedData = Crypto.decrypt(
                data = encryptedData,
                key = key,
                nonce = nonce,
                additionalData = (virtualGroup?.address as? VirtualAddress)?.uuid?.toByteArray(),
                micSize = micSize
            ) ?: return null

            return UpperTransportPdu(
                source = message.source.address,
                destination = virtualGroup?.address?.address?.let { address ->
                    MeshAddress.create(address = address)
                } ?: message.destination,
                aid = message.aid,
                transportMicSize = message.transportMicSize,
                transportPdu = message.upperTransportPdu,
                accessPdu = decryptedData,
                sequence = message.sequence,
                ivIndex = message.ivIndex,
                message = null,
                userInitiated = false
            )
        }

        /**
         * Constructs an UpperTransportPdu from a given AccessPdu.
         *
         * @param pdu           AccessPdu to be decode from.
         * @param keySet        KeySet to be used for encryption.
         * @param sequence      Sequence number of the message.
         * @param ivIndex       Current IV Index.
         * @return              an UpperTransportPdu.
         */
        @OptIn(ExperimentalUuidApi::class)
        fun init(
            pdu: AccessPdu,
            keySet: KeySet,
            sequence: UInt,
            ivIndex: IvIndex,
        ): UpperTransportPdu {
            val security = pdu.message!!.security
            // The nonce type is 0x01 for messages signed with Application Key and 0x02 for messages
            // signed using Device Key (Configuration Messages).
            val type: Byte = if (keySet.aid != null) 0x01 else 0x02
            // ASZMIC is set to 1 for messages that shall be sent with high security
            // (64-bit TransMIC). This is possible only for Segmented Access Messages.
            val aszmic: Byte = if (security == MeshMessageSecurity.High &&
                (pdu.accessPdu.size > 11 || pdu.isSegmented)
            ) 1 else 0

            val seq = sequence.toByteArray().let {
                it.copyOfRange(fromIndex = 1, toIndex = it.size)
            }

            val nonce = byteArrayOf(type, aszmic shl 7) + seq +
                    pdu.source.toByteArray() +
                    pdu.destination.address.toByteArray() +
                    ivIndex.index.toByteArray()
            val transportMicSize: UByte = if (aszmic > 0) 8u else 4u

            return UpperTransportPdu(
                source = pdu.source,
                destination = pdu.destination,
                aid = keySet.aid,
                transportMicSize = transportMicSize,
                transportPdu = Crypto.encrypt(
                    data = pdu.accessPdu,
                    key = keySet.accessKey,
                    nonce = nonce,
                    additionalData = (pdu.destination as? VirtualAddress)?.uuid?.toByteArray(),
                    micSize = transportMicSize.toInt()
                ),
                accessPdu = pdu.accessPdu,
                sequence = sequence,
                ivIndex = ivIndex.transmitIvIndex,
                message = pdu.message,
                userInitiated = pdu.userInitiated
            )
        }

        /**
         * Decodes the Access Message using a matching Application Key based on the 'aid' field
         * value, or the Device Key of hte local or source Node.
         *
         * @param message AccessMessage to be decoded from.
         * @param network Network to be used for encryption.
         * @return A pair containing the UpperTransportPdu and the KeySet used to encrypt the
         *         message or null if the pdu could not be decoded.
         */
        fun decode(
            message: AccessMessage,
            network: MeshNetwork,
        ): Pair<UpperTransportPdu, KeySet>? {
            // Was the message signed using Application Key?
            message.aid?.let { aid ->
                // When the message was sent to a Virtual Address, the message must be decoded with
                // the Virtual Label as Additional Data.
                val matchingGroups = if (message.destination is VirtualAddress) {
                    network.groups.filter { it.address == message.destination }
                } else {
                    // If the message was not sent to a Virtual Address, just add nil to the
                    // matching groups. That way it will be decoded once with group = nil.
                    listOf<Group?>(null)
                }

                // Go through all the application keys bound to the network key that the message was
                // decoded with.
                for (applicationKey in network.applicationKeys.boundTo(message.networkKey)) {
                    // The matchingGroups contains either a list of Virtual Groups, or a single nil
                    loop@ for (group in matchingGroups) {
                        // Each time try decoding using the new, or the old key (if such exist) when
                        // the generated aid matches the one sent in the message.
                        if (aid == applicationKey.aid) {
                            init(
                                message = message,
                                key = applicationKey.key,
                                virtualGroup = group
                            )?.let {
                                return Pair(
                                    first = it,
                                    second = AccessKeySet(applicationKey = applicationKey)
                                )
                            }
                        }
                        val oldAid = requireNotNull(applicationKey.oldAid) { continue@loop }
                        require(aid == oldAid) { continue@loop }
                        val key = requireNotNull(applicationKey.oldKey) { continue@loop }
                        init(
                            message = message,
                            key = key,
                            virtualGroup = group
                        )?.let { pdu ->
                            return Pair(pdu, AccessKeySet(applicationKey = applicationKey))
                        }
                    }
                }
            } ?: run {
                // Try decoding using source's Node Device Key. This should work if a status message
                // was sent as a response to a Config Message sent by this Provisioner.
                return decode(network = network, address = message.source, message = message) ?:
                // On the other hand, if another Provisioner is sending a Config Messages, they will
                // be signed using the target node Device Key instead.
                decode(network = network, address = message.destination, message = message)
            }
            return null
        }

        /**
         * Decodes a given access message.
         *
         * @param network Mesh network.
         * @param address Mesh address.
         * @param message Access message.
         * @return a Pair containing UpperTransportPdu and the KeySet used to decode.
         */
        private fun decode(
            network: MeshNetwork,
            address: MeshAddress,
            message: AccessMessage,
        ): Pair<UpperTransportPdu, KeySet>? {
            val node = network.node(address = address) ?: return null
            val deviceKey = node.deviceKey ?: return null
            val pdu = init(message = message, key = deviceKey, virtualGroup = null) ?: return null
            val keySet = DeviceKeySet.init(networkKey = message.networkKey, node = node)
                ?: return null
            return Pair(pdu, keySet)
        }
    }
}