package no.nordicsemi.android.nrfmesh.feature.model.configurationserver

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.SportsScore
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import no.nordicsemi.android.common.ui.view.NordicSliderDefaults
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItemTextField
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshSingleLineListItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.model.utils.periodToTime
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigHeartbeatPublicationGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigHeartbeatPublicationSet
import no.nordicsemi.kotlin.mesh.core.model.AllFriends
import no.nordicsemi.kotlin.mesh.core.model.AllNodes
import no.nordicsemi.kotlin.mesh.core.model.AllProxies
import no.nordicsemi.kotlin.mesh.core.model.AllRelays
import no.nordicsemi.kotlin.mesh.core.model.Feature
import no.nordicsemi.kotlin.mesh.core.model.Friend
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatPublication
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatPublicationDestination
import no.nordicsemi.kotlin.mesh.core.model.LowPower
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.Proxy
import no.nordicsemi.kotlin.mesh.core.model.Relay
import no.nordicsemi.kotlin.mesh.core.model.UnassignedAddress
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HeartBeatPublicationContent(
    model: Model,
    messageState: MessageState,
    publication: HeartbeatPublication?,
    send: (AcknowledgedConfigMessage) -> Unit,
    onAddGroupClicked: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var keyIndex by remember { mutableIntStateOf(publication?.index?.toInt() ?: 0) }
    var ttl by remember { mutableIntStateOf(publication?.ttl?.toInt() ?: 5) }
    var destination by remember { mutableStateOf(publication?.address) }
    var countLog by remember { mutableStateOf(0.toUByte()) }
    var periodLog by remember { mutableStateOf(publication?.periodLog ?: 1u) }
    var features by remember { mutableStateOf(publication?.features?.toList() ?: listOf()) }

    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.MonitorHeart,
        title = stringResource(R.string.label_publications),
        titleAction = {
            AnimatedVisibility(visible = publication != null) {
                IconButton(
                    enabled = !messageState.isInProgress(),
                    onClick = { send(ConfigHeartbeatPublicationSet()) },
                    content = {
                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                    }
                )
            }
        },
        actions = {
            MeshOutlinedButton(
                enabled = !messageState.isInProgress(),
                isOnClickActionInProgress = messageState.isInProgress()
                        && messageState.message is ConfigHeartbeatPublicationGet,
                onClick = { send(ConfigHeartbeatPublicationGet()) },
                buttonIcon = Icons.Outlined.Download,
                text = stringResource(R.string.label_get_state)
            )
            Spacer(modifier = Modifier.padding(8.dp))
            MeshOutlinedButton(
                enabled = !messageState.isInProgress(),
                isOnClickActionInProgress = messageState.isInProgress()
                        && messageState.message is ConfigHeartbeatPublicationSet,
                onClick = { showBottomSheet = true },
                buttonIcon = Icons.Outlined.Upload,
                text = stringResource(R.string.label_set_state)
            )
        }
    )
    DisposableEffect(showBottomSheet) {
        onDispose {
            if (!showBottomSheet) {
                keyIndex = 0
                ttl = 5
                destination = null
                countLog = 0u
                periodLog = 1u
                features = listOf()
            }
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            containerColor = MaterialTheme.colorScheme.surface,
            sheetState = bottomSheetState,
            onDismissRequest = { showBottomSheet = !showBottomSheet },
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(state = rememberScrollState()),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = {
                            SectionTitle(
                                modifier = Modifier
                                    .weight(weight = 1f)
                                    .padding(horizontal = 16.dp),
                                title = stringResource(R.string.label_heartbeat_publication)
                            )
                            MeshOutlinedButton(
                                enabled = destination != null,
                                onClick = {
                                    send(
                                        ConfigHeartbeatPublicationSet(
                                            networkKeyIndex = keyIndex.toUShort(),
                                            destination = destination!!,
                                            countLog = countLog,
                                            periodLog = periodLog,
                                            ttl = ttl.toUByte(),
                                            features = emptyList()//features
                                        )
                                    ).also {
                                        scope
                                            .launch { bottomSheetState.hide() }
                                            .invokeOnCompletion {
                                                if (!bottomSheetState.isVisible) {
                                                    showBottomSheet = false
                                                }
                                            }
                                    }
                                },
                                buttonIcon = Icons.AutoMirrored.Outlined.Send,
                                text = stringResource(R.string.label_send),
                            )
                        }
                    )
                    NetworkKeysRow(
                        network = model.parentElement?.parentNode?.network,
                        selectedKeyIndex = keyIndex,
                        onNetworkKeySelected = { keyIndex = it }
                    )
                    SectionTitle(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        title = stringResource(R.string.label_destination)
                    )
                    DestinationRow(
                        network = model.parentElement?.parentNode?.network,
                        model = model,
                        destination = destination,
                        onDestinationSelected = { destination = it },
                        onAddGroupClicked = onAddGroupClicked
                    )
                    TtlRow(ttl = ttl, onTtlChanged = { ttl = it })
                    PeriodicHeartbeatsRow(
                        publication = publication,
                        countLog = countLog,
                        onCountLogChanged = { countLog = it },
                        periodLog = periodLog,
                        onPeriodLogChanged = { periodLog = it }
                    )
                    FeaturesRow(
                        node = model.parentElement?.parentNode
                            ?: throw IllegalStateException("Model does not belong to a node!"),
                        features = features,
                        onFeatureChanged = { feature, isEnabled ->
                            when (isEnabled) {
                                true -> features += feature
                                false -> features -= feature
                            }
                        }
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkKeysRow(
    network: MeshNetwork?,
    selectedKeyIndex: Int,
    onNetworkKeySelected: (Int) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        ElevatedCardItem(
            modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            onClick = { expanded = true },
            imageVector = Icons.Outlined.VpnKey,
            title = stringResource(R.string.label_network_key),
            titleAction = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    expanded = expanded
                )
            },
            subtitle = network?.networkKeys?.firstOrNull {
                it.index == selectedKeyIndex.toUShort()
            }?.name ?: stringResource(R.string.label_unknown)
        )
        DropdownMenu(
            modifier = Modifier.exposedDropdownSize(),
            expanded = expanded,
            onDismissRequest = { expanded = !expanded },
            content = {
                network?.networkKeys?.forEachIndexed { index, key ->
                    DropdownMenuItem(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        text = {
                            MeshSingleLineListItem(
                                imageVector = Icons.Outlined.VpnKey,
                                title = key.name,
                            )
                        },
                        onClick = {
                            onNetworkKeySelected(key.index.toInt())
                                .also { expanded = !expanded }
                        }
                    )
                    if (index < network.networkKeys.size - 1)
                        HorizontalDivider()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DestinationRow(
    network: MeshNetwork?,
    model: Model,
    destination: HeartbeatPublicationDestination?,
    onDestinationSelected: (HeartbeatPublicationDestination) -> Unit,
    onAddGroupClicked: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        modifier = Modifier.padding(horizontal = 16.dp),
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        ElevatedCardItem(
            modifier = Modifier
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            onClick = { expanded = true },
            imageVector = Icons.Outlined.SportsScore,
            title = when (destination) {
                is UnicastAddress -> network
                    ?.node(address = destination.address)
                    ?.name ?: destination.toHexString()

                is AllRelays -> stringResource(R.string.label_all_relays)
                is AllFriends -> stringResource(R.string.label_all_friends)
                is AllProxies -> stringResource(R.string.label_all_proxies)
                is AllNodes -> stringResource(R.string.label_all_nodes)
                is GroupAddress -> network?.group(address = destination.address)?.name
                    ?: destination.toHexString()

                is UnassignedAddress -> stringResource(R.string.label_unassigned_address)
                else -> stringResource(R.string.label_select_destination)
            },
            titleAction = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    expanded = expanded
                )
            },
            subtitle = destination?.toHexString()
        )
        HeartbeatPublicationDestinationsDropdownMenu(
            network = network,
            model = model,
            expanded = expanded,
            onDismissed = { expanded = !expanded },
            onDestinationSelected = {
                onDestinationSelected(it)
                expanded = !expanded
            },
            onAddGroupClicked = onAddGroupClicked
        )
    }
}

@Composable
private fun TtlRow(ttl: Int, onTtlChanged: (Int) -> Unit) {
    ElevatedCardItemTextField(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        imageVector = Icons.Outlined.Timer,
        title = stringResource(id = R.string.label_initial_ttl),
        subtitle = "$ttl",
        onValueChanged = { onTtlChanged(it.toInt()) },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        )
    )
}

@Composable
private fun PeriodicHeartbeatsRow(
    publication: HeartbeatPublication?,
    countLog: UByte,
    onCountLogChanged: (UByte) -> Unit,
    periodLog: UByte,
    onPeriodLogChanged: (UByte) -> Unit,
) {
    var countLogValue by rememberSaveable { mutableIntStateOf(countLog.toInt()) }
    ElevatedCardItem(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        imageVector = Icons.Outlined.Timer,
        title = stringResource(R.string.title_heartbeat_count_and_period),
        body = {
            Slider(
                value = countLog.toFloat(),
                onValueChange = {
                    countLogValue = it.roundToInt()
                    if (countLogValue > 1 && periodLog == 0.toUByte()) {
                        onPeriodLogChanged(HeartbeatPublication.MIN_PERIOD_LOG.toUByte())
                    }
                    if (countLogValue == HeartbeatPublication.MAX_COUNT_LOG + 1)
                        onCountLogChanged(HeartbeatPublication.INDEFINITE_COUNT_LOG.toUByte())
                    else {
                        onCountLogChanged(countLogValue.toUByte())
                    }
                },
                valueRange = HeartbeatPublication.MIN_COUNT_LOG.toFloat()..(HeartbeatPublication.MAX_COUNT_LOG + 1).toFloat(),
                steps = 16,
                colors = NordicSliderDefaults.colors()
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .sizeIn(minWidth = 80.dp),
                text = when (publication) {
                    null -> stringResource(R.string.label_unknown)
                    else -> "${
                        when (countLogValue) {
                            HeartbeatPublication.MIN_COUNT_LOG ->
                                stringResource(R.string.label_disabled)

                            in ((HeartbeatPublication.MIN_COUNT_LOG)..HeartbeatPublication.MAX_COUNT_LOG) ->
                                HeartbeatPublication.countLog2Count(countLog = countLog)

                            else -> stringResource(R.string.label_indefinitely)
                        }
                    }"
                },
                textAlign = TextAlign.End
            )
            Slider(
                enabled = countLog != 0.toUByte(),
                value = periodLog.toFloat(),
                onValueChange = { onPeriodLogChanged(it.roundToInt().toUByte()) },
                valueRange = HeartbeatPublication.PERIOD_LOG_RANGE.toFloat(),
                steps = 30,
                colors = NordicSliderDefaults.colors()
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .sizeIn(minWidth = 80.dp),
                text = when (publication) {
                    null -> stringResource(R.string.label_unknown)
                    else -> if (countLog > 0.toUByte()) {
                        val periodValue =
                            HeartbeatPublication.periodLog2Period(periodLog = periodLog)
                        periodToTime(periodValue.toInt())
                    } else stringResource(R.string.label_not_applicable)
                },
                textAlign = TextAlign.End
            )
        }
    )
}

@Composable
private fun FeaturesRow(
    node: Node,
    features: List<Feature>,
    onFeatureChanged: (Feature, Boolean) -> Unit,
) {
    val nodeFeatures = node.features.toList()
    ElevatedCardItem(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(vertical = 8.dp),
        imageVector = Icons.Outlined.AutoFixHigh,
        title = stringResource(id = R.string.label_features),
        body = {
            nodeFeatures.forEach { feature ->
                FeatureRow(
                    text = when (feature) {
                        is Friend -> stringResource(id = R.string.label_friend)
                        is LowPower -> stringResource(id = R.string.label_low_power)
                        is Proxy -> stringResource(id = R.string.label_proxy)
                        is Relay -> stringResource(id = R.string.label_relay)
                    },
                    isSupported = feature.isSupported,
                    isChecked = features.firstOrNull { it == feature }?.isEnabled ?: false,
                    onCheckedChange = { onFeatureChanged(feature, it) }
                )
            }
        }
    )
}

@Composable
private fun FeatureRow(
    text: String,
    isSupported: Boolean,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(modifier = Modifier.weight(1f), text = text)
        Switch(enabled = isSupported, checked = isChecked, onCheckedChange = onCheckedChange)
    }
}