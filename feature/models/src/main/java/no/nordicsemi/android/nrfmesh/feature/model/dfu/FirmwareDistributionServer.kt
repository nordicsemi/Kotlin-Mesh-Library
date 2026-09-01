package no.nordicsemi.android.nrfmesh.feature.model.dfu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schema
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.SecurityUpdate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdatePhase
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.TransferMode
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionApply
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionCancel
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionCapabilitiesGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionGet
import no.nordicsemi.kotlin.mesh.core.model.Model

@Composable
internal fun FirmwareDistributionServer(
    model: Model,
    messageState: MessageState,
    send: (Model, MeshMessage) -> Unit,
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Controls(messageState = messageState, model = model, send = send)
        Capabilities(messageState = messageState, model = model, send = send)
    }
}

@Composable
private fun Controls(model: Model, messageState: MessageState, send: (Model, MeshMessage) -> Unit) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(
            modifier = Modifier.weight(weight = 1f),
            title = stringResource(R.string.label_controls)
        )
        MeshIconButton(
            buttonIcon = Icons.Outlined.Check,
            onClick = { send(model, FirmwareDistributionApply()) }
        )
        MeshIconButton(
            buttonIcon = Icons.Outlined.PlayArrow,
            onClick = { /*send(model, FirmwareDistributionStart(status = ))*/ }
        )
        MeshIconButton(
            buttonIcon = Icons.Outlined.Cancel,
            onClick = { send(model, FirmwareDistributionCancel()) }
        )
        MeshIconButton(
            buttonIcon = Icons.Outlined.Refresh,
            onClick = { send(model, FirmwareDistributionGet()) }
        )
    }
    Status(status = null)
    Phase(phase = null)
    Ttl(ttl = null)
    TimeoutBase(transferMode = null)
    TransferMode(transferMode = null)
    UpdatePolicy(updatePolicy = null)
    DistributionFirmwareImageIndex(imageIndex = null)
}

@Composable
private fun Status(status: FirmwareDistributionMessageStatus?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Numbers,
        title = stringResource(R.string.label_status),
        subtitle = status?.toString() ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun Phase(phase: FirmwareUpdatePhase?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Numbers,
        title = stringResource(R.string.label_phase),
        subtitle = phase?.toString() ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun Ttl(ttl: UByte?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Numbers,
        title = stringResource(R.string.label_ttl),
        subtitle = ttl?.toInt()?.toString() ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun TimeoutBase(transferMode: TransferMode?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Numbers,
        title = stringResource(R.string.label_transfer_mode),
        subtitle = transferMode?.name ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun TransferMode(transferMode: TransferMode?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Numbers,
        title = stringResource(R.string.label_transfer_mode),
        subtitle = transferMode?.name ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun UpdatePolicy(updatePolicy: String?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Numbers,
        title = stringResource(R.string.label_max_receivers_list_size),
        subtitle = updatePolicy ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun DistributionFirmwareImageIndex(imageIndex: UByte?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Numbers,
        title = stringResource(R.string.label_max_receivers_list_size),
        subtitle = imageIndex?.toInt().toString() ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun Capabilities(
    model: Model,
    messageState: MessageState,
    send: (Model, MeshMessage) -> Unit,
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(
            modifier = Modifier.weight(weight = 1f),
            title = stringResource(R.string.label_capabilities)
        )
        MeshIconButton(
            buttonIcon = Icons.Outlined.Refresh,
            onClick = { send(model, FirmwareDistributionCapabilitiesGet()) }
        )
    }
    MaxReceiversListSize(receiversListSize = null)
    MaxFirmwareImagesListSize(firmwareImagesListSize = null)
    MaxFirmwareImageSize(firmwareImageSize = null)
    MaxUploadSpace(maxUploadSpace = null)
    RemainingUploadSpace(remaining = null)
    SupportedUriSchemes(uriScheme = stringResource(R.string.label_unknown))
}

@Composable
private fun MaxReceiversListSize(receiversListSize: UShort?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Numbers,
        title = stringResource(R.string.label_max_receivers_list_size),
        subtitle = receiversListSize?.toHexString() ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun MaxFirmwareImagesListSize(firmwareImagesListSize: UShort?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.SecurityUpdate,
        title = stringResource(R.string.label_max_receivers_list_size),
        subtitle = firmwareImagesListSize?.toHexString() ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun MaxFirmwareImageSize(firmwareImageSize: UShort?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.SecurityUpdate,
        title = stringResource(R.string.label_max_receivers_list_size),
        subtitle = firmwareImageSize?.toHexString() ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun MaxUploadSpace(maxUploadSpace: UShort?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.SdCard,
        title = stringResource(R.string.label_max_upload_space),
        subtitle = maxUploadSpace?.toHexString() ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun RemainingUploadSpace(remaining: UShort?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.SdStorage,
        title = stringResource(R.string.label_remaining_upload_space),
        subtitle = remaining?.toHexString() ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun SupportedUriSchemes(uriScheme: String) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Schema,
        title = stringResource(R.string.label_supported_uri_schemes),
        subtitle = uriScheme
    )
}