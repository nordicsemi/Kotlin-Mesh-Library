@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.layers.network

import no.nordicsemi.kotlin.data.getUInt
import no.nordicsemi.kotlin.data.hasBitSet
import no.nordicsemi.kotlin.data.toHexString
import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.KeyDistribution
import no.nordicsemi.kotlin.mesh.core.model.NetworkKey
import no.nordicsemi.kotlin.mesh.crypto.Crypto

/**
 * Defines Secure Network Beacon transmitted by the mesh network.
 */
internal class SecureNetworkBeacon(
    override val pdu: ByteArray,
    override val networkKey: NetworkKey,
    override val validForKeyRefreshProcedure: Boolean,
    override val keyRefreshFlag: Boolean,
    override val ivIndex: IvIndex,
) : NetworkBeaconPdu {

    override val beaconType: BeaconType = BeaconType.SECURE_NETWORK

    override fun toString(): String {
        return "SecureNetworkBeacon(pdu: ${
            pdu.toHexString(
                format = HexFormat {
                    number.prefix = "0x"
                    upperCase = true
                }
            )
        }, Network Key Index: ${networkKey.index}, " +
        "validForKeyRefreshProcedure: $validForKeyRefreshProcedure, " +
        "keyRefreshFlag: $keyRefreshFlag, ivIndex=$ivIndex)"
    }
}

internal object SecureNetworkBeaconDecoder {

    fun decode(pdu: ByteArray, networkKey: NetworkKey): SecureNetworkBeacon? {

        require(pdu.size == 22 && pdu[0].toInt() == 1) { return null }

        val keyRefreshFlag = pdu[1].hasBitSet(bit = 0)
        val updateActive = pdu[1].hasBitSet(bit = 1)
        val networkId = pdu.copyOfRange(fromIndex = 2, toIndex = 10)
        val index = pdu.getUInt(offset = 10)
        val ivIndex = IvIndex(index = index, isIvUpdateActive = updateActive)
        val validForKeyRefreshProcedure = networkKey.oldKey != null

        when {
            networkId.contentEquals(networkKey.networkId) -> {
                require(Crypto.authenticate(pdu, networkKey.derivatives.beaconKey)) {
                    return null
                }
                return SecureNetworkBeacon(
                    pdu = pdu,
                    networkKey = networkKey,
                    validForKeyRefreshProcedure = validForKeyRefreshProcedure,
                    keyRefreshFlag = keyRefreshFlag,
                    ivIndex = ivIndex
                )
            }

            networkKey.phase == KeyDistribution &&
                    networkId.contentEquals(networkKey.oldNetworkId) &&
                    networkKey.oldKey != null -> {
                require(Crypto.authenticate(pdu, networkKey.derivatives.beaconKey)) {
                    return null
                }
                return SecureNetworkBeacon(
                    pdu = pdu,
                    networkKey = networkKey,
                    validForKeyRefreshProcedure = validForKeyRefreshProcedure,
                    keyRefreshFlag = keyRefreshFlag,
                    ivIndex = ivIndex
                )
            }

            else -> return null
        }
    }
}