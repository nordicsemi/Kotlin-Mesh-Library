@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nordicsemi.kotlin.mesh.core.model.serialization.UShortAsStringSerializer

typealias SceneNumber = UShort

/**
 * Scene
 *
 * @property name          Scene name.
 * @property number        Scene number.
 * @property addresses     Addresses containing the scene.
 * @property isInUse       Defines whether the scene is in use by a node.
 */
@ConsistentCopyVisibility
@Serializable
data class Scene internal constructor(
    @SerialName(value = "name")
    private var _name: String,
    @Serializable(with = UShortAsStringSerializer::class)
    val number: SceneNumber,
) {
    var name: String
        get() = _name
        set(value) {
            require(value.isNotBlank()) { "Name cannot be empty!" }
            MeshNetwork.onChange(oldValue = _name, newValue = value) { network?.updateTimestamp() }
            _name = value
        }

    @SerialName(value = "addresses")
    internal var _addresses: MutableList<UnicastAddress> = mutableListOf()
    val addresses: List<UnicastAddress>
        get() = _addresses

    val isInUse: Boolean
        get() = _addresses.isNotEmpty()

    @Transient
    internal var network: MeshNetwork? = null

    init {
        require(isValid(sceneNumber = number)) {
            "Scene number must be within $minSceneNumber and $maxSceneNumber!"
        }
    }

    /**
     * Adds the given unicast address to a scene.
     *
     * @param address Unicast address to be added.
     */
    internal fun add(address: UnicastAddress) {
        if (_addresses.none { it == address }) {
            _addresses.add(address)
            network?.updateTimestamp()
        }
    }

    /**
     * Adds the given list of unicast addresses to a scene.
     *
     * @param addresses List of unicast address.
     */
    internal fun add(addresses: List<UnicastAddress>) {
        // First remove all the items that are contained
        remove(addresses)
        _addresses.addAll(addresses)
        network?.updateTimestamp()
    }

    /**
     * Removes the given unicast address from the list of addresses.
     *
     * @param address Address to be removed.
     */
    internal fun remove(address: UnicastAddress) {
        _addresses.remove(address)
        network?.updateTimestamp()
    }

    /**
     * Removes the given list of unicast addresses from the list of addresses.
     *
     * @param addresses Addresses to be removed.
     */
    internal fun remove(addresses: List<UnicastAddress>) {
        _addresses.removeAll(addresses)
        network?.updateTimestamp()
    }

    /**
     * Returns a list of nodes registered to a given scene address.
     */
    fun nodes(): List<Node> = network?._nodes?.filter { node ->
        node.elements.any { element -> _addresses.contains(element.unicastAddress) }
    } ?: listOf()

    /**
     * Returns a list of elements registered to a given scene address.
     */
    fun elements(): List<Element> = network?._nodes?.flatMap { node ->
        node.elements.filter { element -> _addresses.contains(element.unicastAddress) }
    } ?: listOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Scene

        if (_name != other._name) return false
        if (number != other.number) return false
        if (_addresses != other._addresses) return false

        return true
    }

    override fun hashCode(): Int {
        var result = _name.hashCode()
        result = 31 * result + number.hashCode()
        result = 31 * result + _addresses.hashCode()
        return result
    }

    companion object {
        /**
         * Returns true if the given scene number is within the range of 0x0001 to 0xFFFF
         * @param sceneNumber Scene number to be checked.
         */
        fun isValid(sceneNumber: SceneNumber) = sceneNumber in minSceneNumber..maxSceneNumber
    }
}

const val minSceneNumber: SceneNumber = 0x0001u
const val maxSceneNumber: SceneNumber = 0xFFFFu
val allScenesRange = SceneRange(minSceneNumber, maxSceneNumber)