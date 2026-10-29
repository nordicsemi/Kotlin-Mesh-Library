package no.nordicsemi.android.nrfmesh.core.data.model.vendor.simpleonoff.message

import no.nordicsemi.kotlin.mesh.core.messages.VendorMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.VendorResponse

/**
 * This message is a response to a [SimpleOnOffGet] and [SimpleOnOffSet] that contains the current
 * status of a SimpleOnOffServer vendor model.
 *
 * @property isOn          Current value of the Simple OnOff state.
 *
 * @property opCode The Op Code consists of:
 *                  0xC0-0000 - Vendor Op Code bitmask
 *                  0x03-0000 - The Op Code defined by...
 *                  0x00-5900 - Nordic Semiconductor ASA company ID (in Little Endian) as defined here:
 *                              https://www.bluetooth.com/specifications/assigned-numbers/company-identifiers/
 */
class SimpleOnOffStatus(val isOn: Boolean) : VendorResponse {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() = byteArrayOf(if (isOn) 0x01 else 0x00)

    override fun toString() = "SimpleOnOffStatus(isOn: $isOn)"

    companion object Initializer : VendorMessageInitializer {
        override val opCode = 0xC45900u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 }
            ?.let { params ->
                SimpleOnOffStatus(isOn = params[0] == 0x01.toByte())
            }
    }
}