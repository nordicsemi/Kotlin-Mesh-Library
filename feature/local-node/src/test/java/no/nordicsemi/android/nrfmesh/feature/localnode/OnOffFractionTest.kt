package no.nordicsemi.android.nrfmesh.feature.localnode

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

/** Tests how far a Generic OnOff transition has progressed. */
class OnOffFractionTest {
    private val duration = 10.seconds

    @Test
    fun testStaysAtTheStartWhileTheDelayIsPending() {
        assertEquals(
            0f,
            onOffFractionAt(from = 0f, to = 1f, started = false, remaining = 8.seconds, duration = duration)
        )
    }

    @Test
    fun testStaysAtTheStartWhenNothingHasPassed() {
        assertEquals(
            1f,
            onOffFractionAt(from = 1f, to = 0f, started = true, remaining = duration, duration = duration)
        )
    }

    @Test
    fun testTurningOnMovesTowardsOn() {
        assertEquals(
            0.75f,
            onOffFractionAt(from = 0f, to = 1f, started = true, remaining = 2.5.seconds, duration = duration)
        )
    }

    @Test
    fun testTurningOffMovesTowardsOff() {
        assertEquals(
            0.75f,
            onOffFractionAt(from = 1f, to = 0f, started = true, remaining = 7.5.seconds, duration = duration)
        )
    }

    @Test
    fun testEndsAtTheTarget() {
        assertEquals(
            0f,
            onOffFractionAt(from = 1f, to = 0f, started = true, remaining = 0.seconds, duration = duration)
        )
    }

    @Test
    fun testZeroDurationStaysAtTheStart() {
        assertEquals(
            0f,
            onOffFractionAt(from = 0f, to = 1f, started = true, remaining = 0.seconds, duration = 0.seconds)
        )
    }
}
