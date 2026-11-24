package no.nordicsemi.android.nrfmesh.feature.model.generic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshSingleLineListItem
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnPowerUpGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnPowerUpSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnPowerUpSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.OnPowerUp
import no.nordicsemi.kotlin.mesh.core.model.Model

/**
 * Controls of the Generic Power OnOff Setup Server model. The state is read when the screen is
 * opened.
 */
@Composable
internal fun GenericPowerOnOffSetupServer(
    model: Model,
    messageState: MessageState,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    val hasBoundKey = model.boundApplicationKeys.isNotEmpty()
    LaunchedEffect(key1 = Unit) {
        if (hasBoundKey) sendApplicationMessage(model, GenericOnPowerUpGet())
    }
    Controls(
        model = model,
        enabled = hasBoundKey && !messageState.isInProgress(),
        sendApplicationMessage = sendApplicationMessage
    )
    OnPowerUpStatus(
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
    val behaviors = OnPowerUp.entries
    var selected by rememberSaveable { mutableIntStateOf(OnPowerUp.DEFAULT.ordinal) }
    var acknowledged by rememberSaveable { mutableStateOf(true) }
    SectionTitle(
        modifier = Modifier.padding(horizontal = 16.dp),
        title = stringResource(R.string.label_controls)
    )
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.PowerSettingsNew,
        title = stringResource(R.string.label_on_power_up_behavior),
        body = {
            Column(verticalArrangement = Arrangement.spacedBy(space = 8.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    behaviors.forEachIndexed { index, behavior ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = behaviors.size
                            ),
                            onClick = { selected = index },
                            selected = index == selected,
                            enabled = enabled,
                            label = { Text(text = stringResource(behavior.labelRes())) }
                        )
                    }
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
            OutlinedButton(
                onClick = {
                    sendApplicationMessage(
                        model,
                        onPowerUpSetOf(state = behaviors[selected], acknowledged = acknowledged)
                    )
                },
                enabled = enabled,
                content = { Text(text = stringResource(R.string.label_send)) }
            )
        }
    )
}

/**
 * Creates the message used to set the Generic OnPowerUp state.
 */
internal fun onPowerUpSetOf(state: OnPowerUp, acknowledged: Boolean): MeshMessage =
    if (acknowledged) GenericOnPowerUpSet(state = state)
    else GenericOnPowerUpSetUnacknowledged(state = state)
