package no.nordicsemi.android.nrfmesh.feature.proxy

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.WavingHand
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import no.nordicsemi.android.common.permissions.ble.RequireBluetooth
import no.nordicsemi.android.common.permissions.ble.RequireLocation
import no.nordicsemi.android.common.scanner.R.drawable
import no.nordicsemi.android.common.scanner.rememberFilterState
import no.nordicsemi.android.common.scanner.view.DeviceListItem
import no.nordicsemi.android.common.scanner.view.ScannerView
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.name
import no.nordicsemi.android.nrfmesh.core.data.NetworkConnectionState
import no.nordicsemi.android.nrfmesh.core.data.ProxyConnectionState
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshMessageStatusDialog
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshTwoLineListItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.core.ui.isCompactWidth
import no.nordicsemi.kotlin.ble.client.android.ScanResult
import no.nordicsemi.kotlin.mesh.bearer.gatt.utils.MeshProxyService
import no.nordicsemi.kotlin.mesh.core.ProxyFilterType
import no.nordicsemi.kotlin.mesh.core.messages.proxy.AddAddressesToFilter
import no.nordicsemi.kotlin.mesh.core.messages.proxy.ProxyConfigurationMessage
import no.nordicsemi.kotlin.mesh.core.messages.proxy.RemoveAddressesFromFilter
import no.nordicsemi.kotlin.mesh.core.messages.proxy.SetFilterType
import no.nordicsemi.kotlin.mesh.core.model.FixedGroupAddress
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.ProxyFilterAddress
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import no.nordicsemi.kotlin.mesh.core.model.element
import no.nordicsemi.kotlin.mesh.core.model.fixedGroupAddresses
import no.nordicsemi.kotlin.mesh.core.util.networkIdentity
import no.nordicsemi.kotlin.mesh.core.util.nodeIdentity
import kotlin.uuid.ExperimentalUuidApi

@Composable
internal fun ProxyScreen(
    uiState: ProxyScreenUiState,
    onBluetoothEnabled: (Boolean) -> Unit,
    onLocationEnabled: (Boolean) -> Unit,
    onAutoConnectToggled: (Boolean) -> Unit,
    onScanResultSelected: (ScanResult) -> Unit,
    onDisconnectClicked: () -> Unit,
    send: (ProxyConfigurationMessage) -> Unit,
    resetMessageState: () -> Unit,
) {
    when (uiState.meshNetworkState) {
        is MeshNetworkState.Success -> ProxyContent(
            network = uiState.meshNetworkState.network,
            proxyConnectionState = uiState.proxyConnectionState,
            addresses = uiState.addresses,
            filterType = uiState.filterType,
            isProxyLimitReached = uiState.isProxyLimitReached,
            messageState = uiState.messageState,
            onBluetoothEnabled = onBluetoothEnabled,
            onLocationEnabled = onLocationEnabled,
            onAutoConnectToggled = onAutoConnectToggled,
            onScanResultSelected = onScanResultSelected,
            onDisconnectClicked = onDisconnectClicked,
            send = send,
            resetMessageState = resetMessageState
        )

        else -> {

        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProxyContent(
    network: MeshNetwork,
    proxyConnectionState: ProxyConnectionState,
    addresses: List<ProxyFilterAddress>,
    filterType: ProxyFilterType?,
    isProxyLimitReached: Boolean,
    messageState: MessageState,
    onBluetoothEnabled: (Boolean) -> Unit,
    onLocationEnabled: (Boolean) -> Unit,
    onAutoConnectToggled: (Boolean) -> Unit,
    onScanResultSelected: (ScanResult) -> Unit,
    onDisconnectClicked: () -> Unit,
    send: (ProxyConfigurationMessage) -> Unit,
    resetMessageState: () -> Unit,
) {
    RequireBluetooth(onChanged = onBluetoothEnabled) {
        RequireLocation(onChanged = onLocationEnabled) {
            Column(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .verticalScroll(state = rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProxyFilterInfo(
                    nodes = network.nodes,
                    networkKeys = network.networkKeys,
                    proxyConnectionState = proxyConnectionState,
                    onAutoConnectToggled = onAutoConnectToggled,
                    onScanResultSelected = onScanResultSelected,
                    onDisconnectClicked = onDisconnectClicked
                )
                FilterSection(
                    network = network,
                    type = filterType,
                    addresses = addresses,
                    isConnected = proxyConnectionState.connectionState is NetworkConnectionState.Connected,
                    limitReached = isProxyLimitReached,
                    send = send,
                    messageState = messageState,
                    resetMessageState = resetMessageState
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
private fun ProxyFilterInfo(
    nodes: List<Node>,
    networkKeys: List<NetworkKey>,
    proxyConnectionState: ProxyConnectionState,
    onAutoConnectToggled: (Boolean) -> Unit,
    onDisconnectClicked: () -> Unit,
    onScanResultSelected: (ScanResult) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showProxyScannerSheet by rememberSaveable { mutableStateOf(false) }
    val proxyScannerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    AutomaticConnectionRow(
        proxyConnectionState = proxyConnectionState,
        onAutoConnectToggled = onAutoConnectToggled,
        onConnectClicked = { showProxyScannerSheet = true },
        onDisconnectClicked = onDisconnectClicked
    )
    if (showProxyScannerSheet)
        ModalBottomSheet(
            onDismissRequest = { showProxyScannerSheet = false },
            sheetState = proxyScannerSheetState
        ) {
            SectionTitle(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(R.string.label_proxies),
                style = MaterialTheme.typography.titleMedium,
            )
            ScannerView(
                modifier = Modifier.padding(horizontal = 16.dp),
                state = rememberFilterState(filter = { ServiceUuid(uuid = MeshProxyService.uuid) }),
                onScanningStateChanged = {},
                deviceItem = { scanResult ->
                    scanResult.advertisingData.serviceData[MeshProxyService.uuid]
                        ?.takeIf { it.isNotEmpty() }
                        ?.run {
                            nodeIdentity()?.matches(nodes = nodes)?.let {
                                DeviceListItem(
                                    iconPainter = rememberVectorPainter(Icons.Outlined.WavingHand),
                                    title = it.name,
                                    subtitle = it.primaryUnicastAddress.address.toHexString(
                                        format = HexFormat {
                                            number.prefix = "Address: 0x"
                                            upperCase = true
                                        }
                                    )
                                )
                            } ?: run {
                                networkIdentity()?.matches(networkKeys = networkKeys)
                                    ?.let { netKey ->
                                        DeviceListItem(
                                            iconPainter = painterResource(drawable.ic_mesh),
                                            title = scanResult.advertisingData.name
                                                ?: scanResult.peripheral.name
                                                ?: stringResource(R.string.label_unknown_device),
                                            subtitle = netKey.name
                                        )
                                    }
                            }
                        }
                },
                onScanResultSelected = { result ->
                    onScanResultSelected(result)
                    scope.launch {
                        proxyScannerSheetState.hide()
                    }.invokeOnCompletion {
                        if (!proxyScannerSheetState.isVisible) {
                            showProxyScannerSheet = false
                        }
                    }
                }
            )
        }
}

@Composable
private fun AutomaticConnectionRow(
    proxyConnectionState: ProxyConnectionState,
    onAutoConnectToggled: (Boolean) -> Unit,
    onConnectClicked: () -> Unit,
    onDisconnectClicked: () -> Unit,
) {
    ElevatedCardItem(
        modifier = Modifier
            .padding(top = 8.dp)
            .padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.AutoAwesome,
        title = stringResource(R.string.label_automatic_connection),
        titleAction = {
            Switch(
                modifier = Modifier.padding(horizontal = 16.dp),
                checked = proxyConnectionState.autoConnect,
                onCheckedChange = { onAutoConnectToggled(it) }
            )
        },
        supportingText = stringResource(R.string.label_automatic_connection_rationale),
        body = {
            Text(
                modifier = Modifier.padding(start = 42.dp),
                text = proxyConnectionState.connectionState.describe(),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        actions = {
            OutlinedButton(
                onClick = onConnectClicked,
                enabled = !proxyConnectionState.autoConnect,
                content = { Text(text = stringResource(R.string.label_connect)) }
            )
            Spacer(modifier = Modifier.size(16.dp))
            OutlinedButton(
                onClick = {
                    onAutoConnectToggled(false)
                    onDisconnectClicked()
                },
                enabled = proxyConnectionState.connectionState is NetworkConnectionState.Connected,
                content = { Text(text = stringResource(R.string.label_disconnect)) }
            )
        }
    )
}

@Composable
private fun NetworkConnectionState.describe() = when (this) {
    NetworkConnectionState.Scanning -> stringResource(R.string.label_scanning)
    is NetworkConnectionState.Connecting -> stringResource(
        R.string.label_connecting_to, name ?: R.string.label_unknown
    )

    is NetworkConnectionState.Connected -> stringResource(
        R.string.label_connected_to, name ?: R.string.label_unknown
    )

    NetworkConnectionState.Disconnected -> stringResource(R.string.label_proxy_disconnected)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterSection(
    network: MeshNetwork,
    type: ProxyFilterType?,
    addresses: List<ProxyFilterAddress> = emptyList(),
    limitReached: Boolean,
    isConnected: Boolean,
    send: (ProxyConfigurationMessage) -> Unit,
    messageState: MessageState,
    resetMessageState: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
    val options = listOf(ProxyFilterType.ACCEPT_LIST, ProxyFilterType.REJECT_LIST)
    var selectedIndex by remember {
        mutableIntStateOf(if (type == null) 0 else options.indexOf(type))
    }
    var showBottomSheet by remember { mutableStateOf(false) }
    if (isCompactWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(vertical = 8.dp),
        ) {
            SectionTitle(
                modifier = Modifier.padding(end = 16.dp),
                title = stringResource(R.string.label_proxy_filter)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (isConnected) {
                    MeshIconButton(
                        isOnClickActionInProgress = messageState.isInProgress()
                                && messageState.message is AddAddressesToFilter,
                        buttonIcon = Icons.Outlined.Add,
                        onClick = { showBottomSheet = true },
                        enabled = !messageState.isInProgress()
                    )
                }
                SingleChoiceSegmentedButtonRow {
                    options.forEachIndexed { index, label ->
                        SegmentedButton(
                            modifier = Modifier.defaultMinSize(minWidth = 150.dp),
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = options.size
                            ),
                            colors = SegmentedButtonDefaults.colors(
                                disabledActiveContainerColor = MaterialTheme.colorScheme.primary.copy(
                                    alpha = 0.3f
                                )
                            ),
                            onClick = {
                                selectedIndex = index
                                send(SetFilterType(options[selectedIndex]))
                            },
                            selected = index == selectedIndex,
                            label = { Text(text = label.toString()) },
                            enabled = isConnected
                        )
                    }
                }
            }
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle(
                modifier = Modifier
                    .weight(weight = 1f)
                    .padding(end = 16.dp),
                title = stringResource(R.string.label_proxy_filter)
            )
            MeshOutlinedButton(
                isOnClickActionInProgress = messageState.isInProgress()
                        && messageState.message is AddAddressesToFilter,
                buttonIcon = Icons.Outlined.Add,
                text = stringResource(R.string.label_add_address),
                onClick = { showBottomSheet = true },
                enabled = isConnected && !messageState.isInProgress()
            )
            SingleChoiceSegmentedButtonRow {
                options.forEachIndexed { index, label ->
                    SegmentedButton(
                        modifier = Modifier.defaultMinSize(minWidth = 150.dp),
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = options.size
                        ),
                        colors = SegmentedButtonDefaults.colors(
                            disabledActiveContainerColor = MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.3f
                            )
                        ),
                        onClick = {
                            selectedIndex = index
                            send(SetFilterType(options[selectedIndex]))
                        },
                        selected = index == selectedIndex,
                        label = { Text(text = label.toString()) },
                        enabled = isConnected
                    )
                }
            }
        }
    }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        addresses.forEach { address ->
            key(address.toHexString()) {
                SwipeToDismissAddress(
                    network = network,
                    address = address,
                    onSwiped = { proxyFilterAddress ->
                        send(RemoveAddressesFromFilter(addresses = listOf(proxyFilterAddress)))
                    }
                )
            }
        }
    }

    if (limitReached) {
        MeshMessageStatusDialog(
            text = stringResource(R.string.label_proxy_filter_limit_reached),
            showDismissButton = true,
            onDismissRequest = resetMessageState
        )
    }
    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            content = {
                Addresses(
                    nodes = network.nodes,
                    groups = network.groups,
                    onAddressClicked = {
                        send(AddAddressesToFilter(addresses = listOf(it)))
                        scope.launch {
                            sheetState.hide()
                        }.invokeOnCompletion {
                            if (!sheetState.isVisible) {
                                showBottomSheet = false
                            }
                        }
                    }
                )
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissAddress(
    network: MeshNetwork,
    address: ProxyFilterAddress,
    onSwiped: (ProxyFilterAddress) -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        modifier = Modifier.padding(horizontal = 16.dp),
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.Settled,
                    SwipeToDismissBoxValue.StartToEnd,
                    SwipeToDismissBoxValue.EndToStart,
                        -> Color.Red
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = color, shape = CardDefaults.elevatedShape)
                    .padding(horizontal = 16.dp),
                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                    Alignment.CenterStart
                else Alignment.CenterEnd
            ) {
                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "null")
            }
        },
        onDismiss = { onSwiped(address) },
        content = {
            AddressRow(
                name = address.title(nodes = network.nodes, groups = network.groups),
                subtitle = address.subtitle(network = network)
            )
        }
    )
}

@Composable
private fun Addresses(
    nodes: List<Node>,
    groups: List<Group>,
    onAddressClicked: ((ProxyFilterAddress) -> Unit),
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(R.string.label_elements)
        )
        nodes.forEach { element ->
            ExpandableAddressRow(
                node = element,
                onClick = onAddressClicked
            )
        }
        if (groups.isNotEmpty()) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = stringResource(R.string.label_groups)
            )
        }
        groups.forEach { group ->
            AddressRow(
                name = group.name,
                onClick = { onAddressClicked(group.address as ProxyFilterAddress) }
            )
        }
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(R.string.label_fixed_group_addresses)
        )
        fixedGroupAddresses.forEach { destination ->
            AddressRow(
                name = destination.name(),
                onClick = { onAddressClicked(destination) }
            )
        }
    }
}

@Composable
private fun AddressRow(
    name: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
) {
    ElevatedCardItem(
        imageVector = Icons.Outlined.DeviceHub,
        title = name,
        subtitle = subtitle,
        onClick = onClick
    )
}

@Composable
private fun ExpandableAddressRow(
    node: Node,
    onClick: ((ProxyFilterAddress) -> Unit),
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    ElevatedCardItem(
        imageVector = Icons.Outlined.DeviceHub,
        title = node.name,
        subtitle = node.primaryUnicastAddress.address.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        ),
        titleAction = {
            IconButton(
                onClick = { isExpanded = !isExpanded },
                content = {
                    Icon(
                        modifier = Modifier.rotate(degrees = if (isExpanded) 180f else 0f),
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = null
                    )
                }
            )
        },
        body = if (isExpanded) {
            {
                node.elements.forEach { element ->
                    MeshTwoLineListItem(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable {
                                onClick(element.unicastAddress as ProxyFilterAddress)
                                isExpanded = !isExpanded
                            },
                        title = element.name ?: stringResource(R.string.label_unknown),
                        imageVector = Icons.Outlined.Lan,
                        subtitle = element.unicastAddress.address.toHexString(
                            format = HexFormat {
                                number.prefix = "0x"
                                upperCase = true
                            }
                        )
                    )
                }
            }
        } else null
    )
}

@Composable
private fun ProxyFilterAddress.title(nodes: List<Node>, groups: List<Group>) =
    when (this) {
        is UnicastAddress -> nodes.element(address = this)?.name
            ?: stringResource(R.string.label_unknown)

        is VirtualAddress,
        is GroupAddress,
            -> groups.first { it.address == this }.name

        is FixedGroupAddress -> name()
    }

@Composable
private fun ProxyFilterAddress.subtitle(network: MeshNetwork) = takeIf { it is UnicastAddress }
    ?.let { network.node(address = address)?.name ?: stringResource(R.string.label_unknown) }
    ?: ""
