package no.nordicsemi.kotlin.mesh.core.layers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager
import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.SecurePropertiesStorage
import no.nordicsemi.kotlin.mesh.core.Storage
import no.nordicsemi.kotlin.mesh.core.TransactionHelper
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.TransactionMessage
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDeltaSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDeltaSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericMoveSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericMoveSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.model.Element
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.Location
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.uuid.Uuid

/**
 * Sends Generic Delta and Move messages from the local node to itself through the whole stack
 * (access, transport and network layers loop the message back).
 */
class LocalNodeLoopbackTest {

    /** Records the messages received by a local Generic Level Server. */
    private class RecordingLevelServer : ModelEventHandler() {
        override val messageTypes: Map<UInt, HasInitializer> = mapOf(
            GenericDeltaSet.opCode to GenericDeltaSet.Initializer,
            GenericDeltaSetUnacknowledged.opCode to GenericDeltaSetUnacknowledged.Initializer,
            GenericMoveSet.opCode to GenericMoveSet.Initializer,
            GenericMoveSetUnacknowledged.opCode to GenericMoveSetUnacknowledged.Initializer
        )
        override val isSubscriptionSupported = false
        override val publicationMessageComposer: MessageComposer? = null

        private val helper = TransactionHelper()
        val received = mutableListOf<MeshMessage>()
        val newTransaction = mutableListOf<Boolean>()

        override suspend fun handle(event: ModelEvent): MeshResponse? {
            val (message, source, destination) = when (event) {
                is ModelEvent.AcknowledgedMessageReceived ->
                    Triple(event.request, event.source, event.destination)

                is ModelEvent.UnacknowledgedMessageReceived ->
                    Triple(event.message, event.source, event.destination)

                else -> return null
            }
            received += message
            newTransaction += helper.isNewTransaction(
                message = message as TransactionMessage,
                source = source,
                destination = destination
            )
            return if (event is ModelEvent.AcknowledgedMessageReceived) {
                GenericLevelStatus(level = 0)
            } else null
        }
    }

    private val server = RecordingLevelServer()
    private val manager = MeshNetworkManager(
        storage = NoStorage,
        secureProperties = LoopbackSecureProperties(),
        ioDispatcher = Dispatchers.Unconfined
    )
    private val localAddress = UnicastAddress(address = 0x0001)

    private val model = Model(
        modelId = SigModelId(modelIdentifier = Model.GENERIC_LEVEL_SERVER_MODEL_ID),
        handler = server
    )

    init {
        val json = checkNotNull(
            javaClass.classLoader?.getResourceAsStream("cdb_json.json")?.readAllBytes()
        )
        runBlocking { manager.import(json) }
        manager.localElements = listOf(
            Element(
                _name = "Primary Element",
                location = Location.FIRST,
                _models = mutableListOf(model)
            )
        )
        model.bind(index = 0.toUShort())
    }

    private val applicationKey get() = manager.network!!.applicationKeys.first { it.index == 0.toUShort() }

    private suspend fun send(message: MeshMessage) = manager.send(
        message = message,
        localElement = null,
        destination = localAddress,
        initialTtl = null,
        applicationKey = applicationKey
    )

    @Test
    fun testUnacknowledgedDeltaIsDeliveredToTheLocalServer() = runBlocking {
        send(GenericDeltaSetUnacknowledged(delta = 123))
        assertEquals(1, server.received.size)
        assertEquals(123, assertIs<GenericDeltaSetUnacknowledged>(server.received[0]).delta)
    }

    @Test
    fun testUnacknowledgedMoveIsDeliveredToTheLocalServer() = runBlocking {
        send(GenericMoveSetUnacknowledged(tid = null, deltaLevel = 250, transitionParams = null))
        assertEquals(1, server.received.size)
        assertEquals(250, assertIs<GenericMoveSetUnacknowledged>(server.received[0]).deltaLevel.toInt())
    }

    @Test
    fun testAcknowledgedDeltaIsDeliveredToTheLocalServer() = runBlocking {
        send(GenericDeltaSet(delta = -50))
        assertEquals(-50, assertIs<GenericDeltaSet>(server.received[0]).delta)
    }

    @Test
    fun testAcknowledgedMoveIsDeliveredToTheLocalServer() = runBlocking {
        send(GenericMoveSet(tid = null, deltaLevel = -9, transitionParams = null))
        assertEquals(-9, assertIs<GenericMoveSet>(server.received[0]).deltaLevel.toInt())
    }

    @Test
    fun testRepeatedMoveIsAContinuationOfTheTransaction() = runBlocking {
        send(GenericMoveSetUnacknowledged(tid = 9u, deltaLevel = 1, transitionParams = null))
        send(GenericMoveSetUnacknowledged(tid = 9u, deltaLevel = 2, transitionParams = null))
        assertEquals(listOf(true, false), server.newTransaction)
    }

    private object NoStorage : Storage {
        override suspend fun load() = ByteArray(0)
        override suspend fun save(network: ByteArray) = Unit
    }

    private class LoopbackSecureProperties : SecurePropertiesStorage {
        private var sequence = 0u
        override suspend fun ivIndex(uuid: Uuid): IvIndex = IvIndex()
        override suspend fun storeIvIndex(uuid: Uuid, ivIndex: IvIndex) = Unit
        override suspend fun nextSequenceNumber(uuid: Uuid, address: UnicastAddress): UInt = sequence
        override suspend fun storeNextSequenceNumber(
            uuid: Uuid,
            address: UnicastAddress,
            sequenceNumber: UInt,
        ) {
            sequence = sequenceNumber
        }

        override suspend fun resetSequenceNumber(uuid: Uuid, address: UnicastAddress) {
            sequence = 0u
        }

        override suspend fun lastSeqAuthValue(uuid: Uuid, source: UnicastAddress): ULong? = null
        override fun storeLastSeqAuthValue(uuid: Uuid, source: UnicastAddress, lastSeqAuth: ULong) = Unit
        override suspend fun previousSeqAuthValue(uuid: Uuid, source: UnicastAddress): ULong? = null
        override fun storePreviousSeqAuthValue(uuid: Uuid, source: UnicastAddress, seqAuth: ULong) = Unit
        override suspend fun storeLocalProvisioner(uuid: Uuid, localProvisionerUuid: Uuid) = Unit
        override suspend fun localProvisioner(uuid: Uuid): String? = null
    }
}
