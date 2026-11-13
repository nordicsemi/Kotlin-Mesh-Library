package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.mesh.core.messages.FirmwareId
import java.net.URI
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class FirmwareDistributionUploadOOBStartTest {

    private fun url(value: String) = URI(value).toURL()

    @Test
    fun testParametersStartWithUriLength() {
        val uri = "https://example.com/fw"
        val message = FirmwareDistributionUploadOOBStart(
            url = url(uri),
            currentFirmwareId = FirmwareId(
                companyIdentifier = 0x0059u,
                version = byteArrayOf(0x01, 0x02)
            )
        )

        assertContentEquals(
            byteArrayOf(uri.length.toByte()) + uri.encodeToByteArray() +
                    byteArrayOf(0x59, 0x00, 0x01, 0x02),
            message.parameters
        )
    }

    @Test
    fun testRoundTripWithoutVersion() {
        assertRoundTrip(version = byteArrayOf())
    }

    @Test
    fun testRoundTripWithOneByteVersion() {
        assertRoundTrip(version = byteArrayOf(0x07))
    }

    @Test
    fun testRoundTripWithLongerVersion() {
        assertRoundTrip(version = byteArrayOf(0x01, 0x02, 0x03, 0x04))
    }

    @Test
    fun testRoundTripWithMaximumLengthUri() {
        val prefix = "https://example.com/"
        val uri = prefix + "a".repeat(255 - prefix.length)
        assertEquals(255, uri.length)
        assertRoundTrip(uri = uri, version = byteArrayOf(0x01))
    }

    @Test
    fun testUriLongerThanMaximumIsRejected() {
        val uri = "https://example.com/" + "a".repeat(256)
        assertFailsWith<IllegalArgumentException> {
            FirmwareDistributionUploadOOBStart(
                url = url(uri),
                currentFirmwareId = FirmwareId(companyIdentifier = 0x0059u)
            )
        }
    }

    private fun assertRoundTrip(
        uri: String = "https://example.com/fw",
        version: ByteArray,
    ) {
        val message = FirmwareDistributionUploadOOBStart(
            url = url(uri),
            currentFirmwareId = FirmwareId(companyIdentifier = 0x0059u, version = version)
        )

        val parsed = assertIs<FirmwareDistributionUploadOOBStart>(
            FirmwareDistributionUploadOOBStart.init(message.parameters)
        )
        assertEquals(uri, parsed.url.toString())
        assertEquals(0x0059u.toUShort(), parsed.currentFirmwareId.companyIdentifier)
        assertContentEquals(version, parsed.currentFirmwareId.version)
    }
}
