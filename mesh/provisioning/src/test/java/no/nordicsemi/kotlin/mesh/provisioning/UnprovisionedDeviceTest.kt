package no.nordicsemi.kotlin.mesh.provisioning

import no.nordicsemi.kotlin.data.toHexString
import no.nordicsemi.kotlin.mesh.core.oob.OobInformation
import no.nordicsemi.kotlin.mesh.core.util.Utils
import org.junit.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertFailsWith

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

    @Test
    fun testFromIgnoresUnknownAdsAndShortenedName() {
        val advertisementData = byteArrayOf(
            0x02, 0x01, 0x06, 0x02, 0x0A, 0x04, // Flags, TX power
            0x03, 0x03, 0x27, 0x18,
            0x15, 0x16, 0x27, 0x18, 0xA4.toByte(), 0x60,
            0x72, 0x16, 0x93.toByte(), 0x2B, 0x4E, 0x0E, 0x9B.toByte(), 0x9F.toByte(),
            0x8D.toByte(), 0xE9.toByte(), 0x6C, 0xD4.toByte(), 0xE1.toByte(), 0xF1.toByte(), 0x00,
            0x00,
            0x04, 0xFF.toByte(), 0x01, 0x02, 0x03, // Manufacturer data
            0x07, 0x08, 0x4D, 0x75, 0x72, 0x61, 0x74, 0x61 // Shortened name "Murata"
        )
        val device = UnprovisionedDevice.from(advertisementData)
        assert(device.name == "Murata")
    }

    @Test
    fun testFromWithoutOobInformation() {
        val advertisementData = byteArrayOf(
            0x13, 0x16, 0x27, 0x18, 0xA4.toByte(), 0x60,
            0x72, 0x16, 0x93.toByte(), 0x2B, 0x4E, 0x0E, 0x9B.toByte(), 0x9F.toByte(),
            0x8D.toByte(), 0xE9.toByte(), 0x6C, 0xD4.toByte(), 0xE1.toByte(), 0xF1.toByte()
        )
        assert(UnprovisionedDevice.from(advertisementData).uuid.toString().isNotEmpty())
    }

    private val deviceUuidBytes = byteArrayOf(
        0xA4.toByte(), 0x60, 0x72, 0x16, 0x93.toByte(), 0x2B, 0x4E, 0x0E, 0x9B.toByte(),
        0x9F.toByte(), 0x8D.toByte(), 0xE9.toByte(), 0x6C, 0xD4.toByte(), 0xE1.toByte(),
        0xF1.toByte()
    )
    private val deviceUuid = Utils.decode(deviceUuidBytes.toHexString())

    /** Service data AD with the provisioning service UUID, device UUID and the given extra bytes. */
    private fun serviceData(extra: ByteArray = byteArrayOf()) =
        byteArrayOf((1 + 2 + 16 + extra.size).toByte(), 0x16, 0x27, 0x18) + deviceUuidBytes + extra

    @Test
    fun testFromWithUriAdAndUriHash() {
        val advertisementData = byteArrayOf(0x02, 0x01, 0x06) +
                serviceData(byteArrayOf(0x00, 0x00, 0x01, 0x02, 0x03, 0x04)) +
                byteArrayOf(0x06, 0x24, 0x17, 0x2F, 0x2F, 0x61, 0x62) // URI AD
        val device = UnprovisionedDevice.from(advertisementData)
        assertEquals(deviceUuid, device.uuid)
        assertEquals(OobInformation.None, device.oobInformation)
        assertContentEquals(byteArrayOf(0x01, 0x02, 0x03, 0x04), device.uriHash)
    }

    @Test
    fun testFromWithoutUriHash() {
        assertNull(UnprovisionedDevice.from(serviceData(byteArrayOf(0x00, 0x00))).uriHash)
        assertNull(UnprovisionedDevice.from(serviceData()).uriHash)
    }

    @Test
    fun testFromWithIncompleteUriHashIgnoresIt() {
        val device = UnprovisionedDevice.from(serviceData(byteArrayOf(0x00, 0x00, 0x01, 0x02)))
        assertNull(device.uriHash)
    }

    @Test
    fun testFromParsesOobInformation() {
        val device = UnprovisionedDevice.from(serviceData(byteArrayOf(0x00, 0x00)))
        assertEquals(OobInformation.from(0u), device.oobInformation)
    }

    @Test
    fun testFromWithoutOobBytesDoesNotThrow() {
        val device = UnprovisionedDevice.from(serviceData())
        assertEquals(deviceUuid, device.uuid)
        assertEquals(OobInformation.Other, device.oobInformation)
    }

    @Test
    fun testFromPrefersCompleteNameOverShortenedName() {
        val shortened = byteArrayOf(0x04, 0x08, 0x4D, 0x75, 0x72)
        val complete = byteArrayOf(0x07, 0x09, 0x4D, 0x75, 0x72, 0x61, 0x74, 0x61)
        assertEquals(
            "Murata",
            UnprovisionedDevice.from(serviceData() + shortened + complete).name
        )
        assertEquals(
            "Murata",
            UnprovisionedDevice.from(serviceData() + complete + shortened).name
        )
    }

    @Test
    fun testFromIgnoresServiceDataOfOtherServices() {
        // Service data of the proxy service (0x1828) must not be parsed as a device UUID.
        val other = byteArrayOf(0x13, 0x16, 0x28, 0x18) + deviceUuidBytes
        assertFailsWith<IllegalArgumentException> { UnprovisionedDevice.from(other) }
    }

    @Test
    fun testFromWithoutServiceDataFails() {
        val advertisementData = byteArrayOf(0x02, 0x01, 0x06, 0x03, 0x03, 0x27, 0x18)
        assertFailsWith<IllegalArgumentException> { UnprovisionedDevice.from(advertisementData) }
    }

    @Test
    fun testFromWithTooShortServiceDataFails() {
        val advertisementData = byteArrayOf(0x05, 0x16, 0x27, 0x18, 0x01, 0x02)
        assertFailsWith<IllegalArgumentException> { UnprovisionedDevice.from(advertisementData) }
    }

    @Test
    fun testFromWithTruncatedAdStopsParsing() {
        // The last AD claims more bytes than available, the valid ones before it are kept.
        val advertisementData = serviceData() + byteArrayOf(0x20, 0x09, 0x41)
        val device = UnprovisionedDevice.from(advertisementData)
        assertEquals(deviceUuid, device.uuid)
        assertEquals("", device.name)
    }

    @Test
    fun testFromWithZeroLengthAdStopsParsing() {
        val advertisementData = serviceData() + byteArrayOf(0x00, 0x09, 0x41, 0x42)
        assertEquals("", UnprovisionedDevice.from(advertisementData).name)
    }

    @Test
    fun testFromWithLargeLengthDoesNotThrowIndexError() {
        // A length of 0x80 or more is negative when read as a signed byte.
        val advertisementData = serviceData() + byteArrayOf(0x80.toByte(), 0xFF.toByte(), 0x01)
        assertEquals(deviceUuid, UnprovisionedDevice.from(advertisementData).uuid)
    }

    @Test
    fun testFromWithEmptyAdvertisementFails() {
        assertFailsWith<IllegalArgumentException> { UnprovisionedDevice.from(byteArrayOf()) }
    }

    /**
     * Advertisement of the muRata sensor, captured on a device. The service UUID list is
     * advertised as Incomplete (0x02) which the parser used to reject.
     */
    @Test
    fun testFromMurataSensor() {
        val advertisementData =
            "0201060302271815162718c43f6337850fd3929544a17cdb55a27100000e096d75526174612073656e736f72"
                .hexToByteArray()
        val device = UnprovisionedDevice.from(advertisementData)
        assertEquals("muRata sensor", device.name)
        assertEquals(Utils.decode("c43f6337850fd3929544a17cdb55a271"), device.uuid)
        assertNull(device.uriHash)
    }
}
