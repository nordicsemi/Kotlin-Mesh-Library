package no.nordicsemi.kotlin.mesh.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class TransitionTimeTest {

    @Test
    fun testIntervalOfKnownTimes() {
        assertEquals(500.milliseconds, TransitionTime(steps = 5u, StepResolution.HUNDREDS_OF_MILLISECONDS).interval)
        assertEquals(5.seconds, TransitionTime(steps = 5u, StepResolution.SECONDS).interval)
        assertEquals(50.seconds, TransitionTime(steps = 5u, StepResolution.TENS_OF_SECONDS).interval)
        assertEquals(50.minutes, TransitionTime(steps = 5u, StepResolution.TENS_OF_MINUTES).interval)
        assertEquals(0.milliseconds, TransitionTime.immediate.interval)
    }

    @Test
    fun testIntervalOfUnknownTime() {
        assertNull(TransitionTime.unknown.interval)
        assertNull(TransitionTime.unknown.milliseconds)
    }

    @Test
    fun testInitFromDuration() {
        assertEquals(TransitionTime.immediate, TransitionTime.init(duration = 0.milliseconds))
        assertEquals(
            TransitionTime(steps = 5u, StepResolution.HUNDREDS_OF_MILLISECONDS),
            TransitionTime.init(duration = 500.milliseconds)
        )
        assertEquals(
            TransitionTime(steps = 10u, StepResolution.SECONDS),
            TransitionTime.init(duration = 10.seconds)
        )
        assertEquals(
            TransitionTime(steps = 6u, StepResolution.TENS_OF_SECONDS),
            TransitionTime.init(duration = 60.seconds + 5.seconds)
        )
        assertEquals(
            TransitionTime(steps = 3u, StepResolution.TENS_OF_MINUTES),
            TransitionTime.init(duration = 30.minutes)
        )
    }

    @Test
    fun testInitFromDurationRoundTrips() {
        listOf(300.milliseconds, 2.seconds, 40.seconds, 200.seconds, 20.minutes).forEach {
            assertEquals(it, TransitionTime.init(duration = it).interval)
        }
    }
}
