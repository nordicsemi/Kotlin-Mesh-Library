package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.VpnKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.NotStarted
import no.nordicsemi.android.nrfmesh.core.common.blobTransferClient
import no.nordicsemi.android.nrfmesh.core.common.blobTransferServer
import no.nordicsemi.android.nrfmesh.core.common.firmwareDistributionServer
import no.nordicsemi.android.nrfmesh.core.common.firmwareUpdateClient
import no.nordicsemi.android.nrfmesh.core.common.firmwareUpdateServer
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.ProxyConnectionState
import no.nordicsemi.android.nrfmesh.core.data.configurator.AppTask
import no.nordicsemi.android.nrfmesh.core.data.configurator.ConfigTask
import no.nordicsemi.android.nrfmesh.core.data.configurator.MeshTask
import no.nordicsemi.android.nrfmesh.core.data.configurator.Messenger
import no.nordicsemi.android.nrfmesh.core.data.configurator.TaskStatus
import no.nordicsemi.android.nrfmesh.core.data.name
import no.nordicsemi.android.nrfmesh.feature.dfu.DfuSession
import no.nordicsemi.android.nrfmesh.feature.dfu.util.DfuParameters
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Receiver
import no.nordicsemi.android.nrfmesh.feature.dfu.util.UpdatePackage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppBind
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelSubscriptionAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelSubscriptionVirtualAddressAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetKeyAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionReceiversAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionReceiversDeleteAll
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.UnassignedAddress
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@HiltViewModel
internal class Page4ViewModel @Inject internal constructor(
    private val repository: CoreDataRepository,
) : ViewModel() {
    val uiState: StateFlow<Page4ScreenUiState>
        field = MutableStateFlow(Page4ScreenUiState())
    private lateinit var network: MeshNetwork
    private var enqueuedSession: DfuSession? = null
    private var executionJob: Job? = null

    init {
        observeNetwork()
    }

    override fun onCleared() {
        clear()
    }

    /**
     * Stops the execution and clears the tasks enqueued by this screen.
     *
     * The execution is cancelled first, so that running tasks do not update the cleared lists.
     * Only the messengers used by this screen are cleared.
     */
    internal fun clear() {
        executionJob?.cancel()
        executionJob = null
        uiState.value.targets.forEach { uuid ->
            repository.messengers.messenger(uuid = uuid)?.clear()
        }
        enqueuedSession = null
        uiState.update {
            it.copy(
                targets = emptyList(),
                names = emptyMap(),
                tasks = emptyMap(),
                isCompleted = false
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun observeNetwork() {
        repository.networkEvents
            .mapNotNull { repository.meshNetwork }
            .onEach { network = it }
            .launchIn(scope = viewModelScope)
    }

    /**
     * Enqueues the tasks required to perform the firmware update based on the given session.
     *
     * The session is ignored if it is incomplete or if the tasks for the same session have
     * already been enqueued, e.g. after a configuration change. If the session has changed, e.g.
     * the user navigated back and modified the selection, the previously enqueued tasks are
     * discarded.
     */
    internal fun enqueue(session: DfuSession) {
        val updatePackage = session.updatePackage ?: return
        enqueuedSession = session
        val key = repository.meshNetwork?.applicationKey(index = session.keyIndex.toUShort()) ?: return
        enqueueTasks(
            receivers = session.receivers,
            dfuParameters = session.dfuParameters,
            updatePackage = updatePackage,
            key = key
        )
        // The mesh network manager sends one message at a time, so there is nothing to gain from
        // running the messengers in parallel. They are run one after the other instead, receivers
        // first, so that keys, binds and subscriptions are in place before the distributor is
        // given the list of receivers.
        val distributor = repository.proxyFilter.proxy
        val executionOrder = session.receivers
            .mapNotNull { network.node(address = it.address)?.uuid }
            .distinct()
            .filter { it != distributor?.uuid } + listOfNotNull(distributor?.uuid)
        uiState.update { state ->
            state.copy(
                targets = executionOrder,
                names = executionOrder.associateWith { network.node(uuid = it)?.name ?: "Unknown" },
                tasks = emptyMap(),
                isCompleted = false
            )
        }
        executionJob = viewModelScope.launch {
            // Observe every messenger before executing so that the tasks of all the nodes are
            // listed from the start. The observers are children of this job and are cancelled
            // with it.
            executionOrder.forEach { uuid ->
                repository.messengers.messenger(uuid = uuid)?.let { messenger ->
                    launch { observeMessenger(uuid = uuid, messenger = messenger) }
                }
            }
            executionOrder.forEach { uuid ->
                repository.messengers.messenger(uuid = uuid)?.execute()
            }
            // The current tasks are read from the messengers rather than from the ui state, as the
            // observers may not have published the last status update yet.
            val isCompleted = executionOrder.all { uuid ->
                repository.messengers.messenger(uuid = uuid)
                    ?.meshTaskFlow?.first()
                    ?.all { it.status is TaskStatus.Completed } ?: true
            }
            uiState.update { it.copy(isCompleted = isCompleted) }
        }
    }

    /**
     * Observes the tasks of the given messenger and publishes them in the ui state.
     */
    private suspend fun observeMessenger(uuid: Uuid, messenger: Messenger) {
        messenger.meshTaskFlow.collect { meshTasks ->
            uiState.update { state -> state.copy(tasks = state.tasks + (uuid to meshTasks)) }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun enqueueTasks(
        receivers: List<Receiver>,
        dfuParameters: DfuParameters,
        updatePackage: UpdatePackage,
        key: ApplicationKey,
    ) {
        val distributor = repository.proxyFilter.proxy ?: return
        val firmwareDistributionServer =
            distributor.model(modelId = firmwareDistributionServer) ?: throw IllegalStateException("Distributor does not have a Firmware Distribution Server model")
        val element = firmwareDistributionServer.parentElement ?: throw IllegalStateException("Firmware Distribution Server model does not have a parent element")
        val firmwareUpdateClient = element.model(modelId = firmwareUpdateClient) ?: throw IllegalStateException("Element does not have a Firmware Update Client model")
        val blobTransferClient = element.model(modelId = blobTransferClient) ?: throw IllegalStateException("Element does not have a BLOB Transfer Client model")
        // Before starting the DFU we need to bind the selected Application Key
        // to the BLOB Transfer models and Firmware Update models.
        bind(keys = listOf(key), models = listOf(firmwareUpdateClient, blobTransferClient))

        // The Firmware Update Server models on Target Nodes have already been bound to the key on
        // the previous screen, where there user was using it to check metadata compatibility,
        // therefore only BLOB Transfer Server models are listed.
        // In addition, if a multicast distribution was selected, both Firmware Update Server and
        // BLOB Transfer Server models must be subscribed to the selected group.
        // However, as these models are related, only the BLOB Transfer Server models, already
        // listed will be subscribed.

        // List BLOB Transfer Server models that are on the same Element as the Firmware Update
        // Server model.
        val models = receivers
            // Receives to nodes
            .mapNotNull { network.node(address = it.address) }
            // list Firmware Update Server models
            .flatMap { it.models(modelId = firmwareUpdateServer) }
            // list parent elements
            .mapNotNull { it.parentElement }
            // list BLOB Transfer Server models
            .flatMap {
                it.models.filter { model ->
                    model.isBluetoothSigAssigned && model.modelId == blobTransferServer
                }
            }
        // Bind the selected Application Key to the selected models
        bind(keys = listOf(key), models = models)

        // If a multicast distribution was selected, subscribe to the selected group
        if (dfuParameters.address !is UnassignedAddress) {
            network.group(address = dfuParameters.address.address)?.let { group ->
                subscribe(models = models, groups = listOf(group))
            }
        }

        // Finally, add Receivers. The number of receivers is limited to 10 per message. Maximum
        // number is ensured by the Page2, which disallowed selecting higher number of receivers
        // than Distributor's limit
        // As the Back button is enabled and the user can go back to modify the list of Receivers,
        // we need to clear the list of receivers first.
        clearReceivers(distributor = distributor, model = firmwareDistributionServer)
        addReceivers(
            distributor = distributor,
            receivers = receivers,
            model = firmwareDistributionServer
        )
    }

    /**
     * Binds the given application keys to the given models.
     *
     * This function will go through the parent node of hte model to check if the Application Key
     * is known to the node and if the bound Network Key is known to the node as well. If not know
     * the bound Network Key will be added first prior to sending the application key and finally
     * the application key will be bound.
     *
     * @param keys List of Application Keys to bind.
     * @param models List of Models to which the application keys should be bound.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun bind(keys: List<ApplicationKey>, models: List<Model>) {
        // Cache to prevent sending the same message multiple times
        val cache = mutableMapOf<Node, MutableList<ApplicationKey>>()
        models.forEach { model ->
            model.parentElement?.parentNode?.let { node ->
                // Get the correct messenger by based on the parent node of the model
                val messenger = repository.messengers
                    .run {
                        messenger(uuid = node.uuid) ?: create(uuid = node.uuid)
                    }
                keys.forEach { appKey ->
                    // Checks if the application key is not known to the node
                    // In addition we should check if the Check if the application key messages are
                    // not queued already and is not known to the node
                    if (cache[node]?.contains(appKey) != true && !node.knows(appKey)) {
                        val boundNetKey = appKey.boundNetworkKey
                        // Checks if the bound network key of the application key is known to the node
                        if (!node.knows(key = boundNetKey)) {
                            // Enqueue a task to add the bound network key to the node
                            messenger.enqueueTask(
                                task = ConfigTask(
                                    icon = Icons.Outlined.VpnKey,
                                    label = "Adding ${boundNetKey.name} to ${node.name}",
                                    message = ConfigNetKeyAdd(boundNetKey),
                                    target = node
                                )
                            )
                        } else {
                            // Enqueue a task to add the application key to the node
                            messenger.enqueueTask(
                                task = ConfigTask(
                                    icon = Icons.Outlined.VpnKey,
                                    label = "Adding ${appKey.name} to ${node.name}",
                                    message = ConfigAppKeyAdd(appKey),
                                    target = node
                                )
                            )
                        }
                        cache.getOrPut(key = node, defaultValue = { mutableListOf() }).add(appKey)
                    }
                    // Checks if the application key is not bound to the model
                    if (!model.isBound(key = appKey)) {
                        // Enqueue a task to bind the application key to the model
                        messenger.enqueueTask(
                            task = ConfigTask(
                                icon = Icons.Outlined.AddLink,
                                label = "Binding ${appKey.name} to ${model.name()}",
                                message = ConfigModelAppBind(
                                    model = model,
                                    applicationKey = appKey
                                ),
                                target = node
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Subscribes the given models to the given groups.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun subscribe(models: List<Model>, groups: List<Group>) {
        models.forEach { model ->
            model.parentElement?.parentNode?.let { node ->
                val messenger = repository.messengers
                    .run { messenger(uuid = node.uuid) ?: create(uuid = node.uuid) }
                groups.forEach { group ->
                    if (!model.isSubscribedTo(group)) {
                        messenger.enqueueTask(
                            task = ConfigTask(
                                icon = Icons.Outlined.GroupWork,
                                label = "Subscribing ${model.name()} to ${group.name}",
                                message = when (group.address is VirtualAddress) {
                                    true -> ConfigModelSubscriptionVirtualAddressAdd(
                                        model = model,
                                        group = group
                                    )

                                    false -> ConfigModelSubscriptionAdd(
                                        model = model,
                                        group = group
                                    )
                                },
                                target = node
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Clears the distribution receivers on the given model.
     *
     * @param distributor Distributor node
     * @param model Model to clear the distribution receivers on
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun clearReceivers(distributor: Node, model: Model) {
        val messenger = repository.messengers
            .run {
                messenger(uuid = distributor.uuid) ?: create(uuid = distributor.uuid)
            }
        messenger.enqueueTask(
            task = AppTask(
                icon = Icons.Outlined.Recycling,
                model = model,
                label = "Clearing distribution receivers on ${model.name()}",
                message = FirmwareDistributionReceiversDeleteAll(),
                applicationKey = model.boundApplicationKeys.first()
            )
        )
    }

    /**
     * Adds the given receivers to the given model.
     *
     * @param distributor Distributor node
     * @param receivers   List of receivers to add
     * @param model       Model to add the receivers to
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun addReceivers(distributor: Node, receivers: List<Receiver>, model: Model) {
        val messenger = repository.messengers
            .run { messenger(uuid = distributor.uuid) ?: create(uuid = distributor.uuid) }
        receivers.chunked(10).forEach { chunks ->
            messenger.enqueueTask(
                task = AppTask(
                    icon = Icons.Outlined.Add,
                    model = model,
                    label = if (chunks.size == 1)
                        "Adding Receiver to ${model.name()}"
                    else
                        "Adding ${chunks.size} Receivers to ${model.name()}",
                    message = FirmwareDistributionReceiversAdd(receivers = chunks.toReceiverList()),
                    applicationKey = model.boundApplicationKeys.first()
                )
            )
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
internal data class Page4ScreenUiState(
    val proxyConnectionState: ProxyConnectionState = ProxyConnectionState(),
    val messageState: MessageState = NotStarted,
    val targets: List<Uuid> = emptyList(),
    val names: Map<Uuid, String> = emptyMap(),
    val tasks: Map<Uuid, List<MeshTask>> = emptyMap(),
    val isCompleted: Boolean = false,
)

fun List<Receiver>.toReceiverList() = map {
    FirmwareDistributionReceiversAdd.Receiver(
        address = it.address,
        imageIndex = it.imageIndex
    )
}