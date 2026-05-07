package no.nordicsemi.android.nrfmesh.feature.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.compose
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.kotlin.mesh.core.exception.NoNetwork
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import javax.inject.Inject

@HiltViewModel
internal class GroupsViewModel @Inject internal constructor(
    private val repository: CoreDataRepository,
) : ViewModel() {
    private lateinit var meshNetwork: MeshNetwork
    private val _uiState = MutableStateFlow(GroupsScreenUiState(groups = listOf()))
    val uiState: StateFlow<GroupsScreenUiState> = _uiState.asStateFlow()

    init {
        observeNetworkChanges()
    }

    // Observes the mesh network for any changes i.e. network reset etc.
    private fun observeNetworkChanges() {
        repository.networkEvents
            .map { repository.meshNetwork }
            .filterNotNull()
            .onEach { network ->
                meshNetwork = network
                _uiState.value = GroupsScreenUiState(
                    groups = network.groups
                )
            }
            .launchIn(scope = viewModelScope)
    }

    /**
     * Returns the next available group address
     */
    internal fun nextAvailableGroupAddress(): GroupAddress {
        val provisioner = meshNetwork.provisioners.firstOrNull()
        require(provisioner != null) {
            throw IllegalArgumentException("No provisioner found")
        }
        return meshNetwork.nextAvailableGroup(provisioner)
            ?: throw IllegalArgumentException("No available group address found for ${provisioner.name}")
    }

    /**
     * Adds a group to the mesh network
     */
    internal fun addGroup(group: Group) {
        val meshNetwork = meshNetwork ?: throw NoNetwork()
        meshNetwork.add(group)
        viewModelScope.launch {
            repository.save()
        }
    }
}

@ConsistentCopyVisibility
internal data class GroupsScreenUiState internal constructor(
    val groups: List<Group> = listOf(),
)