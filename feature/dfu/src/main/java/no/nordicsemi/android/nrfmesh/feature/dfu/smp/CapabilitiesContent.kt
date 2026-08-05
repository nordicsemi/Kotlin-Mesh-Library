package no.nordicsemi.android.nrfmesh.feature.dfu.smp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.ui.MaxFirmwareImageSize
import no.nordicsemi.android.nrfmesh.core.ui.MaxFirmwareImagesListSize
import no.nordicsemi.android.nrfmesh.core.ui.MaxReceiversListSize
import no.nordicsemi.android.nrfmesh.core.ui.MaxUploadSpace
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.RemainingUploadSpace
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.core.ui.SupportedUriSchemes
import no.nordicsemi.android.nrfmesh.feature.dfu.R
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionCapabilitiesGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionCapabilitiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionFirmwareDeleteAll
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionFirmwareStatus
import no.nordicsemi.kotlin.mesh.core.model.Model

@Composable
internal fun CapabilitiesContent(
    capabilitiesStatus: FirmwareDistributionCapabilitiesStatus?,
    model: Model,
    send: suspend (Model, AcknowledgedMeshMessage) -> MeshMessage?,
) {
    val scope = rememberCoroutineScope()
    var distributionStatus by remember { mutableStateOf<FirmwareDistributionFirmwareStatus?>(null) }
    var capabilitiesStatus by remember(capabilitiesStatus) { mutableStateOf(capabilitiesStatus) }
    var error by rememberSaveable { mutableStateOf<Throwable?>(null) }
    var isCapabilitiesRequestInProgress by rememberSaveable { mutableStateOf(false) }
    var isDeleteFirmwareInProgress by rememberSaveable { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(
            modifier = Modifier.weight(weight = 1f),
            title = stringResource(R.string.label_capabilities)
        )
        MeshIconButton(
            buttonIcon = Icons.Outlined.ClearAll,
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
            enabled = !isDeleteFirmwareInProgress && !isCapabilitiesRequestInProgress,
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
            if (capabilitiesStatus == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(size = 24.dp)
                )
            } else {
                Icon(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    imageVector = Icons.Outlined.CheckCircle,
                    tint = Color.Green,
                    contentDescription = null
                )
            }
        }
    )
    MaxFirmwareImagesListSize(
        imageListSize = capabilitiesStatus?.maxFirmwareImagesListSize?.toInt(),
        titleAction = {
            if (capabilitiesStatus == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(size = 24.dp)
                )
            } else {
                Icon(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    imageVector = Icons.Outlined.CheckCircle,
                    tint = Color.Green,
                    contentDescription = null
                )
            }
        }
    )
    MaxFirmwareImageSize(
        firmwareImageSize = capabilitiesStatus?.maxFirmwareImageSize?.toInt(),
        titleAction = {
            if (capabilitiesStatus == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(size = 24.dp)
                )
            } else {
                Icon(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    imageVector = Icons.Outlined.CheckCircle,
                    tint = Color.Green,
                    contentDescription = null
                )
            }
        }
    )
    MaxUploadSpace(
        uploadSpace = capabilitiesStatus?.maxUploadSpace?.toInt(),
        titleAction = {
            if (capabilitiesStatus == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(size = 24.dp)
                )
            } else {
                Icon(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    imageVector = Icons.Outlined.CheckCircle,
                    tint = Color.Green,
                    contentDescription = null
                )
            }
        }
    )
    RemainingUploadSpace(
        remainingUploadSpace = capabilitiesStatus?.remainingUploadSpace?.toInt(),
        titleAction = {
            if (capabilitiesStatus == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(size = 24.dp)
                )
            } else {
                Icon(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    imageVector = when (capabilitiesStatus?.remainingUploadSpace?.toInt() != 0) {
                        true -> Icons.Outlined.CheckCircle
                        else -> Icons.Outlined.Error
                    },
                    tint = when (capabilitiesStatus?.remainingUploadSpace?.toInt() != 0) {
                        true -> Color.Green
                        else -> Color.Red
                    },
                    contentDescription = null
                )
            }
        }
    )
    SupportedUriSchemes(
        uriSchemes = capabilitiesStatus?.supportedUriSchemes,
        titleAction = {
            if (capabilitiesStatus == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(size = 24.dp)
                )
            } else {
                Icon(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    imageVector = Icons.Outlined.CheckCircle,
                    tint = Color.Green,
                    contentDescription = null
                )
            }
        }
    )
}