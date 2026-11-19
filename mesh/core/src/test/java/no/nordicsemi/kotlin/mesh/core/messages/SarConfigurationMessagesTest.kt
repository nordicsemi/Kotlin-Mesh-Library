package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.SarReceiverGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.SarReceiverSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.SarReceiverState
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.SarReceiverStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.SarTransmitterGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.SarTransmitterSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.SarTransmitterState
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.SarTransmitterStatus
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class SarConfigurationMessagesTest {

    private val tx = SarTransmitterState(
        segmentIntervalStep = 0x1u,
        unicastRetransmissionsCount = 0x2u,
        unicastRetransmissionsWithoutProgressCount = 0x3u,
        unicastRetransmissionsIntervalStep = 0x4u,
        unicastRetransmissionsIntervalIncrement = 0x5u,
        multicastRetransmissionsCount = 0x6u,
        multicastRetransmissionsIntervalStep = 0x7u
    )
    private val txBytes = byteArrayOf(0x21, 0x43, 0x65, 0x07)

    private val rx = SarReceiverState(
        segmentsThreshold = 0x03u,
        acknowledgmentDelayIncrement = 0x01u,
        discardTimeout = 0x0Au,
        receiverSegmentIntervalStep = 0x05u,
        acknowledgmentRetransmissionsCount = 0x02u
    )
    private val rxBytes = byteArrayOf(0x23, 0x5A, 0x02)

    @Test
    fun testOpCodes() {
        assertEquals(0x806Cu, SarTransmitterGet.opCode)
        assertEquals(0x806Du, SarTransmitterSet.opCode)
        assertEquals(0x806Eu, SarTransmitterStatus.opCode)
        assertEquals(0x806Fu, SarReceiverGet.opCode)
        assertEquals(0x8070u, SarReceiverSet.opCode)
        assertEquals(0x8071u, SarReceiverStatus.opCode)
    }

    @Test
    fun testResponseOpCodes() {
        assertEquals(SarTransmitterStatus.opCode, SarTransmitterGet().responseOpCode)
        assertEquals(SarTransmitterStatus.opCode, SarTransmitterSet(tx).responseOpCode)
        assertEquals(SarReceiverStatus.opCode, SarReceiverGet().responseOpCode)
        assertEquals(SarReceiverStatus.opCode, SarReceiverSet(rx).responseOpCode)
    }

    @Test
    fun testTransmitterEncoding() {
        assertContentEquals(txBytes, tx.encode())
        assertContentEquals(txBytes, SarTransmitterSet(tx).parameters)
        assertContentEquals(txBytes, SarTransmitterStatus(tx).parameters)
        assertNull(SarTransmitterGet().parameters)
    }

    @Test
    fun testTransmitterBoundaryEncoding() {
        val zero = SarTransmitterState(0u, 0u, 0u, 0u, 0u, 0u, 0u)
        assertContentEquals(byteArrayOf(0, 0, 0, 0), zero.encode())
        val max = SarTransmitterState(15u, 15u, 15u, 15u, 15u, 15u, 15u)
        assertContentEquals(
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x0F),
            max.encode()
        )
        assertEquals(max, SarTransmitterState.decode(max.encode()))
    }

    @Test
    fun testTransmitterDecodingIgnoresRfu() {
        val state = SarTransmitterState.decode(byteArrayOf(0x21, 0x43, 0x65, 0xF7.toByte()))
        assertEquals(tx, state)
    }

    @Test
    fun testTransmitterOutOfRangeRejected() {
        assertFailsWith<IllegalArgumentException> {
            SarTransmitterState(16u, 0u, 0u, 0u, 0u, 0u, 0u)
        }
        assertFailsWith<IllegalArgumentException> {
            SarTransmitterState(0u, 0u, 0u, 0u, 0u, 0u, 16u)
        }
    }

    @Test
    fun testTransmitterIntervals() {
        assertEquals(20.milliseconds, tx.segmentTransmissionInterval)
        assertEquals(125.milliseconds, tx.unicastRetransmissionsInterval)
        assertEquals(150.milliseconds, tx.unicastRetransmissionsIncrement)
        assertEquals(200.milliseconds, tx.multicastRetransmissionsInterval)
    }

    @Test
    fun testTransmitterDecoding() {
        val set = SarTransmitterSet.init(txBytes)
        assertNotNull(set)
        assertEquals(tx, set.state)
        val status = SarTransmitterStatus.init(txBytes)
        assertNotNull(status)
        assertEquals(tx, status.state)
        assertNotNull(SarTransmitterGet.init(byteArrayOf()))
    }

    @Test
    fun testTransmitterInvalidDecoding() {
        assertNull(SarTransmitterSet.init(null))
        assertNull(SarTransmitterSet.init(byteArrayOf(0, 0, 0)))
        assertNull(SarTransmitterSet.init(byteArrayOf(0, 0, 0, 0, 0)))
        assertNull(SarTransmitterStatus.init(null))
        assertNull(SarTransmitterStatus.init(byteArrayOf(0, 0, 0)))
        assertNull(SarTransmitterStatus.init(byteArrayOf(0, 0, 0, 0, 0)))
        assertNull(SarTransmitterGet.init(null))
        assertNull(SarTransmitterGet.init(byteArrayOf(0)))
    }

    @Test
    fun testTransmitterRoundTrip() {
        val decoded = SarTransmitterSet.init(SarTransmitterSet(tx).parameters)
        assertNotNull(decoded)
        assertEquals(tx, decoded.state)
        val status = SarTransmitterStatus.init(SarTransmitterStatus(tx).parameters)
        assertNotNull(status)
        assertEquals(tx, status.state)
    }

    @Test
    fun testReceiverEncoding() {
        assertContentEquals(rxBytes, rx.encode())
        assertContentEquals(rxBytes, SarReceiverSet(rx).parameters)
        assertContentEquals(rxBytes, SarReceiverStatus(rx).parameters)
        assertNull(SarReceiverGet().parameters)
    }

    @Test
    fun testReceiverBoundaryEncoding() {
        val zero = SarReceiverState(0u, 0u, 0u, 0u, 0u)
        assertContentEquals(byteArrayOf(0, 0, 0), zero.encode())
        val max = SarReceiverState(31u, 7u, 15u, 15u, 3u)
        assertContentEquals(
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0x03),
            max.encode()
        )
        assertEquals(max, SarReceiverState.decode(max.encode()))
    }

    @Test
    fun testReceiverDecodingIgnoresRfu() {
        assertEquals(rx, SarReceiverState.decode(byteArrayOf(0x23, 0x5A, 0xFE.toByte())))
    }

    @Test
    fun testReceiverOutOfRangeRejected() {
        assertFailsWith<IllegalArgumentException> { SarReceiverState(32u, 0u, 0u, 0u, 0u) }
        assertFailsWith<IllegalArgumentException> { SarReceiverState(0u, 8u, 0u, 0u, 0u) }
        assertFailsWith<IllegalArgumentException> { SarReceiverState(0u, 0u, 16u, 0u, 0u) }
        assertFailsWith<IllegalArgumentException> { SarReceiverState(0u, 0u, 0u, 16u, 0u) }
        assertFailsWith<IllegalArgumentException> { SarReceiverState(0u, 0u, 0u, 0u, 4u) }
    }

    @Test
    fun testReceiverDurations() {
        assertEquals(55.seconds, rx.discardTimeoutDuration)
        assertEquals(60.milliseconds, rx.segmentReceptionInterval)
    }

    @Test
    fun testReceiverDecoding() {
        val set = SarReceiverSet.init(rxBytes)
        assertNotNull(set)
        assertEquals(rx, set.state)
        val status = SarReceiverStatus.init(rxBytes)
        assertNotNull(status)
        assertEquals(rx, status.state)
        assertNotNull(SarReceiverGet.init(byteArrayOf()))
    }

    @Test
    fun testReceiverInvalidDecoding() {
        assertNull(SarReceiverSet.init(null))
        assertNull(SarReceiverSet.init(byteArrayOf(0, 0)))
        assertNull(SarReceiverSet.init(byteArrayOf(0, 0, 0, 0)))
        assertNull(SarReceiverStatus.init(null))
        assertNull(SarReceiverStatus.init(byteArrayOf(0, 0)))
        assertNull(SarReceiverStatus.init(byteArrayOf(0, 0, 0, 0)))
        assertNull(SarReceiverGet.init(null))
        assertNull(SarReceiverGet.init(byteArrayOf(0)))
    }

    @Test
    fun testReceiverRoundTrip() {
        val set = SarReceiverSet.init(SarReceiverSet(rx).parameters)
        assertNotNull(set)
        assertEquals(rx, set.state)
        val status = SarReceiverStatus.init(SarReceiverStatus(rx).parameters)
        assertNotNull(status)
        assertEquals(rx, status.state)
    }
}
