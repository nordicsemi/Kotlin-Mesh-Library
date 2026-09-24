package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import android.content.ContentResolver
import android.content.res.Resources
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.CoroutineScope
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
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Status
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Target
import no.nordicsemi.android.nrfmesh.feature.dfu.util.TargetState
import no.nordicsemi.android.nrfmesh.feature.dfu.util.UpdatePackage
import java.util.Locale
import kotlin.uuid.ExperimentalUuidApi

@Composable
internal fun Page2(
    index: Int,
    snackbarHostState: SnackbarHostState,
) {
    val viewModel = hiltViewModel<Page2ViewModel, Page2ViewModel.Factory> { factory ->
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
    TargetNodes(
        snackbarHostState = snackbarHostState,
        messageState = uiState.messageState,
        metadata = uiState.updatePackage?.metadata,
        targets = uiState.targets,
        downloadFirmwareInformation = viewModel::downloadFirmwareInformation,
        checkCompatibility = viewModel::checkCompatibility,
        downloadFirmwareUpdate = viewModel::downloadFirmwareUpdate
    )
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
            title = stringResource(R.string.label_file),
            subtitle = updatePackage?.fileName ?: stringResource(R.string.label_unknown)
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

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun TargetNodes(
    snackbarHostState: SnackbarHostState,
    messageState: MessageState,
    targets: List<Target>,
    metadata: Metadata?,
    downloadFirmwareInformation: suspend (Target) -> Target,
    checkCompatibility: suspend (Target, Int, FirmwareEntry, Metadata, Boolean) -> Unit,
    downloadFirmwareUpdate: suspend (Target, Int, FirmwareEntry) -> Unit,
) {
    var isRequestInProgress by rememberSaveable { mutableStateOf(false) }
    var isSelectAllInProgress by rememberSaveable { mutableStateOf(false) }
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
                onClick = dropUnlessResumed {
                    // TODO Select All
                },
                enabled = targets.isNotEmpty() && !isRequestInProgress,
                isOnClickActionInProgress = isRequestInProgress && isSelectAllInProgress
            )
        }
        when (targets.isEmpty()) {
            true -> MeshNoItemsAvailable(
                modifier = Modifier.fillMaxSize(),
                imageVector = Icons.Outlined.AutoAwesome,
                title = stringResource(R.string.label_no_nodes_available)
            )

            false -> {
                Spacer(modifier = Modifier.size(size = 8.dp))
                Text(
                    text = stringResource(R.string.label_target_nodes_description),
                    style = MaterialTheme.typography.bodySmall
                )
                targets.forEach { target ->
                    key(target.node.uuid.toString()) {
                        TargetNode(
                            snackbarHostState = snackbarHostState,
                            messageState = messageState,
                            target = target,
                            metadata = metadata,
                            isRequestInProgress = isRequestInProgress,
                            downloadFirmwareInformation = downloadFirmwareInformation,
                            checkCompatibility = checkCompatibility,
                            downloadFirmwareUpdate = downloadFirmwareUpdate
                        )
                    }
                }
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
        Spacer(modifier = Modifier.size(size = 8.dp))
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = stringResource(R.string.label_distributor_update_description),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun TargetNode(
    snackbarHostState: SnackbarHostState,
    messageState: MessageState,
    metadata: Metadata?,
    target: Target,
    isRequestInProgress: Boolean = false,
    downloadFirmwareInformation: suspend (Target) -> Target,
    checkCompatibility: suspend (Target, Int, FirmwareEntry, Metadata, Boolean) -> Unit,
    downloadFirmwareUpdate: suspend (Target, Int, FirmwareEntry) -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(target.isSelected) }
    Box(modifier = Modifier.padding(top = 8.dp)) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Images(
                snackbarHostState = snackbarHostState,
                messageState = messageState,
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
                    messageState = messageState,
                    target = target,
                    isExpanded = isExpanded,
                    onExpandStateChanged = { isExpanded = it },
                    downloadFirmwareInformation = downloadFirmwareInformation
                )
            },
            subtitle = target.node.uuid.toString()
                .uppercase(locale = Locale.ROOT),
        )
    }
}

@Composable
private fun ExpandRowAction(
    messageState: MessageState,
    target: Target,
    isExpanded: Boolean,
    onExpandStateChanged: (Boolean) -> Unit,
    downloadFirmwareInformation: suspend (Target) -> Target,
) {
    val scope = rememberCoroutineScope()
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
            when (target.targetState) {
                is TargetState.ConfigurationRequired,
                is TargetState.Configured,
                    -> scope.launch {
                    isConfiguring = true
                    downloadFirmwareInformation(target)
                        .takeIf { it.targetState is TargetState.Ready }
                        ?.let { onExpandStateChanged(true) }
                    isConfiguring = false
                }

                is TargetState.Ready -> onExpandStateChanged(!isExpanded)

                else -> {}
            }
        },
        isOnClickActionInProgress = isConfiguring,
        enabled = !messageState.isInProgress() && !isConfiguring
    )
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun Images(
    snackbarHostState: SnackbarHostState,
    messageState: MessageState,
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
                messageState = messageState,
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

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun ImageRow(
    snackbarHostState: SnackbarHostState,
    messageState: MessageState,
    metadata: Metadata?,
    target: Target,
    indexOfEntry: Int,
    entry: FirmwareEntry,
    isRequestInProgress: Boolean,
    checkCompatibility: suspend (Target, Int, FirmwareEntry, Metadata, Boolean) -> Unit,
    downloadFirmwareUpdate: suspend (Target, Int, FirmwareEntry) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
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
                    isCompatibilityCheckInProgress && isRequestInProgress ->
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
                            onCheckedChange(
                                scope = scope,
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
                        },
                        enabled = !isRequestInProgress
                    )
                }
            },
            subtitle = target.node.name
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

private fun onCheckedChange(
    scope: CoroutineScope,
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
    scope.launch {
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
}

private fun ContentResolver.fileName(uri: Uri) = when (uri.scheme) {
    ContentResolver.SCHEME_CONTENT -> query(
        uri,
        arrayOf(OpenableColumns.DISPLAY_NAME),
        null, null, null,
    )?.use { cursor ->
        val i = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (i >= 0 && cursor.moveToFirst()) cursor.getString(i) else null
    }

    else -> ""
}