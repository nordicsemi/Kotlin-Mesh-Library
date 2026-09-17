package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import android.content.ContentResolver
import android.net.Uri
import androidx.core.net.toUri
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
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.Failed
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.NotStarted
import no.nordicsemi.android.nrfmesh.core.common.Sending
import no.nordicsemi.android.nrfmesh.core.common.blobTransferServerModel
import no.nordicsemi.android.nrfmesh.core.common.firmwareUpdateServer
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.ProxyConnectionState
import no.nordicsemi.android.nrfmesh.core.data.checkForUpdates
import no.nordicsemi.android.nrfmesh.feature.dfu.util.FirmwareEntry
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Metadata
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Status
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Target
import no.nordicsemi.android.nrfmesh.feature.dfu.util.TargetState
import no.nordicsemi.android.nrfmesh.feature.dfu.util.UpdatePackage
import no.nordicsemi.android.nrfmesh.feature.dfu.util.ZipPackage
import no.nordicsemi.kotlin.mesh.core.ProxyFilterState
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdateMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppBind
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetKeyAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareUpdateFirmwareMetadataCheck
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareUpdateFirmwareMetadataStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareUpdateInformationGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareUpdateInformationStatus
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import java.net.URL
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
                    when (state.targets.isEmpty()) {
                        true -> {
                            state.copy(
                                targets = network.nodes
                                    .filter { node ->
                                        node.model(modelId = firmwareUpdateServer) != null &&
                                                node.model(modelId = blobTransferServerModel) != null
                                    }.mapNotNull { node ->
                                        node.models(modelId = firmwareUpdateServer)
                                            .firstOrNull()
                                            ?.let { model ->
                                                Target(
                                                    node = node,
                                                    targetState = when (model.isBound(index = index.toUShort())) {
                                                        true -> TargetState.Configured
                                                        else -> TargetState.ConfigurationRequired
                                                    }
                                                )
                                            }
                                    }
                                    .sortedBy { it.node.uuid == repository.proxyFilter.proxy?.uuid }
                            )
                        }

                        else -> state
                    }
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

    internal fun resetTargets() {
        _uiState.update { status ->
            status.copy(
                targets = status.targets
                    .toMutableList()
                    .map { target ->
                        if(target.targetState is TargetState.Ready){
                            val entries = target.targetState.entries.map { entry ->
                                entry.copy(status = Status.Unselected)
                            }
                            target.copy(targetState = TargetState.Ready(entries = entries))
                        } else target
                    }
            )
        }
    }

    /**
     * Checks the firmware compatibility
     * @param target The target node
     * @param indexOfEntry The index of the firmware entry
     * @param entry The firmware entry
     * @param metadata The metadata of the firmware
     */
    @OptIn(ExperimentalUuidApi::class)
    internal suspend fun checkCompatibility(
        target: Target,
        indexOfEntry: Int,
        entry: FirmwareEntry,
        metadata: ByteArray,
        isChecked: Boolean,
    ) {
        val targetIndex = _uiState.value.targets.indexOfFirst { it.node.uuid == target.node.uuid }
        val targetState = (target.targetState as TargetState.Ready)
        if (!isChecked) {
            _uiState.update { status ->
                status.copy(
                    targets = status.targets
                        .toMutableList()
                        .also {
                            it[targetIndex] = target.copy(
                                targetState = target.targetState.with(
                                    index = indexOfEntry,
                                    entry = entry.copy(status = Status.Unselected)
                                )
                            )
                        }.toList()
                )
            }
        } else {
            _uiState.update { status ->
                status.copy(
                    targets = status.targets
                        .toMutableList()
                        .also {
                            it[targetIndex] = target.copy(
                                targetState = target.targetState.with(
                                    index = indexOfEntry,
                                    entry = entry.copy(status = Status.CheckingMetadata)
                                )
                            )
                        }.toList()
                )
            }
            val firmwareUpdateServerModel =
                target.node.model(modelId = firmwareUpdateServer) ?: return
            val metadataStatusCheck = send(
                model = firmwareUpdateServerModel,
                message = FirmwareUpdateFirmwareMetadataCheck(
                    imageIndex = entry.index,
                    metadata = metadata
                )
            ) as? FirmwareUpdateFirmwareMetadataStatus
            metadataStatusCheck?.let {
                val updatedEntry = when (metadataStatusCheck.status) {
                    FirmwareUpdateMessageStatus.SUCCESS -> entry.copy(
                        status = Status.Selected(
                            additionalInformation = metadataStatusCheck.additionalInformation
                        )
                    )

                    else -> entry.copy(
                        status = Status.Error(message = metadataStatusCheck.status.debugDescription)
                    )
                }
                // Updates the entry in the state of the given target
                _uiState.update { status ->
                    status.copy(
                        targets = status.targets
                            .toMutableList()
                            .also {
                                it[targetIndex] = target.copy(
                                    targetState = targetState.with(
                                        index = indexOfEntry,
                                        entry = updatedEntry
                                    )
                                )
                            }.toList()
                    )
                }
            } ?: run {
                throw IllegalStateException("No response received during metadata check")
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    internal suspend fun downloadFirmwareInformation(target: Target) {
        val key = network.applicationKey(index = index.toUShort()) ?: return
        val node = target.node
        val firmwareUpdateServer = node.models(modelId = firmwareUpdateServer)
            .firstOrNull() ?: return
        // Make sure the Target Node knows the selected Network Key
        if (!node.knows(key = key.boundNetworkKey)) {
            val _ = send(node = node, message = ConfigNetKeyAdd(key = key.boundNetworkKey))
        }
        // Make sure the Target Node knows the selected Application Key
        if (!node.knows(key = key)) {
            val _ = send(node = node, message = ConfigAppKeyAdd(key = key))
        }
        // Make sure the selected App Key is bound to the Firmware Update Server model.
        if (!firmwareUpdateServer.isBound(key = key)) {
            val _ = send(
                node = node,
                message = ConfigModelAppBind(model = firmwareUpdateServer, applicationKey = key)
            )
        }
        val message = FirmwareUpdateInformationGet(firstIndex = 0, entriesLimit = 2)
        _uiState.value = _uiState.value.copy(messageState = Sending(message = message))
        try {
            send(model = firmwareUpdateServer, message = message)
                ?.let { response ->
                    val index = _uiState.value.targets
                        .indexOfFirst { it.node.uuid == target.node.uuid }
                    val status = response as FirmwareUpdateInformationStatus
                    _uiState.value = _uiState.value.copy(
                        messageState = Completed(
                            message = message,
                            response = response as MeshResponse
                        ),
                    )
                    lateinit var updateTarget: Target
                    try {
                        val entries = status.list.mapIndexed { index, image ->
                            try {
                                FirmwareEntry(
                                    index = index.toUByte(),
                                    firmware = image,
                                    availableUpdate = image.updateUri?.let { url ->
                                        val newUrl = url
                                            .toString()
                                            .replace(
                                                oldValue = "192.168.0.173",
                                                newValue = "192.168.68.63"
                                            )
                                            .toUri()
                                            .buildUpon()
                                            .appendPath("check")
                                            .appendQueryParameter(
                                                "cfwid",
                                                image.currentFirmwareId.bytes.toHexString()
                                            )
                                            .build()
                                            .let { URL(it.toString()) }
                                        checkForUpdates(newUrl)
                                    }
                                )
                            } catch (e: Exception) {
                                FirmwareEntry(
                                    index = index.toUByte(),
                                    firmware = image,
                                    availableUpdate = null
                                )
                            }
                        }
                        updateTarget = target
                            .copy(targetState = TargetState.Ready(entries = entries))
                    } catch (e: Exception) {
                        updateTarget = target.copy(
                            targetState = TargetState.Error(
                                message = e.message ?: "Unknown error"
                            )
                        )
                    } finally {
                        _uiState.update { status ->
                            if (index > -1) {
                                val list = status.targets
                                    .toMutableList()
                                    .also { it[index] = updateTarget }
                                status.copy(targets = list)
                            } else status
                        }
                    }
                }
                ?: run {
                    _uiState.value = _uiState.value.copy(
                        messageState = Failed(
                            message = message,
                            error = IllegalStateException("No response received")
                        ),
                    )
                }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                messageState = Failed(message = message, error = e),
            )
        }
    }

    internal suspend fun send(
        model: Model,
        message: AcknowledgedMeshMessage,
        applicationKey: ApplicationKey? = null,
    ) = repository.send(model = model, ackedMessage = message, applicationKey = applicationKey)

    internal suspend fun send1(
        model: Model,
        message: AcknowledgedMeshMessage,
        applicationKey: ApplicationKey? = null,
    ): MeshMessage? {
        _uiState.value = _uiState.value.copy(messageState = Sending(message = message))
        return try {
            repository.send(model = model, ackedMessage = message, applicationKey = applicationKey)
                ?.let { response ->
                    _uiState.value = _uiState.value.copy(
                        messageState = Completed(
                            message = message,
                            response = response as ConfigResponse
                        ),
                    )
                    response
                }
                ?: run {
                    _uiState.value = _uiState.value.copy(
                        messageState = Failed(
                            message = message,
                            error = IllegalStateException("No response received")
                        ),
                    )
                    null
                }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                messageState = Failed(message = message, error = e),
            )
            null
        }
    }

    private suspend fun send(node: Node, message: AcknowledgedConfigMessage): MeshMessage? {
        _uiState.value = _uiState.value.copy(messageState = Sending(message = message))
        return try {
            repository.send(node, message)?.let { response ->
                _uiState.value = _uiState.value.copy(
                    messageState = Completed(
                        message = message,
                        response = response as ConfigResponse
                    ),
                )
                response
            } ?: run {
                _uiState.value = _uiState.value.copy(
                    messageState = Failed(
                        message = message,
                        error = IllegalStateException("No response received")
                    ),
                )
                null
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                messageState = Failed(message = message, error = e),
            )
            null
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(index: Int): Page2ViewModel
    }
}

internal data class Page2ScreenUiState(
    val proxyConnectionState: ProxyConnectionState = ProxyConnectionState(),
    val messageState: MessageState = NotStarted,
    val targets: List<Target> = emptyList(),
)