package no.nordicsemi.kotlin.mesh.core.layers.access

import no.nordicsemi.kotlin.mesh.core.layers.uppertransport.UpperTransportPdu
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnOffGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnOffSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AccessPduTest {

    private val source = UnicastAddress(0x0001u)
    private val destination = UnicastAddress(0x0002u)

    private fun upperTransportPdu(accessPdu: ByteArray) = UpperTransportPdu(
        source = source.address,
        destination = destination,
        aid = null,
        transportMicSize = 4u,
        transportPdu = byteArrayOf(),
        accessPdu = accessPdu,
        sequence = 0u,
        ivIndex = 0u,
        message = null,
        userInitiated = false
    )

    private fun decode(vararg bytes: Int) =
        AccessPdu.init(pdu = upperTransportPdu(ByteArray(bytes.size) { bytes[it].toByte() }))

    @Test
    fun testDecodeOneOctetOpCode() {
        val pdu = assertNotNull(decode(0x00, 0xAA, 0xBB))
        assertEquals(0x00u, pdu.opCode)
        assertContentEquals(byteArrayOf(0xAA.toByte(), 0xBB.toByte()), pdu.parameters)
    }

    @Test
    fun testDecodeTwoOctetOpCodeIsBigEndian() {
        val pdu = assertNotNull(decode(0x82, 0x01))
        assertEquals(0x8201u, pdu.opCode)
        assertContentEquals(byteArrayOf(), pdu.parameters)
    }

    @Test
    fun testDecodeTwoOctetOpCodeWithParameters() {
        val pdu = assertNotNull(decode(0x82, 0x03, 0x01, 0x07))
        assertEquals(0x8203u, pdu.opCode)
        assertContentEquals(byteArrayOf(0x01, 0x07), pdu.parameters)
    }

    @Test
    fun testDecodeThreeOctetOpCode() {
        val pdu = assertNotNull(decode(0xC0, 0x59, 0x00, 0x11))
        assertEquals(0xC05900u, pdu.opCode)
        assertContentEquals(byteArrayOf(0x11), pdu.parameters)
    }

    @Test
    fun testDecodeFailsForEmptyPdu() {
        assertNull(decode())
    }

    @Test
    fun testDecodeFailsForReservedOpCode() {
        assertNull(decode(0x7F))
    }

    @Test
    fun testDecodeFailsForTruncatedTwoOctetOpCode() {
        assertNull(decode(0x82))
    }

    @Test
    fun testDecodeFailsForTruncatedThreeOctetOpCode() {
        assertNull(decode(0xC0, 0x59))
    }

    @Test
    fun testEncodeTwoOctetOpCode() {
        val pdu = AccessPdu.init(
            message = GenericOnOffGet(),
            source = source.address,
            destination = destination,
            userInitiated = true
        )
        assertEquals(0x8201u, pdu.opCode)
        assertContentEquals(byteArrayOf(0x82.toByte(), 0x01), pdu.accessPdu)
    }

    @Test
    fun testEncodeTwoOctetOpCodeWithParameters() {
        val pdu = AccessPdu.init(
            message = GenericOnOffSetUnacknowledged(tid = 5u, on = true),
            source = source.address,
            destination = destination,
            userInitiated = true
        )
        assertContentEquals(
            byteArrayOf(0x82.toByte(), 0x03, 0x01, 0x05),
            pdu.accessPdu
        )
    }

    @Test
    fun testEncodedPduDecodesToSameOpCodeAndParameters() {
        val encoded = AccessPdu.init(
            message = GenericOnOffSetUnacknowledged(tid = 5u, on = true),
            source = source.address,
            destination = destination,
            userInitiated = true
        )
        val decoded = assertNotNull(AccessPdu.init(pdu = upperTransportPdu(encoded.accessPdu)))
        assertEquals(encoded.opCode, decoded.opCode)
        assertContentEquals(encoded.parameters, decoded.parameters)
    }
}
