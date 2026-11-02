@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.layers.network

import no.nordicsemi.kotlin.data.getUInt
import no.nordicsemi.kotlin.data.hasBitSet
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.KeyDistribution
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.crypto.Crypto

/**
 * Defines Private Beacon transmitted by the mesh network.
 */
internal class PrivateBeacon(
    override val pdu: ByteArray,
    override val networkKey: NetworkKey,
    override val validForKeyRefreshProcedure: Boolean,
    override val keyRefreshFlag: Boolean,
    override val ivIndex: IvIndex
) : NetworkBeaconPdu {
    override val beaconType = BeaconType.PRIVATE

    override fun toString() = "Secure Network beacon (" +
            "Network ID: ${networkKey.networkId.toHexString()}, " +
            "IV Index: $ivIndex, " +
            "Key Refresh Flag: $keyRefreshFlag)"
}

internal object PrivateBeaconDecoder {

    /**
     * Decodes the private beacon
     *
     * @param pdu          Private beacon pdu.
     * @param networkKey   Network key to decode with.
     * @return PrivateBeacon or null if the beacon could not be decoded or authenticated.
     */
    fun decode(pdu: ByteArray, networkKey: NetworkKey): PrivateBeacon? {
        require(pdu.size == 27 && pdu[0] == 2.toByte()) { return null }

        val privateBeaconData = networkKey.derivatives.let { derivatives ->
            Crypto.decodeAndAuthenticate(
                pdu = pdu, privateBeaconKey = derivatives.privateBeaconKey
            )
        } ?: let {
            val oldDerivatives = networkKey.oldDerivatives
            require(networkKey.phase == KeyDistribution && oldDerivatives != null) {
                return null
            }

            Crypto.decodeAndAuthenticate(
                pdu = pdu, privateBeaconKey = oldDerivatives.privateBeaconKey
            )
        }

        require(privateBeaconData != null) { return null }

        return privateBeaconData.let {
            val flags = it.first
            val keyRefreshFlag = flags hasBitSet 0
            val updateActive = flags hasBitSet 1
            val index = it.second.getUInt(offset = 0)
            val ivIndex = IvIndex(index = index, isIvUpdateActive = updateActive)
            PrivateBeacon(
                pdu = pdu,
                networkKey = networkKey,
                validForKeyRefreshProcedure = true,
                keyRefreshFlag = keyRefreshFlag,
                ivIndex = ivIndex
            )
        }
    }
}