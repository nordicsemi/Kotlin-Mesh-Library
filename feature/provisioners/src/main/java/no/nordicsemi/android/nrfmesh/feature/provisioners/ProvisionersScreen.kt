@file:OptIn(ExperimentalMaterial3Api::class)

package no.nordicsemi.android.nrfmesh.feature.provisioners

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PersonOutline
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
import no.nordicsemi.android.nrfmesh.core.data.ui.ProvisionerData
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshNoItemsAvailable
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.kotlin.mesh.core.model.Provisioner
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
@Composable
internal fun ProvisionersScreen(
    snackbarHostState: SnackbarHostState,
    highlightSelectedItem: Boolean,
    selectedProvisionerUuid: Uuid?,
    provisioners: List<ProvisionerData>,
    onAddProvisionerClicked: () -> Provisioner,
    onProvisionerClicked: (Uuid) -> Unit,
    onSwiped: (ProvisionerData) -> Unit,
    onUndoClicked: (ProvisionerData) -> Unit,
    remove: (ProvisionerData) -> Unit,
    navigateToProvisioner: (Uuid) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Box(modifier = Modifier.fillMaxSize()) {
        when (provisioners.isEmpty()) {
            true -> MeshNoItemsAvailable(
                modifier = Modifier.fillMaxSize(),
                imageVector = Icons.Outlined.PersonOutline,
                title = stringResource(id = R.string.label_no_provisioners_available)
            )

            false -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                // Removed in favor of padding in SwipeToDismissProvisioner so that hiding an item will not leave any gaps
                //verticalArrangement = Arrangement.spacedBy(space = 8.dp)
            ) {
                itemsIndexed(
                    items = provisioners,
                    key = { _, item -> item.id }
                ) { index, item ->
                    var visibility by remember { mutableStateOf(true) }
                    if (index == 0) {
                        SectionTitle(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 16.dp),
                            title = stringResource(id = R.string.label_this_provisioner)
                        )
                    }
                    if (index == 1) {
                        SectionTitle(
                            modifier = Modifier
                                .padding(bottom = 8.dp)
                                .padding(horizontal = 16.dp),
                            title = stringResource(id = R.string.label_other_provisioner)
                        )
                    }
                    AnimatedVisibility(visibility) {
                        SwipeToDismissProvisioner(
                            index = index,
                            provisioner = item,
                            scope = scope,
                            context = context,
                            snackbarHostState = snackbarHostState,
                            isSelected = selectedProvisionerUuid == item.uuid && highlightSelectedItem,
                            onProvisionerClicked = onProvisionerClicked,
                            onSwiped = {
                                visibility = false
                                onSwiped(it)
                            },
                            onUndoClicked = {
                                visibility = true
                                onUndoClicked(it)
                            },
                            remove = remove,
                            isOnlyProvisioner = provisioners.size == 1,
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
            text = { Text(text = stringResource(R.string.label_add_provisioner)) },
            icon = { Icon(imageVector = Icons.Outlined.Add, contentDescription = null) },
            onClick = {
                runCatching {
                    onAddProvisionerClicked()
                }.onSuccess {
                    navigateToProvisioner(it.uuid)
                }
            },
            expanded = true
        )
    }
}

@OptIn(ExperimentalUuidApi::class)
@Composable
private fun SwipeToDismissProvisioner(
    scope: CoroutineScope,
    context: Context,
    snackbarHostState: SnackbarHostState,
    provisioner: ProvisionerData,
    onProvisionerClicked: (Uuid) -> Unit,
    onSwiped: (ProvisionerData) -> Unit,
    onUndoClicked: (ProvisionerData) -> Unit,
    remove: (ProvisionerData) -> Unit,
    isSelected: Boolean = false,
    isOnlyProvisioner: Boolean,
    index: Int,
) {
    val dismissState = rememberSwipeToDismissBoxState()
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
                        -> if (isOnlyProvisioner) Color.Gray else Color.Red
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
            if (isOnlyProvisioner) {
                // The following functions are invoked in their own coroutine to ensure
                // that they are executed sequentially
                scope.launch {
                    dismissState.reset()
                }
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = context.getString(
                            R.string.error_cannot_delete_last_provisioner,
                            provisioner.name
                        )
                    )
                }
            } else {
                scope.launch {
                    onSwiped(provisioner)
                }
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(
                            R.string.label_provisioner_deleted,
                            provisioner.name
                        ),
                        actionLabel = context.getString(R.string.action_undo),
                        withDismissAction = true,
                        duration = SnackbarDuration.Short
                    )

                    when (result) {
                        SnackbarResult.ActionPerformed -> {
                            dismissState.reset()
                            onUndoClicked(provisioner)
                        }

                        SnackbarResult.Dismissed -> remove(provisioner)
                    }
                }
            }
        },
        content = {
            ElevatedCardItem(
                colors = isSelected.selectedColor(),
                onClick = { onProvisionerClicked(provisioner.uuid) },
                imageVector = index.toImageVector(),
                title = provisioner.name,
                subtitle = provisioner.address?.let {
                    it.address.toHexString(
                        format = HexFormat {
                            number.prefix = "Address: 0x"
                            upperCase = true
                        }
                    )
                } ?: context.getString(R.string.label_unassigned),
            )
        }
    )
}

@Composable
private fun Boolean.selectedColor() = when (this) {
    true -> CardDefaults.outlinedCardColors(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    )

    else -> CardDefaults.outlinedCardColors()
}

@Composable
private fun Int.toImageVector() = when (this == 0) {
    true -> Icons.Filled.PersonPin
    false -> Icons.Outlined.PersonOutline
}
