package no.nordicsemi.android.nrfmesh.feature.dfu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.Completed
import no.nordicsemi.android.nrfmesh.core.common.Failed
import no.nordicsemi.android.nrfmesh.core.common.MessageState
import no.nordicsemi.android.nrfmesh.core.common.NotStarted
import no.nordicsemi.android.nrfmesh.core.common.Sending
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.feature.dfu.util.DfuParameters
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Receiver
import no.nordicsemi.android.nrfmesh.feature.dfu.util.UpdatePackage
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import javax.inject.Inject

@HiltViewModel
internal class DfuSessionViewModel @Inject internal constructor(
    private val repository: CoreDataRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(FirmwareUpdateScreenUiState())
    internal val uiState = _uiState.asStateFlow()

    /**
     * State shared between the pages of the firmware update pager. As the ViewModel is scoped to
     * the navigation entry, every page gets the same instance and the state survives configuration
     * changes.
     */
    internal val session: StateFlow<DfuSession>
        field = MutableStateFlow(DfuSession())

    internal fun setKeyIndex(index: Int) = session.update { it.copy(keyIndex = index) }

    internal fun setReceivers(receivers: List<Receiver>) =
        session.update { it.copy(receivers = receivers) }

    internal fun setUpdatePackage(updatePackage: UpdatePackage?) =
        session.update { it.copy(updatePackage = updatePackage) }

    internal fun setDfuParameters(dfuParameters: DfuParameters) =
        session.update { it.copy(dfuParameters = dfuParameters) }

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


/**
 * Data collected across the pages of the firmware update pager.
 *
 * @property keyIndex       Index of the selected application key, or -1 if none is selected.
 * @property receivers      Receivers selected to be updated.
 * @property updatePackage  Selected firmware package.
 * @property dfuParameters  Firmware distribution parameters.
 */
internal data class DfuSession(
    val keyIndex: Int = -1,
    val receivers: List<Receiver> = emptyList(),
    val updatePackage: UpdatePackage? = null,
    val dfuParameters: DfuParameters = DfuParameters(),
) {
    /**
     * Returns whether the user has made the selections required to leave the given page.
     *
     * @param page           Index of the page in the firmware update pager.
     * @param tasksCompleted Whether all the tasks enqueued on page 4 have completed.
     */
    fun canProceed(page: Int, tasksCompleted: Boolean): Boolean = when (page) {
        // A key is selected once the distributor's capabilities have been retrieved.
        0 -> keyIndex >= 0
        // LE Pairing, advances automatically once the device is bonded.
        1 -> true
        // A firmware package and at least one receiver must be selected.
        2 -> updatePackage != null && receivers.isNotEmpty()
        // Distribution parameters always have valid defaults.
        3 -> dfuParameters.ttl <= 127u
        // All the enqueued tasks must have completed before the distribution can start.
        4 -> tasksCompleted
        // Last page.
        else -> false
    }
}
