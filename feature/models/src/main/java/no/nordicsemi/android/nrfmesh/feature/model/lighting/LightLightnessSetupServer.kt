package no.nordicsemi.android.nrfmesh.feature.model.lighting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.common.ui.view.NordicSliderDefaults
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.Failed
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshSingleLineListItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.model.utils.toLightness
import no.nordicsemi.android.nrfmesh.feature.model.utils.toLightnessPercent
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessDefaultGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessDefaultSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessDefaultSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessDefaultStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessRangeGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessRangeSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessRangeSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessRangeStatus
import no.nordicsemi.kotlin.mesh.core.model.Model

/** The states read from the Light Lightness Setup Server, in the order they are read. */
private val readSteps: List<() -> MeshMessage> = listOf(
    { LightLightnessDefaultGet() },
    { LightLightnessRangeGet() },
)

/**
 * Displays the Default and Range states of the Light Lightness Setup Server model.
 *
 * Both states can be read and set. They are read one after another when the screen is opened and
 * the model has at least one bound application key. Each section has its own refresh button. A failed
 * Range Set is reported using the status code received from the node.
 *
 * @param onReadingAll Called with true while all states are being read, so that the controls of the
 *                     other sections of the screen stay disabled until the whole batch is done.
 */
@Composable
internal fun LightLightnessSetupServer(
    model: Model,
    messageState: MessageState,
    onReadingAll: (Boolean) -> Unit,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    val hasBoundKey = model.boundApplicationKeys.isNotEmpty()
    var defaultStatus by remember { mutableStateOf<LightLightnessDefaultStatus?>(null) }
    var rangeStatus by remember { mutableStateOf<LightLightnessRangeStatus?>(null) }
    // Index of the state being read while all states are being read, -1 otherwise.
    var readingIndex by rememberSaveable { mutableIntStateOf(if (hasBoundKey) 0 else -1) }
    val isReadingAll = readingIndex >= 0
    val enabled = hasBoundKey && !isReadingAll && !messageState.isInProgress()

    LaunchedEffect(key1 = isReadingAll) {
        onReadingAll(isReadingAll)
    }
    DisposableEffect(key1 = Unit) {
        onDispose { onReadingAll(false) }
    }

    LaunchedEffect(key1 = Unit) {
        if (readingIndex in readSteps.indices) {
            sendApplicationMessage(model, readSteps[readingIndex]())
        }
    }

    LaunchedEffect(key1 = messageState) {
        when (messageState) {
            is Completed -> {
                val response = messageState.response
                when (response) {
                    is LightLightnessDefaultStatus -> defaultStatus = response
                    is LightLightnessRangeStatus -> rangeStatus = response
                    else -> {}
                }
                val message = messageState.message
                when {
                    // Unacknowledged sets do not get a response, so the value is read back.
                    message is LightLightnessDefaultSetUnacknowledged ->
                        sendApplicationMessage(model, LightLightnessDefaultGet())

                    message is LightLightnessRangeSetUnacknowledged ->
                        sendApplicationMessage(model, LightLightnessRangeGet())

                    readingIndex in readSteps.indices && (
                            (readingIndex == 0 && response is LightLightnessDefaultStatus) ||
                                    (readingIndex == 1 && response is LightLightnessRangeStatus)
                            ) -> {
                        readingIndex++
                        if (readingIndex in readSteps.indices) {
                            sendApplicationMessage(model, readSteps[readingIndex]())
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

    DefaultControls(
        model = model,
        messageState = messageState,
        status = defaultStatus,
        enabled = enabled,
        isReadingAll = isReadingAll,
        sendApplicationMessage = sendApplicationMessage
    )
    RangeControls(
        model = model,
        messageState = messageState,
        status = rangeStatus,
        enabled = enabled,
        isReadingAll = isReadingAll,
        sendApplicationMessage = sendApplicationMessage
    )
}

@Composable
private fun SectionHeader(
    title: String,
    enabled: Boolean,
    isReadInProgress: Boolean,
    onRead: () -> Unit,
) {
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
            title = title
        )
        MeshIconButton(
            onClick = onRead,
            buttonIcon = Icons.Outlined.Autorenew,
            enabled = enabled,
            isOnClickActionInProgress = isReadInProgress,
        )
    }
}

@Composable
private fun AcknowledgedItem(
    enabled: Boolean,
    acknowledged: Boolean,
    onAcknowledgedChange: (Boolean) -> Unit,
) {
    MeshSingleLineListItem(
        modifier = Modifier.fillMaxSize(),
        imageVector = Icons.Outlined.Verified,
        title = stringResource(R.string.label_acknowledged),
        trailingComposable = {
            Switch(
                enabled = enabled,
                checked = acknowledged,
                onCheckedChange = onAcknowledgedChange,
            )
        }
    )
}

@Composable
private fun PercentSlider(
    label: String? = null,
    value: Float,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
) {
    Column {
        Slider(
            modifier = Modifier.fillMaxWidth(),
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            steps = 99,
            enabled = enabled,
            colors = NordicSliderDefaults.colors()
        )
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = listOfNotNull(
                label,
                stringResource(R.string.label_percent_value, value.toInt())
            ).joinToString(separator = ": "),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun DefaultControls(
    model: Model,
    messageState: MessageState,
    status: LightLightnessDefaultStatus?,
    enabled: Boolean,
    isReadingAll: Boolean,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    var percent by rememberSaveable { mutableFloatStateOf(0f) }
    var acknowledged by rememberSaveable { mutableStateOf(true) }
    // Shows the value reported by the node when it is read.
    LaunchedEffect(key1 = status) {
        status?.let { percent = it.lightness.toLightnessPercent().toFloat() }
    }
    SectionHeader(
        title = stringResource(R.string.label_light_lightness_default),
        enabled = enabled,
        isReadInProgress = !isReadingAll && messageState.isInProgress() &&
                messageState.message is LightLightnessDefaultGet,
        onRead = { sendApplicationMessage(model, LightLightnessDefaultGet()) }
    )
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Lightbulb,
        title = stringResource(R.string.label_current),
        titleAction = {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = status?.let {
                    stringResource(R.string.label_percent_value, it.lightness.toLightnessPercent())
                } ?: stringResource(R.string.label_unknown).uppercase()
            )
        },
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                AcknowledgedItem(
                    enabled = enabled,
                    acknowledged = acknowledged,
                    onAcknowledgedChange = { acknowledged = it }
                )
                PercentSlider(
                    value = percent,
                    enabled = enabled,
                    onValueChange = { percent = it }
                )
            }
        },
        actions = {
            OutlinedButton(
                onClick = {
                    val lightness = percent.toLightness()
                    sendApplicationMessage(
                        model,
                        if (acknowledged) LightLightnessDefaultSet(lightness = lightness)
                        else LightLightnessDefaultSetUnacknowledged(lightness = lightness)
                    )
                },
                enabled = enabled,
                content = { Text(text = stringResource(R.string.label_set)) }
            )
        }
    )
}

@Composable
private fun RangeControls(
    model: Model,
    messageState: MessageState,
    status: LightLightnessRangeStatus?,
    enabled: Boolean,
    isReadingAll: Boolean,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    var min by rememberSaveable { mutableFloatStateOf(0f) }
    var max by rememberSaveable { mutableFloatStateOf(100f) }
    var acknowledged by rememberSaveable { mutableStateOf(true) }
    // Shows the range reported by the node when it is read.
    LaunchedEffect(key1 = status) {
        status?.takeIf { it.isSuccess }?.let {
            min = it.min.toLightnessPercent().toFloat()
            max = it.max.toLightnessPercent().toFloat()
        }
    }
    SectionHeader(
        title = stringResource(R.string.label_light_lightness_range),
        enabled = enabled,
        isReadInProgress = !isReadingAll && messageState.isInProgress() &&
                messageState.message is LightLightnessRangeGet,
        onRead = { sendApplicationMessage(model, LightLightnessRangeGet()) }
    )
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Tune,
        title = stringResource(R.string.label_range),
        titleAction = {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = status?.let {
                    "${it.min.toLightnessPercent()}% - ${it.max.toLightnessPercent()}%"
                } ?: stringResource(R.string.label_unknown).uppercase()
            )
        },
        supportingText = status?.takeIf { !it.isSuccess }?.let {
            stringResource(R.string.label_range_status_error, it.message)
        },
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                AcknowledgedItem(
                    enabled = enabled,
                    acknowledged = acknowledged,
                    onAcknowledgedChange = { acknowledged = it }
                )
                PercentSlider(
                    label = stringResource(R.string.label_range_min),
                    value = min,
                    enabled = enabled,
                    onValueChange = {
                        min = it
                        if (max < it) max = it
                    }
                )
                PercentSlider(
                    label = stringResource(R.string.label_range_max),
                    value = max,
                    enabled = enabled,
                    onValueChange = {
                        max = it
                        if (min > it) min = it
                    }
                )
            }
        },
        actions = {
            OutlinedButton(
                onClick = {
                    val minLightness = min.toLightness()
                    val maxLightness = max.toLightness()
                    sendApplicationMessage(
                        model,
                        if (acknowledged) LightLightnessRangeSet(
                            min = minLightness,
                            max = maxLightness
                        ) else LightLightnessRangeSetUnacknowledged(
                            min = minLightness,
                            max = maxLightness
                        )
                    )
                },
                enabled = enabled,
                content = { Text(text = stringResource(R.string.label_set)) }
            )
        }
    )
}
