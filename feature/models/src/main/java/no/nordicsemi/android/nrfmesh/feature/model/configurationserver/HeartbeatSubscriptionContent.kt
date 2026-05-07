package no.nordicsemi.android.nrfmesh.feature.model.configurationserver

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.SportsScore
import androidx.compose.material.icons.outlined.Start
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import no.nordicsemi.android.common.ui.view.NordicSliderDefaults
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshTwoLineListItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.model.utils.periodToTime
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigHeartbeatSubscriptionGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigHeartbeatSubscriptionSet
import no.nordicsemi.kotlin.mesh.core.model.AllFriends
import no.nordicsemi.kotlin.mesh.core.model.AllNodes
import no.nordicsemi.kotlin.mesh.core.model.AllProxies
import no.nordicsemi.kotlin.mesh.core.model.AllRelays
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatSubscription
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatSubscriptionDestination
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatSubscriptionSource
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.PeriodLog
import no.nordicsemi.kotlin.mesh.core.model.UnassignedAddress
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import kotlin.math.roundToInt


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HeartBeatSubscriptionContent(
    model: Model,
    messageState: MessageState,
    subscription: HeartbeatSubscription?,
    send: (AcknowledgedConfigMessage) -> Unit,
    onAddGroupClicked: () -> Unit,
) {
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var source by remember { mutableStateOf(subscription?.source) }
    var destination by remember { mutableStateOf(subscription?.destination) }
    var periodLog by remember { mutableStateOf(subscription?.state?.periodLog ?: 1.toUByte()) }

    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.MonitorHeart,
        title = stringResource(R.string.label_subscriptions),
        titleAction = {
            AnimatedVisibility(visible = subscription != null) {
                IconButton(
                    enabled = !messageState.isInProgress(),
                    onClick = { send(ConfigHeartbeatSubscriptionSet()) },
                    content = {
                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
                    }
                )
            }
        },
        body = {
            if (subscription != null &&
                subscription.source !is UnassignedAddress &&
                subscription.destination !is UnassignedAddress
            ) {
                Spacer(modifier = Modifier.size(16.dp))
                MeshTwoLineListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 50.dp)
                        .padding(horizontal = 40.dp),
                    title = stringResource(R.string.label_source),
                    subtitle = with(subscription.source) {
                        model.parentElement?.parentNode?.network?.node(address)?.name
                            ?: toHexString()
                    }
                )
                MeshTwoLineListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 50.dp)
                        .padding(horizontal = 40.dp),
                    title = stringResource(R.string.label_destination),
                    subtitle = with(subscription.destination) {
                        model.parentElement?.parentNode?.network?.node(address)?.name
                            ?: toHexString()
                    }
                )
                MeshTwoLineListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 50.dp)
                        .padding(horizontal = 40.dp),
                    title = stringResource(R.string.label_remaining_period),
                    subtitle = subscription.state?.period?.toString()
                        ?: stringResource(R.string.label_unknown)
                )
                MeshTwoLineListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 50.dp)
                        .padding(horizontal = 40.dp),
                    title = stringResource(R.string.label_count),
                    subtitle = subscription.state?.count?.toString()
                        ?: stringResource(R.string.label_unknown)
                )
                MeshTwoLineListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 50.dp)
                        .padding(horizontal = 40.dp),
                    title = stringResource(R.string.label_min_hops),
                    subtitle = subscription.state?.minHops?.toString()
                        ?: stringResource(R.string.label_unknown)
                )
                MeshTwoLineListItem(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(height = 50.dp)
                        .padding(horizontal = 40.dp),
                    title = stringResource(R.string.label_max_hops),
                    subtitle = subscription.state?.maxHops?.toString()
                        ?: stringResource(R.string.label_unknown)
                )
            }
        },
        actions = {
            MeshOutlinedButton(
                enabled = !messageState.isInProgress(),
                buttonIcon = Icons.Outlined.Download,
                text = stringResource(R.string.label_get_state),
                isOnClickActionInProgress = messageState.isInProgress()
                        && messageState.message is ConfigHeartbeatSubscriptionGet,
                onClick = { send(ConfigHeartbeatSubscriptionGet()) },
            )
            Spacer(modifier = Modifier.size(8.dp))
            MeshOutlinedButton(
                enabled = !messageState.isInProgress(),
                buttonIcon = Icons.Outlined.Upload,
                text = stringResource(R.string.label_set_state),
                isOnClickActionInProgress = messageState.isInProgress()
                        && messageState.message is ConfigHeartbeatSubscriptionSet,
                onClick = { showBottomSheet = true }
            )
        }
    )

    if (showBottomSheet) {
        ModalBottomSheet(
            containerColor = MaterialTheme.colorScheme.surface,
            sheetState = bottomSheetState,
            onDismissRequest = { showBottomSheet = !showBottomSheet },
            content = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .verticalScroll(state = rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(space = 8.dp)
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
                                title = stringResource(R.string.label_heartbeat_subscription)
                            )
                            MeshOutlinedButton(
                                enabled = source != null && destination != null,
                                onClick = {
                                    send(
                                        ConfigHeartbeatSubscriptionSet(
                                            source = source!!,
                                            destination = destination!!,
                                            periodLog = periodLog
                                        )
                                    ).also {
                                        scope
                                            .launch { bottomSheetState.hide() }
                                            .invokeOnCompletion {
                                                if (!bottomSheetState.isVisible)
                                                    showBottomSheet = false
                                            }
                                    }
                                },
                                buttonIcon = Icons.AutoMirrored.Outlined.Send,
                                text = stringResource(R.string.label_send),
                            )
                        }
                    )
                    PeriodRow(
                        periodLog = periodLog,
                        onPeriodLogChanged = { periodLog = it }
                    )
                    SectionTitle(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        title = stringResource(R.string.label_source)
                    )
                    SourceRow(
                        model = model,
                        source = source,
                        onSourceSelected = { source = it }
                    )
                    SectionTitle(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        title = stringResource(R.string.label_destination)
                    )
                    DestinationRow(
                        model = model,
                        destination = destination,
                        onDestinationSelected = { destination = it },
                        onAddGroupClicked = {
                            scope
                                .launch { bottomSheetState.hide() }
                                .invokeOnCompletion {
                                    if (!bottomSheetState.isVisible) {
                                        showBottomSheet = false
                                        onAddGroupClicked()
                                    }
                                }
                            //onAddGroupClicked()
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun PeriodRow(
    periodLog: PeriodLog,
    onPeriodLogChanged: (PeriodLog) -> Unit,
) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Timer,
        title = stringResource(R.string.label_period),
        subtitle = stringResource(R.string.label_remaining_period_seconds),
        body = {
            Slider(
                modifier = Modifier.padding(start = 40.dp),
                value = periodLog.toFloat(),
                onValueChange = { onPeriodLogChanged(it.roundToInt().toUByte()) },
                valueRange = (HeartbeatSubscription.PERIOD_LOG_MIN + 1).toFloat()..HeartbeatSubscription.PERIOD_LOG_MAX.toFloat(),
                steps = 16,
                colors = NordicSliderDefaults.colors()
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .sizeIn(minWidth = 80.dp),
                text = periodToTime(
                    seconds = HeartbeatSubscription.periodLog2Period(periodLog = periodLog).toInt()
                ),
                textAlign = TextAlign.End
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SourceRow(
    model: Model,
    source: HeartbeatSubscriptionSource?,
    onSourceSelected: (HeartbeatSubscriptionSource) -> Unit,
) {
    val network = model.parentElement?.parentNode?.network
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = Modifier.padding(horizontal = 16.dp),
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        ElevatedCardItem(
            modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            onClick = { expanded = true },
            imageVector = Icons.Outlined.Start,
            title = when (source) {
                is UnicastAddress -> network
                    ?.node(address = source.address)
                    ?.name ?: stringResource(R.string.label_unknown)

                is UnassignedAddress -> stringResource(R.string.label_unassigned_address)
                else -> stringResource(R.string.label_select_source)
            },
            titleAction = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    expanded = expanded
                )
            },
            subtitle = source?.toHexString()
        )
        HeartbeatSubscriptionSourcesDropdownMenu(
            network = network,
            model = model,
            expanded = expanded,
            onDismissed = { expanded = !expanded },
            onSourceSelected = {
                onSourceSelected(it)
                expanded = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DestinationRow(
    model: Model,
    destination: HeartbeatSubscriptionDestination?,
    onDestinationSelected: (HeartbeatSubscriptionDestination) -> Unit,
    onAddGroupClicked: () -> Unit,
) {
    val network = model.parentElement?.parentNode?.network ?: return
    var expanded by rememberSaveable { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        ElevatedCardItem(
            modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            onClick = { expanded = true },
            imageVector = Icons.Outlined.SportsScore,
            title = when (destination) {
                is UnicastAddress -> network
                    .node(address = destination.address)
                    ?.name ?: stringResource(R.string.label_unknown)

                is AllRelays -> stringResource(R.string.label_all_relays)
                is AllFriends -> stringResource(R.string.label_all_friends)
                is AllProxies -> stringResource(R.string.label_all_proxies)
                is AllNodes -> stringResource(R.string.label_all_nodes)
                is GroupAddress -> network.group(address = destination.address)?.name
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
        HeartbeatSubscriptionDestinationsDropdownMenu(
            network = network,
            model = model,
            expanded = expanded,
            onDismissed = { expanded = !expanded },
            onDestinationSelected = {
                onDestinationSelected(it)
                expanded = false
            },
            onAddGroupClicked = {
                onAddGroupClicked()
                expanded = false
            }
        )
    }
}