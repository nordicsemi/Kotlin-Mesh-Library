package no.nordicsemi.kotlin.mesh.core

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nordicsemi.kotlin.mesh.bearer.MeshBearer
import no.nordicsemi.kotlin.mesh.bearer.Transmitter
import no.nordicsemi.kotlin.mesh.core.exception.ImportError
import no.nordicsemi.kotlin.mesh.core.exception.InvalidKeyLength
import no.nordicsemi.kotlin.mesh.core.exception.NoNetwork
import no.nordicsemi.kotlin.mesh.core.layers.NetworkManager
import no.nordicsemi.kotlin.mesh.core.layers.NetworkManagerEvent
import no.nordicsemi.kotlin.mesh.core.layers.NetworkParameters
import no.nordicsemi.kotlin.mesh.core.layers.access.CannotDelete
import no.nordicsemi.kotlin.mesh.core.layers.access.CannotRelay
import no.nordicsemi.kotlin.mesh.core.layers.access.InvalidDestination
import no.nordicsemi.kotlin.mesh.core.layers.access.InvalidElement
import no.nordicsemi.kotlin.mesh.core.layers.access.InvalidKey
import no.nordicsemi.kotlin.mesh.core.layers.access.InvalidSource
import no.nordicsemi.kotlin.mesh.core.layers.access.InvalidTtl
import no.nordicsemi.kotlin.mesh.core.layers.access.ModelNotBoundToAppKey
import no.nordicsemi.kotlin.mesh.core.layers.access.NoAppKeysBoundToModel
import no.nordicsemi.kotlin.mesh.core.layers.lowertransport.AccessMessage
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.BaseMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetKeyDelete
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionTimer
import no.nordicsemi.kotlin.mesh.core.messages.proxy.ProxyConfigurationMessage
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.Element
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.Location
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.Provisioner
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.serialization.MeshNetworkSerializer.deserialize
import no.nordicsemi.kotlin.mesh.core.model.serialization.MeshNetworkSerializer.serialize
import no.nordicsemi.kotlin.mesh.core.model.serialization.config.NetworkConfiguration
import no.nordicsemi.kotlin.mesh.logger.LogCategory
import no.nordicsemi.kotlin.mesh.logger.Logger
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * MeshNetworkManager is the entry point to the Mesh library.
 *
 * @param storage                     Custom storage option allowing users to save the mesh network
 *                                    to a custom location.
 * @param secureProperties            Custom storage option allowing users to save the sequence
 *                                    number.
 * @param scope                       The scope in which the mesh network will be created.
 * @property meshNetwork              The state flow with the active mesh network.
 * @property meshBearer               Mesh bearer is responsible for sending and receiving mesh
 *                                    messages.
 * @property logger                   The logger is responsible for logging mesh messages.
 * @property networkManager           Handles the mesh networking stack.
 * @property proxyFilter              Proxy filter is responsible for filtering messages sent to the
 *                                    proxy node.
 * @property localElements            List of local elements in the mesh network. The primary
 *                                    element is always the first element in the list.
 * @property incomingMeshMessages     Flow containing incoming access messages received by the local
 *                                    node.
 * @property attentionTimer           Flow that notifies the client app about the health attention
 *                                    timer start and stop events.
 */
class MeshNetworkManager(
    private val storage: Storage,
    internal val secureProperties: SecurePropertiesStorage,
    internal val ioDispatcher: CoroutineDispatcher,
) : Publisher {
    internal val scope = CoroutineScope(context = SupervisorJob() + ioDispatcher)
    private val mutex by lazy { Mutex() }
    var networkParameters = NetworkParameters()

    /** The currently loaded mesh network. */
    internal var network: MeshNetwork? = null
    private val _meshNetwork = MutableStateFlow<MeshNetwork?>(null)
    val meshNetwork = _meshNetwork.asStateFlow()

    internal var observeNetworkManagerEvents: Job? = null
    internal var observeMeshMessages: Job? = null

    internal var networkManager: NetworkManager? = null
        private set(value) {
            field = value
            value?.let {
                observeNetworkManagerEvents()
                observeMeshMessages()
            }
        }

    private val _incomingMeshMessages = MutableSharedFlow<BaseMeshMessage>()
    val incomingMeshMessages: SharedFlow<BaseMeshMessage>
        get() = _incomingMeshMessages.asSharedFlow()

    var logger: Logger? = null

    var meshBearer: MeshBearer? = null
        set(value) {
            field = value
            networkManager?.bearer = value
        }
    val proxyFilter: ProxyFilter = ProxyFilter(scope = scope, manager = this)

    private val _attentionTimer = MutableSharedFlow<HealthAttentionTimer>()
    val attentionTimer = _attentionTimer.asSharedFlow()

    var localElements: List<Element>
        get() = network?.localElements ?: emptyList()
        set(value) {
            val network = requireNotNull(network) {
                logger?.e(category = LogCategory.MODEL) {
                    "Error: Mesh Network must be created or imported before setting up local " +
                            "elements."
                }
                throw NoNetwork()
            }
            // Some models, which are supported by the library, will be added automatically.
            // Let's make sure they are not in the array.
            var elements = value.onEach {
                it.removePrimaryElementModels()
            }
            // Remove all empty elements
            elements = elements.filter { it.models.isNotEmpty() }

            // Add the required Models in the Primary Element.
            if (elements.isEmpty()) elements = elements + Element(location = Location.UNKNOWN)

            elements
                .first()
                .addPrimaryElementModels(
                    triggerAttentionTimer = { _attentionTimer.tryEmit(value = it) },
                    logger = logger
                )

            elements.forEach { element ->
                element.models.forEach { model ->
                    model.eventHandler?.let {
                        // Set the mesh network for all [ModelEventHandler]
                        it.meshNetwork = network
                        // Set the model for all [ModelEventHandler]
                        it.model = model
                        // Set the publisher for all [ModelEventHandler]
                        it.publisher = this
                    }
                }
            }

            network._localElements = elements.toMutableList()
            networkManager?.accessLayer?.reinitializePublishers()
        }

    /**
     * Loads the network from the storage provided by the user.
     *
     * @return true if the configuration was successfully loaded or false otherwise.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun load() = storage
        .load()
        .takeIf { it.isNotEmpty() }
        ?.let {
            network = deserialize(it)
                // Load the IvIndex from the secure properties' storage.
                .apply { ivIndex = secureProperties.ivIndex(uuid = uuid) }
            networkManager = NetworkManager(manager = this)
            proxyFilter.onNewNetworkCreated()
            _meshNetwork.update { network }
            true
        } == true

    /**
     * Saves the network in the local storage provided by the user.
     */
    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    suspend fun save() {
        println("AAA Saving network")
        export()?.let {
            mutex.withLock { storage.save(network = it) }
        }
        _meshNetwork.update {
            network?.copy(id = network!!.id.inc())
        }
    }

    /**
     * Creates a Mesh Network with a given name and a UUID. If a UUID is not provided a random will
     * be generated.
     *
     * @param name Name of the mesh network.
     * @param uuid 128-bit Universally Unique Identifier (Uuid), which allows differentiation among
     *             multiple mesh networks.
     * @throws InvalidKeyLength if the key length is invalid.
     */
    @OptIn(ExperimentalUuidApi::class)
    @Throws(InvalidKeyLength::class)
    suspend fun create(
        name: String = "Mesh Network",
        uuid: Uuid = Uuid.random(),
        provisionerName: String = "Mesh Provisioner",
        networkKeys: List<ByteArray> = emptyList(),
    ) = create(
        name = name,
        uuid = uuid,
        provisioner = Provisioner(name = provisionerName),
        networkKeys = networkKeys
    )

    /**
     * Creates a Mesh Network with a given name and a UUID. If a UUID is not provided a random will
     * be generated.
     *
     * @param name Name of the mesh network.
     * @param uuid 128-bit UUID of the mesh network.
     * @param provisioner Provisioner to be added to the network.
     * @throws InvalidKeyLength if the key length is invalid.
     */
    @OptIn(ExperimentalTime::class, ExperimentalUuidApi::class)
    @Throws(InvalidKeyLength::class)
    suspend fun create(
        name: String = "Mesh Network",
        uuid: Uuid = Uuid.random(),
        provisioner: Provisioner,
        networkKeys: List<ByteArray> = emptyList(),
    ): MeshNetwork {
        // Check if the Network Key is of valid length
        networkKeys.forEach {
            if (it.size != 16) {
                logger?.e(category = LogCategory.FOUNDATION_MODEL) {
                    "Key length must be 16 bytes"
                }
                throw InvalidKeyLength()
            }
        }

        return MeshNetwork(uuid = uuid, _name = name).also { network ->
            if (networkKeys.isNotEmpty()) {
                networkKeys.forEachIndexed { index, key ->
                    network.add(
                        name = if (index == 0) "Primary Network Key" else "Network Key $index",
                        key = key
                    )
                }
            } else {
                network.add(name = "Primary Network Key", index = 0u)
            }
            network.add(provisioner)
            this.network = network
            networkManager = NetworkManager(this)
            // Store the IvIndex of the newly created network.
            secureProperties.storeLocalProvisioner(
                uuid = uuid,
                localProvisionerUuid = network.provisioners.first().uuid
            )
            secureProperties.storeIvIndex(
                uuid = network.uuid,
                ivIndex = network.ivIndex
            )
            _meshNetwork.update { network }
        }
    }

    /**
     * Forgets the currently loaded mesh network and saves the state.
     */
    suspend fun clear() {
        networkManager = null
        network = null
        _meshNetwork.update { null }
        mutex.withLock { storage.save(network = byteArrayOf()) }
    }

    /**
     * Imports a Mesh Network from a byte array containing a JSON defined by the Mesh Configuration
     * Database profile.
     *
     * @return a mesh network configuration decoded from the given byte array.
     * @throws ImportError if deserializing fails.
     */
    @Throws(ImportError::class)
    suspend fun import(array: ByteArray) = runCatching {
        deserialize(array)
            .also { network ->
                this.network = network
                networkManager = NetworkManager(this)
                proxyFilter.onNewNetworkCreated()
                _meshNetwork.update { network }
            }
    }.getOrElse {
        if (it is ImportError) throw it
        else throw ImportError(error = "Error while deserializing the mesh network", throwable = it)
    }

    /**
     * Exports a mesh network to a JSON defined by the Mesh Configuration Database Profile based
     * on the given configuration.
     *
     * @param configuration Specifies if the network should be fully exported or partially.
     * @return Bytearray containing the Mesh network configuration.
     */
    fun export(configuration: NetworkConfiguration = NetworkConfiguration.Full) = network
        ?.let { serialize(network = it, configuration = configuration) }
        ?.toString()
        ?.toByteArray()

    override fun publish(message: UnacknowledgedMeshMessage, model: Model) {
        networkManager?.let {
            model.let {
                requireNotNull(it.parentElement) {
                    logger?.e(category = LogCategory.MODEL) {
                        "Model does not belong to an Element"
                    }
                    throw IllegalStateException("Model does not belong to an Element")
                }
                it.publish?.let { publish ->
                    network?.applicationKey(index = publish.index)?.let {
                        networkManager?.let { networkManager ->
                            scope.launch {
                                networkManager.publish(message = message, from = model)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * This method checks whether the proxy node knows the given Network Key.
     *
     * @param key The Network Key to check.
     */
    internal fun ensureNetworkKeyExists(key: NetworkKey) {
        proxyFilter.proxy?.let { node ->
            if (!node.knows(key = key)) {
                logger?.w(category = LogCategory.PROXY) {
                    "${node.name} cannot relay messages using ${key.name}, messages will be sent " +
                            "only to the local Node."
                }
            }
        } ?: run {
            logger?.w(category = LogCategory.PROXY) {
                "No GATT Proxy connected, message will be sent only to the local Node."
            }
        }
    }

    /**
     * Encrypts the message with the Application Key and the Network Key bound to it, and sends to
     * the given destination address.
     *
     * The method completes when the message has been sent or an error occurred.
     *
     * @param message              Message to be sent.
     * @param localElement         Source Element. If `nil`, the primary Element will be used. The
     *                             Element must belong to the local Provisioner's Node.
     * @param destination          Destination address.
     * @param initialTtl           Initial TTL (Time To Live) value of the message. If `null`, the
     *                             default Node TTL will be used.
     * @param applicationKey       Application Key to sign the message.
     * @throws NoNetwork if the mesh network has not been created.
     * @throws InvalidSource if the Local Provisioner has no Unicast Address assigned.
     * @throws InvalidDestination if the Address is not a Unicast Address, an Unknown destination
     *                            Node, the Node does not have a Network Key,the Node's device key
     *                            is unknown or Cannot remove last Network Key.
     * @throws InvalidElement if the element does not belong to the local node.
     * @throws InvalidTtl if the TTL value is invalid.
     */
    @Throws(
        NoNetwork::class,
        InvalidDestination::class,
        InvalidSource::class,
        InvalidElement::class,
        InvalidTtl::class
    )
    suspend fun send(
        message: MeshMessage,
        localElement: Element?,
        destination: MeshAddress,
        initialTtl: UByte?,
        applicationKey: ApplicationKey,
    ) {
        val networkManager = requireNotNull(networkManager) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Mesh Network not created"
            }
            throw NoNetwork()
        }
        val network = requireNotNull(network) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Mesh Network not created"
            }
            throw NoNetwork()
        }
        val localNode = requireNotNull(network.localProvisioner?.node) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Local Provisioner has no Unicast Address assigned"
            }
            throw InvalidSource()
        }
        val source = localElement ?: localNode.elements.firstOrNull() ?: run {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Local Provisioner has no Unicast Address assigned"
            }
            throw InvalidSource()
        }
        require(source.parentNode == localNode) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "The given Element does not belong to the local Node"
            }
            throw InvalidElement()
        }
        require(initialTtl == null || initialTtl <= 127u) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "TTL value $initialTtl is invalid"
            }
            throw InvalidTtl()
        }
        ensureNetworkKeyExists(key = applicationKey.boundNetworkKey)
        networkManager.send(
            message = message,
            element = source,
            destination = destination,
            initialTtl = initialTtl,
            applicationKey = applicationKey
        )
    }

    /**
     * Encrypts the message with the Application Key and a Network Key bound to it, and sends to the
     * given [Group].
     *
     * The method completes when the message has been sent or an error occurred.
     *
     * @param message        Message to be sent.
     * @param localElement   Source Element. If null, the primary Element will be used. The Element
     *                       must belong to the local Provisioner's Node.
     * @param group          Target Group.
     * @param initialTtl     Initial TTL (Time To Live) value of the message. If `null`, the default
     *                       Node TTL will be used.
     * @param key            Application Key to sign the message.
     * @throws NoNetwork if the mesh network has not been created.
     * @throws InvalidSource if the Local Provisioner has no Unicast Address assigned.
     * @throws InvalidDestination if the Address is not Unicast Address, an Unknown destination Node,
     *                            the Node does not have a Network Key,the Node's device key is
     *                            unknown or Cannot remove last Network Key.
     * @throws InvalidElement if the element does not belong to the local node.
     * @throws InvalidTtl if the TTL value is invalid.
     */
    @Throws(
        NoNetwork::class,
        InvalidDestination::class,
        InvalidSource::class,
        InvalidElement::class,
        InvalidTtl::class
    )
    suspend fun send(
        message: MeshMessage,
        localElement: Element? = null,
        group: Group,
        initialTtl: UByte? = null,
        key: ApplicationKey,
    ) {
        send(
            message = message,
            localElement = localElement,
            destination = group.address as MeshAddress,
            initialTtl = initialTtl,
            applicationKey = key
        )
    }

    /**
     * Encrypts the message with the first Application Key bound to the given [Model] and the
     * Network Key bound to it, and sends it to the Node to which the Model belongs to.
     *
     * The method completes when the message has been sent or an error occurred.
     *
     * @param message        Message to be sent.
     * @param localElement   Source Element. If `nil`, the primary Element will be used. The
     *                       Element must belong to the local Provisioner's Node.
     * @param model          Destination Model.
     * @param initialTtl     Initial TTL (Time To Live) value of the message. If `nil`, the default
     *                       Node TTL will be used.
     * @throws NoNetwork if the mesh network has not been created.
     * @throws InvalidDestination if the element does not belong to a node.
     * @throws ModelNotBoundToAppKey if the model is not bound to any application key.
     */
    @Throws(
        NoNetwork::class,
        InvalidDestination::class,
        ModelNotBoundToAppKey::class,
        InvalidSource::class,
        InvalidElement::class
    )
    suspend fun send(
        message: UnacknowledgedMeshMessage,
        localElement: Element? = null,
        model: Model,
        initialTtl: UByte? = null,
        applicationKey: ApplicationKey? = null,
    ) {
        if (network == null) throw NoNetwork()

        val node = model.parentElement?.parentNode ?: run {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Element does not belong to a Node"
            }
            throw InvalidDestination()
        }
        val destination = model.parentElement?.unicastAddress ?: run {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Element does not belong to a Node"
            }
            throw InvalidDestination()
        }

        // if the Application Key is given, check if it is bound to the Model.
        if (applicationKey != null) {
            if (!applicationKey.isBoundTo(model = model)) {
                logger?.e(LogCategory.FOUNDATION_MODEL) {
                    "Model is not bound to this Application Key"
                }
                throw ModelNotBoundToAppKey()
            }
        } else {
            // If not, make sure there are ay bound Application Keys.
            if (model.boundApplicationKeys.isEmpty()) {
                logger?.e(LogCategory.FOUNDATION_MODEL) {
                    "Error: Model is not bound to any Application Key."
                }
                throw NoAppKeysBoundToModel()
            }
        }
        // Check if the Application Key is known to the Proxy Node, or the message is sent to the
        // local Node.
        val selectedAppKey = applicationKey ?: model.boundApplicationKeys
            .firstOrNull { key ->
                // Unless the message is sent locally, take only keys known to the Proxy Node.
                node.isLocalProvisioner ||
                        proxyFilter.proxy?.knows(key = key.boundNetworkKey) == true
            }
        ?: run {
            logger?.e(LogCategory.PROXY) {
                "No GATT Proxy connected or no common Network Keys"
            }
            throw CannotRelay()
        }

        send(
            message = message,
            localElement = localElement,
            destination = destination,
            initialTtl = initialTtl,
            applicationKey = selectedAppKey
        )
    }

    /**
     * Encrypts the message with the common Application Key bound to both given [Model]s and the
     * Network Key bound to it, and sends it to the Node to which the target Model belongs to.
     *
     * The method completes when the message has been sent or an error occurred.
     *
     * @param message      Message to be sent.
     * @param localModel   Source Model who's primary Element will be used.
     * @param model        Destination Model.
     * @param initialTtl   Initial TTL (Time To Live) value of the message. If `nil`, the default
     *                     Node TTL will be used.
     * @throws NoNetwork if the mesh network has not been created.
     * @throws InvalidDestination if the element does not belong to a node.
     * @throws ModelNotBoundToAppKey if the model is not bound to any application key.
     * @throws InvalidSource if the source model does not belong to an Element or if the element
     *                       does not belong to the local node.
     */
    @Throws(
        NoNetwork::class,
        InvalidDestination::class,
        ModelNotBoundToAppKey::class,
        InvalidSource::class,
        InvalidElement::class
    )
    suspend fun send(
        message: UnacknowledgedMeshMessage,
        localModel: Model,
        model: Model,
        initialTtl: UByte? = null,
        applicationKey: ApplicationKey? = null,
    ) {
        localModel.parentElement?.let {
            send(
                message = message,
                localElement = it,
                model = model,
                initialTtl = initialTtl,
                applicationKey = applicationKey
            )
        } ?: {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Source Model does not belong to an Element"
            }
            throw InvalidSource()
        }
    }

    /**
     * Encrypts the message with the first Application Key bound to the given [Model] and a Network
     * Key bound to it, and sends it to the Node to which the Model belongs to and returns the
     * response.
     *
     * @param message        Message to be sent.
     * @param localElement   Source Element. If `nil`, the primary Element will be used. The Element
     *                       must belong to the local Provisioner's Node.
     * @param model          Destination Model.
     * @param initialTtl     Initial TTL (Time To Live) value of the message. If `nil`, the default
     *                       Node TTL will be used.
     * @returns A Response with the expected [AcknowledgedMeshMessage.responseOpCode] received from
     *          the target Node.
     * @throws NoNetwork if the mesh network has not been created.
     * @throws InvalidDestination if the Model does not belong to an Element.
     * @throws ModelNotBoundToAppKey if the model is not bound to any application key.
     * @throws InvalidSource if the Local Provisioner has no Unicast Address assigned.
     * @throws InvalidElement if the element does not belong to the local node.
     * @throws InvalidTtl if the TTL value is invalid.
     */
    @Throws(
        NoNetwork::class,
        InvalidDestination::class,
        ModelNotBoundToAppKey::class,
        InvalidSource::class,
        InvalidElement::class,
        InvalidTtl::class
    )
    suspend fun send(
        message: AcknowledgedMeshMessage,
        localElement: Element? = null,
        model: Model,
        initialTtl: UByte? = null,
        applicationKey: ApplicationKey? = null,
    ): MeshMessage? {
        val networkManager = requireNotNull(networkManager) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Mesh Network not created"
            }
            throw NoNetwork()
        }
        val network = requireNotNull(network) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Mesh Network not created"
            }
            throw NoNetwork()
        }
        val destination = requireNotNull(model.parentElement?.unicastAddress) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Model does not belong to an Element"
            }
            throw InvalidDestination()
        }
        val node = model.parentElement?.parentNode ?: run {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Element does not belong to a Node"
            }
            throw InvalidDestination()
        }

        if (applicationKey != null && !applicationKey.isBoundTo(model = model)) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Model is not bound to this Application Key"
            }
            throw ModelNotBoundToAppKey()
        }

        if (applicationKey == null && model.boundApplicationKeys.isEmpty()) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Model is not bound to any Application Key"
            }
            throw NoAppKeysBoundToModel()
        }

        // Check if the application Key is known to the Proxy Node, or the message is sent to the
        // local Node.
        val selectedAppKey = applicationKey ?: model.boundApplicationKeys
            .firstOrNull { key ->
                // Unless the message is sent locally, take only keys known to the Proxy Node.
                node.isLocalProvisioner ||
                        proxyFilter.proxy?.knows(key = key.boundNetworkKey) == true
            }
        ?: run {
            logger?.e(LogCategory.PROXY) {
                "No GATT Proxy connected or no common Network Keys"
            }
            throw CannotRelay()
        }

        val localNode = requireNotNull(network.localProvisioner?.node) {
            logger?.e(LogCategory.PROXY) {
                "Local Provisioner has no Unicast Address assigned"
            }
            throw InvalidSource()
        }
        val source = requireNotNull(localElement ?: localNode.elements.firstOrNull()) {
            logger?.e(LogCategory.PROXY) {
                "Local Provisioner has no Unicast Address assigned"
            }
            throw InvalidSource()
        }

        require(source.parentNode == localNode) {
            logger?.e(LogCategory.PROXY) {
                "The Element does not belong to the local Node"
            }
            throw InvalidElement()
        }
        require(initialTtl == null || initialTtl <= 127u) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "TTL value $initialTtl is invalid"
            }
            throw InvalidTtl()
        }
        ensureNetworkKeyExists(key = selectedAppKey.boundNetworkKey)
        return networkManager.send(
            message = message,
            element = source,
            destination = destination,
            initialTtl = initialTtl, // ?: 1u, uncomment to emulate sending messages with TTL = 1 to local node
            applicationKey = selectedAppKey
        )
    }

    /**
     * Encrypts the message with the common Application Key bound to both given [Model]s and a
     * Network Key bound to it, and sends it to the Node to which the target Model belongs to.
     *
     * @param message         Message to be sent.
     * @param localModel      Source Model who's primary element will be used.
     * @param model           Destination Model.
     * @param initialTtl      Initial TTL (Time To Live) value of the message. If `nil`, the default
     *                        Node TTL will be used.
     * @returns A response with the expected [AcknowledgedMeshMessage.responseOpCode] received from
     *          the target Node.
     * @throws NoNetwork if the mesh network has not been created.
     * @throws InvalidDestination if the Model does not belong to an Element.
     * @throws ModelNotBoundToAppKey if the model is not bound to any application key.
     * @throws InvalidSource if the source model does not belong to an Element.
     * @throws InvalidElement if the element does not belong to the local node.
     * @throws InvalidTtl if the TTL value is invalid.
     */
    @Throws(
        NoNetwork::class,
        InvalidDestination::class,
        ModelNotBoundToAppKey::class,
        InvalidSource::class,
        InvalidElement::class,
        InvalidTtl::class
    )
    suspend fun send(
        message: AcknowledgedMeshMessage,
        localModel: Model,
        model: Model,
        initialTtl: UByte?,
    ): MeshMessage = localModel.parentElement
        ?.let {
            send(message = message, localElement = it, model = model, initialTtl = initialTtl)
        } ?: run {
        logger?.e(LogCategory.FOUNDATION_MODEL) {
            "Source Model does not belong to an Element"
        }
        throw InvalidSource()
    }

    /**
     * Sends a Configuration Message to the Node with given destination address and returns the
     * received response.
     *
     * The `destination` must be a Unicast Address, otherwise the method throws an
     * [InvalidDestination] error.
     *
     * @param message                Message to be sent.
     * @param destination            Destination Unicast Address.
     * @param initialTtl             Initial TTL (Time To Live) value of the message. If `nil`,
     *                               the default Node TTL will be used.
     * @throws NoNetwork             If the mesh network has not been created.
     * @throws InvalidSource         Local Node does not have configuration capabilities (no Unicast
     *                               Address assigned).
     * @throws InvalidDestination    Destination address is not a Unicast Address, or it belongs to
     *                               an unknown Node.
     * @throws CannotDelete          When trying to delete the last Network Key on the device.
     * @returns Message handle that can be used to cancel sending.
     * @throws NoNetwork if the mesh network has not been created.
     * @throws InvalidSource if the Local Provisioner has no Unicast Address assigned.
     * @throws InvalidDestination if the Address is not Unicast Address, an Unknown destination
     *                            Node, the Node does not have a Network Key,the Node's device key
     *                            is unknown or Cannot remove last Network Key.
     * @throws InvalidTtl if the TTL value is invalid.
     */
    @OptIn(ExperimentalStdlibApi::class)
    @Throws(NoNetwork::class, InvalidSource::class, InvalidDestination::class, CannotDelete::class)
    suspend fun send(
        message: UnacknowledgedConfigMessage,
        destination: Address,
        initialTtl: UByte? = null,
        networkKey: NetworkKey? = null,
    ) {
        val networkManager = requireNotNull(networkManager) {
            logger?.e(LogCategory.PROXY) {
                "Mesh Network not created"
            }
            throw NoNetwork()
        }
        val network = requireNotNull(network) {
            logger?.e(LogCategory.PROXY) {
                "Mesh Network not created"
            }
            throw NoNetwork()
        }
        val element = requireNotNull(network.localProvisioner?.node?.primaryElement) {
            logger?.e(LogCategory.PROXY) {
                "Local Provisioner has no Unicast Address assigned"
            }
            throw InvalidSource()
        }
        val dst = MeshAddress.create(address = destination)
        require(dst is UnicastAddress) {
            logger?.e(LogCategory.PROXY) {
                "Address ${destination.toHexString()} is not a Unicast Address"
            }
            throw InvalidDestination()
        }
        val node = requireNotNull(value = network.node(address = dst)) {
            logger?.e(LogCategory.PROXY) {
                "Unknown destination Node"
            }
            throw InvalidDestination()
        }
        require(node.netKeys.isNotEmpty()) {
            logger?.e(LogCategory.PROXY) {
                "The target Node does not have a Network Key"
            }
            throw InvalidDestination()
        }

        if (networkKey != null && !node.knows(key = networkKey)) {
            logger?.e(LogCategory.PROXY) {
                "Node does not know the given Network Key"
            }
            throw InvalidKey()
        }

        // Check if the application Key is known to the Proxy Node, or the message is sent to the
        // local Node.
        val selectedNetKey = networkKey ?: node.networkKeys
            .firstOrNull {
                // Unless the message is sent locally, take only keys known to the Proxy Node.
                node.isLocalProvisioner || proxyFilter.proxy?.knows(it) == true
            }
        ?: run {
            logger?.e(LogCategory.PROXY) {
                "No GATT Proxy connected or no common Network Keys"
            }
            throw CannotRelay()
        }

        requireNotNull(node.deviceKey) {
            logger?.e(LogCategory.PROXY) {
                "Node's device key is unknown"
            }
            throw InvalidDestination()
        }
        require(initialTtl == null || initialTtl <= 127u) {
            logger?.e(LogCategory.PROXY) {
                "TTL value $initialTtl is invalid"
            }
            throw InvalidTtl()
        }
        ensureNetworkKeyExists(key = selectedNetKey)
        networkManager.send(
            configMessage = message,
            element = element,
            destination = destination,
            initialTtl = initialTtl,
            networkKey = selectedNetKey
        )
    }

    /**
     * Sends a Configuration Message to the primary Element on the given [Node].
     *
     * @param message                Message to be sent.
     * @param node                   Destination Node.
     * @param initialTtl             Initial TTL (Time To Live) value of the message. If `nil`, the
     *                               default Node TTL will be used.
     * @throws CannotDelete when trying to delete the last Network Key on the device.
     * @returns Message handle that can be used to cancel sending.
     * @throws NoNetwork if the mesh network has not been created.
     * @throws InvalidSource if the Local Provisioner has no Unicast Address assigned.
     * @throws InvalidDestination if the Address is not Unicast Address, an Unknown destination
     *                            Node, the Node does not have a Network Key,the Node's device key
     *                            is unknown or Cannot remove last Network Key.
     * @throws InvalidTtl if the TTL value is invalid.
     */
    @Throws(
        NoNetwork::class,
        InvalidDestination::class,
        InvalidSource::class,
        InvalidTtl::class
    )
    suspend fun send(message: UnacknowledgedConfigMessage, node: Node, initialTtl: UByte?) = send(
        message = message,
        destination = node.primaryUnicastAddress.address,
        initialTtl = initialTtl
    )

    /**
     * Sends Configuration Message to the Node with given destination Address.
     *
     * The [destination] must be a [UnicastAddress], otherwise the method throws an
     * [InvalidDestination] error.
     *
     * @param message         Message to be sent.
     * @param destination     Destination Unicast Address.
     * @param initialTtl      Initial TTL (Time To Live) value of the message. If `nil`, the default
     *                        Node TTL will be used.
     * @throws CannotDelete   when trying to delete the last Network Key on the device.
     * @returns Response associated with the message.
     * @throws NoNetwork if the mesh network has not been created.
     * @throws InvalidSource if the Local Provisioner has no Unicast Address assigned.
     * @throws InvalidDestination if the Address is not Unicast Address, an Unknown destination
     *                            Node, the Node does not have a Network Key,the Node's device key
     *                            is unknown or Cannot remove last Network Key.
     * @throws InvalidTtl if the TTL value is invalid.
     */
    @OptIn(ExperimentalStdlibApi::class)
    @Throws(
        NoNetwork::class,
        InvalidDestination::class,
        InvalidSource::class,
        InvalidTtl::class
    )
    suspend fun send(
        message: AcknowledgedConfigMessage,
        destination: Address,
        initialTtl: UByte? = null,
        networkKey: NetworkKey? = null,
    ): MeshMessage? {
        val networkManager = requireNotNull(networkManager) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Mesh Network not created"
            }
            throw NoNetwork()
        }
        val network = requireNotNull(network) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Mesh Network not created"
            }
            throw NoNetwork()
        }
        val element = requireNotNull(network.localProvisioner?.node?.primaryElement) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Local Provisioner has no Unicast Address assigned"
            }
            throw InvalidSource()
        }
        val dst = MeshAddress.create(address = destination)
        require(dst is UnicastAddress) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "${
                    destination.toHexString(
                        format = HexFormat {
                            number {
                                prefix = "0x"
                                minLength = 4
                                upperCase = true
                            }
                        }
                    )
                } is not a Unicast Address"
            }
            throw InvalidDestination()
        }
        val node = requireNotNull(value = network.node(address = dst)) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Unknown destination Node ${
                    destination.toHexString(
                        format = HexFormat {
                            number {
                                prefix = "0x"
                                minLength = 4
                                upperCase = true
                            }
                        }
                    )
                }"
            }
            throw InvalidDestination()
        }
        require(node.netKeys.isNotEmpty()) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "The target Node does not have a Network Key"
            }
            throw InvalidDestination()
        }

        if (networkKey != null && !node.knows(key = networkKey)) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Node does not know the given Network Key"
            }
            throw InvalidKey()
        }

        val selectedNetworkKey = networkKey ?: node.networkKeys
            .firstOrNull { key ->
                // A key that is being deleted cannot be used to send a message.
                (message as? ConfigNetKeyDelete)?.networkKeyIndex != key.index &&
                        // Unless the message is sent locally, take only keys known to the Proxy Node.
                        (node.isLocalProvisioner || proxyFilter.proxy?.knows(key = key) == true)
            }
        ?: run {
            if (message as? ConfigNetKeyDelete != null) {
                logger?.e(LogCategory.FOUNDATION_MODEL) {
                    "Cannot delete the last Network Key or a key used to secure the message"
                }
                throw CannotDelete()
            }
            logger?.e(LogCategory.PROXY) {
                "No GATT Proxy connected or no common Network Keys"
            }
            throw CannotRelay()
        }
        requireNotNull(node.deviceKey) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Node's device key is unknown"
            }
            throw InvalidDestination()
        }
        if (message is ConfigNetKeyDelete) {
            require(node.netKeys.size > 1) {
                logger?.e(LogCategory.FOUNDATION_MODEL) {
                    "Cannot remove last Network Key"
                }
                throw InvalidDestination()
            }
        }
        require(initialTtl == null || initialTtl <= 127u) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "TTL value $initialTtl is invalid"
            }
            throw InvalidTtl()
        }
        return networkManager.send(
            configMessage = message,
            element = element,
            destination = destination,
            initialTtl = initialTtl,
            networkKey = selectedNetworkKey
        )
    }

    /**
     * Sends a Configuration Message to the primary Element on the given [Node] and returns the
     * received response.
     *
     * @param message     Message to be sent.
     * @param node        Destination Node.
     * @param initialTtl  Initial TTL (Time To Live) value of the message. If `nil`, the default
     *                    Node TTL will be used.
     * @throws CannotDelete when trying to delete the last Network Key on the device.
     * @returns Response associated with the message.
     * @throws NoNetwork if the mesh network has not been created.
     * @throws InvalidSource if the Local Provisioner has no Unicast Address assigned.
     * @throws InvalidDestination if the Address is not Unicast Address, an Unknown destination
     *                            Node, the Node does not have a Network Key,the Node's device key
     *                            is unknown or Cannot remove last Network Key.
     * @throws InvalidTtl if the TTL value is invalid.
     */
    suspend fun send(message: AcknowledgedConfigMessage, node: Node, initialTtl: UByte?) = send(
        message = message,
        destination = node.primaryUnicastAddress.address,
        initialTtl = initialTtl
    )

    /**
     * Sends the Configuration Message to the primary Element of the local [Node] and returns the
     * received response.
     *
     * @param message The acknowledged configuration message to be sent.
     * @return The response associated with the message.
     * @throws NoNetwork when the mesh network has not been created
     * @throws InvalidSource when the local Node does not have configuration capabilities.
     */
    @Throws(NoNetwork::class, InvalidSource::class)
    suspend fun sendToLocalNode(message: AcknowledgedConfigMessage): MeshMessage? {
        val network = requireNotNull(network) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Mesh Network not created"
            }
            throw NoNetwork()
        }

        val destination = requireNotNull(network.localProvisioner?.node?.primaryUnicastAddress) {
            logger?.e(LogCategory.FOUNDATION_MODEL) {
                "Local Provisioner has no Unicast Address assigned"
            }
            throw InvalidSource()
        }
        return send(message = message, destination = destination.address, initialTtl = 1u)
    }

    /**
     * Sends the Proxy Configuration Message to the connected Proxy Node.
     *
     * This method will only work if the bearer uses is GATT Proxy. The message will be encrypted
     * and sent to the [Transmitter], which should deliver the PDU to the connected Node.
     *
     * @param message     Proxy Configuration message to be sent.
     * @throws NoNetwork When there is no mesh network created.
     * @throws IllegalStateException This method throws when the mesh network has not been created.
     */
    @Throws(NoNetwork::class, IllegalStateException::class)
    suspend fun send(message: ProxyConfigurationMessage): ProxyConfigurationMessage =
        networkManager?.send(message) ?: run {
            logger?.e(category = LogCategory.PROXY) { "Mesh Network not created" }
            throw NoNetwork()
        }

    /**
     * Observes network manager events.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun observeNetworkManagerEvents() {
        if (observeNetworkManagerEvents == null || observeNetworkManagerEvents?.isActive == false) {
            runCatching {
                observeNetworkManagerEvents = scope.launch {
                    networkManager?.networkManagerEventFlow?.onEach {
                        when (it) {
                            NetworkManagerEvent.OnNetworkChanged -> save()
                            NetworkManagerEvent.OnNetworkReset -> {
                                network?.localProvisioner?.let { provisioner ->
                                    val localElements = this@MeshNetworkManager.localElements
                                    provisioner.network = null
                                    create(provisioner = provisioner)
                                    this@MeshNetworkManager.localElements = localElements
                                }
                            }
                        }
                    }?.launchIn(scope = scope)
                }
            }.onFailure {
                logger?.w(category = LogCategory.FOUNDATION_MODEL) {
                    "Error while observing network manager events: ${it.message}"
                }
            }
        }
    }

    /**
     * Observes incoming mesh messages.
     */
    private fun observeMeshMessages() {
        if (observeMeshMessages == null || observeMeshMessages?.isActive == false) {
            runCatching {
                observeMeshMessages = networkManager?.incomingMeshMessages?.onEach {
                    if (it.message is AccessMessage) {
                        _incomingMeshMessages.emit(it.message)
                    }
                }?.launchIn(scope = scope)
            }.onFailure {
                logger?.w(category = LogCategory.FOUNDATION_MODEL) {
                    "Error while observing incoming mesh messages: ${it.message}"
                }
            }
        }
    }
}