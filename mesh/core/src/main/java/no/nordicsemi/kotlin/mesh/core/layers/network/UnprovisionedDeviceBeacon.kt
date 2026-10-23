@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package no.nordicsemi.kotlin.mesh.core.layers.network

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.getUuid
import no.nordicsemi.kotlin.mesh.core.oob.OobInformation
import kotlin.uuid.Uuid

/**
 * Class that defines an Unprovisioned device beacon PDU.
 *
 * @property pdu               Raw beacon PDU data.
 * @property deviceUuid        Uuid of the unprovisioned device.
 * @property oobInformation    OOB information.
 * @property uriHash           URI hash.
 * @property beaconType        Type of beacon.
 */
internal class UnprovisionedDeviceBeacon(
    override val pdu: ByteArray,
    val deviceUuid: Uuid,
    val oobInformation: OobInformation,
    val uriHash: ByteArray? = null
) : BeaconPdu {

    override val beaconType: BeaconType = BeaconType.UNPROVISIONED_DEVICE

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() = "Unprovisioned Device beacon (Uuid: $deviceUuid \n," +
            "OOB Information: $oobInformation \n, URI Hash: ${uriHash?.toHexString() ?: "null"})"
}

internal object UnprovisionedDeviceBeaconDecoder {

    /**
     * Decodes the unprovisioned device beacon.
     *
     * @param pdu Unprovisioned device beacon PDU.
     * @return an Unprovisioned Device beacon or null otherwise.
     */
    fun decode(pdu: ByteArray): UnprovisionedDeviceBeacon? = when {
        pdu.size > 1 -> when (BeaconType.from(pdu[0].toUByte())) {
            BeaconType.UNPROVISIONED_DEVICE -> {
                val uuid = pdu.getUuid(1)
                val oob = OobInformation.from(pdu.getUShort(17))
                val uriHash = when (pdu.size == 23) {
                    true -> pdu.copyOfRange(19, pdu.size)
                    false -> null
                }
                UnprovisionedDeviceBeacon(pdu,
                    deviceUuid = uuid,
                    oobInformation = oob,
                    uriHash = uriHash
                )
            }

            else -> null
        }

        else -> null
    }
}
