package no.nordicsemi.android.nrfmesh.feature.model.lighting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.Failed
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshAlertDialog
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshSingleLineListItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.DeviceProperty
import no.nordicsemi.kotlin.mesh.core.messages.DevicePropertyCharacteristic
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCPropertyGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCPropertySet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCPropertySetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCPropertyStatus
import no.nordicsemi.kotlin.mesh.core.model.Model
import java.math.BigDecimal

/**
 * The Light LC Properties handled by the Light LC Setup Server.
 */
private val lightLCProperties = DeviceProperty.entries.filter {
    it.name.startsWith(prefix = "LIGHT_CONTROL_")
}

/**
 * Displays the Light LC Properties of the Light LC Setup Server model.
 *
 * Each property can be read and edited. When the screen is opened and the model has at least one
 * bound application key, all properties are read one after another. The Read all button reads
 * them again.
 */
@Composable
internal fun LightLCSetupServer(
    model: Model,
    messageState: MessageState,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    val hasBoundKey = model.boundApplicationKeys.isNotEmpty()
    val values = remember { mutableStateMapOf<DeviceProperty, DevicePropertyCharacteristic>() }
    // Index of the property being read while all properties are being read, -1 otherwise.
    var readingIndex by rememberSaveable { mutableStateOf(if (hasBoundKey) 0 else -1) }
    var editing by remember { mutableStateOf<DeviceProperty?>(null) }
    val isReadingAll = readingIndex >= 0
    val enabled = hasBoundKey && !isReadingAll && !messageState.isInProgress()

    LaunchedEffect(key1 = Unit) {
        if (readingIndex in lightLCProperties.indices) {
            sendApplicationMessage(model, LightLCPropertyGet(lightLCProperties[readingIndex]))
        }
    }

    LaunchedEffect(key1 = messageState) {
        when (messageState) {
            is Completed -> {
                val response = messageState.response as? LightLCPropertyStatus
                val property = response?.propertyId?.let { DeviceProperty.from(id = it) }
                if (response != null && property != null) {
                    values[property] = response.propertyValue
                }
                val message = messageState.message
                when {
                    // Unacknowledged sets do not get a response, so the value is read back.
                    message is LightLCPropertySetUnacknowledged ->
                        message.property?.let {
                            sendApplicationMessage(model, LightLCPropertyGet(it))
                        }

                    readingIndex in lightLCProperties.indices &&
                            property == lightLCProperties[readingIndex] -> {
                        readingIndex++
                        if (readingIndex in lightLCProperties.indices) {
                            sendApplicationMessage(
                                model,
                                LightLCPropertyGet(lightLCProperties[readingIndex])
                            )
                        } else {
                            readingIndex = -1
                        }
                    }
                }
            }

            is Failed -> readingIndex = -1
            else -> {}
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
            title = stringResource(R.string.label_light_lc_properties)
        )
        MeshIconButton(
            onClick = {
                readingIndex = 0
                sendApplicationMessage(model, LightLCPropertyGet(lightLCProperties.first()))
            },
            buttonIcon = Icons.Outlined.Autorenew,
            enabled = enabled,
            isOnClickActionInProgress = isReadingAll,
        )
    }
    lightLCProperties.forEach { property ->
        ElevatedCardItem(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            imageVector = Icons.Outlined.Tune,
            title = property.propertyName.removePrefix(prefix = "Light Control "),
            subtitle = values[property]?.description ?: stringResource(R.string.label_not_read),
            titleAction = {
                MeshIconButton(
                    onClick = {
                        sendApplicationMessage(model, LightLCPropertyGet(property))
                    },
                    buttonIcon = Icons.Outlined.Autorenew,
                    enabled = enabled,
                    isOnClickActionInProgress = !isReadingAll && messageState.isInProgress() &&
                            (messageState.message as? LightLCPropertyGet)?.propertyId == property.id
                )
                MeshIconButton(
                    onClick = { editing = property },
                    buttonIcon = Icons.Outlined.Edit,
                    enabled = enabled,
                )
            }
        )
    }

    editing?.let { property ->
        EditPropertyDialog(
            property = property,
            currentValue = values[property],
            onDismiss = { editing = null },
            onSet = { value, acknowledged ->
                editing = null
                sendApplicationMessage(
                    model,
                    if (acknowledged) LightLCPropertySet(property = property, value = value)
                    else LightLCPropertySetUnacknowledged(property = property, value = value)
                )
            }
        )
    }
}

@Composable
private fun EditPropertyDialog(
    property: DeviceProperty,
    currentValue: DevicePropertyCharacteristic?,
    onDismiss: () -> Unit,
    onSet: (DevicePropertyCharacteristic, Boolean) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(currentValue?.editableText().orEmpty()) }
    var acknowledged by rememberSaveable { mutableStateOf(true) }
    val parsed = parse(property = property, text = text)
    MeshAlertDialog(
        onDismissRequest = onDismiss,
        icon = Icons.Outlined.Edit,
        iconColor = MaterialTheme.colorScheme.primary,
        title = stringResource(R.string.label_edit_property,
            property.propertyName.removePrefix(prefix = "Light Control ")
        ),
        confirmButtonText = stringResource(R.string.label_set),
        onConfirmClick = { parsed?.let { onSet(it, acknowledged) } },
        error = parsed == null,
        onDismissClick = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text(text = stringResource(R.string.label_property_value)) },
                isError = parsed == null,
                supportingText = {
                    Text(
                        text = stringResource(
                            if (parsed == null) R.string.label_invalid_property_value
                            else property.rangeHint()
                        )
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            MeshSingleLineListItem(
                modifier = Modifier.fillMaxWidth(),
                imageVector = Icons.Outlined.Verified,
                title = stringResource(R.string.label_acknowledged),
                trailingComposable = {
                    Switch(checked = acknowledged, onCheckedChange = { acknowledged = it })
                }
            )
        }
    }
}

private fun DeviceProperty.rangeHint() = when (this) {
    DeviceProperty.LIGHT_CONTROL_LIGHTNESS_ON,
    DeviceProperty.LIGHT_CONTROL_LIGHTNESS_PROLONG,
    DeviceProperty.LIGHT_CONTROL_LIGHTNESS_STANDBY -> R.string.label_property_range_lightness

    DeviceProperty.LIGHT_CONTROL_AMBIENT_LUX_LEVEL_ON,
    DeviceProperty.LIGHT_CONTROL_AMBIENT_LUX_LEVEL_PROLONG,
    DeviceProperty.LIGHT_CONTROL_AMBIENT_LUX_LEVEL_STANDBY -> R.string.label_property_range_lux

    DeviceProperty.LIGHT_CONTROL_REGULATOR_ACCURACY -> R.string.label_property_range_percent

    DeviceProperty.LIGHT_CONTROL_REGULATOR_KID,
    DeviceProperty.LIGHT_CONTROL_REGULATOR_KIU,
    DeviceProperty.LIGHT_CONTROL_REGULATOR_KPD,
    DeviceProperty.LIGHT_CONTROL_REGULATOR_KPU -> R.string.label_property_range_coefficient

    else -> R.string.label_property_range_ms
}

/**
 * Returns the characteristic value as plain text, without the unit, that can be edited.
 */
private fun DevicePropertyCharacteristic.editableText() = when (this) {
    is DevicePropertyCharacteristic.PerceivedLightness -> "$value"
    is DevicePropertyCharacteristic.Illuminance -> value?.toPlainString()
    is DevicePropertyCharacteristic.Percentage8 -> value?.toPlainString()
    is DevicePropertyCharacteristic.TimeMillisecond24 -> value?.toString()
    is DevicePropertyCharacteristic.Coefficient -> "$value"
    else -> null
}

/**
 * Parses the text entered by the user to the characteristic of the given property.
 *
 * @return the characteristic or null if the text is not a valid value for the property.
 */
private fun parse(property: DeviceProperty, text: String): DevicePropertyCharacteristic? {
    val input = text.trim().replace(oldChar = ',', newChar = '.')
    return when (property) {
        DeviceProperty.LIGHT_CONTROL_LIGHTNESS_ON,
        DeviceProperty.LIGHT_CONTROL_LIGHTNESS_PROLONG,
        DeviceProperty.LIGHT_CONTROL_LIGHTNESS_STANDBY ->
            input.toUShortOrNull()?.let { DevicePropertyCharacteristic.PerceivedLightness(it) }

        DeviceProperty.LIGHT_CONTROL_AMBIENT_LUX_LEVEL_ON,
        DeviceProperty.LIGHT_CONTROL_AMBIENT_LUX_LEVEL_PROLONG,
        DeviceProperty.LIGHT_CONTROL_AMBIENT_LUX_LEVEL_STANDBY ->
            input.toBigDecimalOrNull()
                ?.takeIf { it >= BigDecimal.ZERO && it <= BigDecimal("167772.13") }
                ?.let { DevicePropertyCharacteristic.Illuminance(it) }

        DeviceProperty.LIGHT_CONTROL_REGULATOR_ACCURACY ->
            input.toBigDecimalOrNull()
                ?.takeIf { it >= BigDecimal.ZERO && it <= BigDecimal(100) }
                ?.let { DevicePropertyCharacteristic.Percentage8(it) }

        DeviceProperty.LIGHT_CONTROL_REGULATOR_KID,
        DeviceProperty.LIGHT_CONTROL_REGULATOR_KIU,
        DeviceProperty.LIGHT_CONTROL_REGULATOR_KPD,
        DeviceProperty.LIGHT_CONTROL_REGULATOR_KPU ->
            input.toFloatOrNull()
                ?.takeIf { it.isFinite() }
                ?.let { DevicePropertyCharacteristic.Coefficient(it) }

        else -> input.toUIntOrNull()
            ?.takeIf { it <= 0xFFFFFEu }
            ?.let { DevicePropertyCharacteristic.TimeMillisecond24(it) }
    }
}
