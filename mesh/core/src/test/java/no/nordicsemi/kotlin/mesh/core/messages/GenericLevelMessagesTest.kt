package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDeltaSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDeltaSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericMoveSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericMoveSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class GenericLevelMessagesTest {

    private val transition = TransitionParameters(
        transitionTime = TransitionTime(rawValue = 0x45u),
        delay = 0x0Au
    )

    @Test
    fun testOpCodes() {
        assertEquals(0x8205u, GenericLevelGet.opCode)
        assertEquals(0x8206u, GenericLevelSet.opCode)
        assertEquals(0x8207u, GenericLevelSetUnacknowledged.opCode)
        assertEquals(0x8208u, GenericLevelStatus.opCode)
        assertEquals(0x8209u, GenericDeltaSet.opCode)
        assertEquals(0x820Au, GenericDeltaSetUnacknowledged.opCode)
        assertEquals(0x820Bu, GenericMoveSet.opCode)
        assertEquals(0x820Cu, GenericMoveSetUnacknowledged.opCode)
    }

    @Test
    fun testAcknowledgedMessagesRespondWithGenericLevelStatus() {
        assertEquals(GenericLevelStatus.opCode, GenericLevelGet().responseOpCode)
        assertEquals(GenericLevelStatus.opCode, GenericLevelSet(level = 0).responseOpCode)
        assertEquals(GenericLevelStatus.opCode, GenericDeltaSet(delta = 0).responseOpCode)
        assertEquals(GenericLevelStatus.opCode, GenericMoveSet(deltaLevel = 0).responseOpCode)
    }

    @Test
    fun testGetHasNoParameters() {
        assertNull(GenericLevelGet().parameters)
    }

    // Messages are created without a TID, which is assigned when the message is sent. Building
    // the parameters must therefore be deferred until the TID is set.

    @Test
    fun testParametersAreNotBuiltBeforeTheTidIsSet() {
        // Creating the messages must not fail even though the TID is not set.
        val messages = listOf(
            GenericLevelSet(level = 1),
            GenericLevelSetUnacknowledged(level = 1),
            GenericDeltaSet(delta = 1),
            GenericDeltaSetUnacknowledged(delta = 1),
            GenericMoveSet(deltaLevel = 1),
            GenericMoveSetUnacknowledged(deltaLevel = 1)
        )
        messages.forEach { assertNull(it.tid) }
    }

    @Test
    fun testGenericMoveSetParametersUseTheTidAssignedLater() {
        val message = GenericMoveSet(deltaLevel = 0x0102)
        message.tid = 0x07u
        assertContentEquals(byteArrayOf(0x02, 0x01, 0x07), message.parameters)
    }

    @Test
    fun testLevelSetParameters() {
        assertContentEquals(
            byteArrayOf(0x34, 0x12, 0x05),
            GenericLevelSet(level = 0x1234, tid = 5u).parameters
        )
        assertContentEquals(
            byteArrayOf(0x34, 0x12, 0x05, 0x45, 0x0A),
            GenericLevelSet(tid = 5u, level = 0x1234, transitionParams = transition).parameters
        )
        assertContentEquals(
            byteArrayOf(0x00, 0x80.toByte(), 0x05),
            GenericLevelSetUnacknowledged(level = Short.MIN_VALUE, tid = 5u).parameters
        )
    }

    @Test
    fun testDeltaSetParameters() {
        // Delta is a signed 32-bit value.
        assertContentEquals(
            byteArrayOf(0x10, 0x00, 0x00, 0x00, 0x09),
            GenericDeltaSet(delta = 16, tid = 9u).parameters
        )
        assertContentEquals(
            byteArrayOf(0xF0.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x09),
            GenericDeltaSetUnacknowledged(delta = -16, tid = 9u).parameters
        )
        assertContentEquals(
            byteArrayOf(0x10, 0x00, 0x00, 0x00, 0x09, 0x45, 0x0A),
            GenericDeltaSet(tid = 9u, delta = 16, transitionParams = transition).parameters
        )
    }

    @Test
    fun testMoveSetParameters() {
        assertContentEquals(
            byteArrayOf(0x0A, 0x00, 0x03),
            GenericMoveSet(deltaLevel = 10, tid = 3u).parameters
        )
        assertContentEquals(
            byteArrayOf(0xF6.toByte(), 0xFF.toByte(), 0x03),
            GenericMoveSetUnacknowledged(deltaLevel = -10, tid = 3u).parameters
        )
        assertContentEquals(
            byteArrayOf(0x0A, 0x00, 0x03, 0x45, 0x0A),
            GenericMoveSet(tid = 3u, deltaLevel = 10, transitionParams = transition).parameters
        )
        assertContentEquals(
            byteArrayOf(0x0A, 0x00, 0x03, 0x45, 0x0A),
            GenericMoveSetUnacknowledged(
                tid = 3u,
                deltaLevel = 10,
                transitionParams = transition
            ).parameters
        )
    }

    @Test
    fun testMovePercentIsScaledToTheSignedRange() {
        // The percentage of a Move message is relative, from -100% to 100%.
        assertEquals(0, GenericMoveSet(tid = 1u, percent = 0f).deltaLevel.toInt())
        assertEquals(3276, GenericMoveSet(tid = 1u, percent = 10f).deltaLevel.toInt())
        assertEquals(-3276, GenericMoveSet(tid = 1u, percent = -10f).deltaLevel.toInt())
        assertEquals(
            3276,
            GenericMoveSetUnacknowledged(tid = 1u, percent = 10f).deltaLevel.toInt()
        )
        assertEquals(
            -3276,
            GenericMoveSetUnacknowledged(tid = 1u, percent = -10f).deltaLevel.toInt()
        )
    }

    @Test
    fun testMovePercentIsClamped() {
        assertEquals(Short.MAX_VALUE, GenericMoveSet(tid = 1u, percent = 200f).deltaLevel)
        assertEquals(Short.MIN_VALUE, GenericMoveSet(tid = 1u, percent = -200f).deltaLevel)
        assertEquals(
            Short.MAX_VALUE,
            GenericMoveSetUnacknowledged(tid = 1u, percent = 200f).deltaLevel
        )
        assertEquals(
            Short.MIN_VALUE,
            GenericMoveSetUnacknowledged(tid = 1u, percent = -200f).deltaLevel
        )
    }

    @Test
    fun testLevelPercentIsScaledToTheSignedRange() {
        assertEquals(Short.MIN_VALUE, GenericLevelSet(tid = 1u, percent = 0f).level)
        assertEquals(Short.MAX_VALUE, GenericLevelSet(tid = 1u, percent = 100f).level)
        assertEquals(0, GenericLevelSet(tid = 1u, percent = 50f).level.toInt())
        assertEquals(
            Short.MIN_VALUE,
            GenericLevelSetUnacknowledged(tid = 1u, percent = 0f).level
        )
    }

    @Test
    fun testStatusParameters() {
        assertContentEquals(
            byteArrayOf(0x34, 0x12),
            GenericLevelStatus(level = 0x1234).parameters
        )
        assertContentEquals(
            byteArrayOf(0x34, 0x12, 0x78, 0x56, 0x45),
            GenericLevelStatus(
                remainingTime = TransitionTime(rawValue = 0x45u),
                level = 0x1234,
                targetLevel = 0x5678
            ).parameters
        )
    }

    @Test
    fun testStatusIsParsedFromParameters() {
        val idle = GenericLevelStatus.init(byteArrayOf(0x00, 0x80.toByte()))
        assertNotNull(idle)
        assertEquals(Short.MIN_VALUE, idle.level)
        assertNull(idle.targetLevel)
        assertNull(idle.remainingTime)

        val transitioning = GenericLevelStatus.init(
            byteArrayOf(0x34, 0x12, 0x78, 0x56, 0x45)
        )
        assertNotNull(transitioning)
        assertEquals(0x1234, transitioning.level.toInt())
        assertEquals(0x5678, transitioning.targetLevel?.toInt())
        assertEquals(TransitionTime(rawValue = 0x45u), transitioning.remainingTime)
    }

    @Test
    fun testMalformedParametersAreRejected() {
        assertNull(GenericLevelStatus.init(null))
        assertNull(GenericLevelStatus.init(byteArrayOf(0x00)))
        assertNull(GenericLevelStatus.init(byteArrayOf(0x00, 0x00, 0x00)))
        assertNull(GenericLevelSet.init(null))
        assertNull(GenericLevelSet.init(byteArrayOf(0x00, 0x00)))
        assertNull(GenericMoveSet.init(byteArrayOf(0x00, 0x00)))
        assertNull(GenericMoveSetUnacknowledged.init(byteArrayOf(0x00, 0x00)))
    }

    @Test
    fun testSetMessagesAreParsedFromParameters() {
        val set = GenericLevelSet.init(byteArrayOf(0x34, 0x12, 0x05, 0x45, 0x0A))
        assertNotNull(set)
        assertEquals(0x1234, set.level.toInt())
        assertEquals(5.toUByte(), set.tid)
        assertEquals(TransitionTime(rawValue = 0x45u), set.transitionTime)
        assertEquals(0x0A.toUByte(), set.delay)

        val move = GenericMoveSet.init(byteArrayOf(0xF6.toByte(), 0xFF.toByte(), 0x03))
        assertNotNull(move)
        assertEquals(-10, move.deltaLevel.toInt())
        assertEquals(3.toUByte(), move.tid)
        assertNull(move.transitionTime)
        assertNull(move.delay)

        val delta = GenericDeltaSet.init(byteArrayOf(0x10, 0x00, 0x00, 0x00, 0x09))
        assertNotNull(delta)
        assertEquals(16, delta.delta)
        assertEquals(9.toUByte(), delta.tid)
    }
}
