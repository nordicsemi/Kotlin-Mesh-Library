package no.nordicsemi.android.nrfmesh.core.data.meshnetwork.simpleonoff.messages

import no.nordicsemi.android.nrfmesh.core.data.meshnetwork.simpleonoff.messages.SimpleOnOffGet.Initializer.opCode
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedVendorMessage
import no.nordicsemi.kotlin.mesh.core.messages.VendorMessageInitializer

/**
 * This message is used to request the current status of a On off status of SimpleOnOff vendor model
 * by Nordic Semiconductor.
 *
 * @property opCode The Op Code consists of:
 *                  0xC0-0000 - Vendor Op Code bitmask
 *                  0x03-0000 - The Op Code defined by...
 *                  0x00-5900 - Nordic Semiconductor ASA company ID (in Little Endian) as defined here:
 *                              https://www.bluetooth.com/specifications/assigned-numbers/company-identifiers/
 */
class SimpleOnOffGet : AcknowledgedVendorMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SimpleOnOffStatus.opCode
    override val parameters = null

    override fun toString() = "SimpleOnOffGet"

    companion object Initializer : VendorMessageInitializer {
        override val opCode = 0xC25900u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.isEmpty()
        }?.let {
            SimpleOnOffGet()
        }
    }
}