package no.nordicsemi.android.nrfmesh.feature.dfu.pager

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
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
import no.nordicsemi.kotlin.mesh.core.model.Node
import kotlin.uuid.ExperimentalUuidApi

@Composable
internal fun Page2() {
    val viewModel = hiltViewModel<Page2ViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    FileSelector()
    TargetNodes(nodes = uiState.nodes)
}

@Composable
private fun FileSelector() {
    val resources = LocalResources.current
    val openDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
        onResult = {}
    )
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
            onClick = dropUnlessResumed {
                openDocument.launch(
                    arrayOf(resources.getString(R.string.document_type))
                )
            }
        )
    }
    ElevatedCardItem(
        imageVector = Icons.Outlined.FileOpen,
        title = stringResource(R.string.label_file),
        subtitle = ""
    )
    AnimatedVisibility(true) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ElevatedCardItem(
                imageVector = Icons.Outlined.DeveloperBoard,
                title = stringResource(R.string.label_application),
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
