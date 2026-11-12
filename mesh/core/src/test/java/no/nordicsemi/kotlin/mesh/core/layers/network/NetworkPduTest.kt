package no.nordicsemi.kotlin.mesh.core.layers.network

import no.nordicsemi.kotlin.mesh.core.layers.lowertransport.LowerTransportPduType
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NetworkPduTest {

    private fun networkPdu(sequence: UInt, vararg transportPdu: Int) = NetworkPdu(
        pdu = byteArrayOf(),
        key = NetworkKey(),
        ivIndex = 0u,
        ivi = 0,
        nid = 0,
        type = LowerTransportPduType.ACCESS_MESSAGE,
        ttl = 0u,
        sequence = sequence,
        source = UnicastAddress(0x0001u),
        destination = UnicastAddress(0x0002u),
        transportPdu = ByteArray(transportPdu.size) { transportPdu[it].toByte() }
    )

    /** Segmented access message with SeqZero 0x1FFF, so octet 2 has bit 7 set (0xFC). */
    @Test
    fun testSequenceZeroWhenOctet2HasMsbSet() {
        val pdu = networkPdu(0u, 0x80, 0x7F, 0xFC, 0x00, 0xAA)
        assertEquals(0x1FFFu.toUShort(), pdu.sequenceZero)
    }

    @Test
    fun testSequenceZeroWhenOctet2HasMsbCleared() {
        // SeqZero 0x1849 -> octet 1 = 0x61, octet 2 = 0x24
        val pdu = networkPdu(0u, 0x80, 0x61, 0x24, 0x00, 0xAA)
        assertEquals(0x1849u.toUShort(), pdu.sequenceZero)
    }

    @Test
    fun testSequenceZeroIsNullForUnsegmentedMessage() {
        assertNull(networkPdu(0u, 0x00, 0x7F, 0xFC, 0x00).sequenceZero)
    }

    @Test
    fun testMessageSequenceWithoutWrap() {
        // SeqZero 0x187F (octet 2 = 0xFC), seq & 0x1FFF = 0x1900 >= SeqZero
        val pdu = networkPdu(0x649900u, 0x80, 0x61, 0xFC, 0x00, 0xAA)
        assertEquals(0x64987Fu, pdu.messageSequence)
    }

    @Test
    fun testMessageSequenceWithWrap() {
        // seq & 0x1FFF = 0x1262 < SeqZero 0x187F, so the first segment used the previous block
        val pdu = networkPdu(0x647262u, 0x80, 0x61, 0xFC, 0x00, 0xAA)
        assertEquals(0x64587Fu, pdu.messageSequence)
    }

    @Test
    fun testMessageSequenceOfUnsegmentedMessageIsSequence() {
        assertEquals(0x647262u, networkPdu(0x647262u, 0x00, 0x7F, 0xFC, 0x00).messageSequence)
    }
}
