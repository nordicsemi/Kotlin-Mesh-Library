package no.nordicsemi.kotlin.mesh.core.layers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager
import no.nordicsemi.kotlin.mesh.core.SecurePropertiesStorage
import no.nordicsemi.kotlin.mesh.core.Storage
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.uuid.Uuid

/**
 * Resetting the local node clears the network of the [MeshNetworkManager] while the
 * [NetworkManager] may still be processing the message that triggered the reset.
 */
class NetworkManagerResetTest {

    private fun manager() = MeshNetworkManager(
        storage = NoStorage,
        secureProperties = NoSecureProperties,
        ioDispatcher = Dispatchers.Unconfined
    )

    @Test
    fun testNetworkManagerKeepsItsNetworkWhenTheNetworkIsCleared() = runBlocking {
        val manager = manager()
        val network = manager.create()
        val networkManager = assertNotNull(manager.networkManager)

        manager.clear()

        // This used to throw a NullPointerException.
        assertSame(network, networkManager.meshNetwork)
    }

    @Test
    fun testClearForgetsTheNetwork() = runBlocking {
        val manager = manager()
        manager.create()

        manager.clear()

        assertNull(manager.network)
        assertNull(manager.networkManager)
    }

    private object NoStorage : Storage {
        override suspend fun load() = ByteArray(0)
        override suspend fun save(network: ByteArray) = Unit
    }

    private object NoSecureProperties : SecurePropertiesStorage {
        override suspend fun ivIndex(uuid: Uuid): IvIndex = IvIndex()
        override suspend fun storeIvIndex(uuid: Uuid, ivIndex: IvIndex) = Unit
        override suspend fun nextSequenceNumber(uuid: Uuid, address: UnicastAddress): UInt = 0u
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
