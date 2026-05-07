package no.nordicsemi.android.nrfmesh.feature.bind.appkeys

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshAlertDialog
import no.nordicsemi.android.nrfmesh.core.ui.MeshNoItemsAvailable
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppBind
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppUnbind
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.Model
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Composable
fun BindAppKeysScreen(
    model: Model,
    addedKeys: List<ApplicationKey> = model.parentElement?.parentNode?.applicationKeys.orEmpty(),
    send: (AcknowledgedConfigMessage) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        SectionTitle(
            title = stringResource(R.string.label_bound_app_keys),
            modifier = Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp)
        )
        when (addedKeys.isNotEmpty()) {
            true -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                items(items = addedKeys, key = { it.index.toInt() + 1 }) { key ->
                    AddedKeyRow(model = model, key = key, send = send)
                }
            }

            false -> model.parentElement?.parentNode?.applicationKeys
                ?.takeIf { it.isEmpty() }
                ?.let {
                    MeshNoItemsAvailable(
                        modifier = Modifier.fillMaxSize(),
                        imageVector = Icons.Outlined.VpnKey,
                        title = stringResource(R.string.label_no_bound_app_keys),
                        rationale = stringResource(R.string.label_bind_an_app_key_rationale),
                    )
                }
        }
    }
}

@Composable
private fun AddedKeyRow(
    model: Model,
    key: ApplicationKey,
    send: (AcknowledgedConfigMessage) -> Unit,
) {
    var isBound by rememberSaveable { mutableStateOf(key.isBoundTo(model = model)) }
    var displayWarningDialog by rememberSaveable { mutableStateOf(false) }
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.VpnKey,
        title = key.name,
        titleAction = {
            Checkbox(
                checked = isBound,
                onCheckedChange = {
                    if (!isBound) {
                        send(ConfigModelAppBind(model = model, applicationKey = key))
                    } else {
                        // Check if the key is in use before unbinding.
                        if (model.publish?.index == key.index) {
                            displayWarningDialog = true
                        } else {
                            send(ConfigModelAppUnbind(model = model, applicationKey = key))
                        }
                    }
                    isBound = it
                }
            )
        }
    )

    if (displayWarningDialog) {
        MeshAlertDialog(
            onDismissRequest = { displayWarningDialog = !displayWarningDialog },
            icon = Icons.Outlined.Warning,
            iconColor = MaterialTheme.colorScheme.error,
            title = stringResource(R.string.warning),
            text = stringResource(R.string.warning_unbind_rationale),
            dismissButtonText = stringResource(R.string.label_cancel),
            onDismissClick = { displayWarningDialog = !displayWarningDialog },
            confirmButtonText = stringResource(R.string.label_ok),
            onConfirmClick = {
                displayWarningDialog = !displayWarningDialog
                send(ConfigModelAppUnbind(model = model, applicationKey = key))
            }
        )
    }
}