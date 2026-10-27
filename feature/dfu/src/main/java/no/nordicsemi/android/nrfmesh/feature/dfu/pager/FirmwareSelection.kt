package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import android.content.res.Resources
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.SubdirectoryArrowRight
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.launch
import no.nordicsemi.android.common.ui.view.CircularIcon
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshNoItemsAvailable
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.dfu.R
import no.nordicsemi.android.nrfmesh.feature.dfu.util.FirmwareEntry
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Metadata
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Receiver
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Status
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Target
import no.nordicsemi.android.nrfmesh.feature.dfu.util.TargetState
import no.nordicsemi.android.nrfmesh.feature.dfu.util.UpdatePackage
import no.nordicsemi.kotlin.mesh.core.model.Node
import java.util.Locale

@Composable
internal fun FirmwareSelection(
    index: Int,
    maxReceiversListSize: Int,
    snackbarHostState: SnackbarHostState,
    onUpdatePackageChanged: (UpdatePackage?) -> Unit,
    onReceiversSelected: (List<Receiver>) -> Unit,
    onDistributorConnectedChanged: (Boolean) -> Unit,
) {
    // The state of the targets depends on the selected application key, so a different view model
    // is used for every key.
    val viewModel = hiltViewModel<FirmwareSelectionViewModel, FirmwareSelectionViewModel.Factory>(
        key = "firmware-selection"
    ) { factory ->
        factory.create(index = index)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            viewModel.loadZipPackage(uri, context.contentResolver)
        }
    }
    FileSelector(
        updatePackage = uiState.updatePackage,
        fileLauncher = fileLauncher,
    )
    val selectedReceivers = uiState.targets.mapNotNull { it.selectedReceiver }
    TargetNodes(
        snackbarHostState = snackbarHostState,
        messageState = uiState.messageState,
        metadata = uiState.updatePackage?.metadata,
        targets = uiState.targets,
        distributor = uiState.proxy,
        isDistributorConnected = uiState.isProxyConnected,
        selectedReceiversCount = selectedReceivers.size,
        maxReceiversListSize = maxReceiversListSize,
        applicationKeyName = uiState.applicationKeyName,
        downloadFirmwareInformation = viewModel::downloadFirmwareInformation,
        checkCompatibility = viewModel::checkCompatibility,
        downloadFirmwareUpdate = viewModel::downloadFirmwareUpdate,
        configure = viewModel::configure
    )
    // The Distributor is the node that is connected when the page is shown.
    LaunchedEffect(Unit) { viewModel.lockDistributor() }
    LaunchedEffect(uiState.isProxyConnected) {
        onDistributorConnectedChanged(uiState.isProxyConnected)
    }
    LaunchedEffect(uiState.updatePackage) {
        onUpdatePackageChanged(uiState.updatePackage)
    }
    LaunchedEffect(selectedReceivers) {
        onReceiversSelected(selectedReceivers)
    }
}

@Composable
private fun FileSelector(
    updatePackage: UpdatePackage?,
    fileLauncher: ActivityResultLauncher<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionTitle(
                modifier = Modifier.weight(weight = 1f),
                title = stringResource(R.string.label_firmware)
            )
            MeshIconButton(
                buttonIcon = Icons.Outlined.FolderOpen,
                onClick = dropUnlessResumed { fileLauncher.launch(input = "*/*") }
            )
        }
        ElevatedCardItem(
            imageVector = Icons.Outlined.FileOpen,
            title = updatePackage?.let {
                stringResource(R.string.label_file)
            } ?: stringResource(R.string.label_no_file),
            subtitle = updatePackage?.let {
                it.fileName ?: stringResource(R.string.label_unknown)
            }
        )
        AnimatedVisibility(
            visible = updatePackage?.fileName?.isNotEmpty() == true,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ElevatedCardItem(
                    imageVector = Icons.Outlined.DeveloperBoard,
                    title = stringResource(R.string.label_application),
                    subtitle = stringResource(
                        R.string.label_application_size,
                        updatePackage?.packageSize ?: 0
                    )
                )
                Text(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .padding(bottom = 8.dp),
                    text = stringResource(R.string.label_available_space, 500000),
                    style = MaterialTheme.typography.bodySmall
                )
                ElevatedCardItem(
                    imageVector = Icons.Outlined.WorkOutline,
                    title = stringResource(R.string.label_company),
                    subtitle = updatePackage?.metadata?.compositionData?.companyName
                        ?: stringResource(R.string.label_unknown)
                )
                ElevatedCardItem(
                    imageVector = Icons.Outlined.Numbers,
                    title = stringResource(R.string.label_version),
                    subtitle = updatePackage?.metadata?.signVersion?.toString()
                        ?: stringResource(R.string.label_unknown)
                )
                ElevatedCardItem(
                    imageVector = Icons.Outlined.AccountTree,
                    title = stringResource(R.string.label_metadata),
                    subtitle = updatePackage?.metadata?.metadataString
                        ?.uppercase(locale = Locale.ROOT)
                        ?: stringResource(R.string.label_unknown)
                )
            }
        }
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = stringResource(R.string.label_firmware_selection_description),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun TargetNodes(
    snackbarHostState: SnackbarHostState,
    messageState: MessageState,
    targets: List<Target>,
    distributor: Node?,
    isDistributorConnected: Boolean,
    selectedReceiversCount: Int,
    maxReceiversListSize: Int,
    applicationKeyName: String?,
    metadata: Metadata?,
    downloadFirmwareInformation: suspend (Target) -> Target,
    checkCompatibility: suspend (Target, Int, FirmwareEntry, Metadata, Boolean) -> Unit,
    downloadFirmwareUpdate: suspend (Target, Int, FirmwareEntry) -> Unit,
    configure: suspend (Target) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    // The connected Distributor is also a target, but it is shown in a section of its own.
    val distributorTarget = targets.firstOrNull { it.node.uuid == distributor?.uuid }
    val availableTargets = targets.filter { it.node.uuid != distributor?.uuid }
    var isRequestInProgress by rememberSaveable { mutableStateOf(false) }
    var isSelectAllInProgress by rememberSaveable { mutableStateOf(false) }
    // Expanded state of the rows is kept here so that it can also be changed by Select All. A row
    // without an entry is expanded if the target has a selected firmware entry.
    val expandedTargets = rememberSaveable(
        saver = listSaver(
            save = { map -> map.map { (id, expanded) -> "$id|$expanded" } },
            restore = { list ->
                mutableStateMapOf<String, Boolean>().apply {
                    list.forEach {
                        val (id, expanded) = it.split("|")
                        put(id, expanded.toBoolean())
                    }
                }
            }
        )
    ) { mutableStateMapOf<String, Boolean>() }
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SectionTitle(
                modifier = Modifier.weight(weight = 1f),
                title = stringResource(R.string.label_available_target_nodes)
            )
            MeshIconButton(
                buttonIcon = Icons.Outlined.SelectAll,
                onClick = {
                    metadata?.let { metadata ->
                        scope.launch {
                            isSelectAllInProgress = true
                            isRequestInProgress = true
                            try {
                                loop@ for (target in availableTargets) {
                                    if (target.isSelected)
                                        continue@loop
                                    val newTarget = loadFirmwareInformation(
                                        resources = resources,
                                        snackbarHostState = snackbarHostState,
                                        applicationKeyName = applicationKeyName,
                                        target = target,
                                        isExpanded = target.isSelected,
                                        onExpandStateChanged = {
                                            expandedTargets[target.node.uuid.toString()] = it
                                        },
                                        onConfigurationRequired = { isSelectAllInProgress = it },
                                        downloadFirmwareInformation = downloadFirmwareInformation,
                                        configure = configure
                                    ) ?: target
                                    if (newTarget.targetState is TargetState.Ready) {
                                        expandedTargets[target.node.uuid.toString()] = true
                                        newTarget.targetState.entries.forEachIndexed { index, entry ->
                                            selectFirmwareEntry(
                                                snackbarHostState = snackbarHostState,
                                                resources = resources,
                                                checked = true,
                                                target = newTarget,
                                                indexOfEntry = index,
                                                entry = entry,
                                                metadata = metadata,
                                                checkCompatibility = checkCompatibility,
                                                downloadFirmwareUpdate = downloadFirmwareUpdate
                                            )
                                        }
                                    }
                                }
                            } catch (ex: Exception) {
                                snackbarHostState.run {
                                    currentSnackbarData?.dismiss()
                                    showSnackbar(
                                        message = "Error while selecting all targets: ${ex.message}",
                                        withDismissAction = true
                                    )
                                }
                            } finally {
                                isSelectAllInProgress = false
                                isRequestInProgress = false
                            }
                        }
                    }
                },
                enabled = metadata != null && availableTargets.isNotEmpty() && !isRequestInProgress,
                isOnClickActionInProgress = isRequestInProgress && isSelectAllInProgress
            )
        }
        // The Distributor can only hold a limited number of receivers, the selection cannot be
        // used until it fits.
        if (selectedReceiversCount > maxReceiversListSize) {
            Spacer(modifier = Modifier.size(size = 8.dp))
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = stringResource(
                    R.string.label_receivers_limit_exceeded,
                    selectedReceiversCount,
                    maxReceiversListSize
                ),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        when (availableTargets.isEmpty()) {
            true -> MeshNoItemsAvailable(
                modifier = when (distributorTarget) {
                    null -> Modifier.fillMaxSize()
                    else -> Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                },
                imageVector = Icons.Outlined.AutoAwesome,
                title = stringResource(R.string.label_no_nodes_available)
            )

            false -> {
                Spacer(modifier = Modifier.size(size = 8.dp))
                Text(
                    text = stringResource(R.string.label_target_nodes_description),
                    style = MaterialTheme.typography.bodySmall
                )
                availableTargets.forEach { target ->
                    key(target.node.uuid.toString()) {
                        TargetNode(
                            snackbarHostState = snackbarHostState,
                            messageState = messageState,
                            target = target,
                            applicationKeyName = applicationKeyName,
                            metadata = metadata,
                            isExpanded = expandedTargets[target.node.uuid.toString()]
                                ?: target.isSelected,
                            onExpandStateChanged = {
                                expandedTargets[target.node.uuid.toString()] = it
                            },
                            isRequestInProgress = isRequestInProgress,
                            downloadFirmwareInformation = downloadFirmwareInformation,
                            checkCompatibility = checkCompatibility,
                            downloadFirmwareUpdate = downloadFirmwareUpdate,
                            configure = configure
                        )
                    }
                }
                Spacer(modifier = Modifier.size(size = 8.dp))
                Text(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = stringResource(R.string.label_node_firmware_details_description),
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.size(size = 8.dp))
                Text(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = stringResource(R.string.label_image_firmware_details_description),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        distributorTarget?.let { target ->
            Spacer(modifier = Modifier.size(size = 16.dp))
            SectionTitle(title = stringResource(R.string.label_distributor))
            key(target.node.uuid.toString()) {
                // A Distributor that is no longer connected is greyed out and cannot be used.
                Box(modifier = Modifier.alpha(alpha = if (isDistributorConnected) 1f else 0.38f)) {
                    TargetNode(
                        snackbarHostState = snackbarHostState,
                        messageState = messageState,
                        target = target,
                        applicationKeyName = applicationKeyName,
                        metadata = metadata,
                        isExpanded = expandedTargets[target.node.uuid.toString()]
                            ?: target.isSelected,
                        onExpandStateChanged = {
                            expandedTargets[target.node.uuid.toString()] = it
                        },
                        isRequestInProgress = isRequestInProgress,
                        downloadFirmwareInformation = downloadFirmwareInformation,
                        checkCompatibility = checkCompatibility,
                        downloadFirmwareUpdate = downloadFirmwareUpdate,
                        configure = configure
                    )
                    if (!isDistributorConnected) {
                        Box(modifier = Modifier.matchParentSize().pointerInput(Unit) {})
                    }
                }
            }
            Spacer(modifier = Modifier.size(size = 8.dp))
            Text(
                modifier = Modifier.padding(horizontal = 8.dp),
                text = stringResource(R.string.label_distributor_update_description),
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.size(size = 8.dp))
        }
    }
}

@Composable
private fun TargetNode(
    snackbarHostState: SnackbarHostState,
    messageState: MessageState,
    applicationKeyName: String?,
    metadata: Metadata?,
    target: Target,
    isExpanded: Boolean,
    onExpandStateChanged: (Boolean) -> Unit,
    isRequestInProgress: Boolean,
    downloadFirmwareInformation: suspend (Target) -> Target,
    checkCompatibility: suspend (Target, Int, FirmwareEntry, Metadata, Boolean) -> Unit,
    downloadFirmwareUpdate: suspend (Target, Int, FirmwareEntry) -> Unit,
    configure: suspend (Target) -> Unit,
) {
    Box(modifier = Modifier.padding(top = 8.dp)) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Images(
                snackbarHostState = snackbarHostState,
                metadata = metadata,
                target = target,
                isRequestInProgress = isRequestInProgress,
                checkCompatibility = checkCompatibility,
                downloadFirmwareUpdate = downloadFirmwareUpdate
            )
        }
        ElevatedCardItem(
            leadingIcon = {
                Spacer(modifier = Modifier.size(size = 16.dp))
                CircularIcon(painter = painterResource(R.drawable.ic_mesh))
                Spacer(modifier = Modifier.size(size = 16.dp))
            },
            title = target.node.name,
            titleAction = {
                ExpandRowAction(
                    snackbarHostState = snackbarHostState,
                    messageState = messageState,
                    applicationKeyName = applicationKeyName,
                    target = target,
                    isExpanded = isExpanded,
                    onExpandStateChanged = onExpandStateChanged,
                    downloadFirmwareInformation = downloadFirmwareInformation,
                    configure = configure
                )
            },
            subtitle = target.node.uuid.toString()
                .uppercase(locale = Locale.ROOT),
        )
    }
}

@Composable
private fun ExpandRowAction(
    snackbarHostState: SnackbarHostState,
    messageState: MessageState,
    applicationKeyName: String?,
    target: Target,
    isExpanded: Boolean,
    onExpandStateChanged: (Boolean) -> Unit,
    downloadFirmwareInformation: suspend (Target) -> Target,
    configure: suspend (Target) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    var isConfiguring by rememberSaveable { mutableStateOf(false) }
    MeshIconButton(
        modifier = Modifier
            .padding(end = 16.dp)
            .rotate(
                degrees = when {
                    isExpanded && target.targetState !is TargetState.ConfigurationRequired -> 180f
                    else -> 0f
                }
            ),
        buttonIcon = when (target.targetState) {
            is TargetState.ConfigurationRequired -> Icons.Rounded.WarningAmber
            else -> Icons.Rounded.ArrowDropDown
        },
        buttonIconTint = when (target.targetState) {
            is TargetState.ConfigurationRequired -> Color(
                red = 1f,
                green = 0.6f,
                blue = 0f
            )

            else -> MaterialTheme.colorScheme.primary
        },
        onClick = dropUnlessResumed {
            scope.launch {
                val _ = loadFirmwareInformation(
                    resources = resources,
                    snackbarHostState = snackbarHostState,
                    applicationKeyName = applicationKeyName,
                    target = target,
                    isExpanded = isExpanded,
                    onExpandStateChanged = onExpandStateChanged,
                    onConfigurationRequired = { isConfiguring = it },
                    downloadFirmwareInformation = downloadFirmwareInformation,
                    configure = configure
                )
            }
        },
        isOnClickActionInProgress = isConfiguring,
        enabled = !messageState.isInProgress() && !isConfiguring
    )
}

private suspend fun loadFirmwareInformation(
    resources: Resources,
    snackbarHostState: SnackbarHostState,
    applicationKeyName: String?,
    target: Target,
    isExpanded: Boolean,
    onExpandStateChanged: (Boolean) -> Unit,
    onConfigurationRequired: (Boolean) -> Unit,
    downloadFirmwareInformation: suspend (Target) -> Target,
    configure: suspend (Target) -> Unit,
): Target? {
    return when (target.targetState) {
        is TargetState.ConfigurationRequired -> {
            snackbarHostState.run {
                currentSnackbarData?.dismiss()
                val result = showSnackbar(
                    message = resources.getString(
                        R.string.label_configuration_required,
                        applicationKeyName ?: resources.getString(R.string.label_unknown)
                    ),
                    withDismissAction = true,
                    actionLabel = resources.getString(R.string.label_confirm)
                )
                if (result == SnackbarResult.ActionPerformed) {
                    onConfigurationRequired(true)
                    configure(target)
                    onConfigurationRequired(false)
                }
            }
            null
        }

        is TargetState.Configured,
            -> {
            onConfigurationRequired(true)
            val updatedTarget = downloadFirmwareInformation(target)
                .takeIf { it.targetState is TargetState.Ready }
                ?.let {
                    onExpandStateChanged(true)
                    it
                }
            onConfigurationRequired(false)
            updatedTarget ?: target
        }

        is TargetState.Ready -> {
            onExpandStateChanged(!isExpanded)
            target
        }

        else -> null
    }
}

@Composable
private fun Images(
    snackbarHostState: SnackbarHostState,
    metadata: Metadata?,
    target: Target,
    isRequestInProgress: Boolean,
    checkCompatibility: suspend (Target, Int, FirmwareEntry, Metadata, Boolean) -> Unit,
    downloadFirmwareUpdate: suspend (Target, Int, FirmwareEntry) -> Unit,
) {
    val entries = (target.targetState as? TargetState.Ready)
        ?.entries
        .orEmpty()
    Column(modifier = Modifier.padding(top = 72.dp)) {
        entries.forEachIndexed { indexOfEntry, entry ->
            ImageRow(
                snackbarHostState = snackbarHostState,
                metadata = metadata,
                target = target,
                indexOfEntry = indexOfEntry,
                entry = entry,
                isRequestInProgress = isRequestInProgress,
                checkCompatibility = checkCompatibility,
                downloadFirmwareUpdate = downloadFirmwareUpdate
            )
        }
    }
}

@Composable
private fun ImageRow(
    snackbarHostState: SnackbarHostState,
    metadata: Metadata?,
    target: Target,
    indexOfEntry: Int,
    entry: FirmwareEntry,
    isRequestInProgress: Boolean,
    checkCompatibility: suspend (Target, Int, FirmwareEntry, Metadata, Boolean) -> Unit,
    downloadFirmwareUpdate: suspend (Target, Int, FirmwareEntry) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val resources = LocalResources.current
    val entries = (target.targetState as? TargetState.Ready)
        ?.entries
        .orEmpty()
    var isCompatibilityCheckInProgress by remember { mutableStateOf(false) }
    key(target.node.uuid.toString() + entry.index) {
        val title = stringResource(
            R.string.label_image_value,
            entry.index.toInt() + indexOfEntry
        )
        ElevatedCardItem(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp),
            imageVector = Icons.Rounded.SubdirectoryArrowRight,
            shape = if (indexOfEntry < entries.size - 1) MaterialTheme.shapes.medium.copy(
                all = CornerSize(size = 0.dp)
            ) else MaterialTheme.shapes.medium.copy(
                topStart = CornerSize(size = 0.dp),
                topEnd = CornerSize(size = 0.dp)
            ),
            title = title,
            titleAction = {
                when {
                    isCompatibilityCheckInProgress || isRequestInProgress ->
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .size(size = 24.dp)
                        )

                    target.targetState is TargetState.Error -> Icon(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        imageVector = Icons.Rounded.SubdirectoryArrowRight,
                        contentDescription = null
                    )

                    else -> Checkbox(
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                        checked = target.isSelected,
                        onCheckedChange = { checked ->
                            scope.launch {
                                isCompatibilityCheckInProgress = true
                                selectFirmwareEntry(
                                    snackbarHostState = snackbarHostState,
                                    resources = resources,
                                    checked = checked,
                                    target = target,
                                    indexOfEntry = indexOfEntry,
                                    entry = entry,
                                    metadata = metadata,
                                    checkCompatibility = checkCompatibility,
                                    downloadFirmwareUpdate = downloadFirmwareUpdate
                                )
                                isCompatibilityCheckInProgress = false
                            }
                        },
                        enabled = !isRequestInProgress
                    )
                }
            },
            subtitle = target.node.name,
            subtitlesMaxLines = 1,
            body = entry.availableUpdate?.let {
                {
                    Text(
                        text = resources.getString(R.string.label_update_available_for_download),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )

        LaunchedEffect(entry.status) {
            if (entry.status is Status.Error) {
                snackbarHostState.run {
                    currentSnackbarData?.dismiss()
                    showSnackbar(
                        message = entry.status.message,
                        withDismissAction = true
                    )
                }
            }
        }
    }
}

private suspend fun selectFirmwareEntry(
    snackbarHostState: SnackbarHostState,
    resources: Resources,
    checked: Boolean,
    target: Target,
    indexOfEntry: Int,
    entry: FirmwareEntry,
    metadata: Metadata?,
    checkCompatibility: suspend (Target, Int, FirmwareEntry, Metadata, Boolean) -> Unit,
    downloadFirmwareUpdate: suspend (Target, Int, FirmwareEntry) -> Unit,
) {
    if (!checked) {
        checkCompatibility(
            target,
            indexOfEntry,
            entry,
            metadata!!,
            false
        )
    } else {
        entry.availableUpdate?.manifest?.firmware?.firmwareId
            ?.let { newFirmwareId ->
                // Check if a manifest is available
                metadata?.firmwareId?.let { selectedFirmwareId ->
                    if (selectedFirmwareId != newFirmwareId) {
                        snackbarHostState.run {
                            currentSnackbarData?.dismiss()
                            val result = showSnackbar(
                                message = resources.getString(R.string.label_firmware_available),
                                actionLabel = resources.getString(R.string.label_download),
                                withDismissAction = true
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                try {
                                    downloadFirmwareUpdate(
                                        target,
                                        indexOfEntry,
                                        entry,
                                    )
                                } catch (ex: Exception) {
                                    showSnackbar(
                                        message = "Error while downloading firmware: ${ex.message}",
                                        withDismissAction = true
                                    )
                                }
                            }
                        }
                    } else {
                        checkCompatibility(
                            target,
                            indexOfEntry,
                            entry,
                            metadata,
                            true
                        )
                    }
                } ?: run {
                    try {
                        downloadFirmwareUpdate(
                            target,
                            indexOfEntry,
                            entry,
                        )
                    } catch (ex: Exception) {
                        snackbarHostState.run {
                            currentSnackbarData?.dismiss()
                            showSnackbar(
                                message = "Error while downloading firmware: ${ex.message}",
                                withDismissAction = true
                            )
                        }
                    }
                }
            }
            ?: run {
                metadata?.let {
                    checkCompatibility(
                        target,
                        indexOfEntry,
                        entry,
                        metadata,
                        true
                    )
                } ?: run {
                    snackbarHostState.run {
                        currentSnackbarData?.dismiss()
                        showSnackbar(
                            message = resources.getString(R.string.label_select_a_firmware_file_first),
                            withDismissAction = true
                        )
                    }
                }
            }
    }
}