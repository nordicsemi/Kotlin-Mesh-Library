@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package no.nordicsemi.android.nrfmesh.core.data.ui

import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.PrimaryGroupAddress

/**
 * GroupData is a data class that represents a Group in the Mesh network.
 *
 * @property name             Group name.
 * @property address          Address of the group.
 * @property parent           Parent group of the group if the given group is a sub group.
 * @property isUsed           Defines whether the group is in use by a node.
 */
data class GroupData(
    val name: String,
    val address: PrimaryGroupAddress,
    val parent: GroupData?,
    val isUsed: Boolean
) {
    constructor(group: Group) : this(
        name = group.name,
        address = group.address,
        parent = group.parent?.let { GroupData(group = it) },
        isUsed = group.isUsed
    )
}
