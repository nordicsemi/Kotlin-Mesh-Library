package no.nordicsemi.android.nrfmesh.feature.model.sensor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.sensor.SensorGet
import no.nordicsemi.kotlin.mesh.core.messages.sensor.SensorStatus
import no.nordicsemi.kotlin.mesh.core.model.Model


@Composable
internal fun SensorServer(
    model: Model,
    messageState: MessageState,
    sendApplicationMessage: (Model, MeshMessage) -> Unit
) {
    val sensorValues = remember(messageState) {
        (messageState.response as? SensorStatus)?.values.orEmpty()
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
            title = stringResource(R.string.label_sensor_information)
        )
        MeshIconButton(
            onClick = dropUnlessResumed { sendApplicationMessage(model, SensorGet()) },
            buttonIcon = Icons.Outlined.Refresh,
            enabled = !messageState.isInProgress(),
            isOnClickActionInProgress = messageState.isInProgress() &&
                    (messageState.message is SensorGet)
        )
    }
    when (sensorValues.isEmpty()) {
        true ->
            ElevatedCardItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                imageVector = Icons.Outlined.Sensors,
                title = "No sensor information available"
            )

        false -> sensorValues.forEach {
            ElevatedCardItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                imageVector = Icons.Outlined.Sensors,
                title = it.property?.propertyName ?: stringResource(R.string.label_unknown_sensor),
                subtitle = it.value.description
            )
        }
    }
}