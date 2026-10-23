package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.ui.MaxFirmwareImageSize
import no.nordicsemi.android.nrfmesh.core.ui.MaxFirmwareImagesListSize
import no.nordicsemi.android.nrfmesh.core.ui.MaxReceiversListSize
import no.nordicsemi.android.nrfmesh.core.ui.MaxUploadSpace
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.RemainingUploadSpace
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.dfu.R
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionPhase
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionCancel
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionCapabilitiesGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionCapabilitiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionFirmwareDeleteAll
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionFirmwareGetByIndex
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionFirmwareStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionStatus
import no.nordicsemi.kotlin.mesh.core.model.Model

@Composable
internal fun CapabilitiesContent(
    phase: FirmwareDistributionPhase,
    capabilitiesStatus: FirmwareDistributionCapabilitiesStatus?,
    model: Model,
    send: suspend (Model, AcknowledgedMeshMessage) -> MeshMessage?,
    onDistributionStatusChanged: (FirmwareDistributionStatus) -> Unit,
    onNextStageEnabledChanged: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var distributionStatus by remember { mutableStateOf<FirmwareDistributionFirmwareStatus?>(null) }
    var capabilitiesStatus by remember(capabilitiesStatus) { mutableStateOf(capabilitiesStatus) }
    var error by rememberSaveable { mutableStateOf<Throwable?>(null) }
    var isCapabilitiesRequestInProgress by rememberSaveable { mutableStateOf(false) }
    var isDeleteFirmwareInProgress by rememberSaveable { mutableStateOf(false) }
    // Not remembered, as the capabilities state is recreated when the capabilities change, and a
    // remembered value would keep reading the previous state.
    val availableEntries = capabilitiesStatus?.let { capabilities ->
        distributionStatus?.let { (capabilities.maxFirmwareImagesListSize - it.entryCount).toInt() }
    }
    // The phases, in which the distribution has to be canceled before the Distributor is idle again.
    val needsReset = phase == FirmwareDistributionPhase.COMPLETED ||
            phase == FirmwareDistributionPhase.FAILED ||
            phase == FirmwareDistributionPhase.APPLYING_UPDATE
    val isTransferInProgress = phase == FirmwareDistributionPhase.TRANSFER_ACTIVE ||
            phase == FirmwareDistributionPhase.TRANSFER_SUCCESS ||
            phase == FirmwareDistributionPhase.TRANSFER_SUSPENDED

    /** Reads the number of the stored firmware images from the Distributor. */
    suspend fun readStoredImages() {
        val status = send(model, FirmwareDistributionFirmwareGetByIndex(imageIndex = 0u))
                as? FirmwareDistributionFirmwareStatus
            ?: throw IllegalStateException("No response received")
        // No stored images is reported as firmware not found.
        if (status.status != FirmwareDistributionMessageStatus.SUCCESS &&
            status.status != FirmwareDistributionMessageStatus.FIRMWARE_NOT_FOUND
        ) {
            throw IllegalStateException(status.status.toString())
        }
        distributionStatus = status
    }

    Row(
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
                    isCapabilitiesRequestInProgress = true
                    try {
                        capabilitiesStatus = send(
                            model,
                            FirmwareDistributionCapabilitiesGet()
                        ) as? FirmwareDistributionCapabilitiesStatus
                        readStoredImages()
                    } catch (e: Exception) {
                        error = e
                    } finally {
                        isCapabilitiesRequestInProgress = false
                    }
                }
            },
            enabled = !isCapabilitiesRequestInProgress,
            isOnClickActionInProgress = isCapabilitiesRequestInProgress
        )
    }
    MaxReceiversListSize(
        receiversSize = capabilitiesStatus?.maxReceiversCount?.toInt(),
        titleAction = {
            capabilitiesStatus?.let {
                Icon(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    imageVector = when (it.maxReceiversCount > 0u) {
                        true -> Icons.Outlined.CheckCircle
                        else -> Icons.Rounded.WarningAmber
                    },
                    tint = when (it.maxReceiversCount > 0u) {
                        true -> Color.Green
                        else -> Color(red = 1f, green = 0.6f, blue = 0f)
                    },
                    contentDescription = null
                )
            } ?: run {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(size = 24.dp)
                )
            }
        }
    )
    MaxFirmwareImagesListSize(
        title = stringResource(R.string.label_available_firmware_image_entries),
        imageListSize = capabilitiesStatus?.maxFirmwareImagesListSize?.toInt(),
        subtitle = if (availableEntries != null) {
            "$availableEntries / ${capabilitiesStatus!!.maxFirmwareImagesListSize}"
        } else stringResource(R.string.label_unknown),
        titleAction = {
            if (capabilitiesStatus == null || distributionStatus == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(size = 24.dp)
                )
            } else {
                availableEntries?.let {
                    if (it > 0 && !needsReset) {
                        Icon(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            imageVector = Icons.Outlined.CheckCircle,
                            tint = Color.Green,
                            contentDescription = null
                        )
                    } else {
                        MeshIconButton(
                            modifier = Modifier.padding(end = 8.dp),
                            buttonIcon = Icons.Rounded.WarningAmber,
                            buttonIconTint = Color(red = 1f, green = 0.6f, blue = 0f),
                            onClick = {
                                scope.launch {
                                    isDeleteFirmwareInProgress = true
                                    try {
                                        // The distribution that is over has to be canceled first, so
                                        // that the Distributor is idle again.
                                        if (needsReset) {
                                            (send(model, FirmwareDistributionCancel())
                                                    as? FirmwareDistributionStatus)
                                                ?.let(onDistributionStatusChanged)
                                        }
                                        // The stored images are deleted when there is no space left.
                                        if (availableEntries == 0) {
                                            distributionStatus = send(
                                                model,
                                                FirmwareDistributionFirmwareDeleteAll()
                                            ) as? FirmwareDistributionFirmwareStatus
                                            // The remaining upload space has changed.
                                            capabilitiesStatus = send(
                                                model,
                                                FirmwareDistributionCapabilitiesGet()
                                            ) as? FirmwareDistributionCapabilitiesStatus
                                        }
                                    } catch (e: Exception) {
                                        error = e
                                    } finally {
                                        isDeleteFirmwareInProgress = false
                                    }
                                }
                            },
                            // The Distributor cannot be freed while a distribution is in progress.
                            enabled = !phase.isBusy,
                            isOnClickActionInProgress = isDeleteFirmwareInProgress
                        )
                    }
                }
            }
        }
    )
    MaxFirmwareImageSize(
        firmwareImageSize = capabilitiesStatus?.maxFirmwareImageSize?.toInt(),
        titleAction = {
            capabilitiesStatus?.let {
                Icon(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    imageVector = when (it.maxFirmwareImageSize > 0u) {
                        true -> Icons.Outlined.CheckCircle
                        else -> Icons.Rounded.WarningAmber
                    },
                    tint = when (it.maxFirmwareImageSize > 0u) {
                        true -> Color.Green
                        else -> Color(red = 1f, green = 0.6f, blue = 0f)
                    },
                    contentDescription = null
                )
            } ?: run {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(size = 24.dp)
                )
            }
        }
    )
    MaxUploadSpace(
        uploadSpace = capabilitiesStatus?.maxUploadSpace?.toInt(),
        titleAction = {
            capabilitiesStatus?.let {
                Icon(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    imageVector = when (it.maxUploadSpace > 0u) {
                        true -> Icons.Outlined.CheckCircle
                        else -> Icons.Rounded.WarningAmber
                    },
                    tint = when (it.maxUploadSpace > 0u) {
                        true -> Color.Green
                        else -> Color(red = 1f, green = 0.6f, blue = 0f)
                    },
                    contentDescription = null
                )
            } ?: run {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(size = 24.dp)
                )
            }
        }
    )
    RemainingUploadSpace(
        remainingUploadSpace = capabilitiesStatus?.remainingUploadSpace?.toInt(),
        titleAction = {
            capabilitiesStatus?.let {
                Icon(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    imageVector = when (it.remainingUploadSpace > 0u) {
                        true -> Icons.Outlined.CheckCircle
                        else -> Icons.Rounded.WarningAmber
                    },
                    tint = when (it.remainingUploadSpace > 0u) {
                        true -> Color.Green
                        else -> Color(red = 1f, green = 0.6f, blue = 0f)
                    },
                    contentDescription = null
                )
            } ?: run {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(size = 24.dp)
                )
            }
        }
    )
    LaunchedEffect(capabilitiesStatus == null) {
        if (capabilitiesStatus != null) {
            try {
                readStoredImages()
            } catch (e: Exception) {
                error = e
            } finally {
                isDeleteFirmwareInProgress = false
            }
        }
    }

    // A new firmware update can be started if the Distributor is idle, and there is space for the
    // image and a free slot, or the space is not used yet. A distribution that is in progress can
    // be followed.
    val storedImages = distributionStatus?.entryCount
    val isNextStageSupported = capabilitiesStatus?.let { capabilities ->
        storedImages != null && (
                isTransferInProgress || (
                        phase == FirmwareDistributionPhase.IDLE &&
                                capabilities.maxFirmwareImageSize > 0u &&
                                capabilities.maxUploadSpace > 0u &&
                                capabilities.remainingUploadSpace > 0u && (
                                capabilities.remainingUploadSpace == capabilities.maxUploadSpace ||
                                        capabilities.maxFirmwareImagesListSize > storedImages
                                )
                        )
                )
    } ?: false
    LaunchedEffect(isNextStageSupported) {
        onNextStageEnabledChanged(isNextStageSupported)
    }
}