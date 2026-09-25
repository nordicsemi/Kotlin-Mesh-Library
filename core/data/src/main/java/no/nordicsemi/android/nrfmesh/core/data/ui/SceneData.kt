@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package no.nordicsemi.android.nrfmesh.core.data.ui

import no.nordicsemi.android.nrfmesh.core.common.KeyIdGenerator
import no.nordicsemi.kotlin.mesh.core.model.Scene
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress

typealias SceneNumber = UShort

/**
 * Scene
 *
 * @property name          Scene name.
 * @property number        Scene number.
 * @property addresses     Addresses containing the scene.
 * @property isInUse       Defines whether the scene is in use by a node.
 * @property id            Unique ID for the scene data.
 */
data class SceneData(
    val name: String,
    val number: SceneNumber,
    val addresses: List<UnicastAddress>,
    val isInUse: Boolean,
) {
    internal val id: Long = KeyIdGenerator.nextId()

    constructor(scene: Scene) : this(
        name = scene.name,
        number = scene.number,
        addresses = scene.addresses,
        isInUse = scene.isInUse
    )
}
