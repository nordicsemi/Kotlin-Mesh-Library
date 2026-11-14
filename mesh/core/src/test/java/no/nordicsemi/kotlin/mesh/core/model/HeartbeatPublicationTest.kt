package no.nordicsemi.kotlin.mesh.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class HeartbeatPublicationTest {

    @Test
    fun `countLog2Count disabled and indefinite`() {
        assertEquals(0.toUShort(), HeartbeatPublication.countLog2Count(0x00u))
        assertEquals(0xFFFF.toUShort(), HeartbeatPublication.countLog2Count(0xFFu))
    }

    @Test
    fun `countLog2Count is 2 pow (countLog - 1) for 1 to 0x10`() {
        for (log in 0x01..0x10) {
            assertEquals(
                (1 shl (log - 1)).toUShort(),
                HeartbeatPublication.countLog2Count(log.toUByte()),
                "countLog $log"
            )
        }
    }

    @Test
    fun `countLog2Count max is 65534`() {
        assertEquals(65534.toUShort(), HeartbeatPublication.countLog2Count(0x11u))
    }

    @Test
    fun `countLog2Count throws for prohibited values`() {
        for (log in 0x12..0xFE) {
            assertFailsWith<IllegalArgumentException>("countLog $log") {
                HeartbeatPublication.countLog2Count(log.toUByte())
            }
        }
    }

    @Test
    fun `count2CountLog round trips for all valid countLogs`() {
        for (log in 0x00..HeartbeatPublication.MAX_COUNT_LOG) {
            val count = HeartbeatPublication.countLog2Count(log.toUByte())
            assertEquals(log.toUByte(), HeartbeatPublication.count2CountLog(count), "countLog $log")
        }
        assertEquals(
            0xFF.toUByte(),
            HeartbeatPublication.count2CountLog(HeartbeatPublication.countLog2Count(0xFFu))
        )
    }

    @Test
    fun `periodLog2Period disabled`() {
        assertEquals(0.toUShort(), HeartbeatPublication.periodLog2Period(0x00u))
    }

    @Test
    fun `periodLog2Period is 2 pow (periodLog - 1) for 1 to 0x10`() {
        for (log in 0x01..0x10) {
            assertEquals(
                (1 shl (log - 1)).toUShort(),
                HeartbeatPublication.periodLog2Period(log.toUByte()),
                "periodLog $log"
            )
        }
    }

    @Test
    fun `periodLog2Period max is 0xFFFF`() {
        assertEquals(0xFFFF.toUShort(), HeartbeatPublication.periodLog2Period(0x11u))
    }

    @Test
    fun `periodLog2Period throws for out of range values`() {
        for (log in 0x12..0xFF) {
            assertFailsWith<IllegalArgumentException>("periodLog $log") {
                HeartbeatPublication.periodLog2Period(log.toUByte())
            }
        }
    }

    @Test
    fun `period2PeriodLog round trips for all valid periodLogs`() {
        for (log in 0x00..HeartbeatPublication.MAX_PERIOD_LOG) {
            val period = HeartbeatPublication.periodLog2Period(log.toUByte())
            assertEquals(log.toUByte(), HeartbeatPublication.period2PeriodLog(period), "periodLog $log")
        }
    }

    @Test
    fun `period2PeriodLog returns null for non power of two`() {
        assertNull(HeartbeatPublication.period2PeriodLog(3u))
        assertNull(HeartbeatPublication.period2PeriodLog(100u))
    }

    @Test
    fun `slider ranges are consistent`() {
        assertEquals(0x00, HeartbeatPublication.MIN_COUNT_LOG)
        assertEquals(0x11, HeartbeatPublication.MAX_COUNT_LOG)
        assertEquals(0x01..0x11, HeartbeatPublication.PERIOD_LOG_RANGE)
    }
}
