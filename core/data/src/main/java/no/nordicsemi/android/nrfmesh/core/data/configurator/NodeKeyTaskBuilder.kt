package no.nordicsemi.android.nrfmesh.core.data.configurator

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.VpnKey
import no.nordicsemi.android.nrfmesh.core.common.name
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetKeyAdd
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.Node

/**
 * Creates the tasks required to send Application Keys to a [node].
 *
 * An Application Key can only be added to a node that knows the Network Key it is bound to, so if
 * the node does not know it yet, the Network Key is added first. The builder remembers the keys it
 * has already created tasks for, as the node learns about them only once the tasks are executed.
 * Use one builder for all the tasks that are enqueued for the same node.
 *
 * @param node  Node to which the keys will be sent.
 * @param verb  Verb used in the label of the tasks, e.g. "Add".
 */
class NodeKeyTaskBuilder(private val node: Node, private val verb: String = "Add") {
    private val queuedNetKeys = mutableSetOf<KeyIndex>()
    private val queuedAppKeys = mutableSetOf<KeyIndex>()

    /**
     * Returns the tasks that send the given [keys], and the Network Keys they are bound to, to the
     * node. Keys that the node already knows, or that were returned before, are not repeated.
     */
    fun tasksFor(keys: List<ApplicationKey>): List<ConfigTask> = buildList {
        keys.forEach { key ->
            if (node.knows(key) || !queuedAppKeys.add(key.index)) return@forEach
            val netKey = key.boundNetworkKey
            if (!node.knows(netKey) && queuedNetKeys.add(netKey.index)) {
                add(
                    ConfigTask(
                        icon = Icons.Outlined.VpnKey,
                        label = "$verb ${netKey.name} to ${node.name}",
                        message = ConfigNetKeyAdd(key = netKey),
                        target = node
                    )
                )
            }
            add(
                ConfigTask(
                    icon = Icons.Outlined.VpnKey,
                    label = "$verb ${key.name} to ${node.name}",
                    message = ConfigAppKeyAdd(key = key),
                    target = node
                )
            )
        }
    }
}
