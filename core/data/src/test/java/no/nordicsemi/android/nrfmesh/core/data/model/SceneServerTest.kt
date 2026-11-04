package no.nordicsemi.android.nrfmesh.core.data.model

import kotlinx.coroutines.runBlocking
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRecall
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRecallUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRegisterGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRegisterStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneStatusCode
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneStore
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SceneServerTest {
    private val node = SceneTestNode()

    private suspend fun store(scene: UShort) = node.sendToSceneSetupServer(SceneStore(scene))

    @Test
    fun testSceneRegisterIsEmptyInitially() = runBlocking {
        val status = node.sendToSceneServer(SceneRegisterGet())
        assertIs<SceneRegisterStatus>(status)
        assertEquals(SceneStatusCode.SUCCESS, status.status)
        assertEquals(0.toUShort(), status.currentScene)
        assertTrue(status.scenes.isEmpty())
    }

    @Test
    fun testSceneGetReportsNoCurrentSceneInitially() = runBlocking {
        val status = node.sendToSceneServer(SceneGet())
        assertIs<SceneStatus>(status)
        assertEquals(SceneStatusCode.SUCCESS, status.status)
        assertEquals(0.toUShort(), status.currentScene)
        assertNull(status.targetScene)
    }

    @Test
    fun testRecallOfUnknownSceneReturnsNotFound() = runBlocking {
        val status = node.sendToSceneServer(SceneRecall(sceneNumber = 5u))
        assertIs<SceneStatus>(status)
        assertEquals(SceneStatusCode.SCENE_NOT_FOUND, status.status)
        assertTrue(node.storedWithScene.recalled.isEmpty())
    }

    @Test
    fun testRecallOfStoredSceneIsForwardedToModels() = runBlocking {
        store(1u)
        store(2u)

        val status = node.sendToSceneServer(SceneRecall(sceneNumber = 1u))
        assertIs<SceneStatus>(status)
        assertEquals(SceneStatusCode.SUCCESS, status.status)
        assertEquals(1.toUShort(), status.currentScene)
        assertEquals(1, node.storedWithScene.recalled.size)
        assertEquals(1.toUShort(), node.storedWithScene.recalled.single().first)
    }

    @Test
    fun testRecallOfCurrentSceneIsNotForwarded() = runBlocking {
        store(1u)

        val status = node.sendToSceneServer(SceneRecall(sceneNumber = 1u))
        assertIs<SceneStatus>(status)
        assertEquals(SceneStatusCode.SUCCESS, status.status)
        assertEquals(1.toUShort(), status.currentScene)
        assertTrue(node.storedWithScene.recalled.isEmpty())
    }

    @Test
    fun testRecallWithTransitionReportsTargetScene() = runBlocking {
        store(1u)
        store(2u)

        val status = node.sendToSceneServer(
            SceneRecall(
                tid = 1u,
                sceneNumber = 1u,
                transitionParams = TransitionParameters(
                    // 10 steps of 1 second.
                    transitionTime = TransitionTime(rawValue = 0x4Au),
                    delay = 0u
                )
            )
        )
        assertIs<SceneStatus>(status)
        assertEquals(SceneStatusCode.SUCCESS, status.status)
        // While a transition is in progress the current scene is 0.
        assertEquals(0.toUShort(), status.currentScene)
        assertEquals(1.toUShort(), status.targetScene)
        assertTrue(status.remainingTime != null)
        assertEquals(1, node.storedWithScene.recalled.size)
    }

    @Test
    fun testUnacknowledgedRecallHasNoResponse() = runBlocking {
        store(1u)
        store(2u)

        val response = node.sendToSceneServer(SceneRecallUnacknowledged(tid = 1u, sceneNumber = 1u))
        assertNull(response)
        assertEquals(1.toUShort(), node.sceneServer.currentScene)
        assertEquals(1, node.storedWithScene.recalled.size)
    }

    @Test
    fun testSceneRegisterIsRestoredFromStorage() = runBlocking {
        store(3u)
        store(1u)

        // A new Scene Server using the same storage, i.e. after the app was restarted.
        val restarted = SceneTestNode(storage = node.storage)
        val status = restarted.sendToSceneServer(SceneRegisterGet())
        assertIs<SceneRegisterStatus>(status)
        assertEquals(listOf<UShort>(1u, 3u), status.scenes)
        assertEquals(0.toUShort(), status.currentScene)
    }
}
