package no.nordicsemi.android.nrfmesh.feature.quickconfiguration

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.Publish
import androidx.compose.material.icons.outlined.VpnKey
import no.nordicsemi.android.nrfmesh.core.data.configurator.ConfigTask
import no.nordicsemi.android.nrfmesh.core.data.configurator.MeshTask
import no.nordicsemi.android.nrfmesh.core.data.configurator.NodeKeyTaskBuilder
import no.nordicsemi.android.nrfmesh.core.data.name
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelAppBind
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationVirtualAddressSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelSubscriptionAdd
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelSubscriptionVirtualAddressAdd
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.PublicationAddress
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import no.nordicsemi.kotlin.mesh.core.model.MasterSecurity
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.Publish
import no.nordicsemi.kotlin.mesh.core.model.PublishPeriod
import no.nordicsemi.kotlin.mesh.core.model.Retransmit

/**
 * Builds the tasks required to send the given keys to the [node], if it does not know them yet.
 *
 * The bound Network Key is sent before the Application Key.
 */
private fun keyTasks(node: Node, keys: List<ApplicationKey>): List<MeshTask> =
    NodeKeyTaskBuilder(node = node).tasksFor(keys = keys)

/** Binds each of the [keys] to each of the [models] of the [node]. */
internal fun bindTasks(node: Node, keys: List<ApplicationKey>, models: List<Model>): List<MeshTask> =
    keyTasks(node = node, keys = keys) + models.flatMap { model ->
        keys.filterNot { it.isBoundTo(model) }.map { key ->
            ConfigTask(
                icon = Icons.Outlined.AddLink,
                label = "Bind ${key.name} to ${model.name()}",
                message = ConfigModelAppBind(model = model, applicationKey = key),
                target = node
            )
        }
    }

/** Subscribes each of the [models] of the [node] to each of the [groups]. */
internal fun subscribeTasks(node: Node, groups: List<Group>, models: List<Model>): List<MeshTask> =
    models.flatMap { model ->
        groups.filterNot { model.isSubscribedTo(it) }.map { group ->
            ConfigTask(
                icon = Icons.Outlined.GroupWork,
                label = "Subscribe ${model.name()} to ${group.name}",
                // Groups with a virtual label require the virtual address variant of the message.
                message = when (group.address) {
                    is VirtualAddress -> ConfigModelSubscriptionVirtualAddressAdd(
                        group = group,
                        model = model
                    )

                    else -> ConfigModelSubscriptionAdd(group = group, model = model)
                },
                target = node
            )
        }
    }

/**
 * Sets the publication of each of the [models] of the [node] to the [destination] using the [key].
 * The key is sent and bound to the models if required.
 */
internal fun publishTasks(
    node: Node,
    key: ApplicationKey,
    destination: Group,
    models: List<Model>,
): List<MeshTask> = bindTasks(node = node, keys = listOf(key), models = models) +
        models.map { model ->
            ConfigTask(
                icon = Icons.Outlined.Publish,
                label = "Publish ${model.name()} to ${destination.name}",
                message = Publish(
                    address = destination.publicationAddress(),
                    index = key.index,
                    ttl = DEFAULT_PUBLISH_TTL,
                    period = PublishPeriod.disabled,
                    credentials = MasterSecurity,
                    retransmit = Retransmit.disabled
                ).let { publish ->
                    when (publish.address) {
                        is VirtualAddress ->
                            ConfigModelPublicationVirtualAddressSet(publish = publish, model = model)

                        else -> ConfigModelPublicationSet(publish = publish, model = model)
                    }
                },
                target = node
            )
        }

private fun Group.publicationAddress(): PublicationAddress = when (val primary = address) {
    is GroupAddress -> primary
    is VirtualAddress -> primary
    else -> error("Unsupported group address: $primary")
}

/** TTL 255 makes the node use its Default TTL. */
private const val DEFAULT_PUBLISH_TTL: UByte = 0xFFu

/** Something that does not have to be configured, as the node already has the configuration. */
internal data class AlreadyConfigured(
    val mode: QuickConfigurationMode,
    val itemName: String,
    val modelName: String,
)

/** The [keys] that are already bound to the [models]. */
internal fun alreadyBound(keys: List<ApplicationKey>, models: List<Model>) =
    models.flatMap { model ->
        keys.filter { it.isBoundTo(model) }.map {
            AlreadyConfigured(QuickConfigurationMode.Bind, itemName = it.name, modelName = model.name())
        }
    }

/** The [groups] that the [models] are already subscribed to. */
internal fun alreadySubscribed(groups: List<Group>, models: List<Model>) =
    models.flatMap { model ->
        groups.filter { model.isSubscribedTo(it) }.map {
            AlreadyConfigured(QuickConfigurationMode.Subscribe, itemName = it.name, modelName = model.name())
        }
    }
