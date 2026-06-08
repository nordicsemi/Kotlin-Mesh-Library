package no.nordicsemi.android.nrfmesh.feature.model.lepairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.Utils.describe
import no.nordicsemi.android.nrfmesh.core.data.model.vendor.lepairing.message.PairingRequest
import no.nordicsemi.android.nrfmesh.core.data.model.vendor.lepairing.message.PairingResponse
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshMessageStatusDialog
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.models.R
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.model.Model

@Composable
internal fun LePairingResponder(
    model: Model,
    isInProgress: Boolean,
    send: suspend (Model, AcknowledgedMeshMessage) -> MeshMessage?,
    startPairing: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<PairingResponse?>(null) }
    var error by rememberSaveable { mutableStateOf<Throwable?>(null) }
    var shouldShowProgressIcon by rememberSaveable { mutableStateOf(false) }
    var passKey by rememberSaveable { mutableStateOf<Int?>(null) }
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(
            modifier = Modifier.weight(weight = 1f),
            title = stringResource(R.string.label_controls)
        )
        MeshIconButton(
            buttonIcon = Icons.Outlined.Refresh,
            onClick = dropUnlessResumed {
                scope.launch {
                    shouldShowProgressIcon = true
                    try {
                        status = send(
                            model,
                            PairingRequest()
                        ) as PairingResponse?
                        passKey = status?.passKey
                        status
                            ?.takeIf { it.status == 0.toUByte() }
                            ?.let {
                                passKey = it.passKey
                                startPairing()
                            }
                    } catch (e: Exception) {
                        error = e
                    } finally {
                        shouldShowProgressIcon = false
                    }
                }
            },
            enabled = !isInProgress,
            isOnClickActionInProgress = shouldShowProgressIcon
        )
    }
    ElevatedCardItem(
        modifier = Modifier.padding(horizontal = 16.dp),
        imageVector = Icons.Outlined.Password,
        title = stringResource(R.string.label_pass_key),
        enabled = !isInProgress
    )

    error?.let {
        MeshMessageStatusDialog(
            text = it.describe(),
            showDismissButton = true,
            onDismissRequest = { error = null },
        )
    }
}