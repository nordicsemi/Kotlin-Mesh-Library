package no.nordicsemi.android.nrfmesh.core.data.bearer.sniffer

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import no.nordicsemi.kotlin.mesh.bearer.Pdu
import no.nordicsemi.kotlin.mesh.bearer.PduType
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class SnifferTest {

    private class Collector : SnifferPduSink {
        val pdus = mutableListOf<SnifferPdu>()
        override fun offer(pdu: SnifferPdu) {
            pdus += pdu
        }
    }

    @Test
    fun `PduType is mapped explicitly to the Proxy PDU type`() {
        assertEquals(0, SnifferPdu.proxyType(PduType.NETWORK_PDU))
        assertEquals(1, SnifferPdu.proxyType(PduType.MESH_BEACON))
        assertEquals(2, SnifferPdu.proxyType(PduType.PROXY_CONFIGURATION))
        assertEquals(3, SnifferPdu.proxyType(PduType.PROVISIONING_PDU))
    }

    @Test
    fun `sent PDU has no address`() {
        val sink = Collector()
        val observer = SnifferPduObserver(sink, SnifferBearer.GATT, { "AA:BB" }, { "Node" }, { 42L })
        observer.onPduSent(byteArrayOf(1, 2), PduType.PROXY_CONFIGURATION)
        val values = sink.pdus.single().toValues()
        assertEquals(42L, values["timestamp"])
        assertContentEquals(byteArrayOf(1, 2), values["data"] as ByteArray)
        assertEquals(2, values["type"])
        assertEquals(1, values["direction"])
        assertEquals(0, values["bearer"])
        assertEquals("Node", values["name"])
        assertFalse(values.containsKey("address"))
    }

    @Test
    fun `received PDU has address and the bearer`() {
        val sink = Collector()
        val observer = SnifferPduObserver(sink, SnifferBearer.PB_GATT, { "AA:BB" }, { null }, { 7L })
        observer.onPduReceived(Pdu(byteArrayOf(9), PduType.PROVISIONING_PDU))
        val values = sink.pdus.single().toValues()
        assertEquals(3, values["type"])
        assertEquals(0, values["direction"])
        assertEquals(1, values["bearer"])
        assertEquals("AA:BB", values["address"])
        assertFalse(values.containsKey("name"))
    }

    @Test
    fun `timestamp is taken at the call and data is copied`() {
        val sink = Collector()
        var time = 100L
        val observer = SnifferPduObserver(sink, SnifferBearer.GATT, { null }, { null }, { time })
        val data = byteArrayOf(1)
        observer.onPduSent(data, PduType.NETWORK_PDU)
        time = 200L
        data[0] = 5
        val pdu = sink.pdus.single()
        assertEquals(100L, pdu.timestamp)
        assertEquals(1, pdu.data[0].toInt())
        assertNull(pdu.address)
    }

    @Test
    fun `observer does not throw when the sink throws`() {
        val observer = SnifferPduObserver(
            { error("boom") }, SnifferBearer.GATT, { null }, { null }, { 0L }
        )
        observer.onPduSent(byteArrayOf(), PduType.NETWORK_PDU)
        observer.onPduReceived(Pdu(byteArrayOf(), PduType.NETWORK_PDU))
    }

    private fun pdu(i: Int) = SnifferPdu(
        i.toLong(), byteArrayOf(i.toByte()), 0,
        SnifferDirection.SENT, SnifferBearer.GATT, null, null
    )

    @Test
    fun `feed delivers in order and survives a failing transport`() = runTest {
        val delivered = mutableListOf<Long>()
        val feed = SnifferFeed(
            transport = {
                if (it.timestamp == 1L) error("provider died")
                delivered += it.timestamp
            },
            scope = this.backgroundScope
        )
        (0L..4L).forEach { feed.offer(pdu(it.toInt())) }
        runCurrent()
        assertEquals(listOf(0L, 2L, 3L, 4L), delivered)
    }

    @Test
    fun `feed drops the oldest when full and never blocks`() = runTest {
        val delivered = mutableListOf<Long>()
        val feed = SnifferFeed({ delivered += it.timestamp }, backgroundScope, capacity = 3)
        // Consumer has not run yet, so the queue overflows.
        (0..9).forEach { feed.offer(pdu(it)) }
        runCurrent()
        assertEquals(listOf(7L, 8L, 9L), delivered)
    }
}
