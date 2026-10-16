package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.common.ui.view.CircularIcon
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.dfu.DfuSession
import no.nordicsemi.android.nrfmesh.feature.dfu.R
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Receiver
import no.nordicsemi.kotlin.mesh.core.model.Node

@Composable
internal fun Page5(session: DfuSession) {
    val viewModel = hiltViewModel<Page5ViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // The image is uploaded to the Distributor and distributed when the page is shown, or the
    // distribution that is already in progress is followed.
    LaunchedEffect(session) {
        viewModel.start(session = session)
    }
    Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
        UploadProgress(upload = uiState.upload)
        DistributionProgress(
            distributor = uiState.distributor ?: return,
            distribution = uiState.distribution,
            receivers = uiState.receivers,
            receiverNames = uiState.receiverNames
        )
    }
}

@Composable
private fun UploadProgress(upload: UploadState) {
    SectionTitle(title = stringResource(R.string.label_upload_progress))
    ElevatedCardItem(
        imageVector = Icons.Outlined.FileUpload,
        title = stringResource(R.string.label_firmware_upload),
        subtitle = when (upload) {
            UploadState.Idle -> stringResource(R.string.label_firmware_upload_distributor)
            UploadState.CreatingSlot -> stringResource(R.string.label_creating_slot)
            UploadState.ReadingParameters -> stringResource(R.string.label_reading_parameters)
            is UploadState.Uploading -> stringResource(
                R.string.label_uploading_image,
                (upload.progress * 100).toInt(),
                upload.speedBytesPerSecond / 1000f
            )

            is UploadState.Completed -> stringResource(R.string.label_upload_completed)
            is UploadState.Failed -> stringResource(R.string.label_upload_failed, upload.message)
        },
        subtitleTextColor = when (upload) {
            is UploadState.Failed -> MaterialTheme.colorScheme.error
            else -> Color.Unspecified
        }
    )
    if (upload is UploadState.Uploading) {
        LinearProgressIndicator(
            progress = { upload.progress },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DistributionProgress(
    distributor: Node,
    distribution: DistributionState,
    receivers: List<Receiver>,
    receiverNames: Map<UShort, String>,
) {
    SectionTitle(title = stringResource(R.string.label_distribution_progress))
    MeshItem(
        modifier = Modifier.fillMaxWidth(),
        icon = {
            CircularIcon(painter = painterResource(R.drawable.ic_mesh))
        },
        title = distributor.name,
        subtitle = distributor.primaryUnicastAddress.address
            .toHexString(
                format = HexFormat {
                    number.prefix = "Address: 0x"
                    upperCase = true
                }
            ),
        onClick = { }
    )
    if (distribution != DistributionState.Idle) {
        ElevatedCardItem(
            imageVector = Icons.Outlined.PlayArrow,
            title = stringResource(R.string.label_firmware_distribution),
            subtitle = when (distribution) {
                DistributionState.Idle,
                DistributionState.Starting -> stringResource(R.string.label_starting_distribution)

                is DistributionState.Started -> stringResource(R.string.label_distribution_started)
                DistributionState.Completed -> stringResource(R.string.label_distribution_completed)
                is DistributionState.Failed -> stringResource(
                    R.string.label_distribution_failed,
                    distribution.message
                )
            },
            subtitleTextColor = when (distribution) {
                is DistributionState.Failed -> MaterialTheme.colorScheme.error
                else -> Color.Unspecified
            }
        )
    }
    if (receivers.isNotEmpty()) {
        SectionTitle(title = stringResource(R.string.label_receivers))
        receivers.forEach { receiver ->
            ReceiverItem(
                name = receiverNames[receiver.address.address]
                    ?: receiver.address.address.toHexString(
                        format = HexFormat {
                            number.prefix = "0x"
                            upperCase = true
                        }
                    ),
                receiver = receiver
            )
        }
    }
}

@Composable
private fun ReceiverItem(name: String, receiver: Receiver) {
    val status = receiver.status
    Column {
        MeshItem(
            modifier = Modifier.fillMaxWidth(),
            icon = {
                CircularIcon(painter = painterResource(R.drawable.ic_mesh))
            },
            title = name,
            subtitle = when (status) {
                Receiver.Status.Idle -> stringResource(R.string.label_receiver_idle)
                is Receiver.Status.Distribution -> stringResource(
                    R.string.label_receiver_distributing,
                    status.progress,
                    status.speedBytesPerSecond
                )

                Receiver.Status.Verified -> stringResource(R.string.label_receiver_verified)
                Receiver.Status.Applied -> stringResource(R.string.label_receiver_applied)
                Receiver.Status.Failure -> stringResource(R.string.label_receiver_failed)
            },
            onClick = { }
        )
        if (status is Receiver.Status.Distribution) {
            LinearProgressIndicator(
                progress = { status.progress / 100f },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}