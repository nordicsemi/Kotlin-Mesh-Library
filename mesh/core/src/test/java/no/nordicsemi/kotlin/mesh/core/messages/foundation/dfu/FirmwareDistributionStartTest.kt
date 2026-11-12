package no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu

import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdatePolicy
import no.nordicsemi.kotlin.mesh.core.messages.TransferMode
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs

class FirmwareDistributionStartTest {

    private fun params(octet5: Int) = byteArrayOf(
        0x01, 0x00,             // Application Key Index
        0x05,                   // TTL
        0x76, 0x00,             // Timeout Base
        octet5.toByte(),        // Transfer Mode | Update Policy
        0x02, 0x00,             // Firmware Image Index
        0x00, 0xC0.toByte()     // Multicast address 0xC000
    )

    private fun parse(octet5: Int) = assertIs<FirmwareDistributionStart>(
        FirmwareDistributionStart.init(params(octet5))
    )

    @Test
    fun testParsePullModeVerifyAndApply() {
        // Transfer Mode (bits 0-1) = 2, Update Policy (bit 2) = 1
        val message = parse(0x06)
        assertEquals(TransferMode.PULL, message.distributionTransferMode)
        assertEquals(FirmwareUpdatePolicy.VERIFY_AND_APPLY, message.updatePolicy)
    }

    @Test
    fun testParsePushModeVerifyOnly() {
        val message = parse(0x01)
        assertEquals(TransferMode.PUSH, message.distributionTransferMode)
        assertEquals(FirmwareUpdatePolicy.VERIFY_ONLY, message.updatePolicy)
    }

    @Test
    fun testParseRoundTrip() {
        val message = FirmwareDistributionStart(
            applicationKeyIndex = 1u,
            ttl = 5u,
            distributionTimeoutBase = 0x76u,
            distributionTransferMode = TransferMode.PULL,
            updatePolicy = FirmwareUpdatePolicy.VERIFY_AND_APPLY,
            firmwareImageIndex = 2u,
            address = 0xC000u
        )
        val parsed = parse(0x06)
        assertContentEquals(message.parameters, parsed.parameters)
    }
}
