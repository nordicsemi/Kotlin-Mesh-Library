package no.nordicsemi.android.nrfmesh.feature.nodes.node

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SafetyCheck
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.KeyIdGenerator
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.copyToClipboard
import no.nordicsemi.android.nrfmesh.core.data.configurator.MeshTask
import no.nordicsemi.android.nrfmesh.core.data.configurator.TaskStatus
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItemTextField
import no.nordicsemi.android.nrfmesh.core.ui.MeshAlertDialog
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedTextField
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.nodes.R
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigDefaultTtlGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigDefaultTtlSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNodeReset
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.util.CompanyIdentifier
import java.util.Locale.ROOT
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
internal fun NodeScreen(
    messageState: MessageState,
    nodeData: NodeInfoListData,
    node: Node,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    highlightSelectedItem: Boolean,
    selectedItem: ClickableNodeInfoItem?,
    onNetworkKeysClicked: (Uuid) -> Unit,
    onApplicationKeysClicked: (Uuid) -> Unit,
    onElementClicked: (Address) -> Unit,
    onExcluded: (Boolean) -> Unit,
    send: (AcknowledgedConfigMessage) -> Unit,
    save: () -> Unit,
    navigateBack: () -> Unit,
    removeNode: () -> Unit,
    tasks: List<MeshTask>,
    onReconfigCompletePressed: () -> Unit,
    onCancelPressed: () -> Unit,
    onRetryPressed: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()
    var showConfigurationTasks by rememberSaveable { mutableStateOf(tasks.isNotEmpty()) }
    val reconfigBottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    PullToRefreshBox(
        modifier = Modifier.fillMaxSize(),
        state = pullToRefreshState,
        onRefresh = onRefresh,
        isRefreshing = isRefreshing
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            SectionTitle(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .padding(horizontal = 16.dp),
                title = stringResource(R.string.label_node)
            )
            NodeNameRow(
                name = nodeData.name,
                onNameChanged = {
                    node.name = it
                    save()
                }
            )
            AddressRow(address = nodeData.address)
            SectionTitle(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(id = R.string.title_keys)
            )
            DeviceKeyRow(deviceKey = nodeData.deviceKey ?: stringResource(R.string.unknown))
            NetworkKeysRow(
                count = nodeData.netKeys.size,
                isSelected = selectedItem == ClickableNodeInfoItem.NetworkKeys
                        && highlightSelectedItem,
                onNetworkKeysClicked = { onNetworkKeysClicked(nodeData.uuid) }
            )
            ApplicationKeysRow(
                count = nodeData.appKeys.size,
                isSelected = selectedItem == ClickableNodeInfoItem.ApplicationKeys
                        && highlightSelectedItem,
                onApplicationKeysClicked = { onApplicationKeysClicked(nodeData.uuid) }
            )
            SectionTitle(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(id = R.string.title_elements)
            )
            nodeData.elements.forEach { element ->
                key(element.index) {
                    ElementRow(
                        element = element,
                        isSelected = (selectedItem as? ClickableNodeInfoItem.Element)?.address
                                == element.unicastAddress.address
                                && highlightSelectedItem,
                        onElementsClicked = { onElementClicked(element.unicastAddress.address) }
                    )
                }
            }
            SectionTitle(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(id = R.string.title_node_information)
            )
            CompanyIdentifier(companyIdentifier = nodeData.companyIdentifier)
            ProductIdentifier(productIdentifier = nodeData.productIdentifier)
            ProductVersion(productVersion = nodeData.versionIdentifier)
            ReplayProtectionCount(replayProtectionCount = nodeData.replayProtectionCount)
            Security(node = nodeData)
            SectionTitle(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(id = R.string.title_time_to_live)
            )
            DefaultTtlRow(
                ttl = nodeData.defaultTtl,
                messageState = messageState,
                send = send
            )
            SectionTitle(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(id = R.string.title_exclusions)
            )
            ExclusionRow(isExcluded = nodeData.excluded, onExcluded = onExcluded)
            SectionTitle(
                modifier = Modifier.padding(horizontal = 16.dp),
                title = stringResource(id = R.string.label_node_deletion)
            )
            ResetRow(messageState = messageState, navigateBack = navigateBack, send = send)
            RemoveNode(
                navigateBack = navigateBack,
                removeNode = removeNode
            )
            Spacer(modifier = Modifier.size(size = 16.dp))
        }
        if (showConfigurationTasks) {
            ModalBottomSheet(
                sheetState = reconfigBottomSheetState,
                onDismissRequest = {
                    showConfigurationTasks = !showConfigurationTasks
                    onReconfigCompletePressed()
                },
                sheetGesturesEnabled = !tasks.any { it.status !is TaskStatus.Completed },
                properties = ModalBottomSheetProperties(
                    shouldDismissOnBackPress = false,
                    shouldDismissOnClickOutside = false
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 16.dp)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle(
                        modifier = Modifier
                            .weight(weight = 1f)
                            .padding(horizontal = 16.dp),
                        title = stringResource(id = R.string.label_configuration)
                    )
                    MeshOutlinedButton(
                        enabled = tasks.any { it.status is TaskStatus.Error },
                        onClick = onRetryPressed,
                        buttonIcon = Icons.Outlined.Refresh,
                        text = stringResource(R.string.label_retry)
                    )
                    MeshOutlinedButton(
                        onClick = {
                            scope.launch {
                                reconfigBottomSheetState.hide()
                            }.invokeOnCompletion {
                                if (!reconfigBottomSheetState.isVisible) {
                                    if (!tasks.any { it.status !is TaskStatus.Completed }) {
                                        onReconfigCompletePressed()
                                    } else {
                                        onCancelPressed()
                                    }
                                    showConfigurationTasks = !showConfigurationTasks
                                }
                            }
                        },
                        buttonIcon = if (!tasks.any { it.status !is TaskStatus.Completed }) Icons.Outlined.AutoFixHigh else Icons.Outlined.Cancel,
                        text = stringResource(if (!tasks.any { it.status !is TaskStatus.Completed }) R.string.label_done else R.string.label_cancel),
                    )
                }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                    items(items = tasks, key = { KeyIdGenerator.nextId() }) {
                        ElevatedCardItem(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            imageVector = it.icon,
                            title = it.label,
                            subtitle = it.status.description(),
                            subtitleTextColor = it.status.color()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NodeNameRow(name: String, onNameChanged: (String) -> Unit) {
    ElevatedCardItemTextField(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Badge,
        title = stringResource(id = R.string.label_name),
        subtitle = name,
        placeholder = stringResource(id = R.string.label_placeholder_node_name),
        onValueChanged = onNameChanged
    )
}

@Composable
private fun AddressRow(address: UnicastAddress) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Lan,
        title = stringResource(id = R.string.label_unicast_address),
        subtitle = address.address.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        )
    )
}

@Composable
private fun DeviceKeyRow(deviceKey: String) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val message = stringResource(R.string.label_device_key)
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.VpnKey,
        title = stringResource(id = R.string.label_device_key),
        subtitle = deviceKey,
        onClick = {
            copyToClipboard(
                scope = scope,
                clipboard = clipboard,
                text = deviceKey,
                label = message
            )
        }
    )
}

@Composable
private fun NetworkKeysRow(count: Int, isSelected: Boolean, onNetworkKeysClicked: () -> Unit) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        colors = when (isSelected) {
            true -> CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )

            else -> CardDefaults.outlinedCardColors()
        },
        imageVector = Icons.Outlined.VpnKey,
        onClick = onNetworkKeysClicked,
        title = stringResource(R.string.label_network_keys),
        subtitle = "$count ${if (count == 1) "key" else "keys"} added"
    )
}

@Composable
private fun ApplicationKeysRow(
    count: Int,
    isSelected: Boolean,
    onApplicationKeysClicked: () -> Unit,
) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        colors = when (isSelected) {
            true -> CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
            else -> CardDefaults.outlinedCardColors()
        },
        onClick = onApplicationKeysClicked,
        imageVector = Icons.Outlined.VpnKey,
        title = stringResource(R.string.label_application_keys),
        subtitle = "$count ${if (count == 1) "key" else "keys"} added"
    )
}

@Composable
private fun ElementRow(
    element: ElementListData,
    isSelected: Boolean,
    onElementsClicked: () -> Unit,
) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        colors = when (isSelected) {
            true -> CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )

            else -> CardDefaults.outlinedCardColors()
        },
        onClick = onElementsClicked,
        imageVector = Icons.Outlined.DeviceHub,
        title = element.name ?: "Unknown",
        subtitle = "${element.models.size} ${if (element.models.size == 1) "model" else "models"}"
    )
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
private fun CompanyIdentifier(companyIdentifier: UShort?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.WorkOutline,
        title = stringResource(R.string.label_company_identifier),
        subtitle = companyIdentifier
            ?.let {
                CompanyIdentifier.name(id = it) ?: it
                    .toHexString(
                        format = HexFormat {
                            number.prefix = "0x"
                            upperCase = true
                        }
                    ).uppercase()
            }
            ?: stringResource(R.string.unknown),
    )
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
private fun ProductIdentifier(productIdentifier: UShort?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.QrCode,
        title = stringResource(R.string.label_product_identifier),
        subtitle = productIdentifier?.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        ) ?: stringResource(R.string.unknown),
    )
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
private fun ProductVersion(productVersion: UShort?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Numbers,
        title = stringResource(R.string.label_product_version),
        subtitle = productVersion?.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        ) ?: stringResource(R.string.unknown),
    )
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
private fun ReplayProtectionCount(replayProtectionCount: UShort?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.SafetyCheck,
        title = stringResource(R.string.label_replay_protection_count),
        subtitle = "${replayProtectionCount ?: stringResource(R.string.unknown)}",
    )
}

@Composable
private fun Security(node: NodeInfoListData) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Security,
        title = stringResource(R.string.label_security),
        subtitle = node.security.toString().replaceFirstChar { it.titlecase(locale = ROOT) }
    )
}

@Composable
private fun DefaultTtlRow(
    ttl: UByte?,
    messageState: MessageState,
    send: (AcknowledgedConfigMessage) -> Unit,
) {
    val context = LocalContext.current
    var showDefaultTtlDialog by rememberSaveable { mutableStateOf(false) }
    var ttlInput by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(
            TextFieldValue(
                text = ttl?.toInt()?.toString() ?: "127",
                selection = TextRange((ttl?.toInt()?.toString() ?: "127").length)
            )
        )
    }
    var isError by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf("") }

    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Timer,
        title = stringResource(R.string.label_default_time_to_live),
        subtitle = if (ttl != null) "TTL set to $ttl." else "Unknown",
        supportingText = stringResource(R.string.label_default_ttl_rationale)
    ) {
        MeshOutlinedButton(
            onClick = { send(ConfigDefaultTtlGet()) },
            text = stringResource(R.string.label_get_ttl),
            buttonIcon = Icons.Outlined.Download,
            enabled = !messageState.isInProgress(),
            isOnClickActionInProgress = messageState.isInProgress()
                    && messageState.message is ConfigDefaultTtlGet
        )
        Spacer(modifier = Modifier.padding(horizontal = 8.dp))
        MeshOutlinedButton(
            onClick = { showDefaultTtlDialog = !showDefaultTtlDialog },
            text = stringResource(R.string.label_set_ttl),
            buttonIcon = Icons.Outlined.Upload,
            enabled = !messageState.isInProgress(),
            isOnClickActionInProgress = messageState.isInProgress()
                    && messageState.message is ConfigDefaultTtlSet
        )
    }

    if (showDefaultTtlDialog) {
        MeshAlertDialog(
            onDismissRequest = { showDefaultTtlDialog = !showDefaultTtlDialog },
            onConfirmClick = {
                if (ttlInput.text.isBlank()) {
                    isError = !isError
                    errorMessage = context.getString(R.string.label_default_ttl_empty_error)
                } else {
                    showDefaultTtlDialog = !showDefaultTtlDialog
                    send(ConfigDefaultTtlSet(ttl = ttlInput.text.toUByte()))
                }
            },
            onDismissClick = { showDefaultTtlDialog = !showDefaultTtlDialog },
            icon = Icons.Outlined.Timer,
            iconColor = AlertDialogDefaults.iconContentColor,
            title = stringResource(R.string.label_default_ttl),
            content = {
                Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                    Text(text = stringResource(R.string.label_default_ttl_definition))
                    MeshOutlinedTextField(
                        externalLeadingIcon = {
                            Icon(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp)
                                    .padding(end = 8.dp),
                                imageVector = Icons.Outlined.FormatListNumbered,
                                contentDescription = null
                            )
                        },
                        value = ttlInput,
                        onValueChanged = {
                            ttlInput = it
                            if (it.text.isNotEmpty()) {
                                isError = false
                                errorMessage = ""
                            }
                        },
                        label = { Text(text = stringResource(R.string.label_default_ttl)) },
                        keyboardOptions = KeyboardOptions(
                            autoCorrectEnabled = false,
                            keyboardType = KeyboardType.Number
                        ),
                        isError = isError,
                        supportingText = {
                            if (isError) {
                                Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        regex = Regex("^$|^(0|[1-9]\\d?|1[01]\\d|12[0-7])$")
                    )
                }
            }
        )
    }
}

@Composable
private fun ExclusionRow(isExcluded: Boolean, onExcluded: (Boolean) -> Unit) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Block,
        title = stringResource(R.string.label_exclude_node),
        titleAction = {
            Switch(
                modifier = Modifier.padding(horizontal = 16.dp),
                checked = isExcluded,
                onCheckedChange = onExcluded
            )
        },
        supportingText = stringResource(R.string.label_exclusion_rationale),
        body = { Spacer(modifier = Modifier.size(8.dp)) }
    )
}

@Composable
private fun ResetRow(
    messageState: MessageState,
    send: (AcknowledgedConfigMessage) -> Unit,
    navigateBack: () -> Unit,
) {
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Recycling,
        title = stringResource(R.string.label_reset_node),
        supportingText = stringResource(R.string.label_reset_node_rationale)
    ) {
        MeshOutlinedButton(
            border = BorderStroke(width = 1.dp, color = Color.Red),
            onClick = { showResetDialog = !showResetDialog },
            text = stringResource(R.string.label_reset),
            buttonIcon = Icons.Outlined.Recycling,
            buttonIconTint = Color.Red,
            textColor = Color.Red,
            enabled = !messageState.isInProgress(),
            isOnClickActionInProgress = messageState.isInProgress()
                    && messageState.message is ConfigNodeReset
        )
    }
    if (showResetDialog) {
        MeshAlertDialog(
            onDismissRequest = { showResetDialog = !showResetDialog },
            icon = Icons.Outlined.Recycling,
            title = stringResource(R.string.label_reset_node),
            text = stringResource(R.string.label_are_you_sure_rationale),
            iconColor = Color.Red,
            onDismissClick = { showResetDialog = !showResetDialog },
            onConfirmClick = {
                showResetDialog = !showResetDialog
                send(ConfigNodeReset())
            }
        )
    }
}

@Composable
private fun RemoveNode(
    removeNode: () -> Unit,
    navigateBack: () -> Unit,
) {
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.DeleteForever,
        title = stringResource(R.string.label_remove_node),
        supportingText = stringResource(R.string.label_remove_node_rationale)
    ) {
        MeshOutlinedButton(
            border = BorderStroke(width = 1.dp, color = Color.Red),
            onClick = { showResetDialog = !showResetDialog },
            text = stringResource(R.string.label_remove),
            buttonIcon = Icons.Outlined.DeleteForever,
            buttonIconTint = Color.Red,
            textColor = Color.Red
        )
    }
    if (showResetDialog) {
        MeshAlertDialog(
            onDismissRequest = { showResetDialog = !showResetDialog },
            icon = Icons.Outlined.DeleteForever,
            title = stringResource(R.string.label_remove_node),
            text = stringResource(R.string.label_are_you_sure_rationale),
            iconColor = Color.Red,
            onDismissClick = { showResetDialog = !showResetDialog },
            onConfirmClick = {
                showResetDialog = !showResetDialog
                removeNode()
                navigateBack()
            }
        )
    }
}

@Composable
fun TaskStatus.color() = when (this) {
    is TaskStatus.Idle -> Color.Gray
    is TaskStatus.InProgress -> Color.Blue
    is TaskStatus.Skipped -> Color.Gray
    is TaskStatus.Completed -> Color.Green
    is TaskStatus.Error -> Color.Red
}

fun TaskStatus.description() = when (this) {
    TaskStatus.Idle -> "Waiting..."
    TaskStatus.InProgress -> "In progress"
    TaskStatus.Skipped -> "Skipped"
    TaskStatus.Completed -> "Success"
    is TaskStatus.Error -> error
}