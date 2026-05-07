package no.nordicsemi.android.nrfmesh.feature.export.navigation

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Rule
import androidx.compose.material.icons.outlined.ChecklistRtl
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedButton
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.export.ExportOption
import no.nordicsemi.android.nrfmesh.feature.export.ExportScreenUiState
import no.nordicsemi.android.nrfmesh.feature.export.ExportState
import no.nordicsemi.android.nrfmesh.feature.export.NetworkKeyItemState
import no.nordicsemi.android.nrfmesh.feature.export.ProvisionerItemState
import no.nordicsemi.android.nrfmesh.feature.export.R
import no.nordicsemi.kotlin.data.toHexString
import no.nordicsemi.kotlin.mesh.core.exception.AtLeastOneNetworkKeyMustBeSelected
import no.nordicsemi.kotlin.mesh.core.exception.AtLeastOneProvisionerMustBeSelected
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.Provisioner

@Composable
internal fun ExportScreen(
    uiState: ExportScreenUiState,
    onExportOptionSelected: (ExportOption) -> Unit,
    onNetworkKeySelected: (NetworkKey, Boolean) -> Unit,
    onProvisionerSelected: (Provisioner, Boolean) -> Unit,
    onExportDeviceKeysToggled: (Boolean) -> Unit,
    export: (ContentResolver, Uri) -> Unit,
    onExportStateDisplayed: () -> Unit,
    onExportCompleted: (message: String) -> Unit,
) {
    val context = LocalContext.current
    val createDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(stringResource(R.string.document_type)),
        onResult = { it?.let { export(context.contentResolver, it) } }
    )
    ExportContent(
        uiState = uiState,
        onExportOptionSelected = onExportOptionSelected,
        onNetworkKeySelected = onNetworkKeySelected,
        onProvisionerSelected = onProvisionerSelected,
        onExportDeviceKeysToggled = onExportDeviceKeysToggled,
        onExportClicked = { createDocument.launch(uiState.networkName) },
        onExportStateDisplayed = onExportStateDisplayed,
        onExportCompleted = onExportCompleted
    )
}

@Composable
private fun ExportContent(
    uiState: ExportScreenUiState,
    onExportOptionSelected: (ExportOption) -> Unit,
    onNetworkKeySelected: (NetworkKey, Boolean) -> Unit,
    onProvisionerSelected: (Provisioner, Boolean) -> Unit,
    onExportDeviceKeysToggled: (Boolean) -> Unit,
    onExportClicked: () -> Unit,
    onExportStateDisplayed: () -> Unit,
    onExportCompleted: (message: String) -> Unit,
) {
    when (uiState.exportState) {
        is ExportState.Success -> {
            onExportCompleted(stringResource(R.string.label_success))
            onExportStateDisplayed()
        }

        is ExportState.Error -> {
            onExportCompleted(
                when (uiState.exportState.throwable) {
                    is AtLeastOneProvisionerMustBeSelected ->
                        stringResource(R.string.error_select_one_provisioner)

                    is AtLeastOneNetworkKeyMustBeSelected ->
                        stringResource(R.string.error_select_one_network_key)

                    else -> stringResource(R.string.error_unknown)
                }
            )
            onExportStateDisplayed()
        }

        is ExportState.Unknown -> { /*Do nothing*/
        }
    }
    Column(
        modifier = Modifier.verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        ExportSelection(
            uiState = uiState,
            onExportOptionSelected = onExportOptionSelected,
            onExportClicked = onExportClicked
        )
        if (uiState.exportOption == ExportOption.ALL) {
            Column(modifier = Modifier.fillMaxSize()) {
                Icon(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null
                )
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(id = R.string.label_export_configuration_rationale)
                )
            }
        } else {
            uiState.apply {
                SectionTitle(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = stringResource(R.string.label_provisioners)
                )
                provisionerItemStates.forEach { state ->
                    ProvisionerRow(state = state, onProvisionerSelected = onProvisionerSelected)
                }
                SectionTitle(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = stringResource(R.string.label_network_keys)
                )
                networkKeyItemStates.forEach { state ->
                    NetworkKeyRow(state = state, onNetworkKeySelected = onNetworkKeySelected)
                }
            }
            SectionTitle(title = stringResource(R.string.label_export_device_keys))
            ExportDeviceKeysRow(
                uiState = uiState,
                onExportDeviceKeysToggled = onExportDeviceKeysToggled
            )
        }
    }
}

@Composable
private fun ExportSelection(
    uiState: ExportScreenUiState,
    onExportOptionSelected: (ExportOption) -> Unit,
    onExportClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 16.dp),
        content = {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .weight(weight = 1f)
                    .padding(horizontal = 16.dp)
            ) {
                ExportOption.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        modifier = Modifier.defaultMinSize(minWidth = 60.dp),
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ExportOption.entries.size
                        ),
                        onClick = { onExportOptionSelected(option) },
                        selected = option == uiState.exportOption,
                        icon = {
                            SegmentedButtonDefaults.Icon(active = option == uiState.exportOption) {
                                Icon(
                                    imageVector = option.icon(),
                                    contentDescription = null,
                                    modifier = Modifier.size(SegmentedButtonDefaults.IconSize)
                                )
                            }
                        },
                        label = { Text(text = option.description()) }
                    )
                }
            }
            MeshOutlinedButton(
                onClick = onExportClicked,
                buttonIcon = Icons.Outlined.Save,
                text = stringResource(id = R.string.label_export)
            )
        }
    )
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
private fun ProvisionerRow(
    state: ProvisionerItemState,
    onProvisionerSelected: (Provisioner, Boolean) -> Unit,
) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Groups,
        title = state.provisioner.name,
        subtitle = state.provisioner.node?.primaryUnicastAddress?.address?.toHexString() ?: "",
        titleAction = {
            Checkbox(
                checked = state.isSelected,
                onCheckedChange = { onProvisionerSelected(state.provisioner, it) },
            )
        },
    )
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
private fun NetworkKeyRow(
    state: NetworkKeyItemState,
    onNetworkKeySelected: (NetworkKey, Boolean) -> Unit,
) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.VpnKey,
        title = state.networkKey.name,
        subtitle = state.networkKey.key.toHexString(),
        titleAction = {
            Checkbox(
                checked = state.isSelected,
                onCheckedChange = { onNetworkKeySelected(state.networkKey, it) },
            )
        }
    )
}

@Composable
private fun ExportDeviceKeysRow(
    uiState: ExportScreenUiState,
    onExportDeviceKeysToggled: (Boolean) -> Unit,
) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.VpnKey,
        title = stringResource(R.string.label_export_device_keys),
        supportingText = stringResource(R.string.label_export_device_keys_rationale),
        titleAction = {
            Switch(
                modifier = Modifier.padding(start = 16.dp),
                checked = uiState.exportDeviceKeys,
                onCheckedChange = onExportDeviceKeysToggled
            )
            Spacer(modifier = Modifier.size(size = 16.dp))
        }
    )
}

@Composable
private fun ExportOption.description() = when (this) {
    ExportOption.ALL -> stringResource(id = R.string.label_all)
    ExportOption.PARTIAL -> stringResource(id = R.string.label_partial)
}

@Composable
private fun ExportOption.icon() = when (this) {
    ExportOption.ALL -> Icons.Outlined.ChecklistRtl
    ExportOption.PARTIAL -> Icons.AutoMirrored.Outlined.Rule
}