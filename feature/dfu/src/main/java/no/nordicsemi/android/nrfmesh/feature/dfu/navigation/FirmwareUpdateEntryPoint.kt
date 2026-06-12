package no.nordicsemi.android.nrfmesh.feature.dfu.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.nrfmesh.core.ui.BottomSheetSceneStrategy
import no.nordicsemi.android.nrfmesh.feature.dfu.FirmwareUpdate

@Serializable
data object FirmwareUpdateKey : NavKey

@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
fun EntryProviderScope<NavKey>.firmwareUpdateEntry() {
    entry<FirmwareUpdateKey>(metadata = BottomSheetSceneStrategy.bottomSheet()) {
        FirmwareUpdate()
    }
}
