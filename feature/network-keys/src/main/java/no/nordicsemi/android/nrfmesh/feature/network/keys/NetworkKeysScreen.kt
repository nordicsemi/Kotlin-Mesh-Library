package no.nordicsemi.android.nrfmesh.feature.network.keys

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.data.ui.NetworkKeyData
import no.nordicsemi.android.nrfmesh.core.ui.MeshNoItemsAvailable
import no.nordicsemi.android.nrfmesh.core.ui.NetworkKeyRow
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey

@Composable
internal fun NetworkKeysScreen(
    snackbarHostState: SnackbarHostState,
    highlightSelectedItem: Boolean,
    selectedKeyIndex: KeyIndex?,
    onAddKeyClicked: () -> NetworkKey,
    keys: List<NetworkKeyData>,
    onNetworkKeyClicked: (KeyIndex) -> Unit,
    navigateToKey: (KeyIndex) -> Unit,
    onSwiped: (NetworkKeyData) -> Unit,
    onUndoClicked: (NetworkKeyData) -> Unit,
    remove: (NetworkKeyData) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            when (keys.isEmpty()) {
                true -> MeshNoItemsAvailable(
                    modifier = Modifier.fillMaxSize(),
                    imageVector = Icons.Outlined.VpnKey,
                    title = stringResource(R.string.no_network_keys_available),
                )

                false ->
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        // Removed in favor of padding in SwipeToDismissKey so that hiding an item will not leave any gaps
                        //verticalArrangement = Arrangement.spacedBy(space = 8.dp)
                    ) {
                        item {
                            SectionTitle(
                                modifier = Modifier.padding(vertical = 8.dp),
                                title = stringResource(R.string.label_network_keys)
                            )
                        }
                        items(items = keys, key = { it.id }) { key ->
                            val isSelected = highlightSelectedItem && key.index == selectedKeyIndex
                            var visibility by remember { mutableStateOf(true) }
                            AnimatedVisibility(visibility) {
                                SwipeToDismissKey(
                                    scope = scope,
                                    context = context,
                                    snackbarHostState = snackbarHostState,
                                    key = key,
                                    isSelected = isSelected,
                                    onNetworkKeyClicked = onNetworkKeyClicked,
                                    onSwiped = {
                                        visibility = false
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
        }
        ExtendedFloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .defaultMinSize(minWidth = 150.dp),
            text = { Text(text = stringResource(R.string.label_add_key)) },
            icon = { Icon(imageVector = Icons.Outlined.Add, contentDescription = null) },
            onClick = {
                runCatching {
                    onAddKeyClicked()
                }.onSuccess {
                    navigateToKey(it.index)
                }
            },
            expanded = true
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissKey(
    scope: CoroutineScope,
    context: Context,
    snackbarHostState: SnackbarHostState,
    key: NetworkKeyData,
    isSelected: Boolean,
    onNetworkKeyClicked: (KeyIndex) -> Unit,
    onSwiped: (NetworkKeyData) -> Unit,
    onUndoClicked: (NetworkKeyData) -> Unit,
    remove: (NetworkKeyData) -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        // Added instead of using Arrangement.spacedBy to avoid leaving gaps when an item is swiped away.
        modifier = Modifier.padding(bottom = 8.dp),
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.Settled,
                    SwipeToDismissBoxValue.StartToEnd,
                    SwipeToDismissBoxValue.EndToStart,
                    -> if (key.isInUse) Color.Gray else Color.Red
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
            snackbarHostState.currentSnackbarData?.dismiss()
            if (key.isPrimary) {
                scope.launch {
                    dismissState.reset()
                    snackbarHostState.showSnackbar(
                        message = context.getString(R.string.label_cannot_delete_the_primary_network_key)
                    )
                }
            } else if (key.isInUse) {
                scope.launch {
                    dismissState.reset()
                    snackbarHostState.showSnackbar(
                        message = context.getString(
                            R.string.label_cannot_delete_is_in_use,
                            key.name
                        )
                    )
                }
            } else {
                scope.launch { onSwiped(key) }
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.label_key_deleted, key.name),
                        actionLabel = context.getString(R.string.action_undo),
                        withDismissAction = true,
                        duration = SnackbarDuration.Short
                    )

                    when (result) {
                        SnackbarResult.ActionPerformed -> {
                            dismissState.reset()
                            onUndoClicked(key)
                        }

                        SnackbarResult.Dismissed -> remove(key)
                    }
                }
            }
        },
        content = {
            NetworkKeyRow(
                onClick = { onNetworkKeyClicked(key.index) },
                colors = when (isSelected) {
                    true -> CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    else -> CardDefaults.outlinedCardColors()
                },
                title = key.name,
            )
        }
    )
}