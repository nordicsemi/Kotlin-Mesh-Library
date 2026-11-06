package no.nordicsemi.android.nrfmesh.feature.nodes.node.scenes

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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.Failed
import no.nordicsemi.android.nrfmesh.core.common.Utils.describe
import no.nordicsemi.android.nrfmesh.core.ui.MeshMessageStatusDialog
import no.nordicsemi.android.nrfmesh.core.ui.MeshNoItemsAvailable
import no.nordicsemi.android.nrfmesh.core.ui.NoScenesAvailable
import no.nordicsemi.android.nrfmesh.core.ui.SceneItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.core.ui.SwipeToDeleteBackground
import no.nordicsemi.android.nrfmesh.feature.nodes.R
import no.nordicsemi.android.nrfmesh.feature.nodes.node.ConfigurationTasksBottomSheet
import no.nordicsemi.kotlin.mesh.core.messages.StatusMessage
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber

/**
 * Displays the scenes stored on a node.
 *
 * @param uiState           State of the screen.
 * @param onRefresh         Invoked when the user pulls down to read the scenes from the node.
 * @param onSceneClicked    Invoked with the number of the scene to be recalled.
 * @param onSceneDeleted    Invoked with the number of the scene to be deleted from the node.
 * @param onFixClicked      Invoked when the user wants to set up the scene models.
 * @param onRetryPressed    Invoked when the user wants to retry the tasks of the setup.
 * @param onTasksDismissed  Invoked when the progress of the setup is closed.
 * @param onMessageHandled  Invoked when the error dialog is dismissed.
 */
@Composable
internal fun NodeScenesScreen(
    uiState: NodeScenesUiState,
    onRefresh: () -> Unit,
    onSceneClicked: (SceneNumber) -> Unit,
    onSceneDeleted: (SceneNumber) -> Unit,
    onFixClicked: () -> Unit,
    onRetryPressed: () -> Unit,
    onTasksDismissed: () -> Unit,
    onMessageHandled: () -> Unit,
) {
    // Errors and statuses reporting an error are shown in a dialog, like on the model screen.
    when (val messageState = uiState.messageState) {
        is Failed -> MeshMessageStatusDialog(
            text = messageState.error.describe(),
            showDismissButton = false,
            onDismissRequest = onMessageHandled
        )

        is Completed -> (messageState.response as? StatusMessage)
            ?.takeIf { !it.isSuccess }
            ?.let {
                MeshMessageStatusDialog(
                    text = it.message,
                    showDismissButton = false,
                    onDismissRequest = onMessageHandled
                )
            }

        else -> Unit
    }

    // Shows the progress of the setup of the scene models.
    if (uiState.tasks.isNotEmpty()) {
        ConfigurationTasksBottomSheet(
            tasks = uiState.tasks,
            onRetryPressed = onRetryPressed,
            onCompletePressed = onTasksDismissed,
            onCancelPressed = onTasksDismissed
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
                                onClickText = stringResource(R.string.action_set_up).takeIf { uiState.canFix },
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
                            // The current scene is marked with a check.
                            isCurrent = scene.number == uiState.currentScene,
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
        onClickText = stringResource(R.string.action_set_up).takeIf { uiState.canFix },
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
        onClickText = stringResource(R.string.action_set_up).takeIf { uiState.canFix },
        onClickIcon = Icons.Outlined.AutoFixHigh,
        onClick = onFixClicked.takeIf { uiState.canFix }
    )
}
