package no.nordicsemi.kotlin.mesh.core

import kotlinx.coroutines.runBlocking
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDeltaSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericMoveSet
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class TransactionHelperTest {
    private val helper = TransactionHelper()
    private val source = 0x0001.toUShort()
    private val destination = UnicastAddress(address = 0x0002)

    @Test
    fun testFirstMessageIsNewTransaction() = runBlocking {
        assertTrue(helper.isNewTransaction(GenericMoveSet(tid = 1u, deltaLevel = 1), source, destination))
    }

    @Test
    fun testSameTidWithinSixSecondsIsNotNew() = runBlocking {
        helper.isNewTransaction(GenericMoveSet(tid = 1u, deltaLevel = 1), source, destination)
        assertFalse(helper.isNewTransaction(GenericMoveSet(tid = 1u, deltaLevel = 2), source, destination))
    }

    @Test
    fun testDifferentTidIsNew() = runBlocking {
        helper.isNewTransaction(GenericMoveSet(tid = 1u, deltaLevel = 1), source, destination)
        assertTrue(helper.isNewTransaction(GenericMoveSet(tid = 2u, deltaLevel = 1), source, destination))
    }

    @Test
    fun testDifferentSourceOrDestinationIsNew() = runBlocking {
        helper.isNewTransaction(GenericMoveSet(tid = 1u, deltaLevel = 1), source, destination)
        assertTrue(
            helper.isNewTransaction(
                GenericMoveSet(tid = 1u, deltaLevel = 1), (source + 1u).toUShort(), destination
            )
        )
        assertTrue(
            helper.isNewTransaction(
                GenericMoveSet(tid = 1u, deltaLevel = 1), (source + 1u).toUShort(),
                UnicastAddress(address = 0x0003)
            )
        )
    }

    @Test
    fun testTransactionsAreTrackedPerOpCode() = runBlocking {
        helper.isNewTransaction(GenericMoveSet(tid = 1u, deltaLevel = 1), source, destination)
        // A Delta message with the same TID is not a continuation of a Move.
        assertTrue(helper.isNewTransaction(GenericDeltaSet(tid = 1u, delta = 1), source, destination))
    }

    @Test
    fun testContinuationIsDetected() = runBlocking {
        helper.isTransactionContinuation(GenericDeltaSet(tid = 4u, delta = 1), source, destination)
        assertTrue(helper.isTransactionContinuation(GenericDeltaSet(tid = 4u, delta = 2), source, destination))
    }

    @Test
    fun testSameTidAfterSixSecondsIsNew() {
        val message = GenericMoveSet(tid = 1u, deltaLevel = 1)
        assertTrue(message.isNewTransaction(previousTid = 1u, timestamp = Clock.System.now() - 7.seconds))
        assertFalse(message.isNewTransaction(previousTid = 1u, timestamp = Clock.System.now() - 1.seconds))
    }
}
