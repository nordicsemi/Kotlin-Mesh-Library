package no.nordicsemi.android.nrfmesh.feature.model.dfu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.PlaylistAddCheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material.icons.outlined.SdCardAlert
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.KeyIdGenerator
import no.nordicsemi.android.nrfmesh.core.common.Utils.describe
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshMessageStatusDialog
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareInformation
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdateAdditionalInformation
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdateMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdatePhase
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionApply
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionCancel
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareUpdateApply
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareUpdateCancel
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareUpdateGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareUpdateInformationGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareUpdateInformationStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareUpdateStatus
import no.nordicsemi.kotlin.mesh.core.model.Model

@Composable
internal fun FirmwareUpdateServer(
    model: Model,
    isInProgress: Boolean,
    onFirmwareInformationPressed: (Model, FirmwareInformation) -> Unit,
    send: suspend (Model, AcknowledgedMeshMessage) -> MeshMessage?,
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Controls(model = model, isInProgress = isInProgress, send = send)
        FirmwareInformationGet(
            model = model,
            isInProgress = isInProgress,
            onFirmwareInformationPressed = onFirmwareInformationPressed,
            send = send
        )
    }
}

@Composable
private fun Controls(
    model: Model,
    isInProgress: Boolean,
    send: suspend (Model, AcknowledgedMeshMessage) -> MeshMessage?,
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<FirmwareUpdateStatus?>(null) }
    var error by rememberSaveable { mutableStateOf<Throwable?>(null) }
    var shouldShowProgressIcon by rememberSaveable { mutableStateOf(false) }
    var opCode by rememberSaveable { mutableStateOf<Int?>(null) }
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
            onClick = dropUnlessResumed {
                scope.launch {
                    try {
                        shouldShowProgressIcon = true
                        opCode = FirmwareDistributionApply.opCode.toInt()
                        status =
                            send(model, FirmwareUpdateApply()) as? FirmwareUpdateStatus
                    } catch (e: Exception) {
                        error = e
                    } finally {
                        shouldShowProgressIcon = false
                    }
                }
            },
            enabled = !isInProgress && status?.updatePhase == FirmwareUpdatePhase.IDLE,
            isOnClickActionInProgress = shouldShowProgressIcon
                    && opCode == FirmwareUpdateApply.opCode.toInt()
        )
        MeshIconButton(
            buttonIcon = Icons.Outlined.Cancel,
            buttonIconTint = Color.Red,
            onClick = dropUnlessResumed {
                scope.launch {
                    try {
                        shouldShowProgressIcon = true
                        opCode = FirmwareDistributionCancel.opCode.toInt()
                        status =
                            send(model, FirmwareUpdateCancel()) as? FirmwareUpdateStatus
                    } catch (e: Exception) {
                        error = e
                    } finally {
                        shouldShowProgressIcon = false
                    }
                }
            },
            enabled = !isInProgress && status?.updatePhase == FirmwareUpdatePhase.IDLE,
            isOnClickActionInProgress = shouldShowProgressIcon && opCode == FirmwareDistributionCancel.opCode.toInt()
        )
        MeshIconButton(
            buttonIcon = Icons.Outlined.Refresh,
            onClick = dropUnlessResumed {
                scope.launch {
                    shouldShowProgressIcon = true
                    try {
                        opCode = FirmwareUpdateGet.opCode.toInt()
                        status = send(model, FirmwareUpdateGet()) as? FirmwareUpdateStatus
                    } catch (e: Exception) {
                        error = e
                    } finally {
                        shouldShowProgressIcon = false
                    }
                }
            },
            enabled = !isInProgress,
            isOnClickActionInProgress = shouldShowProgressIcon && opCode == FirmwareUpdateGet.opCode.toInt()
        )
    }
    Status(status = status?.status)
    Phase(phase = status?.updatePhase)
    Ttl(ttl = status?.updateTtl)
    AdditionalInformation(information = status?.additionalInformation)
    UpdateTimeoutBase(updateTimeoutBase = status?.updateTimeoutBase)
    BlobId(blobId = status?.blobId)
    ImageIndex(index = status?.imageIndex)

    error?.let {
        MeshMessageStatusDialog(
            text = it.describe(),
            showDismissButton = true,
            onDismissRequest = { error = null },
        )
    }
}

@Composable
private fun Status(status: FirmwareUpdateMessageStatus?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.PlaylistAddCheckCircle,
        title = stringResource(R.string.label_status),
        subtitle = status?.debugDescription ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun Phase(phase: FirmwareUpdatePhase?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Checklist,
        title = stringResource(R.string.label_phase),
        subtitle = phase?.debugDescription ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun Ttl(ttl: UByte?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Timer,
        title = stringResource(R.string.label_ttl),
        subtitle = ttl?.toString() ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun AdditionalInformation(information: FirmwareUpdateAdditionalInformation?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.SdCardAlert,
        title = stringResource(R.string.label_additional_information),
        subtitle = information?.debugDescription ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun UpdateTimeoutBase(updateTimeoutBase: UShort?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Timelapse,
        title = stringResource(R.string.label_timeout_base),
        subtitle = updateTimeoutBase?.toHexString() ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun BlobId(blobId: ULong?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.DataObject,
        title = stringResource(R.string.label_blob_id),
        subtitle = blobId?.toString() ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun ImageIndex(index: UByte?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Numbers,
        title = stringResource(R.string.label_image_index),
        subtitle = index?.toString() ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun FirmwareInformationGet(
    model: Model,
    isInProgress: Boolean,
    send: suspend (Model, AcknowledgedMeshMessage) -> MeshMessage?,
    onFirmwareInformationPressed: (Model, FirmwareInformation) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<FirmwareUpdateInformationStatus?>(null) }
    var error by rememberSaveable { mutableStateOf<Throwable?>(null) }
    var shouldShowProgressIcon by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(
            modifier = Modifier.weight(weight = 1f),
            title = stringResource(R.string.label_firmware_information)
        )
        MeshIconButton(
            buttonIcon = Icons.Outlined.Refresh,
            onClick = dropUnlessResumed {
                scope.launch {
                    shouldShowProgressIcon = true
                    try {
                        status = send(
                            model,
                            FirmwareUpdateInformationGet(
                                firstIndex = 0u,
                                entriesLimit = 10u
                            )
                        ) as FirmwareUpdateInformationStatus?
                    } catch (e: Exception) {
                        error = e
                    } finally {
                        shouldShowProgressIcon = false
                    }
                }
            },
            enabled = !isInProgress,
            isOnClickActionInProgress = shouldShowProgressIcon
        )
    }
    status?.list?.takeIf {
        it.isNotEmpty()
    }?.forEachIndexed { index, information ->
        key(KeyIdGenerator.nextId()) {
            val title = stringResource(
                R.string.label_image_value,
                (status!!.firstIndex + index.toUByte()).toInt()
            )
            ElevatedCardItem(
                modifier = Modifier.padding(horizontal = 16.dp),
                imageVector = Icons.Outlined.SdCard,
                title = stringResource(
                    R.string.label_image_value,
                    (status!!.firstIndex + index.toUByte()).toInt()
                ),
                subtitle = information.currentFirmwareId.versionString?.let { versionString ->
                    stringResource(R.string.label_version_value, versionString)
                } ?: stringResource(R.string.label_unknown),
                onClick = dropUnlessResumed { onFirmwareInformationPressed(model, information) }
            )
        }
    } ?: run {
        ElevatedCardItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            imageVector = Icons.Outlined.SdCard,
            title = stringResource(R.string.label_unknown),
        )
    }

    error?.let {
        MeshMessageStatusDialog(
            text = it.describe(),
            showDismissButton = true,
            onDismissRequest = { error = null },
        )
    }
}