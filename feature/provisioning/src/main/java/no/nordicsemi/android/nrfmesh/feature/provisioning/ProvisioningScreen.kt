@file:OptIn(ExperimentalMaterial3Api::class)

package no.nordicsemi.android.nrfmesh.feature.provisioning

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.SyncLock
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.SentimentVeryDissatisfied
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import no.nordicsemi.android.common.scanner.rememberFilterState
import no.nordicsemi.android.common.scanner.view.DeviceListItem
import no.nordicsemi.android.common.scanner.view.ScannerView
import no.nordicsemi.android.common.theme.nordicGreen
import no.nordicsemi.android.common.theme.nordicSun
import no.nordicsemi.android.nrfmesh.core.common.Utils.describe
import no.nordicsemi.android.nrfmesh.core.ui.MeshAlertDialog
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedButton
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.provisioning.ProvisionerState.Error
import no.nordicsemi.kotlin.ble.client.android.ScanResult
import no.nordicsemi.kotlin.mesh.bearer.gatt.utils.MeshProvisioningService
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.provisioning.AuthAction
import no.nordicsemi.kotlin.mesh.provisioning.AuthenticationMethod
import no.nordicsemi.kotlin.mesh.provisioning.ProvisioningParameters
import no.nordicsemi.kotlin.mesh.provisioning.ProvisioningState
import no.nordicsemi.kotlin.mesh.provisioning.UnprovisionedDevice
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
internal fun ProvisioningScreen(
    snackbarHostState: SnackbarHostState,
    uiState: ProvisioningScreenUiState,
    beginProvisioning: (shouldReconfigure: Boolean) -> Unit,
    onNameChanged: (String) -> Unit,
    onAddressChanged: (Address) -> Unit,
    isValidAddress: (Address) -> Boolean,
    onNetworkKeyClicked: (NetworkKey) -> Unit,
    onAuthenticationMethodSelected: (AuthenticationMethod) -> Unit,
    authenticate: (AuthAction, String) -> Unit,
    onProvisioningComplete: (Uuid) -> Unit,
    onProvisioningFailed: () -> Unit,
    disconnect: () -> Unit,
    onBackPressed: () -> Unit,
    onScanResultSelected: (ScanResult) -> Boolean,
) {
    val scope = rememberCoroutineScope()
    var openDeviceCapabilitiesSheet by rememberSaveable { mutableStateOf(false) }
    val capabilitiesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAuthenticationBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showReprovisionDialog by rememberSaveable { mutableStateOf(false) }
    ScannerView(
        modifier = Modifier.padding(horizontal = 16.dp),
        state = rememberFilterState(filter = { ServiceUuid(uuid = MeshProvisioningService.uuid) }),
        onScanningStateChanged = { println("AAA did scanning state changed?: $it") },
        deviceItem = { scanResult ->
            runCatching {
                UnprovisionedDevice.from(advertisementData = scanResult.advertisingData.raw)
            }.onSuccess { device ->
                DeviceListItem(
                    iconPainter = rememberVectorPainter(Icons.Outlined.Bluetooth),
                    title = when {
                        scanResult.advertisingData.name.isNullOrEmpty() -> device.name
                        else -> scanResult.advertisingData.name
                            ?: stringResource(R.string.label_unknown_device)
                    },
                    subtitle = device.uuid.toString().uppercase()
                )
            }
        },
        onScanResultSelected = { scanResult ->
            val isDeviceAlreadyProvisioned = onScanResultSelected(scanResult)
            if (isDeviceAlreadyProvisioned && !uiState.developerSettings.alwaysReconfigure) {
                showReprovisionDialog = true
            } else {
                beginProvisioning(uiState.developerSettings.alwaysReconfigure)
                openDeviceCapabilitiesSheet = true
            }
        }
    )
    if (showReprovisionDialog) {
        MeshAlertDialog(
            icon = Icons.Outlined.GroupWork,
            iconColor = nordicSun,
            title = stringResource(R.string.label_warning),
            text = stringResource(R.string.label_warning_provisioning_rationale),
            onDismissRequest = { showReprovisionDialog = !showReprovisionDialog },
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    TextButton(
                        onClick = {
                            beginProvisioning(true)
                            showReprovisionDialog = !showReprovisionDialog
                            openDeviceCapabilitiesSheet = true
                        },
                        content = { Text(text = stringResource(R.string.label_reprovision_configure)) }
                    )
                    TextButton(
                        onClick = {
                            beginProvisioning(false)
                            showReprovisionDialog = !showReprovisionDialog
                            openDeviceCapabilitiesSheet = true
                        },
                        content = { Text(text = stringResource(R.string.label_reprovision_as_new_device)) }
                    )
                    TextButton(
                        onClick = { showReprovisionDialog = !showReprovisionDialog },
                        content = { Text(text = stringResource(R.string.label_cancel)) }
                    )
                }
            }
        )
    }
    if (openDeviceCapabilitiesSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                disconnect()
                openDeviceCapabilitiesSheet = !openDeviceCapabilitiesSheet
            },
            sheetState = capabilitiesSheetState,
            contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Top) },
            sheetGesturesEnabled = false,
            properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        disconnect()
                        onBackPressed()
                        openDeviceCapabilitiesSheet = !openDeviceCapabilitiesSheet
                    },
                    content = {
                        Icon(imageVector = Icons.Outlined.Close, contentDescription = null)
                    }
                )
                SectionTitle(
                    modifier = Modifier
                        .weight(weight = 1f)
                        .padding(horizontal = 16.dp),
                    title = stringResource(R.string.label_device_information),
                    style = MaterialTheme.typography.titleMedium
                )
                MeshOutlinedButton(
                    modifier = Modifier.padding(end = 16.dp),
                    enabled = uiState.provisionerState is ProvisionerState.Provisioning && !uiState.developerSettings.quickProvisioning,
                    onClick = { showAuthenticationBottomSheet = !showAuthenticationBottomSheet },
                    buttonIcon = Icons.Outlined.SyncLock,
                    buttonIconTint = nordicGreen,
                    text = stringResource(id = R.string.label_provision),
                    textColor = nordicGreen,
                    border = BorderStroke(width = 1.dp, color = nordicGreen)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 16.dp)
                    .verticalScroll(state = rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(
                    space = 8.dp,
                    alignment = Alignment.CenterVertically
                )
            ) {
                ProvisioningContent(
                    provisionerState = uiState.provisionerState,
                    networkKeys = uiState.networkKeys,
                    parameters = uiState.provisioningParameters,
                    snackbarHostState = snackbarHostState,
                    showAuthenticationBottomSheet = showAuthenticationBottomSheet,
                    onAuthenticationBottomSheetDismissed = {
                        showAuthenticationBottomSheet = false
                    },
                    onNameChanged = onNameChanged,
                    onAddressChanged = onAddressChanged,
                    isValidAddress = isValidAddress,
                    onNetworkKeyClicked = onNetworkKeyClicked,
                    onAuthenticationMethodSelected = onAuthenticationMethodSelected,
                    authenticate = authenticate,
                    onProvisioningComplete = onProvisioningComplete,
                    onProvisioningFailed = onProvisioningFailed,
                    dismissCapabilitiesSheet = { scope.launch { capabilitiesSheetState.hide() } }
                )
            }
        }
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun ProvisioningContent(
    provisionerState: ProvisionerState,
    networkKeys: List<NetworkKey>,
    parameters: ProvisioningParameters?,
    snackbarHostState: SnackbarHostState,
    showAuthenticationBottomSheet: Boolean,
    onAuthenticationBottomSheetDismissed: (Boolean) -> Unit,
    onNameChanged: (String) -> Unit,
    onAddressChanged: (Address) -> Unit,
    isValidAddress: (Address) -> Boolean,
    onNetworkKeyClicked: (NetworkKey) -> Unit,
    onAuthenticationMethodSelected: (AuthenticationMethod) -> Unit,
    authenticate: (AuthAction, String) -> Unit,
    onProvisioningComplete: (Uuid) -> Unit,
    onProvisioningFailed: () -> Unit,
    dismissCapabilitiesSheet: () -> Unit,
) {
    when (provisionerState) {
        is ProvisionerState.Connecting -> ProvisionerStateInfo(
            text = stringResource(
                R.string.label_connecting_to,
                provisionerState.unprovisionedDevice.name
            )
        )

        is ProvisionerState.Connected -> ProvisionerStateInfo(
            text = stringResource(
                R.string.label_connected,
                provisionerState.unprovisionedDevice.name
            )
        )

        is ProvisionerState.Identifying -> ProvisionerStateInfo(
            text = stringResource(
                R.string.label_identifying,
                provisionerState.unprovisionedDevice.name
            )
        )

        is ProvisionerState.Provisioning -> ProvisioningStateInfo(
            state = provisionerState.state,
            networkKeys = networkKeys,
            parameters = parameters,
            unprovisionedDevice = provisionerState.unprovisionedDevice,
            snackbarHostState = snackbarHostState,
            showAuthenticationBottomSheet = showAuthenticationBottomSheet,
            onAuthenticationBottomSheetDismissed = onAuthenticationBottomSheetDismissed,
            onNameChanged = onNameChanged,
            onAddressChanged = onAddressChanged,
            isValidAddress = isValidAddress,
            onNetworkKeyClicked = onNetworkKeyClicked,
            authenticate = authenticate,
            onProvisioningComplete = onProvisioningComplete,
            onProvisioningFailed = onProvisioningFailed,
            onInputComplete = { },
            onAuthenticationMethodSelected = onAuthenticationMethodSelected,
            dismissCapabilitiesSheet = dismissCapabilitiesSheet
        )

        is Error -> {
            dismissCapabilitiesSheet()
            var showAlertDialog by remember { mutableStateOf(true) }
            if (showAlertDialog) {
                MeshAlertDialog(
                    onDismissRequest = {
                        showAlertDialog = !showAlertDialog
                        onProvisioningFailed()
                    },
                    confirmButtonText = stringResource(id = R.string.label_ok),
                    onConfirmClick = {
                        showAlertDialog = !showAlertDialog
                        onProvisioningFailed()
                    },
                    dismissButtonText = null,
                    icon = Icons.Rounded.ErrorOutline,
                    iconColor = MaterialTheme.colorScheme.error,
                    title = stringResource(R.string.label_status),
                    text = "Error while provisioning: ${provisionerState.throwable.describe()}"
                )
            }
        }

        is ProvisionerState.Disconnected -> ProvisionerStateInfo(
            text = stringResource(
                R.string.label_disconnected,
                provisionerState.unprovisionedDevice.name
            ),
            isError = true,
            imageVector = Icons.Rounded.SentimentVeryDissatisfied
        )

        else -> {}
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun ProvisioningStateInfo(
    state: ProvisioningState,
    networkKeys: List<NetworkKey>,
    parameters: ProvisioningParameters?,
    unprovisionedDevice: UnprovisionedDevice,
    snackbarHostState: SnackbarHostState,
    showAuthenticationBottomSheet: Boolean,
    onAuthenticationBottomSheetDismissed: (Boolean) -> Unit,
    onNameChanged: (String) -> Unit,
    onAddressChanged: (Address) -> Unit,
    isValidAddress: (Address) -> Boolean,
    onNetworkKeyClicked: (NetworkKey) -> Unit,
    authenticate: (AuthAction, String) -> Unit,
    onProvisioningComplete: (Uuid) -> Unit,
    onProvisioningFailed: () -> Unit,
    onInputComplete: () -> Unit,
    onAuthenticationMethodSelected: (AuthenticationMethod) -> Unit,
    dismissCapabilitiesSheet: () -> Unit,
) {
    when (state) {
        is ProvisioningState.RequestingCapabilities -> ProvisionerStateInfo(
            text = stringResource(id = R.string.label_provisioning_requesting_capabilities)
        )

        is ProvisioningState.CapabilitiesReceived -> DeviceCapabilities(
            capabilities = state.capabilities,
            networkKeys = networkKeys,
            parameters = parameters ?: state.defaultParameters,
            snackbarHostState = snackbarHostState,
            unprovisionedDevice = unprovisionedDevice,
            showAuthenticationBottomSheet = showAuthenticationBottomSheet,
            onAuthenticationBottomSheetDismissed = onAuthenticationBottomSheetDismissed,
            onNameChanged = onNameChanged,
            onAddressChanged = onAddressChanged,
            isValidAddress = isValidAddress,
            onNetworkKeyClicked = onNetworkKeyClicked,
            onAuthenticationMethodSelected = onAuthenticationMethodSelected
        )

        is ProvisioningState.Provisioning -> ProvisionerStateInfo(
            text = stringResource(R.string.provisioning_in_progress)
        )

        is ProvisioningState.AuthActionRequired -> {
            ProvisionerStateInfo(
                text = stringResource(R.string.label_provisioning_authentication_required)
            )
            AuthenticationDialog(
                action = state.action,
                onOkClicked = authenticate,
                onCancelClicked = onProvisioningFailed
            )
        }

        ProvisioningState.InputComplete -> {
            ProvisionerStateInfo(text = stringResource(R.string.label_provisioning_authentication_completed))
            onInputComplete()
        }

        is ProvisioningState.Failed -> {
            var showAlertDialog by remember { mutableStateOf(true) }
            if (showAlertDialog) {
                dismissCapabilitiesSheet()
                MeshAlertDialog(
                    onDismissRequest = {
                        showAlertDialog = !showAlertDialog
                        onProvisioningFailed()
                    },
                    confirmButtonText = stringResource(id = R.string.label_ok),
                    onConfirmClick = {
                        showAlertDialog = !showAlertDialog
                        onProvisioningFailed()
                    },
                    dismissButtonText = null,
                    icon = Icons.Rounded.ErrorOutline,
                    iconColor = MaterialTheme.colorScheme.error,
                    title = stringResource(R.string.label_status),
                    text = stringResource(R.string.label_provisioning_failed, state.error)
                )
            }
        }

        is ProvisioningState.Complete -> {
            var showAlertDialog by remember { mutableStateOf(true) }
            if (showAlertDialog) {
                dismissCapabilitiesSheet()
                MeshAlertDialog(
                    onDismissRequest = {
                        showAlertDialog = !showAlertDialog
                        onProvisioningComplete(unprovisionedDevice.uuid)
                    },
                    confirmButtonText = stringResource(id = R.string.label_ok),
                    onConfirmClick = {
                        showAlertDialog = !showAlertDialog
                        onProvisioningComplete(unprovisionedDevice.uuid)
                    },
                    dismissButtonText = null,
                    icon = Icons.Rounded.CheckCircleOutline,
                    title = stringResource(R.string.label_status),
                    text = stringResource(R.string.label_provisioning_completed)
                )
            }
        }
    }
}

@Composable
private fun ProvisionerStateInfo(
    text: String,
    shouldShowProgress: Boolean = true,
    isError: Boolean = false,
    imageVector: ImageVector = Icons.Rounded.Error,
    errorTint: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (shouldShowProgress && !isError) {
            CircularProgressIndicator()
        }
        if (isError) {
            Icon(
                modifier = Modifier.size(100.dp),
                imageVector = imageVector,
                contentDescription = null,
                tint = errorTint
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        Text(text = text)
    }
}

