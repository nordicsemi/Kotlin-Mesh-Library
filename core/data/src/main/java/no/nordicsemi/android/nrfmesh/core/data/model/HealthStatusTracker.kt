package no.nordicsemi.android.nrfmesh.core.data.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import no.nordicsemi.kotlin.mesh.core.NetworkEvent
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthCurrentStatus
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFault
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultStatus
import no.nordicsemi.kotlin.mesh.core.model.Address

/**
 * The faults last reported by a Health Server.
 *
 * @property testId             Identifier of the most recent test performed.
 * @property companyIdentifier  Company identifier the faults belong to.
 * @property faults             Faults reported by the node.
 * @property isCurrent          True if the faults were published by the node as a Health Current
 *                              Status, false if they are the response to a Health Fault Get,
 *                              Clear or Test.
 */
data class HealthFaults(
    val testId: UByte,
    val companyIdentifier: UShort,
    val faults: List<HealthFault>,
    val isCurrent: Boolean,
)

/**
 * Keeps the faults last reported by every Health Server, so that they are not lost while no screen
 * is observing the network. Like in the iOS app, a Health Current Status and a Health Fault Status
 * replace each other, the latest one wins.
 */
class HealthStatusTracker {
    /** The faults last reported by the nodes, by the unicast address of the sending element. */
    val faults: StateFlow<Map<Address, HealthFaults>>
        field = MutableStateFlow(value = emptyMap())

    /** Updates the faults of the node that sent the message, if it is a health status. */
    fun onEvent(event: NetworkEvent) {
        if (event !is NetworkEvent.MeshMessageReceived) return
        when (val message = event.message) {
            is HealthCurrentStatus -> store(event.source, message.toFaults(isCurrent = true))
            is HealthFaultStatus -> store(event.source, message.toFaults(isCurrent = false))
            else -> {}
        }
    }

    /** Forgets all faults, e.g. when another network is loaded. */
    fun clear() = faults.update { emptyMap() }

    private fun store(source: Address, value: HealthFaults) =
        faults.update { it + (source to value) }

    private fun HealthCurrentStatus.toFaults(isCurrent: Boolean) =
        HealthFaults(testId, companyIdentifier, this.faults, isCurrent)

    private fun HealthFaultStatus.toFaults(isCurrent: Boolean) =
        HealthFaults(testId, companyIdentifier, this.faults, isCurrent)
}
