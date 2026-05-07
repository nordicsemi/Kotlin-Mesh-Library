package no.nordicsemi.android.nrfmesh.core.data.meshnetwork.simpleonoff.messages

import no.nordicsemi.android.nrfmesh.core.data.meshnetwork.simpleonoff.messages.SimpleOnOffSet.Initializer.opCode
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedVendorMessage
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer

/**
 * This message is used to set the current status of a On off status of SimpleOnOff vendor model
 * by Nordic Semiconductor.
 *
 * @property opCode The Op Code consists of:
 *                  0xC0-0000 - Vendor Op Code bitmask
 *                  0x03-0000 - The Op Code defined by...
 *                  0x00-5900 - Nordic Semiconductor ASA company ID (in Little Endian) as defined here:
 *                              https://www.bluetooth.com/specifications/assigned-numbers/company-identifiers/
 */
class SimpleOnOffSet(val isOn: Boolean) : AcknowledgedVendorMessage {
    override val opCode = Initializer.opCode
    override val responseOpCode = SimpleOnOffStatus.opCode
    override val parameters
        get() = byteArrayOf(if (isOn) 0x01 else 0x00)

    override fun toString() = "SimpleOnOffSet(isOn: $isOn)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0xC15900u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 }
            ?.let { params -> SimpleOnOffSet(isOn = params[0] == 0x01.toByte()) }
    }
}