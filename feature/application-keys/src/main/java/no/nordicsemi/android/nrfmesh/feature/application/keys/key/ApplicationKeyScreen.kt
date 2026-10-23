@file:Suppress("unused")

package no.nordicsemi.android.nrfmesh.feature.application.keys.key

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssistWalker
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.copyToClipboard
import no.nordicsemi.android.nrfmesh.core.data.ui.ApplicationKeyData
import no.nordicsemi.android.nrfmesh.core.ui.ApplicationKeyRow
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItemTextField
import no.nordicsemi.android.nrfmesh.core.ui.KeyRow
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.application.keys.R
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey

@Composable
internal fun ApplicationKeyScreen(
    snackbarHostState: SnackbarHostState,
    uiState: ApplicationKeyScreenUiState,
    save: () -> Unit,
) {
    when (uiState.keyState) {
        is AppKeyState.Success -> {
            ApplicationKeyContent(
                snackbarHostState = snackbarHostState,
                key = uiState.keyState.key,
                networkKeys = uiState.networkKeys,
                save = save
            )
        }

        else -> {}
    }
}

@Composable
internal fun ApplicationKeyContent(
    snackbarHostState: SnackbarHostState,
    key: ApplicationKey,
    networkKeys: List<NetworkKey>,
    save: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val errorMessage = stringResource(R.string.error_cannot_change_bound_net_key)
    var isCurrentlyEditable by rememberSaveable { mutableStateOf(true) }
    val applicationKey by remember(key.index) { derivedStateOf { ApplicationKeyData(key = key) } }
    var boundNetKeyIndex by remember(key.index) {
        mutableIntStateOf(key.boundNetworkKey.index.toInt())
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp)
    ) {
        SectionTitle(
            modifier = Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp),
            title = stringResource(id = R.string.label_application_key)
        )
        Name(
            name = applicationKey.name,
            onNameChanged = {
                key.name = it
                save()
            },
            isCurrentlyEditable = isCurrentlyEditable,
            onEditableStateChanged = { isCurrentlyEditable = !isCurrentlyEditable }
        )
        Key(
            key = applicationKey.key,
            isInUse = applicationKey.isInUse,
            onKeyChanged = {
                key.setKey(key = it)
                save()
            },
            isCurrentlyEditable = isCurrentlyEditable,
            onEditableStateChanged = { isCurrentlyEditable = !isCurrentlyEditable }
        )
        OldKey(oldKey = applicationKey.oldKey)
        KeyIndex(index = applicationKey.index)
        SectionTitle(
            modifier = Modifier.padding(horizontal = 16.dp),
            title = stringResource(R.string.label_bound_network_key)
        )
        networkKeys.forEach { networkKey ->
            key(networkKey.index) {
                ElevatedCardItem(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = {
                        if (!applicationKey.isInUse) {
                            boundNetKeyIndex = networkKey.index.toInt()
                            key.bind(networkKey = networkKey)
                            save()
                        } else scope.launch {
                            snackbarHostState.showSnackbar(message = errorMessage)
                        }
                    },
                    imageVector = Icons.Outlined.VpnKey,
                    title = networkKey.name,
                    titleAction = {
                        if (networkKey.index.toInt() == boundNetKeyIndex) {
                            Icon(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                imageVector = Icons.Outlined.CheckCircleOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.surfaceTint
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun Name(
    name: String,
    onNameChanged: (String) -> Unit,
    isCurrentlyEditable: Boolean,
    onEditableStateChanged: () -> Unit,
) {
    ElevatedCardItemTextField(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Badge,
        title = stringResource(id = R.string.label_name),
        subtitle = name,
        onValueChanged = onNameChanged,
        isEditable = isCurrentlyEditable,
        onEditableStateChanged = onEditableStateChanged,
    )
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
private fun Key(
    key: ByteArray,
    isInUse: Boolean,
    onKeyChanged: (ByteArray) -> Unit,
    isCurrentlyEditable: Boolean,
    onEditableStateChanged: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val applicationKeyLabel = stringResource(id = R.string.label_application_key)
    KeyRow(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clickable(enabled = isCurrentlyEditable) {
                copyToClipboard(
                    scope = scope,
                    clipboard = clipboard,
                    text = key.toHexString(format = HexFormat.UpperCase),
                    label = applicationKeyLabel
                )
            },
        title = stringResource(id = R.string.label_key),
        key = key,
        onKeyChanged = onKeyChanged,
        isEditable = isCurrentlyEditable && !isInUse,
        onEditableStateChanged = onEditableStateChanged
    )
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
private fun OldKey(oldKey: ByteArray?) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboard.current
    val oldKeyLabel = stringResource(id = R.string.label_old_key)
    ApplicationKeyRow(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.AssistWalker,
        title = stringResource(id = R.string.label_old_key),
        subtitle = oldKey?.toHexString() ?: stringResource(id = R.string.label_na),
        onClick = {
            if (oldKey != null) {
                copyToClipboard(
                    scope = scope,
                    clipboard = clipboard,
                    text = oldKey.toHexString(format = HexFormat.UpperCase),
                    label = oldKeyLabel
                )
            }
        }
    )
}

@Composable
private fun KeyIndex(index: KeyIndex) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.FormatListNumbered,
        title = stringResource(id = R.string.label_key_index),
        subtitle = index.toString()
    )
}
