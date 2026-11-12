package no.nordicsemi.kotlin.mesh.core.layers.lowertransport

import no.nordicsemi.kotlin.mesh.core.layers.network.NetworkPdu
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SegmentedControlMessageTest {

    private fun networkPdu(vararg transportPdu: Int) = NetworkPdu(
        pdu = byteArrayOf(),
        key = NetworkKey(),
        ivIndex = 0u,
        ivi = 0,
        nid = 0,
        type = LowerTransportPduType.CONTROL_MESSAGE,
        ttl = 0u,
        sequence = 0u,
        source = UnicastAddress(0x0001u),
        destination = UnicastAddress(0x0002u),
        transportPdu = ByteArray(transportPdu.size) { transportPdu[it].toByte() }
    )

    @Test
    fun testSequenceZeroWhenOctet2HasMsbSet() {
        // SeqZero 0x1FFF, SegO 0, SegN 0
        val message = assertNotNull(
            SegmentedControlMessage.init(networkPdu(0x83, 0x7F, 0xFC, 0x00, 0xAA))
        )
        assertEquals(0x1FFFu.toUShort(), message.sequenceZero)
        assertEquals(0u.toUByte(), message.segmentOffset)
    }

    @Test
    fun testSequenceZeroAndSegmentOffsetWithAllBitsSet() {
        // SeqZero 0x1FFF, SegO 0x1F, SegN 0x1F
        val message = assertNotNull(
            SegmentedControlMessage.init(networkPdu(0x83, 0x7F, 0xFF, 0xFF, 0xAA))
        )
        assertEquals(0x1FFFu.toUShort(), message.sequenceZero)
        assertEquals(0x1Fu.toUByte(), message.segmentOffset)
        assertEquals(0x1Fu.toUByte(), message.lastSegmentNumber)
    }
}
