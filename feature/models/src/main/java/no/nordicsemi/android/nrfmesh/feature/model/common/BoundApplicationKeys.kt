package no.nordicsemi.android.nrfmesh.feature.model.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.bind.appkeys.BindAppKeysScreen
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigSigModelAppGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigVendorModelAppGet
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import kotlin.uuid.ExperimentalUuidApi


@OptIn(ExperimentalMaterial3Api::class, ExperimentalUuidApi::class)
@Composable
internal fun BoundApplicationKeys(
    model: Model,
    messageState: MessageState,
    send: (AcknowledgedConfigMessage) -> Unit,
) {
    val bottomSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = model.parentElement?.parentNode?.applicationKeys?.isEmpty() ?: false
    )
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
            title = pluralStringResource(
                R.plurals.label_bound_application_keys_count,
                model.bind.size,
                model.bind.size
            )
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
    }
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.AddLink,
        title = stringResource(R.string.label_bind_application_keys),
        subtitle = pluralStringResource(
            R.plurals.label_bound_application_keys_count,
            model.boundApplicationKeys.size,
            model.boundApplicationKeys.size),
        onClick = { showBottomSheet = true }
    )
    if (showBottomSheet) {
        ModalBottomSheet(
            containerColor = MaterialTheme.colorScheme.surface,
            sheetState = bottomSheetState,
            onDismissRequest = { showBottomSheet = !showBottomSheet },
            content = {
                BindAppKeysScreen(
                    model = model,
                    send = send
                )
            }
        )
    }
}