package no.nordicsemi.android.nrfmesh.feature.model.generic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedHexTextField
import no.nordicsemi.android.nrfmesh.core.ui.MeshOutlinedTextField
import no.nordicsemi.android.nrfmesh.core.ui.MeshSingleLineListItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.DevicePropertyCharacteristic
import no.nordicsemi.kotlin.mesh.core.messages.GenericPropertyAccess
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertiesGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertyGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertySet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertySetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertyStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericClientPropertiesGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericClientPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertiesGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertyGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertySet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertySetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertyStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertiesGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertyGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertySet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertySetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertyStatus
import no.nordicsemi.kotlin.mesh.core.model.Model

/** The Generic Property Server models. Each one exposes a different set of properties. */
internal enum class PropertyServerKind { MANUFACTURER, ADMIN, USER, CLIENT }

/** The state of a property, as reported by a Property Status message. */
internal data class PropertyStatus(
    val propertyId: UShort,
    val access: GenericPropertyAccess?,
    val value: DevicePropertyCharacteristic?,
)

/**
 * Lists the properties of a Generic Manufacturer, Admin, User or Client Property Server and allows
 * the selected property to be read and, except for the Client Property Server, set.
 *
 * The properties are read using the refresh button, and a property is read when it is selected.
 * Nothing is sent when the screen is opened. Values are entered as hexadecimal bytes
 * as the format of a value depends on the property.
 */
@Composable
internal fun GenericPropertyServer(
    model: Model,
    kind: PropertyServerKind,
    messageState: MessageState,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    val hasBoundKey = model.boundApplicationKeys.isNotEmpty()
    val enabled = hasBoundKey && !messageState.isInProgress()
    var propertyIds by remember { mutableStateOf<List<UShort>?>(null) }
    var status by remember { mutableStateOf<PropertyStatus?>(null) }
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    val selectedId = propertyIds?.getOrNull(selected)

    LaunchedEffect(key1 = messageState) {
        if (messageState is Completed) {
            messageState.response?.propertyIds()?.let {
                propertyIds = it
                // The selected property may no longer exist.
                if (selected !in it.indices) selected = -1
            }
            messageState.response?.toPropertyStatus()?.let { status = it }
            // There is no response to an unacknowledged set, so the property is read back.
            if (messageState.message.isUnacknowledgedPropertySet()) {
                selectedId?.let { kind.propertyGet(it) }?.let { sendApplicationMessage(model, it) }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionTitle(
            modifier = Modifier
                .weight(weight = 1f)
                .padding(horizontal = 16.dp),
            title = stringResource(R.string.label_properties)
        )
        MeshIconButton(
            onClick = { sendApplicationMessage(model, kind.propertiesGet()) },
            buttonIcon = Icons.Outlined.Autorenew,
            enabled = enabled,
            isOnClickActionInProgress = messageState.isInProgress() &&
                    messageState.message.isPropertiesGet(),
        )
    }
    propertyIds?.let { ids ->
        if (ids.isEmpty()) {
            ElevatedCardItem(
                modifier = Modifier.padding(horizontal = 16.dp),
                imageVector = Icons.Outlined.Tune,
                title = stringResource(R.string.label_no_properties)
            )
        }
        ids.forEachIndexed { index, id ->
            ElevatedCardItem(
                modifier = Modifier.padding(horizontal = 16.dp),
                imageVector = Icons.Outlined.Tune,
                title = id.propertyName(),
                subtitle = id.toHex(),
                enabled = enabled && kind != PropertyServerKind.CLIENT,
                onClick = if (kind == PropertyServerKind.CLIENT) null else {
                    {
                        selected = index
                        status = null
                        kind.propertyGet(id)?.let { sendApplicationMessage(model, it) }
                    }
                },
                titleAction = {
                    if (index == selected) {
                        Icon(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .size(24.dp),
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        }
        if (selectedId == null && ids.isNotEmpty() && kind != PropertyServerKind.CLIENT) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.label_select_property),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
    if (selectedId != null && kind != PropertyServerKind.CLIENT) {
        PropertyControls(
            model = model,
            kind = kind,
            propertyId = selectedId,
            enabled = enabled,
            messageState = messageState,
            sendApplicationMessage = sendApplicationMessage
        )
        SectionTitle(
            modifier = Modifier.padding(horizontal = 16.dp),
            title = stringResource(R.string.label_status)
        )
        val current = status?.takeIf { it.propertyId == selectedId }
        val unknown = stringResource(R.string.label_unknown)
        ElevatedCardItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            imageVector = Icons.Outlined.Lock,
            title = stringResource(R.string.label_access),
            subtitle = current?.access?.label() ?: unknown
        )
        ElevatedCardItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            imageVector = Icons.Outlined.Tune,
            title = stringResource(R.string.label_value),
            subtitle = current?.value?.description ?: unknown,
            subtitlesMaxLines = Int.MAX_VALUE
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PropertyControls(
    model: Model,
    kind: PropertyServerKind,
    propertyId: UShort,
    enabled: Boolean,
    messageState: MessageState,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    val options = when (kind) {
        PropertyServerKind.ADMIN -> GenericPropertyAccess.entries
        PropertyServerKind.MANUFACTURER ->
            listOf(GenericPropertyAccess.PROHIBITED, GenericPropertyAccess.READ)

        else -> emptyList()
    }
    var accessIndex by rememberSaveable(propertyId) { mutableIntStateOf(options.lastIndex.coerceAtLeast(0)) }
    var isAccessExpanded by rememberSaveable { mutableStateOf(false) }
    var hex by rememberSaveable(propertyId, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    var acknowledged by rememberSaveable { mutableStateOf(true) }
    val bytes = hex.text.parseHexBytes()
    // A user property always has a value, but an admin property may be set without one.
    val isValueValid = when (kind) {
        PropertyServerKind.USER -> bytes != null && bytes.isNotEmpty()
        PropertyServerKind.ADMIN -> bytes != null
        else -> true
    }
    val access = options.getOrNull(accessIndex)
    val value = bytes?.takeIf { it.isNotEmpty() }?.let { DevicePropertyCharacteristic.Other(it) }

    SectionTitle(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = stringResource(R.string.label_controls)
    )
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Lock,
        title = propertyId.propertyName(),
        subtitle = propertyId.toHex(),
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                if (access != null) {
                    ExposedDropdownMenuBox(
                        expanded = isAccessExpanded,
                        onExpandedChange = { isAccessExpanded = it && enabled },
                    ) {
                        MeshOutlinedTextField(
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            label = { Text(text = stringResource(R.string.label_access)) },
                            value = TextFieldValue(access.label()),
                            onValueChanged = {},
                            readOnly = true,
                            enabled = enabled,
                            isError = false,
                            internalTrailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isAccessExpanded)
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = isAccessExpanded,
                            onDismissRequest = { isAccessExpanded = false },
                        ) {
                            options.forEachIndexed { index, option ->
                                DropdownMenuItem(
                                    text = { Text(text = option.label()) },
                                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                    onClick = {
                                        accessIndex = index
                                        isAccessExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                if (kind != PropertyServerKind.MANUFACTURER) {
                    MeshOutlinedHexTextField(
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(text = stringResource(R.string.label_value_hex)) },
                        value = hex,
                        onValueChanged = { hex = it },
                        enabled = enabled,
                        isError = !isValueValid && hex.text.isNotEmpty()
                    )
                }
                MeshSingleLineListItem(
                    modifier = Modifier.fillMaxSize(),
                    imageVector = Icons.Outlined.Verified,
                    title = stringResource(R.string.label_acknowledged),
                    trailingComposable = {
                        Switch(
                            enabled = enabled,
                            checked = acknowledged,
                            onCheckedChange = { acknowledged = it },
                        )
                    }
                )
            }
        },
        actions = {
            MeshOutlinedButton(
                text = stringResource(R.string.label_get),
                buttonIcon = Icons.Outlined.Download,
                enabled = enabled,
                isOnClickActionInProgress = messageState.isInProgress() &&
                        messageState.message.isPropertyGet(),
                onClick = { kind.propertyGet(propertyId)?.let { sendApplicationMessage(model, it) } }
            )
            Spacer(modifier = Modifier.size(16.dp))
            MeshOutlinedButton(
                text = stringResource(R.string.label_set),
                buttonIcon = Icons.Outlined.Upload,
                enabled = enabled && isValueValid,
                isOnClickActionInProgress = messageState.isInProgress() &&
                        messageState.message.isPropertySet(),
                onClick = {
                    kind.propertySet(propertyId, access, value, acknowledged)
                        ?.let { sendApplicationMessage(model, it) }
                }
            )
        }
    )
}

internal fun PropertyServerKind.propertiesGet(): MeshMessage = when (this) {
    PropertyServerKind.MANUFACTURER -> GenericManufacturerPropertiesGet()
    PropertyServerKind.ADMIN -> GenericAdminPropertiesGet()
    PropertyServerKind.USER -> GenericUserPropertiesGet()
    // The Client Property ID 0x0000 is prohibited, so the list starts at the lowest valid ID.
    PropertyServerKind.CLIENT -> GenericClientPropertiesGet(propertyId = 0x0001u)
}

internal fun PropertyServerKind.propertyGet(id: UShort): MeshMessage? = when (this) {
    PropertyServerKind.MANUFACTURER -> GenericManufacturerPropertyGet(propertyId = id)
    PropertyServerKind.ADMIN -> GenericAdminPropertyGet(propertyId = id)
    PropertyServerKind.USER -> GenericUserPropertyGet(propertyId = id)
    PropertyServerKind.CLIENT -> null
}

internal fun PropertyServerKind.propertySet(
    id: UShort,
    access: GenericPropertyAccess?,
    value: DevicePropertyCharacteristic?,
    acknowledged: Boolean,
): MeshMessage? = when (this) {
    PropertyServerKind.MANUFACTURER -> access?.let {
        if (acknowledged) GenericManufacturerPropertySet(id, it)
        else GenericManufacturerPropertySetUnacknowledged(id, it)
    }

    PropertyServerKind.ADMIN -> access?.let {
        if (acknowledged) GenericAdminPropertySet(id, it, value)
        else GenericAdminPropertySetUnacknowledged(id, it, value)
    }

    PropertyServerKind.USER -> value?.let {
        if (acknowledged) GenericUserPropertySet(id, it)
        else GenericUserPropertySetUnacknowledged(id, it)
    }

    PropertyServerKind.CLIENT -> null
}

internal fun MeshMessage.propertyIds(): List<UShort>? = when (this) {
    is GenericManufacturerPropertiesStatus -> propertyIds
    is GenericAdminPropertiesStatus -> propertyIds
    is GenericUserPropertiesStatus -> propertyIds
    is GenericClientPropertiesStatus -> propertyIds
    else -> null
}

internal fun MeshMessage.toPropertyStatus(): PropertyStatus? = when (this) {
    is GenericManufacturerPropertyStatus -> PropertyStatus(propertyId, access, value)
    is GenericAdminPropertyStatus -> PropertyStatus(propertyId, access, value)
    is GenericUserPropertyStatus -> PropertyStatus(propertyId, access, value)
    else -> null
}

private fun Any?.isPropertiesGet() = this is GenericManufacturerPropertiesGet ||
        this is GenericAdminPropertiesGet || this is GenericUserPropertiesGet ||
        this is GenericClientPropertiesGet

private fun Any?.isPropertyGet() = this is GenericManufacturerPropertyGet ||
        this is GenericAdminPropertyGet || this is GenericUserPropertyGet

private fun Any?.isPropertySet() = this is GenericManufacturerPropertySet ||
        this is GenericManufacturerPropertySetUnacknowledged ||
        this is GenericAdminPropertySet || this is GenericAdminPropertySetUnacknowledged ||
        this is GenericUserPropertySet || this is GenericUserPropertySetUnacknowledged

private fun Any?.isUnacknowledgedPropertySet() =
    this is GenericManufacturerPropertySetUnacknowledged ||
            this is GenericAdminPropertySetUnacknowledged ||
            this is GenericUserPropertySetUnacknowledged

/** Returns the name of the Device Property, or its Property ID if it is not known. */
private fun UShort.propertyName(): String = DeviceProperty.from(id = this)?.propertyName ?: toHex()

private fun UShort.toHex(): String = toHexString(
    format = HexFormat {
        number.prefix = "0x"
        upperCase = true
    }
)

@Composable
private fun GenericPropertyAccess.label(): String = stringResource(
    when (this) {
        GenericPropertyAccess.PROHIBITED -> R.string.label_prohibited
        GenericPropertyAccess.READ -> R.string.label_read
        GenericPropertyAccess.WRITE -> R.string.label_write
        GenericPropertyAccess.READ_WRITE -> R.string.label_read_write
    }
)

/** Parses hexadecimal bytes, ignoring white space, or returns null if the text is not valid. */
internal fun String.parseHexBytes(): ByteArray? {
    val digits = filterNot { it.isWhitespace() }
    if (digits.length % 2 != 0 || !digits.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) {
        return null
    }
    return ByteArray(digits.length / 2) { digits.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
