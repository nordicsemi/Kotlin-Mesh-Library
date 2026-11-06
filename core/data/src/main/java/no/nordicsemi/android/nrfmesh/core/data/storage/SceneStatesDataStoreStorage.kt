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
import kotlin.uuid.Uuid

class SceneStatesDataStoreStorage @Inject constructor(
    private val sceneStore: DataStore<ProtoSceneStatesDataStore>,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GenericOnOffStateStorage, GenericLevelStateStorage, SceneRegisterStorage {
     private val scope = CoroutineScope(context = SupervisorJob() + ioDispatcher)

    private suspend fun sceneStates(uuid: Uuid, sceneNumber: SceneNumber) = sceneStore.data
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
     * Persists the scene states of the given [ProtoSceneStatesDataStore] for the networks that
     * have none yet. Scene states that are already stored are never overwritten.
     */
    private fun storeSceneStatesDataStore(store: ProtoSceneStatesDataStore) {
        scope.launch {
            sceneStore.updateData {
                it.copy(sceneStates = store.sceneStates + it.sceneStates)
            }
        }
    }

    override fun storeGenericOnOffState(
        uuid: Uuid,
        sceneNumber: SceneNumber,
        elementIndex: Int,
        on: Boolean,
    ) {
        scope.launch {
            sceneStore.updateData {
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

    override suspend fun readGenericOnOffState(
        uuid: Uuid,
        sceneNumber: SceneNumber,
        elementIndex: Int,
    ) = sceneStates(uuid = uuid, sceneNumber = sceneNumber)
        .states[sceneNumber.toInt()]
        ?.genericOnOffStates[elementIndex]
        ?.on == true

    override fun storeGenericLevelState(
        uuid: Uuid,
        sceneNumber: SceneNumber,
        elementIndex: Int,
        level: UShort,
    ) {
        scope.launch {
            sceneStore.updateData {
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
                    mapOf(elementIndex to GenericLevelState(level = level.toInt()))
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

    override suspend fun readGenericLevelState(
        uuid: Uuid,
        sceneNumber: SceneNumber,
        elementIndex: Int,
    ) = sceneStates(uuid = uuid, sceneNumber = sceneNumber)
        .states[sceneNumber.toInt()]
        ?.genericLevelStates[elementIndex]
        ?.level
        ?.toUShort()

    override suspend fun readScenes(uuid: Uuid) = sceneStore.data
        .firstOrNull()
        ?.sceneStates
        ?.get(uuid.toString())
        ?.states
        ?.keys
        ?.map { it.toUShort() }
        ?.sorted()
        ?: emptyList()

    override fun storeScene(uuid: Uuid, sceneNumber: SceneNumber) {
        scope.launch {
            sceneStore.updateData {
                val states = it.sceneStates[uuid.toString()] ?: SceneStates(states = mapOf())
                if (states.states.containsKey(sceneNumber.toInt())) return@updateData it
                val updatedStates = states.states.plus(
                    sceneNumber.toInt() to SceneState(
                        genericOnOffStates = mapOf(),
                        genericLevelStates = mapOf()
                    )
                )
                it.copy(
                    sceneStates = it.sceneStates.plus(
                        uuid.toString() to states.copy(states = updatedStates)
                    )
                )
            }
        }
    }

    override fun deleteScene(uuid: Uuid, sceneNumber: SceneNumber) {
        scope.launch {
            sceneStore.updateData {
                val states = it.sceneStates[uuid.toString()] ?: return@updateData it
                it.copy(
                    sceneStates = it.sceneStates.plus(
                        uuid.toString() to states.copy(
                            states = states.states.minus(sceneNumber.toInt())
                        )
                    )
                )
            }
        }
    }
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
