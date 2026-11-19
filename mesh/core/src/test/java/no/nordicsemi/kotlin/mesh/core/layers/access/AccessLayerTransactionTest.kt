package no.nordicsemi.kotlin.mesh.core.layers.access

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager
import no.nordicsemi.kotlin.mesh.core.SecurePropertiesStorage
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.TransactionMessage
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDeltaSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnOffSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.Element
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.TestStorage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Tests that [AccessLayer.send] assigns the transaction identifier of [TransactionMessage]s as
 * expected. The messages are not delivered anywhere as the manager has no bearer.
 */
class AccessLayerTransactionTest {

    private class TestClock(var current: Instant = Instant.fromEpochSeconds(1_000)) : Clock {
        override fun now() = current
        fun advance(duration: Duration) {
            current += duration
        }
    }

    private val clock = TestClock()
    private val manager = MeshNetworkManager(
        storage = TestStorage(),
        secureProperties = CountingSecureProperties(),
        ioDispatcher = Dispatchers.Unconfined
    )
    private val network = runBlocking {
        manager.import(javaClass.classLoader.getResourceAsStream("cdb_json.json")!!.readAllBytes())
    }
    private val accessLayer = AccessLayer(networkManager = manager.networkManager!!, clock = clock)
    private val element: Element = network.nodes.first { it.name == "Bedside Light (Brian)" }
        .elements.first()
    private val applicationKey: ApplicationKey = network.applicationKeys.first()
    private val group = GroupAddress(0xC024u)

    private fun send(
        message: MeshMessage,
        destination: MeshAddress = group,
        retransmit: Boolean = false,
    ) = runBlocking {
        accessLayer.send(
            message = message,
            element = element,
            destination = destination,
            ttl = null,
            applicationKey = applicationKey,
            retransmit = retransmit
        )
        assertNotNull((message as TransactionMessage).tid)
    }

    private fun delta() = GenericDeltaSet(tid = null, delta = 1)
    private fun onOff() = GenericOnOffSetUnacknowledged(tid = null, on = true)

    @Test
    fun `messages continuing a transaction share the tid`() {
        val first = delta()
        val second = delta()
        send(first)
        clock.advance(1.seconds)
        send(second)
        assertEquals(first.tid, second.tid)
    }

    @Test
    fun `messages not continuing a transaction get new tids`() {
        val first = onOff()
        val second = onOff()
        send(first)
        send(second)
        assertNotEquals(first.tid, second.tid)
    }

    @Test
    fun `continuing message sent after the transaction expired gets a new tid`() {
        val first = delta()
        val second = delta()
        send(first)
        clock.advance(7.seconds)
        send(second)
        assertNotEquals(first.tid, second.tid)
    }

    @Test
    fun `transaction stays alive while messages keep continuing it`() {
        val tids = (1..4).map {
            delta().also {
                send(it)
                clock.advance(4.seconds)
            }.tid
        }
        assertEquals(1, tids.toSet().size)
    }

    @Test
    fun `retransmission reuses the tid of the previous message`() {
        val first = onOff()
        val retransmission = onOff()
        send(first)
        clock.advance(1.seconds)
        send(retransmission, retransmit = true)
        assertEquals(first.tid, retransmission.tid)
    }

    @Test
    fun `retransmission of an expired transaction gets a new tid`() {
        val first = onOff()
        val retransmission = onOff()
        send(first)
        clock.advance(7.seconds)
        send(retransmission, retransmit = true)
        assertNotEquals(first.tid, retransmission.tid)
    }

    @Test
    fun `transactions are tracked per destination`() {
        val toGroup = delta()
        val toOther = delta()
        send(toGroup)
        send(toOther, destination = GroupAddress(0xC025u))
        // Each destination has its own random initial TID, so only check that the first message to
        // the group is still continued afterwards.
        val again = delta()
        send(again)
        assertEquals(toGroup.tid, again.tid)
    }

    @Test
    fun `message with a tid keeps it`() {
        val message = GenericDeltaSet(tid = 42u, delta = 1)
        send(message)
        assertEquals(42u.toUByte(), message.tid)
    }

    /** Hands out increasing sequence numbers and ignores everything else. */
    private class CountingSecureProperties : SecurePropertiesStorage {
        private var sequence = 0u
        override suspend fun ivIndex(uuid: Uuid): IvIndex = IvIndex()
        override suspend fun storeIvIndex(uuid: Uuid, ivIndex: IvIndex) = Unit
        override suspend fun nextSequenceNumber(uuid: Uuid, address: UnicastAddress): UInt =
            sequence++

        override suspend fun storeNextSequenceNumber(
            uuid: Uuid,
            address: UnicastAddress,
            sequenceNumber: UInt,
        ) = Unit

        override suspend fun resetSequenceNumber(uuid: Uuid, address: UnicastAddress) = Unit
        override suspend fun lastSeqAuthValue(uuid: Uuid, source: UnicastAddress): ULong? = null
        override fun storeLastSeqAuthValue(
            uuid: Uuid,
            source: UnicastAddress,
            lastSeqAuth: ULong,
        ) = Unit

        override suspend fun previousSeqAuthValue(uuid: Uuid, source: UnicastAddress): ULong? = null
        override fun storePreviousSeqAuthValue(
            uuid: Uuid,
            source: UnicastAddress,
            seqAuth: ULong,
        ) = Unit

        override suspend fun storeLocalProvisioner(uuid: Uuid, localProvisionerUuid: Uuid) = Unit
        override suspend fun localProvisioner(uuid: Uuid): String? = null
    }
}
