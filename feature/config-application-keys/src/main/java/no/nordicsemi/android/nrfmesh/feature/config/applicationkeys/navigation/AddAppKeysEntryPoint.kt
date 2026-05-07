package no.nordicsemi.android.nrfmesh.feature.config.applicationkeys.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.core.ui.BottomSheetSceneStrategy
import no.nordicsemi.android.nrfmesh.feature.config.applicationkeys.AddAppKeysViewModel
import no.nordicsemi.android.nrfmesh.feature.config.applicationkeys.AddApplicationKeysScreen
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyAdd

@Serializable
data class AddAppKeysKey(val uuid: String) : NavKey

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
fun EntryProviderScope<NavKey>.addAppKeysEntry(navigator: Navigator) {
    entry<AddAppKeysKey>(
        metadata = BottomSheetSceneStrategy.bottomSheet()
    ) { key ->
        val uuid = key.uuid
        val viewModel = hiltViewModel<AddAppKeysViewModel, AddAppKeysViewModel.Factory>(
            key = uuid
        ) { factory ->
            factory.create(uuid = uuid)
        }
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        AddApplicationKeysScreen(
            messageState = uiState.messageState,
            keys = uiState.availableAppKeys,
            onAddApplicationKeyClicked = viewModel::addApplicationKey,
            onAppKeyClicked = { viewModel.send(message = ConfigAppKeyAdd(key = it)) },
            resetMessageState = viewModel::resetMessageState
        )
    }
}