package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toFile
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.Failed
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.NotStarted
import no.nordicsemi.android.nrfmesh.core.common.Sending
import no.nordicsemi.android.nrfmesh.core.common.blobTransferServer
import no.nordicsemi.android.nrfmesh.core.common.firmwareUpdateServer
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.NetworkConnectionState
import no.nordicsemi.android.nrfmesh.core.data.ProxyConnectionState
import no.nordicsemi.android.nrfmesh.core.data.checkForUpdates
import no.nordicsemi.android.nrfmesh.core.data.downloadFirmware
import no.nordicsemi.android.nrfmesh.core.data.saveToDownloads
import no.nordicsemi.android.nrfmesh.feature.dfu.util.FirmwareEntry
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Metadata
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Status
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Target
import no.nordicsemi.android.nrfmesh.feature.dfu.util.TargetState
import no.nordicsemi.android.nrfmesh.feature.dfu.util.UpdatePackage
import no.nordicsemi.android.nrfmesh.feature.dfu.util.ZipPackage
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.ConfigStatusMessage
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
import no.nordicsemi.kotlin.mesh.logger.LogCategory
import no.nordicsemi.kotlin.mesh.logger.LogLevel
import java.io.FileInputStream
import java.net.URL
import java.util.zip.ZipInputStream

@HiltViewModel(assistedFactory = Page2ViewModel.Factory::class)
internal class Page2ViewModel @AssistedInject internal constructor(
    @ApplicationContext private val context: Context,
    private val repository: CoreDataRepository,
    @Assisted private val index: Int,
) : ViewModel() {
    internal val uiState: StateFlow<Page2ScreenUiState>
        field = MutableStateFlow(Page2ScreenUiState())
    private lateinit var network: MeshNetwork
    private val json = Json {
        ignoreUnknownKeys = true    // Ignores the keys not available in the mesh network mode.
    }

    init {
        observeNetwork()
        observeProxyConnectionState()
        observeProxyFilterState()
    }

    /**
     * Observes the network state
     */
    private fun observeNetwork() {
        repository.networkEvents
            .mapNotNull { repository.meshNetwork }
            .onEach { network ->
                this.network = network
                // The name of the selected application key, shown when a target needs to be
                // configured for it.
                uiState.update {
                    it.copy(applicationKeyName = network.applicationKey(index = index.toUShort())?.name)
                }
                uiState.update { state ->
                    when (state.targets.isEmpty()) {
                        true -> {
                            state.copy(
                                targets = network.nodes
                                    .filter { node ->
                                        node.model(modelId = firmwareUpdateServer) != null &&
                                                node.model(modelId = blobTransferServer) != null
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
                            ).withProxy()
                        }

                        else -> state.copy(
                            targets = state.targets
                                .sortedBy { it.node.uuid == repository.proxyFilter.proxy?.uuid }
                        ).withProxy()
                    }
                }
            }
            .launchIn(scope = viewModelScope)
    }

    /**
     * Observes the proxy connection state
     */
    private fun observeProxyConnectionState() {
        repository.proxyConnectionStateFlow
            .onEach { proxyConnectionState ->
                uiState.update {
                    it.copy(proxyConnectionState = proxyConnectionState).withProxy()
                }
            }
            .launchIn(scope = viewModelScope)
    }

    /**
     * Locks the Distributor to the node that is connected now, which is the node that was chosen
     * on the previous pages.
     */
    internal fun lockDistributor() = uiState.update { it.copy(proxy = null).withProxy() }

    /**
     * Updates the Distributor and whether it is connected.
     *
     * The Distributor is the first connected proxy, and it is kept when the connection drops or
     * when another node gets connected. It is shown as disconnected until the same node is
     * connected again.
     */
    private fun Page2ScreenUiState.withProxy(): Page2ScreenUiState {
        val connected = when (proxyConnectionState.connectionState) {
            is NetworkConnectionState.Connected -> repository.proxyFilter.proxy
            else -> null
        }
        val distributor = proxy ?: connected
        return copy(
            proxy = distributor,
            isProxyConnected = connected != null && connected.uuid == distributor?.uuid
        )
    }

    /**
     * Observes the proxy filter state
     */
    private fun observeProxyFilterState() {
        repository.proxyFilter.proxyFilterStateFlow
            .onEach {
                // The proxy node is known once the proxy has replied to the filter messages, and
                // it is cleared when the proxy disconnects.
                uiState.update { it.withProxy() }
            }
            .launchIn(scope = viewModelScope)
    }

    /**
     * Loads the zip package from the given URI
     *
     * @param uri             URI of the zip package
     * @param contentResolver Content resolver to use
     */
    internal fun loadZipPackage(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            try {
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
                val fileName = contentResolver.fileName(uri)
                uiState.update {
                    it.copy(
                        updatePackage = UpdatePackage(
                            fileName = fileName,
                            zipPackage = zipPackage,
                            metadata = metadata
                        )
                    )
                }
                resetTargets()
            } catch (e: Exception) {
                repository.logger.log(
                    message = { "Error while loading zip file: $e" },
                    category = LogCategory.PROVISIONING,
                    level = LogLevel.ERROR
                )
            }
        }
    }

    /**
     * Loads the zip package from the given URI
     *
     * @param uri The URI of the zip package
     */
    private fun loadZipPackage(uri: Uri) {
        try {
            val file = uri.toFile()
            val bytes = file.readBytes()
            val zipPackage = ZipPackage(data = bytes)
            val metadata = FileInputStream(file).use { fis ->
                ZipInputStream(fis.buffered()).use { zis ->
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
            }
            uiState.update {
                it.copy(
                    updatePackage = UpdatePackage(
                        fileName = file.name,
                        zipPackage = zipPackage,
                        metadata = metadata
                    )
                )
            }
        } catch (e: Exception) {
            repository.logger.log(
                message = { "Error while loading zip file: $e" },
                category = LogCategory.PROVISIONING,
                level = LogLevel.ERROR
            )
        }
    }

    /**
     * Resets the targets to unselected
     */
    private fun resetTargets() {
        uiState.update { status ->
            status.copy(
                targets = status.targets
                    .toMutableList()
                    .map { target ->
                        if (target.targetState is TargetState.Ready) {
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
     * Downloads the firmware update information for a given target.
     *
     * @param target        Target to download the firmware update for
     * @param indexOfEntry  Index of the firmware entry
     * @param entry         Firmware entry belonging to the given target
     */
    internal suspend fun downloadFirmwareUpdate(
        target: Target,
        indexOfEntry: Int,
        entry: FirmwareEntry,
    ) {
        entry.availableUpdate?.let {
            val index = uiState.value.targets.indexOfFirst { it.node.uuid == target.node.uuid }
            downloadFirmware(
                context = context,
                url = entry.firmware.updateUri!!,
                firmwareId = entry.firmware.currentFirmwareId
            ).let { file ->
                val uri = saveToDownloads(zipFile = file)
                uiState.update { status ->
                    status.copy(
                        targets = status.targets
                            .toMutableList()
                            .also {
                                it[index] = target.copy(
                                    targetState = target.targetState
                                        .with(index = indexOfEntry, entry = entry)
                                )
                            }
                            .toList()
                            .sortedBy { it.node.uuid == repository.proxyFilter.proxy?.uuid }
                    )
                }
                loadZipPackage(uri = uri)
                checkCompatibility(
                    target = target,
                    indexOfEntry = indexOfEntry,
                    entry = entry,
                    metadata = uiState.value.updatePackage?.metadata ?: throw IllegalStateException(
                        "Unknown error, no metadata available"
                    ),
                    isChecked = true
                )
            }
        }
    }

    /**
     * Downloads the firmware information for a given target
     */
    internal suspend fun downloadFirmwareInformation(target: Target): Target {
        val key = network.applicationKey(index = index.toUShort()) ?: return target
        val node = target.node
        val firmwareUpdateServer = node.models(modelId = firmwareUpdateServer)
            .firstOrNull() ?: return target
        // Make sure the Target Node knows the selected Application Key and that it is bound to
        // the Firmware Update Server model.
        if (!bind(key = key, model = firmwareUpdateServer)) return target
        val targetIndex =
            uiState.value.targets.indexOfFirst { it.node.uuid == target.node.uuid }
        val message = FirmwareUpdateInformationGet(firstIndex = 0, entriesLimit = 2)
        return send(model = firmwareUpdateServer, message = message)
            ?.let { response ->
                val status = response as FirmwareUpdateInformationStatus
                val updatedTarget = try {
                    val entries = status.list.mapIndexed { index, fwInformation ->
                        try {
                            FirmwareEntry(
                                index = index.toUByte(),
                                firmware = fwInformation,
                                availableUpdate = fwInformation.updateUri?.let { url ->
                                    val newUrl = url
                                        .toString()
                                        .replace(
                                            oldValue = "192.168.0.173",
                                            newValue = "10.0.0.104"
                                        )
                                        .toUri()
                                        .buildUpon()
                                        .appendPath("check")
                                        .appendQueryParameter(
                                            "cfwid",
                                            fwInformation.currentFirmwareId.bytes.toHexString()
                                        )
                                        .build()
                                        .let { URL(it.toString()) }
                                    checkForUpdates(url = newUrl)
                                }
                            )
                        } catch (e: Exception) {
                            FirmwareEntry(
                                index = index.toUByte(),
                                firmware = fwInformation,
                                availableUpdate = null
                            )
                        }
                    }
                    target
                        .copy(targetState = TargetState.Ready(entries = entries))
                } catch (e: Exception) {
                    target.copy(
                        targetState = TargetState.Error(message = e.message ?: "Unknown error")
                    )
                }
                uiState.update { status ->
                    status.copy(
                        targets = status.targets
                            .toMutableList()
                            .also { it[targetIndex] = updatedTarget }
                            .toList()
                            .sortedBy { it.node.uuid == repository.proxyFilter.proxy?.uuid }
                    )
                }
                updatedTarget
            }
            ?: run {
                uiState.update {
                    it.copy(
                        messageState = Failed(
                            message = message,
                            error = IllegalStateException("No response received")
                        ),
                    )
                }
                target
            }
    }

    /**
     * Checks the firmware compatibility
     * @param target The target node
     * @param indexOfEntry The index of the firmware entry
     * @param entry The firmware entry
     * @param metadata The metadata of the firmware
     */
    internal suspend fun checkCompatibility(
        target: Target,
        indexOfEntry: Int,
        entry: FirmwareEntry,
        metadata: Metadata,
        isChecked: Boolean,
    ) {
        // The target passed in may be a stale snapshot, e.g. when selecting all the targets, so
        // the latest target is always read from the state before updating one of its entries.
        fun updateEntry(updatedEntry: FirmwareEntry) = uiState.update { status ->
            val targetIndex = status.targets.indexOfFirst { it.node.uuid == target.node.uuid }
            if (targetIndex < 0) return@update status
            val current = status.targets[targetIndex]
            status.copy(
                targets = status.targets
                    .toMutableList()
                    .also {
                        it[targetIndex] = current.copy(
                            targetState = current.targetState.with(
                                index = indexOfEntry,
                                entry = updatedEntry
                            )
                        )
                    }.toList()
            )
        }
        if (!isChecked) {
            updateEntry(updatedEntry = entry.copy(status = Status.Unselected))
        } else {
            updateEntry(updatedEntry = entry.copy(status = Status.CheckingMetadata))
            val firmwareUpdateServerModel =
                target.node.model(modelId = firmwareUpdateServer) ?: return
            val metadataStatusCheck = send(
                model = firmwareUpdateServerModel,
                message = FirmwareUpdateFirmwareMetadataCheck(
                    imageIndex = entry.index,
                    metadata = metadata.metadataOctets ?: byteArrayOf()
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
                        status = Status.Error(
                            message = if (metadataStatusCheck.status == FirmwareUpdateMessageStatus.METADATA_CHECK_FAILED) {
                                "Invalid firmware package"
                            } else {
                                metadataStatusCheck.status.debugDescription
                            }
                        )
                    )
                }
                // Updates the entry in the state of the given target
                updateEntry(updatedEntry = updatedEntry)
            } ?: run {
                throw IllegalStateException("No response received during metadata check")
            }
        }
    }

    /**
     * Configures the given target for the selected application key, so that its Firmware Update
     * Server model can be used.
     *
     * The key is added to the target node, together with the network key it is bound to, if the
     * node does not know them yet, and then bound to the Firmware Update Server model. The target
     * is marked as configured once the key is bound to the model.
     *
     * @param target The target to configure.
     */
    internal suspend fun configure(target: Target) {
        val key = network.applicationKey(index = index.toUShort()) ?: return
        val model = target.node.models(modelId = firmwareUpdateServer).firstOrNull() ?: return
        if (bind(key = key, model = model)) {
            uiState.update { state ->
                state.copy(
                    targets = state.targets.map {
                        when (it.node.uuid == target.node.uuid) {
                            true -> it.copy(targetState = TargetState.Configured)
                            false -> it
                        }
                    }
                )
            }
        }
    }

    /**
     * Makes sure the given application key is known to the node of the given model, and that it is
     * bound to the model.
     *
     * @param key   Application key to bind.
     * @param model Model to bind the application key to.
     * @return True if the application key is bound to the model, false otherwise.
     */
    private suspend fun bind(key: ApplicationKey, model: Model): Boolean {
        val node = model.parentElement?.parentNode ?: return false
        // Make sure the node knows the network key the application key is bound to.
        if (!node.knows(key = key.boundNetworkKey)) {
            val response = send(node = node, message = ConfigNetKeyAdd(key = key.boundNetworkKey))
            if ((response as? ConfigStatusMessage)?.isSuccess != true) return false
        }
        // Make sure the node knows the application key.
        if (!node.knows(key = key)) {
            val response = send(node = node, message = ConfigAppKeyAdd(key = key))
            if ((response as? ConfigStatusMessage)?.isSuccess != true) return false
        }
        // Make sure the application key is bound to the model.
        if (!model.isBound(key = key)) {
            val response = send(
                node = node,
                message = ConfigModelAppBind(model = model, applicationKey = key)
            )
            if ((response as? ConfigStatusMessage)?.isSuccess != true) return false
        }
        return model.isBound(key = key)
    }

    /**
     * Sends a message to the given model
     *
     * @param model             Model to send the message to
     * @param message           Message to send
     * @param applicationKey    Application key to send the message with
     * @return Response from the model or null if the message times out or no response was received
     */
    internal suspend fun send(
        model: Model,
        message: AcknowledgedMeshMessage,
        applicationKey: ApplicationKey? = null,
    ): MeshMessage? {
        uiState.value = uiState.value.copy(messageState = Sending(message = message))
        return try {
            repository.send(model = model, ackedMessage = message, applicationKey = applicationKey)
                ?.let { response ->
                    uiState.value = uiState.value.copy(
                        messageState = Completed(
                            message = message,
                            response = response as MeshResponse
                        ),
                    )
                    response
                }
                ?: run {
                    uiState.value = uiState.value.copy(
                        messageState = Failed(
                            message = message,
                            error = IllegalStateException("No response received")
                        ),
                    )
                    null
                }
        } catch (e: Exception) {
            uiState.value = uiState.value.copy(
                messageState = Failed(message = message, error = e),
            )
            null
        }
    }

    /**
     * Sends a message to the given model
     *
     * @param node              Node to send the message to
     * @param message           Message to send
     * @return Response from the model or null if the message times out or no response was received
     */
    private suspend fun send(node: Node, message: AcknowledgedConfigMessage): MeshMessage? {
        uiState.value = uiState.value.copy(messageState = Sending(message = message))
        return try {
            repository.send(node, message)?.let { response ->
                uiState.value = uiState.value.copy(
                    messageState = Completed(
                        message = message,
                        response = response as ConfigResponse
                    ),
                )
                response
            } ?: run {
                uiState.value = uiState.value.copy(
                    messageState = Failed(
                        message = message,
                        error = IllegalStateException("No response received")
                    ),
                )
                null
            }
        } catch (e: Exception) {
            uiState.value = uiState.value.copy(
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
    val proxy: Node? = null,
    val isProxyConnected: Boolean = false,
    val targets: List<Target> = emptyList(),
    val updatePackage: UpdatePackage? = null,
    val applicationKeyName: String? = null,
)

private fun ContentResolver.fileName(uri: Uri) = when (uri.scheme) {
    ContentResolver.SCHEME_CONTENT -> query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null, null, null,
    )?.use { cursor ->
        val i = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (i >= 0 && cursor.moveToFirst()) cursor.getString(i) else null
    }

    else -> ""
}