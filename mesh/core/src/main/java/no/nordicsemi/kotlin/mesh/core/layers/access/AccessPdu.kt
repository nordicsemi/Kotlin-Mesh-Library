@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package no.nordicsemi.kotlin.mesh.core.layers.access

import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getUInt
import no.nordicsemi.kotlin.data.hasBitCleared
import no.nordicsemi.kotlin.data.or
import no.nordicsemi.kotlin.data.shl
import no.nordicsemi.kotlin.mesh.core.layers.uppertransport.UpperTransportPdu
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessageSecurity
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress

/**
 * Defines the Access PDU
 *
 * @property message         Mesh message being sent of null, when the message was received.
 * @property userInitiated   Flag to determine if the message was user initiated.
 * @property source          Source address of the message.
 * @property destination     Destination address of the message.
 * @property opCode          Opcode of the message.
 * @property parameters      Parameters of the message.
 * @property accessPdu       Access Layer pdu that will be sent.
 * @property isSegmented     Flag to determine if the message is segmented.
 * @property segmentsCount   Number of packets for this PDU.
 */
internal data class AccessPdu(
    val message: MeshMessage?,
    val userInitiated: Boolean,
    val source: Address,
    val destination: MeshAddress,
    val opCode: UInt,
    val parameters: ByteArray,
    val accessPdu: ByteArray,
) {
    val isSegmented: Boolean
        get() = message?.let { accessPdu.size > 11 || it.isSegmented } ?: false

    val segmentsCount: Int
        get() = message?.let { msg ->
            if (!isSegmented) 1
            else when (msg.security) {
                MeshMessageSecurity.Low -> 1 + (accessPdu.size + 3) / 12
                MeshMessageSecurity.High -> 1 + (accessPdu.size + 7) / 12
            }
        } ?: 0

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "Access PDU (" +
            "opCode: ${
                opCode.toHexString(format = HexFormat {
                    number {
                        prefix = "0x"
                        minLength = 2
                        removeLeadingZeros = true
                        upperCase = true
                    }
                })
            }" + (
                parameters
                    .takeIf { it.isNotEmpty() }
                    ?.let { ", parameters: 0x${parameters.toHexString(HexFormat.UpperCase)})"}
                    ?: ")"
            )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AccessPdu

        if (message != other.message) return false
        if (userInitiated != other.userInitiated) return false
        if (source != other.source) return false
        if (destination != other.destination) return false
        if (opCode != other.opCode) return false
        if (!parameters.contentEquals(other.parameters)) return false
        if (!accessPdu.contentEquals(other.accessPdu)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = message.hashCode()
        result = 31 * result + userInitiated.hashCode()
        result = 31 * result + source.hashCode()
        result = 31 * result + destination.hashCode()
        result = 31 * result + opCode.hashCode()
        result = 31 * result + parameters.contentHashCode()
        result = 31 * result + accessPdu.contentHashCode()
        return result
    }

    internal companion object {

        /**
         * Constructs an AccessPDu using the given UpperTransportPdu.
         *
         * @param pdu UpperTransportPdu to decode.
         * @return an AccessPdu or null if the pdu could not be decoded.
         */
        fun init(pdu: UpperTransportPdu): AccessPdu? {
            // At least 1 octet is required.
            require(pdu.accessPdu.isNotEmpty()) { return null }
            val octet0 = pdu.accessPdu[0]

            // Opcode 0b01111111 is reserved for future use.
            require(octet0 != 0b01111111.toByte()) { return null }

            // 1-octet Opcodes.
            if (octet0 hasBitCleared 7) {
                return AccessPdu(
                    message = null,
                    userInitiated = false,
                    source = pdu.source,
                    destination = pdu.destination,
                    opCode = octet0.toUInt(),
                    parameters = pdu.accessPdu.copyOfRange(
                        fromIndex = 1, toIndex = pdu.accessPdu.size
                    ),
                    accessPdu = pdu.accessPdu
                )
            }

            // 2-Octet Opcodes.
            if (octet0 hasBitCleared 6) {
                // At least 2 octets are required.
                require(pdu.accessPdu.size >= 2) { return null }
                val octet1 = pdu.accessPdu[1]
                return AccessPdu(
                    message = null,
                    userInitiated = false,
                    source = pdu.source,
                    destination = pdu.destination,
                    opCode = (octet0.toUShort() shl 8 or octet1.toUShort()).toUInt() and 0x0000FFFFu,
                    parameters = pdu.accessPdu.copyOfRange(
                        fromIndex = 2, toIndex = pdu.accessPdu.size
                    ),
                    accessPdu = pdu.accessPdu
                )
            }

            // 3-Octet Opcodes.
            // At least 3 octets are required.
            require(pdu.accessPdu.size >= 3) { return null }

            return AccessPdu(
                message = null,
                userInitiated = false,
                source = pdu.source,
                destination = pdu.destination,
                opCode = pdu.accessPdu.getUInt(offset = 0, format = IntFormat.UINT24),
                parameters = pdu.accessPdu.copyOfRange(fromIndex = 3, toIndex = pdu.accessPdu.size),
                accessPdu = byteArrayOf()
            )
        }

        /**
         * Constructs an AccessPdu from a given MeshMessage.
         *
         * @param message         Mesh message to be encoded.
         * @param source          Source address of the message.
         * @param destination     Destination address of the message.
         * @param userInitiated   Flag to determine if the message was user initiated.
         * @return the decoded AccessPdu
         */
        @OptIn(ExperimentalStdlibApi::class)
        fun init(
            message: MeshMessage,
            source: Address,
            destination: MeshAddress,
            userInitiated: Boolean,
        ): AccessPdu {
            val opCode = message.opCode
            val parameters = message.parameters ?: byteArrayOf()
            return AccessPdu(
                message = message,
                userInitiated = userInitiated,
                source = source,
                destination = destination,
                opCode = opCode,
                parameters = parameters,
                accessPdu = when {
                    opCode == 0b01111111u ->
                        throw IllegalArgumentException("Opcode reserved for future use")

                    opCode < 0x80u -> byteArrayOf(opCode.toByte())

                    opCode and 0xFF_C0_00u == 0x00_80_00u ->
                        byteArrayOf((opCode shr 8).toByte() or 0x80, opCode.toByte())

                    opCode and 0xC0_00_00u == 0xC0_00_00u ->
                        byteArrayOf(
                            (opCode shr 16).toByte() or 0xC0,
                            (opCode shr 8).toByte(),
                            opCode.toByte()
                        )

                    else ->
                        throw IllegalArgumentException(
                            "Invalid opCode: ${
                                opCode.toHexString(
                                    format = HexFormat {
                                        number.prefix = "0x"
                                        upperCase = true
                                    }
                                )
                            }"
                        )
                } + parameters
            )
        }
    }
}