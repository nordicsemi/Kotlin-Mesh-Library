package no.nordicsemi.android.nrfmesh.feature.model.generic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
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
import no.nordicsemi.android.nrfmesh.core.ui.MeshTwoLineListItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.DevicePropertyCharacteristic
import no.nordicsemi.kotlin.mesh.core.messages.GenericPropertyAccess
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.PropertyField
import no.nordicsemi.kotlin.mesh.core.messages.PropertyFieldType
import no.nordicsemi.kotlin.mesh.core.messages.characteristicOf
import no.nordicsemi.kotlin.mesh.core.messages.inputFields
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
    // The node does not reply to an unacknowledged set, so its status is not known after one is sent.
    val send: (Model, MeshMessage) -> Unit = { target, message ->
        if (message.isUnacknowledgedPropertySet()) status = null
        sendApplicationMessage(target, message)
    }

    LaunchedEffect(key1 = messageState) {
        if (messageState is Completed) {
            messageState.response?.propertyIds()?.let {
                propertyIds = it
                // The selected property may no longer exist.
                if (selected !in it.indices) selected = -1
            }
            messageState.response?.toPropertyStatus()?.let { status = it }
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
    if (propertyIds.isNullOrEmpty()) {
        ElevatedCardItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            imageVector = Icons.Outlined.Tune,
            title = stringResource(R.string.label_no_properties)
        )
    }
    propertyIds?.takeIf { it.isNotEmpty() }?.let { ids ->
        if (kind == PropertyServerKind.CLIENT) {
            // The properties of a Client Property Server can only be listed.
            ids.forEach { id ->
                ElevatedCardItem(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    imageVector = propertyIconFor(propertyName = id.propertyName()),
                    title = id.propertyName(),
                    subtitle = stringResource(R.string.label_property_id, id.toHex())
                )
            }
        } else {
            PropertyDropdown(
                propertyIds = ids,
                selected = selected,
                enabled = enabled,
                onSelected = { index ->
                    selected = index
                    status = null
                    kind.propertyGet(ids[index])?.let { sendApplicationMessage(model, it) }
                }
            )
        }
    }
    if (selectedId != null && kind != PropertyServerKind.CLIENT) {
        PropertyControls(
            model = model,
            kind = kind,
            propertyId = selectedId,
            currentAccess = status?.takeIf { it.propertyId == selectedId }?.access,
            enabled = enabled,
            messageState = messageState,
            sendApplicationMessage = send
        )
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
                title = stringResource(R.string.label_status)
            )
            MeshIconButton(
                onClick = {
                    kind.propertyGet(selectedId)?.let { sendApplicationMessage(model, it) }
                },
                buttonIcon = Icons.Outlined.Autorenew,
                enabled = enabled,
                isOnClickActionInProgress = messageState.isInProgress() &&
                        messageState.message.isPropertyGet(),
            )
        }
        val current = status?.takeIf { it.propertyId == selectedId }
        val unknown = stringResource(R.string.label_unknown)
        StatusItem(
            icon = Icons.Outlined.Lock,
            title = stringResource(R.string.label_access),
            value = current?.access?.label()
        )
        ElevatedCardItem(
            modifier = Modifier.padding(horizontal = 16.dp),
            imageVector = propertyIconFor(propertyName = selectedId.propertyName()),
            title = stringResource(R.string.label_value),
            subtitle = current?.value?.description ?: unknown,
            subtitlesMaxLines = Int.MAX_VALUE
        )
    }
}

/** Lets the user pick one of the [propertyIds] from a drop down list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PropertyDropdown(
    propertyIds: List<UShort>,
    selected: Int,
    enabled: Boolean,
    onSelected: (Int) -> Unit,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val current = propertyIds.getOrNull(selected)
    ExposedDropdownMenuBox(
        modifier = Modifier.padding(horizontal = 16.dp),
        expanded = isExpanded,
        onExpandedChange = { isExpanded = it && enabled },
    ) {
        ElevatedCardItem(
            modifier = Modifier.menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            imageVector = current?.let { propertyIconFor(propertyName = it.propertyName()) }
                ?: Icons.Outlined.Tune,
            title = current?.propertyName() ?: stringResource(R.string.label_select_property),
            subtitle = current?.let { stringResource(R.string.label_property_id, it.toHex()) },
            enabled = enabled,
            titleAction = {
                ExposedDropdownMenuDefaults.TrailingIcon(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    expanded = isExpanded
                )
            }
        )
        ExposedDropdownMenu(
            modifier = Modifier.exposedDropdownSize(),
            expanded = isExpanded,
            shape = RoundedCornerShape(size = 16.dp),
            onDismissRequest = { isExpanded = false },
        ) {
            propertyIds.forEachIndexed { index, id ->
                DropdownMenuItem(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    text = {
                        MeshTwoLineListItem(
                            leadingComposable = {
                                Icon(
                                    modifier = Modifier.padding(end = 16.dp),
                                    imageVector = propertyIconFor(propertyName = id.propertyName()),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            title = id.propertyName(),
                            subtitle = stringResource(R.string.label_property_id, id.toHex()),
                            trailingComposable = {
                                if (index == selected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    },
                    onClick = {
                        isExpanded = false
                        onSelected(index)
                    }
                )
                if (index < propertyIds.lastIndex) HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PropertyControls(
    model: Model,
    kind: PropertyServerKind,
    propertyId: UShort,
    currentAccess: GenericPropertyAccess?,
    enabled: Boolean,
    messageState: MessageState,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    val canWrite = canWriteValue(kind = kind, access = currentAccess)
    val options = when (kind) {
        PropertyServerKind.ADMIN -> GenericPropertyAccess.entries
        PropertyServerKind.MANUFACTURER ->
            listOf(GenericPropertyAccess.PROHIBITED, GenericPropertyAccess.READ)

        else -> emptyList()
    }
    var accessIndex by rememberSaveable(propertyId) {
        mutableIntStateOf(options.lastIndex.coerceAtLeast(minimumValue = 0))
    }
    // Shows the access reported by the node.
    LaunchedEffect(key1 = currentAccess) {
        options.indexOf(currentAccess).takeIf { it >= 0 }?.let { accessIndex = it }
    }
    var isAccessExpanded by rememberSaveable { mutableStateOf(false) }
    var hex by rememberSaveable(propertyId, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    var acknowledged by rememberSaveable { mutableStateOf(true) }
    // The value is typed when the library knows the characteristic of the property, otherwise it
    // is given as hexadecimal bytes.
    val property = remember(propertyId) { DeviceProperty.from(id = propertyId) }
    val fields = property?.inputFields.takeIf { kind != PropertyServerKind.MANUFACTURER }
    var entries by rememberSaveable(propertyId, stateSaver = entriesSaver) {
        mutableStateOf(fields.orEmpty().map { TextFieldValue(text = it.defaultText()) })
    }
    val bytes = hex.text.parseHexBytes()
    val typed = if (fields != null) property?.characteristicOf(entries.map { it.text }) else null
    val isValueValid = isSetAllowed(
        kind = kind,
        canWrite = canWrite,
        hasTypedFields = fields != null,
        typed = typed,
        bytes = bytes
    )
    val access = options.getOrNull(accessIndex)
    val value = if (!canWrite) null else if (fields != null) typed else {
        bytes?.takeIf { it.isNotEmpty() }?.let { DevicePropertyCharacteristic.Other(it) }
    }

    SectionTitle(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = stringResource(R.string.label_controls)
    )
    OutlinedCard(modifier = Modifier.padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            if (access != null) {
                ExposedDropdownMenuBox(
                    expanded = isAccessExpanded,
                    onExpandedChange = { isAccessExpanded = it && enabled },
                ) {
                    MeshOutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(type = ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        externalLeadingIcon = {
                            Icon(
                                modifier = Modifier.padding(end = 16.dp),
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
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
            if (kind == PropertyServerKind.USER) {
                MeshSingleLineListItem(
                    modifier = Modifier.fillMaxSize(),
                    imageVector = Icons.Outlined.Lock,
                    title = stringResource(R.string.label_access),
                    trailingComposable = {
                        Text(
                            text = currentAccess?.label()
                                ?: stringResource(R.string.label_unknown)
                        )
                    }
                )
            }
            if (fields != null) {
                fields.forEachIndexed { index, field ->
                    PropertyFieldInput(
                        field = field,
                        value = entries[index],
                        enabled = enabled && canWrite,
                        icon = propertyIconFor(propertyId.propertyName()),
                        onValueChanged = { new ->
                            entries = entries.toMutableList().also { it[index] = new }
                        }
                    )
                }
            } else if (kind != PropertyServerKind.MANUFACTURER) {
                MeshOutlinedHexTextField(
                    modifier = Modifier.fillMaxWidth(),
                    externalLeadingIcon = {
                        Icon(
                            modifier = Modifier.padding(end = 16.dp),
                            imageVector = propertyIconFor(propertyId.propertyName()),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    label = { Text(text = stringResource(R.string.label_value_hex)) },
                    value = hex,
                    onValueChanged = { hex = it },
                    regex = Regex(pattern = "^[0-9A-Fa-f]*$"),
                    enabled = enabled && canWrite,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
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
        }
    }
}

/** Keeps the typed values of a property while the screen is rotated. */
private val entriesSaver = listSaver<List<TextFieldValue>, String>(
    save = { values -> values.map { it.text } },
    restore = { texts -> texts.map { TextFieldValue(text = it, selection = TextRange(it.length)) } }
)

private fun PropertyField.defaultText() = if (type == PropertyFieldType.BOOLEAN) "false" else ""

/** Whether the text is a number in the range of the field, without a fraction when required. */
internal fun PropertyField.accepts(text: String): Boolean {
    if (type == PropertyFieldType.BOOLEAN) return text == "true" || text == "false"
    val number = text.trim().toBigDecimalOrNull() ?: return false
    val inRange = range?.contains(number) ?: true
    return inRange && (type != PropertyFieldType.WHOLE_NUMBER || number.stripTrailingZeros().scale() <= 0)
}

/** A switch for a boolean, or a number field with the unit of the value. */
@Composable
private fun PropertyFieldInput(
    field: PropertyField,
    value: TextFieldValue,
    enabled: Boolean,
    icon: ImageVector,
    onValueChanged: (TextFieldValue) -> Unit,
) {
    if (field.type == PropertyFieldType.BOOLEAN) {
        MeshSingleLineListItem(
            modifier = Modifier.fillMaxSize(),
            imageVector = icon,
            title = field.name,
            trailingComposable = {
                Switch(
                    enabled = enabled,
                    checked = value.text == "true",
                    onCheckedChange = { onValueChanged(TextFieldValue(text = it.toString())) },
                )
            }
        )
    } else {
        val isWhole = field.type == PropertyFieldType.WHOLE_NUMBER
        val allowed = remember(isWhole) {
            // Negative numbers are accepted only when the range starts below zero.
            val sign = if (field.range?.start?.signum() == -1) "-?" else ""
            Regex(if (isWhole) "^$sign\\d*$" else "^$sign\\d*\\.?\\d*$")
        }
        MeshOutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            externalLeadingIcon = {
                Icon(
                    modifier = Modifier.padding(end = 16.dp),
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            label = { Text(text = field.name) },
            value = value,
            onValueChanged = onValueChanged,
            regex = allowed,
            enabled = enabled,
            suffix = field.unit?.let { unit -> { Text(text = unit) } },
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isWhole) KeyboardType.Number else KeyboardType.Decimal
            ),
            isError = value.text.isNotEmpty() && !field.accepts(value.text)
        )
    }
}

/**
 * Whether the value of a property can be changed.
 *
 * The access is what a user may do with the property. A user cannot write the value unless the
 * node reported that it can be written, but an admin is not limited by it. Until the access is
 * known, the value can be tried.
 */
internal fun canWriteValue(kind: PropertyServerKind, access: GenericPropertyAccess?): Boolean =
    kind != PropertyServerKind.USER || access == null ||
            access == GenericPropertyAccess.WRITE || access == GenericPropertyAccess.READ_WRITE

/**
 * Whether Set can be pressed. A user and an admin property are set with a value, so nothing can be
 * set until one is entered. The access of a Manufacturer property is always set.
 *
 * @param canWrite       Whether the value can be changed, see [canWriteValue].
 * @param hasTypedFields Whether the value is entered as typed values, and not as hexadecimal bytes.
 * @param typed          The characteristic created from the typed values, or `null` if they are
 *                       not valid.
 * @param bytes          The bytes entered in hexadecimal, or `null` if they are not valid.
 */
internal fun isSetAllowed(
    kind: PropertyServerKind,
    canWrite: Boolean,
    hasTypedFields: Boolean,
    typed: DevicePropertyCharacteristic?,
    bytes: ByteArray?,
): Boolean = when {
    // A read only property cannot be given a value, so only its access may be set.
    !canWrite -> kind != PropertyServerKind.USER
    hasTypedFields -> typed != null
    kind == PropertyServerKind.MANUFACTURER -> true
    else -> bytes != null && bytes.isNotEmpty()
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

internal fun Any?.isUnacknowledgedPropertySet() =
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
