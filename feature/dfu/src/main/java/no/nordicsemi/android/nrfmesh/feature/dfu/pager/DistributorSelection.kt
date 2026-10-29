package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.nrfmesh.core.ui.MeshAlertDialog
import no.nordicsemi.android.nrfmesh.feature.dfu.FirmwareUpdateOptions
import no.nordicsemi.android.nrfmesh.feature.dfu.R
import no.nordicsemi.android.nrfmesh.feature.dfu.icon
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionPhase
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.Model

/**
 * A firmware distribution that is in progress on the Distributor.
 *
 * @property keyIndex     Index of the application key bound to the Firmware Distribution Server.
 * @property firmwareSize Estimated size of the firmware image being distributed in bytes.
 */
internal data class OngoingDistribution(val keyIndex: Int, val firmwareSize: Int)

/** The phases of the Distributor, in which a distribution is in progress. */
private val ongoingPhases = setOf(
    FirmwareDistributionPhase.TRANSFER_ACTIVE,
    FirmwareDistributionPhase.TRANSFER_SUCCESS,
    FirmwareDistributionPhase.TRANSFER_SUSPENDED
)

@Composable
internal fun DistributorSelection(
    option: FirmwareUpdateOptions,
    onOptionChanged: (FirmwareUpdateOptions) -> Unit,
    onGattProxyClicked: () -> Unit,
    onBindAppKeysClicked: (Model) -> Unit,
    onKeySelected: (KeyIndex) -> Unit,
    onKeyDeselected: () -> Unit,
    onMaxReceiversListSizeChanged: (Int) -> Unit,
    onOngoingDistributionChanged: (OngoingDistribution?) -> Unit,
    onLePairingSupportedChanged: (Boolean) -> Unit,
) {
    val viewModel = hiltViewModel<DistributorSelectionViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    // If the Distributor is already distributing a firmware image, there is nothing to set up, the
    // progress can be followed on the last page.
    LaunchedEffect(uiState.selectedKey, uiState.distributionStatus?.phase, uiState.capabilitiesStatus) {
        val key = uiState.selectedKey
        val capabilities = uiState.capabilitiesStatus
        onOngoingDistributionChanged(
            when {
                key != null && capabilities != null &&
                        uiState.distributionStatus?.phase in ongoingPhases ->
                    OngoingDistribution(
                        keyIndex = key.index.toInt(),
                        // The size of the image is the space used by the Distributor to store it.
                        firmwareSize = capabilities.maxUploadSpace.toInt() -
                                capabilities.remainingUploadSpace.toInt()
                    )

                else -> null
            }
        )
    }
    // A node without the Firmware Distribution Server model cannot be used as the Distributor, so
    // a key selected before connecting to it is no longer valid.
    LaunchedEffect(uiState.isDistributorServerModelSupported) {
        if (uiState.isDistributorServerModelSupported == false) {
            onKeyDeselected()
        }
    }
    // Without the LE Pairing Responder model, there is nothing to pair with.
    LaunchedEffect(uiState.isLePairingSupported) {
        onLePairingSupportedChanged(uiState.isLePairingSupported == true)
    }
    // The size of the receivers list is needed to limit the number of selected receivers.
    LaunchedEffect(uiState.capabilitiesStatus) {
        onMaxReceiversListSizeChanged(uiState.capabilitiesStatus?.maxReceiversCount?.toInt() ?: 0)
    }
    var isInfoDialogVisible by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OptionSelector(
            modifier = Modifier.weight(weight = 1f),
            option = option,
            onOptionChanged = onOptionChanged
        )
        IconButton(onClick = { isInfoDialogVisible = true }) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = stringResource(R.string.label_more_information)
            )
        }
    }
    if (isInfoDialogVisible) {
        // The option selected in the dialog is only used to read about the options, it doesn't
        // change the option selected on the screen.
        var infoOption by rememberSaveable { mutableStateOf(option) }
        MeshAlertDialog(
            onDismissRequest = { isInfoDialogVisible = false },
            confirmButtonText = stringResource(R.string.label_close),
            onConfirmClick = { isInfoDialogVisible = false },
            dismissButtonText = null,
            icon = Icons.Outlined.Info,
            iconColor = AlertDialogDefaults.iconContentColor
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
                OptionSelector(
                    option = infoOption,
                    onOptionChanged = { infoOption = it },
                    showIcons = false
                )
                // All the texts are laid out on top of each other, so the dialog is as tall as
                // the longest one and doesn't resize when switching between the options.
                Box {
                    FirmwareUpdateOptions.entries.forEach { entry ->
                        val isSelected = entry == infoOption
                        Text(
                            modifier = Modifier
                                .alpha(alpha = if (isSelected) 1f else 0f)
                                .then(if (isSelected) Modifier else Modifier.clearAndSetSemantics {}),
                            text = stringResource(
                                id = when (entry) {
                                    FirmwareUpdateOptions.SMP -> R.string.label_dfu_over_smp_rationale
                                    FirmwareUpdateOptions.BLOB -> R.string.label_dfu_over_blob_rationale
                                    FirmwareUpdateOptions.HTTPS -> R.string.label_dfu_over_https_rationale
                                }
                            )
                        )
                    }
                }
            }
        }
    }
    when (option) {
        FirmwareUpdateOptions.SMP -> {
            SmpContent(
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
                onDistributionStatusChanged = viewModel::onDistributionStatusChanged,
                onNextStageEnabledChanged = { isEnabled ->
                    when (isEnabled) {
                        true -> uiState.selectedKey?.let { onKeySelected(it.index) }
                        false -> onKeyDeselected()
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

/**
 * Segmented buttons to select one of the [FirmwareUpdateOptions].
 *
 * @param option           Currently selected option.
 * @param onOptionChanged  Called when an option is selected.
 * @param showIcons        Whether the icon of each option is shown.
 */
@Composable
private fun OptionSelector(
    option: FirmwareUpdateOptions,
    onOptionChanged: (FirmwareUpdateOptions) -> Unit,
    modifier: Modifier = Modifier,
    showIcons: Boolean = true,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        FirmwareUpdateOptions.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                modifier = Modifier.defaultMinSize(minWidth = 60.dp),
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = FirmwareUpdateOptions.entries.size
                ),
                onClick = { onOptionChanged(entry) },
                selected = entry == option,
                icon = {
                    if (showIcons) {
                        SegmentedButtonDefaults.Icon(active = entry == option) {
                            Icon(
                                imageVector = entry.icon(),
                                contentDescription = null,
                                modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                            )
                        }
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
}
