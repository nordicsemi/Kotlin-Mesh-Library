package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class FirmwareDistributionUploadParsingTest {

    @Test
    fun testUploadOobStartWithUriLongerThan127Bytes() {
        val uri = "https://example.com/" + "a".repeat(180) // 200 bytes
        val params = byteArrayOf(uri.length.toByte()) +
                uri.encodeToByteArray() +
                byteArrayOf(0x59, 0x00, 0x01, 0x02) // Company ID 0x0059, version 1.2

        val message = assertIs<FirmwareDistributionUploadOOBStart>(
            FirmwareDistributionUploadOOBStart.init(params)
        )
        assertEquals(uri, message.url.toString())
        assertEquals(0x0059u.toUShort(), message.currentFirmwareId.companyIdentifier)
        assertContentEquals(byteArrayOf(0x01, 0x02), message.currentFirmwareId.version)
    }

    @Test
    fun testUploadStartWithMetadataLongerThan127Bytes() {
        val metadata = ByteArray(200) { it.toByte() }
        val params = byteArrayOf(0xFF.toByte()) +           // TTL
                byteArrayOf(0x0A, 0x00) +                   // Timeout Base
                ByteArray(8) { 0x11 } +                     // BLOB ID
                byteArrayOf(0x00, 0x10, 0x00, 0x00) +       // Firmware Size
                byteArrayOf(metadata.size.toByte()) +       // Metadata Length (200)
                metadata +
                byteArrayOf(0x59, 0x00, 0x01, 0x02)         // Firmware ID

        val message = assertIs<FirmwareDistributionUploadStart>(
            FirmwareDistributionUploadStart.init(params)
        )
        assertContentEquals(metadata, message.metadata)
        assertEquals(0x0059u.toUShort(), message.firmwareId.companyIdentifier)
        assertContentEquals(byteArrayOf(0x01, 0x02), message.firmwareId.version)
    }

    @Test
    fun testUploadStartWithMetadataLongerThanMessageIsRejected() {
        val params = byteArrayOf(0xFF.toByte()) +           // TTL
                byteArrayOf(0x0A, 0x00) +                   // Timeout Base
                ByteArray(8) { 0x11 } +                     // BLOB ID
                byteArrayOf(0x00, 0x10, 0x00, 0x00) +       // Firmware Size
                byteArrayOf(200.toByte()) +                 // Metadata Length (200)
                ByteArray(10) +                             // Only 10 octets of metadata
                byteArrayOf(0x59, 0x00)                     // Firmware ID

        assertNull(FirmwareDistributionUploadStart.init(params))
    }
}
