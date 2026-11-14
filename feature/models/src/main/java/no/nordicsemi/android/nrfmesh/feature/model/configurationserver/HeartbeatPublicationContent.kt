package no.nordicsemi.android.nrfmesh.feature.model.configurationserver

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.SportsScore
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
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
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigHeartbeatPublicationStatus
import no.nordicsemi.kotlin.mesh.core.model.Feature
import no.nordicsemi.kotlin.mesh.core.model.FeatureState
import no.nordicsemi.kotlin.mesh.core.model.Friend
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatPublication
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatPublicationDestination
import no.nordicsemi.kotlin.mesh.core.model.LowPower
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.Proxy
import no.nordicsemi.kotlin.mesh.core.model.Relay
import no.nordicsemi.kotlin.mesh.core.model.RemainingHeartbeatPublicationCount
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
    var features by remember {
        mutableStateOf(publication?.features?.filter { it.isSupported } ?: listOf())
    }

    val network = model.parentElement?.parentNode?.network
    // As the remaining count is dynamic it is not stored in the publication. It is kept from the
    // last received status instead, which also holds the features.
    var lastStatus by remember { mutableStateOf<ConfigHeartbeatPublicationStatus?>(null) }
    LaunchedEffect(messageState) {
        (messageState.response as? ConfigHeartbeatPublicationStatus)?.let { lastStatus = it }
    }
    HeartbeatSectionTitle(
        title = stringResource(R.string.label_heartbeat_publication),
        enabled = !messageState.isInProgress(),
        isRefreshInProgress = messageState.isInProgress()
                && messageState.message is ConfigHeartbeatPublicationGet,
        isAddInProgress = messageState.isInProgress()
                && messageState.message is ConfigHeartbeatPublicationSet,
        showDelete = true,
        onRefresh = { send(ConfigHeartbeatPublicationGet()) },
        onAdd = { showBottomSheet = true },
        onDelete = { send(ConfigHeartbeatPublicationSet()) }
    )
    val unknown = stringResource(R.string.label_unknown)
    if (publication == null || !publication.isEnabled) {
        ElevatedCardItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            imageVector = Icons.Outlined.MonitorHeart,
            title = stringResource(R.string.label_no_publication)
        )
    } else {
        ElevatedCardItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            imageVector = Icons.Outlined.SportsScore,
            title = stringResource(R.string.label_destination),
            subtitle = addressText(network = network, address = publication.address)
        )
        ElevatedCardItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            imageVector = Icons.Outlined.VpnKey,
            title = stringResource(R.string.label_network_key),
            subtitle = network?.networkKeys
                ?.firstOrNull { it.index == publication.index }?.name ?: unknown
        )
        ElevatedCardItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            imageVector = Icons.Outlined.Timer,
            title = stringResource(R.string.label_remaining_count),
            subtitle = when (val count = lastStatus?.count) {
                null, is RemainingHeartbeatPublicationCount.Invalid -> unknown
                is RemainingHeartbeatPublicationCount.Disabled ->
                    stringResource(R.string.label_disabled)

                is RemainingHeartbeatPublicationCount.Indefinitely ->
                    stringResource(R.string.label_indefinitely)

                is RemainingHeartbeatPublicationCount.Exact -> "${count.value}"
                is RemainingHeartbeatPublicationCount.Range -> "${count.low} - ${count.high}"
            }
        )
        ElevatedCardItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            imageVector = Icons.Outlined.AutoFixHigh,
            title = stringResource(R.string.label_features),
            subtitle = (lastStatus?.features ?: publication.features)
                .filter { it.isSupported }
                .map {
                    when (it) {
                        is Friend -> stringResource(R.string.label_friend)
                        is LowPower -> stringResource(R.string.label_low_power)
                        is Proxy -> stringResource(R.string.label_proxy)
                        is Relay -> stringResource(R.string.label_relay)
                    }
                }
                .ifEmpty { listOf(stringResource(R.string.label_none)) }
                .joinToString(separator = ", ")
        )
    }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp),
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
                                            periodLog = if (countLog > 0.toUByte()) periodLog else 0u,
                                            ttl = ttl.toUByte(),
                                            features = features
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
                                buttonIcon = Icons.Outlined.Upload,
                                text = stringResource(R.string.label_set),
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
                            // Features are tracked by type as the state of the node's feature is
                            // irrelevant for triggering heartbeats.
                            features = features.filterNot { it::class == feature::class }
                                .let { if (isEnabled) it + feature else it }
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
            // onClick = { expanded = true }, menuAnchor handles the expansion by default
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
        ExposedDropdownMenu(
            modifier = Modifier.exposedDropdownSize(),
            expanded = expanded,
            onDismissRequest = { expanded = !expanded },
            shape = RoundedCornerShape(size = 16.dp),
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
            // onClick = { expanded = true }, menuAnchor handles the expansion by default
            imageVector = Icons.Outlined.SportsScore,
            title = destination?.let { addressName(network = network, address = it) }
                ?: stringResource(R.string.label_select_destination),
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
        onValueChanged = { text ->
            text.toIntOrNull()?.takeIf { it in 0..127 }?.let { onTtlChanged(it) }
        },
        regex = Regex("^(?:[0-9]|[1-9][0-9]|1[01][0-9]|12[0-7])$"),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number
        )
    )
}

@Composable
private fun PeriodicHeartbeatsRow(
    countLog: UByte,
    onCountLogChanged: (UByte) -> Unit,
    periodLog: UByte,
    onPeriodLogChanged: (UByte) -> Unit,
) {
    // Indefinite (0xFF) is represented as MAX_COUNT_LOG + 1 on the slider.
    val countLogValue = when (countLog.toInt()) {
        HeartbeatPublication.INDEFINITE_COUNT_LOG -> HeartbeatPublication.MAX_COUNT_LOG + 1
        else -> countLog.toInt()
    }
    ElevatedCardItem(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        imageVector = Icons.Outlined.Timer,
        title = stringResource(R.string.title_heartbeat_count_and_period),
        body = {
            Slider(
                value = countLogValue.toFloat(),
                onValueChange = {
                    val newValue = it.roundToInt()
                    if (newValue > 1 && periodLog == 0.toUByte()) {
                        onPeriodLogChanged(HeartbeatPublication.MIN_PERIOD_LOG.toUByte())
                    }
                    if (newValue == HeartbeatPublication.MAX_COUNT_LOG + 1)
                        onCountLogChanged(HeartbeatPublication.INDEFINITE_COUNT_LOG.toUByte())
                    else {
                        onCountLogChanged(newValue.toUByte())
                    }
                },
                valueRange = HeartbeatPublication.MIN_COUNT_LOG.toFloat()..(HeartbeatPublication.MAX_COUNT_LOG + 1).toFloat(),
                steps = 17, // 0..18 -> 19 positions
                colors = NordicSliderDefaults.colors()
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .sizeIn(minWidth = 80.dp),
                text = when (countLogValue) {
                    HeartbeatPublication.MIN_COUNT_LOG ->
                        stringResource(R.string.label_disabled)

                    in ((HeartbeatPublication.MIN_COUNT_LOG)..HeartbeatPublication.MAX_COUNT_LOG) ->
                        "${HeartbeatPublication.countLog2Count(countLog = countLog)}"

                    else -> stringResource(R.string.label_indefinitely)
                },
                textAlign = TextAlign.End
            )
            Slider(
                enabled = countLog != 0.toUByte(),
                value = periodLog.toFloat(),
                onValueChange = { onPeriodLogChanged(it.roundToInt().toUByte()) },
                valueRange = HeartbeatPublication.PERIOD_LOG_RANGE.toFloat(),
                steps = 15, // 1..17 -> 17 positions
                colors = NordicSliderDefaults.colors()
            )

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .sizeIn(minWidth = 80.dp),
                text = if (countLog > 0.toUByte()) {
                    val periodValue =
                        HeartbeatPublication.periodLog2Period(periodLog = periodLog)
                    periodToTime(periodValue.toInt())
                } else stringResource(R.string.label_not_applicable),
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
    // Features whose state is unknown (null) are treated as supported, as only features known to
    // be unsupported can't trigger heartbeats.
    val nodeFeatures = with(node.features) {
        listOf(
            relay ?: Relay(state = FeatureState.Disabled),
            proxy ?: Proxy(state = FeatureState.Disabled),
            friend ?: Friend(state = FeatureState.Disabled),
            lowPower ?: LowPower(state = FeatureState.Disabled)
        )
    }
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
                    isChecked = features.any { it::class == feature::class },
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