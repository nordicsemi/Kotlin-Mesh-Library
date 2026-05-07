@file:Suppress("UNUSED_PARAMETER")

package no.nordicsemi.android.nrfmesh.feature.nodes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import no.nordicsemi.android.common.ui.view.CircularIcon
import no.nordicsemi.android.nrfmesh.core.ui.MeshItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshNoItemsAvailable
import no.nordicsemi.android.nrfmesh.core.ui.isCompactWidth
import no.nordicsemi.kotlin.mesh.core.model.Node
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
internal fun NodesScreen(
    uiState: NodesScreenUiState,
    navigateToNode: (Uuid) -> Unit,
    addNode: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (uiState.nodes.isEmpty()) {
            true -> MeshNoItemsAvailable(
                modifier = Modifier.fillMaxSize(),
                imageVector = Icons.Outlined.AutoAwesome,
                title = stringResource(R.string.no_nodes_currently_added)
            )

            false -> NodesList(
                nodes = uiState.nodes,
                navigateToNode = navigateToNode
            )
        }
        ExtendedFloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .defaultMinSize(minWidth = 150.dp),
            text = { Text(text = stringResource(R.string.label_add_node)) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null
                )
            },
            onClick = dropUnlessResumed { addNode() },
            expanded = true
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalStdlibApi::class, ExperimentalUuidApi::class)
@Composable
private fun NodesList(
    nodes: List<Node>,
    navigateToNode: (Uuid) -> Unit,
) {
    if (isCompactWidth()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(all = 16.dp),
            content = {
                items(items = nodes, key = { it.uuid }) { node ->
                    MeshItem(
                        modifier = Modifier.fillMaxWidth(),
                        icon = {
                            CircularIcon(painter = painterResource(R.drawable.ic_mesh))
                        },
                        title = node.name,
                        subtitle = node.primaryUnicastAddress.address
                            .toHexString(
                                format = HexFormat {
                                    number.prefix = "Address: 0x"
                                    upperCase = true
                                }
                            ),
                        onClick = { navigateToNode(node.uuid) },
                    )
                }
            }
        )
    } else {
        FlowRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(state = rememberScrollState()),
            maxItemsInEachRow = 5,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(
                modifier = Modifier
                    .height(height = 8.dp)
                    .fillMaxWidth()
            )
            nodes.forEach { node ->
                MeshItem(
                    icon = {
                        CircularIcon(painter = painterResource(R.drawable.ic_mesh))
                    },
                    title = node.name,
                    subtitle = node.primaryUnicastAddress.address
                        .toHexString(
                            format = HexFormat {
                                number.prefix = "Address: 0x"
                                upperCase = true
                            }
                        ),
                    onClick = { navigateToNode(node.uuid) },
                )
            }
            Spacer(
                modifier = Modifier
                    .height(height = 8.dp)
                    .fillMaxWidth()
            )
        }
    }
}
