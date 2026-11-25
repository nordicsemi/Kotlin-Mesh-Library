package no.nordicsemi.android.nrfmesh.feature.localnode

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Tests the bulb of the Generic OnOff Server card of the local node. */
class OnOffBulbTest {

    @Test
    fun testFollowsTheStateWithoutATransition() {
        assertEquals(OnOffBulb(isOn = true), onOffBulbOf(true, null, 0.seconds))
        assertEquals(OnOffBulb(isOn = false), onOffBulbOf(false, null, 0.seconds))
    }

    @Test
    fun testTurnsOnAsSoonAsATransitionToOnStarts() {
        assertEquals(OnOffBulb(isOn = true), onOffBulbOf(false, true, 10.seconds))
    }

    @Test
    fun testStaysOnUntilATransitionToOffEnds() {
        assertEquals(
            OnOffBulb(isOn = true, turnsOffAfter = 10.seconds + 100.milliseconds),
            onOffBulbOf(true, false, 10.seconds)
        )
    }

    @Test
    fun testTurnsOffAfterTheRemainingTimeIncludingTheDelay() {
        assertEquals(
            OnOffBulb(isOn = true, turnsOffAfter = 2.seconds + 100.milliseconds),
            onOffBulbOf(true, false, 2.seconds)
        )
    }
}
