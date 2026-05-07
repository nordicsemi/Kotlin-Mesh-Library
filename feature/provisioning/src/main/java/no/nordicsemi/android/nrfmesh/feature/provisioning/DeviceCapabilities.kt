package no.nordicsemi.android.nrfmesh.feature.provisioning

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.DisplaySettings
import androidx.compose.material.icons.outlined.EnhancedEncryption
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.KeyboardAlt
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Numbers
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItemHexTextField
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItemTextField
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.core.ui.showSnackbar
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.provisioning.AuthenticationMethod
import no.nordicsemi.kotlin.mesh.provisioning.ProvisioningCapabilities
import no.nordicsemi.kotlin.mesh.provisioning.ProvisioningParameters
import no.nordicsemi.kotlin.mesh.provisioning.UnprovisionedDevice

@Composable
internal fun DeviceCapabilities(
    capabilities: ProvisioningCapabilities,
    networkKeys: List<NetworkKey>,
    parameters: ProvisioningParameters,
    snackbarHostState: SnackbarHostState,
    unprovisionedDevice: UnprovisionedDevice,
    showAuthenticationBottomSheet: Boolean,
    onAuthenticationBottomSheetDismissed: (Boolean) -> Unit,
    onNameChanged: (String) -> Unit,
    onAddressChanged: (Address) -> Unit,
    isValidAddress: (Address) -> Boolean,
    onNetworkKeyClicked: (NetworkKey) -> Unit,
    onAuthenticationMethodSelected: (AuthenticationMethod) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    var isCurrentlyEditable by rememberSaveable { mutableStateOf(true) }
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Name(
            name = unprovisionedDevice.name,
            onNameChanged = onNameChanged,
            isCurrentlyEditable = isCurrentlyEditable,
            onEditableStateChanged = { isCurrentlyEditable = !isCurrentlyEditable }
        )
        SectionTitle(
            modifier = Modifier.padding(horizontal = 16.dp),
            title = stringResource(R.string.title_provisioning_data)
        )
        UnicastAddressRow(
            context = context,
            scope = scope,
            snackbarHostState = snackbarHostState,
            keyboardController = keyboardController,
            address = parameters.unicastAddress.address,
            onAddressChanged = {
                onAddressChanged(it)
            },
            isValidAddress = isValidAddress,
            isCurrentlyEditable = isCurrentlyEditable,
            onEditableStateChanged = { isCurrentlyEditable = !isCurrentlyEditable }
        )
        NetworkKeyRow(
            networkKeys = networkKeys,
            networkKey = parameters.networkKey,
            onNetworkKeyClick = onNetworkKeyClicked
        )
        SectionTitle(
            modifier = Modifier.padding(horizontal = 16.dp),
            title = stringResource(R.string.title_device_capabilities)
        )
        ElementsRow(
            title = stringResource(R.string.label_element_count),
            subtitle = "${capabilities.numberOfElements}"
        )
        SupportedAlgorithmsRow(
            title = stringResource(R.string.label_supported_algorithms),
            subtitle = capabilities.algorithms
                .joinToString(separator = "\n")
                .ifEmpty { "None" }
        )
        PublicKeyTypeRow(
            title = stringResource(R.string.label_public_key_type),
            subtitle = capabilities.publicKeyType
                .joinToString()
                .ifEmpty { "None" }
        )
        StaticOobTypeRow(
            title = stringResource(R.string.label_static_oob_type),
            subtitle = capabilities.oobTypes
                .joinToString()
                .ifEmpty { "None" }
        )
        OutputOobSizeRow(
            title = stringResource(R.string.label_output_oob_size),
            subtitle = "${capabilities.outputOobSize}"
        )
        OutputOobActionsRow(
            title = stringResource(R.string.label_output_oob_actions),
            subtitle = capabilities.outputOobActions
                .joinToString()
                .ifEmpty { "None" }
        )
        InputOobSizeRow(
            title = stringResource(R.string.label_input_oob_size),
            subtitle = "${capabilities.inputOobSize}"
        )
        InputOobActionsRow(
            title = stringResource(R.string.label_input_oob_actions),
            subtitle = capabilities.inputOobActions
                .joinToString()
                .ifBlank { "None" }
        )
        Spacer(modifier = Modifier.size(size = 16.dp))
    }

    if (showAuthenticationBottomSheet) {
        AuthSelectionBottomSheet(
            capabilities = capabilities,
            onConfirmClicked = { onAuthenticationMethodSelected(it) },
            onDismissRequest = { onAuthenticationBottomSheetDismissed(false) },
        )
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
        placeholder = stringResource(id = R.string.label_name),
        onValueChanged = onNameChanged,
        isEditable = isCurrentlyEditable,
        onEditableStateChanged = onEditableStateChanged
    )
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
private fun UnicastAddressRow(
    context: Context,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    keyboardController: SoftwareKeyboardController?,
    address: Address = UnicastAddress(1u).address,
    onAddressChanged: (Address) -> Unit,
    isValidAddress: (UShort) -> Boolean,
    isCurrentlyEditable: Boolean,
    onEditableStateChanged: () -> Unit,
) {
    var isError by rememberSaveable { mutableStateOf(false) }
    var supportingErrorText by rememberSaveable { mutableStateOf("") }
    ElevatedCardItemHexTextField(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Lan,
        title = stringResource(id = R.string.label_unicast_address),
        subtitle = address.toHexString(format = HexFormat.UpperCase),
        placeholder = stringResource(id = R.string.label_name),
        onValueChanged = {
            keyboardController?.hide()
            if (it.isNotEmpty()) {
                runCatching {
                    isError = !isValidAddress(it.toUShort(16))
                    onAddressChanged(it.toUShort(radix = 16))
                }.onFailure { throwable ->
                    supportingErrorText = throwable.message ?: ""
                    isError = true
                    showSnackbar(
                        scope = scope,
                        snackbarHostState = snackbarHostState,
                        message = context.getString(R.string.error_invalid_address)
                    )
                }
            }
        },
        isEditable = isCurrentlyEditable,
        onEditableStateChanged = onEditableStateChanged,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Text
        ),
        isError = isError,
        supportingText = {
            if (isError)
                Text(text = supportingErrorText)
        },
        regex = Regex(pattern = "[0-9A-Fa-f]{0,4}"),
        validator = Regex(pattern = "[0-9A-Fa-f]{4}")
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NetworkKeyRow(
    networkKey: NetworkKey,
    networkKeys: List<NetworkKey>,
    onNetworkKeyClick: (NetworkKey) -> Unit,
) {
    var name by remember(key1 = networkKey.index) { mutableStateOf(networkKey.name) }
    var isExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        modifier = Modifier.padding(horizontal = 16.dp),
        expanded = isExpanded,
        onExpandedChange = { isExpanded = it },
    ) {
        ElevatedCardItem(
            modifier = Modifier
                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            imageVector = Icons.Outlined.VpnKey,
            title = stringResource(R.string.label_network_key),
            titleAction = {
                IconButton(
                    modifier = Modifier.rotate(if (isExpanded) 180f else 0f),
                    onClick = { isExpanded = true },
                    content = {
                        Icon(
                            imageVector = Icons.Outlined.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            },
            subtitle = name
        )
        DropdownMenu(
            modifier = Modifier.exposedDropdownSize(),
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            networkKeys.forEachIndexed { index, key ->
                DropdownMenuItem(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.VpnKey,
                            contentDescription = null
                        )
                    },
                    text = {
                        Text(text = key.name)
                    },
                    onClick = {
                        name = key.name
                        onNetworkKeyClick(key)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ElementsRow(title: String, subtitle: String) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.DeviceHub,
        title = title,
        subtitle = subtitle
    )
}

@Composable
private fun SupportedAlgorithmsRow(title: String, subtitle: String) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.EnhancedEncryption,
        title = title,
        subtitle = subtitle,
        subtitlesMaxLines = Int.MAX_VALUE
    )
}

@Composable
private fun PublicKeyTypeRow(title: String, subtitle: String) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Campaign,
        title = title,
        subtitle = subtitle,
        subtitlesMaxLines = Int.MAX_VALUE
    )
}

@Composable
private fun StaticOobTypeRow(title: String, subtitle: String) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Key,
        title = title,
        subtitle = subtitle
    )
}

@Composable
private fun OutputOobSizeRow(title: String, subtitle: String) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Numbers,
        title = title,
        subtitle = subtitle,
        subtitlesMaxLines = Int.MAX_VALUE
    )
}

@Composable
private fun OutputOobActionsRow(title: String, subtitle: String) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.DisplaySettings,
        title = title,
        subtitle = subtitle,
        subtitlesMaxLines = Int.MAX_VALUE
    )
}

@Composable
private fun InputOobSizeRow(title: String, subtitle: String) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Numbers,
        title = title,
        subtitle = subtitle
    )
}

@Composable
private fun InputOobActionsRow(title: String, subtitle: String) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.KeyboardAlt,
        title = title,
        subtitle = subtitle,
        subtitlesMaxLines = Int.MAX_VALUE
    )
}