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
     * @return Level value.
     */
    suspend fun readGenericLevelState(
        uuid: Uuid,
        sceneNumber: SceneNumber,
        elementIndex: Int,
    ): UShort
}