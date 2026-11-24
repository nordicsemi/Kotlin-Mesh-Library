package no.nordicsemi.android.nrfmesh.feature.model.generic

import no.nordicsemi.android.nrfmesh.feature.model.utils.GenericLevelOptions
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDeltaSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDeltaSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericMoveSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericMoveSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.StepResolution
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Tests the message that is sent when Set is pressed on the Generic Level controls. */
class GenericLevelSendTest {

    private val model = Model(modelId = SigModelId(Model.GENERIC_LEVEL_SERVER_MODEL_ID))
    private val time = TransitionTime(steps = 5u, stepResolution = StepResolution.SECONDS)

    private fun send(
        option: GenericLevelOptions,
        percent: Float = 150f,
        acknowledged: Boolean = true,
        defaultTransition: Boolean = false,
        delay: Int = 10,
    ): List<Pair<Model, MeshMessage>> {
        val sent = mutableListOf<Pair<Model, MeshMessage>>()
        onSliderValueChanged(
            levelOption = option.value,
            percent = percent,
            acknowledged = acknowledged,
            model = model,
            delay = delay,
            defaultTransitionEnabled = defaultTransition,
            transitionTime = time,
            sendApplicationMessage = { m, msg -> sent += m to msg }
        )
        return sent
    }

    private fun message(option: GenericLevelOptions, percent: Float = 150f) =
        send(option = option, percent = percent).single().second

    @Test
    fun testSetSendsExactlyOneMessageToTheModel() {
        GenericLevelOptions.entries.forEach {
            val sent = send(option = it)
            assertEquals(1, sent.size)
            assertEquals(model, sent.single().first)
        }
    }

    @Test
    fun testMessageTypePerOptionAndAcknowledgement() {
        fun msg(o: GenericLevelOptions, ack: Boolean) =
            send(option = o, acknowledged = ack).single().second
        assertIs<GenericLevelSet>(msg(GenericLevelOptions.LEVEL, true))
        assertIs<GenericLevelSetUnacknowledged>(msg(GenericLevelOptions.LEVEL, false))
        assertIs<GenericDeltaSet>(msg(GenericLevelOptions.DELTA, true))
        assertIs<GenericDeltaSetUnacknowledged>(msg(GenericLevelOptions.DELTA, false))
        assertIs<GenericMoveSet>(msg(GenericLevelOptions.MOVE, true))
        assertIs<GenericMoveSetUnacknowledged>(msg(GenericLevelOptions.MOVE, false))
    }

    @Test
    fun testTransitionParametersAreSentWhenDefaultIsDisabled() {
        val expected = TransitionParameters(transitionTime = time, delay = 10u)
        assertEquals(expected, (message(GenericLevelOptions.LEVEL) as GenericLevelSet).transitionParams)
        assertEquals(expected, (message(GenericLevelOptions.DELTA) as GenericDeltaSet).transitionParams)
        assertEquals(expected, (message(GenericLevelOptions.MOVE) as GenericMoveSet).transitionParams)
    }

    @Test
    fun testTransitionParametersAreOmittedWhenDefaultIsEnabled() {
        GenericLevelOptions.entries.forEach {
            val params = when (val msg = send(option = it, defaultTransition = true).single().second) {
                is GenericLevelSet -> msg.transitionParams
                is GenericDeltaSet -> msg.transitionParams
                is GenericMoveSet -> msg.transitionParams
                else -> error("Unexpected message $msg")
            }
            assertNull(params)
        }
    }

    @Test
    fun testDeltaSignFollowsSlider() {
        assertTrue((message(GenericLevelOptions.DELTA, 150f) as GenericDeltaSet).delta > 0)
        assertTrue((message(GenericLevelOptions.DELTA, 50f) as GenericDeltaSet).delta < 0)
    }

    @Test
    fun testMoveSignFollowsSlider() {
        assertTrue((message(GenericLevelOptions.MOVE, 150f) as GenericMoveSet).deltaLevel > 0)
        assertTrue((message(GenericLevelOptions.MOVE, 50f) as GenericMoveSet).deltaLevel < 0)
    }

    @Test
    fun testLevelIncreasesWithSlider() {
        val low = message(GenericLevelOptions.LEVEL, 0f) as GenericLevelSet
        val high = message(GenericLevelOptions.LEVEL, 200f) as GenericLevelSet
        assertTrue(low.level < high.level)
    }
}
