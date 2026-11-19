package no.nordicsemi.kotlin.mesh.core.layers.access

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TransactionTest {

    private class TestClock(var current: Instant = Instant.fromEpochSeconds(1_000)) : Clock {
        override fun now() = current
        fun advance(duration: Duration) {
            current += duration
        }
    }

    private val clock = TestClock()
    private val transaction = Transaction(lastTid = 10u, clock = clock)

    @Test
    fun `new transaction is active`() {
        assertTrue(transaction.isActive)
    }

    @Test
    fun `transaction is active within 6 seconds`() {
        clock.advance(5.seconds + 900.milliseconds)
        assertTrue(transaction.isActive)
    }

    @Test
    fun `transaction is not active after 6 seconds`() {
        clock.advance(6.seconds + 1.milliseconds)
        assertFalse(transaction.isActive)
    }

    @Test
    fun `message continuing an active transaction reuses the tid`() {
        clock.advance(1.seconds)
        assertEquals(10u.toUByte(), transaction.tid(retransmit = false, continueTransaction = true))
    }

    @Test
    fun `continuing transaction keeps the transaction alive`() {
        repeat(4) {
            clock.advance(4.seconds)
            assertEquals(
                10u.toUByte(),
                transaction.tid(retransmit = false, continueTransaction = true)
            )
        }
    }

    @Test
    fun `message continuing an expired transaction gets a new tid`() {
        clock.advance(7.seconds)
        assertEquals(11u.toUByte(), transaction.tid(retransmit = false, continueTransaction = true))
    }

    @Test
    fun `message not continuing a transaction gets a new tid`() {
        clock.advance(1.seconds)
        assertEquals(11u.toUByte(), transaction.tid(retransmit = false, continueTransaction = false))
        assertEquals(12u.toUByte(), transaction.tid(retransmit = false, continueTransaction = false))
    }

    @Test
    fun `retransmission within 6 seconds reuses the tid`() {
        clock.advance(5.seconds)
        assertEquals(10u.toUByte(), transaction.tid(retransmit = true, continueTransaction = false))
    }

    @Test
    fun `retransmission of an expired transaction gets a new tid`() {
        clock.advance(7.seconds)
        assertEquals(11u.toUByte(), transaction.tid(retransmit = true, continueTransaction = false))
    }

    @Test
    fun `tid wraps around after 255`() {
        val t = Transaction(lastTid = 255u, clock = clock)
        assertEquals(0u.toUByte(), t.tid(retransmit = false, continueTransaction = false))
    }

    @Test
    fun `new tid differs from the previous one`() {
        val previous = transaction.lastTid
        assertNotEquals(previous, transaction.tid(retransmit = false, continueTransaction = false))
    }
}
