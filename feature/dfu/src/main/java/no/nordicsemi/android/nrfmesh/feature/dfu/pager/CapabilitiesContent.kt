package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SdCardAlert
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.ui.MaxFirmwareImageSize
import no.nordicsemi.android.nrfmesh.core.ui.MaxFirmwareImagesListSize
import no.nordicsemi.android.nrfmesh.core.ui.MaxReceiversListSize
import no.nordicsemi.android.nrfmesh.core.ui.MaxUploadSpace
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.RemainingUploadSpace
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.dfu.R
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionPhase
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionCapabilitiesGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionCapabilitiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionFirmwareDeleteAll
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionFirmwareGetByIndex
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionFirmwareStatus
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.Model

@Composable
internal fun CapabilitiesContent(
    selectedKey: ApplicationKey,
    phase: FirmwareDistributionPhase,
    capabilitiesStatus: FirmwareDistributionCapabilitiesStatus?,
    model: Model,
    send: suspend (Model, AcknowledgedMeshMessage) -> MeshMessage?,
    enableNextStage: () -> Unit,
    messageState: MessageState,
) {
    val scope = rememberCoroutineScope()
    var distributionStatus by remember { mutableStateOf<FirmwareDistributionFirmwareStatus?>(null) }
    var capabilitiesStatus by remember(capabilitiesStatus) { mutableStateOf(capabilitiesStatus) }
    var error by rememberSaveable { mutableStateOf<Throwable?>(null) }
    var isCapabilitiesRequestInProgress by rememberSaveable { mutableStateOf(false) }
    var isDeleteFirmwareInProgress by rememberSaveable { mutableStateOf(false) }
    val availableEntries by remember {
        derivedStateOf {
            if (distributionStatus != null && capabilitiesStatus != null) {
                (capabilitiesStatus!!.maxFirmwareImagesListSize - distributionStatus!!.entryCount).toInt()
            } else {
                null
            }
        }
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
            buttonIcon = Icons.Outlined.SdCardAlert,
            buttonIconTint = Color(red = 1f, green = 0.6f, blue = 0f),
            onClick = {
                scope.launch {
                    isDeleteFirmwareInProgress = true
                    try {
                        distributionStatus = send(
                            model,
                            FirmwareDistributionFirmwareDeleteAll()
                        ) as? FirmwareDistributionFirmwareStatus
                    } catch (e: Exception) {
                        error = e
                    } finally {
                        isDeleteFirmwareInProgress = false
                    }
                }
            },
            enabled = availableEntries == 0,
            isOnClickActionInProgress = isDeleteFirmwareInProgress
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
                        else -> Icons.Outlined.WarningAmber
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
                    if (it > 0) {
                        Icon(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            imageVector = Icons.Outlined.CheckCircle,
                            tint = Color.Green,
                            contentDescription = null
                        )
                    } else {
                        Icon(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            imageVector = Icons.Outlined.WarningAmber,
                            tint = Color(red = 1f, green = 0.6f, blue = 0f),
                            contentDescription = null
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
                        else -> Icons.Outlined.WarningAmber
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
                        else -> Icons.Outlined.WarningAmber
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
                        else -> Icons.Outlined.WarningAmber
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
                distributionStatus = send(
                    model,
                    FirmwareDistributionFirmwareGetByIndex(imageIndex = 0u)
                ) as? FirmwareDistributionFirmwareStatus
            } catch (e: Exception) {
                error = e
            } finally {
                isDeleteFirmwareInProgress = false
            }
        }
    }

    LaunchedEffect(availableEntries) {
        availableEntries?.takeIf {
            it > 0
        }?.let {
            enableNextStage()
        }
    }
}