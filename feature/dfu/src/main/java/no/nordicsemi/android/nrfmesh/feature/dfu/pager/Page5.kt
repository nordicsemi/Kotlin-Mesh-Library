package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.common.ui.view.CircularIcon
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.dfu.DfuSession
import no.nordicsemi.android.nrfmesh.feature.dfu.R
import no.nordicsemi.kotlin.mesh.core.model.Node

@Composable
internal fun Page5(session: DfuSession) {
    val viewModel = hiltViewModel<Page5ViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        UploadProgress()
        DistributionProgress(distributor = uiState.distributor ?: return)
    }
}

@Composable
private fun UploadProgress() {
    SectionTitle(title = stringResource(R.string.label_upload_progress))
    ElevatedCardItem(
        imageVector = Icons.Outlined.FileUpload,
        title = stringResource(R.string.label_firmware_upload),
        subtitle = stringResource(R.string.label_firmware_upload_distributor)
    )
}

@Composable
private fun DistributionProgress(distributor: Node) {
    SectionTitle(title = stringResource(R.string.label_distribution_progress))
    MeshItem(
        modifier = Modifier.fillMaxWidth(),
        icon = {
            CircularIcon(painter = painterResource(R.drawable.ic_mesh))
        },
        title = distributor.name,
        subtitle = distributor.primaryUnicastAddress.address
            .toHexString(
                format = HexFormat {
                    number.prefix = "Address: 0x"
                    upperCase = true
                }
            ),
        onClick = { }
    )
}