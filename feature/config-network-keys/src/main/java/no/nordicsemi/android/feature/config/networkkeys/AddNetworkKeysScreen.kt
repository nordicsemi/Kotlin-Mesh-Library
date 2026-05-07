package no.nordicsemi.android.feature.config.networkkeys

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.Failed
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.Utils.describe
import no.nordicsemi.android.nrfmesh.core.ui.MeshMessageStatusDialog
import no.nordicsemi.android.nrfmesh.core.ui.MeshNoItemsAvailable
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedButton
import no.nordicsemi.android.nrfmesh.core.ui.Row
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.config.networkkeys.R
import no.nordicsemi.kotlin.mesh.core.messages.ConfigStatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetKeyAdd
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddNetworkKeysScreen(
    messageState: MessageState,
    keys: List<NetworkKey>,
    onAddNetworkKeyClicked: () -> Unit,
    onNetworkKeyClicked: (NetworkKey) -> Unit,
    resetMessageState: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
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
                title = stringResource(R.string.label_available_network_keys)
            )
            MeshOutlinedButton(
                enabled = !messageState.isInProgress(),
                onClick = onAddNetworkKeyClicked,
                buttonIcon = Icons.Outlined.AutoFixHigh,
                text = stringResource(R.string.label_generate)
            )
        }
        when (keys.isEmpty()) {
            true -> MeshNoItemsAvailable(
                modifier = Modifier.fillMaxSize(),
                imageVector = Icons.Outlined.VpnKey,
                title = stringResource(R.string.label_no_keys_available),
                rationale = stringResource(R.string.label_no_keys_available_rationale),
                onClickText = stringResource(R.string.label_settings),
            )

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                items(items = keys) { key ->
                    val showProgress =
                        (messageState.message as? ConfigNetKeyAdd)?.networkKeyIndex == key.index &&
                                messageState.isInProgress()
                    key.Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        enabled = !messageState.isInProgress(),
                        onClick = { onNetworkKeyClicked(key) },
                        titleAction = {
                            if (showProgress) {
                                CircularProgressIndicator(modifier = Modifier.size(size = 24.dp))
                            }
                        }
                    )
                }
            }
        }
    }

    when (messageState) {
        is Failed -> MeshMessageStatusDialog(
            text = messageState.error.describe(),
            showDismissButton = !messageState.didFail(),
            onDismissRequest = resetMessageState,
        )

        is Completed -> messageState.response?.takeIf {
            it is ConfigStatusMessage
        }?.let {
            when (!(it as ConfigStatusMessage).isSuccess) {
                true -> MeshMessageStatusDialog(
                    text = it.message,
                    showDismissButton = true,
                    onDismissRequest = resetMessageState,
                )

                false -> {

                }
            }
        }

        else -> {}
    }
}
