package no.nordicsemi.android.nrfmesh.feature.model.generic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeStatus
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime

/**
 * Controls of the Generic Default Transition Time Server model. The state is read when the screen
 * is opened.
 */
@Composable
internal fun GenericDefaultTransitionTimeServer(
    model: Model,
    messageState: MessageState,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    val hasBoundKey = model.boundApplicationKeys.isNotEmpty()
    LaunchedEffect(key1 = Unit) {
        if (hasBoundKey) sendApplicationMessage(model, GenericDefaultTransitionTimeGet())
    }
    Controls(
        model = model,
        enabled = hasBoundKey && !messageState.isInProgress(),
        sendApplicationMessage = sendApplicationMessage
    )
    Status(
        model = model,
        hasBoundKey = hasBoundKey,
        messageState = messageState,
        sendApplicationMessage = sendApplicationMessage
    )
}

@Composable
private fun Controls(
    model: Model,
    enabled: Boolean,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    var sliderValue by rememberSaveable { mutableFloatStateOf(0f) }
    var acknowledged by rememberSaveable { mutableStateOf(true) }
    val (transitionTime, label) = transitionTimeOf(value = sliderValue)
    SectionTitle(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = stringResource(R.string.label_controls)
    )
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Timer,
        title = stringResource(R.string.label_transition_time),
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                Slider(
                    modifier = Modifier.fillMaxWidth(),
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..235f,
                    steps = 236,
                    enabled = enabled,
                    colors = NordicSliderDefaults.colors()
                )
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = label,
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
            }
        },
        actions = {
            OutlinedButton(
                onClick = {
                    sendApplicationMessage(
                        model,
                        defaultTransitionTimeSetOf(
                            transitionTime = transitionTime,
                            acknowledged = acknowledged
                        )
                    )
                },
                enabled = enabled,
                content = { Text(text = stringResource(R.string.label_set)) }
            )
        }
    )
}

@Composable
private fun Status(
    model: Model,
    hasBoundKey: Boolean,
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
            onClick = { sendApplicationMessage(model, GenericDefaultTransitionTimeGet()) },
            buttonIcon = Icons.Outlined.Autorenew,
            enabled = hasBoundKey && !messageState.isInProgress(),
            isOnClickActionInProgress = messageState.isInProgress() &&
                    messageState.message is GenericDefaultTransitionTimeGet,
        )
    }
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Timer,
        title = stringResource(R.string.label_current),
        titleAction = {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = (messageState.response as? GenericDefaultTransitionTimeStatus)
                    ?.transitionTime?.toString()
                    ?: stringResource(R.string.label_unknown)
            )
        }
    )
}

/**
 * Converts the slider value to a [TransitionTime] and its label.
 */
internal fun transitionTimeOf(value: Float): Pair<TransitionTime, String> {
    var result = TransitionTime(rawValue = 0u) to ""
    handleTransitionTimeChange(value = value) { transitionTime, label ->
        result = transitionTime to label
    }
    return result
}

/**
 * Creates the message used to set the Default Transition Time.
 */
internal fun defaultTransitionTimeSetOf(
    transitionTime: TransitionTime,
    acknowledged: Boolean,
): MeshMessage = if (acknowledged) {
    GenericDefaultTransitionTimeSet(transitionTime = transitionTime)
} else {
    GenericDefaultTransitionTimeSetUnacknowledged(transitionTime = transitionTime)
}
