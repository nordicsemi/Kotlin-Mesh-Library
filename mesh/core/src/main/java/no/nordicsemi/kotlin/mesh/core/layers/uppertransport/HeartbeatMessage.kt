@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package no.nordicsemi.kotlin.mesh.core.layers.uppertransport

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.exception.InvalidPdu
import no.nordicsemi.kotlin.mesh.core.layers.lowertransport.ControlMessage
import no.nordicsemi.kotlin.mesh.core.model.Features
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatDestination
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatPublication
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.data.ByteOrder

/**
 * Defines a HeartbeatMessage
 *
 * @property source          Source address of the message.
 * @property destination     Destination address of the message.
 * @property ivIndex         IV Index of the message.
 * @property initialTtl      Initial TTL value of the message.
 * @property features        Currently active features of the Node.
 * @property receivedTtl     Received TTL value of the message, or null for outgoing messages.
 * @property ivIndex         IV Index of the message.
 * @property transportPdu    Transport PDU of the message.
 * @property hops            Number of hops the message has taken.
 * @constructor Creates a Heartbeat message.
 */
internal class HeartbeatMessage(
    val source: MeshAddress,
    val destination: MeshAddress,
    val ivIndex: UInt,
    val transportPdu: ByteArray,
    val initialTtl: UByte,
    val receivedTtl: UByte?,
    val features: Features,
) {
    val hops: UByte
        get() = receivedTtl?.let {
                    (initialTtl + 1u - it).toUByte()
                } ?: 0u

    override fun toString() = receivedTtl?.let { receivedTtl ->
        "Heartbeat Message (initial TTL: $initialTtl, received TTL: $receivedTtl, " +
        "hops: $hops, features: $features)"
    } ?: "Heartbeat Message (initial TTL: $initialTtl, features: $features)"

    internal companion object {
        val OP_CODE: UByte = 0x0Au

        /**
         * Constructs a HeartbeatMessage from the given ControlMessage.
         *
         * @param message ControlMessage containing the HeartbeatMessage.
         * @return HeartbeatMessage or null if the message is invalid.
         */
        fun init(message: ControlMessage): HeartbeatMessage {
            // Heartbeat message must have the opcode 0x0A.
            require(message.opCode == OP_CODE) { throw InvalidPdu() } // TODO Change exception?

            // Required fields are:
            // * 1 byte for Initial TTL,
            // * 2 bytes for Features
            require(message.upperTransportPdu.size == 3) { throw InvalidPdu() } // TODO Change exception?

            return HeartbeatMessage(
                source = message.source,
                destination = message.destination,
                ivIndex = message.ivIndex,
                transportPdu = message.upperTransportPdu,
                // TTL is 7 bits long, the MSB bit is RFU and must be 0.
                initialTtl = message.upperTransportPdu[0].toUByte() and 0x7Fu,
                receivedTtl = message.ttl,
                features = Features(rawValue = message.upperTransportPdu.getUShort(1)),
            )
        }

        /**
         * Constructs a HeartbeatMessage from the given HeartbeatPublication.
         *
         * @param heartbeatPublication         HeartbeatPublication containing the HeartbeatMessage.
         * @param source                       Source address of the HeartbeatMessage.
         * @param destination                  Destination address of the HeartbeatMessage.
         * @param ivIndex                      Current IV Index.
         * @return HeartbeatMessage or null if the message is invalid.
         */
        fun init(
            heartbeatPublication: HeartbeatPublication,
            source: MeshAddress,
            destination: HeartbeatDestination,
            ivIndex: IvIndex
        ): HeartbeatMessage {
            val features = Features(0u) // No features are supported, so always 0
            return HeartbeatMessage(
                source = source,
                destination = destination as MeshAddress,
                ivIndex = ivIndex.index,
                transportPdu = heartbeatPublication.ttl.toByteArray() +
                               features.rawValue.toByteArray(ByteOrder.BIG_ENDIAN),
                // TTL is 7 bits long, the MSB bit is RFU and must be 0.
                initialTtl = heartbeatPublication.ttl and 0x7Fu,
                receivedTtl = null,
                features = features,
            )
        }
    }
}