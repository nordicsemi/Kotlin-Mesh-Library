package no.nordicsemi.android.nrfmesh.core.data.model

import no.nordicsemi.android.nrfmesh.core.data.model.GenericLevelClient.Companion.LEVEL_STEP
import no.nordicsemi.android.nrfmesh.core.data.model.GenericLevelClient.Companion.nextLevel
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Tests the state, stepping and published message of the Generic Level Client. */
class GenericLevelClientTest {

    @Test
    fun testInitialStateMatchesIos() {
        assertEquals(Short.MIN_VALUE, GenericLevelClient().state)
    }

    @Test
    fun testStepIs8192() {
        assertEquals(8192, LEVEL_STEP)
    }

    @Test
    fun testIncreaseFromInitialState() {
        assertEquals((-24576).toShort(), nextLevel(Short.MIN_VALUE, increase = true))
    }

    @Test
    fun testDecreaseFromInitialStateWrapsToPositive() {
        assertEquals(24576.toShort(), nextLevel(Short.MIN_VALUE, increase = false))
    }

    @Test
    fun testIncreaseFromZero() {
        assertEquals(8192.toShort(), nextLevel(0, increase = true))
    }

    @Test
    fun testDecreaseFromZero() {
        assertEquals((-8192).toShort(), nextLevel(0, increase = false))
    }

    @Test
    fun testIncreaseWrapsPastMaximum() {
        assertEquals((-24577).toShort(), nextLevel(Short.MAX_VALUE, increase = true))
    }

    @Test
    fun testDecreaseWrapsPastMinimum() {
        assertEquals(24577.toShort(), nextLevel((Short.MIN_VALUE + 1).toShort(), increase = false))
    }

    @Test
    fun testIncreaseThenDecreaseIsIdentityForEveryLevel() {
        for (v in Short.MIN_VALUE..Short.MAX_VALUE) {
            val level = v.toShort()
            assertEquals(level, nextLevel(nextLevel(level, true), false))
            assertEquals(level, nextLevel(nextLevel(level, false), true))
        }
    }

    @Test
    fun testEightIncreasesReturnToStart() {
        // 8 * 8192 = 65536 = one full turn of the 16-bit level.
        var level = Short.MIN_VALUE
        repeat(8) { level = nextLevel(level, increase = true) }
        assertEquals(Short.MIN_VALUE, level)
    }

    @Test
    fun testEightDecreasesReturnToStart() {
        var level = Short.MIN_VALUE
        repeat(8) { level = nextLevel(level, increase = false) }
        assertEquals(Short.MIN_VALUE, level)
    }

    @Test
    fun testStepsVisitEightDistinctLevels() {
        var level = Short.MIN_VALUE
        val seen = mutableSetOf<Short>()
        repeat(8) {
            seen += level
            level = nextLevel(level, increase = true)
        }
        assertEquals(8, seen.size)
    }

    @Test
    fun testPublishedMessageIsUnacknowledgedLevelSetWithCurrentState() {
        val composer = assertNotNull(GenericLevelClient().publicationMessageComposer)
        val message = composer()
        assertIs<GenericLevelSetUnacknowledged>(message)
        assertEquals(Short.MIN_VALUE, message.level)
        assertEquals(null, message.transitionParams)
    }

    @Test
    fun testSupportsLevelStatusAndSubscription() {
        val client = GenericLevelClient()
        assertTrue(client.isSubscriptionSupported)
        assertEquals(setOf(GenericLevelStatus.opCode), client.messageTypes.keys)
    }
}
