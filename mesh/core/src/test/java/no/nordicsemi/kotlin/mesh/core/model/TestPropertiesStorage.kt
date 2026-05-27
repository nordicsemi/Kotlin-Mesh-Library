@file:OptIn(ExperimentalUuidApi::class)

package no.nordicsemi.kotlin.mesh.core.model

import no.nordicsemi.kotlin.mesh.core.SecurePropertiesStorage
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Mocked for tests.
 */
internal class TestPropertiesStorage : SecurePropertiesStorage {
    override suspend fun ivIndex(uuid: Uuid): IvIndex {
        TODO("Not yet implemented")
    }

    override suspend fun storeIvIndex(uuid: Uuid, ivIndex: IvIndex) {
        TODO("Not yet implemented")
    }

    override suspend fun nextSequenceNumber(uuid: Uuid, address: UnicastAddress): UInt {
        TODO("Not yet implemented")
    }

    override suspend fun storeNextSequenceNumber(
        uuid: Uuid,
        address: UnicastAddress,
        sequenceNumber: UInt
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun resetSequenceNumber(uuid: Uuid, address: UnicastAddress) {
        TODO("Not yet implemented")
    }

    override suspend fun lastSeqAuthValue(uuid: Uuid, source: UnicastAddress): ULong? {
        TODO("Not yet implemented")
    }

    override fun storeLastSeqAuthValue(
        uuid: Uuid,
        source: UnicastAddress,
        lastSeqAuth: ULong
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun previousSeqAuthValue(uuid: Uuid, source: UnicastAddress): ULong? {
        TODO("Not yet implemented")
    }

    override fun storePreviousSeqAuthValue(
        uuid: Uuid,
        source: UnicastAddress,
        seqAuth: ULong
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun storeLocalProvisioner(
        uuid: Uuid,
        localProvisionerUuid: Uuid,
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun localProvisioner(uuid: Uuid): String? {
        TODO("Not yet implemented")
    }
}