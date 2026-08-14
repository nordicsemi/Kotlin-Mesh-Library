package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.firmwareDistributionServer
import no.nordicsemi.android.nrfmesh.core.data.NetworkConnectionState
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.Row
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.dfu.R
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionPhase
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppBind
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigSigModelAppGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigVendorModelAppGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionCapabilitiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionStatus
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId

@Composable
internal fun SmpContent(
    snackbarHostState: SnackbarHostState,
    messageState: MessageState,
    connectionState: NetworkConnectionState,
    node: Node?,
    name: String?,
    unicastAddress: UnicastAddress?,
    isSmpServiceSupported: Boolean?,
    isDistributorServerModelSupported: Boolean?,
    isLePairingSupported: Boolean?,
    onGattProxyClicked: () -> Unit,
    selectedKey: ApplicationKey?,
    onApplicationKeyClicked: (ApplicationKey) -> Unit,
    onBindAppKeysClicked: (Model) -> Unit,
    firmwareDistributionStatus: FirmwareDistributionStatus?,
    capabilitiesStatus: FirmwareDistributionCapabilitiesStatus?,
    send: suspend (Model, AcknowledgedMeshMessage) -> MeshMessage?,
    enableNextStage: () -> Unit,
) {
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = stringResource(R.string.label_dfu_over_smp_rationale),
        style = MaterialTheme.typography.bodySmall
    )
    GattProxy(
        connectionState = connectionState,
        name = name,
        unicastAddress = unicastAddress,
        onGattProxyClicked = onGattProxyClicked
    )
    DeviceManagement(
        connectionState = connectionState,
        isSmpServiceSupported = isSmpServiceSupported,
        isLePairingSupported = isLePairingSupported
    )
    FirmwareDistributor(
        connectionState = connectionState,
        isDistributorServerModelSupported = isDistributorServerModelSupported
    )
    if (isSmpServiceSupported != null && isDistributorServerModelSupported != null && isLePairingSupported != null) {
        if (!isSmpServiceSupported || !isDistributorServerModelSupported || !isLePairingSupported) {
            ReadMore()
        } else if (node != null) {
            BoundApplicationKeys(
                messageState = messageState,
                model = node.model(modelId = firmwareDistributionServer) ?: return,
                selectedKey = selectedKey,
                onBindAppKeyClicked = onBindAppKeysClicked,
                onApplicationKeyClicked = onApplicationKeyClicked,
                send = send
            )
            DistributorStatus(
                messageState = messageState,
                connectionState = connectionState,
                model = node.model(modelId = firmwareDistributionServer) ?: return,
                firmwareDistributionStatus = firmwareDistributionStatus,
                selectedKey = selectedKey,
                send = send
            )
            if (selectedKey != null && firmwareDistributionStatus?.phase == FirmwareDistributionPhase.IDLE) {
                CapabilitiesContent(
                    selectedKey = selectedKey,
                    phase = firmwareDistributionStatus.phase,
                    messageState = messageState,
                    capabilitiesStatus = capabilitiesStatus,
                    model = node.model(modelId = firmwareDistributionServer) ?: return,
                    send = send,
                    enableNextStage = enableNextStage
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GattProxy(
    connectionState: NetworkConnectionState,
    name: String?,
    unicastAddress: UnicastAddress?,
    onGattProxyClicked: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SectionTitle(
            modifier = Modifier.weight(weight = 1f),
            title = stringResource(R.string.label_gatt_proxy)
        )
        MeshIconButton(
            onClick = onGattProxyClicked,
            buttonIcon = Icons.Outlined.Bluetooth,
            isOnClickActionInProgress = connectionState is NetworkConnectionState.Connecting,
            enabled = connectionState !is NetworkConnectionState.Connecting
        )
    }
    ElevatedCardItem(
        imageVector = Icons.AutoMirrored.Outlined.Label,
        title = stringResource(R.string.label_name),
        subtitle = name ?: stringResource(R.string.label_na),
        titleAction = {
            if (connectionState is NetworkConnectionState.Connecting && name == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(size = 24.dp)
                )
            }
        }
    )
    ElevatedCardItem(
        imageVector = Icons.Outlined.Lan,
        title = stringResource(R.string.label_unicast_address),
        subtitle = unicastAddress?.toHexString() ?: stringResource(R.string.label_na),
        titleAction = {
            if (connectionState is NetworkConnectionState.Connecting && unicastAddress == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(size = 24.dp)
                )
            }
        }
    )
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = stringResource(R.string.label_smp_dfu_rationale),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun DeviceManagement(
    connectionState: NetworkConnectionState,
    isSmpServiceSupported: Boolean?,
    isLePairingSupported: Boolean?,
) {
    SectionTitle(title = stringResource(R.string.label_device_management))
    ElevatedCardItem(
        imageVector = Icons.AutoMirrored.Outlined.Label,
        title = stringResource(R.string.label_smp_service),
        subtitle = when (isSmpServiceSupported) {
            true -> stringResource(R.string.label_supported)
            false -> stringResource(R.string.label_not_supported)
            null -> stringResource(R.string.label_unknown)
        },
        titleAction = {
            when (connectionState) {
                // 1. Connection in progress, but SMP support is still unknown
                is NetworkConnectionState.Connecting -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(size = 24.dp)
                    )
                }
                // 2. Connected, but SMP support is not yet known
                is NetworkConnectionState.Connected if isSmpServiceSupported == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(size = 24.dp)
                    )
                }
                // 3. Connected and SMP is supported
                is NetworkConnectionState.Connected if isSmpServiceSupported == true -> {
                    Icon(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        imageVector = Icons.Outlined.CheckCircle,
                        tint = Color.Green,
                        contentDescription = null
                    )
                }
                // 4. Connected, but SMP is not supported
                is NetworkConnectionState.Connected if isSmpServiceSupported == false -> {
                    Icon(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        imageVector = Icons.Outlined.Close,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = null
                    )
                }

                else -> {}
            }
        }
    )
    ElevatedCardItem(
        imageVector = Icons.Outlined.Lan,
        title = stringResource(R.string.label_access),
        subtitle = when (isLePairingSupported) {
            true -> stringResource(R.string.label_secure)
            false -> stringResource(R.string.label_unsecure)
            null -> stringResource(R.string.label_unknown)
        },
        titleAction = {
            when (connectionState) {
                // 1. Connection in progress, but LE Pairing support is still unknown
                is NetworkConnectionState.Connecting -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(size = 24.dp)
                    )
                }
                // 2. Connected, but LE Pairing support is not yet known
                is NetworkConnectionState.Connected if isLePairingSupported == null -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(size = 24.dp)
                    )
                }
                // 3. Connected and LE Pairing is supported
                is NetworkConnectionState.Connected if isLePairingSupported == true -> {
                    Icon(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        imageVector = Icons.Outlined.CheckCircle,
                        tint = Color.Green,
                        contentDescription = null
                    )
                }
                // 4. Connected, but LE Pairing is not supported
                is NetworkConnectionState.Connected if isLePairingSupported == false -> {
                    Icon(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        imageVector = Icons.Outlined.Close,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = null
                    )
                }

                else -> {}
            }
        }
    )
}

@Composable
private fun FirmwareDistributor(
    connectionState: NetworkConnectionState,
    isDistributorServerModelSupported: Boolean?,
) {
    SectionTitle(title = stringResource(R.string.label_firmware_distributor))
    ElevatedCardItem(
        imageVector = Icons.AutoMirrored.Outlined.Label,
        title = stringResource(R.string.label_firmware_distributor_model),
        subtitle = when (isDistributorServerModelSupported) {
            true -> stringResource(R.string.label_found)
            false -> stringResource(R.string.label_not_found)
            else -> stringResource(R.string.label_unknown)
        },
        titleAction = {
            when (connectionState) {
                // 1. Connection in progress, but distribution server model support is still unknown
                is NetworkConnectionState.Connecting -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(size = 24.dp)
                    )
                }
                // 2. Connected, but distribution server model support is not yet known
                is NetworkConnectionState.Connected if isDistributorServerModelSupported == null ->
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(size = 24.dp)
                    )
                // 3. Connected and distribution server model is supported
                is NetworkConnectionState.Connected if isDistributorServerModelSupported == true ->
                    Icon(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        imageVector = Icons.Outlined.CheckCircle,
                        tint = Color.Green,
                        contentDescription = null
                    )
                // 4. Connected, but distribution server model is not supported
                is NetworkConnectionState.Connected if isDistributorServerModelSupported == false ->
                    Icon(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        imageVector = Icons.Outlined.Close,
                        tint = MaterialTheme.colorScheme.error,
                        contentDescription = null
                    )

                else -> {}
            }
        }
    )
}

@Composable
private fun ReadMore() {
    val uriHandler = LocalUriHandler.current
    val resources = LocalResources.current
    SectionTitle(title = stringResource(R.string.label_read_more))
    ElevatedCardItem(
        imageVector = Icons.AutoMirrored.Outlined.Label,
        title = stringResource(R.string.label_device_firmware_update),
        subtitle = stringResource(R.string.label_device_firmware_update_link),
        titleAction = {
            MeshIconButton(
                onClick = dropUnlessResumed {
                    uriHandler.openUri(
                        uri = resources.getString(R.string.label_device_firmware_update_link)
                    )
                },
                buttonIcon = Icons.AutoMirrored.Outlined.OpenInNew,
            )
        }
    )
    ElevatedCardItem(
        imageVector = Icons.AutoMirrored.Outlined.Label,
        title = stringResource(R.string.label_dfu_over_bluetooth_mesh),
        subtitle = stringResource(R.string.label_dfu_over_bluetooth_mesh_link),
        titleAction = {
            MeshIconButton(
                onClick = dropUnlessResumed {
                    uriHandler.openUri(
                        uri = resources.getString(R.string.label_dfu_over_bluetooth_mesh_link)
                    )
                },
                buttonIcon = Icons.AutoMirrored.Outlined.OpenInNew,
            )
        }
    )
    ElevatedCardItem(
        imageVector = Icons.AutoMirrored.Outlined.Label,
        title = stringResource(R.string.label_sample_distributor),
        subtitle = stringResource(R.string.label_sample_distributor_link),
        titleAction = {
            MeshIconButton(
                onClick = dropUnlessResumed {
                    uriHandler.openUri(
                        uri = resources.getString(R.string.label_sample_distributor_link)
                    )
                },
                buttonIcon = Icons.AutoMirrored.Outlined.OpenInNew,
            )
        }
    )
    ElevatedCardItem(
        imageVector = Icons.AutoMirrored.Outlined.Label,
        title = stringResource(R.string.label_device_management_smp),
        subtitle = stringResource(R.string.label_device_management_smp_link),
        titleAction = {
            MeshIconButton(
                onClick = dropUnlessResumed {
                    uriHandler.openUri(
                        uri = resources.getString(R.string.label_device_management_smp_link)
                    )
                },
                buttonIcon = Icons.AutoMirrored.Outlined.OpenInNew,
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoundApplicationKeys(
    messageState: MessageState,
    model: Model,
    selectedKey: ApplicationKey?,
    onBindAppKeyClicked: (Model) -> Unit,
    onApplicationKeyClicked: (ApplicationKey) -> Unit,
    send: suspend (Model, AcknowledgedMeshMessage) -> MeshMessage?,
) {
    val scope = rememberCoroutineScope()
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SectionTitle(
            modifier = Modifier.weight(weight = 1f),
            title = stringResource(R.string.label_application_key)
        )
        MeshIconButton(
            onClick = dropUnlessResumed {
                scope.launch {
                    val _ = runCatching {
                        val _ = send(
                            model,
                            when (model.modelId) {
                                is SigModelId -> {
                                    ConfigSigModelAppGet(
                                        elementAddress = model.parentElement?.unicastAddress
                                            ?: throw IllegalStateException("Model should have a parent element with a unicast address"),
                                        modelId = model.modelId as SigModelId
                                    )
                                }

                                is VendorModelId -> ConfigVendorModelAppGet(
                                    elementAddress = model.parentElement?.unicastAddress
                                        ?: throw IllegalStateException("Model should have a parent element with a unicast address"),
                                    modelId = model.modelId as VendorModelId
                                )
                            }
                        )
                    }
                }
            },
            buttonIcon = Icons.Outlined.Refresh,
            enabled = !messageState.isInProgress(),
            isOnClickActionInProgress = messageState.isInProgress() &&
                    (messageState.message is ConfigSigModelAppGet ||
                            messageState.message is ConfigVendorModelAppGet)
        )
        MeshIconButton(
            onClick = dropUnlessResumed { onBindAppKeyClicked(model) },
            buttonIcon = Icons.Outlined.Link,
            enabled = !messageState.isInProgress(),
            isOnClickActionInProgress = messageState.isInProgress() &&
                    (messageState.message is ConfigModelAppBind)
        )
    }
    model
        .boundApplicationKeys
        .takeIf { it.isNotEmpty() }
        ?.forEach { key ->
            key(key.index.toInt() + 1) {
                key.Row(
                    onClick = { onApplicationKeyClicked(key) },
                    titleAction = {
                        if (selectedKey?.index == key.index) {
                            Icon(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                imageVector = Icons.Outlined.CheckCircle,
                                tint = Color.Green,
                                contentDescription = null
                            )
                        }
                    }
                )
            }
        }
        ?: run {
            ElevatedCardItem(
                imageVector = Icons.Outlined.VpnKey,
                title = stringResource(R.string.label_no_app_keys_bound),
                titleAction = {
                    Icon(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        imageVector = Icons.Rounded.WarningAmber,
                        tint = Color(red = 1f, green = 0.6f, blue = 0f),
                        contentDescription = null
                    )
                }
            )
        }
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = stringResource(R.string.label_app_key_selection_rationale),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun DistributorStatus(
    messageState: MessageState,
    connectionState: NetworkConnectionState,
    model: Model,
    firmwareDistributionStatus: FirmwareDistributionStatus?,
    selectedKey: ApplicationKey?,
    send: suspend (Model, AcknowledgedMeshMessage) -> MeshMessage?,
) {
    val scope = rememberCoroutineScope()
    var distributionStatus by remember { mutableStateOf(firmwareDistributionStatus) }
    var error by rememberSaveable { mutableStateOf<Throwable?>(null) }
    //var phaseValue by remember(key1 = phase) { mutableStateOf(phase) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(
            modifier = Modifier.weight(weight = 1f),
            title = stringResource(R.string.label_distributor_status)
        )
        MeshIconButton(
            buttonIcon = Icons.Outlined.Refresh,
            onClick = dropUnlessResumed {
                scope.launch {
                    try {
                        distributionStatus = send(
                            model,
                            FirmwareDistributionGet()
                        ) as? FirmwareDistributionStatus
                        //phaseValue = distributionStatus?.phase
                    } catch (e: Exception) {
                        error = e
                    }
                }
            },
            enabled = !messageState.isInProgress(),
            isOnClickActionInProgress = messageState.isInProgress() &&
                    messageState.message is FirmwareDistributionGet
        )
    }
    ElevatedCardItem(
        imageVector = Icons.Outlined.Checklist,
        title = stringResource(R.string.label_phase),
        subtitle = distributionStatus?.phase?.debugDescription ?: stringResource(R.string.label_na),
        titleAction = {
            when (connectionState) {
                // 1. Connection in progress, but distribution server model support is still unknown
                is NetworkConnectionState.Connecting -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(size = 24.dp)
                    )
                }
                // 2. Connected, but distribution server model support is not yet known
                is NetworkConnectionState.Connected -> {
                    /*if (distributionStatus?.phase == null) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(size = 24.dp)
                        )
                    }*/
                    if (selectedKey == null) {
                        Icon(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            imageVector = Icons.Outlined.Close,
                            tint = MaterialTheme.colorScheme.error,
                            contentDescription = null
                        )
                    } else if(distributionStatus?.phase == null) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(size = 24.dp)
                        )
                    } else if (distributionStatus?.phase == FirmwareDistributionPhase.IDLE) {
                        Icon(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            imageVector = Icons.Outlined.CheckCircle,
                            tint = Color.Green,
                            contentDescription = null
                        )
                    } else {
                        Icon(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            imageVector = Icons.Outlined.Close,
                            tint = MaterialTheme.colorScheme.error,
                            contentDescription = null
                        )
                    }
                }

                else -> {}
            }
        }
    )
    LaunchedEffect(selectedKey) {
        if (selectedKey != null && distributionStatus == null) {
            try {
                distributionStatus = send(
                    model,
                    FirmwareDistributionGet()
                ) as? FirmwareDistributionStatus
                //phaseValue = distributionStatus?.phase
            } catch (e: Exception) {
                error = e
            }
        }
    }
}
