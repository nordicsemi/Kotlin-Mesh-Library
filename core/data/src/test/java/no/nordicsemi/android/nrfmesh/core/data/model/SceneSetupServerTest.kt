package no.nordicsemi.android.nrfmesh.core.data.model

import kotlinx.coroutines.runBlocking
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneDelete
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneDeleteUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRegisterStatus
import no.nordicsemi.kotlin.mesh.core.messages.SceneMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneStore
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneStoreUnacknowledged
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SceneSetupServerTest {
    private val node = SceneTestNode()

    @Test
    fun testStoreAddsSceneAndMakesItCurrent() = runBlocking {
        val status = node.sendToSceneSetupServer(SceneStore(sceneNumber = 7u))
        assertIs<SceneRegisterStatus>(status)
        assertEquals(SceneMessageStatus.SUCCESS, status.status)
        assertEquals(7.toUShort(), status.currentScene)
        assertEquals(listOf<UShort>(7u), status.scenes)
        assertEquals(listOf<UShort>(7u), node.storedWithScene.stored)
    }

    @Test
    fun testStoringAnExistingSceneDoesNotDuplicateIt() = runBlocking {
        node.sendToSceneSetupServer(SceneStore(sceneNumber = 7u))
        val status = node.sendToSceneSetupServer(SceneStore(sceneNumber = 7u))
        assertIs<SceneRegisterStatus>(status)
        assertEquals(SceneMessageStatus.SUCCESS, status.status)
        assertEquals(listOf<UShort>(7u), status.scenes)
        // The states are stored with the scene again.
        assertEquals(2, node.storedWithScene.stored.size)
    }

    @Test
    fun testScenesAreKeptSorted() = runBlocking {
        node.sendToSceneSetupServer(SceneStore(sceneNumber = 9u))
        node.sendToSceneSetupServer(SceneStore(sceneNumber = 2u))
        val status = node.sendToSceneSetupServer(SceneStore(sceneNumber = 5u))
        assertIs<SceneRegisterStatus>(status)
        assertEquals(listOf<UShort>(2u, 5u, 9u), status.scenes)
    }

    @Test
    fun testStoreFailsWhenSceneRegisterIsFull() = runBlocking {
        // The Scene Register can hold up to 16 scenes.
        (1..16).forEach {
            node.sendToSceneSetupServer(SceneStore(sceneNumber = it.toUShort()))
        }

        val status = node.sendToSceneSetupServer(SceneStore(sceneNumber = 100u))
        assertIs<SceneRegisterStatus>(status)
        assertEquals(SceneMessageStatus.SCENE_REGISTER_FULL, status.status)
        assertEquals(16, status.scenes.size)
        // An existing scene can still be overwritten.
        val overwrite = node.sendToSceneSetupServer(SceneStore(sceneNumber = 3u))
        assertIs<SceneRegisterStatus>(overwrite)
        assertEquals(SceneMessageStatus.SUCCESS, overwrite.status)
    }

    @Test
    fun testDeleteRemovesSceneAndClearsCurrentScene() = runBlocking {
        node.sendToSceneSetupServer(SceneStore(sceneNumber = 1u))
        node.sendToSceneSetupServer(SceneStore(sceneNumber = 2u))

        val status = node.sendToSceneSetupServer(SceneDelete(sceneNumber = 2u))
        assertIs<SceneRegisterStatus>(status)
        assertEquals(SceneMessageStatus.SUCCESS, status.status)
        assertEquals(listOf<UShort>(1u), status.scenes)
        // Scene 2 was the current scene.
        assertEquals(0.toUShort(), status.currentScene)
        assertEquals(listOf<UShort>(1u), node.storage.readScenes(node.networkUuid))
    }

    @Test
    fun testDeleteOfUnknownSceneReturnsNotFound() = runBlocking {
        node.sendToSceneSetupServer(SceneStore(sceneNumber = 1u))

        val status = node.sendToSceneSetupServer(SceneDelete(sceneNumber = 4u))
        assertIs<SceneRegisterStatus>(status)
        assertEquals(SceneMessageStatus.SCENE_NOT_FOUND, status.status)
        assertEquals(listOf<UShort>(1u), status.scenes)
    }

    @Test
    fun testUnacknowledgedStoreAndDeleteHaveNoResponse() = runBlocking {
        assertNull(node.sendToSceneSetupServer(SceneStoreUnacknowledged(sceneNumber = 1u)))
        assertNull(node.sendToSceneSetupServer(SceneStoreUnacknowledged(sceneNumber = 2u)))
        assertEquals(listOf<UShort>(1u, 2u), node.sceneServer.storedScenes)

        assertNull(node.sendToSceneSetupServer(SceneDeleteUnacknowledged(sceneNumber = 1u)))
        assertEquals(listOf<UShort>(2u), node.sceneServer.storedScenes)
    }
}
