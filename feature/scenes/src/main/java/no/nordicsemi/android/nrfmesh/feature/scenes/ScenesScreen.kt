@file:OptIn(ExperimentalMaterial3Api::class)

package no.nordicsemi.android.nrfmesh.feature.scenes

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.data.ui.SceneData
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshNoItemsAvailable
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.kotlin.mesh.core.exception.NoSceneRangeAllocated
import no.nordicsemi.kotlin.mesh.core.model.Scene
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber

@Composable
internal fun ScenesScreen(
    snackbarHostState: SnackbarHostState,
    highlightSelectedItem: Boolean,
    selectedSceneNumber: SceneNumber?,
    scenes: List<SceneData>,
    onAddSceneClicked: () -> Scene,
    onSceneClicked: (SceneNumber) -> Unit,
    onSwiped: (SceneData) -> Unit,
    onUndoClicked: (SceneData) -> Unit,
    remove: (SceneData) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxSize()) {
        when (scenes.isEmpty()) {
            true -> MeshNoItemsAvailable(
                modifier = Modifier.fillMaxSize(),
                imageVector = Icons.Outlined.AutoAwesome,
                title = stringResource(R.string.no_scenes_currently_added)
            )

            false -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp)
                // Removed in favor of padding in SwipeToDismissKey so that hiding an item will not leave any gaps
                // verticalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                item {
                    SectionTitle(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        title = stringResource(id = R.string.label_scenes)
                    )
                }
                items(items = scenes, key = { it.number.toInt() }) { scene ->
                    val isSelected = highlightSelectedItem && scene.number == selectedSceneNumber
                    val dismissState = rememberSwipeToDismissBoxState()
                    var visibility by rememberSaveable(scene.number.toInt()) {
                        mutableStateOf(true)
                    }
                    AnimatedVisibility(visible = visibility) {
                        SwipeToDismissScene(
                            scope = scope,
                            context = context,
                            snackbarHostState = snackbarHostState,
                            dismissState = dismissState,
                            scene = scene,
                            isSelected = isSelected,
                            onSceneClicked = onSceneClicked,
                            onSwiped = {
                                // Force a reset of the internal state before showing
                                visibility = false
                                scope.launch { dismissState.snapTo(SwipeToDismissBoxValue.Settled) }
                                onSwiped(it)
                            },
                            onUndoClicked = {
                                visibility = true
                                onUndoClicked(it)
                            },
                            remove = remove
                        )
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .defaultMinSize(minWidth = 150.dp),
            text = { Text(text = stringResource(R.string.label_add_scene)) },
            icon = { Icon(imageVector = Icons.Outlined.Add, contentDescription = null) },
            onClick = dropUnlessResumed {
                runCatching {
                    onAddSceneClicked()
                }.onSuccess { scene ->
                    onSceneClicked(scene.number)
                }.onFailure { t ->
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = when (t) {
                                is NoSceneRangeAllocated -> t.message ?: context.getString(
                                    R.string.error_allocate_scene_range_to_provisioner
                                )

                                else -> t.message ?: context.getString(R.string.unknown_error)
                            }
                        )
                    }
                }
            },
            expanded = true
        )
    }
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
private fun SwipeToDismissScene(
    scope: CoroutineScope,
    context: Context,
    snackbarHostState: SnackbarHostState,
    dismissState: SwipeToDismissBoxState,
    scene: SceneData,
    isSelected: Boolean,
    onSceneClicked: (SceneNumber) -> Unit,
    onSwiped: (SceneData) -> Unit,
    onUndoClicked: (SceneData) -> Unit,
    remove: (SceneData) -> Unit,
) {
    SwipeToDismissBox(
        // Added instead of using Arrangement.spacedBy to avoid leaving gaps when an item is swiped away.
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 8.dp),
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.Settled,
                    SwipeToDismissBoxValue.StartToEnd,
                    SwipeToDismissBoxValue.EndToStart,
                        -> if (scene.isInUse) Color.Gray else Color.Red
                }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color = color, shape = CardDefaults.elevatedShape)
                    .padding(horizontal = 16.dp),
                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                    Alignment.CenterStart
                else Alignment.CenterEnd
            ) {
                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "null")
            }
        },
        onDismiss = {
            // Check if the dismissal was caused by a swipe (target not Settled)
            if (dismissState.targetValue != SwipeToDismissBoxValue.Settled) {
                snackbarHostState.currentSnackbarData?.dismiss()
                if (scene.isInUse) {
                    // The following functions are invoked in their own coroutine to ensure
                    // that they are executed sequentially
                    scope.launch {
                        dismissState.reset()
                        snackbarHostState.showSnackbar(
                            message = context.getString(
                                R.string.label_cannot_delete_scene_in_use,
                                scene.name
                            )
                        )
                    }
                } else {
                    onSwiped(scene)
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = context.getString(
                                R.string.label_scene_deleted,
                                scene.name
                            ),
                            actionLabel = context.getString(R.string.action_undo),
                            withDismissAction = true,
                            duration = SnackbarDuration.Short
                        )

                        when (result) {
                            SnackbarResult.ActionPerformed -> {
                                // IMPORTANT: Reset the offset before updating the visibility
                                dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                                onUndoClicked(scene)
                            }

                            SnackbarResult.Dismissed -> remove(scene)
                        }
                    }
                }
            }
        },
        content = {
            ElevatedCardItem(
                onClick = { onSceneClicked(scene.number) },
                colors = when (isSelected) {
                    true -> CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    else -> CardDefaults.outlinedCardColors()
                },
                imageVector = Icons.Outlined.AutoAwesome,
                title = scene.name,
                subtitle = "Scene number: ${
                    scene.number.toHexString(
                        format = HexFormat {
                            number.prefix = "0x"
                            upperCase = true
                        }
                    )
                }"
            )
        }
    )
}
