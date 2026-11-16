@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.util

import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.crypto.Crypto

/**
 * The Network Identity contains information from Network Identity or Private Network Identity
 * beacon.
 *
 */
sealed interface NetworkIdentity {

    /**
     * Returns whether the identity matches given network key.
     *
     * @param networkKey Network key to be matched.
     * @return true if the identity matches, false otherwise.
     */
    fun matches(networkKey: NetworkKey): Boolean

    /**
     * Returns the first Network Key that matches the identity from the given list.
     *
     * @param networkKeys List of Network Keys to be matched.
     * @return The first matching Network Key.
     */
    fun matches(networkKeys: List<NetworkKey>) = networkKeys.firstOrNull { networkKey ->
        matches(networkKey = networkKey)
    }

    /**
     * Returns the Network Identity as a hex string.
     *
     * @return Network Identity in hex string format.
     */
    fun toHexString() = when (this) {
        is PrivateNetworkIdentity ->
            hash.toHexString(
                format = HexFormat {
                    number.prefix = "0x"
                    upperCase = true
                }
            ) + random.toHexString(format = HexFormat.UpperCase)

        is PublicNetworkIdentity ->
            networkId.toHexString(
                format = HexFormat {
                    number.prefix = "0x"
                    upperCase = true
                }
            )
    }

}

/**
 * Represents a Private Node Identity advertisement packet.
 *
 * @param networkId 64-bit network identifier derived from the network key.
 */
@ConsistentCopyVisibility
data class PublicNetworkIdentity internal constructor(val networkId: ByteArray) : NetworkIdentity {

    override fun matches(networkKey: NetworkKey) = networkKey.networkId.contentEquals(networkId) ||
            networkKey.oldNetworkId.contentEquals(networkId)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PublicNetworkIdentity

        if (!networkId.contentEquals(other.networkId)) return false

        return true
    }

    override fun hashCode(): Int {
        return networkId.contentHashCode()
    }
}

/**
 * Represents a Private Node Identity advertisement packet.
 *
 * @param hash    Function of the included random number and identity information.
 * @param random  64-bit random number.
 */
@ConsistentCopyVisibility
data class PrivateNetworkIdentity internal constructor(
    val hash: ByteArray, val random: ByteArray,
) : NetworkIdentity {

    override fun matches(networkKey: NetworkKey): Boolean {
        val data = networkKey.networkId + random
        val calculatedHash = Crypto.calculateHash(data, networkKey.derivatives.identityKey)
        if (calculatedHash.contentEquals(hash)) return true
        return networkKey.takeIf {
            it.oldDerivatives != null && it.oldNetworkId != null
        }?.let {
            val oldData = it.oldNetworkId!! + random
            hash.contentEquals(Crypto.calculateHash(oldData, it.oldDerivatives!!.identityKey))
        } ?: false
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PrivateNetworkIdentity

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
 * Creates the Network Identity from the given advertisement data.
 *
 * @receiver ByteArray Advertisement data.
 * @return NetworkIdentity if the data is valid, null otherwise.
 */
fun ByteArray.networkIdentity() = when (size) {
    9 if get(0) == 0x00.toByte() -> PublicNetworkIdentity(
        networkId = sliceArray(1 until 9)
    )

    // the type, the 64-bit hash and the 64-bit random
    17 if get(0) == 0x02.toByte() -> PrivateNetworkIdentity(
        hash = sliceArray(1 until 9),
        random = sliceArray(9 until 17)
    )

    else -> null
}