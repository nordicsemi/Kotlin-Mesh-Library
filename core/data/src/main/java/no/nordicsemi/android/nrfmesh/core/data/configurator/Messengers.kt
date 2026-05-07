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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.name
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
class Messengers(
    private val meshNetworkManager: MeshNetworkManager,
    private val scope: CoroutineScope,
) {
    private var _messengers = mutableMapOf<Uuid, Messenger>()

    /**
     * Creates a new configurator for the given node.
     */
    fun createMessenger(nodeUuid: Uuid) =
        Messenger(meshNetworkManager = meshNetworkManager, scope = scope).also {
            _messengers[nodeUuid] = it
        }

    fun messenger(uuid: Uuid) = _messengers[uuid]

    @Suppress("unused")
    fun removeMessenger(nodeUuid: Uuid) {
        _messengers.remove(nodeUuid)
    }
}


/**
 * Configurator is responsible for configuring a node.
 *
 * @param meshNetworkManager MeshNetworkManager to be used.
 * @param originalNode       Original Node to be configured.
 */
@OptIn(ExperimentalUuidApi::class)
class Messenger(
    private val meshNetworkManager: MeshNetworkManager,
    private val scope: CoroutineScope,
) {

    private val _configTasksFlow = MutableStateFlow<List<ConfigTask>>(mutableListOf())
    private val _reconfigTasksFlow = MutableStateFlow<List<ConfigTask>>(mutableListOf())
    private val _appTasksFlow = MutableStateFlow<List<AppTask>>(mutableListOf())

    private var originalNode: Node? = null
    private var job: Job? = null

    @Suppress("unused")
    private val isCompleted: Boolean
        get() = _configTasksFlow.value.none {
            it.status !is TaskStatus.Completed
        } && _appTasksFlow.value.none {
            it.status !is TaskStatus.Completed
        }


    val meshTaskFlow: Flow<List<MeshTask>>
        get() = combine(
            flow = _configTasksFlow,
            flow2 = _reconfigTasksFlow,
            flow3 = _appTasksFlow
        ) { configTasks, reconfigTasks, appTasks ->
            configTasks + reconfigTasks + appTasks
        }

    /**
     * Enqueues the configuration tasks
     */
    fun enqueueTask(task: MeshTask) {
        when (task) {
            is ConfigTask ->
                _configTasksFlow.update {
                    (it + task).toMutableList()
                }

            is AppTask -> _appTasksFlow.update {
                (it + task).toMutableList()
            }
        }
    }

    /**
     * Enqueues the reconfiguration tasks based on the given original node
     */
    fun enqueueReconfigurationWith(originalNode: Node) {
        this.originalNode = originalNode
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun enqueueReconfiguration(meshNetwork: MeshNetwork, newNode: Node) {
        originalNode?.run {
            val reconfigTasks = mutableListOf<ConfigTask>()
            defaultTTL?.let {
                reconfigTasks.add(
                    element = ConfigTask(
                        icon = Icons.Outlined.Timer,
                        label = "Set Default TTL to $it on ${newNode.name}",
                        message = ConfigDefaultTtlSet(ttl = it)
                    )
                )
            }
            secureNetworkBeacon?.let {
                reconfigTasks.add(
                    element = ConfigTask(
                        icon = Icons.Outlined.WifiTethering,
                        label = "${if (it) "Enable" else "Disable"} Secure Network beacon on ${newNode.name}",
                        message = ConfigBeaconSet(enable = it)
                    )
                )
            }
            networkTransmit?.let {
                reconfigTasks.add(
                    element = ConfigTask(
                        icon = Icons.Outlined.NetworkPing,
                        label = "Set Network Transmit",
                        message = ConfigNetworkTransmitSet(networkTransmit = it)
                    )
                )
            }
            features.run { ->
                relay?.let { relay ->
                    when (relay.state) {
                        FeatureState.Enabled -> {
                            relayRetransmit?.let {
                                reconfigTasks.add(
                                    element = ConfigTask(
                                        icon = Icons.Outlined.NetworkPing,
                                        label = "Set Retransmit to ${it.interval} ms on ${newNode.name}",
                                        message = ConfigRelaySet(relayRetransmit = it)
                                    )
                                )
                            }
                        }

                        FeatureState.Disabled -> reconfigTasks.add(
                            element = ConfigTask(
                                icon = Icons.Outlined.NetworkCell,
                                label = "Disable Relay Feature on ${newNode.name}",
                                message = ConfigRelaySet()
                            )
                        )

                        FeatureState.Unsupported -> {
                            // Do nothing
                        }
                    }
                }

                proxy?.let { proxy ->
                    if (proxy.state is FeatureState.Unsupported) {
                        reconfigTasks.add(
                            element = ConfigTask(
                                icon = Icons.Outlined.Hub,
                                label = "${if (proxy.state is FeatureState.Enabled) "Enabling" else "Disabling"} GATT Proxy on ${newNode.name}",
                                message = ConfigGattProxySet(enable = proxy.state is FeatureState.Enabled)
                            )
                        )
                    }
                }

                friend?.let { friend ->
                    if (friend.state != FeatureState.Unsupported) {
                        reconfigTasks.add(
                            element = ConfigTask(
                                icon = Icons.Outlined.Diversity1,
                                label = "${if (friend.state is FeatureState.Enabled) "Enabling" else "Disabling"} Friend Feature on ${newNode.name}",
                                message = ConfigFriendSet(enable = friend.state is FeatureState.Enabled)
                            )
                        )
                    }
                }
            }
            meshNetwork.networkKeys
                .knownTo(node = this)
                // During provisioning a network key is already added let's filter it out.
                .filter { !newNode.knows(key = it) }
                .forEach {
                    reconfigTasks.add(
                        element = ConfigTask(
                            icon = Icons.Outlined.VpnKey,
                            label = "Add ${it.name} to ${newNode.name}",
                            message = ConfigNetKeyAdd(key = it)
                        )
                    )
                }
            meshNetwork.applicationKeys
                .knownTo(this)
                .forEach {
                    reconfigTasks.add(
                        element = ConfigTask(
                            icon = Icons.Outlined.VpnKey,
                            label = "Add ${it.name} to ${newNode.name}",
                            message = ConfigAppKeyAdd(key = it)
                        )
                    )
                }
            heartbeatPublication?.let { heartbeat ->
                meshNetwork.networkKey(heartbeat.index)?.let {
                    reconfigTasks.add(
                        element = ConfigTask(
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
                            )
                        )
                    )
                }
            }
            // Let's not set the heartbeat subscription as the current subscription period is
            // unknown
            // Application key Bindings
            val elementCount = min(elements.size, newNode.elements.size)
            repeat(times = elementCount) { index ->
                val originalElement = elements[index]
                val targetElement = newNode.elements[index]

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
                                        reconfigTasks.add(
                                            element = ConfigTask(
                                                icon = Icons.Outlined.AddLink,
                                                label = "Bind ${key.name} to ${targetModel.name}",
                                                message = ConfigModelAppBind(
                                                    model = targetModel,
                                                    applicationKey = key
                                                )
                                            )
                                        )
                                    }
                            }
                    }
            }
            // Model publications
            repeat(times = elementCount) { index ->
                val originalElement = this.elements[index]
                val targetElement = newNode.elements[index]
                originalElement
                    .models
                    .filter { it.supportsModelPublication == true }
                    .forEach { originalModel ->
                        originalModel.publish?.let { publish ->
                            targetElement
                                .model(modelId = originalModel.modelId)
                                ?.let { targetModel ->
                                    val newPublication = publish.copy(
                                        address = translate(
                                            address = publish.address,
                                            oldNode = this,
                                            newNode = newNode
                                        )
                                    )
                                    reconfigTasks.add(
                                        element = ConfigTask(
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
                                            }
                                        )
                                    )
                                }
                        }
                    }
            }
            // Model subscriptions
            repeat(times = elementCount) { index ->
                val originalElement = this.elements[index]
                val targetElement = newNode.elements[index]
                originalElement
                    .models
                    .filter { it.supportsModelSubscription == true }
                    .forEach { originalModel ->
                        targetElement
                            .model(modelId = originalModel.modelId)
                            ?.let { targetModel ->
                                originalModel
                                    .subscribe
                                    .filter { it !is AllNodes }
                                    .forEach { address ->
                                        reconfigTasks.add(
                                            element = ConfigTask(
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
                                                icon = Icons.Outlined.GroupWork,
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
                                                }
                                            )
                                        )
                                    }
                            }
                    }
            }
            // Reconfigure other Nodes that may be publishing to the previous address of the Node.
            meshNetwork
                .nodes
                .filter { it.uuid != newNode.uuid }
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
                                newNode = newNode
                            )
                        )
                        reconfigTasks.add(
                            element = ConfigTask(
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
                                }
                            )
                        )
                    }
                }
            _reconfigTasksFlow.update {
                (it + reconfigTasks).toMutableList()
            }
        }
    }

    /**
     * Translates the publication address to the new node.
     *
     * @param address    Address to be translated.
     * @param oldNode    Original Node.
     * @param newNode    New Node.
     */
    private fun translate(
        address: PublicationAddress,
        oldNode: Node,
        newNode: Node,
    ): PublicationAddress {
        if (oldNode.containsElementWithAddress(address = address.address)) {
            return newNode.primaryUnicastAddress + address as UnicastAddress - oldNode.primaryUnicastAddress
        }
        return address
    }

    /**
     * Executes the enqueued tasks
     *
     * @param meshNetwork MeshNetwork to be configured.
     * @param newNode     Node to be configured.
     */
    fun execute(meshNetwork: MeshNetwork, newNode: Node) {
        job = scope.launch {
            executeConfigurationTasks(newNode = newNode)
            enqueueReconfiguration(meshNetwork = meshNetwork, newNode = newNode)
            executeReconfigurationTasks(newNode = newNode)
            executeAppTasks()
        }
    }

    fun retry(node: Node){
        job?.cancel()
        job = scope.launch {
            executeReconfigurationTasks(newNode = node)
            executeAppTasks()
        }
    }

    fun clear() {
        job?.cancel()
        _configTasksFlow.value = emptyList()
        _reconfigTasksFlow.value = emptyList()
        _appTasksFlow.value = emptyList()
    }

    private suspend fun executeConfigurationTasks(newNode: Node) {
        _configTasksFlow.value.forEachIndexed { index, task ->
            var tempTask = task
            _configTasksFlow.update {
                tempTask = tempTask.copy(status = TaskStatus.InProgress)
                it.toMutableList()
                    .apply { this[index] = tempTask }
            }
            try {
                meshNetworkManager.send(
                    message = task.message,
                    node = newNode,
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
            } catch (e: Exception) {
                tempTask =
                    tempTask.copy(status = TaskStatus.Error(error = e.message ?: "Unknown Error"))
                _configTasksFlow.update {
                    it.toMutableList()
                        .apply { this[index] = tempTask }
                }
                return@forEachIndexed
            }
            _configTasksFlow.update {
                it.toMutableList()
                    .apply { this[index] = tempTask }
            }
        }
    }

    private suspend fun executeReconfigurationTasks(newNode: Node) {
        for ((index, task) in _reconfigTasksFlow.value.withIndex()) {
            if (task.status is TaskStatus.Completed)
                continue
            var tempTask = task
            _reconfigTasksFlow.update {
                tempTask = tempTask.copy(status = TaskStatus.InProgress)
                it.toMutableList()
                    .apply { this[index] = tempTask }
            }
            try {
                meshNetworkManager.send(
                    message = task.message,
                    node = newNode,
                    initialTtl = null
                )?.let {
                    tempTask = when ((it as? ConfigStatusMessage) != null) {
                        true -> if(it.isSuccess) {
                            tempTask.copy(status = TaskStatus.Completed)
                        }
                        else {
                            tempTask.copy(status = TaskStatus.Error(error = it.message))
                        }
                        else -> tempTask.copy(status = TaskStatus.Completed)
                    }
                } ?: run { tempTask = tempTask.copy(status = TaskStatus.Skipped) }
            } catch (e: Exception) {
                tempTask = tempTask.copy(
                    status = TaskStatus.Error(
                        error = e.message ?: "Unknown Error"
                    )
                )
                _reconfigTasksFlow.update {
                    it.toMutableList()
                        .apply { this[index] = tempTask }
                }
                break
            }
            _reconfigTasksFlow.update {
                it.toMutableList()
                    .apply { this[index] = tempTask }
            }
        }
    }

    private suspend fun executeAppTasks() {
        _appTasksFlow.value.forEachIndexed { index, task ->
            var tempTask = task
            _appTasksFlow.update {
                tempTask = task.copy(status = TaskStatus.InProgress)
                it.toMutableList()
                    .apply { this[index] = tempTask }
            }
            try {
                if (task.message is AcknowledgedMeshMessage) {
                    meshNetworkManager.send(
                        message = task.message,
                        model = task.model,
                        localElement = task.element,
                        applicationKey = task.applicationKey,
                        initialTtl = null
                    )?.let {
                        tempTask = tempTask.copy(status = TaskStatus.Completed)
                    } ?: run {
                        tempTask =
                            tempTask.copy(status = TaskStatus.Error(error = "Response not received!"))
                    }
                } else {
                    meshNetworkManager.send(
                        message = task.message as UnacknowledgedMeshMessage,
                        model = task.model,
                        localElement = task.element,
                        applicationKey = task.applicationKey,
                        initialTtl = null
                    )
                    tempTask = tempTask.copy(status = TaskStatus.Completed)
                }
            } catch (e: Exception) {
                tempTask =
                    tempTask.copy(status = TaskStatus.Error(error = e.message ?: "Unknown Error"))
                _appTasksFlow.update {
                    it.toMutableList()
                        .apply { this[index] = tempTask }
                }
                return@forEachIndexed
            }
            _appTasksFlow.update {
                it.toMutableList()
                    .apply { this[index] = tempTask }
            }
        }
    }
}