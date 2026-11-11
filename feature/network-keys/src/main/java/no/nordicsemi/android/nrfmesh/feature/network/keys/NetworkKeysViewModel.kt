package no.nordicsemi.android.nrfmesh.feature.network.keys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.ui.NetworkKeyData
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import javax.inject.Inject

@HiltViewModel
class NetworkKeysViewModel @Inject internal constructor(
    private val repository: CoreDataRepository,
) : ViewModel() {

    private lateinit var network: MeshNetwork
    val uiState: StateFlow<NetworkKeysScreenUiState>
        field = MutableStateFlow(NetworkKeysScreenUiState())

    init {
        observeNetwork()
    }

    override fun onCleared() {
        removeKeys()
        super.onCleared()
    }

    private fun observeNetwork() {
        repository.networkEvents
            .mapNotNull { repository.meshNetwork }
            .onEach {
                network = it
                uiState.update { state ->
                    state.copy(
                        keys = network.networkKeys
                            .map { NetworkKeyData(key = it) }
                            // Filter out the keys that are marked for deletion.
                            .filter { it !in state.keysToBeRemoved },
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Adds a network key to the network.
     */
    internal fun addNetworkKey() = repository.addNetworkKey()

    /**
     * Invoked when a key is swiped to be deleted. The given key is added to a list of keys that
     * is to be deleted.
     *
     * @param key Network key to be deleted.
     */
    internal fun onSwiped(key: NetworkKeyData) {
        uiState.update { state ->
            state.copy(keysToBeRemoved = state.keysToBeRemoved + key)
        }
    }

    /**
     * Invoked when a key is swiped to be deleted is undone. When invoked the given key is removed
     * from the list of keys to be deleted.
     *
     * @param key Network key to be reverted.
     */
    internal fun onUndoSwipe(key: NetworkKeyData) {
        uiState.update { state ->
            state.copy(keysToBeRemoved = state.keysToBeRemoved - key)
        }
    }

    /**
     * Remove a given key from the network.
     *
     * @param key Key to be removed.
     */
    internal fun remove(key: NetworkKeyData) {
        uiState.update { state ->
            state.copy(
                keys = state.keys - key,
                keysToBeRemoved = state.keysToBeRemoved - key
            )
        }
        network.removeNetworkKeyWithIndex(index = key.index)
        // In addition, lets remove the keys queued for deletion as well.
        removeKeys()
    }

    /**
     * Removes all keys that are queued for deletion.
     */
    private fun removeKeys() {
        val _ = runCatching {
            uiState.value.keysToBeRemoved.forEach { keyData ->
                network.removeNetworkKeyWithIndex(index = keyData.index)
            }
        }
        repository.save()
    }

    internal fun selectKeyIndex(keyIndex: KeyIndex) {
        uiState.update { state ->
            state.copy(selectedKeyIndex = keyIndex)
        }
    }
}

@ConsistentCopyVisibility
data class NetworkKeysScreenUiState internal constructor(
    val keys: List<NetworkKeyData> = listOf(),
    val keysToBeRemoved: List<NetworkKeyData> = listOf(),
    val selectedKeyIndex: KeyIndex? = null,
)
