@file:Suppress("unused")

package no.nordicsemi.android.nrfmesh.core.data.storage

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.preferences.protobuf.InvalidProtocolBufferException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nordicsemi.android.nrfmesh.core.common.di.IoDispatcher
import no.nordicsemi.android.nrfmesh.core.data.ProtoIvIndex
import no.nordicsemi.android.nrfmesh.core.data.ProtoSecureProperties
import no.nordicsemi.android.nrfmesh.core.data.ProtoSecurePropertiesMap
import no.nordicsemi.android.nrfmesh.core.data.ProtoSeqAuth
import no.nordicsemi.kotlin.mesh.core.SecurePropertiesStorage
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import java.io.InputStream
import java.io.OutputStream
import kotlin.uuid.Uuid
import javax.inject.Inject
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class MeshSecurePropertiesStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val securePropertiesStore: DataStore<ProtoSecurePropertiesMap>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SecurePropertiesStorage {

    private val scope = CoroutineScope(context = SupervisorJob() + ioDispatcher)

    /**
     * Creates a default [ProtoSecurePropertiesMap] with a single entry for the given [uuid].
     *
     * @param uuid Uuid of the mesh network.
     * @return [ProtoSecurePropertiesMap] with a single entry for the given [uuid].
     */
    private fun createProtoSecurePropertiesMap(
        uuid: Uuid,
        provisionerUUID: String = "",
        ivIndex: ProtoIvIndex = ProtoIvIndex(),
        sequenceNumbers: Map<Int, Int> = mutableMapOf(),
        seqAuths: Map<Int, ProtoSeqAuth> = mutableMapOf(),
    ) = ProtoSecurePropertiesMap(
        properties = mutableMapOf(
            uuid.toString() to ProtoSecureProperties(
                localProvisionerUuid = provisionerUUID,
                ivIndex = ivIndex,
                sequenceNumbers = sequenceNumbers,
                seqAuths = seqAuths
            )
        )
    ).also {
        storeSecurePropertiesMap(it)
    }

    /**
     * Returns the [ProtoSecurePropertiesMap] for the given [uuid] or creates a new one for a given
     * [uuid] if it doesn't exist.
     *
     * @param uuid Uuid of the mesh network.
     * @return [ProtoSecurePropertiesMap] for the given [uuid].
     */
    private suspend fun secureProperties(uuid: Uuid) = withContext(ioDispatcher) {
        securePropertiesStore.data
            .firstOrNull()
            ?.properties
            ?.get(key = uuid.toString())
            ?: createProtoSecurePropertiesMap(uuid = uuid).properties[uuid.toString()]
    }

    /**
     * Stores the given secure properties for a given network.
     *
     * @param securePropertiesMap Map of secure properties.
     */
    private fun storeSecurePropertiesMap(securePropertiesMap: ProtoSecurePropertiesMap) {
        scope.launch {
            securePropertiesStore.updateData {
                it.copy(properties = securePropertiesMap.properties)
                it
            }
        }
    }

    override suspend fun localProvisioner(uuid: Uuid): String? {
        return secureProperties(uuid = uuid)?.localProvisionerUuid
    }

    override suspend fun storeLocalProvisioner(uuid: Uuid, localProvisionerUuid: Uuid): Unit =
        withContext(context = ioDispatcher) {
            securePropertiesStore.updateData { securePropertiesMap ->
                val propertiesMap = securePropertiesMap
                    .properties
                    .toMutableMap()
                var properties = propertiesMap[uuid.toString()] ?: ProtoSecureProperties(
                    localProvisionerUuid = localProvisionerUuid.toString()
                )
                properties = properties.copy(localProvisionerUuid = localProvisionerUuid.toString())
                propertiesMap[uuid.toString()] = properties
                securePropertiesMap.copy(properties = propertiesMap.toMap())
            }
        }

    @OptIn(ExperimentalTime::class)
    override suspend fun ivIndex(uuid: Uuid): IvIndex = withContext(context = ioDispatcher) {
        secureProperties(uuid = uuid)
            ?.ivIndex?.toIvIndex() ?: IvIndex().also {
            storeIvIndex(uuid, it)
        }
    }

    override suspend fun storeIvIndex(uuid: Uuid, ivIndex: IvIndex): Unit =
        withContext(context = ioDispatcher) {
            securePropertiesStore.updateData { securePropertiesMap ->
                val propertiesMap = securePropertiesMap
                    .properties
                    .toMutableMap()
                var properties = propertiesMap[uuid.toString()] ?: ProtoSecureProperties()
                properties = properties.copy(ivIndex = ivIndex.toProtoIvIndex())
                propertiesMap[uuid.toString()] = properties
                securePropertiesMap.copy(properties = propertiesMap.toMap())
            }
        }

    override suspend fun nextSequenceNumber(uuid: Uuid, address: UnicastAddress): UInt =
        withContext(context = ioDispatcher) {
            // Lets get the sequence number from the data store for a given address or 0 if it doesn't
            // exist
            val sequenceNumber = secureProperties(uuid = uuid)
                ?.sequenceNumbers
                ?.get(key = address.address.toInt())?.toUInt() ?: 0u
            // Increment the sequence number and store it back to the data store
            storeNextSequenceNumber(
                uuid = uuid,
                address = address,
                sequenceNumber = (sequenceNumber + 1u)
            )
            sequenceNumber
        }

    override suspend fun storeNextSequenceNumber(
        uuid: Uuid,
        address: UnicastAddress,
        sequenceNumber: UInt,
    ) {
        scope.launch {
            securePropertiesStore.updateData { securePropertiesMap ->
                val propertiesMap = securePropertiesMap
                    .properties
                    .toMutableMap()
                var properties = propertiesMap[uuid.toString()] ?: ProtoSecureProperties()
                val sequenceNumbers = properties
                    .sequenceNumbers
                    .toMutableMap()
                    .also { map ->
                        map[address.address.toInt()] = sequenceNumber.toInt()
                    }
                properties = properties.copy(sequenceNumbers = sequenceNumbers)
                propertiesMap[uuid.toString()] = properties
                securePropertiesMap.copy(properties = propertiesMap.toMap())
            }
        }
    }

    override suspend fun resetSequenceNumber(uuid: Uuid, address: UnicastAddress) {
        storeNextSequenceNumber(uuid = uuid, address = address, sequenceNumber = 0u)
    }

    override suspend fun lastSeqAuthValue(uuid: Uuid, source: UnicastAddress) =
        secureProperties(uuid = uuid)
            ?.seqAuths
            ?.get(source.address.toInt())
            ?.last?.toULong() //?: 0uL

    override fun storeLastSeqAuthValue(
        uuid: Uuid,
        source: UnicastAddress,
        lastSeqAuth: ULong,
    ) {
        scope.launch {
            securePropertiesStore.updateData { securePropertiesMap ->
                val propertiesMap = securePropertiesMap
                    .properties
                    .toMutableMap()
                var properties = propertiesMap[uuid.toString()] ?: ProtoSecureProperties()

                val seqAuths = properties
                    .seqAuths
                    .toMutableMap()
                var protoSeqAuth = seqAuths[source.address.toInt()] ?: ProtoSeqAuth()
                protoSeqAuth = protoSeqAuth.copy(last = lastSeqAuth.toLong())
                seqAuths[source.address.toInt()] = protoSeqAuth
                properties = properties.copy(seqAuths = seqAuths)
                propertiesMap[uuid.toString()] = properties
                securePropertiesMap.copy(properties = propertiesMap.toMap())
            }
        }
    }

    override suspend fun previousSeqAuthValue(uuid: Uuid, source: UnicastAddress) =
        secureProperties(uuid = uuid)
            ?.seqAuths?.get(key = source.address.toInt())
            ?.previous?.toULong() ?: 0uL

    override fun storePreviousSeqAuthValue(
        uuid: Uuid,
        source: UnicastAddress,
        seqAuth: ULong,
    ) {
        scope.launch {
            securePropertiesStore.updateData { securePropertiesMap ->
                val propertiesMap = securePropertiesMap
                    .properties
                    .toMutableMap()
                var properties = propertiesMap[uuid.toString()] ?: ProtoSecureProperties()

                val seqAuths = properties
                    .seqAuths
                    .toMutableMap()
                var protoSeqAuth = seqAuths[source.address.toInt()] ?: ProtoSeqAuth()
                protoSeqAuth = protoSeqAuth.copy(previous = seqAuth.toLong())
                seqAuths[source.address.toInt()] = protoSeqAuth
                properties = properties.copy(seqAuths = seqAuths)
                propertiesMap[uuid.toString()] = properties
                securePropertiesMap.copy(properties = propertiesMap.toMap())
            }
        }
    }
}

/**
 * Serializer for [ProtoSecurePropertiesMap] to be used when writing and reading from Proto
 * DataStore.
 */
object ProtoSecurePropertiesMapSerializer : Serializer<ProtoSecurePropertiesMap> {
    override val defaultValue: ProtoSecurePropertiesMap
        get() = ProtoSecurePropertiesMap()

    override suspend fun readFrom(input: InputStream): ProtoSecurePropertiesMap {
        try {
            return ProtoSecurePropertiesMap.ADAPTER.decode(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", e)
        }
    }

    override suspend fun writeTo(t: ProtoSecurePropertiesMap, output: OutputStream) {
        t.encode(output)
    }
}

/**
 * Converts a [ProtoIvIndex] to [IvIndex].
 * @receiver [ProtoIvIndex]
 * @return [IvIndex]
 */
@OptIn(ExperimentalTime::class)
private fun ProtoIvIndex.toIvIndex() = IvIndex(
    index = index.toUInt(),
    isIvUpdateActive = updateActive,
    transitionDate = Instant.fromEpochMilliseconds(transitionDate)
)

/**
 * Converts a [IvIndex] to [ProtoIvIndex].
 *
 * @receiver [IvIndex]
 * @return [ProtoIvIndex]
 */
@OptIn(ExperimentalTime::class)
private fun IvIndex.toProtoIvIndex() = ProtoIvIndex(
    index = index.toInt(),
    updateActive = isIvUpdateActive,
    transitionDate = transitionDate.toEpochMilliseconds()
)