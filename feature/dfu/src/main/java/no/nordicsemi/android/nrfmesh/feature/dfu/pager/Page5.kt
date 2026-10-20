package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import no.nordicsemi.android.common.ui.view.CircularIcon
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshAlertDialog
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.dfu.DfuSession
import no.nordicsemi.android.nrfmesh.feature.dfu.R
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Receiver

@Composable
internal fun Page5(
    session: DfuSession,
    onUploadCompletedChanged: (Boolean) -> Unit,
    onCancelled: () -> Unit,
) {
    val viewModel = hiltViewModel<Page5ViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // The image is uploaded to the Distributor and distributed when the page is shown, or the
    // distribution that is already in progress is followed.
    LaunchedEffect(session) {
        viewModel.start(session = session)
    }
    val scope = rememberCoroutineScope()
    var isCancelDialogShown by rememberSaveable { mutableStateOf(false) }
    // Once the image is on the Distributor, the distribution continues without the phone.
    LaunchedEffect(uiState.upload is UploadState.Completed) {
        onUploadCompletedChanged(uiState.upload is UploadState.Completed)
    }
    Column(modifier = Modifier.fillMaxSize()) {
        // The progress is scrollable, while the button stays at the bottom of the screen.
        Column(
            modifier = Modifier
                .weight(weight = 1f)
                .verticalScroll(state = rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            UploadProgress(upload = uiState.upload)
            DistributionProgress(
                distribution = uiState.distribution,
                receivers = uiState.receivers,
                receiverNames = uiState.receiverNames
            )
        }
        CancelButton(
            isCancelling = uiState.isCancelling,
            error = uiState.cancelError,
            onClick = { isCancelDialogShown = true }
        )
    }
    if (isCancelDialogShown) {
        MeshAlertDialog(
            onDismissRequest = { isCancelDialogShown = false },
            icon = Icons.Outlined.Cancel,
            iconColor = MaterialTheme.colorScheme.error,
            title = stringResource(R.string.label_cancel_update_title),
            text = stringResource(R.string.label_cancel_update_message),
            confirmButtonText = stringResource(R.string.label_confirm),
            onConfirmClick = {
                isCancelDialogShown = false
                scope.launch {
                    // The screen is closed when the firmware update has been canceled.
                    if (viewModel.cancel()) onCancelled()
                }
            },
            dismissButtonText = stringResource(R.string.label_cancel),
            onDismissClick = { isCancelDialogShown = false }
        )
    }
}

/**
 * A button that cancels the whole firmware update, that is the upload and the distribution.
 *
 * @param isCancelling Whether the firmware update is being cancelled.
 * @param error        The reason why the firmware update could not be cancelled, if it failed.
 * @param onClick      Invoked when the button is clicked.
 */
@Composable
private fun CancelButton(isCancelling: Boolean, error: String?, onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.error
    OutlinedButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        onClick = onClick,
        enabled = !isCancelling,
        border = BorderStroke(
            width = 1.dp,
            color = color.copy(alpha = if (isCancelling) 0.38f else 1f)
        ),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = color)
    ) {
        Text(
            text = stringResource(
                if (isCancelling) R.string.label_cancelling else R.string.label_cancel_update
            )
        )
    }
    error?.let {
        Text(
            modifier = Modifier.padding(horizontal = 8.dp),
            text = stringResource(R.string.label_cancel_failed, it),
            color = color,
            style = MaterialTheme.typography.bodySmall
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
        },
        titleAction = {
            if (upload is UploadState.Uploading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(24.dp),
                    progress = { upload.progress }
                )
            }
        }
    )
}

@Composable
private fun DistributionProgress(
    distribution: DistributionState,
    receivers: List<Receiver>,
    receiverNames: Map<UShort, String>,
) {
    if (distribution != DistributionState.Idle) {
        SectionTitle(title = stringResource(R.string.label_distribution_progress))
        ElevatedCardItem(
            imageVector = Icons.Outlined.PlayArrow,
            title = stringResource(R.string.label_firmware_distribution),
            subtitle = when (distribution) {
                DistributionState.Idle,
                DistributionState.Starting,
                    -> stringResource(R.string.label_starting_distribution)

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
        ElevatedCardItem(
            leadingIcon = {
                Spacer(modifier = Modifier.size(size = 16.dp))
                CircularIcon(painter = painterResource(R.drawable.ic_mesh))
                Spacer(modifier = Modifier.size(size = 16.dp))
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
            titleAction = {
                if (status is Receiver.Status.Distribution) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(24.dp),
                        progress = { status.progress / 100f },
                    )
                }
            }
        )
    }
}