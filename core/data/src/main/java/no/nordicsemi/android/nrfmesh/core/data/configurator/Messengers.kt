package no.nordicsemi.android.nrfmesh.core.data.configurator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.Diversity1
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.NetworkCell
import androidx.compose.material.icons.outlined.NetworkPing
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.WifiTethering
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.nrfmesh.core.common.name
import no.nordicsemi.android.nrfmesh.core.data.isDestinedToLocalNode
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigStatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigBeaconSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigDefaultTtlSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigFriendSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigGattProxySet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigHeartbeatPublicationSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppBind
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationVirtualAddressSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelSubscriptionAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelSubscriptionVirtualAddressAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetKeyAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetworkTransmitSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigRelaySet
import no.nordicsemi.kotlin.mesh.core.model.AllNodes
import no.nordicsemi.kotlin.mesh.core.model.FeatureState
import no.nordicsemi.kotlin.mesh.core.model.FixedGroupAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.PublicationAddress
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import no.nordicsemi.kotlin.mesh.core.model.knownTo
import kotlin.math.min
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
class Messengers(private val meshNetworkManager: MeshNetworkManager) {
    val messengers: Map<Uuid, Messenger>
        field = mutableMapOf<Uuid, Messenger>()

    /**
     * Creates a new configurator for the given node.
     *
     * @param uuid Uuid of the node.
     */
    fun create(uuid: Uuid) = Messenger(meshNetworkManager = meshNetworkManager)
        .also { messengers[uuid] = it }

    /**
     * Gets the configurator for the given node.
     *
     * @param uuid Uuid of the node.
     */
    fun messenger(uuid: Uuid) = messengers[uuid]

    /**
     * Removes the configurator for the given node.
     *
     * @param uuid Uuid of the node.
     */
    @Suppress("unused")
    fun removeMessenger(uuid: Uuid) {
        messengers.remove(uuid)
    }
}


/**
 * Configurator is responsible for configuring a node.
 *
 * The messenger does not own a coroutine scope. [execute] and [retry] are suspending functions,
 * so the caller decides in which scope the tasks run, and cancels them by cancelling its job.
 *
 * @param meshNetworkManager MeshNetworkManager to be used.
 */
@OptIn(ExperimentalUuidApi::class)
class Messenger(private val meshNetworkManager: MeshNetworkManager) {
    private val meshNetwork: MeshNetwork?
        get() = meshNetworkManager.network
    private val configTasksFlow = MutableStateFlow<List<ConfigTask>>(mutableListOf())
    private val appTasksFlow = MutableStateFlow<List<AppTask>>(mutableListOf())

    @Suppress("unused")
    private val isCompleted: Boolean
        get() = configTasksFlow.value.none {
            it.status !is TaskStatus.Completed
        } && appTasksFlow.value.none {
            it.status !is TaskStatus.Completed
        }


    val meshTaskFlow: Flow<List<MeshTask>>
        get() = combine(
            flow = configTasksFlow,
            flow2 = appTasksFlow
        ) { configTasks, appTasks ->
            configTasks + appTasks
        }

    /**
     * Enqueues the configuration tasks
     */
    fun enqueueTask(task: MeshTask) {
        when (task) {
            is ConfigTask ->
                configTasksFlow.update {
                    (it + task).toMutableList()
                }

            is AppTask -> appTasksFlow.update {
                (it + task).toMutableList()
            }
        }
    }

    /**
     * Executes the enqueued tasks, suspending until all of them have been processed.
     *
     * Cancelling the calling coroutine stops the execution.
     */
    suspend fun execute() {
        executeConfigurationTasks()
        executeAppTasks()
    }

    /**
     * Retry the enqueued tasks, suspending until all of them have been processed.
     */
    suspend fun retry() {
        executeConfigurationTasks()
        executeAppTasks()
    }

    /**
     * Clears the enqueued tasks.
     *
     * This does not stop a running [execute] or [retry], cancel the calling job before clearing.
     */
    fun clear() {
        configTasksFlow.value = emptyList()
        appTasksFlow.value = emptyList()
    }

    private suspend fun executeConfigurationTasks() {
        configTasksFlow.value.forEachIndexed { index, task ->
            var tempTask = task
            configTasksFlow.update {
                tempTask = tempTask.copy(status = TaskStatus.InProgress)
                it.toMutableList()
                    .apply { this[index] = tempTask }
            }
            try {
                meshNetworkManager.send(
                    message = task.message,
                    node = task.target,
                    initialTtl = null
                )?.let {
                    tempTask = if (it is ConfigStatusMessage) {
                        when (it.isSuccess) {
                            true -> tempTask.copy(status = TaskStatus.Completed)
                            else -> tempTask.copy(status = TaskStatus.Error(error = it.message))
                        }
                    } else {
                        tempTask.copy(status = TaskStatus.Completed)
                    }

                } ?: run { tempTask = tempTask.copy(status = TaskStatus.Skipped) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                tempTask =
                    tempTask.copy(status = TaskStatus.Error(error = e.message ?: "Unknown Error"))
                configTasksFlow.update {
                    it.toMutableList()
                        .apply { this[index] = tempTask }
                }
                return@forEachIndexed
            }
            configTasksFlow.update {
                it.toMutableList()
                    .apply { this[index] = tempTask }
            }
        }
    }

    private suspend fun executeAppTasks() {
        val network = requireNotNull(meshNetwork) { "Mesh network is not available" }
        appTasksFlow.value.forEachIndexed { index, task ->
            var tempTask = task
            val parentElement = requireNotNull(tempTask.model.parentElement) { "Parent element is not available" }
            appTasksFlow.update {
                tempTask = task.copy(status = TaskStatus.InProgress)
                it.toMutableList()
                    .apply { this[index] = tempTask }
            }
            val ttl = when (isDestinedToLocalNode(
                meshNetwork = network,
                destination = parentElement.unicastAddress
            )) {
                true -> 1.toUByte() // Use TTL 1 for messages destined to the local node
                else -> null
            }
            try {
                when (val message = task.message) {
                    is AcknowledgedMeshMessage -> {
                        meshNetworkManager.send(
                            message = message,
                            model = task.model,
                            applicationKey = task.applicationKey,
                            initialTtl = ttl,
                        )?.let {
                            tempTask = tempTask.copy(status = TaskStatus.Completed)
                        } ?: run {
                            tempTask =
                                tempTask.copy(status = TaskStatus.Error(error = "Response not received!"))
                        }
                    }

                    is UnacknowledgedMeshMessage -> {
                        meshNetworkManager.send(
                            message = message,
                            model = task.model,
                            applicationKey = task.applicationKey,
                            initialTtl = ttl
                        )
                        tempTask = tempTask.copy(status = TaskStatus.Completed)
                    }

                    else -> tempTask =
                        tempTask.copy(status = TaskStatus.Error(error = "Unsupported message type"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                tempTask =
                    tempTask.copy(status = TaskStatus.Error(error = e.message ?: "Unknown Error"))
                appTasksFlow.update {
                    it.toMutableList()
                        .apply { this[index] = tempTask }
                }
                return@forEachIndexed
            }
            appTasksFlow.update {
                it.toMutableList()
                    .apply { this[index] = tempTask }
            }
        }
    }
}

/**
 * Enqueues the reconfiguration tasks
 *
 * @param messenger          Messenger to be used.
 * @param meshNetwork        MeshNetwork to be used.
 * @param originalNode       Original Node from which the configuration should be derived from
 * @param target            New Node.
 */
@OptIn(ExperimentalUuidApi::class)
fun enqueueReconfiguration(
    messenger: Messenger,
    meshNetwork: MeshNetwork,
    originalNode: Node,
    target: Node,
) {
    originalNode.run {
        defaultTTL?.let {
            messenger.enqueueTask(
                task = ConfigTask(
                    icon = Icons.Outlined.Timer,
                    label = "Set Default TTL to $it on ${target.name}",
                    message = ConfigDefaultTtlSet(ttl = it),
                    target = target
                )
            )
        }
        secureNetworkBeacon?.let {
            messenger.enqueueTask(
                task = ConfigTask(
                    icon = Icons.Outlined.WifiTethering,
                    label = "${if (it) "Enable" else "Disable"} Secure Network beacon on ${target.name}",
                    message = ConfigBeaconSet(enable = it),
                    target = target
                )
            )
        }
        networkTransmit?.let {
            messenger.enqueueTask(
                task = ConfigTask(
                    icon = Icons.Outlined.NetworkPing,
                    label = "Set Network Transmit",
                    message = ConfigNetworkTransmitSet(networkTransmit = it),
                    target = target
                )
            )
        }
        features.run { ->
            relay?.let { relay ->
                when (relay.state) {
                    FeatureState.Enabled -> {
                        relayRetransmit?.let {
                            messenger.enqueueTask(
                                task = ConfigTask(
                                    icon = Icons.Outlined.NetworkPing,
                                    label = "Set Retransmit to ${it.interval} ms on ${target.name}",
                                    message = ConfigRelaySet(relayRetransmit = it),
                                    target = target
                                )
                            )
                        }
                    }

                    FeatureState.Disabled -> messenger.enqueueTask(
                        task = ConfigTask(
                            icon = Icons.Outlined.NetworkCell,
                            label = "Disable Relay Feature on ${target.name}",
                            message = ConfigRelaySet(),
                            target = target
                        )
                    )

                    FeatureState.Unsupported -> {
                        // Do nothing
                    }
                }
            }

            proxy?.let { proxy ->
                if (proxy.state is FeatureState.Unsupported) {
                    messenger.enqueueTask(
                        task = ConfigTask(
                            icon = Icons.Outlined.Hub,
                            label = "${if (proxy.state is FeatureState.Enabled) "Enabling" else "Disabling"} GATT Proxy on ${target.name}",
                            message = ConfigGattProxySet(enable = proxy.state is FeatureState.Enabled),
                            target = target
                        )
                    )
                }
            }

            friend?.let { friend ->
                if (friend.state != FeatureState.Unsupported) {
                    messenger.enqueueTask(
                        task = ConfigTask(
                            icon = Icons.Outlined.Diversity1,
                            label = "${if (friend.state is FeatureState.Enabled) "Enabling" else "Disabling"} Friend Feature on ${target.name}",
                            message = ConfigFriendSet(enable = friend.state is FeatureState.Enabled),
                            target = target
                        )
                    )
                }
            }
        }
        meshNetwork.networkKeys
            .knownTo(node = this)
            // During provisioning a network key is already added let's filter it out.
            .filter { !target.knows(key = it) }
            .forEach {
                messenger.enqueueTask(
                    task = ConfigTask(
                        icon = Icons.Outlined.VpnKey,
                        label = "Add ${it.name} to ${target.name}",
                        message = ConfigNetKeyAdd(key = it),
                        target = target
                    )
                )
            }
        meshNetwork.applicationKeys
            .knownTo(this)
            .forEach {
                messenger.enqueueTask(
                    task = ConfigTask(
                        icon = Icons.Outlined.VpnKey,
                        label = "Add ${it.name} to ${target.name}",
                        message = ConfigAppKeyAdd(key = it),
                        target = target
                    )
                )
            }
        heartbeatPublication?.let { heartbeat ->
            meshNetwork.networkKey(heartbeat.index)?.let {
                messenger.enqueueTask(
                    task = ConfigTask(
                        icon = Icons.Outlined.MonitorHeart,
                        label = "Set up Heartbeat Publications to 0x${
                            heartbeat.address.address.toHexString(HexFormat.UpperCase)
                        }",
                        message = ConfigHeartbeatPublicationSet(
                            networkKeyIndex = heartbeat.index,
                            destination = heartbeat.address,
                            countLog = 0u,
                            periodLog = 0u,
                            ttl = heartbeat.ttl,
                            features = heartbeat.features
                        ),
                        target = target
                    )
                )
            }
        }
        // Let's not set the heartbeat subscription as the current subscription period is
        // unknown
        // Application key Bindings
        val elementCount = min(elements.size, target.elements.size)
        repeat(times = elementCount) { index ->
            val originalElement = elements[index]
            val targetElement = target.elements[index]

            originalElement
                .models
                .filter { it.supportsApplicationKeyBinding }
                .forEach { originalModel ->
                    targetElement
                        .model(modelId = originalModel.modelId)
                        ?.let { targetModel ->
                            meshNetwork.applicationKeys
                                .filter { it.isBoundTo(originalModel) }
                                .forEach { key ->
                                    messenger.enqueueTask(
                                        task = ConfigTask(
                                            icon = Icons.Outlined.AddLink,
                                            label = "Bind ${key.name} to ${targetModel.name}",
                                            message = ConfigModelAppBind(
                                                model = targetModel,
                                                applicationKey = key
                                            ),
                                            target = target
                                        )
                                    )
                                }
                        }
                }
        }
        // Model publications
        repeat(times = elementCount) { index ->
            val originalElement = this.elements[index]
            val targetElement = target.elements[index]
            originalElement
                .models
                .filter { it.supportsModelPublication }
                .forEach { originalModel ->
                    originalModel.publish?.let { publish ->
                        targetElement
                            .model(modelId = originalModel.modelId)
                            ?.let { targetModel ->
                                val newPublication = publish.copy(
                                    address = translate(
                                        address = publish.address,
                                        oldNode = this,
                                        newNode = target
                                    )
                                )
                                messenger.enqueueTask(
                                    task = ConfigTask(
                                        icon = Icons.Outlined.VpnKey,
                                        label = "Set Publications to 0x${
                                            publish.address.address.toHexString(HexFormat.UpperCase)
                                        }",
                                        message = if (publish.address is VirtualAddress) {
                                            ConfigModelPublicationVirtualAddressSet(
                                                publish = newPublication,
                                                model = targetModel
                                            )
                                        } else {
                                            ConfigModelPublicationSet(
                                                publish = newPublication,
                                                model = targetModel
                                            )
                                        },
                                        target = target
                                    )
                                )
                            }
                    }
                }
        }
        // Model subscriptions
        repeat(times = elementCount) { index ->
            val originalElement = this.elements[index]
            val targetElement = target.elements[index]
            originalElement
                .models
                .filter { it.supportsModelSubscription }
                .forEach { originalModel ->
                    targetElement
                        .model(modelId = originalModel.modelId)
                        ?.let { targetModel ->
                            originalModel
                                .subscribe
                                .filter { it !is AllNodes }
                                .forEach { address ->
                                    messenger.enqueueTask(
                                        task = ConfigTask(
                                            icon = Icons.Outlined.GroupWork,
                                            label = "Subscribe ${targetModel.name} to ${
                                                if (address is FixedGroupAddress) {
                                                    address.name()
                                                } else {
                                                    meshNetwork
                                                        .group(address = address.address)
                                                        ?.name
                                                        ?: address.address.toHexString(
                                                            format = HexFormat {
                                                                number.prefix = "0x"
                                                                upperCase = true
                                                            }
                                                        )
                                                }
                                            }",
                                            message = if (address is VirtualAddress) {
                                                ConfigModelSubscriptionVirtualAddressAdd(
                                                    virtualAddress = address,
                                                    model = targetModel
                                                )
                                            } else {
                                                ConfigModelSubscriptionAdd(
                                                    address = address,
                                                    model = targetModel
                                                )
                                            },
                                            target = target
                                        )
                                    )
                                }
                        }
                }
        }
        // Reconfigure other Nodes that may be publishing to the previous address of the Node.
        meshNetwork
            .nodes
            .filter { it.uuid != target.uuid }
            .flatMap { it.elements }
            .flatMap { it.models }
            .filter { model ->
                model.publish?.address?.let { publicationAddress ->
                    containsElementWithAddress(address = publicationAddress.address)
                } ?: false
            }.forEach { model ->
                model.publish?.let { publish ->
                    val newPublication = publish.copy(
                        address = translate(
                            address = publish.address,
                            oldNode = this,
                            newNode = target
                        )
                    )
                    messenger.enqueueTask(
                        task = ConfigTask(
                            icon = Icons.Outlined.VpnKey,
                            label = "Publish to ${publish.address}",
                            message = if (publish.address is VirtualAddress) {
                                ConfigModelPublicationVirtualAddressSet(
                                    publish = newPublication,
                                    model = model
                                )
                            } else {
                                ConfigModelPublicationSet(
                                    publish = newPublication,
                                    model = model
                                )
                            },
                            target = target
                        )
                    )
                }
            }
    }
}

/**
 * Translates the publication address to the new node.
 *
 * @param address    Address to be translated.
 * @param oldNode    Original Node.
 * @param newNode    New Node.
 *
 * @return New Publication Address.
 */
private fun translate(
    address: PublicationAddress,
    oldNode: Node,
    newNode: Node,
) = when (oldNode.containsElementWithAddress(address = address.address)) {
    true -> newNode.primaryUnicastAddress + address as UnicastAddress - oldNode.primaryUnicastAddress
    else -> address
}