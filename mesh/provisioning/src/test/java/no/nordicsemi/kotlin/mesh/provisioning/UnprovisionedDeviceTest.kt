package no.nordicsemi.kotlin.mesh.provisioning

import no.nordicsemi.kotlin.data.toHexString
import no.nordicsemi.kotlin.mesh.core.oob.OobInformation
import no.nordicsemi.kotlin.mesh.core.util.Utils
import org.junit.Test

class UnprovisionedDeviceTest {

    @Test
    fun testFrom() {
        val expectedDevice = UnprovisionedDevice(
            name = "Mesh Light",
            uuid = Utils.decode(
                byteArrayOf(
                    0xA4.toByte(), 0x60, 0x72, 0x16, 0x93.toByte(), 0x2B, 0x4E, 0x0E, 0x9B.toByte(),
                    0x9F.toByte(), 0x8D.toByte(), 0xE9.toByte(), 0x6C, 0xD4.toByte(), 0xE1.toByte(),
                    0xF1.toByte()
                ).toHexString()
            ),
            oobInformation = OobInformation.None
        )
        val advertisementData = byteArrayOf(
            0x02, 0x01, 0x06, 0x03, 0x03, 0x27, 0x18, 0x15, 0x16, 0x27, 0x18, 0xA4.toByte(), 0x60,
            0x72, 0x16, 0x93.toByte(), 0x2B, 0x4E, 0x0E, 0x9B.toByte(), 0x9F.toByte(),
            0x8D.toByte(), 0xE9.toByte(), 0x6C, 0xD4.toByte(), 0xE1.toByte(), 0xF1.toByte(), 0x00,
            0x00, 0x0B, 0x09, 0x4D, 0x65, 0x73, 0x68, 0x20, 0x4C, 0x69, 0x67, 0x68, 0x74
        )
        val unprovisionedDevice = UnprovisionedDevice.from(advertisementData)
        assert(expectedDevice == unprovisionedDevice)
    }
}