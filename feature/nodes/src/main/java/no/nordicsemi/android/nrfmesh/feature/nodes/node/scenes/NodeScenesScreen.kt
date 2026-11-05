package no.nordicsemi.android.nrfmesh.feature.nodes.node.scenes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.Failed
import no.nordicsemi.android.nrfmesh.core.common.Utils.describe
import no.nordicsemi.android.nrfmesh.core.ui.MeshMessageStatusDialog
import no.nordicsemi.android.nrfmesh.core.ui.MeshNoItemsAvailable
import no.nordicsemi.android.nrfmesh.core.ui.NoScenesAvailable
import no.nordicsemi.android.nrfmesh.core.ui.SceneItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.core.ui.SwipeToDeleteBackground
import no.nordicsemi.android.nrfmesh.feature.nodes.R
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber

/**
 * Displays the scenes stored on a node.
 *
 * @param uiState           State of the screen.
 * @param onRefresh         Invoked when the user pulls down to read the scenes from the node.
 * @param onSceneClicked    Invoked with the number of the scene to be recalled.
 * @param onSceneDeleted    Invoked with the number of the scene to be deleted from the node.
 * @param onFixClicked      Invoked when the user wants to bind the keys to the Scene Client.
 * @param onMessageHandled  Invoked when the error dialog is dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NodeScenesScreen(
    uiState: NodeScenesUiState,
    onRefresh: () -> Unit,
    onSceneClicked: (SceneNumber) -> Unit,
    onSceneDeleted: (SceneNumber) -> Unit,
    onFixClicked: () -> Unit,
    onMessageHandled: () -> Unit,
) {
    // Errors are shown in a dialog, like on the other screens.
    (uiState.messageState as? Failed)?.let {
        MeshMessageStatusDialog(
            // The status of a failed scene operation is described by the message of the error.
            text = (it.error as? IllegalStateException)?.message ?: it.error.describe(),
            showDismissButton = false,
            onDismissRequest = onMessageHandled
        )
    }

    if (!uiState.isSceneServerReady || !uiState.isSceneClientReady) {
        IncompleteSetup(uiState = uiState, onFixClicked = onFixClicked)
    } else {
        val canSetup = uiState.isSceneSetupServerReady && uiState.isSceneClientReadyToSetup
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                if (uiState.scenes.isEmpty()) {
                    item {
                        when (canSetup) {
                            true -> NoScenesAvailable(
                                modifier = Modifier.fillParentMaxSize(),
                                title = stringResource(R.string.label_no_scenes_on_node)
                            )

                            // Explains what is needed to store and delete scenes.
                            false -> MeshNoItemsAvailable(
                                modifier = Modifier.fillParentMaxSize(),
                                imageVector = Icons.Outlined.AutoAwesome,
                                title = stringResource(R.string.label_no_scenes_on_node),
                                rationale = stringResource(R.string.label_scene_setup_rationale),
                                onClickText = stringResource(R.string.action_fix).takeIf { uiState.canFix },
                                onClickIcon = Icons.Outlined.AutoFixHigh,
                                onClick = onFixClicked.takeIf { uiState.canFix }
                            )
                        }
                    }
                } else {
                    item {
                        SectionTitle(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            title = stringResource(R.string.label_scenes)
                        )
                    }
                }
                items(items = uiState.scenes, key = { it.number.toInt() }) { scene ->
                    val item = @Composable {
                        SceneItem(
                            name = scene.name,
                            sceneNumber = scene.number,
                            // The current scene is highlighted.
                            isSelected = scene.number == uiState.currentScene,
                            onClick = { onSceneClicked(scene.number) }
                        )
                    }
                    when (canSetup) {
                        true -> SwipeToDeleteScene(
                            onDeleted = { onSceneDeleted(scene.number) },
                            content = item
                        )

                        false -> Box(modifier = Modifier.sceneItemPadding()) { item() }
                    }
                }
                if (uiState.scenes.isNotEmpty()) {
                    item {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            text = stringResource(R.string.label_scenes_hint),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                if (!canSetup && uiState.scenes.isNotEmpty()) {
                    item {
                        SetupRationale(uiState = uiState, onFixClicked = onFixClicked)
                    }
                }
            }
        }
    }
}

// The same spacing is used for the scenes in the scenes settings. It is added to the item instead
// of using Arrangement.spacedBy to avoid leaving gaps when an item is swiped away.
private fun Modifier.sceneItemPadding() = padding(horizontal = 16.dp).padding(bottom = 8.dp)

@Composable
private fun SwipeToDeleteScene(onDeleted: () -> Unit, content: @Composable () -> Unit) {
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        modifier = Modifier.sceneItemPadding(),
        state = dismissState,
        backgroundContent = { SwipeToDeleteBackground(dismissState = dismissState) },
        onDismiss = {
            onDeleted()
            // The scene is removed from the list when the node confirms the deletion.
            scope.launch { dismissState.reset() }
        },
        content = { content() }
    )
}

@Composable
private fun SetupRationale(uiState: NodeScenesUiState, onFixClicked: () -> Unit) {
    MeshNoItemsAvailable(
        modifier = Modifier.fillMaxWidth(),
        imageVector = Icons.Outlined.Info,
        title = stringResource(R.string.label_scene_setup_incomplete),
        rationale = stringResource(R.string.label_scene_setup_rationale),
        onClickText = stringResource(R.string.action_fix).takeIf { uiState.canFix },
        onClickIcon = Icons.Outlined.AutoFixHigh,
        onClick = onFixClicked.takeIf { uiState.canFix }
    )
}

@Composable
private fun IncompleteSetup(uiState: NodeScenesUiState, onFixClicked: () -> Unit) {
    MeshNoItemsAvailable(
        modifier = Modifier.fillMaxSize(),
        imageVector = Icons.Outlined.Info,
        title = stringResource(R.string.label_scene_setup_incomplete),
        rationale = stringResource(R.string.label_scene_setup_rationale),
        onClickText = stringResource(R.string.action_fix).takeIf { uiState.canFix },
        onClickIcon = Icons.Outlined.AutoFixHigh,
        onClick = onFixClicked.takeIf { uiState.canFix }
    )
}
