package no.nordicsemi.kotlin.mesh.core

import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress

/**
 * Defines events the MeshNetwork that can be observed.
 */
sealed class NetworkEvent {

    /**
     * An event that's emitted when the network is updated. This event is emitted when the network
     * is loaded, saved or cleared.
     *
     * Any changes that may occur due to the behavior of a Mesh Network will also trigger this event.
     * For example, when a node is added to the network where [MeshNetworkManager.save] is invoked.
     */
    data object NetworkUpdated : NetworkEvent()

    /**
     * An event that's emitted when a mesh message is received from the network.
     *
     * @property source      Address of the node that sent the message.
     * @property destination Address to which the message is destined to.
     * @property message     Mesh message that was received by the node.
     */
    data class MeshMessageReceived(
        val source: Address,
        val destination: MeshAddress,
        val message: MeshMessage,
    ) : NetworkEvent()
}