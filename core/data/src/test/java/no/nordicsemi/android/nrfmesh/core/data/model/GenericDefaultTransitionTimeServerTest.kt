package no.nordicsemi.android.nrfmesh.core.data.model

import kotlinx.coroutines.runBlocking
import no.nordicsemi.android.nrfmesh.core.data.storage.GenericDefaultTransitionTimeStorage
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeStatus
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.StepResolution
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

/** Default Transition Time kept in memory. */
private class InMemoryDefaultTransitionTimeStorage : GenericDefaultTransitionTimeStorage {
    val values = mutableMapOf<Uuid, UByte>()

    override fun storeDefaultTransitionTime(uuid: Uuid, rawValue: UByte) {
        values[uuid] = rawValue
    }

    override suspend fun readDefaultTransitionTime(uuid: Uuid) = values[uuid]
}

/** Tests the Generic Default Transition Time Server of the local node. */
class GenericDefaultTransitionTimeServerTest {
    private val server = GenericDefaultTransitionTimeServer()
    private val model = Model(
        modelId = SigModelId(modelIdentifier = Model.GENERIC_DEFAULT_TRANSITION_TIME_SERVER_MODEL_ID),
        handler = server
    )
    private val address = UnicastAddress(address = 0x0001u)
    private val time = TransitionTime(steps = 5u, stepResolution = StepResolution.SECONDS)

    private suspend fun send(message: AcknowledgedMeshMessage) = server.handle(
        ModelEvent.AcknowledgedMessageReceived(
            model = model,
            request = message,
            source = address.address,
            destination = address
        )
    )

    private suspend fun send(message: UnacknowledgedMeshMessage) = server.handle(
        ModelEvent.UnacknowledgedMessageReceived(
            model = model,
            message = message,
            source = address.address,
            destination = address
        )
    )

    @Test
    fun testSupportsGetSetAndSetUnacknowledged() {
        assertTrue(server.isSubscriptionSupported)
        assertEquals(
            setOf(
                GenericDefaultTransitionTimeGet.opCode,
                GenericDefaultTransitionTimeSet.opCode,
                GenericDefaultTransitionTimeSetUnacknowledged.opCode
            ),
            server.messageTypes.keys
        )
    }

    @Test
    fun testGetRespondsWithTheCurrentTime() = runBlocking {
        val status = send(GenericDefaultTransitionTimeGet())
        assertIs<GenericDefaultTransitionTimeStatus>(status)
        assertEquals(TransitionTime(rawValue = 0u), status.transitionTime)
    }

    @Test
    fun testSetChangesTheTimeAndRespondsWithStatus() = runBlocking {
        val status = send(GenericDefaultTransitionTimeSet(transitionTime = time))
        assertIs<GenericDefaultTransitionTimeStatus>(status)
        assertEquals(time, status.transitionTime)
        assertEquals(time, server.defaultTransitionTime)
    }

    @Test
    fun testSetUnacknowledgedChangesTheTimeWithoutAResponse() = runBlocking {
        val response = send(GenericDefaultTransitionTimeSetUnacknowledged(transitionTime = time))
        assertNull(response)
        assertEquals(time, server.defaultTransitionTime)
    }

    @Test
    fun testSetToTheUnknownTimeIsRejected() {
        val unknown = TransitionTime(rawValue = 0x3Fu)
        assertFailsWith<ModelError.InvalidMessage> {
            runBlocking { send(GenericDefaultTransitionTimeSet(transitionTime = unknown)) }
        }
        assertEquals(TransitionTime(rawValue = 0u), server.defaultTransitionTime)
    }

    @Test
    fun testSetUnacknowledgedToTheUnknownTimeIsIgnored() = runBlocking {
        send(GenericDefaultTransitionTimeSetUnacknowledged(transitionTime = time))
        send(GenericDefaultTransitionTimeSetUnacknowledged(TransitionTime(rawValue = 0x3Fu)))
        assertEquals(time, server.defaultTransitionTime)
    }

    @Test
    fun testTimeIsStoredWhenItChanges() = runBlocking {
        val storage = InMemoryDefaultTransitionTimeStorage()
        val uuid = Uuid.random()
        val persisted = GenericDefaultTransitionTimeServer(storage = storage, uuid = uuid)
        persisted.defaultTransitionTime = time
        assertEquals(time.rawValue, storage.values[uuid])
    }

    @Test
    fun testTimeIsRestoredByANewServer() = runBlocking {
        val storage = InMemoryDefaultTransitionTimeStorage()
        val uuid = Uuid.random()
        GenericDefaultTransitionTimeServer(storage = storage, uuid = uuid)
            .defaultTransitionTime = time

        val restarted = GenericDefaultTransitionTimeServer(storage = storage, uuid = uuid)
        assertEquals(TransitionTime(rawValue = 0u), restarted.defaultTransitionTime)
        restarted.restore()
        assertEquals(time, restarted.defaultTransitionTime)
    }

    @Test
    fun testTimeOfAnotherNetworkIsNotRestored() = runBlocking {
        val storage = InMemoryDefaultTransitionTimeStorage()
        GenericDefaultTransitionTimeServer(storage = storage, uuid = Uuid.random())
            .defaultTransitionTime = time

        val other = GenericDefaultTransitionTimeServer(storage = storage, uuid = Uuid.random())
        other.restore()
        assertEquals(TransitionTime(rawValue = 0u), other.defaultTransitionTime)
    }
}
