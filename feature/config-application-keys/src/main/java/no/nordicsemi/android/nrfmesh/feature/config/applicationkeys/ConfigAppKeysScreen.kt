package no.nordicsemi.android.nrfmesh.feature.config.applicationkeys

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.Failed
import no.nordicsemi.android.nrfmesh.core.common.KeyIdGenerator
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.Utils.describe
import no.nordicsemi.android.nrfmesh.core.ui.MeshAlertDialog
import no.nordicsemi.android.nrfmesh.core.ui.MeshMessageStatusDialog
import no.nordicsemi.android.nrfmesh.core.ui.MeshNoItemsAvailable
import no.nordicsemi.android.nrfmesh.core.ui.Row
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigStatusMessage
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyDelete
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyGet
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ConfigAppKeysScreen(
    snackbarHostState: SnackbarHostState,
    isLocalProvisionerNode: Boolean,
    addedApplicationKeys: List<ApplicationKey>,
    messageState: MessageState,
    availableApplicationKeys: List<ApplicationKey>,
    onAddAppKeyClicked: () -> Unit,
    readApplicationKeys: () -> Unit,
    isKeyInUse: (ApplicationKey) -> Boolean,
    send: (AcknowledgedConfigMessage) -> Unit,
    resetMessageState: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val bottomSheetState =
        rememberModalBottomSheetState(skipPartiallyExpanded = addedApplicationKeys.isEmpty())
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmationDialog by rememberSaveable { mutableStateOf(false) }
    var keyToDelete by remember { mutableStateOf<ApplicationKey?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            modifier = Modifier.fillMaxSize(),
            onRefresh = { readApplicationKeys() },
            isRefreshing = messageState.isInProgress() && messageState.message is ConfigAppKeyGet
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                when (addedApplicationKeys.isNotEmpty()) {
                    true -> {
                        item {
                            SectionTitle(
                                modifier = Modifier.padding(top = 8.dp),
                                title = stringResource(R.string.label_added_application_keys)
                            )
                        }
                        items(
                            items = addedApplicationKeys,
                            key = { KeyIdGenerator.nextId() }
                        ) { key ->
                            // Hold the current state from the Swipe to Dismiss composable
                            val dismissState = rememberSwipeToDismissBoxState()
                            val isInUse = isKeyInUse(key)
                            SwipeToDismissKey(
                                isInUse = isInUse,
                                dismissState = dismissState,
                                key = key,
                                onSwiped = {
                                    if (isInUse) {
                                        keyToDelete = it
                                        showDeleteConfirmationDialog = true
                                        scope.launch { dismissState.reset() }
                                    } else {
                                        if (!messageState.isInProgress()) {
                                            send(ConfigAppKeyDelete(key = key))
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = context.getString(R.string.label_application_key_deleting),
                                                    duration = SnackbarDuration.Short,
                                                )
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        item { Spacer(modifier = Modifier.size(size = 16.dp)) }
                    }

                    false -> item {
                        MeshNoItemsAvailable(
                            modifier = Modifier.fillParentMaxSize(),
                            imageVector = Icons.Outlined.VpnKey,
                            title = stringResource(R.string.label_no_app_keys_added),
                            rationale = stringResource(R.string.label_no_app_keys_added_rationale)
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !showBottomSheet,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            ExtendedFloatingActionButton(
                modifier = Modifier.defaultMinSize(minWidth = 150.dp),
                text = { Text(text = stringResource(R.string.label_add_key)) },
                icon = { Icon(imageVector = Icons.Outlined.Add, contentDescription = null) },
                onClick = { showBottomSheet = true },
                expanded = true
            )
        }
    }

    if (showBottomSheet) {
        BottomSheetApplicationKeys(
            bottomSheetState = bottomSheetState,
            messageState = messageState,
            keys = availableApplicationKeys,
            onAppKeyClicked = { key ->
                scope
                    .launch { bottomSheetState.hide() }
                    .invokeOnCompletion {
                        send(ConfigAppKeyAdd(key = key))
                        if (!bottomSheetState.isVisible) showBottomSheet = !showBottomSheet
                    }
            },
            onAddApplicationKeyClicked = {
                runCatching {
                    onAddAppKeyClicked()
                    if (isLocalProvisionerNode) {
                        scope
                            .launch { bottomSheetState.hide() }
                            .invokeOnCompletion {
                                if (!bottomSheetState.isVisible) showBottomSheet = !showBottomSheet
                            }
                    }
                }.onFailure {
                    scope.launch { snackbarHostState.showSnackbar(message = it.describe()) }
                }
            },
            onDismissClick = {
                scope
                    .launch { bottomSheetState.hide() }
                    .invokeOnCompletion {
                        if (!bottomSheetState.isVisible) showBottomSheet = !showBottomSheet
                    }
            }
        )
    }

    if (showDeleteConfirmationDialog) {
        MeshAlertDialog(
            onDismissRequest = {
                keyToDelete = null
                showDeleteConfirmationDialog = !showDeleteConfirmationDialog
            },
            icon = Icons.Outlined.DeleteForever,
            iconColor = Color.Red,
            title = stringResource(R.string.label_remove_key),
            text = stringResource(id = R.string.label_remove_key_confirmation),
            dismissButtonText = stringResource(R.string.label_cancel),
            onDismissClick = {
                keyToDelete = null
                showDeleteConfirmationDialog = !showDeleteConfirmationDialog
            },
            confirmButtonText = stringResource(R.string.label_ok),
            onConfirmClick = {
                keyToDelete?.let { send(ConfigAppKeyDelete(key = it)) }
                showDeleteConfirmationDialog = !showDeleteConfirmationDialog
            }
        )
    }

    when (messageState) {
        is Failed -> MeshMessageStatusDialog(
            text = messageState.error.describe(),
            showDismissButton = !messageState.didFail(),
            onDismissRequest = resetMessageState,
        )

        is Completed -> messageState.response?.takeIf {
            it is ConfigStatusMessage
        }?.let {
            when (!(it as ConfigStatusMessage).isSuccess) {
                true -> MeshMessageStatusDialog(
                    text = it.message,
                    showDismissButton = true,
                    onDismissRequest = resetMessageState,
                )

                false -> scope.launch {
                    if (messageState.message is ConfigAppKeyDelete) {
                        snackbarHostState.run {
                            showSnackbar(
                                message = context.getString(R.string.label_application_key_deleted),
                                duration = SnackbarDuration.Short,
                            ).also { result ->
                                when (result) {
                                    SnackbarResult.Dismissed,
                                    SnackbarResult.ActionPerformed,
                                        -> resetMessageState()
                                }
                            }
                        }
                    } else if (messageState.message is ConfigAppKeyAdd) {
                        snackbarHostState
                            .run {
                                currentSnackbarData?.dismiss()
                                showSnackbar(
                                    message = context.getString(R.string.label_application_key_added),
                                    duration = SnackbarDuration.Short,
                                ).also { result ->
                                    when (result) {
                                        SnackbarResult.Dismissed,
                                        SnackbarResult.ActionPerformed,
                                            -> resetMessageState()
                                    }
                                }
                            }
                    }
                }
            }
        }

        else -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissKey(
    isInUse: Boolean,
    dismissState: SwipeToDismissBoxState,
    key: ApplicationKey,
    onSwiped: (ApplicationKey) -> Unit,
) {
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.Settled,
                    SwipeToDismissBoxValue.StartToEnd,
                    SwipeToDismissBoxValue.EndToStart,
                        -> if (isInUse) Color.Gray else Color.Red
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
        onDismiss = { onSwiped(key) },
        content = { key.Row() }
    )
}