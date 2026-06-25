package no.nordicsemi.android.nrfmesh.feature.dfu

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.common.ui.view.NordicAppBar
import no.nordicsemi.android.nrfmesh.core.ui.isCompactWidth
import no.nordicsemi.android.nrfmesh.feature.dfu.smp.SmpContent
import no.nordicsemi.android.nrfmesh.feature.dfu.smp.SmpViewModel
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork


@Composable
internal fun FirmwareUpdateScreen(
    uiState: FirmwareUpdateScreenUiState,
    onGattProxyClicked: () -> Unit,
    onBindAppKeysClicked: () -> Unit,
    onBackClick: () -> Unit,
) {
    when (uiState.meshNetworkState) {
        MeshNetworkState.Loading -> {}
        is MeshNetworkState.Success -> FirmwareUpdate(
            network = uiState.meshNetworkState.network,
            onGattProxyClicked = onGattProxyClicked,
            onBindAppKeysClicked = onBindAppKeysClicked,
            onBackClick = onBackClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FirmwareUpdate(
    network: MeshNetwork,
    onGattProxyClicked: () -> Unit,
    onBindAppKeysClicked: () -> Unit,
    onBackClick: () -> Unit,
) {
    var option by rememberSaveable { mutableStateOf(FirmwareUpdateOptions.SMP) }
    Scaffold(
        modifier = Modifier.consumeWindowInsets(WindowInsets(0)),
        topBar = {
            NordicAppBar(
                title = { Text(text = stringResource(R.string.label_firmware_update)) },
                showBackButton = true,
                backButtonIcon = Icons.Outlined.Close,
                onNavigationButtonClick = onBackClick
            )
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = it)
                .background(
                    color = if (!isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.background
                ),
            horizontalArrangement = Arrangement.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(fraction = if (!isCompactWidth()) 0.5f else 1f)
                    .padding(horizontal = 16.dp)
                    .fillMaxHeight()
                    .verticalScroll(state = rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
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
                        val viewModel = hiltViewModel<SmpViewModel>()
                        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                        SmpContent(
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
                            phase = uiState.phase,
                            send = viewModel::send
                        )
                    }
                    FirmwareUpdateOptions.BLOB -> BlobContent()
                    FirmwareUpdateOptions.HTTPS -> HttpsContent()
                }
            }
        }
    }
}

@Composable
fun FirmwareUpdateOptions.Content() {
    // when (this) {
    //     FirmwareUpdateOptions.SMP -> SmpContent(onGattProxyClicked = onGattProxyClicked)
    //     FirmwareUpdateOptions.BLOB -> BlobContent()
    //     FirmwareUpdateOptions.HTTPS -> HttpsContent()
    // }
}

@Composable
fun BlobContent() {
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = stringResource(R.string.label_dfu_over_blob_rationale),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
fun HttpsContent() {
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = stringResource(R.string.label_dfu_over_https_rationale),
        style = MaterialTheme.typography.bodySmall
    )
}