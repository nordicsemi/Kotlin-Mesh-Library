package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.messages.foundation.time.TimeGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.time.TimeSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.time.TimeStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.time.TimeZoneGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.time.TimeZoneSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.time.TimeZoneStatus
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TimeMessagesTest {

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    private val time = TaiTime(
        seconds = 0x0102030405uL,
        subSecond = 0x06u,
        uncertainty = 0x07u,
        timeAuthority = true,
        taiUtcDelta = 0,
        timeZoneOffset = TimeZoneOffset.UTC
    )

    @Test
    fun testOpCodes() {
        assertEquals(0x8237u, TimeGet.opCode)
        assertEquals(0x5Cu, TimeSet.opCode)
        assertEquals(0x5Du, TimeStatus.opCode)
        assertEquals(0x823Bu, TimeZoneGet.opCode)
        assertEquals(0x823Cu, TimeZoneSet.opCode)
        assertEquals(0x823Du, TimeZoneStatus.opCode)
    }

    @Test
    fun testGetMessages() {
        assertNull(TimeGet().parameters)
        assertNull(TimeZoneGet().parameters)
        assertNull(TimeGet.init(null))
        assertNull(TimeZoneGet.init(null))
        assertNotNull(TimeGet.init(byteArrayOf()))
        assertNotNull(TimeZoneGet.init(byteArrayOf()))
        assertNull(TimeGet.init(byteArrayOf(0)))
        assertNull(TimeZoneGet.init(byteArrayOf(0)))
        assertEquals(TimeStatus.opCode, TimeGet().responseOpCode)
        assertEquals(TimeZoneStatus.opCode, TimeZoneGet().responseOpCode)
    }

    @Test
    fun testTimeSetEncoding() {
        val set = TimeSet(time)
        assertContentEquals(
            bytes(0x05, 0x04, 0x03, 0x02, 0x01, 0x06, 0x07, 0xFF, 0x01, 0x40),
            set.parameters
        )
        assertEquals(TimeStatus.opCode, set.responseOpCode)
    }

    @Test
    fun testTaiUtcDeltaBoundaries() {
        // Delta is stored with an offset of 255 in 15 bits, authority in the LSB.
        val min = TaiTime(seconds = 1uL, taiUtcDelta = -255).toByteArray()
        assertEquals(0x00, min[7].toInt())
        assertEquals(0x00, min[8].toInt())
        val max = TaiTime(seconds = 1uL, taiUtcDelta = 32512, timeAuthority = true).toByteArray()
        assertEquals(0xFF, max[7].toInt() and 0xFF)
        assertEquals(0xFF, max[8].toInt() and 0xFF)
        assertEquals(32512, TaiTime.from(max)!!.taiUtcDelta)
        assertEquals(-255, TaiTime.from(min)!!.taiUtcDelta)
        assertFailsWith<IllegalArgumentException> { TaiTime(taiUtcDelta = 32513) }
        assertFailsWith<IllegalArgumentException> { TaiTime(taiUtcDelta = -256) }
        assertFailsWith<IllegalArgumentException> { TaiTime(seconds = 0x01_0000_0000_00uL) }
    }

    @Test
    fun testTimeSetDecoding() {
        val decoded = TimeSet.init(TimeSet(time).parameters) as TimeSet
        assertEquals(time, decoded.time)
        assertNull(TimeSet.init(null))
        assertNull(TimeSet.init(ByteArray(5)))
        assertNull(TimeSet.init(ByteArray(11)))
    }

    @Test
    fun testTimeStatusKnownRoundTrip() {
        val status = TimeStatus(time)
        assertEquals(10, status.parameters.size)
        assertEquals(time, (TimeStatus.init(status.parameters) as TimeStatus).time)
    }

    @Test
    fun testTimeStatusUnknownIsFiveBytes() {
        val status = TimeStatus(TaiTime())
        assertContentEquals(ByteArray(5), status.parameters)
        val decoded = TimeStatus.init(ByteArray(5)) as TimeStatus
        assertFalse(decoded.time.isKnown)
        // 5 bytes with non-zero seconds are invalid.
        assertNull(TimeStatus.init(bytes(1, 0, 0, 0, 0)))
        assertNull(TimeStatus.init(ByteArray(7)))
        assertNull(TimeStatus.init(null))
        assertTrue(time.isKnown)
    }

    @Test
    fun testTimeZoneOffset() {
        assertEquals(0, TimeZoneOffset.UTC.minutes)
        assertEquals(-16 * 60, TimeZoneOffset(0x00u).minutes)
        assertEquals(47 * 60 + 45, TimeZoneOffset(0xFFu).minutes)
        assertEquals(0x44u.toUByte(), TimeZoneOffset.ofMinutes(60).rawValue)
        assertEquals(0x00u.toUByte(), TimeZoneOffset.ofMinutes(-10_000).rawValue)
        assertEquals(0xFFu.toUByte(), TimeZoneOffset.ofMinutes(10_000).rawValue)
    }

    @Test
    fun testTimeZoneSet() {
        val set = TimeZoneSet(newOffset = TimeZoneOffset(0x44u), taiOfZoneChange = 0x0102030405uL)
        assertContentEquals(bytes(0x44, 0x05, 0x04, 0x03, 0x02, 0x01), set.parameters)
        assertEquals(TimeZoneStatus.opCode, set.responseOpCode)
        val decoded = TimeZoneSet.init(set.parameters) as TimeZoneSet
        assertEquals(TimeZoneOffset(0x44u), decoded.newOffset)
        assertEquals(0x0102030405uL, decoded.taiOfZoneChange)
        assertNull(TimeZoneSet.init(ByteArray(5)))
        assertNull(TimeZoneSet.init(ByteArray(7)))
        assertNull(TimeZoneSet.init(null))
    }

    @Test
    fun testTimeZoneStatus() {
        val status = TimeZoneStatus(TimeZoneOffset(0x00u), TimeZoneOffset(0xFFu), 0xFF_FFFF_FFFFuL)
        assertContentEquals(bytes(0x00, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF), status.parameters)
        val decoded = TimeZoneStatus.init(status.parameters) as TimeZoneStatus
        assertEquals(TimeZoneOffset(0x00u), decoded.currentOffset)
        assertEquals(TimeZoneOffset(0xFFu), decoded.newOffset)
        assertEquals(0xFF_FFFF_FFFFuL, decoded.taiOfZoneChange)
        assertNull(TimeZoneStatus.init(ByteArray(6)))
        assertNull(TimeZoneStatus.init(ByteArray(8)))
        assertNull(TimeZoneStatus.init(null))
    }
}
