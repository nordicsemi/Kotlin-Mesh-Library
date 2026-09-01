package no.nordicsemi.android.nrfmesh.feature.model.configurationserver

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Start
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.ui.MeshSingleLineListItem
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatSubscriptionSource
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExposedDropdownMenuBoxScope.HeartbeatSubscriptionSourcesDropdownMenu(
    network: MeshNetwork?,
    model: Model,
    expanded: Boolean,
    onDismissed: () -> Unit,
    onSourceSelected: (HeartbeatSubscriptionSource) -> Unit,
) {
    val node = model.parentElement?.parentNode ?: return
    val otherNodes = network?.nodes?.filter { it != node }.orEmpty()
    ExposedDropdownMenu(
        modifier = Modifier
            .exposedDropdownSize()
            .wrapContentHeight(),
        expanded = expanded,
        onDismissRequest = onDismissed,
        shape = RoundedCornerShape(size = 16.dp),
        content = {
            Text(
                modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                text = stringResource(R.string.label_unicast_sources)
            )
            otherNodes.forEach { otherNode ->
                DropdownMenuItem(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    text = {
                        MeshSingleLineListItem(
                            imageVector = Icons.Outlined.Start,
                            title = otherNode.name
                        )
                    },
                    onClick = { onSourceSelected(otherNode.primaryUnicastAddress) }
                )
            }
        }
    )
}