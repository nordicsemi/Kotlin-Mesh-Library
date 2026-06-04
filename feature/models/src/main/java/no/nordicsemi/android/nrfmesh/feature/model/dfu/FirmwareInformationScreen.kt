package no.nordicsemi.android.nrfmesh.feature.model.dfu

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.DatasetLinked
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedHexTextField
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareId
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareInformation
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdateAdditionalInformation
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdateMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionApply
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareUpdateApply
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareUpdateFirmwareMetadataCheck
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareUpdateFirmwareMetadataStatus
import no.nordicsemi.kotlin.mesh.core.util.CompanyIdentifier
import java.net.URL
import androidx.core.net.toUri

@Composable
internal fun FirmwareInformationScreen(
    isInProgress: Boolean,
    title: String,
    information: FirmwareInformation,
    send: suspend (AcknowledgedMeshMessage) -> MeshMessage?,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    Box {
        Column(
            modifier = Modifier
                .verticalScroll(state = rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FirmwareInformation(title = title, firmwareId = information.currentFirmwareId)
            FirmwareUpdate(
                snackbarHostState = snackbarHostState,
                url = information.updateUri,
                firmwareId = information.currentFirmwareId,
                isInProgress = isInProgress
            )
            FirmwareCompatibility(
                snackbarHostState = snackbarHostState,
                isInProgress = isInProgress,
                send = send
            )
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun FirmwareInformation(title: String, firmwareId: FirmwareId) {
    SectionTitle(modifier = Modifier.padding(horizontal = 16.dp), title = title)
    CompanyIdentifier(companyIdentifier = firmwareId.companyIdentifier)
    Version(version = firmwareId.versionString)
}

@Composable
private fun CompanyIdentifier(companyIdentifier: UShort) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.WorkOutline,
        title = stringResource(id = R.string.label_company),
        subtitle = CompanyIdentifier.name(id = companyIdentifier)
            ?: stringResource(id = R.string.unknown)
    )
}

@Composable
private fun Version(version: String?) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.QrCode,
        title = stringResource(id = R.string.label_version),
        subtitle = version ?: stringResource(id = R.string.unknown)
    )
}

@Composable
private fun FirmwareUpdate(
    snackbarHostState: SnackbarHostState,
    url: URL?,
    firmwareId: FirmwareId,
    isInProgress: Boolean,
) {
    val context = LocalContext.current
    var updatedFirmwareInformation by rememberSaveable(stateSaver = firmwareSaver) {
        mutableStateOf(null)
    }
    val updateUrl = url?.toString()
        ?.replace(oldValue = "192.168.0.173", newValue = "10.0.0.22")
        ?.toUri()
        ?.buildUpon()
        ?.appendPath("check")
        ?.appendQueryParameter("cfwid", firmwareId.bytes.toHexString())
        ?.build()
        ?.let { URL(it.toString()) }
        ?: stringResource(id = R.string.label_unknown)
    val scope = rememberCoroutineScope()
    var error by rememberSaveable { mutableStateOf<Throwable?>(null) }
    var isCheckingForUpdates by rememberSaveable { mutableStateOf(false) }
    var isDownloadInProgress by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(
            modifier = Modifier.weight(weight = 1f),
            title = stringResource(R.string.label_firmware_update)
        )
        MeshIconButton(
            buttonIcon = Icons.Outlined.CloudDownload,
            onClick = dropUnlessResumed {
                scope.launch {
                    try {
                        isDownloadInProgress = true
                        url?.let {
                            val file = downloadFirmware(
                                context = context,
                                url = it,
                                firmwareId = firmwareId
                            )
                            saveToDownloads(context = context, zipFile = file)
                        }
                    } catch (e: Exception) {
                        error = e
                    } finally {
                        isDownloadInProgress = false
                    }
                }
            },
            enabled = !isInProgress && updatedFirmwareInformation != null,
            isOnClickActionInProgress = isDownloadInProgress
        )
        MeshIconButton(
            buttonIcon = Icons.Outlined.Refresh,
            onClick = dropUnlessResumed {
                scope.launch {
                    try {
                        isCheckingForUpdates = true
                        updatedFirmwareInformation = null
                        updatedFirmwareInformation = checkForUpdates(url = updateUrl as URL)
                    } catch (e: Exception) {
                        error = e
                    } finally {
                        isCheckingForUpdates = false
                    }
                }
            },
            enabled = !isInProgress && url != null,
            isOnClickActionInProgress = isCheckingForUpdates
        )
    }
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.DatasetLinked,
        title = stringResource(R.string.label_uri),
        subtitle = updateUrl.toString()
    )
    LaunchedEffect(key1 = error) {
        error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error.message ?: context.getString(R.string.label_failed_to_check_for_updates),
            )
        }
    }
}

@Composable
private fun FirmwareCompatibility(
    snackbarHostState: SnackbarHostState,
    isInProgress: Boolean,
    send: suspend (AcknowledgedMeshMessage) -> MeshMessage?,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<FirmwareUpdateFirmwareMetadataStatus?>(null) }
    var error by rememberSaveable { mutableStateOf<Throwable?>(null) }
    var shouldShowProgressIcon by rememberSaveable { mutableStateOf(false) }
    var opCode by rememberSaveable { mutableStateOf<Int?>(null) }

    var metaData by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = "", selection = TextRange(index = 0)))
    }
    SectionTitle(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = stringResource(R.string.label_compatibility)
    )
    OutlinedCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.padding(start = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountTree,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            MeshOutlinedHexTextField(
                modifier = Modifier
                    .padding(start = 16.dp, end = 8.dp, bottom = 6.dp)
                    .weight(weight = 1f),
                value = metaData,
                onValueChanged = { metaData = it },
                label = { Text(stringResource(id = R.string.label_metadata)) },
                keyboardActions = KeyboardActions(onDone = KeyboardActions.Default.onDone),
                internalTrailingIcon = {
                    MeshIconButton(
                        buttonIcon = Icons.Outlined.DeleteSweep,
                        onClick = dropUnlessResumed {
                            metaData = TextFieldValue(text = "", selection = TextRange(index = 0))
                        },
                        enabled = !isInProgress,
                        isOnClickActionInProgress = shouldShowProgressIcon
                                && opCode == FirmwareUpdateApply.opCode.toInt()
                    )
                },
                regex = Regex("^[0-9a-fA-F]*$")
            )
            MeshIconButton(
                buttonIcon = Icons.Outlined.Refresh,
                onClick = dropUnlessResumed {
                    keyboard?.hide()
                    scope.launch {
                        try {
                            shouldShowProgressIcon = true
                            opCode = FirmwareDistributionApply.opCode.toInt()
                            status = send(
                                FirmwareUpdateFirmwareMetadataCheck(
                                    imageIndex = 0.toUByte(),
                                    metaData = metaData.text.hexToByteArray()
                                )
                            ) as? FirmwareUpdateFirmwareMetadataStatus
                        } catch (e: Exception) {
                            error = e
                            snackbarHostState.showSnackbar(
                                message = e.message
                                    ?: "An error occurred while checking firmware compatibility.",
                            )
                        } finally {
                            shouldShowProgressIcon = false
                        }
                    }
                },
                enabled = !isInProgress && metaData.text.isNotEmpty(),
                isOnClickActionInProgress = shouldShowProgressIcon
                        && opCode == FirmwareUpdateApply.opCode.toInt()
            )
        }
    }
    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = "Metadata are usually generated together with the update package",
        style = MaterialTheme.typography.bodySmall
    )
    Spacer(modifier = Modifier.height(32.dp))
    LaunchedEffect(status) {
        status?.let { metadataStatus ->
            snackbarHostState.showSnackbar(
                message = when (metadataStatus.status) {
                    FirmwareUpdateMessageStatus.SUCCESS ->
                        context.getString(
                            R.string.label_firmware_compatibility_rationale,
                            metadataStatus.additionalInformation.rationale(context = context)
                        )

                    else -> context.getString(R.string.label_firmware_compatibility_check_failed_rationale)
                },
                actionLabel = context.getString(R.string.label_ok)
            )
        }
    }
}

private fun FirmwareUpdateAdditionalInformation.rationale(context: Context) = when (this) {
    FirmwareUpdateAdditionalInformation.DEVICE_UNPROVISIONED ->
        context.getString(R.string.label_device_unprovisioned_rationale)

    FirmwareUpdateAdditionalInformation.COMPOSITION_DATA_UNCHANGED ->
        context.getString(R.string.label_composition_data_will_not_change_rationale)

    FirmwareUpdateAdditionalInformation.COMPOSITION_DATA_CHANGED_AND_RPR_SUPPORTED ->
        context.getString(R.string.label_composition_data_will_change_remote_provisioning_will_be_supported_rationale)

    FirmwareUpdateAdditionalInformation.COMPOSITION_DATA_CHANGED_AND_RPR_UNSUPPORTED ->
        context.getString(R.string.label_composition_data_will_change_remote_provisioning_will_not_be_supported_rationale)
}

/**
 * Firmware information saver to be used with rememberSaveable.
 */
private val firmwareSaver = Saver<UpdatedFirmwareInformation?, String>(
    save = { it?.let { Json.encodeToString(it) } ?: "" },
    restore = { if (it.isEmpty()) null else Json.decodeFromString(it) }
)