package no.nordicsemi.android.nrfmesh.feature.quickconfiguration.navigation

import androidx.compose.runtime.getValue
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.DialogSceneStrategy
import kotlinx.serialization.Serializable
import no.nordicsemi.android.nrfmesh.feature.quickconfiguration.QuickConfigurationScreen
import no.nordicsemi.android.nrfmesh.feature.quickconfiguration.QuickConfigurationViewModel

/** Navigation key of the Quick Configuration screen of the node with the given [nodeUuid]. */
@Serializable
data class QuickConfigurationKey(val nodeUuid: String) : NavKey

fun EntryProviderScope<NavKey>.quickConfigurationEntry(onFinished: () -> Unit) {
    entry<QuickConfigurationKey>(
        metadata = DialogSceneStrategy.dialog(
            dialogProperties = DialogProperties(
                dismissOnBackPress = false,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        )
    ) { key ->
        val viewModel = hiltViewModel<QuickConfigurationViewModel, QuickConfigurationViewModel.Factory>(
            key = key.nodeUuid
        ) { it.create(uuid = key.nodeUuid) }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        QuickConfigurationScreen(
            uiState = uiState,
            onModeSelected = viewModel::onModeSelected,
            onKeyToggled = viewModel::onKeyToggled,
            onGroupToggled = viewModel::onGroupToggled,
            onPublishKeySelected = viewModel::onPublishKeySelected,
            onDestinationSelected = viewModel::onDestinationSelected,
            onModelToggled = viewModel::onModelToggled,
            onAllModelsToggled = viewModel::onAllModelsToggled,
            onAllKeysToggled = viewModel::onAllKeysToggled,
            onAllGroupsToggled = viewModel::onAllGroupsToggled,
            onAddKeyClicked = viewModel::onAddKeyClicked,
            onAddGroupClicked = viewModel::onAddGroupClicked,
            onApplyPressed = viewModel::onApplyPressed,
            onRetryPressed = viewModel::onRetryPressed,
            onFailureDismissed = viewModel::onFailureDismissed,
            onClose = {
                viewModel.onTasksClosed()
                onFinished()
            }
        )
    }
}
