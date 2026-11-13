package no.nordicsemi.android.nrfmesh.feature.quickconfiguration

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nordicsemi.android.nrfmesh.core.data.configurator.ConfigTask
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager
import no.nordicsemi.kotlin.mesh.core.SecurePropertiesStorage
import no.nordicsemi.kotlin.mesh.core.Storage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppBind
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelSubscriptionAdd
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class QuickConfigurationTasksTest {

    private val network: MeshNetwork = runBlocking {
        MeshNetworkManager(
            storage = NoStorage,
            secureProperties = NoSecureProperties,
            ioDispatcher = Dispatchers.Unconfined
        ).import(
            checkNotNull(javaClass.classLoader?.getResourceAsStream("cdb_json.json")).readBytes()
        )
    }

    private val node: Node get() = network.nodes.first { it.name == "Bedroom Light" }
    private val models: List<Model> get() = node.elements.flatMap { it.models }
    private fun key(index: Int): ApplicationKey =
        network.applicationKeys.first { it.index.toInt() == index }

    private fun List<*>.configTasks() = filterIsInstance<ConfigTask>()

    @Test
    fun bindSkipsKeysAlreadyBoundToKnownNode() {
        // The node knows key 0 and has it bound to all models that support binding.
        val bindable = models.filter { it.supportsApplicationKeyBinding && it.bind.contains(0.toUShort()) }
        val tasks = bindTasks(node = node, keys = listOf(key(0)), models = bindable)
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun bindSendsKeyAndBindsWhenNodeDoesNotKnowKey() {
        val unknown = key(2)
        assertTrue(!node.knows(unknown))
        val model = models.first { it.supportsApplicationKeyBinding }
        val tasks = bindTasks(node = node, keys = listOf(unknown), models = listOf(model))
            .configTasks()
        // Key tasks come first, bind last.
        assertTrue(tasks.size >= 2)
        assertTrue(tasks.last().message is ConfigModelAppBind)
        assertEquals(1, tasks.count { it.message is ConfigModelAppBind })
    }

    @Test
    fun subscribeSkipsFixedGroupsAlreadySubscribed() {
        val model = models.first { it.supportsModelSubscription }
        val group = network.groups.first()
        val tasks = subscribeTasks(node = node, groups = listOf(group), models = listOf(model))
            .configTasks()
        val expected = if (model.isSubscribedTo(group)) 0 else 1
        assertEquals(expected, tasks.size)
        tasks.forEach { assertTrue(it.message is ConfigModelSubscriptionAdd) }
    }

    @Test
    fun alreadySubscribedReportsExistingSubscriptions() {
        val (model, group) = network.nodes.flatMap { it.elements }.flatMap { it.models }
            .filter { it.supportsModelSubscription }
            .firstNotNullOf { m -> network.groups.firstOrNull { m.isSubscribedTo(it) }?.let { m to it } }
        val reported = alreadySubscribed(groups = listOf(group), models = listOf(model))
        assertEquals(1, reported.size)
        assertEquals(group.name, reported.first().itemName)
        assertTrue(subscribeTasks(node = node, groups = listOf(group), models = listOf(model)).isEmpty())
    }

    @Test
    fun publishCreatesOnePublicationTaskPerModel() {
        val selected = models.filter { it.supportsModelPublication }.take(2)
        val group = network.groups.first()
        val tasks = publishTasks(
            node = node,
            key = key(0),
            destination = group.address as no.nordicsemi.kotlin.mesh.core.model.PublicationAddress,
            destinationName = group.name,
            models = selected
        ).configTasks()
        assertEquals(selected.size, tasks.count { it.message is ConfigModelPublicationSet })
    }

    @Test
    fun describeFallsBackToHexForUnknownAddress() {
        val address = UnicastAddress(address = 0x7000.toUShort())
        assertEquals(address.toHexString(), describe(address, network))
    }

    @Test
    fun describeUsesNodeName() {
        val address = node.primaryUnicastAddress
        assertEquals(node.name, describe(address, network))
    }
}

private object NoStorage : Storage {
    override suspend fun load() = ByteArray(0)
    override suspend fun save(network: ByteArray) = Unit
}

private object NoSecureProperties : SecurePropertiesStorage {
    override suspend fun ivIndex(uuid: Uuid): IvIndex = TODO()
    override suspend fun storeIvIndex(uuid: Uuid, ivIndex: IvIndex) = Unit
    override suspend fun nextSequenceNumber(uuid: Uuid, address: UnicastAddress): UInt = 0u
    override suspend fun storeNextSequenceNumber(uuid: Uuid, address: UnicastAddress, sequenceNumber: UInt) = Unit
    override suspend fun resetSequenceNumber(uuid: Uuid, address: UnicastAddress) = Unit
    override suspend fun lastSeqAuthValue(uuid: Uuid, source: UnicastAddress): ULong? = null
    override fun storeLastSeqAuthValue(uuid: Uuid, source: UnicastAddress, lastSeqAuth: ULong) = Unit
    override suspend fun previousSeqAuthValue(uuid: Uuid, source: UnicastAddress): ULong? = null
    override fun storePreviousSeqAuthValue(uuid: Uuid, source: UnicastAddress, seqAuth: ULong) = Unit
    override suspend fun storeLocalProvisioner(uuid: Uuid, localProvisionerUuid: Uuid) = Unit
    override suspend fun localProvisioner(uuid: Uuid): String? = null
}
