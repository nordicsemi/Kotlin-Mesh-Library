package no.nordicsemi.android.nrfmesh.feature.model.generic

import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnPowerUpSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnPowerUpSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.OnPowerUp
import no.nordicsemi.kotlin.mesh.core.model.StepResolution
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/** Tests the messages sent by the Default Transition Time and OnPowerUp controls. */
class PowerAndTransitionTimeMessagesTest {

    private val time = TransitionTime(steps = 5u, stepResolution = StepResolution.SECONDS)

    @Test
    fun testAcknowledgedTransitionTimeSet() {
        val message = defaultTransitionTimeSetOf(transitionTime = time, acknowledged = true)
        assertIs<GenericDefaultTransitionTimeSet>(message)
        assertEquals(time, message.transitionTime)
    }

    @Test
    fun testUnacknowledgedTransitionTimeSet() {
        val message = defaultTransitionTimeSetOf(transitionTime = time, acknowledged = false)
        assertIs<GenericDefaultTransitionTimeSetUnacknowledged>(message)
        assertEquals(time, message.transitionTime)
    }

    @Test
    fun testAcknowledgedOnPowerUpSet() {
        OnPowerUp.entries.forEach {
            val message = onPowerUpSetOf(state = it, acknowledged = true)
            assertIs<GenericOnPowerUpSet>(message)
            assertEquals(it, message.state)
        }
    }

    @Test
    fun testUnacknowledgedOnPowerUpSet() {
        OnPowerUp.entries.forEach {
            val message = onPowerUpSetOf(state = it, acknowledged = false)
            assertIs<GenericOnPowerUpSetUnacknowledged>(message)
            assertEquals(it, message.state)
        }
    }

    @Test
    fun testSliderStartIsImmediate() {
        val (time, label) = transitionTimeOf(value = 0f)
        assertEquals(TransitionTime(rawValue = 0u), time)
        assertEquals("Immediate", label)
    }

    @Test
    fun testSliderAtFiveIsFiveHundredMilliseconds() {
        val (time, label) = transitionTimeOf(value = 5f)
        assertEquals(
            TransitionTime(steps = 5u, stepResolution = StepResolution.HUNDREDS_OF_MILLISECONDS),
            time
        )
        assertEquals("500 ms", label)
    }

    @Test
    fun testSliderAtSeventyIsFourteenSeconds() {
        val (time, label) = transitionTimeOf(value = 70f)
        assertEquals(TransitionTime(steps = 14u, stepResolution = StepResolution.SECONDS), time)
        assertEquals("14 sec", label)
    }

    @Test
    fun testSliderNeverProducesTheUnknownTransitionTime() {
        // The Default Transition Time state cannot be set to the unknown value.
        for (value in 0..235) {
            assertEquals(true, transitionTimeOf(value = value.toFloat()).first.isKnown, "value $value")
        }
    }
}
