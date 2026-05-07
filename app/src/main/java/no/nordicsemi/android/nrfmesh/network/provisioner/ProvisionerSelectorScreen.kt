package no.nordicsemi.android.nrfmesh.network.provisioner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.R
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.kotlin.mesh.core.model.Provisioner
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
@Composable
internal fun ProvisionerSelectorScreen(
    provisioners: List<Provisioner>,
    onProvisionerSelected: (provisioner: Provisioner) -> Unit,
) {
    SectionTitle(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(vertical = 8.dp),
        title = stringResource(R.string.label_select_provisioner_rationale),
        style = MaterialTheme.typography.titleMedium
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(items = provisioners, key = { it.uuid.toString() }) {
            ElevatedCardItem(
                imageVector = Icons.Filled.PersonPin, title = it.name, onClick = {
                    onProvisionerSelected(it)
                    // scope.launch { exportSheetState.hide() }.invokeOnCompletion {
                    //     if (!exportSheetState.isVisible) {
                    //         // showExportBottomSheet = false
                    //     }
                    // }
                }
            )
        }
    }
}