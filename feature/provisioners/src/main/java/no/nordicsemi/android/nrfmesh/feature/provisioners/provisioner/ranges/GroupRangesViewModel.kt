package no.nordicsemi.android.nrfmesh.feature.provisioners.provisioner.ranges

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
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.kotlin.mesh.core.model.GroupRange
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Provisioner
import no.nordicsemi.kotlin.mesh.core.model.Range
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@HiltViewModel(assistedFactory = GroupRangesViewModel.Factory::class)
internal class GroupRangesViewModel @AssistedInject internal constructor(
    private val repository: CoreDataRepository,
    @Assisted uuid: Uuid,
) : ViewModel() {
    private val provisionerUuid = uuid
    private lateinit var network: MeshNetwork
    private lateinit var provisioner: Provisioner
    private val _uiState = MutableStateFlow(GroupRangesScreenUiState())
    internal val uiState: StateFlow<GroupRangesScreenUiState> = _uiState.asStateFlow()

    init {
        observeNetwork()
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun observeNetwork() {
        repository.networkEvents
            .map { repository.meshNetwork }
            .filterNotNull()
            .onEach {
                network = it
                it.provisioner(uuid = provisionerUuid)?.let { provisioner ->
                    this.provisioner = provisioner
                    _uiState.value = GroupRangesScreenUiState(
                        ranges = provisioner.allocatedGroupRanges,
                        otherRanges = provisioner.otherGroupRanges
                    )
                }
            }
            .launchIn(scope = viewModelScope)
    }

    /**
     * Allocates the given unicast ranges to the provisioner.
     */
    internal fun allocate(ranges: List<Range>) {
        provisioner.allocate(ranges = ranges)
    }

    /**
     * Removes all allocated unicast ranges from the provisioner.
     */
    internal fun removeAllRanges() {
        val ranges = provisioner.allocatedGroupRanges
        provisioner.remove(ranges = ranges)
    }

    /**
     * Saves the network.
     */
    internal fun save() {
        repository.save()
    }

    @AssistedFactory
    interface Factory {
        @OptIn(ExperimentalUuidApi::class)
        fun create(uuid: Uuid): GroupRangesViewModel
    }
}

@ConsistentCopyVisibility
internal data class GroupRangesScreenUiState internal constructor(
    val ranges: List<GroupRange> = emptyList(),
    val otherRanges: List<GroupRange> = emptyList(),
)
