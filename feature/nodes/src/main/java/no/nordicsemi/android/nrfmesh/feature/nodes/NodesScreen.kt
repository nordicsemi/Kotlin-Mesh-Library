@file:Suppress("UNUSED_PARAMETER")

package no.nordicsemi.android.nrfmesh.feature.nodes

import no.nordicsemi.android.nrfmesh.core.ui.BottomInsetSpacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.animation.core.animate
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import no.nordicsemi.android.common.theme.nordicSun
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
import kotlin.uuid.Uuid

@Composable
internal fun NodesScreen(
    uiState: NodesScreenUiState,
    navigateToNode: (Uuid) -> Unit,
    identify: (Node) -> Unit,
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
                navigateToNode = navigateToNode,
                identify = identify
            )
        }
        ExtendedFloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
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

@Composable
private fun NodesList(
    nodes: List<Node>,
    navigateToNode: (Uuid) -> Unit,
    identify: (Node) -> Unit,
) {
    if (isCompactWidth()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(all = 16.dp),
            content = {
                items(items = nodes, key = { it.uuid }) { node ->
                    SwipeToIdentifyNode(
                        modifier = Modifier.fillMaxWidth(),
                        itemModifier = Modifier.fillMaxWidth(),
                        node = node,
                        navigateToNode = navigateToNode,
                        identify = identify
                    )
                }
                item { BottomInsetSpacer(spacing = 8.dp) }
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
                SwipeToIdentifyNode(
                    node = node,
                    navigateToNode = navigateToNode,
                    identify = identify
                )
            }
            Spacer(
                modifier = Modifier
                    .height(height = 8.dp)
                    .fillMaxWidth()
            )
            BottomInsetSpacer(modifier = Modifier.fillMaxWidth(), spacing = 16.dp)
        }
    }
}


/**
 * A node item that starts identifying the node when swiped to the left. The item can only be
 * swiped far enough to reveal the identify icon and snaps back once released.
 */
@Composable
private fun SwipeToIdentifyNode(
    modifier: Modifier = Modifier,
    itemModifier: Modifier = Modifier,
    node: Node,
    navigateToNode: (Uuid) -> Unit,
    identify: (Node) -> Unit,
) {
    val currentIdentify by rememberUpdatedState(identify)
    // Maximum distance the item can be swiped, just enough to reveal the icon.
    val maxOffset = with(LocalDensity.current) { RevealWidth.toPx() }
    var offset by remember { mutableFloatStateOf(0f) }
    Box(modifier = modifier.clip(shape = CardDefaults.elevatedShape)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(color = nordicSun, shape = CardDefaults.elevatedShape)
                .padding(end = 16.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsActive,
                contentDescription = stringResource(R.string.label_identify),
                tint = Color.White
            )
        }
        MeshItem(
            modifier = itemModifier
                .offset { IntOffset(x = offset.roundToInt(), y = 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        offset = (offset + delta).coerceIn(-maxOffset, 0f)
                    },
                    onDragStopped = {
                        // Identify once the icon has been (almost) fully revealed.
                        if (offset <= -maxOffset * 0.8f) currentIdentify(node)
                        // Snap the item back to its original position.
                        animate(initialValue = offset, targetValue = 0f) { value, _ ->
                            offset = value
                        }
                    }
                ),
            icon = { CircularIcon(painter = painterResource(R.drawable.ic_mesh)) },
            title = node.name,
            subtitle = node.primaryUnicastAddress.address.toHexString(
                format = HexFormat {
                    number.prefix = "Address: 0x"
                    upperCase = true
                }
            ),
            onClick = { navigateToNode(node.uuid) },
        )
    }
}

/** Width revealed behind a node item when it is swiped, enough to show the identify icon. */
private val RevealWidth = 58.dp
