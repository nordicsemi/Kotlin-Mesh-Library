package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.ProxyConnectionState
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Node
import javax.inject.Inject

@HiltViewModel
internal class Page5ViewModel @Inject internal constructor(
    private val repository: CoreDataRepository,
) : ViewModel() {
    val uiState: StateFlow<Page5ScreenUiState>
        field = MutableStateFlow(Page5ScreenUiState(distributor = repository.proxyFilter.proxy))
    private lateinit var network: MeshNetwork

    init {
        observeNetwork()
    }

    private fun observeNetwork() {
        repository.networkEvents
            .mapNotNull { repository.meshNetwork }
            .onEach { network = it }
            .launchIn(scope = viewModelScope)
    }
}

internal data class Page5ScreenUiState(
    val proxyConnectionState: ProxyConnectionState = ProxyConnectionState(),
    val distributor: Node?,
)