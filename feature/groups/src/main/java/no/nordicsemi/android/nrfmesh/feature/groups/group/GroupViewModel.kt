package no.nordicsemi.android.nrfmesh.feature.groups.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.isSupportedGroupItem
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.ModelId

@HiltViewModel(assistedFactory = GroupViewModel.Factory::class)
internal class GroupViewModel @AssistedInject internal constructor(
    private val repository: CoreDataRepository,
    @Assisted private val address: Int,
) : ViewModel() {
    private var group: Group? = null
    private val _uiState = MutableStateFlow(GroupScreenUiState())
    val uiState: StateFlow<GroupScreenUiState> = _uiState.asStateFlow()

    private lateinit var network: MeshNetwork

    init {
        observeNetwork()
    }

    private fun observeNetwork() {
        repository.networkEvents
            .map { repository.meshNetwork }
            .filterNotNull()
            .onEach { network ->
                network.group(address = address.toUShort())?.let { group ->
                    this@GroupViewModel.group = group
                    val models = mutableMapOf<ModelId, List<Model>>()
                    network.nodes
                        .flatMap { it.elements }
                        .flatMap { it.models }
                        .filter { it.isSubscribedTo(group = group) }
                        .forEach { model ->
                            if (isSupportedGroupItem(model)) {
                                models[model.modelId] = (models[model.modelId]
                                    ?.plus(model))
                                    ?: listOf(model)
                            }
                        }
                    val state = _uiState.value.copy(
                        groupState = GroupState.Success(
                            network = network,
                            group = group,
                            groupInfoListData = GroupInfoListData(
                                group = group,
                                models = models
                            )
                        )
                    )
                    _uiState.emit(value = state)
                    this@GroupViewModel.network = network
                }
            }.launchIn(scope = viewModelScope)
    }

    internal fun save() {
        viewModelScope.launch { repository.save() }
    }

    @Suppress("unused")
    fun onApplicationKeyClicked(key: ApplicationKey) {
        viewModelScope.launch {
            val modelsMap = mutableMapOf<ModelId, List<Model>>()
            network.run {
                nodes.filter { it.knows(key = key) }
                    .flatMap { it.elements }
                    .flatMap { it.models }
                    .filter { key.isBoundTo(model = it) }
                    .forEach { model ->
                        modelsMap[model.modelId] = modelsMap[model.modelId]?.let {
                            it + model
                        } ?: mutableListOf()
                    }
            }
        }
    }

    fun onModelClicked(index: Int) {
        val state = _uiState.value.groupState as? GroupState.Success ?: return
        _uiState.value = _uiState.value.copy(
            groupState = state.copy(selectedModelIndex = index)
        )
    }

    fun deleteGroup(group: Group): Boolean = network.remove(group = group)

    fun send(message: UnacknowledgedMeshMessage, key: ApplicationKey) {
        viewModelScope.launch {
            repository.send(group = group!!, unackedMessage = message, key = key)
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(address: Int): GroupViewModel
    }
}

internal data class GroupScreenUiState(val groupState: GroupState = GroupState.Loading)

internal sealed interface GroupState {
    data object Loading : GroupState
    data class Success(
        val network: MeshNetwork,
        val group: Group,
        val nextAvailableGroupAddress: GroupAddress? = null,
        val groupInfoListData: GroupInfoListData,
        val selectedModelIndex: Int = -1,
    ) : GroupState

    @Suppress("unused")
    data class Error(val throwable: Throwable) : GroupState
}