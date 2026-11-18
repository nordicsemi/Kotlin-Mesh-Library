package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import no.nordicsemi.android.nrfmesh.core.common.copyToClipboard
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.models.R

@Composable
internal fun LePairing(
    isCurrentStep: Boolean,
    goToNext: () -> Unit,
    onPairedChanged: (Boolean) -> Unit,
) {
    val resources = LocalResources.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val viewModel = hiltViewModel<LePairingViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val state = uiState.state
    // The bond state of the phone is not used, as it may not be correct. Pairing, which sends the
    // pairing request, is only attempted while this is the current step of the firmware update.
    LaunchedEffect(isCurrentStep) { if (isCurrentStep) viewModel.pair() }
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionTitle(
            modifier = Modifier.weight(weight = 1f),
            title = stringResource(R.string.label_le_pairing)
        )
        MeshIconButton(
            buttonIcon = Icons.Outlined.Refresh,
            onClick = viewModel::pair,
            // A new request would change the pass key of the pairing that is in progress.
            enabled = !state.isInProgress,
            isOnClickActionInProgress = state.isInProgress
        )
    }
    ElevatedCardItem(
        imageVector = Icons.Outlined.Password,
        title = stringResource(R.string.label_pass_key),
        // Shown while the pairing is in progress.
        titleAction = {
            if (state.isInProgress) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(size = 24.dp)
                )
            }
        },
        subtitle = uiState.passKey?.toString() ?: stringResource(R.string.label_unknown)
    )
    // The pass key is copied so that it can be pasted in the pairing dialog of the system.
    LaunchedEffect(uiState.passKey) {
        uiState.passKey?.let {
            copyToClipboard(
                scope = scope,
                clipboard = clipboard,
                text = it.toString(),
                label = resources.getString(R.string.label_pass_key)
            )
        }
    }
    LaunchedEffect(state, isCurrentStep) {
        onPairedChanged(state is PairingState.Paired)
        // The step is only left from here, so a result from before this step was shown, or from a
        // page that is composed in the background, must not move the flow on.
        if (isCurrentStep && state is PairingState.Paired) goToNext()
    }
}
