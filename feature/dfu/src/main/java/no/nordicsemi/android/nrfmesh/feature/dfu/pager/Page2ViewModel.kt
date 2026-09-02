package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.NotStarted
import no.nordicsemi.android.nrfmesh.core.common.blobTransferServerModel
import no.nordicsemi.android.nrfmesh.core.common.firmwareUpdateServer
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.ProxyConnectionState
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Target
import no.nordicsemi.android.nrfmesh.feature.dfu.util.ZipPackage
import no.nordicsemi.kotlin.mesh.core.ProxyFilterState
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import kotlin.uuid.ExperimentalUuidApi

@HiltViewModel(assistedFactory = Page2ViewModel.Factory::class)
internal class Page2ViewModel @AssistedInject internal constructor(
    private val repository: CoreDataRepository,
    @Assisted index: Int,
) : ViewModel() {
    private val _uiState = MutableStateFlow(Page2ScreenUiState())
    internal val uiState = _uiState.asStateFlow()
    private lateinit var network: MeshNetwork

    init {
        observeNetwork()
        observeProxyConnectionState()
        observeProxyFilterState()
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun observeNetwork() {
        repository.networkEvents
            .mapNotNull { repository.meshNetwork }
            .onEach {
                network = it
                _uiState.update { state ->
                    state.copy(
                        nodes = network.nodes
                            .filter { node ->
                                node.model(modelId = firmwareUpdateServer) != null &&
                                        node.model(modelId = blobTransferServerModel) != null
                            }
                    )
                }
            }
            .launchIn(scope = viewModelScope)
    }

    private fun observeProxyConnectionState() {
        repository.proxyConnectionStateFlow
            .onEach { proxyConnectionState ->
                _uiState.update { it.copy(proxyConnectionState = proxyConnectionState) }
            }
            .launchIn(scope = viewModelScope)
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun observeProxyFilterState() {
        repository.proxyFilter.proxyFilterStateFlow
            .onEach { filterState ->
                // We update the state here because the proxy state updates confirm that the node is
                // connected and ready to send messages
                when (filterState) {
                    is ProxyFilterState.ProxyFilterUpdateAcknowledged,
                    is ProxyFilterState.ProxyFilterLimitReached,
                        -> {
                    }

                    else -> {
                    }
                }
            }
            .launchIn(scope = viewModelScope)
    }

    internal fun importZipPackage(uri: Uri, contentResolver: ContentResolver): ZipPackage {
        val data = contentResolver.openInputStream(uri)
            ?.use { it.readBytes() }
            ?: byteArrayOf()
        return ZipPackage(data = data)
    }

    internal suspend fun send(model: Model, message: AcknowledgedMeshMessage) =
        repository.send(model = model, ackedMessage = message)

    @AssistedFactory
    interface Factory {
        fun create(index: Int): Page2ViewModel
    }

}

internal data class Page2ScreenUiState(
    val proxyConnectionState: ProxyConnectionState = ProxyConnectionState(),
    val messageState: MessageState = NotStarted,
    val nodes: List<Node> = emptyList(),
    val targets: List<Target> = emptyList(),
)

private fun List<Node>.toTarget() {

}