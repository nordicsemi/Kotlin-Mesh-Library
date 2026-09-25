package no.nordicsemi.android.nrfmesh.feature.dfu.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.nrfmesh.core.ui.BottomSheetSceneStrategy
import no.nordicsemi.android.nrfmesh.feature.bind.appkeys.BindAppKeysScreen
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.model.Model

@Serializable
data class DfuBindAppKeysKey(val model: Model) : NavKey

@OptIn(ExperimentalMaterial3Api::class)
fun EntryProviderScope<NavKey>.dfuBindAppKeysEntry(
    send: (Model, AcknowledgedConfigMessage) -> Unit,
) {
    entry<DfuBindAppKeysKey>(metadata = BottomSheetSceneStrategy.bottomSheet()) { key ->
        BindAppKeysScreen(
            model = key.model,
            send = {
                send(key.model, it)
            }
        )
    }
}