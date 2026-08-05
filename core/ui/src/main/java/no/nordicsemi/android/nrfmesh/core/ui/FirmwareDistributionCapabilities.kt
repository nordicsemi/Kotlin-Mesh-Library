package no.nordicsemi.android.nrfmesh.core.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Schema
import androidx.compose.material.icons.outlined.SdCard
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.SecurityUpdate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import no.nordicsemi.kotlin.mesh.core.model.UriScheme


@Composable
fun MaxReceiversListSize(
    modifier: Modifier = Modifier,
    receiversSize: Int?,
    titleAction: @Composable () -> Unit = {},
) {
    ElevatedCardItem(
        modifier = modifier,
        imageVector = Icons.Outlined.Download,
        title = stringResource(R.string.label_max_receivers_list_size),
        subtitle = receiversSize?.toString() ?: stringResource(R.string.label_unknown),
        titleAction = titleAction
    )
}

@Composable
fun MaxFirmwareImagesListSize(
    modifier: Modifier = Modifier,
    imageListSize: Int?,
    titleAction: @Composable () -> Unit = {},
) {
    ElevatedCardItem(
        modifier = modifier,
        imageVector = Icons.Outlined.SecurityUpdate,
        title = stringResource(R.string.label_max_firmware_images_list_size),
        subtitle = imageListSize?.toString() ?: stringResource(R.string.label_unknown),
        titleAction = titleAction
    )
}

@Composable
fun MaxFirmwareImageSize(
    modifier: Modifier = Modifier,
    firmwareImageSize: Int?,
    titleAction: @Composable () -> Unit = {},
) {
    ElevatedCardItem(
        modifier = modifier,
        imageVector = Icons.Outlined.SecurityUpdate,
        title = stringResource(R.string.label_max_firmware_image_size),
        subtitle = firmwareImageSize
            ?.let { stringResource(R.string.label_value_in_bytes, it) }
            ?: stringResource(R.string.label_unknown),
        titleAction = titleAction
    )
}

@Composable
fun MaxUploadSpace(
    modifier: Modifier = Modifier,
    uploadSpace: Int?,
    titleAction: @Composable () -> Unit = {},
) {
    ElevatedCardItem(
        modifier = modifier,
        imageVector = Icons.Outlined.SdCard,
        title = stringResource(R.string.label_max_upload_space),
        subtitle = uploadSpace
            ?.let { stringResource(R.string.label_value_in_bytes, it) }
            ?: stringResource(R.string.label_unknown),
        titleAction = titleAction
    )
}

@Composable
fun RemainingUploadSpace(
    modifier: Modifier = Modifier,
    remainingUploadSpace: Int?,
    titleAction: @Composable () -> Unit = {},
) {
    ElevatedCardItem(
        modifier = modifier,
        imageVector = Icons.Outlined.SdStorage,
        title = stringResource(R.string.label_remaining_upload_space),
        subtitle = remainingUploadSpace
            ?.let { stringResource(R.string.label_value_in_bytes, it) }
            ?: stringResource(R.string.label_unknown),
        titleAction = titleAction
    )
}

@Composable
fun SupportedUriSchemes(
    modifier: Modifier = Modifier,
    uriSchemes: List<UriScheme>?,
    titleAction: @Composable () -> Unit = {},
) {
    ElevatedCardItem(
        modifier = modifier,
        imageVector = Icons.Outlined.Schema,
        title = stringResource(R.string.label_supported_uri_schemes),
        subtitle = uriSchemes
            ?.let {
                it.takeIf { schemes -> schemes.isNotEmpty() }
                    ?.joinToString(separator = ", ")
                    ?: stringResource(R.string.label_none)
            } ?: stringResource(R.string.label_unknown),
        titleAction = titleAction
    )
}