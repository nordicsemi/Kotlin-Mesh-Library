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
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Radar
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.common.ui.view.NordicSliderDefaults
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshSingleLineListItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.model.utils.handleTransitionTimeChange
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnOffGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnOffSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnOffSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnOffStatus
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.StepResolution
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters


@Composable
internal fun GenericOnOffServer(
    model: Model,
    messageState: MessageState,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    Controls(
        model = model,
        messageState = messageState,
        sendApplicationMessage = sendApplicationMessage
    )
    Status(
        model = model,
        messageState = messageState,
        sendApplicationMessage = sendApplicationMessage
    )
}

@Composable
private fun Controls(
    model: Model,
    messageState: MessageState,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    val resources = LocalResources.current
    var defaultTransitionEnabled by rememberSaveable { mutableStateOf(true) }
    var transitionTime by remember {
        mutableStateOf(
            TransitionTime(steps = 0u, stepResolution = StepResolution.HUNDREDS_OF_MILLISECONDS)
        )
    }
    var transitionTimeLabel by remember {
        mutableStateOf(
            if (defaultTransitionEnabled) {
                resources.getString(R.string.label_default)
            } else {
                transitionTime.toString()
            }
        )
    }
    var delay by remember { mutableIntStateOf(0) }
    var delayLabel by remember {
        mutableStateOf(
            if (defaultTransitionEnabled) {
                resources.getString(R.string.label_no_delay)
            } else {
                transitionTime.toString()
            }
        )
    }
    var acknowledged by rememberSaveable { mutableStateOf(false) }
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
                            resources.getString(R.string.label_no_delay)
                        } else {
                            resources.getString(R.string.delay_ms, delay.toInt() * 5)
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
                    toggle(
                        model = model,
                        defaultTransitionEnabled = defaultTransitionEnabled,
                        acknowledged = acknowledged,
                        transitionTime = transitionTime,
                        delay = delay.toUByte(),
                        on = false,
                        sendApplicationMessage = sendApplicationMessage
                    )
                },
                enabled = !messageState.isInProgress(),
                content = { Text(text = stringResource(R.string.label_off)) }
            )
            Spacer(modifier = Modifier.size(16.dp))
            OutlinedButton(
                onClick = {
                    toggle(
                        model = model,
                        defaultTransitionEnabled = defaultTransitionEnabled,
                        acknowledged = acknowledged,
                        transitionTime = transitionTime,
                        delay = delay.toUByte(),
                        on = true,
                        sendApplicationMessage = sendApplicationMessage
                    )
                },
                enabled = !messageState.isInProgress(),
                content = { Text(text = stringResource(R.string.label_on)) }
            )
        }
    )
}

@Composable
private fun Status(
    model: Model,
    messageState: MessageState,
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
            onClick = { sendApplicationMessage(model, GenericOnOffGet()) },
            buttonIcon = Icons.Outlined.Autorenew,
            enabled = !messageState.isInProgress(),
            isOnClickActionInProgress = messageState.isInProgress() &&
                    messageState.message is GenericOnOffGet,
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
                    id = (messageState.response as? GenericOnOffStatus)
                        ?.let { if (it.isOn) R.string.label_on else R.string.label_off }
                        ?: run { R.string.label_unknown }
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
                text = (messageState.response as? GenericOnOffStatus)
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

private fun toggle(
    model: Model,
    defaultTransitionEnabled: Boolean,
    acknowledged: Boolean,
    transitionTime: TransitionTime,
    delay: UByte,
    on: Boolean,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    sendApplicationMessage(
        model,
        when {
            acknowledged -> GenericOnOffSet(
                tid = null,
                on = on,
                transitionParams = if (!defaultTransitionEnabled) {
                    TransitionParameters(transitionTime = transitionTime, delay = delay)
                } else {
                    null
                }
            )

            else -> GenericOnOffSetUnacknowledged(
                tid = null,
                on = on,
                transitionParams = if (!defaultTransitionEnabled) {
                    TransitionParameters(transitionTime = transitionTime, delay = delay)
                } else {
                    null
                }
            )
        }
    )
}
