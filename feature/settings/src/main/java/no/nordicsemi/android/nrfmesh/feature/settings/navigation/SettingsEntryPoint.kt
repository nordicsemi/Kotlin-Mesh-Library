package no.nordicsemi.android.nrfmesh.feature.settings.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import no.nordicsemi.android.nrfmesh.core.navigation.AppState
import no.nordicsemi.android.nrfmesh.core.navigation.ClickableSetting
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.core.navigation.SettingsKey
import no.nordicsemi.android.nrfmesh.core.navigation.SettingsListDetailSceneKey
import no.nordicsemi.android.nrfmesh.core.ui.PlaceHolder
import no.nordicsemi.android.nrfmesh.core.ui.isCompactWidth
import no.nordicsemi.android.nrfmesh.feature.application.keys.navigation.ApplicationKeysContentKey
import no.nordicsemi.android.nrfmesh.feature.application.keys.navigation.applicationKeysEntry
import no.nordicsemi.android.nrfmesh.feature.developer.navigation.DeveloperSettingsContentKey
import no.nordicsemi.android.nrfmesh.feature.developer.navigation.developerSettingsEntry
import no.nordicsemi.android.nrfmesh.feature.ivindex.navigation.IvIndexContentKey
import no.nordicsemi.android.nrfmesh.feature.ivindex.navigation.ivIndexEntry
import no.nordicsemi.android.nrfmesh.feature.network.keys.navigation.NetworkKeysContentKey
import no.nordicsemi.android.nrfmesh.feature.network.keys.navigation.networkKeysEntry
import no.nordicsemi.android.nrfmesh.feature.provisioners.navigation.ProvisionersContentKey
import no.nordicsemi.android.nrfmesh.feature.provisioners.navigation.provisionersEntry
import no.nordicsemi.android.nrfmesh.feature.scenes.navigation.ScenesContentKey
import no.nordicsemi.android.nrfmesh.feature.scenes.navigation.scenesEntry
import no.nordicsemi.android.nrfmesh.feature.settings.R
import no.nordicsemi.android.nrfmesh.feature.settings.SettingsListScreen
import no.nordicsemi.android.nrfmesh.feature.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun EntryProviderScope<NavKey>.settingsEntry(appState: AppState, navigator: Navigator) {
    entry<SettingsKey>(
        metadata = ListDetailSceneStrategy.listPane(
            sceneKey = SettingsListDetailSceneKey,
            detailPlaceholder = {
                PlaceHolder(
                    modifier = Modifier.fillMaxSize(),
                    imageVector = Icons.Outlined.Settings,
                    text = stringResource(R.string.label_select_settings_item_rationale)
                )
            }
        )
    ) { key ->
        val viewModel = hiltViewModel<SettingsViewModel, SettingsViewModel.Factory>(
            key = "SettingsViewModel"
        ) { factory ->
            factory.create(setting = key.setting)
        }

        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        SettingsListScreen(
            uiState = uiState,
            onNameChanged = viewModel::onNameChanged,
            highlightSelectedItem = !isCompactWidth() &&
                    // If the settings tab is not selected and no item is selected upon back press
                    // we clear the last selected setting
                    if (appState.navigationState.currentTopLevelKey !is SettingsKey) {
                        val shouldNotHighlight = appState.navigationState.subStacks
                            .filterKeys { it is SettingsKey }
                            .entries
                            .size == 1
                        if (shouldNotHighlight) viewModel.resetSelectedSetting()
                        shouldNotHighlight
                    } else true,
            navigateToProvisioners = {
                viewModel.onItemSelected(ClickableSetting.PROVISIONERS)
                navigator.navigateFrom(parent = key, key = ProvisionersContentKey)
            },
            navigateToNetworkKeys = {
                viewModel.onItemSelected(ClickableSetting.NETWORK_KEYS)
                navigator.navigateFrom(parent = key, key = NetworkKeysContentKey)
            },
            navigateToApplicationKeys = {
                viewModel.onItemSelected(ClickableSetting.APPLICATION_KEYS)
                navigator.navigateFrom(parent = key, key = ApplicationKeysContentKey)
            },
            navigateToScenes = {
                viewModel.onItemSelected(ClickableSetting.SCENES)
                navigator.navigateFrom(parent = key, key = ScenesContentKey)
            },
            navigateToIvIndex = {
                viewModel.onItemSelected(ClickableSetting.IV_INDEX)
                navigator.navigateFrom(parent = key, key = IvIndexContentKey)
            },
            navigateToDeveloperSettings = {
                viewModel.onItemSelected(ClickableSetting.DEVELOPER_SETTINGS)
                navigator.navigateFrom(parent = key, key = DeveloperSettingsContentKey)
            }
        )
    }
    provisionersEntry(appState = appState, navigator = navigator)
    networkKeysEntry(appState = appState, navigator = navigator)
    applicationKeysEntry(appState = appState, navigator = navigator)
    scenesEntry(appState = appState, navigator = navigator)
    ivIndexEntry()
    developerSettingsEntry(appState = appState)
}