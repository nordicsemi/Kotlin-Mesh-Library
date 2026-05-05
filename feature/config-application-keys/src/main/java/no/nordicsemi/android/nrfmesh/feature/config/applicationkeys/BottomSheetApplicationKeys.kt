package no.nordicsemi.android.nrfmesh.feature.config.applicationkeys

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.ui.MeshNoItemsAvailable
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedButton
import no.nordicsemi.android.nrfmesh.core.ui.Row
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyAdd
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetApplicationKeys(
    bottomSheetState: SheetState,
    messageState: MessageState,
    keys: List<ApplicationKey>,
    onAddApplicationKeyClicked: () -> Unit,
    onAppKeyClicked: (ApplicationKey) -> Unit,
    onDismissClick: () -> Unit,
) {
    ModalBottomSheet(
        sheetState = bottomSheetState,
        onDismissRequest = onDismissClick
    ) {
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
                title = stringResource(R.string.label_application_keys)
            )
            MeshOutlinedButton(
                enabled = !messageState.isInProgress(),
                onClick = onAddApplicationKeyClicked,
                buttonIcon = Icons.Outlined.AutoFixHigh,
                text = stringResource(R.string.label_generate)
            )
        }
        when (keys.isEmpty()) {
            true -> MeshNoItemsAvailable(
                modifier = Modifier.fillMaxSize(),
                imageVector = Icons.Outlined.VpnKey,
                title = stringResource(R.string.label_no_app_keys_available),
                rationale = stringResource(R.string.label_no_app_keys_to_add_rationale)
            )

            false -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                items(items = keys, key = { it.index.toInt() + 1 }) { key ->
                    val showProgress =
                        (messageState.message as? ConfigAppKeyAdd)?.applicationKeyIndex == key.index &&
                                messageState.isInProgress()
                    key.Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        enabled = !messageState.isInProgress(),
                        onClick = { onAppKeyClicked(key) },
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
}
