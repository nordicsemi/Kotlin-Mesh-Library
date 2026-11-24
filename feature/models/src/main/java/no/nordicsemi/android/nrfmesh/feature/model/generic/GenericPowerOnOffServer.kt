package no.nordicsemi.android.nrfmesh.feature.model.generic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnPowerUpGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnPowerUpStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.OnPowerUp
import no.nordicsemi.kotlin.mesh.core.model.Model

/**
 * Status of the Generic Power OnOff Server model. The state is read when the screen is opened.
 */
@Composable
internal fun GenericPowerOnOffServer(
    model: Model,
    messageState: MessageState,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    val hasBoundKey = model.boundApplicationKeys.isNotEmpty()
    LaunchedEffect(key1 = Unit) {
        if (hasBoundKey) sendApplicationMessage(model, GenericOnPowerUpGet())
    }
    OnPowerUpStatus(
        model = model,
        hasBoundKey = hasBoundKey,
        messageState = messageState,
        sendApplicationMessage = sendApplicationMessage
    )
}

/**
 * Shows the Generic OnPowerUp state with a button to read it.
 */
@Composable
internal fun OnPowerUpStatus(
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
            onClick = { sendApplicationMessage(model, GenericOnPowerUpGet()) },
            buttonIcon = Icons.Outlined.Autorenew,
            enabled = hasBoundKey && !messageState.isInProgress(),
            isOnClickActionInProgress = messageState.isInProgress() &&
                    messageState.message is GenericOnPowerUpGet,
        )
    }
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.PowerSettingsNew,
        title = stringResource(R.string.label_on_power_up),
        titleAction = {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = (messageState.response as? GenericOnPowerUpStatus)
                    ?.let { stringResource(it.state.labelRes()) }
                    ?: stringResource(R.string.label_unknown)
            )
        }
    )
}

internal fun OnPowerUp.labelRes() = when (this) {
    OnPowerUp.OFF -> R.string.label_off
    OnPowerUp.DEFAULT -> R.string.label_default
    OnPowerUp.RESTORE -> R.string.label_restore
}
