@file:Suppress("unused", "ArrayInDataClass")

package no.nordicsemi.kotlin.mesh.bearer

import kotlin.experimental.and

/**
 * Defines the SAR type of the message being sent via the proxy protocol. The messages are segmented
 * based on the MTU size of the bearer.
 *
 * @property rawValue Raw value of the SAR type.
 */
private enum class SAR(private val rawValue: UByte) {
    COMPLETE_MESSAGE(rawValue = 0b00u),
    FIRST_SEGMENT(rawValue = 0b01u),
    CONTINUATION(rawValue = 0b10u),
    LAST_SEGMENT(rawValue = 0b11u);

    val value: UByte
        get() = (rawValue.toUInt() shl 6).toUByte()

    companion object {

        /**
         *  Returns the SAR type based on the pdu
         *
         *  @param data PDU data
         *  @return SAR type or null if the data is invalid.
         */
        fun from(data: ByteArray) = when ((data[0].toUByte().toInt() shr 6).toUByte()) {
            COMPLETE_MESSAGE.rawValue -> COMPLETE_MESSAGE
            FIRST_SEGMENT.rawValue -> FIRST_SEGMENT
            CONTINUATION.rawValue -> CONTINUATION
            LAST_SEGMENT.rawValue -> LAST_SEGMENT
            else -> null
        }
    }
}

/**
 * PDU type identifies the type of the message.
 *
 * @return PDU type or null if the data is invalid.
 */
private fun ByteArray.from(): PduType? {
    if (isEmpty())
        return null
    return when ((first() and 0b00111111).toUByte()) {
        0.toUByte() -> PduType.NETWORK_PDU
        1.toUByte() -> PduType.MESH_BEACON
        2.toUByte() -> PduType.PROXY_CONFIGURATION
        3.toUByte() -> PduType.PROVISIONING_PDU
        else -> null
    }
}

/**
 * Helper class for handling segmentation and reassembly of messages that are sent via the proxy
 * protocol. This is defined in Bluetooth Mesh Protocol Specification 1.1
 */
class ProxyProtocolHandler {

    private var buffer: ByteArray? = null
    private var bufferType: PduType? = null

    /**
     * Segments the given data with given message type to 1+ messages where all but the last one are
     * of the MTU size and the last one is MTU size or smaller. This method implements the Proxy
     * Protocol from Bluetooth Mesh specification.
     *
     * @param data  Data to be segmented.
     * @param type  Type of the message.
     * @param mtu   Maximum size of the message.
     * @return Array of segmented messages.
     */
    fun segment(data: ByteArray, type: PduType, mtu: Int) = if (data.size <= mtu - 1) {
        var singlePacket = byteArrayOf((SAR.COMPLETE_MESSAGE.value or type.value).toByte())
        singlePacket += data
        listOf(singlePacket)
    } else {
        val packets = mutableListOf<ByteArray>()
        for (i in data.indices step mtu - 1) {
            val sar = when {
                i == 0 -> SAR.FIRST_SEGMENT
                i + mtu - 1 > data.size -> SAR.LAST_SEGMENT
                else -> SAR.CONTINUATION
            }
            var singlePacket = byteArrayOf((sar.value or type.value).toByte())
            singlePacket += data.sliceArray(i until minOf(data.size, i + mtu - 1))
            packets += singlePacket
        }
        packets.toList()
    }

    /**
     * This method consumes the given data. If the data were segmented, they are buffered until the
     * last segment is received. This method returns the message and its type when the last segment
     * (or the only one) has been received, otherwise it returns `null`.
     *
     * The packets must be delivered in order. If a new message is received while the previous one is
     * still reassembled, the old one will be disregarded. Invalid messages are disregarded.
     *
     * @param data  The data received.
     * @return The message and its type, or `null`, if more data are expected.
     */
    fun reassemble(data: ByteArray): Pdu? {
        // Disregard invalid packet.
        require(data.isNotEmpty()) { return null }

        // Disregard invalid packet.
        val sar = requireNotNull(SAR.from(data)) { return null }

        // Disregard invalid packet.
        val messageType = requireNotNull(PduType.from(data)) { return null }

        // Ensure, that only complete message or the first segment may be processed if the buffer is
        // empty.
        require(buffer != null || sar == SAR.COMPLETE_MESSAGE || sar == SAR.FIRST_SEGMENT) {
            // Disregard invalid packet.
            return null
        }

        // If the packet is a continuation/lastSegment, it should have the same message type as the
        // current buffer.
        require(
            bufferType == null ||
                    bufferType == messageType ||
                    sar == SAR.COMPLETE_MESSAGE ||
                    sar == SAR.FIRST_SEGMENT
        ) {
            // Disregard invalid packet.
            return null
        }

        // If a new message was received while the old one was processed, disregard the old one.
        if (buffer != null && (sar == SAR.COMPLETE_MESSAGE || sar == SAR.FIRST_SEGMENT)) {
            buffer = null
            bufferType = null
        }

        // Save the message type and append newly received data.
        bufferType = messageType
        if (sar == SAR.COMPLETE_MESSAGE || sar == SAR.FIRST_SEGMENT) {
            buffer = byteArrayOf()
        }
        buffer = buffer!! + data.sliceArray(1 until data.size)

        // If the message is complete, return it.
        // else, just return null.
        return if (sar == SAR.COMPLETE_MESSAGE || sar == SAR.LAST_SEGMENT) {
            val tmp = buffer!!
            buffer = null
            bufferType = null
            Pdu(tmp, messageType)
        } else null
    }
}

/**
 * Pdu containing the pdu and the type of the message.
 *
 * @property data  Reassembled PDU.
 * @property type  Type of the message.
 */
data class Pdu(val data: ByteArray, val type: PduType) {
    override fun toString() = "Pdu(type: $type, " +
            "data: ${data.toHexString(format = HexFormat.UpperCase)})"
}