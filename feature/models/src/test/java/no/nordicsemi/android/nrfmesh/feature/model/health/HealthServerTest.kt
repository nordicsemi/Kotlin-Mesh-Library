package no.nordicsemi.android.nrfmesh.feature.model.health

import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionGet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionSet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFault
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultGet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultTest
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultTestUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthPeriodGet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthPeriodSet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthPeriodSetUnacknowledged
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Tests the messages sent by the Health Server controls and the values shown by them. */
class HealthServerTest {

    @Test
    fun testAttentionSet() {
        val acknowledged = attentionSetOf(seconds = 30, acknowledged = true)
        assertIs<HealthAttentionSet>(acknowledged)
        assertEquals(30u.toUByte(), acknowledged.attentionTimer)
        val unacknowledged = attentionSetOf(seconds = 255, acknowledged = false)
        assertIs<HealthAttentionSetUnacknowledged>(unacknowledged)
        assertEquals(255u.toUByte(), unacknowledged.attentionTimer)
    }

    @Test
    fun testPeriodSet() {
        val acknowledged = periodSetOf(divisor = 4, acknowledged = true)
        assertIs<HealthPeriodSet>(acknowledged)
        assertEquals(4u.toUByte(), acknowledged.fastPeriodDivisor)
        val unacknowledged = periodSetOf(divisor = 15, acknowledged = false)
        assertIs<HealthPeriodSetUnacknowledged>(unacknowledged)
        assertEquals(15u.toUByte(), unacknowledged.fastPeriodDivisor)
    }

    @Test
    fun testOutOfRangeValuesAreClamped() {
        assertEquals(15u.toUByte(), (periodSetOf(20, true) as HealthPeriodSet).fastPeriodDivisor)
        assertEquals(0u.toUByte(), (attentionSetOf(-1, true) as HealthAttentionSet).attentionTimer)
    }

    @Test
    fun testFaultTest() {
        val acknowledged = faultTestOf(testId = 7u, companyId = 0x0059u, acknowledged = true)
        assertIs<HealthFaultTest>(acknowledged)
        assertEquals(7u.toUByte(), acknowledged.testId)
        assertEquals(0x0059u.toUShort(), acknowledged.companyIdentifier)
        val unacknowledged = faultTestOf(testId = 1u, companyId = 0x1234u, acknowledged = false)
        assertIs<HealthFaultTestUnacknowledged>(unacknowledged)
        assertEquals(0x1234u.toUShort(), unacknowledged.companyIdentifier)
    }

    @Test
    fun testReadMessagesAreInTheOrderOfTheirResponses() {
        val messages = healthReadMessages(companyId = 0x0059u)
        assertEquals(3, messages.size)
        assertIs<HealthAttentionGet>(messages[0])
        assertIs<HealthPeriodGet>(messages[1])
        assertEquals(0x0059u.toUShort(), assertIs<HealthFaultGet>(messages[2]).companyIdentifier)
        assertEquals(healthResponseOpCodes.size, messages.size)
    }

    @Test
    fun testFaultsAreNotReadWithoutACompanyId() {
        assertEquals(2, healthReadMessages(companyId = null).size)
    }

    @Test
    fun testFastPeriodIsAPowerOfTwo() {
        assertEquals("1", fastPeriodLabel(0u))
        assertEquals("16", fastPeriodLabel(4u))
        assertEquals("32768", fastPeriodLabel(15u))
    }

    @Test
    fun testCompanyIdParsing() {
        assertEquals(0x0059u.toUShort(), parseCompanyId("0059"))
        assertEquals(0xFFFFu.toUShort(), parseCompanyId("ffff"))
        assertEquals(0x59u.toUShort(), parseCompanyId(" 59 "))
        assertNull(parseCompanyId(""))
        assertNull(parseCompanyId("12345"))
        assertNull(parseCompanyId("XYZ"))
    }

    @Test
    fun testCompanyIdFormatting() {
        assertEquals("0059", companyIdText(NORDIC_COMPANY_ID))
        assertEquals("ABCD", companyIdText(0xABCDu))
    }

    @Test
    fun testTestIdParsing() {
        assertEquals(0u.toUByte(), parseTestId("0"))
        assertEquals(255u.toUByte(), parseTestId("255"))
        assertNull(parseTestId("256"))
        assertNull(parseTestId("-1"))
        assertNull(parseTestId(""))
    }

    @Test
    fun testFaultLabels() {
        assertEquals("Battery Low Warning", HealthFault.BatteryLowWarning.label())
        assertEquals("Overheat Error", HealthFault.OverheatError.label())
        assertEquals("No Fault", HealthFault.NoFault.label())
    }
}
