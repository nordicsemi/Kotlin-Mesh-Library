package no.nordicsemi.android.nrfmesh.feature.dfu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.Failed
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.NotStarted
import no.nordicsemi.android.nrfmesh.core.common.Sending
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import javax.inject.Inject

@HiltViewModel
internal class FirmwareUpdateViewModel @Inject internal constructor(
    private val repository: CoreDataRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FirmwareUpdateScreenUiState())
    internal val uiState = _uiState.asStateFlow()

    internal fun send(model: Model, message: AcknowledgedConfigMessage) {
        _uiState.value = _uiState.value.copy(messageState = Sending(message = message))
        val node = model.parentElement?.parentNode ?: return
        viewModelScope.launch {
            try {
                repository.send(node = node, message = message)?.let { response ->
                    _uiState.value = _uiState.value.copy(
                        messageState = Completed(
                            message = message,
                            response = response as ConfigResponse
                        ),
                        isRefreshing = false
                    )
                } ?: run {
                    _uiState.value = _uiState.value.copy(
                        messageState = Failed(
                            message = message,
                            error = IllegalStateException("No response received")
                        ),
                        isRefreshing = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    messageState = Failed(message = message, error = e),
                    isRefreshing = false
                )
            }
        }
    }
}


internal sealed interface MeshNetworkState {

    data object Loading : MeshNetworkState

    data class Success(val network: MeshNetwork) : MeshNetworkState

}

internal data class FirmwareUpdateScreenUiState(
    val messageState: MessageState = NotStarted,
    val isRefreshing: Boolean = false,
)
