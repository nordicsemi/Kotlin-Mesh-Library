package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material3.Text
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
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshNoItemsAvailable
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.dfu.R
import no.nordicsemi.android.nrfmesh.feature.dfu.util.FirmwareEntry
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
    var metadata by remember { mutableStateOf("") }
    FileSelector(
        updateZipPackage = viewModel::importZipPackage,
        onMetadataChanged = { metadata = it }
    )
    TargetNodes(
        snackbarHostState = snackbarHostState,
        metadata = metadata,
        targets = uiState.targets,
        downloadFirmwareInformation = viewModel::downloadFirmwareInformation,
        checkCompatibility = viewModel::checkCompatibility
    )
}

@Composable
private fun FileSelector(
    updateZipPackage: (uri: Uri, contentResolver: ContentResolver) -> UpdatePackage,
    onMetadataChanged: (String) -> Unit,
) {
    val context = LocalContext.current
    val resource = LocalResources.current
    var fileName by rememberSaveable { mutableStateOf("") }
    var fileSize by rememberSaveable { mutableIntStateOf(0) }
    var company by rememberSaveable { mutableStateOf("") }
    var version by rememberSaveable { mutableStateOf("") }
    var metadata by rememberSaveable { mutableStateOf("") }
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                fileName = context.contentResolver.fileName(it) ?: ""
                val updatePackage = updateZipPackage(uri, context.contentResolver)
                fileSize = updatePackage.zipPackage
                    .getBinaries()
                    .images
                    .firstOrNull()
                    ?.image
                    ?.data
                    ?.size
                    ?: 0
                company = updatePackage.metadata.compositionData?.companyName
                    ?: resource.getString(R.string.label_unknown)
                version = updatePackage.metadata.signVersion.toString()
                metadata = updatePackage.metadata.encodedMetadata
                    ?.uppercase(locale = Locale.ROOT)
                    ?: resource.getString(R.string.label_unknown)
                onMetadataChanged(updatePackage.metadata.encodedMetadata ?: "")
            } catch (e: Exception) {

            }
        }
    }
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
            subtitle = fileName
        )
        AnimatedVisibility(
            visible = fileName.isNotEmpty(),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ElevatedCardItem(
                    imageVector = Icons.Outlined.DeveloperBoard,
                    title = stringResource(R.string.label_application),
                    subtitle = stringResource(R.string.label_application_size, fileSize)
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
                    subtitle = company
                )
                ElevatedCardItem(
                    imageVector = Icons.Outlined.Numbers,
                    title = stringResource(R.string.label_version),
                    subtitle = version
                )
                ElevatedCardItem(
                    imageVector = Icons.Outlined.AccountTree,
                    title = stringResource(R.string.label_metadata),
                    subtitle = metadata
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
    metadata: String,
    targets: List<Target>,
    downloadFirmwareInformation: suspend (Target) -> Unit,
    checkCompatibility: suspend (Target, FirmwareEntry, Int, ByteArray, Boolean) -> Unit,
) {
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

                }
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
                var isRequestInProgress by rememberSaveable { mutableStateOf(false) }
                targets.forEach { target ->
                    key(target.node.uuid.toString()) {
                        TargetNode(
                            snackbarHostState = snackbarHostState,
                            metadata = metadata,
                            target = target,
                            isRequestInProgress = isRequestInProgress,
                            downloadFirmwareInformation = { target ->
                                isRequestInProgress = !isRequestInProgress
                                downloadFirmwareInformation(target)
                                isRequestInProgress = !isRequestInProgress
                            },
                            checkCompatibility = { target, firmwareEntry, indexOfEntry, metadata, isSelected ->
                                isRequestInProgress = isSelected
                                checkCompatibility(
                                    target,
                                    firmwareEntry,
                                    indexOfEntry,
                                    metadata,
                                    isSelected
                                )
                                isRequestInProgress = false
                            }
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
    metadata: String,
    target: Target,
    downloadFirmwareInformation: suspend (Target) -> Unit,
    isRequestInProgress: Boolean = false,
    checkCompatibility: suspend (Target, FirmwareEntry, Int, ByteArray, Boolean) -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
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
                checkCompatibility = checkCompatibility
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
                TitleAction(
                    target = target,
                    downloadFirmwareInformation = downloadFirmwareInformation,
                    isExpanded = isExpanded,
                    onExpandStateChanged = { isExpanded = it },
                    isRequestInProgress = isRequestInProgress
                )
            },
            subtitle = target.node.uuid.toString()
                .uppercase(locale = Locale.ROOT),
        )
    }
}

@Composable
private fun TitleAction(
    target: Target,
    downloadFirmwareInformation: suspend (Target) -> Unit,
    isExpanded: Boolean,
    onExpandStateChanged: (Boolean) -> Unit,
    isRequestInProgress: Boolean,
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
                    -> {
                    scope.launch {
                        isConfiguring = !isConfiguring
                        downloadFirmwareInformation(target)
                        isConfiguring = !isConfiguring
                        onExpandStateChanged(!isExpanded)
                    }
                }

                is TargetState.Ready -> onExpandStateChanged(!isExpanded)

                else -> {}
            }
        },
        isOnClickActionInProgress = isConfiguring,
        enabled = !isRequestInProgress
    )
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun Images(
    snackbarHostState: SnackbarHostState,
    metadata: String,
    target: Target,
    isRequestInProgress: Boolean,
    checkCompatibility: suspend (Target, FirmwareEntry, Int, ByteArray, Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val entries = (target.targetState as? TargetState.Ready)
        ?.entries
        .orEmpty()
    Column(modifier = Modifier.padding(top = 72.dp)) {
        entries.forEachIndexed { indexOfEntry, entry ->
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
                        if (target.targetState is TargetState.Error) {
                            Icon(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                imageVector = Icons.Rounded.SubdirectoryArrowRight,
                                contentDescription = null
                            )
                        } else {
                            if (isCompatibilityCheckInProgress && isRequestInProgress) {
                                CircularProgressIndicator(
                                    modifier = Modifier
                                        .padding(end = 16.dp)
                                        .size(size = 24.dp)
                                )
                            } else {
                                Checkbox(
                                    modifier = Modifier.padding(start = 16.dp, end = 8.dp),
                                    checked = entry.isSelected,
                                    onCheckedChange = { checked ->
                                        isCompatibilityCheckInProgress = checked
                                        scope.launch {
                                            try {
                                                metadata
                                                    .hexToByteArray()
                                                    .takeIf { it.isNotEmpty() }
                                                    ?.let { metadata ->
                                                        checkCompatibility(
                                                            target,
                                                            entry,
                                                            indexOfEntry,
                                                            metadata,
                                                            checked
                                                        )
                                                    }
                                            } catch (e: Exception) {
                                                snackbarHostState.currentSnackbarData?.dismiss()
                                                snackbarHostState.showSnackbar(
                                                    message = e.message
                                                        ?: "Error checking compatibility",
                                                )
                                            } finally {
                                                isCompatibilityCheckInProgress = false
                                            }
                                        }
                                    },
                                    enabled = !isRequestInProgress
                                )
                            }
                        }
                    },
                    subtitle = target.node.name
                )
            }
        }
    }
}

fun ContentResolver.fileName(uri: Uri) = when (uri.scheme) {
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