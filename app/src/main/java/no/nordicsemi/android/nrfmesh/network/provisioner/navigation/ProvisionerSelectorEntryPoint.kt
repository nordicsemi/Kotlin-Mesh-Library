package no.nordicsemi.android.nrfmesh.network.provisioner.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.core.ui.BottomSheetSceneStrategy
import no.nordicsemi.android.nrfmesh.network.provisioner.ProvisionerSelectorScreen
import no.nordicsemi.android.nrfmesh.network.provisioner.ProvisionerSelectorViewModel

@Serializable
data object ProvisionerSelectorKey : NavKey

@OptIn(ExperimentalMaterial3Api::class)
fun EntryProviderScope<NavKey>.provisionerSelectorEntry(navigator: Navigator) =
    entry<ProvisionerSelectorKey>(
        metadata = BottomSheetSceneStrategy.bottomSheet(
            modalBottomSheetProperties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = false,
                shouldDismissOnClickOutside = false
            ),
        )
    ) {
        val viewModel = hiltViewModel<ProvisionerSelectorViewModel>()
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        ProvisionerSelectorScreen(
            provisioners = uiState.provisioners,
            onProvisionerSelected = {
                viewModel.onProvisionerSelected(provisioner = it)
                navigator.goBack()
            }
        )
    }