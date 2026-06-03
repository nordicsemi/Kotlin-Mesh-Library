package no.nordicsemi.android.nrfmesh.feature.model.dfu.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import no.nordicsemi.android.nrfmesh.core.data.name
import no.nordicsemi.android.nrfmesh.core.ui.BottomSheetSceneStrategy
import no.nordicsemi.android.nrfmesh.feature.model.dfu.FirmwareInformationScreen
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareInformation
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.model.Model
import kotlin.uuid.ExperimentalUuidApi

@Serializable
data class FirmwareInformationKey(val model: Model, val information: FirmwareInformation) : NavKey

@OptIn(
    ExperimentalMaterial3AdaptiveApi::class,
    ExperimentalUuidApi::class,
    ExperimentalMaterial3Api::class
)

internal fun EntryProviderScope<NavKey>.firmwareInformationEntryPoint(
    isInProgress: Boolean,
    send: suspend (Model, AcknowledgedMeshMessage) -> MeshMessage?,
) = entry<FirmwareInformationKey>(
    metadata = BottomSheetSceneStrategy.bottomSheet()
) { key ->
    FirmwareInformationScreen(
        title = key.model.name(),
        information = key.information,
        isInProgress = isInProgress,
        send = { send(key.model, it) }
    )
}