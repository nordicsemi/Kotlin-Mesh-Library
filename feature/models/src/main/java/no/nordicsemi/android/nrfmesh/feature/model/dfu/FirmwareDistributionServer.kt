package no.nordicsemi.android.nrfmesh.feature.model.dfu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PlaylistAddCheckCircle
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import no.nordicsemi.android.nrfmesh.core.common.Utils.describe
import no.nordicsemi.android.nrfmesh.core.common.name
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MaxFirmwareImageSize
import no.nordicsemi.android.nrfmesh.core.ui.MaxFirmwareImagesListSize
import no.nordicsemi.android.nrfmesh.core.ui.MaxReceiversListSize
import no.nordicsemi.android.nrfmesh.core.ui.MaxUploadSpace
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshMessageStatusDialog
import no.nordicsemi.android.nrfmesh.core.ui.RemainingUploadSpace
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.core.ui.SupportedUriSchemes
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionPhase
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdatePolicy
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.TransferMode
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionApply
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionCancel
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionCapabilitiesGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionCapabilitiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionFirmwareGetByIndex
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionFirmwareStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionStart
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionSuspend
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.FixedGroupAddress
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress

@Composable
internal fun FirmwareDistributionServer(
    model: Model,
    isInProgress: Boolean,
    send: suspend (Model, AcknowledgedMeshMessage) -> MeshMessage?,
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Controls(model = model, isInProgress = isInProgress, send = send)
        Capabilities(model = model, isInProgress = isInProgress, send = send)
        FirmwareDistributionSlots(model = model, isInProgress = isInProgress, send = send)
    }
}

@Composable
private fun Controls(
    model: Model,
    isInProgress: Boolean,
    send: suspend (Model, AcknowledgedMeshMessage) -> MeshMessage?,
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<FirmwareDistributionStatus?>(null) }
    val isTransferSuspended by rememberSaveable {
        mutableStateOf(status?.phase == FirmwareDistributionPhase.TRANSFER_SUSPENDED)
    }
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
                            send(model, FirmwareDistributionApply()) as? FirmwareDistributionStatus
                    } catch (e: Exception) {
                        error = e
                    } finally {
                        shouldShowProgressIcon = false
                    }
                }
            },
            enabled = !isInProgress && status != null,
            isOnClickActionInProgress = shouldShowProgressIcon
                    && opCode == FirmwareDistributionApply.opCode.toInt()
        )
        MeshIconButton(
            buttonIcon = when (isTransferSuspended) {
                true -> Icons.Outlined.PlayArrow
                else -> Icons.Outlined.Pause
            },
            onClick = dropUnlessResumed {
                scope.launch {
                    shouldShowProgressIcon = true
                    try {
                        status = if (isTransferSuspended) {
                            status?.let { resume ->
                                opCode = FirmwareDistributionStart.opCode.toInt()
                                send(
                                    model,
                                    FirmwareDistributionStart(resumeWithStatus = resume)
                                ) as? FirmwareDistributionStatus
                            }
                        } else {
                            opCode = FirmwareDistributionSuspend.opCode.toInt()
                            send(
                                model,
                                FirmwareDistributionSuspend()
                            ) as? FirmwareDistributionStatus
                        }
                    } catch (e: Exception) {
                        error = e
                    } finally {
                        shouldShowProgressIcon = false
                    }
                }
            },
            enabled = !isInProgress && status != null && !isTransferSuspended,
            isOnClickActionInProgress = shouldShowProgressIcon &&
                    (opCode == FirmwareDistributionStart.opCode.toInt() ||
                            opCode == FirmwareDistributionSuspend.opCode.toInt())
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
                            send(model, FirmwareDistributionCancel()) as? FirmwareDistributionStatus
                    } catch (e: Exception) {
                        error = e
                    } finally {
                        shouldShowProgressIcon = false
                    }
                }
            },
            enabled = !isInProgress && status != null,
            isOnClickActionInProgress = shouldShowProgressIcon && opCode == FirmwareDistributionCancel.opCode.toInt()
        )
        MeshIconButton(
            buttonIcon = Icons.Outlined.Refresh,
            onClick = dropUnlessResumed {
                scope.launch {
                    shouldShowProgressIcon = true
                    try {
                        opCode = FirmwareDistributionGet.opCode.toInt()
                        status =
                            send(model, FirmwareDistributionGet()) as? FirmwareDistributionStatus
                    } catch (e: Exception) {
                        error = e
                    } finally {
                        shouldShowProgressIcon = false
                    }
                }
            },
            enabled = !isInProgress,
            isOnClickActionInProgress = shouldShowProgressIcon && opCode == FirmwareDistributionGet.opCode.toInt()
        )
    }
    Status(status = status?.status)
    Phase(phase = status?.phase)
    MulticastAddress(model = model, address = status?.multicastAddress)
    ApplicationKeyIndex(key = status?.applicationKeyIndex?.let { model.boundApplicationKey(index = it) })
    Ttl(ttl = status?.ttl)
    DistributionTimeoutBase(distributionTimeoutBase = status?.distributionTimeoutBase)
    DistributionTransferMode(distributionTransferMode = status?.distributionTransferMode)
    UpdatePolicy(updatePolicy = status?.updatePolicy)
    DistributionFirmwareImageIndex(index = status?.firmwareImageIndex)

    error?.let {
        MeshMessageStatusDialog(
            text = it.describe(),
            showDismissButton = true,
            onDismissRequest = { error = null },
        )
    }
}

@Composable
private fun Status(status: FirmwareDistributionMessageStatus?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.PlaylistAddCheckCircle,
        title = stringResource(R.string.label_status),
        subtitle = status?.debugDescription ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun Phase(phase: FirmwareDistributionPhase?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Checklist,
        title = stringResource(R.string.label_phase),
        subtitle = phase?.debugDescription ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun MulticastAddress(model: Model, address: Address?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Campaign,
        title = stringResource(R.string.label_multicast_address),
        subtitle = address?.let {
            when {
                GroupAddress.isValid(address = address) ||
                        VirtualAddress.isValid(address = address) -> model
                    .parentElement
                    ?.parentNode
                    ?.network
                    ?.group(address = address)
                    ?.name ?: address.toHexString()

                FixedGroupAddress.isValid(address = address) -> FixedGroupAddress
                    .create(address = address)
                    .name()

                else -> address.toHexString()
            }
        } ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun ApplicationKeyIndex(key: ApplicationKey?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.VpnKey,
        title = stringResource(R.string.label_application_key),
        subtitle = key?.name ?: stringResource(R.string.label_unknown)
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
private fun DistributionTimeoutBase(distributionTimeoutBase: UShort?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Timelapse,
        title = stringResource(R.string.label_timeout_base),
        subtitle = distributionTimeoutBase?.toHexString() ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun DistributionTransferMode(distributionTransferMode: TransferMode?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.SyncAlt,
        title = stringResource(R.string.label_transfer_mode),
        subtitle = distributionTransferMode?.debugDescription
            ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun UpdatePolicy(updatePolicy: FirmwareUpdatePolicy?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Policy,
        title = stringResource(R.string.label_update_policy),
        subtitle = updatePolicy?.debugDescription ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun DistributionFirmwareImageIndex(index: UShort?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Numbers,
        title = stringResource(R.string.label_distribution_firmware_image_index),
        subtitle = index?.toString() ?: stringResource(R.string.label_unknown)
    )
}

@Composable
private fun Capabilities(
    model: Model,
    isInProgress: Boolean,
    send: suspend (Model, AcknowledgedMeshMessage) -> MeshMessage?,
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<FirmwareDistributionCapabilitiesStatus?>(null) }
    var error by rememberSaveable { mutableStateOf<Throwable?>(null) }
    var shouldShowProgressIcon by rememberSaveable { mutableStateOf(false) }

    var maxReceiversCount by rememberSaveable(status) { mutableStateOf(status?.maxReceiversCount?.toInt()) }
    var maxFirmwareImagesListSize by rememberSaveable(status) { mutableStateOf(status?.maxFirmwareImagesListSize?.toInt()) }
    var maxFirmwareImageSize by rememberSaveable(status) { mutableStateOf(status?.maxFirmwareImageSize?.toInt()) }
    var maxUploadSpace by rememberSaveable(status) { mutableStateOf(status?.maxUploadSpace?.toInt()) }
    var remainingUploadSpace by rememberSaveable(status) { mutableStateOf(status?.remainingUploadSpace?.toInt()) }
    var supportedUriSchemes by rememberSaveable(status) { mutableStateOf(status?.supportedUriSchemes) }
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
            onClick = {
                scope.launch {
                    shouldShowProgressIcon = true
                    try {
                        status = send(
                            model,
                            FirmwareDistributionCapabilitiesGet()
                        ) as? FirmwareDistributionCapabilitiesStatus
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
    MaxReceiversListSize(
        modifier = Modifier.padding(horizontal = 16.dp),
        receiversSize = maxReceiversCount
    )
    MaxFirmwareImagesListSize(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageListSize = maxFirmwareImagesListSize
    )
    MaxFirmwareImageSize(
        modifier = Modifier.padding(horizontal = 16.dp),
        firmwareImageSize = maxFirmwareImageSize
    )
    MaxUploadSpace(
        modifier = Modifier.padding(horizontal = 16.dp),
        uploadSpace = maxUploadSpace
    )
    RemainingUploadSpace(
        modifier = Modifier.padding(horizontal = 16.dp),
        remainingUploadSpace = remainingUploadSpace
    )
    SupportedUriSchemes(
        modifier = Modifier.padding(horizontal = 16.dp),
        uriSchemes = supportedUriSchemes
    )

    error?.let {
        MeshMessageStatusDialog(
            text = it.describe(),
            showDismissButton = true,
            onDismissRequest = { error = null },
        )
    }
}

@Composable
private fun FirmwareDistributionSlots(
    model: Model,
    isInProgress: Boolean,
    send: suspend (Model, AcknowledgedMeshMessage) -> MeshMessage?,
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<FirmwareDistributionFirmwareStatus?>(null) }
    var error by rememberSaveable { mutableStateOf<Throwable?>(null) }
    var shouldShowProgressIcon by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(
            modifier = Modifier.weight(weight = 1f),
            title = stringResource(R.string.label_firmware_distribution_slots)
        )
        MeshIconButton(
            buttonIcon = Icons.Outlined.Refresh,
            onClick = dropUnlessResumed {
                scope.launch {
                    shouldShowProgressIcon = true
                    try {
                        status = send(
                            model,
                            FirmwareDistributionFirmwareGetByIndex(imageIndex = 0u)
                        ) as FirmwareDistributionFirmwareStatus?
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
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.SpaceDashboard,
        title = stringResource(R.string.label_slots),
        subtitle = status
            ?.entryCount
            ?.toString(radix = 16)
            ?: stringResource(R.string.label_unknown)
    )

    error?.let {
        MeshMessageStatusDialog(
            text = it.describe(),
            showDismissButton = true,
            onDismissRequest = { error = null },
        )
    }
}