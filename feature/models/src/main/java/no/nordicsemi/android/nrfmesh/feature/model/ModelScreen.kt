package no.nordicsemi.android.nrfmesh.feature.model

import android.content.Context
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import no.nordicsemi.android.nrfmesh.core.common.Sending
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.Failed
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.NodeIdentityStatus
import no.nordicsemi.android.nrfmesh.core.common.Utils.describe
import no.nordicsemi.android.nrfmesh.core.common.isFirmwareDistributionServer
import no.nordicsemi.android.nrfmesh.core.common.isFirmwareUpdateServer
import no.nordicsemi.android.nrfmesh.core.common.isGenericLevelServer
import no.nordicsemi.android.nrfmesh.core.common.isGenericOnOffServer
import no.nordicsemi.android.nrfmesh.core.common.isLePairingResponderServer
import no.nordicsemi.android.nrfmesh.core.common.isLightLCServer
import no.nordicsemi.android.nrfmesh.core.common.isLightLCSetupServer
import no.nordicsemi.android.nrfmesh.core.common.isSensorServer
import no.nordicsemi.android.nrfmesh.core.common.isVendorModel
import no.nordicsemi.android.nrfmesh.core.ui.MeshMessageStatusDialog
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.model.common.BoundApplicationKeys
import no.nordicsemi.android.nrfmesh.feature.model.common.CommonInformation
import no.nordicsemi.android.nrfmesh.feature.model.common.Publication
import no.nordicsemi.android.nrfmesh.feature.model.common.Subscriptions
import no.nordicsemi.android.nrfmesh.feature.model.configurationserver.ConfigurationServer
import no.nordicsemi.android.nrfmesh.feature.model.dfu.FirmwareDistributionServer
import no.nordicsemi.android.nrfmesh.feature.model.dfu.FirmwareUpdateServer
import no.nordicsemi.android.nrfmesh.feature.model.generic.GenericLevelServer
import no.nordicsemi.android.nrfmesh.feature.model.generic.GenericOnOffServer
import no.nordicsemi.android.nrfmesh.feature.model.lepairing.LePairingResponder
import no.nordicsemi.android.nrfmesh.feature.model.lighting.LightLCServer
import no.nordicsemi.android.nrfmesh.feature.model.lighting.LightLCSetupServer
import no.nordicsemi.android.nrfmesh.feature.model.sensor.SensorServer
import no.nordicsemi.android.nrfmesh.feature.model.vendor.VendorModelControls
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigStatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareInformation
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.model.Model

@Composable
internal fun ModelScreen(
    snackbarHostState: SnackbarHostState,
    messageState: MessageState,
    nodeIdentityStates: List<NodeIdentityStatus>,
    modelState: ModelState.Success,
    send: (AcknowledgedConfigMessage) -> Unit,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
    sendAcknowledgedMessage: suspend (Model, AcknowledgedMeshMessage) -> MeshMessage?,
    requestNodeIdentityStates: (Model) -> Unit,
    resetMessageState: () -> Unit,
    onAddGroupClicked: () -> Unit,
    navigateToGroups: () -> Unit,
    navigateToFirmwareInformation: (String, Model, FirmwareInformation) -> Unit,
    navigateToBindApplicationKeys: (Model) -> Unit,
    onRelatedModelsClicked: (Model) -> Unit,
    startPairing: (Context) -> Unit,
) {
    // When entering this screen the TextFields automatically gets focused causing the keyboard
    // to show up. This is a known issue and the workaround is to make the column focusable to
    // clear focus from the TextFields.
    // This workaround applies to the TextFields in the VendorModelControls.
    // The reason for applying to the parent composable is to avoid scrolling directly to the
    // TextField when focused.
    // See issue: https://issuetracker.google.com/issues/445720462
    val model = modelState.model
    // Set by screens that send a series of messages, so that the controls of the other sections
    // stay disabled between the messages instead of being toggled with each of them.
    var isBatchInProgress by remember { mutableStateOf(false) }
    val sectionsMessageState = remember(messageState, isBatchInProgress) {
        messageState.message
            ?.takeIf { isBatchInProgress && messageState !is Failed }
            ?.let { Sending(message = it) }
            ?: messageState
    }
    Column(
        modifier = Modifier
            .verticalScroll(state = rememberScrollState())
            .focusable(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        SectionTitle(
            modifier = Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp),
            title = stringResource(R.string.label_model)
        )
        CommonInformation(model = model,  onRelatedModelsClicked = onRelatedModelsClicked)
        if (model.isConfigurationServer) {
            ConfigurationServer(
                snackbarHostState = snackbarHostState,
                messageState = sectionsMessageState,
                model = model,
                nodeIdentityStates = nodeIdentityStates,
                send = send,
                requestNodeIdentityStates = requestNodeIdentityStates,
                onAddGroupClicked = onAddGroupClicked,
            )
        }
        if (model.supportsApplicationKeyBinding) {
            BoundApplicationKeys(
                model = model,
                messageState = sectionsMessageState,
                send = send,
                navigateToBindApplicationKeys = navigateToBindApplicationKeys
            )
        }
        if (model.supportsModelPublication) {
            Publication(
                snackbarHostState = snackbarHostState,
                messageState = sectionsMessageState,
                model = model,
                send = send
            )
        }
        if (model.supportsModelSubscription) {
            Subscriptions(
                snackbarHostState = snackbarHostState,
                messageState = sectionsMessageState,
                model = model,
                navigateToGroups = navigateToGroups,
                send = send
            )
        }
        when {
            model.isGenericOnOffServer() ->
                GenericOnOffServer(
                    model = model,
                    messageState = messageState,
                    sendApplicationMessage = sendApplicationMessage
                )

            model.isGenericLevelServer() ->
                GenericLevelServer(
                    model = model,
                    messageState = messageState,
                    sendApplicationMessage = sendApplicationMessage
                )

            model.isLightLCServer() ->
                LightLCServer(
                    model = model,
                    messageState = messageState,
                    sendApplicationMessage = sendApplicationMessage
                )

            model.isLightLCSetupServer() ->
                LightLCSetupServer(
                    model = model,
                    messageState = messageState,
                    onReadingAll = { isBatchInProgress = it },
                    sendApplicationMessage = sendApplicationMessage
                )

            model.isSensorServer() ->
                SensorServer(
                    model = model,
                    messageState = messageState,
                    sendApplicationMessage = sendApplicationMessage
                )

            model.isFirmwareDistributionServer() ->
                FirmwareDistributionServer(
                    model = model,
                    isInProgress = messageState.isInProgress(),
                    send = sendAcknowledgedMessage,
                )

            model.isFirmwareUpdateServer() ->
                FirmwareUpdateServer(
                    model = model,
                    isInProgress = messageState.isInProgress(),
                    onFirmwareInformationPressed = navigateToFirmwareInformation,
                    send = sendAcknowledgedMessage,
                )

            model.isLePairingResponderServer() ->
                LePairingResponder(
                    model = model,
                    isInProgress = messageState.isInProgress(),
                    send = sendAcknowledgedMessage,
                    startPairing = startPairing,
                )

            model.isVendorModel() ->
                VendorModelControls(
                    model = model,
                    messageState = messageState,
                    sendApplicationMessage = sendApplicationMessage
                )
        }
        Spacer(modifier = Modifier.size(size = 8.dp))
    }

    when (messageState) {
        is Failed -> MeshMessageStatusDialog(
            text = messageState.error.describe(),
            showDismissButton = !messageState.didFail(),
            onDismissRequest = resetMessageState,
        )

        is Completed -> messageState.response
            ?.takeIf { (it is ConfigStatusMessage && !it.isSuccess) }
            ?.let {
                MeshMessageStatusDialog(
                    text = (messageState.response as ConfigStatusMessage).message,
                    showDismissButton = true,
                    onDismissRequest = resetMessageState,
                )
            }

        else -> {}
    }
}