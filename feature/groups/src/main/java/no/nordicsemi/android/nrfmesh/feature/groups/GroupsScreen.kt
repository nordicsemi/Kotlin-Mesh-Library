package no.nordicsemi.android.nrfmesh.feature.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import no.nordicsemi.android.common.ui.view.CircularIcon
import no.nordicsemi.android.nrfmesh.core.ui.MeshAlertDialog
import no.nordicsemi.android.nrfmesh.core.ui.MeshItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshNoItemsAvailable
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedTextField
import no.nordicsemi.android.nrfmesh.core.ui.isCompactWidth
import no.nordicsemi.kotlin.mesh.core.exception.GroupAlreadyExists
import no.nordicsemi.kotlin.mesh.core.exception.GroupInUse
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.PrimaryGroupAddress
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import kotlin.uuid.Uuid

@OptIn(ExperimentalLayoutApi::class, ExperimentalStdlibApi::class)
@Composable
internal fun GroupsScreen(
    snackbarHostState: SnackbarHostState,
    uiState: GroupsScreenUiState,
    navigateToGroup: (PrimaryGroupAddress) -> Unit,
    onAddGroupClicked: (Group) -> Unit,
    nextAvailableGroupAddress: () -> GroupAddress,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAddGroupDialog by rememberSaveable { mutableStateOf(false) }
    Box(modifier = Modifier.fillMaxSize()) {
        Groups(
            uiState = uiState,
            navigateToGroup = navigateToGroup,
        )
        ExtendedFloatingActionButton(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .defaultMinSize(minWidth = 150.dp),
            text = { Text(text = stringResource(R.string.label_add_group)) },
            icon = {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null
                )
            },
            onClick = { showAddGroupDialog = true },
            expanded = true
        )
    }


    if (showAddGroupDialog) {
        var isError by rememberSaveable { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }
        val initialValue by remember {
            mutableStateOf(
                nextAvailableGroupAddress()
                    .address
                    .toHexString(format = HexFormat.UpperCase)
            )
        }
        var address by rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(
                TextFieldValue(
                    text = initialValue,
                    selection = TextRange(initialValue.length)
                )
            )
        }
        MeshAlertDialog(
            icon = Icons.Outlined.GroupWork,
            title = stringResource(R.string.label_add_group),
            text = stringResource(R.string.label_add_group_rationale),
            onDismissRequest = { showAddGroupDialog = false },
            content = {
                MeshOutlinedTextField(
                    value = address,
                    onValueChanged = {
                        isError = false
                        address = it
                        if (it.text.isNotEmpty()) {
                            if (GroupAddress.isValid(it.text.toUShort(16))) {
                                isError = false
                                snackbarHostState.currentSnackbarData?.dismiss()
                            } else {
                                isError = true
                                errorMessage =
                                    context.getString(R.string.label_invalid_group_address)
                            }
                        }
                    },
                    label = { Text(text = stringResource(id = R.string.label_address)) },
                    supportingText = {
                        if (isError) {
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    regex = Regex("^[0-9A-Fa-f]{0,4}$"),
                    isError = isError,
                )
                Row(
                    modifier = Modifier.padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp)
                ) {
                    TextButton(
                        onClick = {
                            runCatching {
                                val group = Group(
                                    address = VirtualAddress(uuid = Uuid.random()),
                                    _name = "New Group"
                                )
                                onAddGroupClicked(group)
                                    .also {
                                        showAddGroupDialog = false
                                        navigateToGroup(group.address)
                                    }
                            }.onFailure {
                                scope.launch {
                                    snackbarHostState.currentSnackbarData?.dismiss()
                                    snackbarHostState.showSnackbar(
                                        message = when (it) {
                                            is GroupAlreadyExists -> context
                                                .getString(R.string.label_group_already_exists)

                                            is GroupInUse -> context
                                                .getString(R.string.label_group_in_use)

                                            else -> it.message ?: context
                                                .getString(R.string.label_failed_to_add_group)
                                        },
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        },
                        content = {
                            Text(text = stringResource(R.string.label_virtual_label))
                        }
                    )
                    Spacer(modifier = Modifier.weight(weight = 1f))
                    TextButton(
                        onClick = { showAddGroupDialog = false },
                        content = { Text(text = stringResource(R.string.label_cancel)) }
                    )
                    TextButton(
                        onClick = {
                            if (address.text.isNotEmpty()) {
                                if (GroupAddress.isValid(address.text.toUShort(16))) {
                                    isError = false
                                    runCatching {
                                        val group = Group(
                                            address = MeshAddress.create(
                                                address = address.text.toUShort(radix = 16)
                                            ) as GroupAddress,
                                            _name = "New Group"
                                        )
                                        onAddGroupClicked(group).also {
                                            showAddGroupDialog = false
                                            navigateToGroup(group.address)
                                        }
                                    }.onFailure {
                                        scope.launch {
                                            snackbarHostState.currentSnackbarData?.dismiss()
                                            snackbarHostState.showSnackbar(
                                                message = it.message
                                                    ?: context.getString(R.string.label_failed_to_add_group),
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                } else {
                                    isError = true
                                    errorMessage =
                                        context.getString(R.string.label_invalid_group_address)
                                }
                            }
                        },
                        content = { Text(text = stringResource(R.string.label_add)) }
                    )
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalStdlibApi::class)
@Composable
private fun Groups(
    uiState: GroupsScreenUiState,
    navigateToGroup: (PrimaryGroupAddress) -> Unit,
) {
    if (isCompactWidth()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(all = 16.dp),
            content = {
                items(
                    items = uiState.groups,
                    key = { it.address.toHexString() }
                ) { group ->
                    MeshItem(
                        modifier = Modifier.fillMaxWidth(),
                        icon = { CircularIcon(imageVector = Icons.Outlined.GroupWork) },
                        title = group.name,
                        subtitle = group.address.run {
                            when (this) {
                                is GroupAddress -> group.address.address.toHexString(
                                    format = HexFormat {
                                        number.prefix = "Address: 0x"
                                        upperCase = true
                                    }
                                )

                                else -> "Address: ${
                                    (this as VirtualAddress).uuid.toString().uppercase()
                                }"
                            }
                        },
                        onClick = { navigateToGroup(group.address) },
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
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            Spacer(
                modifier = Modifier
                    .height(height = 8.dp)
                    .fillMaxWidth()
            )
            uiState.groups.forEach { group ->
                MeshItem(
                    icon = { CircularIcon(imageVector = Icons.Outlined.GroupWork) },
                    title = group.name,
                    subtitle = group.address.run {
                        if (this is GroupAddress) {
                            "0x${group.address.address.toHexString(format = HexFormat.UpperCase)}"
                        } else {
                            (this as VirtualAddress).uuid.toString().uppercase()
                        }
                    },
                    onClick = { navigateToGroup(group.address) },
                )
            }
            Spacer(
                modifier = Modifier
                    .height(height = 8.dp)
                    .fillMaxWidth()
            )
        }
    }
    if (uiState.groups.isEmpty()) {
        MeshNoItemsAvailable(
            modifier = Modifier.fillMaxSize(),
            imageVector = Icons.Outlined.GroupWork,
            title = stringResource(R.string.label_no_groups_currently_added),
            rationale = stringResource(R.string.label_no_groups_currently_added_rationale),
        )
    }
}