package no.nordicsemi.android.nrfmesh.feature.nodes.node.element

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.NotStarted
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.kotlin.mesh.core.model.Element
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Node

@HiltViewModel(assistedFactory = ElementViewModel.Factory::class)
internal class ElementViewModel @AssistedInject internal constructor(
    private val repository: CoreDataRepository,
    @Assisted address: Int,
) : ViewModel() {
    private lateinit var meshNetwork: MeshNetwork
    private lateinit var selectedNode: Node
    private val address = address.toUShort()

    val uiState: StateFlow<ElementScreenUiState>
        field = MutableStateFlow(ElementScreenUiState())

    init {
        observeNetworkChanges()
    }

    private fun observeNetworkChanges() {
        repository.networkEvents
            .mapNotNull { repository.meshNetwork }
            .onEach {
                val elementState = it.element(elementAddress = address)?.let { element ->
                    selectedNode = element.parentNode!!
                    ElementState.Success(element = element)
                } ?: ElementState.Error(Throwable("Element containing node not found"))
                uiState.update { state ->
                    state.copy(elementState = elementState)
                }
                meshNetwork = it // update the local network instance
            }
            .launchIn(scope = viewModelScope)
    }

    fun save() {
        repository.save()
    }

    @AssistedFactory
    interface Factory {
        fun create(address: Int): ElementViewModel
    }
}

internal sealed interface ElementState {

    data object Loading : ElementState

    data class Success(val element: Element) : ElementState

    data class Error(val throwable: Throwable) : ElementState
}

internal data class ElementScreenUiState(
    val elementState: ElementState = ElementState.Loading,
    val messageState: MessageState = NotStarted,
    val wasNetworkRemoved: Boolean = false,
)