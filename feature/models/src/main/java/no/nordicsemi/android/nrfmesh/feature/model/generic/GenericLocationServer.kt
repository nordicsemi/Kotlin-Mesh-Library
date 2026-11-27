package no.nordicsemi.android.nrfmesh.feature.model.generic

import androidx.compose.runtime.Composable
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.model.Model

/**
 * Displays the global and local location of the Generic Location Server model. The locations are
 * read using the refresh buttons.
 */
@Composable
internal fun GenericLocationServer(
    model: Model,
    messageState: MessageState,
    sendApplicationMessage: (Model, MeshMessage) -> Unit,
) {
    val state = rememberLocationState(messageState = messageState)
    LocationStatus(
        model = model,
        state = state,
        messageState = messageState,
        sendApplicationMessage = sendApplicationMessage
    )
}
