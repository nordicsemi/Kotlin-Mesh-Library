package no.nordicsemi.android.nrfmesh.feature.provisioning

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.Timer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import no.nordicsemi.android.nrfmesh.core.common.di.IoDispatcher
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.DeveloperSettings
import no.nordicsemi.android.nrfmesh.core.data.bearer.AndroidPbGattBearer
import no.nordicsemi.android.nrfmesh.core.data.configurator.ConfigTask
import no.nordicsemi.android.nrfmesh.feature.provisioning.ProvisionerState.Connected
import no.nordicsemi.android.nrfmesh.feature.provisioning.ProvisionerState.Connecting
import no.nordicsemi.android.nrfmesh.feature.provisioning.ProvisionerState.Disconnected
import no.nordicsemi.android.nrfmesh.feature.provisioning.ProvisionerState.Error
import no.nordicsemi.android.nrfmesh.feature.provisioning.ProvisionerState.Provisioning
import no.nordicsemi.android.nrfmesh.feature.provisioning.ProvisionerState.Scanning
import no.nordicsemi.kotlin.ble.client.android.CentralManager
import no.nordicsemi.kotlin.ble.client.android.ScanResult
import no.nordicsemi.kotlin.mesh.bearer.BearerEvent
import no.nordicsemi.kotlin.mesh.bearer.provisioning.ProvisioningBearer
import no.nordicsemi.kotlin.mesh.core.exception.NoNetwork
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigCompositionDataGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigDefaultTtlGet
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.logger.LogCategory
import no.nordicsemi.kotlin.mesh.logger.LogLevel
import no.nordicsemi.kotlin.mesh.provisioning.AuthAction
import no.nordicsemi.kotlin.mesh.provisioning.AuthenticationMethod
import no.nordicsemi.kotlin.mesh.provisioning.ProvisioningManager
import no.nordicsemi.kotlin.mesh.provisioning.ProvisioningParameters
import no.nordicsemi.kotlin.mesh.provisioning.ProvisioningState
import no.nordicsemi.kotlin.mesh.provisioning.UnprovisionedDevice
import javax.inject.Inject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@HiltViewModel
class ProvisioningViewModel @Inject constructor(
    private val repository: CoreDataRepository,
    @param:IoDispatcher private val dispatcher: CoroutineDispatcher,
    private val centralManager: CentralManager,
) : ViewModel() {
    private lateinit var meshNetwork: MeshNetwork
    private var pbBearer: ProvisioningBearer? = null
    private var pbBearerStateObserverJob: Job? = null
    private var provisioningManager: ProvisioningManager? = null
    private var unprovisionedDevice: UnprovisionedDevice? = null
    private var originalNode: Node? = null

    private val _uiState = MutableStateFlow(
        value = ProvisioningScreenUiState(provisionerState = Scanning)
    )
    internal val uiState = _uiState.asStateFlow()

    init {
        observeNetwork()
        observeDeveloperSettings()
    }

    /**
     * Observes the mesh network.
     */
    private fun observeNetwork() {
        repository.networkEvents
            .map { repository.meshNetwork }
            .filterNotNull()
            .onEach {
                meshNetwork = it
                _uiState.update { state ->
                    state.copy(
                        networkKeys = it.networkKeys.toList(),
                    )
                }
            }
            .launchIn(scope = viewModelScope)
    }

    private fun observeDeveloperSettings() {
        repository.developerSettingsStateFlow
            .onEach {
                _uiState.update { state ->
                    state.copy(developerSettings = it)
                }
            }
            .launchIn(scope = viewModelScope)
    }

    /**
     * Selects the scan result for provisioning.
     *
     * This method also returns if there exist a Node already in the network with the same UUID.
     *
     * @param scanResult Scan result of the device to be checked.
     * @return True if there exist a Node already in the network with the same UUID.
     * @throws IllegalArgumentException if the advertisement data is invalid.
     */
    @OptIn(ExperimentalUuidApi::class)
    internal fun onScanResultSelected(scanResult: ScanResult): Boolean {
        assert(pbBearer == null)

        // Note: This method may crash if the advertising data is invalid.
        val device = UnprovisionedDevice
            .from(advertisementData = scanResult.advertisingData.raw)

        // Store the selected results.
        pbBearer = AndroidPbGattBearer(centralManager, scanResult.peripheral, dispatcher)
            .apply { logger = repository.logger }
        unprovisionedDevice = device
        originalNode = meshNetwork?.node(uuid = device.uuid)

        // Return whether the Node with the same UUID already exists in the network.
        return originalNode != null
    }

    /**
     * Starts the provisioning process by identifying the node
     */
    internal fun beginProvisioning(shouldReconfigure: Boolean = false) {
        // The onScanResultSelected must be called prior to this method.
        val pbBearer = pbBearer ?: return
        val device = unprovisionedDevice ?: return

        // Clear the original Node it reconfiguration is not needed.
        if (!shouldReconfigure) {
            originalNode = null
        }

        viewModelScope.launch {
            connectOverPbGattBearer(unprovisionedDevice = device, pbBearer = pbBearer)
            identifyNode(unprovisionedDevice = device, bearer = pbBearer)
        }
    }

    /**
     * Identify the node by sending a provisioning invite.
     *
     * @param unprovisionedDevice  Device to be provisioned.
     * @param bearer               Provisioning bearer to be used.
     */
    private fun identifyNode(
        unprovisionedDevice: UnprovisionedDevice,
        bearer: ProvisioningBearer,
    ) {
        val meshNetwork = requireNotNull(meshNetwork) { throw NoNetwork() }

        // The Provisioning Manager will carry on the provisioning.
        val provisioningManager = ProvisioningManager(
            unprovisionedDevice = unprovisionedDevice,
            meshNetwork = meshNetwork,
            bearer = bearer,
            ioDispatcher = dispatcher
        ).apply {
            logger = repository.logger
            provisioningManager = this
        }

        provisioningManager.provision(attentionTimer = 10u)
            .onEach { state ->
                _uiState.update { uiState ->
                    uiState.copy(
                        provisionerState = Provisioning(
                            unprovisionedDevice = unprovisionedDevice,
                            state = state
                        ),
                        provisioningParameters = when (state) {
                            is ProvisioningState.CapabilitiesReceived -> state.defaultParameters
                            else -> uiState.provisioningParameters
                        }
                    )
                }
                if (state is ProvisioningState.Complete) {
                    repository.save()
                }
            }
            .filterIsInstance<ProvisioningState.CapabilitiesReceived>()
            .onEach { state ->
                // If Quick Provisioning is set, use default parameters.
                if (_uiState.value.developerSettings.quickProvisioning) {
                    state.start(state.defaultParameters)
                }
            }
            .catch { throwable ->
                repository.logger.log(
                    message = { "Error while provisioning: $throwable" },
                    category = LogCategory.PROVISIONING,
                    level = LogLevel.ERROR
                )
                _uiState.value = _uiState.value.copy(
                    provisionerState = Error(
                        unprovisionedDevice = unprovisionedDevice,
                        throwable = throwable
                    )
                )
            }
            .launchIn(scope = viewModelScope)
    }

    /**
     * Connects to the unprovisioned node over PB-Gatt bearer.
     *
     * @param unprovisionedDevice Device to be provisioned.
     * @param pbBearer            Provisioning bearer to be used.
     */
    private suspend fun connectOverPbGattBearer(
        unprovisionedDevice: UnprovisionedDevice,
        pbBearer: ProvisioningBearer,
    ) = withContext(context = dispatcher) {
        // Make sure the old observer is closed.
        pbBearerStateObserverJob?.cancel()

        // Observe state changes.
        pbBearerStateObserverJob = pbBearer.state
            .drop(1)
            .onEach { event ->
                when (event) {
                    is BearerEvent.Opened -> {
                        _uiState.update {
                            it.copy(provisionerState = Connected(unprovisionedDevice = unprovisionedDevice))
                        }
                    }

                    is BearerEvent.Closed -> {
                        this@ProvisioningViewModel.pbBearer = null
                        _uiState.update {
                            it.copy(provisionerState = Disconnected(unprovisionedDevice = unprovisionedDevice))
                        }
                    }
                }
            }
            .launchIn(viewModelScope)

        // And open the bearer.
        _uiState.update {
            it.copy(provisionerState = Connecting(unprovisionedDevice = unprovisionedDevice))
        }
        pbBearer.open()
    }

    /**
     * Disconnect from the device.
     */
    internal fun disconnect() {
        viewModelScope.launch {
            pbBearer?.close()
            pbBearerStateObserverJob?.cancel()
            pbBearer = null
            _uiState.update { it.copy(provisionerState = Scanning) }
        }
    }

    /**
     * Invoked when the user changes the name of the device.
     *
     * @param name New name to be assigned to the device.
     */
    internal fun onNameChanged(name: String) {
        unprovisionedDevice?.name = name
    }

    /**
     * Invoked when the user changes the unicast address.
     *
     * @param address       Address to be assigned to the node.
     */
    internal fun onAddressChanged(
        address: Address,
    ) {
        val provisionerState =
            _uiState.value.provisionerState as? Provisioning ?: return
        val capabilitiesReceivedState =
            provisionerState.state as? ProvisioningState.CapabilitiesReceived ?: return
        val elementCount = capabilitiesReceivedState.capabilities.numberOfElements
        val unicastAddress = UnicastAddress(address)

        provisioningManager?.isUnicastAddressValid(
            unicastAddress = unicastAddress,
            numberOfElements = elementCount,
        ).also {
            _uiState.update {
                it.copy(
                    provisioningParameters = it.provisioningParameters?.copy(unicastAddress = unicastAddress)
                )
            }
        }
    }

    /**
     * Checks if the given address is valid.
     */
    internal fun isValidAddress(address: Address): Boolean = when {
        UnicastAddress.isValid(address = address) -> true
        else -> throw Throwable("Invalid unicast address")
    }

    internal fun onNetworkKeyClicked(key: NetworkKey) {
        _uiState.update {
            it.copy(provisioningParameters = it.provisioningParameters?.copy(networkKey = key))
        }
    }

    /**
     * Starts the provisioning process after the identification is completed.
     *
     * @param authMethod Authentication method to be used.
     */
    internal fun onAuthenticationMethodSelected(authMethod: AuthenticationMethod) {
        val parameters = _uiState.value.provisioningParameters ?: return
        val provisionerState =
            _uiState.value.provisionerState as? Provisioning ?: return
        val capabilitiesReceivedState =
            provisionerState.state as? ProvisioningState.CapabilitiesReceived ?: return

        viewModelScope.launch {
            capabilitiesReceivedState.start(parameters.copy(authMethod = authMethod))
        }
    }

    /**
     * Invoked when the user selects an authentication method.
     *
     * @param method Authentication method to be used.
     * @param input  Authentication input.
     */
    fun authenticate(method: AuthAction, input: String) {
        when (method) {
            is AuthAction.ProvideAlphaNumeric -> method.authenticate(input)
            is AuthAction.ProvideNumeric -> method.authenticate(input.toUInt())
            is AuthAction.ProvideStaticKey -> method.authenticate(input.toByteArray())
            is AuthAction.DisplayAlphaNumeric, is AuthAction.DisplayNumber -> {
                // Do nothing
            }
        }
    }

    /**
     * Invoked when the provisioning process completes and navigates to the list of nodes.
     */
    @OptIn(ExperimentalUuidApi::class)
    internal fun onProvisioningComplete(uuid: Uuid) {
        disconnect()

        // Queue ConfigCompositionDataGet and ConfigDefaultTtl
        // When provisioning is complete we enqueue the next configuration tasks that can be resumed
        val messenger = repository.messengers.createMessenger(nodeUuid = uuid)
        messenger.enqueueTask(
            task = ConfigTask(
                icon = Icons.Outlined.DeviceHub,
                label = "Reading composition data",
                message = ConfigCompositionDataGet(page = 0x00u)
            )
        )
        originalNode?.let {
            messenger.enqueueReconfigurationWith(it)
        } ?: run {
            messenger.enqueueTask(
                task = ConfigTask(
                    icon = Icons.Outlined.Timer,
                    label = "Reading default TTL",
                    message = ConfigDefaultTtlGet()
                )
            )
        }
        repository.connect(uuid)
    }

    /**
     * Invoked when the provisioning process completes and navigates to the list of nodes.
     */
    internal fun onProvisioningFailed() {
        disconnect()
    }
}


/**
 * ProvisionerState represents the state of the provisioning process for the UI.
 */
sealed class ProvisionerState {
    data object Scanning : ProvisionerState()
    data class Connecting(val unprovisionedDevice: UnprovisionedDevice) : ProvisionerState()
    data class Connected(val unprovisionedDevice: UnprovisionedDevice) : ProvisionerState()
    data class Identifying(val unprovisionedDevice: UnprovisionedDevice) : ProvisionerState()
    data class Provisioning(
        val unprovisionedDevice: UnprovisionedDevice,
        val state: ProvisioningState,
    ) : ProvisionerState()

    data class Error(
        val unprovisionedDevice: UnprovisionedDevice,
        val throwable: Throwable,
    ) : ProvisionerState()

    data class Disconnected(val unprovisionedDevice: UnprovisionedDevice) : ProvisionerState()
}

internal data class ProvisioningScreenUiState(
    val networkKeys: List<NetworkKey> = emptyList(),
    val provisioningParameters: ProvisioningParameters? = null,
    val provisionerState: ProvisionerState,
    val developerSettings: DeveloperSettings = DeveloperSettings(),
)