package no.nordicsemi.android.nrfmesh.core.data.storage

import no.nordicsemi.android.nrfmesh.core.data.ui.SceneNumber
import kotlin.uuid.Uuid

/**
 * An Interface for storing Generic OnOff state for a given SceneServerState using Proto DataStore.
 */
interface GenericOnOffStateStorage {

    /**
     * Stores the Generic OnOff state for a given SceneServerState.
     *
     * @param uuid           UUID of the network.
     * @param sceneNumber    Scene number.
     * @param elementIndex   Index of the element.
     * @param on             State of the Generic OnOff.
     */
    fun storeGenericOnOffState(
        uuid: Uuid,
        sceneNumber: SceneNumber,
        elementIndex: Int,
        on: Boolean,
    )

    /**
     * Reads the Generic OnOff state for a given SceneServerState.
     *
     * @param uuid           UUID of the network.
     * @param sceneNumber    Scene number.
     * @param elementIndex   Index of the element.
     * @return State of the Generic OnOff.
     */
    suspend fun readGenericOnOffState(
        uuid: Uuid,
        sceneNumber: SceneNumber,
        elementIndex: Int,
    ): Boolean
}

/**
 * An interface for storing Generic Level state for a given SceneServerState using Proto DataStore.
 */
interface GenericLevelStateStorage {

    /**
     * Stores the Generic Level state for a given SceneServerState.
     *
     * @param uuid           UUID of the network.
     * @param sceneNumber    Scene number.
     * @param elementIndex   Index of the element.
     * @param level          Level value.
     */
    fun storeGenericLevelState(
        uuid: Uuid,
        sceneNumber: SceneNumber,
        elementIndex: Int,
        level: UShort,
    )

    /**
     * Reads the Generic Level state for a given SceneServerState.
     *
     * @param uuid           UUID of the network.
     * @param sceneNumber    Scene number.
     * @param elementIndex   Index of the element.
     * @return Level value, or null if no level was stored for the given scene and element.
     */
    suspend fun readGenericLevelState(
        uuid: Uuid,
        sceneNumber: SceneNumber,
        elementIndex: Int,
    ): UShort?
}

/**
 * An interface for storing the Scene Register of the Scene Server on the local node using Proto
 * DataStore.
 */
interface SceneRegisterStorage {

    /**
     * Reads the scene numbers stored in the Scene Register of a given network.
     *
     * @param uuid UUID of the network.
     * @return Scene numbers sorted in ascending order.
     */
    suspend fun readScenes(uuid: Uuid): List<SceneNumber>

    /**
     * Adds a scene to the Scene Register of a given network. Does nothing if it already exists.
     *
     * @param uuid           UUID of the network.
     * @param sceneNumber    Scene number.
     */
    fun storeScene(uuid: Uuid, sceneNumber: SceneNumber)

    /**
     * Removes a scene, along with the states stored with it, from the Scene Register of a given
     * network.
     *
     * @param uuid           UUID of the network.
     * @param sceneNumber    Scene number.
     */
    fun deleteScene(uuid: Uuid, sceneNumber: SceneNumber)
}
