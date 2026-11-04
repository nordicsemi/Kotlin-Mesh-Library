package no.nordicsemi.android.nrfmesh.core.data.storage

import androidx.datastore.core.DataStoreFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/**
 * Tests [SceneStatesDataStoreStorage] against a real Proto DataStore backed by a temporary file.
 *
 * The storage writes asynchronously, so the tests wait until the expected value can be read back.
 */
class SceneStatesDataStoreStorageTest {
    private val directory = File.createTempFile("scene_states", "").apply {
        delete()
        mkdirs()
    }
    private val storage = SceneStatesDataStoreStorage(
        sceneStore = DataStoreFactory.create(
            serializer = ProtoSceneStatesDataStoreSerializer,
            produceFile = { File(directory, "scene_states.pb") }
        ),
        ioDispatcher = Dispatchers.IO
    )
    private val networkA = Uuid.parse("11111111-1111-1111-1111-111111111111")
    private val networkB = Uuid.parse("22222222-2222-2222-2222-222222222222")

    @AfterTest
    fun tearDown() {
        directory.deleteRecursively()
    }

    private suspend fun <T> eventually(expected: T, read: suspend () -> T) {
        var actual: T? = null
        try {
            withTimeout(5_000) {
                while (true) {
                    actual = read()
                    if (actual == expected) return@withTimeout
                    delay(10)
                }
            }
        } catch (e: Exception) {
            throw AssertionError("Expected <$expected> but was <$actual>", e)
        }
    }

    @Test
    fun testStoredScenesCanBeRead() = runBlocking {
        storage.storeScene(networkA, 9u)
        storage.storeScene(networkA, 2u)
        eventually(listOf<UShort>(2u, 9u)) { storage.readScenes(networkA) }
    }

    @Test
    fun testScenesOfDifferentNetworksAreIndependent() = runBlocking {
        storage.storeScene(networkA, 1u)
        storage.storeScene(networkB, 2u)
        eventually(listOf<UShort>(1u)) { storage.readScenes(networkA) }
        eventually(listOf<UShort>(2u)) { storage.readScenes(networkB) }
    }

    @Test
    fun testDeletedSceneIsRemoved() = runBlocking {
        storage.storeScene(networkA, 1u)
        storage.storeScene(networkA, 2u)
        eventually(listOf<UShort>(1u, 2u)) { storage.readScenes(networkA) }

        storage.deleteScene(networkA, 1u)
        eventually(listOf<UShort>(2u)) { storage.readScenes(networkA) }
    }

    @Test
    fun testStoringAnExistingSceneKeepsItsStates() = runBlocking {
        storage.storeGenericOnOffState(networkA, 3u, elementIndex = 0, on = true)
        eventually(true) { storage.readGenericOnOffState(networkA, 3u, elementIndex = 0) }

        storage.storeScene(networkA, 3u)
        eventually(listOf<UShort>(3u)) { storage.readScenes(networkA) }
        assertTrue(storage.readGenericOnOffState(networkA, 3u, elementIndex = 0))
    }

    @Test
    fun testGenericLevelStateCanBeRead() = runBlocking {
        storage.storeGenericLevelState(networkA, 4u, elementIndex = 1, level = 1234u)
        eventually(1234.toUShort()) {
            storage.readGenericLevelState(networkA, 4u, elementIndex = 1)
        }
    }

    @Test
    fun testReadingStateOfNewNetworkIsPersistedAndKeepsOtherNetworks() = runBlocking {
        storage.storeScene(networkA, 1u)
        eventually(listOf<UShort>(1u)) { storage.readScenes(networkA) }

        // Reading a state of a network without any stored scenes creates a default entry for it.
        assertEquals(false, storage.readGenericOnOffState(networkB, 2u, elementIndex = 0))

        eventually(listOf<UShort>(2u)) { storage.readScenes(networkB) }
        assertEquals(listOf<UShort>(1u), storage.readScenes(networkA))
    }
}
