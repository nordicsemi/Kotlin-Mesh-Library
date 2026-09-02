package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.nrfmesh.feature.dfu.FirmwareUpdateOptions
import no.nordicsemi.android.nrfmesh.feature.dfu.R
import no.nordicsemi.android.nrfmesh.feature.dfu.icon
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.Model

@Composable
internal fun Page0(
    onGattProxyClicked: () -> Unit,
    onBindAppKeysClicked: (Model) -> Unit,
    enableNextStage: (KeyIndex) -> Unit,
) {
    val viewModel = hiltViewModel<Page0ViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var option by rememberSaveable { mutableStateOf(FirmwareUpdateOptions.SMP) }
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
    ) {
        FirmwareUpdateOptions.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                modifier = Modifier.defaultMinSize(minWidth = 60.dp),
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = FirmwareUpdateOptions.entries.size
                ),
                onClick = { option = entry },
                selected = entry == option,
                icon = {
                    SegmentedButtonDefaults.Icon(active = entry == option) {
                        Icon(
                            imageVector = entry.icon(),
                            contentDescription = null,
                            modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                        )
                    }
                },
                label = {
                    Text(
                        text = entry.description(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
    when (option) {
        FirmwareUpdateOptions.SMP -> {
            SmpContent(
                snackbarHostState = snackbarHostState,
                messageState = uiState.messageState,
                connectionState = uiState.proxyConnectionState.connectionState,
                node = uiState.node,
                name = uiState.name,
                unicastAddress = uiState.unicastAddress,
                isSmpServiceSupported = uiState.isSmpServiceSupported,
                isDistributorServerModelSupported = uiState.isDistributorServerModelSupported,
                isLePairingSupported = uiState.isLePairingSupported,
                onGattProxyClicked = onGattProxyClicked,
                selectedKey = uiState.selectedKey,
                onApplicationKeyClicked = viewModel::onApplicationKeyClicked,
                onBindAppKeysClicked = onBindAppKeysClicked,
                firmwareDistributionStatus = uiState.distributionStatus,
                capabilitiesStatus = uiState.capabilitiesStatus,
                send = viewModel::send,
                enableNextStage = {
                    uiState.selectedKey?.let {
                        enableNextStage(it.index)
                    }
                }
            )
        }

        FirmwareUpdateOptions.BLOB -> BlobContent()
        FirmwareUpdateOptions.HTTPS -> HttpsContent()
    }
}


@Composable
private fun BlobContent() {
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = stringResource(R.string.label_dfu_over_blob_rationale),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun HttpsContent() {
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = stringResource(R.string.label_dfu_over_https_rationale),
        style = MaterialTheme.typography.bodySmall
    )
}