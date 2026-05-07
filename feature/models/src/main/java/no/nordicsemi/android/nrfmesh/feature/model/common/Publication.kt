package no.nordicsemi.android.nrfmesh.feature.model.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SportsScore
import androidx.compose.material.icons.outlined.Timer
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import no.nordicsemi.android.common.ui.view.NordicSliderDefaults
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.name
import no.nordicsemi.android.nrfmesh.core.common.publishDestination
import no.nordicsemi.android.nrfmesh.core.common.publishKey
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItemTextField
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshSingleLineListItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshTwoLineListItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.model.configurationserver.toFloat
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationVirtualAddressSet
import no.nordicsemi.kotlin.mesh.core.model.AllFriends
import no.nordicsemi.kotlin.mesh.core.model.AllNodes
import no.nordicsemi.kotlin.mesh.core.model.AllProxies
import no.nordicsemi.kotlin.mesh.core.model.AllRelays
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.Credentials
import no.nordicsemi.kotlin.mesh.core.model.FriendshipSecurity
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.MasterSecurity
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.PublicationAddress
import no.nordicsemi.kotlin.mesh.core.model.Publish
import no.nordicsemi.kotlin.mesh.core.model.PublishPeriod
import no.nordicsemi.kotlin.mesh.core.model.Retransmit
import no.nordicsemi.kotlin.mesh.core.model.StepResolution
import no.nordicsemi.kotlin.mesh.core.model.UnassignedAddress
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import no.nordicsemi.kotlin.mesh.core.model.fixedGroupAddresses
import kotlin.math.roundToInt
import kotlin.time.DurationUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Publication(
    messageState: MessageState,
    model: Model,
    send: (AcknowledgedConfigMessage) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var destination by remember { mutableStateOf(model.publish?.address) }
    var keyIndex by remember { mutableIntStateOf(model.publish?.index?.toInt() ?: 0) }
    var ttl by remember { mutableIntStateOf(model.publish?.ttl?.toInt() ?: 5) }
    var publishPeriod by remember {
        mutableStateOf(model.publish?.period ?: PublishPeriod.disabled)
    }
    var credentials by remember {
        mutableStateOf(model.publish?.credentials ?: MasterSecurity)
    }
    var retransmit by remember { mutableStateOf(model.publish?.retransmit) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle(
            modifier = Modifier
                .weight(weight = 1f)
                .padding(horizontal = 16.dp),
            title = stringResource(R.string.label_publication)
        )
        MeshIconButton(
            onClick = { send(ConfigModelPublicationGet(model = model)) },
            buttonIcon = Icons.Outlined.Refresh,
            enabled = !messageState.isInProgress(),
            isOnClickActionInProgress = messageState.isInProgress() &&
                    messageState.message is ConfigModelPublicationGet,
        )
        MeshIconButton(
            onClick = { send(ConfigModelPublicationSet(model = model)) },
            buttonIcon = Icons.Outlined.Delete,
            isOnClickActionInProgress = messageState.isInProgress() &&
                    (messageState.message as? ConfigModelPublicationSet)?.publish?.isCanceled == true,
            enabled = !messageState.isInProgress(),
        )
        MeshIconButton(
            onClick = { showBottomSheet = true },
            buttonIcon = Icons.Outlined.Add,
            enabled = !messageState.isInProgress(),
            isOnClickActionInProgress = messageState.isInProgress() &&
                  ((messageState.message as? ConfigModelPublicationSet)?.publish?.isCanceled == false ||
                    messageState.message is ConfigModelPublicationVirtualAddressSet),
        )
    }

    if (model.publish != null) {
        ElevatedCardItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            imageVector = Icons.Outlined.SportsScore,
            title = model.publishDestination() ?: stringResource(R.string.label_unknown),
            subtitle = model.publishKey()?.name ?: stringResource(R.string.label_unknown)
        )
    } else {
        ElevatedCardItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            imageVector = Icons.Outlined.SportsScore,
            title = stringResource(R.string.label_no_publication)
        )
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            containerColor = MaterialTheme.colorScheme.surface,
            sheetState = bottomSheetState,
            onDismissRequest = { showBottomSheet = !showBottomSheet },
            content = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = {
                        SectionTitle(
                            modifier = Modifier
                                .weight(weight = 1f)
                                .padding(horizontal = 16.dp),
                            title = stringResource(R.string.label_publication),
                            style = MaterialTheme.typography.titleMedium
                        )
                        MeshOutlinedButton(
                            modifier = Modifier.padding(end = 16.dp),
                            enabled = destination != null,
                            onClick = {
                                send(
                                    if (destination is VirtualAddress)
                                        ConfigModelPublicationVirtualAddressSet(
                                            publish = Publish(
                                                address = destination!!,
                                                index = keyIndex.toUShort(),
                                                ttl = ttl.toUByte(),
                                                period = publishPeriod,
                                                credentials = credentials,
                                                retransmit = retransmit ?: Retransmit.disabled
                                            ),
                                            model = model
                                        )
                                    else
                                        ConfigModelPublicationSet(
                                            model = model,
                                            publish = Publish(
                                                address = destination!!,
                                                index = keyIndex.toUShort(),
                                                ttl = ttl.toUByte(),
                                                period = publishPeriod,
                                                credentials = credentials,
                                                retransmit = retransmit ?: Retransmit.disabled
                                            )
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp)
                        .verticalScroll(state = rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(space = 8.dp)
                ) {
                    ApplicationKeys(
                        keys = model.parentElement?.parentNode?.applicationKeys.orEmpty(),
                        selectedKeyIndex = keyIndex,
                        onApplicationKeySelected = { keyIndex = it }
                    )
                    SectionTitle(
                        modifier = Modifier
                            .weight(weight = 1f)
                            .padding(horizontal = 16.dp),
                        title = stringResource(R.string.label_destination)
                    )
                    Destination(
                        model = model,
                        destination = destination,
                        onDestinationSelected = { destination = it }
                    )
                    Ttl(ttl = ttl, onTtlChanged = { ttl = it })
                    PeriodicPublishingInterval(
                        publishPeriod = publishPeriod,
                        onPeriodChanged = { publishPeriod = it }
                    )
                    FriendshipCredential(
                        credentials = credentials,
                        onCredentialsChanged = { credentials = it }
                    )
                    RetransmissionCountAndInterval(
                        retransmit = retransmit,
                        onRetransmitChanged = { retransmit = it }
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Destination(
    model: Model,
    destination: PublicationAddress?,
    onDestinationSelected: (PublicationAddress) -> Unit,
) {
    val network = model.parentElement?.parentNode?.network ?: return
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
                    .node(address = destination.address)
                    ?.name
                    ?: destination.toHexString()

                is AllRelays -> stringResource(R.string.label_all_relays)
                is AllFriends -> stringResource(R.string.label_all_friends)
                is AllProxies -> stringResource(R.string.label_all_proxies)
                is AllNodes -> stringResource(R.string.label_all_nodes)
                is GroupAddress -> network
                    .group(address = destination.address)?.name
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
        DropdownMenu(
            modifier = Modifier.exposedDropdownSize(),
            expanded = expanded,
            onDismissRequest = { expanded = !expanded },
            content = {
                model.parentElement?.parentNode?.network?.let { network ->
                    Text(
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                        text = stringResource(R.string.label_unicast_destinations)
                    )
                    network.nodes.forEach { node ->
                        var isListExpanded by rememberSaveable { mutableStateOf(false) }
                        MeshSingleLineListItem(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            imageVector = Icons.Outlined.SportsScore,
                            title = node.name,
                            trailingComposable = {
                                IconButton(
                                    onClick = { isListExpanded = !isListExpanded },
                                    content = {
                                        Icon(
                                            modifier = Modifier.rotate(
                                                if (isListExpanded) 180f else 0f
                                            ),
                                            imageVector = Icons.Outlined.ArrowDropDown,
                                            contentDescription = null
                                        )
                                    }
                                )
                            }
                        )
                        if (isListExpanded) {
                            node.elements.forEach { element ->
                                MeshTwoLineListItem(
                                    modifier = Modifier
                                        .exposedDropdownSize()
                                        .clickable {
                                            onDestinationSelected(element.unicastAddress as PublicationAddress)
                                            isListExpanded = !isListExpanded
                                            expanded = !expanded
                                        },
                                    leadingComposable = {
                                        Spacer(modifier = Modifier.width(width = 56.dp))
                                    },
                                    title = element.name
                                        ?: stringResource(R.string.label_unknown),
                                    subtitle = element.unicastAddress.toHexString()
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                    Text(
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                        text = stringResource(R.string.label_groups)
                    )
                    network
                        .groups
                        .takeIf { it.isNotEmpty() }
                        ?.let {
                            it.forEach { destination ->
                                MeshSingleLineListItem(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .clickable {
                                            onDestinationSelected(destination.address as PublicationAddress)
                                            expanded = !expanded
                                        },
                                    imageVector = Icons.Outlined.SportsScore,
                                    title = destination.name
                                )
                            }
                        }
                    MeshSingleLineListItem(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .clickable {

                            },
                        imageVector = Icons.Outlined.Add,
                        title = stringResource(R.string.label_add_group)
                    )
                    HorizontalDivider()
                    Text(
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                        text = stringResource(R.string.label_fixed_group_addresses)
                    )
                    fixedGroupAddresses.forEach {destination ->
                        MeshSingleLineListItem(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .clickable {
                                    onDestinationSelected(destination as PublicationAddress)
                                    expanded = !expanded
                                },
                            imageVector = Icons.Outlined.GroupWork,
                            title = destination.name()
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApplicationKeys(
    keys: List<ApplicationKey>,
    selectedKeyIndex: Int,
    onApplicationKeySelected: (Int) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        modifier = Modifier.padding(horizontal = 16.dp),
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        ElevatedCardItem(
            modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            onClick = { expanded = true },
            imageVector = Icons.Outlined.VpnKey,
            title = stringResource(R.string.label_application_key),
            titleAction = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    expanded = expanded
                )
            },
            subtitle = keys.firstOrNull {
                it.index == selectedKeyIndex.toUShort()
            }?.name ?: stringResource(R.string.label_unknown)
        )
        DropdownMenu(
            modifier = Modifier.exposedDropdownSize(),
            expanded = expanded,
            onDismissRequest = { expanded = !expanded },
            content = {
                keys.forEachIndexed { index, key ->
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
                            onApplicationKeySelected(key.index.toInt())
                                .also { expanded = !expanded }
                        }
                    )
                    if (index < keys.size - 1)
                        HorizontalDivider()
                }
            }
        )
    }
}

@Composable
private fun Ttl(ttl: Int, onTtlChanged: (Int) -> Unit) {
    ElevatedCardItemTextField(
        modifier = Modifier.padding(horizontal = 16.dp),
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
private fun PeriodicPublishingInterval(
    publishPeriod: PublishPeriod?,
    onPeriodChanged: (PublishPeriod) -> Unit,
) {
    val minPublicationInterval = 0f
    val maxPublicationInterval = 239f
    var progress by rememberSaveable { mutableIntStateOf(0) }
    var steps by remember { mutableIntStateOf(publishPeriod?.steps?.toInt() ?: 0) }
    var resolution by remember {
        mutableStateOf(publishPeriod?.resolution ?: StepResolution.HUNDREDS_OF_MILLISECONDS)
    }
    var resource by rememberSaveable { mutableIntStateOf(R.string.label_disabled) }
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Timer,
        title = stringResource(R.string.label_periodic_publishing_interval),
        supportingText = stringResource(R.string.label_periodic_publishing_interval_rationale),
        body = {
            Slider(
                modifier = Modifier.padding(start = 42.dp),
                value = progress.toFloat(),
                onValueChange = {
                    when (it) {
                        in 0f..<1f -> {
                            steps = 0
                            resolution = StepResolution.HUNDREDS_OF_MILLISECONDS
                            resource = R.string.label_disabled
                        }

                        in 1f..<10f -> {
                            steps = it.roundToInt()
                            resolution = StepResolution.HUNDREDS_OF_MILLISECONDS
                            resource = R.string.label_time_ms
                        }

                        in 10f..<64f -> {
                            steps = it.roundToInt()
                            resolution = StepResolution.HUNDREDS_OF_MILLISECONDS
                            resource = R.string.label_time_ms
                        }

                        in 64f..<117f -> {
                            steps = it.roundToInt() - 57
                            resolution = StepResolution.SECONDS
                            resource = R.string.label_time_s
                        }

                        in 117f..<121f -> {
                            steps = it.roundToInt() - 57
                            resolution = StepResolution.SECONDS
                            resource = R.string.label_time_s
                        }

                        in 121f..<178f -> {
                            steps = it.roundToInt() - 114
                            resolution = StepResolution.TENS_OF_SECONDS
                            resource = R.string.label_time_m
                        }

                        in 178f..<182f -> {
                            steps = it.roundToInt() - 176
                            resolution = StepResolution.TENS_OF_MINUTES
                            resource = R.string.label_time_m
                        }

                        in 182f..239f -> {
                            steps = it.roundToInt() - 176
                            resolution = StepResolution.TENS_OF_MINUTES
                            resource = R.string.label_time_h_m
                        }
                    }
                    progress = it.roundToInt()
                    onPeriodChanged(PublishPeriod(steps.toUByte(), resolution))
                },
                valueRange = minPublicationInterval..maxPublicationInterval,
                steps = 237,
                colors = NordicSliderDefaults.colors()
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .sizeIn(minWidth = 80.dp),
                text = when (progress) {
                    0 -> stringResource(R.string.label_disabled)
                    else -> PublishPeriod(
                        steps.toUByte(),
                        resolution
                    ).interval.toString()
                },
                textAlign = TextAlign.End
            )
        }
    )
}

@Composable
private fun FriendshipCredential(
    credentials: Credentials,
    onCredentialsChanged: (Credentials) -> Unit,
) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Shield,
        title = stringResource(id = R.string.label_friendship_credentials_flag),
        titleAction = {
            Switch(
                modifier = Modifier.padding(horizontal = 16.dp),
                checked = credentials is FriendshipSecurity,
                onCheckedChange = {
                    onCredentialsChanged(
                        if (it) FriendshipSecurity else MasterSecurity
                    )
                }
            )
        },
    )
}

@Composable
private fun RetransmissionCountAndInterval(
    retransmit: Retransmit?,
    onRetransmitChanged: (Retransmit) -> Unit,
) {
    var count by remember { mutableIntStateOf(retransmit?.count?.toInt() ?: 0) }
    var steps by remember { mutableIntStateOf(retransmit?.steps?.toInt() ?: 0) }
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Timer,
        title = stringResource(R.string.title_retransmit_count_and_interval),
        supportingText = stringResource(R.string.label_retransmit_count_and_interval_rationale),
        body = {
            Slider(
                modifier = Modifier.padding(start = 42.dp),
                value = count.toFloat(),
                onValueChange = {
                    count = it.roundToInt()
                    onRetransmitChanged(Retransmit(count.toUByte(), steps.toUByte()))
                },
                valueRange = RETRANSMIT_COUNT_RANGE.toFloat(),
                steps = 5,
                colors = NordicSliderDefaults.colors()
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .sizeIn(minWidth = 80.dp),
                text = when (count.toUByte()) {
                    Retransmit.MIN_RETRANSMIT_COUNT ->
                        stringResource(R.string.label_disabled)

                    else -> "$count time(s)"
                },
                textAlign = TextAlign.End
            )
            Slider(
                modifier = Modifier.padding(start = 42.dp, top = 8.dp),
                value = if (count > 0) steps.toFloat() else {
                    steps = 0
                    0f
                },
                enabled = count.toUByte() != Retransmit.MIN_RETRANSMIT_COUNT,
                onValueChange = {
                    steps = it.roundToInt()
                    onRetransmitChanged(Retransmit(count.toUByte(), steps.toUByte()))
                },
                valueRange = 0f..31f,
                steps = 29,
                colors = NordicSliderDefaults.colors()
            )
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp)
                    .sizeIn(minWidth = 80.dp),
                text = when {
                    count.toUByte() == Retransmit.MIN_RETRANSMIT_COUNT ->
                        stringResource(R.string.label_na)

                    else -> retransmit?.interval?.toString(DurationUnit.MILLISECONDS)
                        ?: stringResource(R.string.label_na)
                },
                textAlign = TextAlign.End
            )
        }
    )
}

/**
 * Returns the list of possible addresses that can be selected as a destination address for a
 * publication message for a given non ConfigurationServer Model.
 */
fun Model.groupPublicationDestinations(network: MeshNetwork): List<PublicationAddress> {
    val groups = network.groups.map { it.address as PublicationAddress }
    return groups + listOf<PublicationAddress>(
        AllRelays, AllFriends, AllProxies, AllNodes
    )
}

val RETRANSMIT_COUNT_RANGE =
    Retransmit.MIN_RETRANSMIT_COUNT.toInt()..Retransmit.MAX_RETRANSMIT_COUNT.toInt()