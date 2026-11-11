package no.nordicsemi.kotlin.mesh.core.layers.foundation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.SecurePropertiesStorage
import no.nordicsemi.kotlin.mesh.core.Storage
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionTimer
import no.nordicsemi.kotlin.mesh.core.model.Element
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.Location
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

class HealthServerHandlerTest {
    private val model: Model
    private val events = CopyOnWriteArrayList<HealthAttentionTimer>()
    private var stopped = CountDownLatch(1)
    private val handler = HealthServerHandler(
        triggerAttentionTimer = {
            events += it
            if (it is HealthAttentionTimer.Stop) stopped.countDown()
        }
    )

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
        manager.localElements = listOf(Element(location = Location.FIRST))
        model = manager.localElements.flatMap { it.models }.first { it.isHealthServer }
    }

    private suspend fun attention(seconds: UByte) = handler.handle(
        ModelEvent.UnacknowledgedMessageReceived(
            model = model,
            message = HealthAttentionSetUnacknowledged(attentionTimer = seconds),
            source = UnicastAddress(address = 0x0100).address,
            destination = UnicastAddress(address = 0x0001)
        )
    )

    @Test
    fun testAttentionSetStartsTheAttentionTimer() = runBlocking {
        attention(seconds = 3u)

        assertEquals(listOf<HealthAttentionTimer>(HealthAttentionTimer.Start(3.seconds)), events)
    }

    @Test
    fun testAttentionTimerStopsWhenItExpires() = runBlocking {
        attention(seconds = 1u)

        assertTrue(stopped.await(3, TimeUnit.SECONDS), "Attention timer did not stop")
        assertEquals(
            listOf(HealthAttentionTimer.Start(1.seconds), HealthAttentionTimer.Stop),
            events
        )
    }

    @Test
    fun testRepeatedAttentionSetRestartsTheAttentionTimer() = runBlocking {
        // Used to fail with "Timer already cancelled".
        attention(seconds = 3u)
        attention(seconds = 2u)

        assertEquals(
            listOf<HealthAttentionTimer>(
                HealthAttentionTimer.Start(3.seconds),
                HealthAttentionTimer.Start(2.seconds)
            ),
            events
        )
    }

    @Test
    fun testAttentionSetCanBeSentAgainAfterTheTimerExpired() = runBlocking {
        attention(seconds = 1u)
        assertTrue(stopped.await(3, TimeUnit.SECONDS), "Attention timer did not stop")
        stopped = CountDownLatch(1)

        attention(seconds = 1u)

        assertTrue(stopped.await(3, TimeUnit.SECONDS), "Attention timer did not stop again")
        assertEquals(
            listOf(
                HealthAttentionTimer.Start(1.seconds),
                HealthAttentionTimer.Stop,
                HealthAttentionTimer.Start(1.seconds),
                HealthAttentionTimer.Stop
            ),
            events
        )
    }

    @Test
    fun testAttentionSetWithZeroDurationStopsTheTimerWithoutAnotherStopLater() = runBlocking {
        attention(seconds = 1u)
        attention(seconds = 0u)

        // The cancelled timer must not fire a second Stop once its delay has elapsed.
        Thread.sleep(1500)
        assertEquals(
            listOf(HealthAttentionTimer.Start(1.seconds), HealthAttentionTimer.Stop),
            events
        )
    }

    private object NoStorage : Storage {
        override suspend fun load() = ByteArray(0)
        override suspend fun save(network: ByteArray) = Unit
    }

    private object NoSecureProperties : SecurePropertiesStorage {
        override suspend fun ivIndex(uuid: Uuid): IvIndex = TODO("Not needed by the tests")
        override suspend fun storeIvIndex(uuid: Uuid, ivIndex: IvIndex) = Unit
        override suspend fun nextSequenceNumber(uuid: Uuid, address: UnicastAddress): UInt = 0u
        override suspend fun storeNextSequenceNumber(
            uuid: Uuid,
            address: UnicastAddress,
            sequenceNumber: UInt,
        ) = Unit

        override suspend fun resetSequenceNumber(uuid: Uuid, address: UnicastAddress) = Unit
        override suspend fun lastSeqAuthValue(uuid: Uuid, source: UnicastAddress): ULong? = null
        override fun storeLastSeqAuthValue(uuid: Uuid, source: UnicastAddress, lastSeqAuth: ULong) =
            Unit

        override suspend fun previousSeqAuthValue(uuid: Uuid, source: UnicastAddress): ULong? = null
        override fun storePreviousSeqAuthValue(uuid: Uuid, source: UnicastAddress, seqAuth: ULong) =
            Unit

        override suspend fun storeLocalProvisioner(uuid: Uuid, localProvisionerUuid: Uuid) = Unit
        override suspend fun localProvisioner(uuid: Uuid): String? = null
    }
}
