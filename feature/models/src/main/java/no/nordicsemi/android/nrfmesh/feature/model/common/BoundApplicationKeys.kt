package no.nordicsemi.android.nrfmesh.feature.model.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.Row as KeyRow
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppBind
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigSigModelAppGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigVendorModelAppGet
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BoundApplicationKeys(
    model: Model,
    messageState: MessageState,
    send: (AcknowledgedConfigMessage) -> Unit,
    navigateToBindApplicationKeys: (Model) -> Unit,
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
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.AddLink,
        title = stringResource(R.string.label_application_keys),
        subtitle = pluralStringResource(
            R.plurals.label_bound_application_keys_count,
            model.boundApplicationKeys.size,
            model.boundApplicationKeys.size
        ),
        // Nothing to manage when no keys are bound, so the item is not clickable.
        onClick = model.boundApplicationKeys.takeIf { it.isNotEmpty() }?.let {
            dropUnlessResumed { navigateToBindApplicationKeys(model) }
        }
    )

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
                    unboundKeys.forEach { key ->
                        key.KeyRow(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            onClick = {
                                scope.launch {
                                    bottomSheetState.hide()
                                }.invokeOnCompletion {
                                    if (!bottomSheetState.isVisible) {
                                        showBottomSheet = false
                                    }
                                }
                                send(ConfigModelAppBind(model = model, applicationKey = key))
                            }
                        )
                    }
                }
            }
        )
    }
}