package no.nordicsemi.android.nrfmesh.core.common

import no.nordicsemi.kotlin.mesh.core.model.FixedGroupAddress
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress

/**
 * Returns a list of addresses or names of the destination of the model's publication.
 */
fun Model.publishDestination() = publish?.let { publish ->
    parentElement?.parentNode?.network?.let { network ->
        when (publish.address) {
            is UnicastAddress -> {
                network.element(elementAddress = publish.address.address)?.let { element ->
                    val parentNodeName = element.parentNode?.name
                    element.name?.let { elementName ->
                        if (parentNodeName != null) return@let "$elementName in $parentNodeName"
                        else return@let "$elementName: ${publish.address.toHexString()}"
                    } ?: publish.address.toHexString()
                } ?: publish.address.toHexString()
            }

            is GroupAddress -> network
                .group(address = publish.address.address)
                ?.name
                ?: publish.address.toHexString()

            is FixedGroupAddress -> (publish.address as FixedGroupAddress).name()
            else -> publish.address.toHexString()

        }
    } ?: return@let publish.address.toHexString()
}

/**
 * Returns a list of groups that is not already subscribed to this model.
 */
fun Model.publishKey() = boundApplicationKeys.firstOrNull { it.index == publish?.index }