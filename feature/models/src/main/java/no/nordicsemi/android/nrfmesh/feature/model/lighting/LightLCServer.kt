package no.nordicsemi.android.nrfmesh.feature.model.lighting

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
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.SensorOccupied
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCLightOnOffGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCLightOnOffSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCLightOnOffSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCLightOnOffStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCModeGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCModeSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCModeSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCModeStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCOccupancyModeGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCOccupancyModeSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCOccupancyModeSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCOccupancyModeStatus
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.StepResolution
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters

/**
 * Displays the controls and states of the Light LC Server model.
 *
 * It contains the Light LC Mode, Light LC Occupancy Mode and Light LC Light OnOff controls.
 * When the screen is opened and the model has at least one bound application key, the three
 * states are read one after another, similar to the iOS app.
 */
@Composable
internal fun LightLCServer(
    model: Model,
    messageState: MessageState,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    val hasBoundKey = model.boundApplicationKeys.isNotEmpty()
    var modeStatus by remember { mutableStateOf<LightLCModeStatus?>(null) }
    var occupancyModeStatus by remember { mutableStateOf<LightLCOccupancyModeStatus?>(null) }
    var lightOnOffStatus by remember { mutableStateOf<LightLCLightOnOffStatus?>(null) }
    // True while the initial state is being read after the screen is opened.
    var isRefreshing by rememberSaveable { mutableStateOf(hasBoundKey) }

    LaunchedEffect(key1 = Unit) {
        if (isRefreshing) {
            sendApplicationMessage(model, LightLCModeGet())
        }
    }

    LaunchedEffect(key1 = messageState) {
        when (messageState) {
            is Completed -> when (val response = messageState.response) {
                is LightLCModeStatus -> {
                    modeStatus = response
                    if (isRefreshing) sendApplicationMessage(model, LightLCOccupancyModeGet())
                }

                is LightLCOccupancyModeStatus -> {
                    occupancyModeStatus = response
                    if (isRefreshing) sendApplicationMessage(model, LightLCLightOnOffGet())
                }

                is LightLCLightOnOffStatus -> {
                    lightOnOffStatus = response
                    isRefreshing = false
                }

                else -> {}
            }

            is Failed -> isRefreshing = false
            else -> {}
        }
    }

    ModeControls(
        model = model,
        messageState = messageState,
        status = modeStatus,
        hasBoundKey = hasBoundKey,
        sendApplicationMessage = sendApplicationMessage
    )
    OccupancyModeControls(
        model = model,
        messageState = messageState,
        status = occupancyModeStatus,
        hasBoundKey = hasBoundKey,
        sendApplicationMessage = sendApplicationMessage
    )
    LightOnOffControls(
        model = model,
        messageState = messageState,
        hasBoundKey = hasBoundKey,
        sendApplicationMessage = sendApplicationMessage
    )
    LightOnOffStatus(
        model = model,
        messageState = messageState,
        status = lightOnOffStatus,
        hasBoundKey = hasBoundKey,
        sendApplicationMessage = sendApplicationMessage
    )
}

@Composable
private fun ModeControls(
    model: Model,
    messageState: MessageState,
    status: LightLCModeStatus?,
    hasBoundKey: Boolean,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    var acknowledged by rememberSaveable { mutableStateOf(false) }
    StateSection(
        title = stringResource(R.string.label_light_lc_mode),
        icon = Icons.Outlined.Tune,
        status = status?.let { it.controllerStatus.asOnOff() },
        acknowledged = acknowledged,
        onAcknowledgedChange = { acknowledged = it },
        enabled = hasBoundKey && !messageState.isInProgress(),
        isReadInProgress = messageState.isInProgress() && messageState.message is LightLCModeGet,
        onRead = { sendApplicationMessage(model, LightLCModeGet()) },
        onSet = { on ->
            sendApplicationMessage(
                model,
                if (acknowledged) LightLCModeSet(controllerStatus = on)
                else LightLCModeSetUnacknowledged(controllerStatus = on)
            )
        }
    )
}

@Composable
private fun OccupancyModeControls(
    model: Model,
    messageState: MessageState,
    status: LightLCOccupancyModeStatus?,
    hasBoundKey: Boolean,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    var acknowledged by rememberSaveable { mutableStateOf(false) }
    StateSection(
        title = stringResource(R.string.label_light_lc_occupancy_mode),
        icon = Icons.Outlined.SensorOccupied,
        status = status?.let { it.occupancyMode.asOnOff() },
        acknowledged = acknowledged,
        onAcknowledgedChange = { acknowledged = it },
        enabled = hasBoundKey && !messageState.isInProgress(),
        isReadInProgress = messageState.isInProgress() &&
                messageState.message is LightLCOccupancyModeGet,
        onRead = { sendApplicationMessage(model, LightLCOccupancyModeGet()) },
        onSet = { on ->
            sendApplicationMessage(
                model,
                if (acknowledged) LightLCOccupancyModeSet(occupancyMode = on)
                else LightLCOccupancyModeSetUnacknowledged(occupancyMode = on)
            )
        }
    )
}

/**
 * Displays a titled section with a read button and a card for a boolean state of the model.
 *
 * The card contains the acknowledged switch, the On/Off buttons and the state received from the
 * node.
 */
@Composable
private fun StateSection(
    title: String,
    icon: ImageVector,
    status: String?,
    acknowledged: Boolean,
    onAcknowledgedChange: (Boolean) -> Unit,
    enabled: Boolean,
    isReadInProgress: Boolean,
    onRead: () -> Unit,
    onSet: (Boolean) -> Unit,
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
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = icon,
        title = stringResource(R.string.label_current),
        titleAction = {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = (status ?: stringResource(R.string.label_unknown)).uppercase()
            )
        },
        body = {
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
        },
        actions = {
            OutlinedButton(
                onClick = { onSet(false) },
                enabled = enabled,
                content = { Text(text = stringResource(R.string.label_off)) }
            )
            Spacer(modifier = Modifier.size(16.dp))
            OutlinedButton(
                onClick = { onSet(true) },
                enabled = enabled,
                content = { Text(text = stringResource(R.string.label_on)) }
            )
        }
    )
}

@Composable
private fun LightOnOffControls(
    model: Model,
    messageState: MessageState,
    hasBoundKey: Boolean,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    val context = LocalContext.current
    val enabled = hasBoundKey && !messageState.isInProgress()
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
    var acknowledged by rememberSaveable { mutableStateOf(false) }
    SectionTitle(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = stringResource(R.string.label_light_lc_light_on_off)
    )
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Timer,
        title = stringResource(R.string.label_default_transition_delay),
        titleAction = {
            Switch(
                modifier = Modifier.padding(horizontal = 16.dp),
                enabled = !messageState.isInProgress(),
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
                            enabled = !messageState.isInProgress(),
                            checked = acknowledged,
                            onCheckedChange = { acknowledged = it },
                        )
                    }
                )
            }
        },
        actions = {
            OutlinedButton(
                onClick = {
                    sendLightOnOff(
                        model = model,
                        defaultTransitionEnabled = defaultTransitionEnabled,
                        acknowledged = acknowledged,
                        transitionTime = transitionTime,
                        delay = delay.toUByte(),
                        on = false,
                        sendApplicationMessage = sendApplicationMessage
                    )
                },
                enabled = enabled,
                content = { Text(text = stringResource(R.string.label_off)) }
            )
            Spacer(modifier = Modifier.size(16.dp))
            OutlinedButton(
                onClick = {
                    sendLightOnOff(
                        model = model,
                        defaultTransitionEnabled = defaultTransitionEnabled,
                        acknowledged = acknowledged,
                        transitionTime = transitionTime,
                        delay = delay.toUByte(),
                        on = true,
                        sendApplicationMessage = sendApplicationMessage
                    )
                },
                enabled = enabled,
                content = { Text(text = stringResource(R.string.label_on)) }
            )
        }
    )
}

@Composable
private fun LightOnOffStatus(
    model: Model,
    messageState: MessageState,
    status: LightLCLightOnOffStatus?,
    hasBoundKey: Boolean,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
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
            title = stringResource(R.string.label_status)
        )
        MeshIconButton(
            onClick = { sendApplicationMessage(model, LightLCLightOnOffGet()) },
            buttonIcon = Icons.Outlined.Autorenew,
            enabled = hasBoundKey && !messageState.isInProgress(),
            isOnClickActionInProgress = messageState.isInProgress() &&
                    messageState.message is LightLCLightOnOffGet,
        )
    }
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Lightbulb,
        title = stringResource(R.string.label_current),
        titleAction = {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(
                    id = status
                        ?.let { if (it.isOn) R.string.label_on else R.string.label_off }
                        ?: R.string.label_unknown
                ).uppercase()
            )
        }
    )
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Radar,
        title = stringResource(R.string.label_target),
        titleAction = {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = status
                    ?.let {
                        when (it.targetState) {
                            true if it.remainingTime != null -> stringResource(
                                R.string.label_on_with_remaining_time,
                                it.remainingTime.toString()
                            )

                            false if it.remainingTime != null -> stringResource(
                                R.string.label_off_with_remaining_time,
                                it.remainingTime.toString()
                            )

                            else -> stringResource(R.string.label_na)
                        }
                    } ?: stringResource(R.string.label_unknown).uppercase(),
            )
        }
    )
}

@Composable
private fun Boolean.asOnOff() = stringResource(if (this) R.string.label_on else R.string.label_off)

private fun sendLightOnOff(
    model: Model,
    defaultTransitionEnabled: Boolean,
    acknowledged: Boolean,
    transitionTime: TransitionTime,
    delay: UByte,
    on: Boolean,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    val transitionParams = if (!defaultTransitionEnabled) {
        TransitionParameters(transitionTime = transitionTime, delay = delay)
    } else {
        null
    }
    sendApplicationMessage(
        model,
        if (acknowledged) {
            LightLCLightOnOffSet(tid = null, on = on, transitionParams = transitionParams)
        } else {
            LightLCLightOnOffSetUnacknowledged(
                tid = null,
                on = on,
                transitionParams = transitionParams
            )
        }
    )
}
