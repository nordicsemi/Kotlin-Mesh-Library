package no.nordicsemi.android.nrfmesh.feature.model.lighting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.LocalContext
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
import no.nordicsemi.android.nrfmesh.feature.model.utils.handleTransitionTimeChange
import no.nordicsemi.android.nrfmesh.feature.model.utils.toLightness
import no.nordicsemi.android.nrfmesh.feature.model.utils.toLightnessPercent
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessLastGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessLastStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessLinearGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessLinearSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessLinearSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessLinearStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessStatus
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.StepResolution
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters

/** The states read from the Light Lightness Server, in the order they are read. */
private val readSteps: List<() -> MeshMessage> = listOf(
    { LightLightnessGet() },
    { LightLightnessLinearGet() },
    { LightLightnessLastGet() },
)

/**
 * Displays the controls and the states of the Light Lightness Server model.
 *
 * The Light Lightness or the Light Lightness Linear state can be set using a percentage slider. The
 * Light Lightness, Light Lightness Linear and Light Lightness Last states are read one after
 * another when the screen is opened and the model has at least one bound application key. The
 * refresh button reads them again.
 *
 * @param onReadingAll Called with true while all states are being read, so that the controls of the
 *                     other sections of the screen stay disabled until the whole batch is done.
 */
@Composable
internal fun LightLightnessServer(
    model: Model,
    messageState: MessageState,
    onReadingAll: (Boolean) -> Unit,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    val hasBoundKey = model.boundApplicationKeys.isNotEmpty()
    var status by remember { mutableStateOf<LightLightnessStatus?>(null) }
    var linearStatus by remember { mutableStateOf<LightLightnessLinearStatus?>(null) }
    var lastStatus by remember { mutableStateOf<LightLightnessLastStatus?>(null) }
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
                    is LightLightnessStatus -> status = response
                    is LightLightnessLinearStatus -> linearStatus = response
                    is LightLightnessLastStatus -> lastStatus = response
                    else -> {}
                }
                val message = messageState.message
                when {
                    // Unacknowledged sets do not get a response, so the value is read back.
                    message is LightLightnessSetUnacknowledged ->
                        sendApplicationMessage(model, LightLightnessGet())

                    message is LightLightnessLinearSetUnacknowledged ->
                        sendApplicationMessage(model, LightLightnessLinearGet())

                    readingIndex in readSteps.indices &&
                            response != null && response.opCode == expectedOpCode(readingIndex) -> {
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

    Controls(
        model = model,
        messageState = messageState,
        enabled = hasBoundKey && !isReadingAll && !messageState.isInProgress(),
        sendApplicationMessage = sendApplicationMessage
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
                readingIndex = 0
                sendApplicationMessage(model, readSteps.first()())
            },
            buttonIcon = Icons.Outlined.Autorenew,
            enabled = enabled,
            isOnClickActionInProgress = isReadingAll,
        )
    }
    StateCard(
        title = stringResource(R.string.label_lightness),
        present = status?.lightness,
        target = status?.targetLightness,
        remainingTime = status?.remainingTime?.toString(),
    )
    StateCard(
        title = stringResource(R.string.label_linear),
        present = linearStatus?.lightness,
        target = linearStatus?.targetLightness,
        remainingTime = linearStatus?.remainingTime?.toString(),
    )
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.History,
        title = stringResource(R.string.label_last),
        titleAction = {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = lastStatus?.let {
                    stringResource(R.string.label_percent_value, it.lightness.toLightnessPercent())
                } ?: stringResource(R.string.label_unknown).uppercase()
            )
        }
    )
}

/**
 * Displays the present and target values of a lightness state.
 */
@Composable
private fun StateCard(
    title: String,
    present: UShort?,
    target: UShort?,
    remainingTime: String?,
) {
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Lightbulb,
        title = title,
        titleAction = {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = present?.let {
                    stringResource(R.string.label_percent_value, it.toLightnessPercent())
                } ?: stringResource(R.string.label_unknown).uppercase()
            )
        },
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                MeshSingleLineListItem(
                    modifier = Modifier.fillMaxSize(),
                    imageVector = Icons.Outlined.Radar,
                    title = stringResource(R.string.label_target),
                    trailingComposable = {
                        Text(
                            text = when {
                                present == null -> stringResource(R.string.label_unknown).uppercase()
                                target == null -> stringResource(R.string.label_na)
                                remainingTime != null -> stringResource(
                                    R.string.label_percent_value_with_remaining_time,
                                    target.toLightnessPercent(),
                                    remainingTime
                                )

                                else -> stringResource(
                                    R.string.label_percent_value,
                                    target.toLightnessPercent()
                                )
                            }
                        )
                    }
                )
            }
        }
    )
}

@Composable
private fun Controls(
    model: Model,
    messageState: MessageState,
    enabled: Boolean,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    val context = LocalContext.current
    var defaultTransitionEnabled by rememberSaveable { mutableStateOf(true) }
    var transitionTime by remember {
        mutableStateOf(
            TransitionTime(steps = 0u, stepResolution = StepResolution.HUNDREDS_OF_MILLISECONDS)
        )
    }
    var transitionTimeLabel by remember {
        mutableStateOf(context.getString(R.string.label_default))
    }
    var delay by remember { mutableIntStateOf(0) }
    var delayLabel by remember {
        mutableStateOf(context.getString(R.string.label_no_delay))
    }
    var percent by rememberSaveable { mutableFloatStateOf(0f) }
    var acknowledged by rememberSaveable { mutableStateOf(false) }
    var linear by rememberSaveable { mutableStateOf(false) }
    SectionTitle(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = stringResource(R.string.label_controls)
    )
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Timer,
        title = stringResource(R.string.label_default_transition_delay),
        titleAction = {
            Switch(
                modifier = Modifier.padding(horizontal = 16.dp),
                enabled = enabled,
                checked = defaultTransitionEnabled,
                onCheckedChange = { defaultTransitionEnabled = it },
            )
        },
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                Slider(
                    modifier = Modifier.fillMaxWidth(),
                    value = transitionTime.rawValue.toFloat(),
                    onValueChange = {
                        handleTransitionTimeChange(
                            value = it,
                            onTransitionTimeChanged = { newTransitionTime, label ->
                                transitionTime = newTransitionTime
                                transitionTimeLabel = label
                            }
                        )
                    },
                    valueRange = 0f..235f,
                    steps = 236,
                    enabled = !defaultTransitionEnabled,
                    colors = NordicSliderDefaults.colors()
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = transitionTimeLabel,
                    textAlign = TextAlign.End
                )
                Slider(
                    modifier = Modifier.fillMaxWidth(),
                    value = delay.toFloat(),
                    onValueChange = {
                        delay = it.toInt()
                        delayLabel = if (delay == 0) {
                            context.getString(R.string.label_no_delay)
                        } else {
                            context.getString(R.string.delay_ms, delay * 5)
                        }
                    },
                    valueRange = 0f..255f,
                    steps = 256,
                    enabled = !defaultTransitionEnabled,
                    colors = NordicSliderDefaults.colors()
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = delayLabel,
                    textAlign = TextAlign.End
                )
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
                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                Slider(
                    modifier = Modifier.fillMaxWidth(),
                    value = percent,
                    onValueChange = { percent = it },
                    valueRange = 0f..100f,
                    steps = 99,
                    enabled = enabled,
                    colors = NordicSliderDefaults.colors()
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(R.string.label_percent_value, percent.toInt()),
                    textAlign = TextAlign.End
                )
            }
        },
        actions = {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.padding(horizontal = 16.dp)) {
                listOf(false, true).forEachIndexed { index, isLinear ->
                    SegmentedButton(
                        modifier = Modifier.defaultMinSize(minWidth = 60.dp),
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = 2),
                        onClick = { linear = isLinear },
                        selected = linear == isLinear,
                        label = {
                            Text(
                                text = stringResource(
                                    if (isLinear) R.string.label_linear else R.string.label_lightness
                                )
                            )
                        }
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    sendLightness(
                        model = model,
                        lightness = percent.toLightness(),
                        linear = linear,
                        acknowledged = acknowledged,
                        transitionParams = if (!defaultTransitionEnabled) {
                            TransitionParameters(
                                transitionTime = transitionTime,
                                delay = delay.toUByte()
                            )
                        } else {
                            null
                        },
                        sendApplicationMessage = sendApplicationMessage
                    )
                },
                enabled = enabled,
                content = { Text(text = stringResource(R.string.label_set)) }
            )
        }
    )
}

/** Returns the opcode of the status message expected in response to the read at the index. */
private fun expectedOpCode(index: Int) = when (index) {
    0 -> LightLightnessStatus.opCode
    1 -> LightLightnessLinearStatus.opCode
    else -> LightLightnessLastStatus.opCode
}

private fun sendLightness(
    model: Model,
    lightness: UShort,
    linear: Boolean,
    acknowledged: Boolean,
    transitionParams: TransitionParameters?,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    sendApplicationMessage(
        model,
        when {
            linear && acknowledged -> LightLightnessLinearSet(
                tid = null,
                lightness = lightness,
                transitionParams = transitionParams
            )

            linear -> LightLightnessLinearSetUnacknowledged(
                tid = null,
                lightness = lightness,
                transitionParams = transitionParams
            )

            acknowledged -> LightLightnessSet(
                tid = null,
                lightness = lightness,
                transitionParams = transitionParams
            )

            else -> LightLightnessSetUnacknowledged(
                tid = null,
                lightness = lightness,
                transitionParams = transitionParams
            )
        }
    )
}
