package no.nordicsemi.android.nrfmesh.core.data

import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nordicsemi.android.nrfmesh.core.common.Configuration
import no.nordicsemi.android.nrfmesh.core.common.NORDIC_SEMICONDUCTOR_COMPANY_ID
import no.nordicsemi.android.nrfmesh.core.common.Utils.toAndroidLogLevel
import no.nordicsemi.android.nrfmesh.core.common.VendorModelIds
import no.nordicsemi.android.nrfmesh.core.common.di.IoDispatcher
import no.nordicsemi.android.nrfmesh.core.data.bearer.AndroidGattBearer
import no.nordicsemi.android.nrfmesh.core.data.configurator.Messengers
import no.nordicsemi.android.nrfmesh.core.data.model.GenericDefaultTransitionTimeServer
import no.nordicsemi.android.nrfmesh.core.data.model.GenericOnOffClient
import no.nordicsemi.android.nrfmesh.core.data.model.GenericOnOffServer
import no.nordicsemi.android.nrfmesh.core.data.modeleventhandlers.SensorClientEventHandler
import no.nordicsemi.android.nrfmesh.core.data.model.vendor.lepairing.PairingInitiatorClient
import no.nordicsemi.android.nrfmesh.core.data.model.vendor.simpleonoff.SimpleOnOffClient
import no.nordicsemi.android.nrfmesh.core.data.storage.SceneStatesDataStoreStorage
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.Peripheral
import no.nordicsemi.kotlin.ble.client.exception.BluetoothUnavailableException
import no.nordicsemi.kotlin.ble.client.exception.ScanningException
import no.nordicsemi.kotlin.ble.core.exception.ManagerClosedException
import no.nordicsemi.kotlin.mesh.bearer.BearerEvent
import no.nordicsemi.kotlin.mesh.bearer.MeshBearer
import no.nordicsemi.kotlin.mesh.bearer.gatt.GattBearer
import no.nordicsemi.kotlin.mesh.bearer.gatt.utils.MeshProxyService
import no.nordicsemi.kotlin.mesh.core.MeshNetworkManager
import no.nordicsemi.kotlin.mesh.core.NetworkEvent
import no.nordicsemi.kotlin.mesh.core.ProxyFilter
import no.nordicsemi.kotlin.mesh.core.exception.DoesNotBelongToNetwork
import no.nordicsemi.kotlin.mesh.core.exception.KeyIndexOutOfRange
import no.nordicsemi.kotlin.mesh.core.exception.NoNetwork
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.proxy.ProxyConfigurationMessage
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.Element
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.GroupRange
import no.nordicsemi.kotlin.mesh.core.model.Location
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.Provisioner
import no.nordicsemi.kotlin.mesh.core.model.SceneRange
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.UnicastRange
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import no.nordicsemi.kotlin.mesh.core.model.serialization.config.NetworkConfiguration
import no.nordicsemi.kotlin.mesh.core.util.networkIdentity
import no.nordicsemi.kotlin.mesh.core.util.nodeIdentity
import no.nordicsemi.kotlin.mesh.logger.LogCategory
import no.nordicsemi.kotlin.mesh.logger.LogLevel
import no.nordicsemi.kotlin.mesh.logger.Logger
import java.io.BufferedReader
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private object PreferenceKeys {
    val PROXY_AUTO_CONNECT = booleanPreferencesKey("proxy_auto_connect")
    val QUICK_PROVISIONING = booleanPreferencesKey("quick_provisioning")
    val ALWAYS_RECONFIGURE = booleanPreferencesKey("always_reconfigure")
}

class CoreDataRepository @Inject constructor(
    private val preferences: DataStore<Preferences>,
    private val meshNetworkManager: MeshNetworkManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val storage: SceneStatesDataStoreStorage,
    private val centralManager: CentralManager,
) {
    private var _proxyConnectionStateFlow = MutableStateFlow(ProxyConnectionState())
    val proxyConnectionStateFlow = _proxyConnectionStateFlow.asStateFlow()
    private val isAutoConnectEnabled: Boolean
        get() = _proxyConnectionStateFlow.value.autoConnect

    private var _developerSettingsStateFlow = MutableStateFlow(value = DeveloperSettings())
    val developerSettingsStateFlow = _developerSettingsStateFlow.asStateFlow()

    val networkEvents: SharedFlow<NetworkEvent> = meshNetworkManager.networkEvents
    val meshNetwork: MeshNetwork?
        get() = meshNetworkManager.network

    private var meshBearer: MeshBearer? = null
    private var bearerStateObserverJob: Job? = null
    val identifier: String?
        get() = (meshBearer as? AndroidGattBearer)?.identifier

    val proxyFilter: ProxyFilter
        get() = meshNetworkManager.proxyFilter
    val ivUpdateTestMode: Boolean
        get() = meshNetworkManager.networkParameters.ivUpdateTestMode

    private val ioScope = CoroutineScope(context = SupervisorJob() + ioDispatcher)

    val messengers = Messengers(meshNetworkManager = meshNetworkManager, scope = ioScope)

    private var connectivityJob: Job? = null

    val logger = object : Logger {
        override fun log(message: () -> String, category: LogCategory, level: LogLevel) {
            Log.println(level.toAndroidLogLevel(), category.category, message())
        }
    }

    init {
        // Initialize the mesh network manager logger
        meshNetworkManager.logger = logger
        // Observe proxy connection state changes
        observerAutomaticProxyConnectionState()
        observeDeveloperSettingsState()
    }

    fun close() {
        connectivityJob?.cancel()
        bearerStateObserverJob?.cancel()
        ioScope.launch {
            withContext(NonCancellable) {
                meshBearer?.close()
                meshBearer = null
            }
            ioScope.cancel()
        }
    }

    /**
     * Observes the automatic proxy connection state.
     */
    private fun observerAutomaticProxyConnectionState() {
        preferences.data
            .onEach { prefs ->
                _proxyConnectionStateFlow.update { state ->
                    state.copy(autoConnect = prefs[PreferenceKeys.PROXY_AUTO_CONNECT] == true)
                }
            }
            .launchIn(scope = ioScope)
    }

    /**
     * Observe changes to the developer settings.
     */
    private fun observeDeveloperSettingsState() {
        preferences.data
            .onEach { preferences ->
                _developerSettingsStateFlow.update { state ->
                    state.copy(
                        quickProvisioning = preferences[PreferenceKeys.QUICK_PROVISIONING] == true,
                        alwaysReconfigure = preferences[PreferenceKeys.ALWAYS_RECONFIGURE] == true
                    )
                }
            }
            .launchIn(scope = ioScope)
    }

    /**
     * Loads an existing mesh network.
     *
     * @return True if the network was loaded, false otherwise.
     */
    suspend fun load() = withContext(context = ioDispatcher) {
        if (meshNetworkManager.load()) {
            onMeshNetworkChanged()
            true
        } else {
            false
        }
    }

    /**
     * Creates a new mesh network.
     *
     * @param configuration Initial network configuration.
     * @return The new network instance.
     */
    @OptIn(ExperimentalUuidApi::class)
    suspend fun createNewMeshNetwork(
        configuration: Configuration = Configuration.Empty,
    ) = withContext(context = ioDispatcher) {
        meshNetworkManager.create(
            provisioner = Provisioner(name = createProvisionerName()).apply {
                allocate(
                    range = UnicastRange(
                        lowAddress = UnicastAddress(address = 0x0001),
                        highAddress = UnicastAddress(address = 0x199B)
                    )
                )
                allocate(
                    range = GroupRange(
                        lowAddress = GroupAddress(address = 0xC000),
                        highAddress = GroupAddress(address = 0xCC9A)
                    )
                )
                allocate(range = SceneRange(firstScene = 0x0001u, lastScene = 0x3333u))
            },
            networkKeys = configuration.generateNetworkKeys()
        ).also { meshNetwork ->
            configuration.generateApplicationKeys().forEachIndexed { index, key ->
                meshNetwork.add(
                    name = "Application Key ${index + 1}",
                    key = key,
                    boundNetworkKey = meshNetwork.networkKeys.first()
                )
            }

            for (group in 0 until configuration.groups) {
                val groupAddress = meshNetwork.nextAvailableGroup(
                    provisioner = meshNetwork.provisioners.first()
                ) ?: continue
                meshNetwork.add(group = Group(_name = "Group ${group + 1}", address = groupAddress))
            }
            val groupSize = meshNetwork.groups.size + configuration.virtualGroups
            for (index in meshNetwork.groups.size until groupSize) {
                meshNetwork.add(
                    group = Group(
                        _name = "Group ${index + 1}",
                        address = VirtualAddress(uuid = Uuid.random())
                    )
                )
            }

            for (scene in 0 until configuration.scenes) {
                val sceneAddress = meshNetwork.nextAvailableScene(
                    provisioner = meshNetwork.provisioners.first()
                ) ?: continue
                meshNetwork.add(name = "Scene $scene", number = sceneAddress)
            }
        }.also {
            onMeshNetworkChanged()
            if (isAutoConnectEnabled) {
                startAutomaticConnectivity()
            }
        }
    }

    /**
     * Invoked when the mesh network has changed. This will set up the local elements and
     * reinitialize the connection to the proxy node. This will ensure that the user is connected to
     * the correct network.
     */
    private fun onMeshNetworkChanged() {
        val defaultTransitionTimeServer = GenericDefaultTransitionTimeServer()
        // Sets up the local Elements on the phone
        val element0 = Element(
            _name = "Primary Element",
            location = Location.FIRST,
            _models = mutableListOf(
                Model(modelId = SigModelId(modelIdentifier = Model.SCENE_SERVER_MODEL_ID)),
                Model(modelId = SigModelId(modelIdentifier = Model.SCENE_SETUP_SERVER_MODEL_ID)),
                Model(modelId = SigModelId(modelIdentifier = Model.SENSOR_CLIENT_MODEL_ID), handler = SensorClientEventHandler()),
                Model(modelId = SigModelId(modelIdentifier = Model.GENERIC_POWER_ON_OFF_CLIENT_MODEL_ID)),
                Model(modelId = SigModelId(modelIdentifier = Model.GENERIC_DEFAULT_TRANSITION_TIME_SERVER_MODEL_ID)),
                Model(modelId = SigModelId(modelIdentifier = Model.GENERIC_DEFAULT_TRANSITION_TIME_CLIENT_MODEL_ID)),
                // Generic OnOff and Generic Level models defined by SIG
                Model(
                    modelId = SigModelId(modelIdentifier = Model.GENERIC_ON_OFF_SERVER_MODEL_ID),
                    handler = GenericOnOffServer(
                        ioDispatcher = ioDispatcher,
                        storage = storage,
                        defaultTransitionTimeServer = defaultTransitionTimeServer
                    )
                ),
                Model(modelId = SigModelId(modelIdentifier = Model.GENERIC_LEVEL_SERVER_MODEL_ID)),
                Model(
                    modelId = SigModelId(modelIdentifier = Model.GENERIC_ON_OFF_CLIENT_MODEL_ID),
                    handler = GenericOnOffClient()
                ),
                Model(modelId = SigModelId(modelIdentifier = Model.GENERIC_LEVEL_CLIENT_MODEL_ID)),
                Model(modelId = SigModelId(modelIdentifier = Model.LIGHT_LC_CLIENT_MODEL_ID)),
                // Nordic Pairing Initiator model
                Model(
                    modelId = VendorModelId(
                        modelIdentifier = VendorModelIds.LE_PAIRING_INITIATOR,
                        companyIdentifier = NORDIC_SEMICONDUCTOR_COMPANY_ID
                    ),
                    handler = PairingInitiatorClient()
                ),
                // A simple vendor model
                Model(
                    modelId = VendorModelId(
                        modelIdentifier = VendorModelIds.SIMPLE_ON_OFF_CLIENT_MODEL_ID,
                        companyIdentifier = NORDIC_SEMICONDUCTOR_COMPANY_ID
                    ),
                    handler = SimpleOnOffClient(this)
                )
            )
        )
        val element1 = Element(
            _name = "Secondary Element",
            location = Location.SECOND,
            _models = mutableListOf(
                Model(modelId = SigModelId(Model.GENERIC_ON_OFF_SERVER_MODEL_ID)),
                Model(modelId = SigModelId(Model.GENERIC_LEVEL_SERVER_MODEL_ID)),
                Model(modelId = SigModelId(Model.GENERIC_ON_OFF_CLIENT_MODEL_ID)),
                Model(modelId = SigModelId(Model.GENERIC_LEVEL_CLIENT_MODEL_ID))
            )
        )
        meshNetworkManager.localElements = listOf(element0, element1)
    }

    /**
     * Imports a mesh network.
     *
     * During the import process, the app will disconnect from any existing nodes it may be
     * connected to. If automatic connectivity is toggled the app will connect to one of the new nodes
     * from the newly imported network if it finds any.
     *
     * @param uri                  URI of the file.
     * @param contentResolver      Content resolver.
     */
    suspend fun importNetwork(uri: Uri, contentResolver: ContentResolver) {
        // First lets check the current connectivity state.
        val wasAutoConnectEnabled = isAutoConnectEnabled
        // If auto connect is enabled, disable it first to avoid reconnection when disconnecting in
        // the next step.
        if (wasAutoConnectEnabled) {
            toggleAutomaticConnection(enabled = false)
        }
        // Make sure to disconnect from any existing nodes.
        disconnect()
        // Import the network from the given URI.
        withContext(ioDispatcher) {
            val networkJson = contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(inputStream.reader()).use { bufferedReader ->
                    bufferedReader.readText()
                }
            } ?: ""
            meshNetworkManager.import(array = networkJson.encodeToByteArray())
            onMeshNetworkChanged()
            save()
        }
        // If auto connect was previously enabled, enable it again.
        if (wasAutoConnectEnabled) {
            toggleAutomaticConnection(enabled = true)
        }
    }

    /**
     * Exports a mesh network.
     */
    fun exportNetwork(configuration: NetworkConfiguration) =
        meshNetworkManager.export(configuration = configuration)

    suspend fun resetNetwork() = meshNetworkManager.clear()
        .also { disconnect() }

    /**
     * Adds a network key to the network.
     *
     * @param name            Name of the Network Key.
     */
    fun addNetworkKey(
        name: String = "Network Key ${
            meshNetwork?.let {
                it.nextAvailableNetworkKeyIndex ?: throw KeyIndexOutOfRange()
            } ?: throw NoNetwork()
        }",
    ): NetworkKey = meshNetwork?.add(name = name)
        ?.also { save() }
        ?: throw NoNetwork()

    /**
     * Adds an application key to the network.
     *
     * @param name            Name of the Application Key.
     * @param boundNetworkKey Bound Network Key
     */
    fun addApplicationKey(
        name: String = "Application Key ${
            meshNetwork?.let {
                it.nextAvailableApplicationKeyIndex?.inc() ?: throw KeyIndexOutOfRange()
            } ?: throw NoNetwork()
        }",
        boundNetworkKey: NetworkKey,
    ): ApplicationKey = meshNetwork?.add(name = name, boundNetworkKey = boundNetworkKey)
        ?.also { save() }
        ?: throw NoNetwork()

    /**
     * Saves the mesh network.
     */
    fun save() {
        ioScope.launch {
            meshNetworkManager.save()
        }
    }

    suspend fun toggleQuickProvisioning(flag: Boolean): Unit = withContext(context = ioDispatcher) {
        preferences.edit { preferences ->
            preferences[PreferenceKeys.QUICK_PROVISIONING] = flag
        }
    }

    suspend fun toggleAlwaysReconfigure(flag: Boolean): Unit = withContext(context = ioDispatcher) {
        preferences.edit { preferences ->
            preferences[PreferenceKeys.ALWAYS_RECONFIGURE] = flag
        }
    }

    /**
     * This method initiates the automatic connectivity if it is enabled in the settings.
     */
    fun onBluetoothEnabled() {
        if (isAutoConnectEnabled) {
            startAutomaticConnectivity()
        }
    }

    /**
     * Enables or disables automatic connectivity to a proxy node.
     *
     * @param meshNetwork Mesh network required to match a proxy node.
     * @param enabled     True to enable, false to disable.
     */
    suspend fun toggleAutomaticConnection(enabled: Boolean): Unit =
        withContext(context = ioDispatcher) {
            _proxyConnectionStateFlow.update {
                it.copy(autoConnect = enabled)
            }
            preferences.edit { preferences ->
                preferences[PreferenceKeys.PROXY_AUTO_CONNECT] = enabled
            }

            if (enabled) {
                startAutomaticConnectivity()
            } else {
                cancelAutomaticConnectivity()
            }
        }

    /**
     * Starts automatic connectivity to the proxy node.
     *
     * @param meshNetwork Mesh network required to match the proxy node.
     */
    @OptIn(ExperimentalUuidApi::class)
    private fun startAutomaticConnectivity() {
        // Make sure we don't start this multiple times.
        if (connectivityJob != null) {
            return
        }

        // Start a coroutine that will scan and connect to any GATT Proxy in this network.
        connectivityJob = ioScope.launch {
            while (isAutoConnectEnabled) {
                // Added a check here to avoid a possible crash when the network is reset
                if (meshNetwork == null) {
                    break
                }
                val bearer = meshBearer ?: scanForGattProxy()
                open(bearer)
                awaitDisconnection(bearer)
                // Added a delay of 1500 seconds to avoid any reconnects
                // Clarify this against scan results from gatt proxy
                delay(1500.milliseconds)
            }
            connectivityJob = null
        }
    }

    /**
     * Cancels automatic connectivity to the proxy node.
     *
     * This method will not disconnect an existing connection.
     */
    private fun cancelAutomaticConnectivity() {
        connectivityJob?.cancel()
        connectivityJob = null
    }

    /**
     * Connects to a proxy node with given UUID.
     *
     * @param uuid UUID of the proxy node.
     * @throws NoNetwork If the mesh network has not been initialized.
     * @throws DoesNotBelongToNetwork If the mesh network doesn't contain a node with the given UUID.
     */
    @OptIn(ExperimentalUuidApi::class)
    fun connect(uuid: Uuid) {
        val meshNetwork = requireNotNull(meshNetwork) { throw NoNetwork() }
        val node = requireNotNull(meshNetwork.node(uuid = uuid)) { throw DoesNotBelongToNetwork() }
        ioScope.launch {
            cancelAutomaticConnectivity()
            // open(bearer) below will close any existing bearer.

            val bearer = scanForGattProxy(address = node.primaryUnicastAddress)
            open(bearer)

            if (isAutoConnectEnabled) {
                startAutomaticConnectivity()
            }
        }
    }

    /**
     * Connects to a peripheral.
     *
     * @param peripheral Peripheral to connect to.
     */
    fun connect(peripheral: Peripheral) {
        ioScope.launch {
            val bearer = createGattProxyBearer(peripheral)
            open(bearer)
        }
    }

    /**
     * Disconnects from the connected bearer.
     */
    fun disconnect() {
        ioScope.launch { closeBearer() }
    }

    /**
     * Scans for the proxy node.
     *
     * @param address An optional Unicast Address of the Node.
     * @return The peripheral with Mesh Proxy Service.
     * @throws CancellationException If the scan is canceled.
     * @throws ManagerClosedException If the central manager has been closed.
     * @throws BluetoothUnavailableException If Bluetooth is disabled or not available.
     * @throws SecurityException If the permission to scan is denied.
     * @throws ScanningException If an error occurred while starting the scan.
     */
    @OptIn(ExperimentalUuidApi::class)
    private suspend fun scanForGattProxy(address: UnicastAddress? = null): MeshBearer {
        val meshNetwork = requireNotNull(meshNetwork)
        val peripheral = centralManager
            .scan { ServiceUuid(uuid = MeshProxyService.uuid) }
            .onStart {
                _proxyConnectionStateFlow.update {
                    it.copy(connectionState = NetworkConnectionState.Scanning)
                }
            }
            .onCompletion {
                _proxyConnectionStateFlow.update { state ->
                    state.copy(connectionState = NetworkConnectionState.Disconnected)
                }
            }
            .first { scanResult ->
                // Try to parse the Node Identity beacon or Network Identity beacon.
                val beaconData = scanResult.advertisingData
                    .serviceData[MeshProxyService.uuid] ?: return@first false

                // If the address is given, scan only for the Node Identity.
                if (address != null) {
                    val scannedNodeIdentity = beaconData.nodeIdentity() ?: return@first false
                    return@first meshNetwork.node(scannedNodeIdentity)?.primaryUnicastAddress == address
                }
                // For a generic scan, try any (private or public) Node Identity...
                val scannedNodeIdentity = beaconData.nodeIdentity()
                if (scannedNodeIdentity != null) {
                    return@first meshNetwork.matches(nodeIdentity = scannedNodeIdentity)
                }
                // ...or a (private or public) Network Identity.
                val scannedNetworkIdentity = beaconData.networkIdentity()
                if (scannedNetworkIdentity != null) {
                    return@first meshNetwork.matches(networkId = scannedNetworkIdentity)
                }
                return@first false
            }
            .peripheral
        return createGattProxyBearer(peripheral)
    }

    /**
     * Creates a GATT proxy bearer.
     *
     * @param peripheral Peripheral with Mesh Proxy service.
     */
    private fun createGattProxyBearer(peripheral: Peripheral) =
        AndroidGattBearer(centralManager, peripheral, ioDispatcher)
            .also { it.logger = this@CoreDataRepository.logger }

    /**
     * Opens the bearer.
     *
     * @param bearer       The peripheral with Mesh Proxy service.
     */
    private suspend fun open(bearer: MeshBearer) = withContext(context = ioDispatcher) {
        // Check if the bearer isn't already open. Can't be more open than that...
        if (bearer.isOpen) {
            return@withContext
        }
        // GATT Bearer does have a name - the name of the Bluetooth LE peripheral.
        val name = (bearer as? GattBearer)?.name

        // Make sure the old bearer is closed.
        closeBearer()

        // From now on, use the new bearer.
        meshNetworkManager.meshBearer = bearer

        // Observe bearer state.
        bearerStateObserverJob = bearer.state
            // Skip initial Closed state.
            .drop(1)
            .onEach { event ->
                when (event) {
                    is BearerEvent.Opened -> {
                        meshBearer = bearer
                        _proxyConnectionStateFlow.update {
                            it.copy(connectionState = NetworkConnectionState.Connected(name))
                        }
                    }

                    is BearerEvent.Closed -> {
                        meshBearer = null
                        meshNetworkManager.proxyFilter.proxyDidDisconnect()

                        _proxyConnectionStateFlow.update {
                            it.copy(connectionState = NetworkConnectionState.Disconnected)
                        }
                    }
                }
            }
            .launchIn(ioScope)

        _proxyConnectionStateFlow.update {
            it.copy(connectionState = NetworkConnectionState.Connecting(name))
        }
        try {
            // This will suspend until the bearer is open.
            bearer.open()
        } catch (e: Exception) {
            logger.log(
                message = { "Failed to open bearer: ${e.message}" },
                category = LogCategory.BEARER,
                level = LogLevel.ERROR
            )
        }
    }

    /**
     * Suspends until the bearer is disconnected.
     *
     * @param bearer The bearer to expected to be disconnected.
     */
    private suspend fun awaitDisconnection(bearer: MeshBearer) {
        withContext(context = ioDispatcher) {
            bearer.state.first { it is BearerEvent.Closed }
        }
    }

    /**
     * Closes the bearer and cancels its state observer.
     *
     * This method does nothing if the [meshBearer] is not set.
     */
    private suspend fun closeBearer() {
        withContext(NonCancellable) {
            meshBearer?.close()
            meshBearer = null
            bearerStateObserverJob?.cancel()
        }
    }

    /**
     * Sends a proxy configuration message to the proxy node.
     *
     * @param message Proxy configuration message to be sent.
     */
    suspend fun send(message: ProxyConfigurationMessage) = withContext(context = ioDispatcher) {
        meshNetworkManager.send(message)
    }

    /**
     * Sends an acknowledged config messages to the given node.
     *
     * @param node    Destination node.
     * @param message Message to be sent.
     */
    suspend fun send(node: Node, message: AcknowledgedConfigMessage) =
        withContext(context = ioDispatcher) {
            if (node.primaryUnicastAddress == meshNetwork?.localProvisioner?.node?.primaryUnicastAddress)
                meshNetworkManager.sendToLocalNode(message = message)
            else
                meshNetworkManager.send(message = message, node = node, initialTtl = null)
        }

    /**
     * Sends an unacknowledged mesh message to the given model.
     *
     * @param model          Destination model.
     * @param unackedMessage Unacknowledged mesh message to be sent.
     */
    suspend fun send(model: Model, unackedMessage: UnacknowledgedMeshMessage) = withContext(
        context = ioDispatcher
    ) {
        val parentElement = requireNotNull(model.parentElement)
        meshNetworkManager.send(
            model = model,
            message = unackedMessage,
            initialTtl = if (isDestinedToLocalNode(parentElement.unicastAddress)) {
                1.toUByte() // Use TTL 1 for messages destined to the local node
            } else {
                null
            }
        )
    }

    /**
     * Sends an unacknowledged mesh message to the given model.
     *
     * @param group          Group to which the message should be sent.
     * @param unackedMessage Unacknowledged mesh message to be sent.
     * @param key            Application key to be used for sending the message.
     */
    suspend fun send(group: Group, unackedMessage: UnacknowledgedMeshMessage, key: ApplicationKey) =
        withContext(context = ioDispatcher) {
            meshNetworkManager.send(group = group, message = unackedMessage, key = key)
        }

    /**
     * Sends an acknowledged mesh message to the given model.
     *
     * @param model        Destination model.
     * @param ackedMessage Unacknowledged mesh message to be sent.
     */
    suspend fun send(model: Model, ackedMessage: AcknowledgedMeshMessage) = withContext(
        context = ioDispatcher
    ) {
        val parentElement = requireNotNull(model.parentElement)
        meshNetworkManager.send(
            model = model,
            message = ackedMessage,
            initialTtl = if (isDestinedToLocalNode(parentElement.unicastAddress)) {
                1.toUByte() // Use TTL 1 for messages destined to the local node
            } else {
                null
            }
        )
    }

    private fun isDestinedToLocalNode(destination: UnicastAddress) =
        requireNotNull(meshNetwork).node(destination)?.isLocalProvisioner == true

    /**
     * Creates provisioner name based on the provisioner device model.
     */
    fun createProvisionerName(): String = Build.MODEL.replaceFirstChar {
        if (it.isLowerCase()) it.titlecase(Locale.ROOT)
        else it.toString()
    }

    fun toggleIvUpdateTestMode(flag: Boolean) {
        meshNetworkManager.networkParameters.ivUpdateTestMode = flag
    }

    /**
     * Gets the peripheral with the given identifier.
     */
    fun getPeripheral(): Peripheral? = identifier?.let {
        centralManager.getPeripheralById(id = it)
    }

    companion object {
        @OptIn(ExperimentalUuidApi::class)
        val SMP_SERVICE = Uuid.parse("8D53DC1D-1DB7-4CD3-868B-8A527460AA84")
    }
}

sealed class NetworkConnectionState {
    data object Scanning : NetworkConnectionState()
    data class Connecting(val name: String?) : NetworkConnectionState()
    data class Connected(val name: String?) : NetworkConnectionState()
    data object Disconnected : NetworkConnectionState()
}

data class ProxyConnectionState(
    val autoConnect: Boolean = false, // Auto connect is disabled due to missing permissions
    val connectionState: NetworkConnectionState = NetworkConnectionState.Disconnected,
)