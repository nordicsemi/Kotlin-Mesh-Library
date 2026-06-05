package no.nordicsemi.android.nrfmesh.core.data.storage

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.preferences.protobuf.InvalidProtocolBufferException
import jakarta.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.di.IoDispatcher
import no.nordicsemi.android.nrfmesh.core.data.GenericLevelState
import no.nordicsemi.android.nrfmesh.core.data.GenericOnOffState
import no.nordicsemi.android.nrfmesh.core.data.ProtoSceneStatesDataStore
import no.nordicsemi.android.nrfmesh.core.data.SceneState
import no.nordicsemi.android.nrfmesh.core.data.SceneStates
import no.nordicsemi.android.nrfmesh.core.data.ui.SceneNumber
import java.io.InputStream
import java.io.OutputStream
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SceneStatesDataStoreStorage @Inject constructor(
    private val store: DataStore<ProtoSceneStatesDataStore>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GenericOnOffStateStorage, GenericLevelStateStorage {
     private val scope = CoroutineScope(context = SupervisorJob() + ioDispatcher)

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun sceneStates(uuid: Uuid, sceneNumber: SceneNumber) = store.data
        .firstOrNull()
        ?.sceneStates
        ?.get(uuid.toString())
        ?: createSceneStatesDataStore(
            uuid = uuid,
            sceneNumber = sceneNumber
        ).sceneStates[uuid.toString()]!!

    /**
     * Creates a default [ProtoSceneStatesDataStore] with a single entry for the given [uuid].
     */
    @OptIn(ExperimentalUuidApi::class, ExperimentalStdlibApi::class)
    private fun createSceneStatesDataStore(
        uuid: Uuid,
        sceneNumber: SceneNumber,
    ): ProtoSceneStatesDataStore {
        val sceneState = SceneState(
            genericOnOffStates = mutableMapOf(),
            genericLevelStates = mutableMapOf()
        )
        val sceneStates = SceneStates(
            states = mutableMapOf(sceneNumber.toInt() to sceneState)
        )
        return ProtoSceneStatesDataStore(
            sceneStates = mutableMapOf(uuid.toString() to sceneStates)
        ).also { storeSceneStatesDataStore(it) }
    }

    /**
     * Returns the [ProtoSceneStatesDataStore] for the given [uuid] or creates a new one for a given
     */
    private fun storeSceneStatesDataStore(store1: ProtoSceneStatesDataStore) {
        scope.launch {
            store.updateData {
                it.copy(sceneStates = store1.sceneStates)
                it
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalStdlibApi::class)
    override fun storeGenericOnOffState(
        uuid: Uuid,
        sceneNumber: SceneNumber,
        elementIndex: Int,
        on: Boolean,
    ) {
        scope.launch {
            store.updateData {
                val sceneStates = it.sceneStates
                // get the scene states for given network uuid or create a new one
                val states = sceneStates[uuid.toString()] ?: SceneStates(states = mutableMapOf())
                // get the scene state for given scene number or create a new one
                val sceneState = states.states[sceneNumber.toInt()] ?: SceneState(
                    genericOnOffStates = mapOf(),
                    genericLevelStates = mapOf()
                )
                // add the current state to the scene state
                val genericOnOffStates = sceneState.genericOnOffStates.plus(
                    mapOf(elementIndex to GenericOnOffState(on = on))
                )
                // create a new scene state with the updated generic on off states
                val newSceneState = sceneState.copy(genericOnOffStates = genericOnOffStates)
                val updatedStates = states.states.plus(mapOf(sceneNumber.toInt() to newSceneState))
                val updatedSceneStates =
                    it.sceneStates.plus(mapOf(uuid.toString() to states.copy(states = updatedStates)))
                it.copy(sceneStates = updatedSceneStates)
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun readGenericOnOffState(
        uuid: Uuid,
        sceneNumber: SceneNumber,
        elementIndex: Int,
    ) = sceneStates(uuid = uuid, sceneNumber = sceneNumber)
        .states[elementIndex]
        ?.genericOnOffStates[sceneNumber.toInt()]
        ?.on == true

    @OptIn(ExperimentalUuidApi::class)
    override fun storeGenericLevelState(
        uuid: Uuid,
        sceneNumber: SceneNumber,
        elementInex: Int,
        level: UShort,
    ) {
        scope.launch {
            store.updateData {
                val sceneStates = it.sceneStates
                // get the scene states for given network uuid or create a new one
                val states = sceneStates[uuid.toString()] ?: SceneStates(states = mutableMapOf())
                // get the scene state for given scene number or create a new one
                val sceneState = states.states[sceneNumber.toInt()] ?: SceneState(
                    genericOnOffStates = mapOf(),
                    genericLevelStates = mapOf()
                )
                // add the current state to the scene state
                val genericLevelStates = sceneState.genericLevelStates.plus(
                    mapOf(elementInex to GenericLevelState(level = level.toInt()))
                )
                // create a new scene state with the updated generic on off states
                val newSceneState = sceneState.copy(genericLevelStates = genericLevelStates)
                val updatedStates = states.states.plus(mapOf(sceneNumber.toInt() to newSceneState))
                val updatedSceneStates =
                    it.sceneStates.plus(mapOf(uuid.toString() to states.copy(states = updatedStates)))
                it.copy(sceneStates = updatedSceneStates)
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun readGenericLevelState(
        uuid: Uuid,
        sceneNumber: SceneNumber,
        elementIndex: Int,
    ) = sceneStates(uuid = uuid, sceneNumber = sceneNumber)
        .states[elementIndex]
        ?.genericLevelStates[sceneNumber.toInt()]
        ?.level
        ?.toUShort()
        ?: 0u
}

/**
 * Serializer for [ProtoSceneStatesDataStore] to be used when writing and reading from Proto
 * DataStore.
 */
object ProtoSceneStatesDataStoreSerializer : Serializer<ProtoSceneStatesDataStore> {
    override val defaultValue: ProtoSceneStatesDataStore
        get() = ProtoSceneStatesDataStore()

    override suspend fun readFrom(input: InputStream): ProtoSceneStatesDataStore {
        try {
            return ProtoSceneStatesDataStore.ADAPTER.decode(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read model states proto.", e)
        }
    }

    override suspend fun writeTo(t: ProtoSceneStatesDataStore, output: OutputStream) {
        t.encode(output)
    }
}
