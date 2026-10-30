@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.util

import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.data.toHexString
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.crypto.Crypto

/**
 * The Node Identity contains information from Node Identity or Private Node Identity beacon.
 *
 * This can be used to identify a node in the network.
 *
 * @property hash    Function of the included random number and identity information.
 * @property random  64-bit random number.
 */
sealed interface NodeIdentity {
    val hash: ByteArray
    val random: ByteArray

    /**
     * Returns whether the identity matches given node.
     *
     * @param node Node to be matched.
     * @return true if the identity matches, false otherwise.
     */
    fun matches(node: Node): Boolean

    /**
     * Returns the first Node that matches the identity from the given list.
     *
     * @param nodes List of Nodes to be matched.
     * @return The first matching Node.
     */
    fun matches(nodes: List<Node>) = nodes.firstOrNull { node ->
        matches(node = node)
    }

    /**
     * Returns the Node Identity as a hex string with hash and random concatenated.
     *
     * @return Node Identity in hex string format.
     */
    fun toHexString(): String {
        return hash.toHexString(
            format = HexFormat {
                number.prefix = "0x"
                upperCase = true
            }
        ) + random.toHexString(
            format = HexFormat.UpperCase
        )
    }
}

/**
 * Representation of Node Identity advertisement packet.
 *
 * @property hash    Function of the included random number and identity information.
 * @property random  64-bit random number.
 */
@ConsistentCopyVisibility
data class PublicNodeIdentity internal constructor(
    override val hash: ByteArray,
    override val random: ByteArray,
) : NodeIdentity {

    override fun matches(node: Node): Boolean {
        val data = ByteArray(6) { 0 } + random +
                node.primaryUnicastAddress.address.toByteArray()
        for (key in node.networkKeys) {
            val calculatedHash =
                Crypto.calculateHash(data = data, identityKey = key.derivatives.identityKey)
            if (hash.contentEquals(calculatedHash))
                return true

            key.oldDerivatives?.identityKey?.let {
                if (hash.contentEquals(Crypto.calculateHash(data = data, identityKey = it)))
                    return true
            }
        }
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PublicNodeIdentity

        if (!hash.contentEquals(other.hash)) return false
        if (!random.contentEquals(other.random)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hash.contentHashCode()
        result = 31 * result + random.contentHashCode()
        return result
    }
}

/**
 * Representation of Private Node Identity advertisement packet.
 *
 * @property hash    Function of the included random number and identity information.
 * @property random  64-bit random number.
 */
@ConsistentCopyVisibility
data class PrivateNodeIdentity internal constructor(
    override val hash: ByteArray,
    override val random: ByteArray,
) : NodeIdentity {
    override fun matches(node: Node): Boolean {
        val data = ByteArray(5) + 0x03.toByte() + random +
                node.primaryUnicastAddress.address.toByteArray()
        for (key in node.networkKeys) {
            val calculatedHash =
                Crypto.calculateHash(data = data, identityKey = key.derivatives.identityKey)
            if (hash.contentEquals(calculatedHash))
                return true

            key.oldDerivatives?.identityKey?.let {
                if (hash.contentEquals(Crypto.calculateHash(data = data, identityKey = it)))
                    return true
            }
        }
        return false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PrivateNodeIdentity

        if (!hash.contentEquals(other.hash)) return false
        if (!random.contentEquals(other.random)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = hash.contentHashCode()
        result = 31 * result + random.contentHashCode()
        return result
    }
}

/**
 * Returns the Node Identity for a given Node Identity beacon.
 *
 * @receiver ByteArray Node Identity beacon.
 * @return NodeIdentity or null if the beacon is invalid.
 */
fun ByteArray.nodeIdentity() = when (size) {
    17 if get(0) == 0x01.toByte() -> PublicNodeIdentity(
        hash = sliceArray(1 until 9),
        random = sliceArray(9 until 17)
    )

    17 if get(0) == 0x03.toByte() -> PrivateNodeIdentity(
        hash = sliceArray(1 until 9),
        random = sliceArray(9 until 17)
    )

    else -> null
}