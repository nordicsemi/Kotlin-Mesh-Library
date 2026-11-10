package no.nordicsemi.android.nrfmesh.feature.model.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
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
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshAlertDialog
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppBind
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppUnbind
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigSigModelAppGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigVendorModelAppGet
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import no.nordicsemi.android.nrfmesh.core.ui.Row as KeyRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BoundApplicationKeys(
    model: Model,
    messageState: MessageState,
    send: (AcknowledgedConfigMessage) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle(
            modifier = Modifier
                .weight(weight = 1f)
                .padding(horizontal = 16.dp),
            title = stringResource(R.string.label_bound_application_keys)
        )
        MeshIconButton(
            onClick = {
                send(
                    when (model.modelId) {
                        is SigModelId -> {
                            ConfigSigModelAppGet(
                                elementAddress = model.parentElement?.unicastAddress
                                    ?: throw IllegalStateException("Model should have a parent element with a unicast address"),
                                modelId = model.modelId as SigModelId
                            )
                        }

                        is VendorModelId -> ConfigVendorModelAppGet(
                            elementAddress = model.parentElement?.unicastAddress
                                ?: throw IllegalStateException("Model should have a parent element with a unicast address"),
                            modelId = model.modelId as VendorModelId
                        )
                    }
                )
            },
            buttonIcon = Icons.Outlined.Refresh,
            enabled = !messageState.isInProgress(),
            isOnClickActionInProgress = messageState.isInProgress() &&
                    messageState.message is ConfigSigModelAppGet,
        )
        MeshIconButton(
            onClick = { showBottomSheet = true },
            buttonIcon = Icons.Outlined.Add,
            enabled = !messageState.isInProgress(),
            isOnClickActionInProgress = messageState.isInProgress() &&
                    messageState.message is ConfigModelAppBind,
        )
    }
    if (model.boundApplicationKeys.isNotEmpty()) {
        model.boundApplicationKeys.forEach {
            // A key is used to ensure that the items are stable and do not cause recomposition
            // issues.
            key(it.index) {
                BoundApplicationKeyRow(model = model, applicationKey = it, send = send)
            }
        }
    } else {
        ElevatedCardItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            imageVector = Icons.Outlined.AddLink,
            title = stringResource(R.string.label_application_keys),
            subtitle = stringResource(R.string.label_no_keys_bound)
        )
    }

    if (showBottomSheet) {
        val unboundKeys = model.parentElement?.parentNode?.applicationKeys.orEmpty()
            .filterNot { it.isBoundTo(model = model) }
        ModalBottomSheet(
            sheetState = bottomSheetState,
            onDismissRequest = { showBottomSheet = false },
            content = {
                SectionTitle(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    title = stringResource(R.string.label_bind_application_keys)
                )
                Column(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .verticalScroll(state = rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(space = 8.dp)
                ) {
                    if (unboundKeys.isEmpty()) {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = stringResource(R.string.label_no_app_keys_to_bind)
                        )
                    }
                    unboundKeys.forEach { applicationKey ->
                        applicationKey.KeyRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            onClick = {
                                scope.launch {
                                    bottomSheetState.hide()
                                }.invokeOnCompletion {
                                    if (!bottomSheetState.isVisible) {
                                        showBottomSheet = false
                                    }
                                }
                                send(
                                    ConfigModelAppBind(
                                        model = model,
                                        applicationKey = applicationKey
                                    )
                                )
                            }
                        )
                    }
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoundApplicationKeyRow(
    model: Model,
    applicationKey: ApplicationKey,
    send: (AcknowledgedConfigMessage) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState()
    var showWarningDialog by rememberSaveable { mutableStateOf(false) }
    SwipeToDismissBox(
        modifier = Modifier.padding(horizontal = 16.dp),
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.Settled -> Color.Gray
                    SwipeToDismissBoxValue.StartToEnd,
                    SwipeToDismissBoxValue.EndToStart -> Color.Red
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = color, shape = CardDefaults.elevatedShape)
                    .padding(horizontal = 16.dp),
                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                    Alignment.CenterStart
                else Alignment.CenterEnd
            ) {
                Icon(imageVector = Icons.Outlined.Delete, contentDescription = null)
            }
        },
        onDismiss = {
            // Unbinding a key that is used by the publication clears the publication, so confirm.
            if (model.publish?.index == applicationKey.index) {
                scope.launch { dismissState.reset() }
                showWarningDialog = true
            } else {
                send(ConfigModelAppUnbind(model = model, applicationKey = applicationKey))
            }
        },
        content = { applicationKey.KeyRow() }
    )

    if (showWarningDialog) {
        MeshAlertDialog(
            onDismissRequest = { showWarningDialog = false },
            icon = Icons.Outlined.Warning,
            iconColor = MaterialTheme.colorScheme.error,
            title = stringResource(R.string.label_warning),
            text = stringResource(R.string.label_unbind_application_key_rationale),
            dismissButtonText = stringResource(R.string.label_cancel),
            onDismissClick = { showWarningDialog = false },
            confirmButtonText = stringResource(R.string.label_ok),
            onConfirmClick = {
                showWarningDialog = false
                send(ConfigModelAppUnbind(model = model, applicationKey = applicationKey))
            }
        )
    }
}
