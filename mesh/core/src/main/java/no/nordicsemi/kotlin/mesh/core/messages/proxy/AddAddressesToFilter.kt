@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.proxy

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.model.ProxyFilterAddress
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress

/**
 * This is a message sent by a Proxy Client to add destination addresses to the proxy filter list.
 *
 * @property addresses List of addresses.
 */
class AddAddressesToFilter(
    val addresses: List<ProxyFilterAddress>,
) : AcknowledgedProxyConfigurationMessage {
    override val opCode: UByte = Initializer.opCode
    override val responseOpCode: UByte = FilterStatus.opCode
    override val parameters: ByteArray
        get() {
            var byteArray = ByteArray(0)
            // Send addresses sorted. The primary element will be added as the first one, in case
            // the Proxy Filter supports only on address.
            addresses.sortedBy { it.address }.forEach {
                byteArray += it.address.toByteArray()
            }
            return byteArray
        }

    override fun toString() =
        "AddAddressesToFilter(addresses: [${
            addresses.joinToString { it.toHexString() }
        }])"

    companion object Initializer : ProxyConfigurationMessageInitializer {
        override val opCode: UByte = 0x01u
        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size % 2 == 0
        }?.let { params ->
            val addresses = mutableListOf<UnicastAddress>()
            for(i in params.indices step 2) {
                addresses.add(element = UnicastAddress(params.getUShort(offset = i)))
            }
            AddAddressesToFilter(addresses = addresses)
        }
    }
}