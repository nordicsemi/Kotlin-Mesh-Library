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
import kotlinx.serialization.json.Json
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.NotStarted
import no.nordicsemi.android.nrfmesh.core.common.blobTransferServerModel
import no.nordicsemi.android.nrfmesh.core.common.firmwareUpdateServer
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.ProxyConnectionState
import no.nordicsemi.android.nrfmesh.feature.dfu.util.FirmwareEntries.ConfigurationRequired
import no.nordicsemi.android.nrfmesh.feature.dfu.util.FirmwareEntries.Configured
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Metadata
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Target
import no.nordicsemi.android.nrfmesh.feature.dfu.util.UpdatePackage
import no.nordicsemi.android.nrfmesh.feature.dfu.util.ZipPackage
import no.nordicsemi.kotlin.mesh.core.ProxyFilterState
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import java.util.zip.ZipInputStream
import kotlin.uuid.ExperimentalUuidApi

@HiltViewModel(assistedFactory = Page2ViewModel.Factory::class)
internal class Page2ViewModel @AssistedInject internal constructor(
    private val repository: CoreDataRepository,
    @Assisted private val index: Int,
) : ViewModel() {
    private val _uiState = MutableStateFlow(Page2ScreenUiState())
    internal val uiState = _uiState.asStateFlow()
    private lateinit var network: MeshNetwork
    private val json = Json {
        ignoreUnknownKeys = true    // Ignores the keys not available in the mesh network mode.
    }

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
                        targets = network.nodes
                            .filter { node ->
                                node.model(modelId = firmwareUpdateServer) != null &&
                                        node.model(modelId = blobTransferServerModel) != null
                            }.mapNotNull { node ->
                                node.models(modelId = firmwareUpdateServer)
                                    .firstOrNull()
                                    ?.isBound(index = index.toUShort())
                                    ?.let { isBound ->
                                        Target(
                                            node = node,
                                            entries = when (isBound) {
                                                true -> Configured
                                                else -> ConfigurationRequired
                                            }
                                        )
                                    }
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

    internal fun importZipPackage(uri: Uri, contentResolver: ContentResolver): UpdatePackage {
        val metadata = ZipInputStream(
            contentResolver.openInputStream(uri)?.buffered()
                ?: throw IllegalStateException("Unable to open input stream for URI: $uri")
        ).use { zis ->
            lateinit var metadata: Metadata
            while (true) {
                val entry = zis.nextEntry ?: break
                if (entry.name == "ble_mesh_metadata.json") {
                    metadata = json.decodeFromString(zis.readBytes().decodeToString())
                    break
                }
            }
            metadata
        }

        val data = contentResolver.openInputStream(uri)
            ?.use { it.readBytes() }
            ?: byteArrayOf()
        val zipPackage = ZipPackage(data = data)
        return UpdatePackage(zipPackage = zipPackage, metadata = metadata)
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