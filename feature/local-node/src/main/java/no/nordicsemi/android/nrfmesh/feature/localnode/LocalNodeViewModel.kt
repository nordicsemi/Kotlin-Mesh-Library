package no.nordicsemi.android.nrfmesh.feature.localnode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import no.nordicsemi.android.nrfmesh.core.common.NORDIC_SEMICONDUCTOR_COMPANY_ID
import no.nordicsemi.android.nrfmesh.core.common.VendorModelIds
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.model.GenericLevelClient
import no.nordicsemi.android.nrfmesh.core.data.model.GenericOnOffClient
import no.nordicsemi.android.nrfmesh.core.data.model.vendor.simpleonoff.SimpleOnOffClient
import no.nordicsemi.kotlin.mesh.core.model.Element
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.SigModelId
import no.nordicsemi.kotlin.mesh.core.model.VendorModelId
import javax.inject.Inject

@HiltViewModel
internal class LocalNodeViewModel @Inject internal constructor(
    private val repository: CoreDataRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(value = LocalNodeUiState())
    val uiState: StateFlow<LocalNodeUiState> = _uiState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LocalNodeUiState()
    )

    init {
        repository.meshNetwork?.let { update(network = it) }
        repository.networkEvents
            .mapNotNull { repository.meshNetwork }
            .onEach { update(network = it) }
            .launchIn(scope = viewModelScope)
    }

    private fun update(network: MeshNetwork) {
        val provisioner = network.localProvisioner
        val sections = provisioner?.node?.elements.orEmpty().mapNotNull { element ->
            element.models.filter { it.isSupported() }
                .takeIf { it.isNotEmpty() }
                ?.let { LocalNodeSection(element = element, models = it) }
        }
        _uiState.update {
            LocalNodeUiState(
                sections = sections,
                // The messages are sent using the keys of the local node, which requires the
                // provisioner to have a unicast address assigned.
                isEnabled = provisioner?.hasConfigurationCapabilities == true
            )
        }
    }

    /** Sets the Generic OnOff Client or the Simple OnOff Client state, which is then published. */
    fun setOnOff(model: Model, on: Boolean) {
        when (val handler = model.eventHandler) {
            is GenericOnOffClient -> handler.state = on
            is SimpleOnOffClient -> handler.state = on
            else -> Unit
        }
    }

    /** Increments or decrements the Generic Level Client state, which is then published. */
    fun stepLevel(model: Model, increase: Boolean) {
        (model.eventHandler as? GenericLevelClient)?.let { handler ->
            handler.state = (handler.state + if (increase) LEVEL_STEP else -LEVEL_STEP).toShort()
        }
    }

    private companion object {
        const val LEVEL_STEP = 8192
    }
}

/**
 * Control UI state.
 *
 * @property sections  Elements of the local node with the models that can be controlled.
 * @property isEnabled True if the controls can be used.
 */
internal data class LocalNodeUiState(
    val sections: List<LocalNodeSection> = emptyList(),
    val isEnabled: Boolean = false,
)

internal data class LocalNodeSection(val element: Element, val models: List<Model>)

/** The kind of control that is shown for a model. */
internal enum class ControlType { ON_OFF_CLIENT, ON_OFF_SERVER, LEVEL_CLIENT, LEVEL_SERVER }

internal fun Model.controlType(): ControlType? = when (val id = modelId) {
    is SigModelId -> when (id.modelIdentifier) {
        Model.GENERIC_ON_OFF_CLIENT_MODEL_ID -> ControlType.ON_OFF_CLIENT
        Model.GENERIC_ON_OFF_SERVER_MODEL_ID -> ControlType.ON_OFF_SERVER
        Model.GENERIC_LEVEL_CLIENT_MODEL_ID -> ControlType.LEVEL_CLIENT
        Model.GENERIC_LEVEL_SERVER_MODEL_ID -> ControlType.LEVEL_SERVER
        else -> null
    }

    is VendorModelId ->
        if (id.companyIdentifier == NORDIC_SEMICONDUCTOR_COMPANY_ID &&
            id.modelIdentifier == VendorModelIds.SIMPLE_ON_OFF_CLIENT_MODEL_ID
        ) ControlType.ON_OFF_CLIENT else null
}

private fun Model.isSupported() = controlType() != null && eventHandler != null
