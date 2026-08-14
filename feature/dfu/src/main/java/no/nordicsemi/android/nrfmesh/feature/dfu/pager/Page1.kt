package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.Utils.describe
import no.nordicsemi.android.nrfmesh.core.common.copyToClipboard
import no.nordicsemi.android.nrfmesh.core.common.lePairingResponder
import no.nordicsemi.android.nrfmesh.core.data.model.vendor.lepairing.message.PairingRequest
import no.nordicsemi.android.nrfmesh.core.data.model.vendor.lepairing.message.PairingResponse
import no.nordicsemi.android.nrfmesh.core.ui.ElevatedCardItem
import no.nordicsemi.android.nrfmesh.core.ui.MeshIconButton
import no.nordicsemi.android.nrfmesh.core.ui.MeshMessageStatusDialog
import no.nordicsemi.android.nrfmesh.core.ui.SectionTitle
import no.nordicsemi.android.nrfmesh.feature.models.R

@Composable
internal fun Page1(goToNext: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val viewModel = hiltViewModel<Page1ViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var status by remember { mutableStateOf<PairingResponse?>(null) }
    var error by rememberSaveable { mutableStateOf<Throwable?>(null) }
    var shouldShowProgressIcon by rememberSaveable { mutableStateOf(false) }
    var passKey by rememberSaveable { mutableStateOf<Int?>(null) }
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
            onClick = dropUnlessResumed {
                scope.launch {
                    shouldShowProgressIcon = true
                    try {
                        status = viewModel.send(
                            model = uiState.node?.model(modelId = lePairingResponder)
                                ?: throw IllegalStateException("Le Pairing Responder Model not found"),
                            message = PairingRequest()
                        ) as PairingResponse?
                    } catch (e: Exception) {
                        error = e
                    } finally {
                        shouldShowProgressIcon = false
                    }
                    passKey = status?.let {
                        copyToClipboard(
                            scope = scope,
                            clipboard = clipboard,
                            text = it.passKey.toString(),
                            label = resources.getString(R.string.label_pass_key)
                        )
                        it.passKey
                    }
                    status
                        ?.takeIf { it.status == 0.toUByte() }
                        ?.let {
                            passKey = it.passKey
                            viewModel.startPairing(context)
                        }
                }
            },
            isOnClickActionInProgress = shouldShowProgressIcon
        )
    }
    ElevatedCardItem(
        imageVector = Icons.Outlined.Password,
        title = stringResource(R.string.label_pass_key),
        subtitle = passKey?.toString() ?: stringResource(R.string.label_unknown)
    )

    error?.let {
        MeshMessageStatusDialog(
            text = it.describe(),
            showDismissButton = true,
            onDismissRequest = { error = null },
        )
    }
    LaunchedEffect(uiState.isBonded) {
        if (uiState.isBonded == true) {
            goToNext()
        } else if (uiState.isBonded == false) {
            shouldShowProgressIcon = true
            try {
                status = viewModel.send(
                    model = uiState.node?.model(modelId = lePairingResponder)
                        ?: throw IllegalStateException("Le Pairing Responder Model not found"),
                    message = PairingRequest()
                ) as PairingResponse?
                passKey = status?.passKey
                status
                    ?.takeIf { it.status == 0.toUByte() }
                    ?.let {
                        passKey = it.passKey
                        println("Starting pairing")
                        viewModel.startPairing(context)
                    }
            } catch (e: Exception) {
                error = e
            } finally {
                shouldShowProgressIcon = false
            }
        }
    }
}