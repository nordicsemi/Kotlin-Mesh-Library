package no.nordicsemi.android.nrfmesh.feature.dfu.smp

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.dfu.R

@Composable
fun SmpContent() {
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = stringResource(R.string.label_dfu_over_smp_rationale),
        style = MaterialTheme.typography.bodySmall
    )
    GattProxy()
    DeviceManagement()
    FirmwareDistributor()
    ReadMore()
}

@Composable
private fun GattProxy() {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SectionTitle(
            modifier = Modifier
                .weight(weight = 1f),
            title = stringResource(R.string.label_gatt_proxy)
        )
        MeshIconButton(
            onClick = {},
            buttonIcon = Icons.Outlined.Bluetooth,
        )
    }
    ElevatedCardItem(
        imageVector = Icons.AutoMirrored.Outlined.Label,
        title = stringResource(R.string.label_name),
        subtitle = ""
    )
    ElevatedCardItem(
        imageVector = Icons.Outlined.Lan,
        title = stringResource(R.string.label_unicast_address),
        subtitle = ""
    )
    Text(
        modifier = Modifier.padding(horizontal = 8.dp),
        text = stringResource(R.string.label_smp_dfu_rationale),
        style = MaterialTheme.typography.bodySmall
    )
}

@Composable
private fun DeviceManagement() {
    SectionTitle(title = stringResource(R.string.label_gatt_proxy))
    ElevatedCardItem(
        imageVector = Icons.AutoMirrored.Outlined.Label,
        title = stringResource(R.string.label_smp_service),
        subtitle = ""
    )
    ElevatedCardItem(
        imageVector = Icons.Outlined.Lan,
        title = stringResource(R.string.label_access),
        subtitle = ""
    )
}

@Composable
private fun FirmwareDistributor() {
    var isFirmwareDistributorModelFound by rememberSaveable { mutableStateOf<Boolean?>(null) }
    SectionTitle(title = stringResource(R.string.label_firmware_distributor))
    ElevatedCardItem(
        imageVector = Icons.AutoMirrored.Outlined.Label,
        title = stringResource(R.string.label_firmware_distributor_model),
        subtitle = when (isFirmwareDistributorModelFound) {
            true -> stringResource(R.string.label_found)
            false -> stringResource(R.string.label_not_found)
            else -> stringResource(R.string.label_unknown)
        },
        titleAction = {
            Icon(
                modifier = Modifier.padding(horizontal = 16.dp),
                imageVector = when (isFirmwareDistributorModelFound) {
                    true -> Icons.Outlined.CheckCircle
                    false -> Icons.Outlined.WarningAmber
                    null -> Icons.Outlined.QuestionMark
                },
                tint = when (isFirmwareDistributorModelFound) {
                    true -> Color.Green
                    else -> Color.Yellow
                },
                contentDescription = null
            )
        }
    )
}

@Composable
private fun ReadMore() {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    SectionTitle(title = stringResource(R.string.label_read_more))
    ElevatedCardItem(
        imageVector = Icons.AutoMirrored.Outlined.Label,
        title = stringResource(R.string.label_device_firmware_update),
        subtitle = stringResource(R.string.label_device_firmware_update_link),
        titleAction = {
            MeshIconButton(
                onClick = dropUnlessResumed {
                    uriHandler.openUri(
                        uri = context.getString(R.string.label_device_firmware_update_link)
                    )
                },
                buttonIcon = Icons.Outlined.OpenInBrowser,
            )
        }
    )
    ElevatedCardItem(
        imageVector = Icons.AutoMirrored.Outlined.Label,
        title = stringResource(R.string.label_dfu_over_bluetooth_mesh),
        subtitle = stringResource(R.string.label_dfu_over_bluetooth_mesh_link),
        titleAction = {
            MeshIconButton(
                onClick = dropUnlessResumed {
                    uriHandler.openUri(
                        uri = context.getString(R.string.label_dfu_over_bluetooth_mesh_link)
                    )
                },
                buttonIcon = Icons.Outlined.OpenInBrowser,
            )
        }
    )
    ElevatedCardItem(
        imageVector = Icons.AutoMirrored.Outlined.Label,
        title = stringResource(R.string.label_sample_distributor),
        subtitle = stringResource(R.string.label_sample_distributor_link),
        titleAction = {
            MeshIconButton(
                onClick = dropUnlessResumed {
                    uriHandler.openUri(
                        uri = context.getString(R.string.label_sample_distributor_link)
                    )
                },
                buttonIcon = Icons.Outlined.OpenInBrowser,
            )
        }
    )
    ElevatedCardItem(
        imageVector = Icons.AutoMirrored.Outlined.Label,
        title = stringResource(R.string.label_device_management_smp),
        subtitle = stringResource(R.string.label_device_management_smp_link),
        titleAction = {
            MeshIconButton(
                onClick = dropUnlessResumed {
                    uriHandler.openUri(
                        uri = context.getString(R.string.label_device_management_smp_link)
                    )
                },
                buttonIcon = Icons.Outlined.OpenInBrowser,
            )
        }
    )
}