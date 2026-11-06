package no.nordicsemi.android.nrfmesh.core.data.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nordicsemi.android.nrfmesh.core.data.storage.GenericLevelStateStorage
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDeltaSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDeltaSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericMoveSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericMoveSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.model.Element
import no.nordicsemi.kotlin.mesh.core.model.Location
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.uuid.Uuid

/**
 * Generic Level state kept in memory.
 */
private class InMemoryGenericLevelStorage : GenericLevelStateStorage {
    val levels = mutableMapOf<Pair<SceneNumber, Int>, UShort>()

    override fun storeGenericLevelState(
        uuid: Uuid,
        sceneNumber: SceneNumber,
        elementIndex: Int,
        level: UShort,
    ) {
        levels[sceneNumber to elementIndex] = level
    }

    override suspend fun readGenericLevelState(
        uuid: Uuid,
        sceneNumber: SceneNumber,
        elementIndex: Int,
    ) = levels[sceneNumber to elementIndex]
}

class GenericLevelServerTest {
    private val storage = InMemoryGenericLevelStorage()
    private val defaultTransitionTimeServer = GenericDefaultTransitionTimeServer()
    private val server = GenericLevelServer(
        ioDispatcher = Dispatchers.Unconfined,
        storage = storage,
        defaultTransitionTimeServer = defaultTransitionTimeServer
    )
    private val model = Model(
        modelId = SigModelId(modelIdentifier = Model.GENERIC_LEVEL_SERVER_MODEL_ID),
        handler = server
    )

    /**
     * The last state reported to the observer.
     */
    private var observed: GenericState<Short>? = null

    init {
        val json = checkNotNull(
            javaClass.classLoader?.getResourceAsStream("cdb_json.json")?.readAllBytes()
        )
        val manager = MeshNetworkManager(
            storage = NoStorage,
            secureProperties = NoSecureProperties,
            ioDispatcher = Dispatchers.Unconfined
        )
        runBlocking { manager.import(json) }
        manager.localElements = listOf(
            Element(
                _name = "Primary Element",
                location = Location.FIRST,
                _models = mutableListOf(model)
            )
        )
        server.observer = { observed = it }
    }

    private suspend fun send(message: AcknowledgedMeshMessage) = server.handle(
        ModelEvent.AcknowledgedMessageReceived(
            model = model,
            request = message,
            source = SOURCE.address,
            destination = DESTINATION
        )
    )

    private suspend fun send(message: UnacknowledgedMeshMessage) = server.handle(
        ModelEvent.UnacknowledgedMessageReceived(
            model = model,
            message = message,
            source = SOURCE.address,
            destination = DESTINATION
        )
    )

    private suspend fun level() = (send(GenericLevelGet()) as GenericLevelStatus).level

    @Test
    fun testLevelIsMinimumInitially() = runBlocking {
        val status = send(GenericLevelGet())
        assertIs<GenericLevelStatus>(status)
        assertEquals(Short.MIN_VALUE, status.level)
        assertNull(status.targetLevel)
        assertNull(status.remainingTime)
    }

    @Test
    fun testSetChangesTheLevelAndRespondsWithStatus() = runBlocking {
        val status = send(GenericLevelSet(tid = 1u, level = 1234))
        assertIs<GenericLevelStatus>(status)
        assertEquals(1234, status.level.toInt())
        assertNull(status.targetLevel)
        assertEquals(1234, level().toInt())
        assertEquals(1234, observed?.value?.toInt())
    }

    @Test
    fun testSetUnacknowledgedChangesTheLevelWithoutAResponse() = runBlocking {
        assertNull(send(GenericLevelSetUnacknowledged(tid = 1u, level = -500)))
        assertEquals(-500, level().toInt())
    }

    @Test
    fun testSetWithTransitionReportsTargetAndRemainingTime() = runBlocking {
        val status = send(
            GenericLevelSet(
                tid = 1u,
                level = 1000,
                transitionParams = TransitionParameters(
                    transitionTime = TransitionTime(rawValue = 0x45u), // 5 seconds
                    delay = 0u
                )
            )
        )
        assertIs<GenericLevelStatus>(status)
        // The level has not changed yet, as the transition is in progress.
        assertEquals(Short.MIN_VALUE, status.level)
        assertEquals(1000, status.targetLevel?.toInt())
        assertNotNull(status.remainingTime)
        assertEquals(1000, observed?.transition?.targetValue?.toInt())
    }

    @Test
    fun testDefaultTransitionTimeIsUsedWhenNoneIsGiven() = runBlocking {
        defaultTransitionTimeServer.defaultTransitionTime = TransitionTime(rawValue = 0x45u)
        val status = send(GenericLevelSet(tid = 1u, level = 1000))
        assertIs<GenericLevelStatus>(status)
        assertEquals(Short.MIN_VALUE, status.level)
        assertEquals(1000, status.targetLevel?.toInt())
    }

    @Test
    fun testDeltaIsAddedToTheCurrentLevel() = runBlocking {
        send(GenericLevelSet(tid = 1u, level = 100))
        val status = send(GenericDeltaSet(tid = 2u, delta = 50))
        assertIs<GenericLevelStatus>(status)
        assertEquals(150, status.level.toInt())

        send(GenericDeltaSetUnacknowledged(tid = 3u, delta = -200))
        assertEquals(-50, level().toInt())
    }

    @Test
    fun testDeltaWrapsAroundTheSignedRange() = runBlocking {
        send(GenericLevelSet(tid = 1u, level = Short.MAX_VALUE))
        send(GenericDeltaSet(tid = 2u, delta = 1))
        assertEquals(Short.MIN_VALUE, level())
    }

    @Test
    fun testMoveStartsAnAnimationWithoutChangingTheLevel() = runBlocking {
        send(GenericLevelSet(tid = 1u, level = 0))
        val status = send(
            GenericMoveSet(
                tid = 2u,
                deltaLevel = 1000,
                transitionParams = TransitionParameters(
                    transitionTime = TransitionTime(rawValue = 0x45u), // 5 seconds
                    delay = 0u
                )
            )
        )
        assertIs<GenericLevelStatus>(status)
        assertEquals(0, status.level.toInt())
        assertNull(status.targetLevel)
        val animation = observed?.animation
        assertNotNull(animation)
        // Change of the level per second.
        assertEquals(200, animation.speed.toInt())
    }

    @Test
    fun testMoveWithZeroDeltaStopsTheAnimation() = runBlocking {
        val fiveSeconds = TransitionParameters(
            transitionTime = TransitionTime(rawValue = 0x45u),
            delay = 0u
        )
        send(GenericMoveSetUnacknowledged(tid = 1u, deltaLevel = 1000, transitionParams = fiveSeconds))
        assertNotNull(observed?.animation)

        send(GenericMoveSetUnacknowledged(tid = 2u, deltaLevel = 0, transitionParams = fiveSeconds))
        assertNull(observed?.animation)
    }

    @Test
    fun testSetCancelsAnOngoingAnimation() = runBlocking {
        val fiveSeconds = TransitionParameters(
            transitionTime = TransitionTime(rawValue = 0x45u),
            delay = 0u
        )
        send(GenericMoveSet(tid = 1u, deltaLevel = 1000, transitionParams = fiveSeconds))
        assertNotNull(observed?.animation)

        send(GenericLevelSet(tid = 2u, level = 10))
        assertNull(observed?.animation)
        assertEquals(10, level().toInt())
    }

    @Test
    fun testPublicationComposerReportsTheCurrentLevel() = runBlocking {
        send(GenericLevelSet(tid = 1u, level = 42))
        val published = server.publicationMessageComposer.invoke()
        assertIs<GenericLevelStatus>(published)
        assertEquals(42, published.level.toInt())
    }

    @Test
    fun testLevelIsStoredWithAScene() = runBlocking {
        send(GenericLevelSet(tid = 1u, level = -1))
        server.store(scene = 3u)
        // Stored as the unsigned bit pattern of the level.
        assertEquals(0xFFFF.toUShort(), storage.levels[3.toUShort() to 0])
    }

    @Test
    fun testLevelIsRecalledFromAScene() = runBlocking {
        send(GenericLevelSet(tid = 1u, level = 700))
        server.store(scene = 3u)
        send(GenericLevelSet(tid = 2u, level = -20))

        server.recall(scene = 3u, transitionTime = null, delay = null)
        assertEquals(700, level().toInt())
        assertEquals(true, observed?.storedWithScene)
    }

    @Test
    fun testRecallOfASceneWithoutAStoredLevelDoesNothing() = runBlocking {
        send(GenericLevelSet(tid = 1u, level = 700))

        server.recall(scene = 9u, transitionTime = null, delay = null)
        assertEquals(700, level().toInt())
    }

    private companion object {
        val SOURCE = UnicastAddress(address = 0x0100)
        val DESTINATION = UnicastAddress(address = 0x0001)
    }
}
