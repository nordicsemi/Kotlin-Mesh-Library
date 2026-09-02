package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshNoItemsAvailable
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.dfu.R
import no.nordicsemi.android.nrfmesh.feature.dfu.util.ZipPackage
import no.nordicsemi.kotlin.mesh.core.model.Node
import kotlin.uuid.ExperimentalUuidApi

@Composable
internal fun Page2(index: Int) {
    val viewModel = hiltViewModel<Page2ViewModel, Page2ViewModel.Factory> { factory ->
        factory.create(index = index)
    }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FileSelector(importZipPackage = viewModel::importZipPackage)
    TargetNodes(nodes = uiState.nodes)
}

@Composable
private fun FileSelector(importZipPackage: (uri: Uri, contentResolver: ContentResolver) -> ZipPackage) {
    val context = LocalContext.current
    var fileName by rememberSaveable { mutableStateOf("") }
    var fileSize by rememberSaveable { mutableIntStateOf(0) }
    var version by rememberSaveable { mutableStateOf("") }
    var metadata by rememberSaveable { mutableStateOf("") }
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                fileName = context.contentResolver.fileName(it) ?: ""
                val zipPackage = importZipPackage(uri, context.contentResolver)
                fileSize = zipPackage.getBinaries().images.firstOrNull()?.image?.data?.size ?: 0

            } catch (e: Exception) {

            }
        }
    }
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
    AnimatedVisibility(fileName.isNotEmpty()) {
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
            )
            ElevatedCardItem(
                imageVector = Icons.Outlined.Numbers,
                title = stringResource(R.string.label_version),
            )
            ElevatedCardItem(
                imageVector = Icons.Outlined.AccountTree,
                title = stringResource(R.string.label_metadata)
            )
        }
    }
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = stringResource(R.string.label_firmware_selection_description),
        style = MaterialTheme.typography.bodySmall
    )
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun TargetNodes(nodes: List<Node>) {
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
    when (nodes.isEmpty()) {
        true -> MeshNoItemsAvailable(
            modifier = Modifier.fillMaxSize(),
            imageVector = Icons.Outlined.AutoAwesome,
            title = stringResource(R.string.label_no_nodes_available)
        )

        false -> {
            Text(
                text = stringResource(R.string.label_target_nodes_description),
                style = MaterialTheme.typography.bodySmall
            )
            nodes.forEach {
                key(it.uuid.toString()) {
                    ElevatedCardItem(
                        imageVector = Icons.Outlined.FolderOpen,
                        title = it.name,
                        subtitle = it.uuid.toString()
                    )
                }
            }
        }
    }
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = stringResource(R.string.label_node_firmware_details_description),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = stringResource(R.string.label_image_firmware_details_description),
        style = MaterialTheme.typography.bodySmall
    )
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = stringResource(R.string.label_distributor_update_description),
        style = MaterialTheme.typography.bodySmall
    )
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