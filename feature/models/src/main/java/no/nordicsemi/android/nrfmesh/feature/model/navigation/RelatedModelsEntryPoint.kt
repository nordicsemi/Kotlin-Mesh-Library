package no.nordicsemi.android.nrfmesh.feature.model.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.nrfmesh.core.navigation.Navigator
import no.nordicsemi.android.nrfmesh.core.ui.BottomSheetSceneStrategy
import no.nordicsemi.android.nrfmesh.feature.model.extension.RelatedModelsScreen
import no.nordicsemi.kotlin.mesh.core.model.Model

@Serializable
data class RelatedModelsKey(val model: Model) : NavKey

@OptIn(ExperimentalMaterial3Api::class)
fun EntryProviderScope<NavKey>.relatedModelsEntry(navigator: Navigator) {
    entry<RelatedModelsKey>(metadata = BottomSheetSceneStrategy.bottomSheet()) { key ->
        RelatedModelsScreen(
            model = key.model,
            // TODO currently no operational
            onModelClicked = { model ->
                // Dismiss bottom sheet before navigating to model
                navigator.goBack()
                model.parentElement?.unicastAddress?.let {
                    navigator.navigate(
                        key = ModelKey(address = it.address, modelId = model.modelId.id)
                    )
                }
            }
        )
    }
}