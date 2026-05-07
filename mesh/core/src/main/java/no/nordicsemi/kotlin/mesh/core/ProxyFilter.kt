@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package no.nordicsemi.kotlin.mesh.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nordicsemi.kotlin.mesh.bearer.BearerError
import no.nordicsemi.kotlin.mesh.core.messages.proxy.AddAddressesToFilter
import no.nordicsemi.kotlin.mesh.core.messages.proxy.FilterStatus
import no.nordicsemi.kotlin.mesh.core.messages.proxy.ProxyConfigurationMessage
import no.nordicsemi.kotlin.mesh.core.messages.proxy.RemoveAddressesFromFilter
import no.nordicsemi.kotlin.mesh.core.messages.proxy.SetFilterType
import no.nordicsemi.kotlin.mesh.core.model.AllNodes
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.Provisioner
import no.nordicsemi.kotlin.mesh.core.model.ProxyFilterAddress
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.logger.LogCategory
import no.nordicsemi.kotlin.mesh.logger.Logger

/**
 * Enum class that defines Proxy filter types.
 *
 * @property type Filter type.
 */
enum class ProxyFilterType(val type: UByte) {

    /**
     * An accept list filter has an associated inclusion list containing destination addresses that
     * are of interest for the Proxy Client.
     *
     * The accept list filter blocks all messages except those targeting addresses added to the
     * list.
     */
    ACCEPT_LIST(0x00u),

    /**
     * A reject list filter has an associated exclusion list containing destination addresses that
     * are NOT of the Proxy Client interest.
     *
     * The reject list filter forwards all messages except those targeting addresses added to the
     * list.
     */
    REJECT_LIST(0x01u);

    companion object {

        /**
         * Returns the [ProxyFilterType] from the given filter type.
         *
         * @param filterType Filter type.
         * @return [ProxyFilterType] or if invalid throws an [IllegalArgumentException] exception.
         * @throws IllegalArgumentException if the filter type is invalid.
         */
        @Throws(IllegalArgumentException::class)
        fun from(filterType: UByte) = entries.find {
            it.type == filterType
        } ?: throw IllegalArgumentException("Illegal filter type")
    }

    override fun toString() = when (this) {
        ACCEPT_LIST -> "Accept List"
        REJECT_LIST -> "Reject List"
    }
}

/**
 * Proxy filter state defines the state of the proxy filter.
 */
sealed class ProxyFilterState {

    data object Unknown : ProxyFilterState()

    /**
     * State defining when the Proxy filter has been sent to the proxy.
     *
     * @property type      Filter type.
     * @property addresses List of addresses.
     */
    @ConsistentCopyVisibility
    data class ProxyFilterUpdated internal constructor(
        val type: ProxyFilterType,
        val addresses: List<ProxyFilterAddress>,
    ) : ProxyFilterState()

    /**
     * State defining when the Proxy filter has been acknowledged by the proxy.
     *
     * @property type     Filter type.
     * @property listSize List of addresses.
     */
    @ConsistentCopyVisibility
    data class ProxyFilterUpdateAcknowledged internal constructor(
        val type: ProxyFilterType,
        val listSize: UShort,
    ) : ProxyFilterState()

    /**
     * State defining when the Proxy filter limit has been reached.
     *
     * @property type     Filter type.
     * @property listSize List of addresses.
     */
    @ConsistentCopyVisibility
    data class ProxyFilterLimitReached internal constructor(
        val type: ProxyFilterType,
        val listSize: UShort,
    ) : ProxyFilterState() {
        override fun toString() = "Proxy Filter Limit Reached(filter : $type, listSize: $listSize)"
    }
}

/**
 * An enumeration defining possible initial configuration states of the proxy filter.
 */
enum class ProxyFilterSetup {

    /**
     * This setup will set to [ProxyFilterType.ACCEPT_LIST] with [UnicastAddress]es of all local
     * elements, all [GroupAddress]es with at least one local model subscribed to the [AllNodes]
     * address.
     */
    AUTOMATIC,

    /**
     * The Proxy Filter on each connected Proxy Node will be set to [ProxyFilterType.ACCEPT_LIST]
     * with the given set of addresses.
     */
    ACCEPT_LIST,

    /**
     * The Proxy Filter on each connected Proxy Node will be set to [ProxyFilterType.REJECT_LIST]
     * with the given set of addresses.
     */
    REJECT_LIST
}

internal sealed interface ProxyFilterEventHandler {

    /**
     * Clears the current proxy filter state.
     */
    suspend fun onNewNetworkCreated()

    /**
     * Invoked when a possible change of Proxy Node have been discovered.
     *
     * This method is called in two cases: when the first Secure Network beacon was received (which
     * indicates the first successful connection to a Proxy since app was started) or when the
     * received Secure Network beacon contained information about the same Network Key as one
     * received before. This happens during a reconnection to the same or a different Proxy on the
     * same subnetwork, but may also happen in other Circumstances, for example when the IV Update
     * or Key Refresh Procedure is in progress, or a Network Key was removed and added again.
     *
     * This method reloads the Proxy Filter for the local Provisioner, adding all the addresses the
     * Provisioner is subscribed to, including its Unicast Addresses and All Nodes address.
     */
    suspend fun onNewProxyConnected()

    /**
     * Invoked when a Proxy Configuration Message has been sent. This method refreshes the local
     * type and list of addresses.
     *
     * @param message The message sent.
     */
    suspend fun onManagerDidDeliverMessage(message: ProxyConfigurationMessage)

    /**
     * Invoked when the manager failed to send the Proxy Configuration Message.
     *
     * This method clears the local filter and sets it back to [ProxyFilterType.ACCEPT_LIST].
     * All the messages waiting to be sent are cancelled.
     *
     * @param message Message that has not been sent.
     * @param error Error received.
     */
    suspend fun onManagerFailedToDeliverMessage(
        message: ProxyConfigurationMessage,
        error: Throwable,
    )

    /**
     * Handler for the received Proxy Configuration Messages.
     *
     * This method notifies the delegate about changes in the Proxy Filter.
     *
     * If a mismatch is detected between the local list of services and the list size received, the
     * method will try to clear the remote filter and send all the addresses again.
     *
     * @param message Message received.
     * @param proxy   Connected Proxy Node, or `null` if the Node is unknown.
     */
    suspend fun handle(message: ProxyConfigurationMessage, proxy: Node?)
}

/**
 * Proxy filter allows modifying the proxy filter of the connected Proxy Node.
 *
 * Initially, upon connection to a Proxy Node, the manager will automatically subscribe to the
 * [UnicastAddress]es of all local Elements and all [GroupAddress]es with at least one local Model
 * is subscribed to, including [AllNodes] address.
 *
 * Node: When a local Model gets subscribed to a new Group, or is unsubscribed from a Group that no
 *       other local Model is subscribed to, the proxy filter needs to be modified manually by
 *       calling proper add/remove methods.
 *
 *
 * @property scope Coroutine scope
 * @property mutex Mutex for internal synchronization
 * @property manager Mesh network manager
 */
class ProxyFilter internal constructor(
    val scope: CoroutineScope,
    val manager: MeshNetworkManager,
) : ProxyFilterEventHandler {

    private val _proxyFilterStateFlow =
        MutableStateFlow<ProxyFilterState>(value = ProxyFilterState.Unknown)
    val proxyFilterStateFlow = _proxyFilterStateFlow.asStateFlow()

    // A mutex for internal synchronization.
    private val mutex = Mutex()

    // The flag is set to 'true' when a request has been sent to the connected proxy. It is cleared
    // when a response was received, or in case of an error.
    private var busy = false

    // The last Proxy Configuration message that was sent
    private var request: ProxyConfigurationMessage? = null

    private val logger: Logger?
        get() = manager.logger

    var initializeState: ProxyFilterSetup = ProxyFilterSetup.AUTOMATIC

    private var _addresses = mutableListOf<ProxyFilterAddress>()
    val addresses: List<ProxyFilterAddress>
        get() = _addresses

    var type: ProxyFilterType = ProxyFilterType.ACCEPT_LIST
        private set
    var proxy: Node? = null
        private set

    /**
     * Sets the Filter Type on the connected GATT Proxy node. The filter will be emptied.
     *
     * @param type Filter type.
     */
    suspend fun setType(type: ProxyFilterType) {
        send(message = SetFilterType(filterType = type))
    }

    /**
     * Resets the filter to an empty accept list filter.
     */
    suspend fun reset() {
        send(message = SetFilterType(filterType = ProxyFilterType.ACCEPT_LIST))
    }

    /**
     * Clears the current filter
     */
    suspend fun clear() {
        send(message = SetFilterType(filterType = type))
    }

    /**
     * Adds the given address to the active filter.
     *
     * @param address Address to be added to the filter.
     */
    suspend fun add(address: ProxyFilterAddress) {
        send(message = AddAddressesToFilter(addresses = listOf(element = address)))
    }

    /**
     * Adds the given addresses to the active filter.
     *
     * Proxy message must fit in a single Network PDU, therefore may contain maximum of 5 addresses.
     *
     * @param addresses List of addresses to be added to the filter.
     */
    suspend fun add(addresses: List<ProxyFilterAddress>) {
        addresses.chunked(5).forEach { chunk ->
            send(message = AddAddressesToFilter(addresses = chunk))
        }
    }

    /**
     * Adds the given group to the active filter.
     *
     * @param group Group to be added to the filter.
     */
    suspend fun add(group: Group) {
        addGroups(groups = listOf(element = group))
    }

    /**
     * Adds the given list of groups to the active filter.
     *
     * @param groups Group to be added to the filter.
     */
    suspend fun addGroups(groups: List<Group>) {
        add(addresses = groups.map { it.address as ProxyFilterAddress })
    }

    /**
     * Removes the given address from the active filter.
     *
     * @param address Address to be removed from the filter.
     */
    suspend fun remove(address: ProxyFilterAddress) {
        send(message = RemoveAddressesFromFilter(addresses = listOf(element = address)))
    }

    /**
     * Removes the given list of given addresses from the active filter.
     *
     * @param addresses List of addresses to be removed from the filter.
     */
    suspend fun remove(addresses: List<ProxyFilterAddress>) {
        addresses.chunked(5).forEach { chunk ->
            send(message = RemoveAddressesFromFilter(addresses = chunk))
        }
    }

    /**
     * Adds all the addresses the provisioner is subscribed to the proxy filter.
     *
     * @param provisioner Provisioner to be added to the filter.
     */
    suspend fun setup(provisioner: Provisioner) = provisioner.node?.run {
        // Reset the proxy filter to an empty accept list filter.
        setType(type = ProxyFilterType.ACCEPT_LIST)
        val addresses = mutableListOf<ProxyFilterAddress>()
        // Add all the unicast addresses of all elements of the provisioner's Node
        addresses.addAll(elements.map { it.unicastAddress })
        // Add all the addresses that the Node's Models are subscribed to
        addresses.addAll(
            elements
                .flatMap { it.models }
                .flatMap { it.subscribe }
                .filter { it is ProxyFilterAddress }
                .map { it as ProxyFilterAddress }
        )
        // Add the All Nodes address
        addresses.add(AllNodes)
        // Add all the above addresses to the filter.
        add(addresses = addresses.distinct())
    }

    suspend fun proxyDidDisconnect() {
        onNewNetworkCreated()
        // Clear the Proxy Network Key. This way we make sure the Network Layer will handle the new
        // incoming Secure Network beacon properly, even if it belongs to a non-primary network.
        manager.networkManager?.networkLayer?.proxyNetworkKey = null
        mutex.withLock {
            scope.launch {
                _proxyFilterStateFlow.emit(
                    value = ProxyFilterState.ProxyFilterUpdated(
                        type = ProxyFilterType.ACCEPT_LIST,
                        addresses = listOf()
                    )
                )
            }
        }
    }

    /**
     * Sends the given message to the Proxy Server. If a previous message is still waiting for
     * status, this will buffer the message and send it after the status is received.
     *
     * @param message Message to be sent.
     */
    private suspend fun send(message: ProxyConfigurationMessage) = manager.send(message = message)

    override suspend fun onNewNetworkCreated() {
        mutex.withLock {
            type = ProxyFilterType.ACCEPT_LIST
            _addresses.clear()
            busy = false
            proxy = null
            request = null
        }
    }

    override suspend fun onNewProxyConnected() {
        scope.launch {
            onNewNetworkCreated()
            logger?.i(LogCategory.PROXY) { "New Proxy connected" }
            manager.network?.localProvisioner?.let { provisioner ->
                when (initializeState) {
                    ProxyFilterSetup.AUTOMATIC -> setup(provisioner = provisioner)
                    ProxyFilterSetup.ACCEPT_LIST -> {
                        setType(type = ProxyFilterType.ACCEPT_LIST)
                        add(addresses = addresses)
                    }
                    ProxyFilterSetup.REJECT_LIST -> {
                        setType(type = ProxyFilterType.REJECT_LIST)
                        add(addresses = addresses)
                    }
                }
            }
        }
    }

    override suspend fun onManagerDidDeliverMessage(message: ProxyConfigurationMessage) {
        mutex.withLock { request = message }
    }

    override suspend fun onManagerFailedToDeliverMessage(
        message: ProxyConfigurationMessage,
        error: Throwable,
    ) {
        mutex.withLock { busy = false }

        if (error is BearerError.Closed) proxyDidDisconnect()
    }

    override suspend fun handle(message: ProxyConfigurationMessage, proxy: Node?) {
        when (message) {
            is FilterStatus -> {
                var expectedListSize = addresses.size
                mutex.withLock {
                    this.proxy = proxy
                    request?.let { request ->
                        when (request) {
                            is AddAddressesToFilter -> {
                                // Addresses were sent in ascending order (primary unicast address
                                // first). On every device there's an upper limit of the size of
                                // Proxy Filter List. Assuming that devices are added in the order
                                // they were sent (as they should), we must remove the addresses at
                                // the end.
                                expectedListSize = addresses.size + request.addresses.size
                                val addedAddresses = request.addresses
                                    .sortedBy { it.address }
                                    .take(message.listSize.toInt() - addresses.size)
                                _addresses = addedAddresses.union(_addresses).toMutableList()
                            }

                            is RemoveAddressesFromFilter -> {
                                _addresses.removeAll(request.addresses)
                                //_addresses = request.addresses.subtract(_addresses).toMutableList()
                                expectedListSize = addresses.size
                            }

                            is SetFilterType -> {
                                type = request.filterType
                                _addresses.clear()
                                expectedListSize = 0
                            }

                            else -> { /* Ignore */
                            }
                        }
                        this.request = null
                    }
                    // Notify the app about the current state
                    _proxyFilterStateFlow.value = ProxyFilterState.ProxyFilterUpdated(
                        type = type,
                        addresses = addresses
                    )
                }

                // Ensure the current information about the filter is up to date.
                if (type != message.filterType || expectedListSize != message.listSize.toInt()) {
                    logger?.w(LogCategory.PROXY) {
                        "Proxy Filter limit reached: ${message.listSize} " +
                                "(expected: $expectedListSize)"
                    }
                    _proxyFilterStateFlow.value = ProxyFilterState.ProxyFilterLimitReached(
                        type = message.filterType,
                        listSize = message.listSize
                    )
                } else {
                    _proxyFilterStateFlow.value = ProxyFilterState.ProxyFilterUpdateAcknowledged(
                        type = message.filterType,
                        listSize = message.listSize
                    )
                }
            }

            else -> { /* Ignore */
            }
        }
    }
}