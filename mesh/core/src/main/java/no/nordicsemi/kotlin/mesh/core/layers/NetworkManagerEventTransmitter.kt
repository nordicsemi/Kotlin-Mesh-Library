@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.layers

import kotlinx.coroutines.flow.SharedFlow
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNodeReset

/**
 * Transmits network manager events.
 *
 * @property networkManagerEventFlow Flow containing Network manager events.
 */
internal interface NetworkManagerEventTransmitter {

    val networkManagerEventFlow: SharedFlow<NetworkManagerEvent>

    suspend fun emitNetworkManagerEvent(event: NetworkManagerEvent)
}

/**
 * Defines events that are handled by the [NetworkManagerEvent].
 */
internal sealed class NetworkManagerEvent {

    /**
     * An event used to notify when the Network Configuration has changed.
     */
    data object OnNetworkChanged : NetworkManagerEvent()

    /**
     * An event used to notify when the [ConfigNodeReset] message was received for the local
     * Node.
     *
     * The Node should forget the mesh network, all the keys, nodes, groups and scenes.
     *
     * A network might be created.
     */
    data object OnNetworkReset : NetworkManagerEvent()
}