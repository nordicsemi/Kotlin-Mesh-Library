package no.nordicsemi.android.nrfmesh.feature.scenes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.ui.SceneData
import no.nordicsemi.kotlin.mesh.core.exception.NoSceneNumberAvailable
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber
import javax.inject.Inject

@HiltViewModel
internal class ScenesViewModel @Inject internal constructor(
    private val repository: CoreDataRepository,
) : ViewModel() {
    private lateinit var network: MeshNetwork
    private val _uiState = MutableStateFlow(ScenesScreenUiState())
    val uiState: StateFlow<ScenesScreenUiState> = _uiState.asStateFlow()

    init {
        observeNetwork()
    }

    override fun onCleared() {
        removeAllScenes()
        super.onCleared()
    }

    private fun observeNetwork() {
        repository.networkEvents
            .map { repository.meshNetwork }
            .filterNotNull()
            .onEach {
                network = it
                _uiState.update { state ->
                    state.copy(
                        scenes = network.scenes
                            .map { SceneData(scene = it) }
                            // Filter out the scenes that are marked for deletion.
                            .filter { it !in state.scenesToBeRemoved }
                    )
                }

            }.launchIn(scope = viewModelScope)
    }

    /**
     * Adds a scene to the network.
     */
    internal fun addScene() = network
        .add(
            name = "Scene ${
                network.nextAvailableScene(
                    provisioner = network.provisioners
                        .firstOrNull() ?: throw IllegalStateException()
                ) ?: throw NoSceneNumberAvailable()
            }",
            provisioner = network.provisioners.firstOrNull() ?: throw IllegalStateException()
        )
        .also { repository.save() }

    /**
     * Invoked when a scene is swiped to be deleted. The given scene is added to a list of scenes
     * that is to be deleted.
     *
     * @param scene Scene to be deleted.
     */
    internal fun onSwiped(scene: SceneData) {
        _uiState.update { state ->
            state.copy(scenesToBeRemoved = state.scenesToBeRemoved + scene)
        }
    }

    /**
     * Invoked when a scene is swiped to be deleted is undone. When invoked the given scene is
     * removed from the list of scenes to be deleted.
     *
     * @param scene Scene to be reverted.
     */
    internal fun onUndoSwipe(scene: SceneData) {
        _uiState.update { state ->
            state.copy(scenesToBeRemoved = state.scenesToBeRemoved - scene)
        }
    }

    /**
     * Remove a given scene from the network.
     *
     * @param scene Scene to be removed.
     */
    internal fun remove(scene: SceneData) {
        _uiState.update { state ->
            state.copy(
                scenes = state.scenes - scene,
                scenesToBeRemoved = state.scenesToBeRemoved - scene
            )
        }
        network.remove(sceneNumber = scene.number)
        removeAllScenes()
    }

    /**
     * Removes all the scenes that are queued for deletion.
     */
    private fun removeAllScenes() {
        val _ = runCatching {
            _uiState.value.scenesToBeRemoved.forEach { scene ->
                network.remove(sceneNumber = scene.number)
            }
        }
        repository.save()
    }

    internal fun selectScene(number: SceneNumber?) {
        _uiState.update { state ->
            state.copy(selectedSceneNumber = number)
        }
    }
}

@ConsistentCopyVisibility
data class ScenesScreenUiState internal constructor(
    val scenes: List<SceneData> = listOf(),
    val scenesToBeRemoved: List<SceneData> = listOf(),
    val selectedSceneNumber: SceneNumber? = null,
)
