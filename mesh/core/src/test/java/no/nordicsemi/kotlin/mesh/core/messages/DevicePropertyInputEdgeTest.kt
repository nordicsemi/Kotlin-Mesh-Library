package no.nordicsemi.kotlin.mesh.core.messages

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The lowest and highest values, the encoding of them, and how text is read. */
class DevicePropertyInputEdgeTest {

    private fun DeviceProperty.accepts(vararg values: String) =
        characteristicOf(values.toList()) != null

    private fun DeviceProperty.bytes(vararg values: String): ByteArray =
        assertNotNull(characteristicOf(values.toList()), "$name did not accept ${values.toList()}")
            .data

    // MARK: - Ranges

    @Test
    fun testUvIndexAcceptsTheWholeByteRange() {
        assertTrue(DeviceProperty.UV_INDEX.accepts("0"))
        assertTrue(DeviceProperty.UV_INDEX.accepts("255"))
        assertFalse(DeviceProperty.UV_INDEX.accepts("256"))
        assertFalse(DeviceProperty.UV_INDEX.accepts("-1"))
    }

    @Test
    fun testCurrentRangeEndsAt655Point34() {
        assertTrue(DeviceProperty.PRESENT_INPUT_CURRENT.accepts("0"))
        assertTrue(DeviceProperty.PRESENT_INPUT_CURRENT.accepts("655.34"))
        assertFalse(DeviceProperty.PRESENT_INPUT_CURRENT.accepts("655.35"))
    }

    @Test
    fun testVoltageRangeEndsAt1022() {
        assertTrue(DeviceProperty.PRESENT_INPUT_VOLTAGE.accepts("1022"))
        assertFalse(DeviceProperty.PRESENT_INPUT_VOLTAGE.accepts("1022.01"))
        assertFalse(DeviceProperty.PRESENT_INPUT_VOLTAGE.accepts("-0.01"))
    }

    @Test
    fun testTemperaturesAcceptNegativeValues() {
        val temperature8 = DeviceProperty.PRESENT_AMBIENT_TEMPERATURE
        assertTrue(temperature8.accepts("-64"))
        assertTrue(temperature8.accepts("63"))
        assertFalse(temperature8.accepts("-64.5"))
        assertFalse(temperature8.accepts("63.5"))

        val temperature = DeviceProperty.PRECISE_PRESENT_AMBIENT_TEMPERATURE
        assertTrue(temperature.accepts("-273.15"))
        assertFalse(temperature.accepts("-273.16"))
        assertTrue(temperature.accepts("327.67"))
        assertFalse(temperature.accepts("327.68"))
    }

    @Test
    fun testCountsLeaveTheHighestValueForUnknown() {
        // 0xFFFF and 0xFFFFFF mean that the value is not known, so they cannot be entered.
        assertTrue(DeviceProperty.PEOPLE_COUNT.accepts("65534"))
        assertFalse(DeviceProperty.PEOPLE_COUNT.accepts("65535"))
        assertTrue(DeviceProperty.TOTAL_DEVICE_STARTS.accepts("16777214"))
        assertFalse(DeviceProperty.TOTAL_DEVICE_STARTS.accepts("16777215"))
    }

    @Test
    fun testSensingDurationCannotBeNegativeOrTooLong() {
        val property = DeviceProperty.AVERAGE_INPUT_CURRENT
        assertTrue(property.accepts("1", "66560641"))
        assertFalse(property.accepts("1", "66560642"))
        assertFalse(property.accepts("1", "-1"))
    }

    @Test
    fun testPowerRangeEndsAt1677721Point3() {
        // 0xFFFFFE means that the power is not valid, so 1677721.4 W cannot be entered.
        val power = DeviceProperty.PRESENT_DEVICE_INPUT_POWER
        assertTrue(power.accepts("1677721.3"))
        assertFalse(power.accepts("1677721.4"))
        // 1677721.3 W in steps of 0.1 W is 16777213 = 0xFFFFFD.
        assertContentEquals(
            byteArrayOf(0xFD.toByte(), 0xFF.toByte(), 0xFF.toByte()),
            power.bytes("1677721.3")
        )
    }

    @Test
    fun testIlluminanceRangeEndsAt167772Point14() {
        val illuminance = DeviceProperty.PRESENT_ILLUMINANCE
        assertTrue(illuminance.accepts("167772.14"))
        assertFalse(illuminance.accepts("167772.15"))
        // 167772.14 lux in steps of 0.01 lux is 16777214 = 0xFFFFFE.
        assertContentEquals(
            byteArrayOf(0xFE.toByte(), 0xFF.toByte(), 0xFF.toByte()),
            illuminance.bytes("167772.14")
        )
        // The highest value is shown as it is, and not as the one below it.
        assertEquals(
            "167772.14 lux",
            DevicePropertyCharacteristic.Illuminance(BigDecimal("167772.14")).description
        )
    }

    // MARK: - Encoding

    @Test
    fun testTheLimitsAreEncodedToTheirRawValues() {
        assertContentEquals(byteArrayOf(0xFF.toByte()), DeviceProperty.UV_INDEX.bytes("255"))
        // 655.34 A is 65534 = 0xFFFE, sent least significant byte first.
        assertContentEquals(
            byteArrayOf(0xFE.toByte(), 0xFF.toByte()),
            DeviceProperty.PRESENT_INPUT_CURRENT.bytes("655.34")
        )
        // 1022 V in steps of 1/64 V is 65408 = 0xFF80.
        assertContentEquals(
            byteArrayOf(0x80.toByte(), 0xFF.toByte()),
            DeviceProperty.PRESENT_INPUT_VOLTAGE.bytes("1022")
        )
        // A temperature in steps of 0.5 degrees is a signed byte: -64 is -128 and 63 is 126.
        assertContentEquals(
            byteArrayOf(0x80.toByte()),
            DeviceProperty.PRESENT_AMBIENT_TEMPERATURE.bytes("-64")
        )
        assertContentEquals(
            byteArrayOf(0x7E),
            DeviceProperty.PRESENT_AMBIENT_TEMPERATURE.bytes("63")
        )
    }

    @Test
    fun testAValueBetweenTwoStepsIsRoundedDown() {
        // 12.349 A is 1234.9 steps of 0.01 A, which is encoded as 1234 = 0x04D2.
        assertContentEquals(
            byteArrayOf(0xD2.toByte(), 0x04),
            DeviceProperty.PRESENT_INPUT_CURRENT.bytes("12.349")
        )
    }

    @Test
    fun testSensingDurationIsRoundedToTheNearestEncodedTime() {
        // The time is stored as 1.1^(N-64) seconds, so 55 seconds becomes 54.764 seconds.
        val characteristic = DeviceProperty.AVERAGE_INPUT_CURRENT.characteristicOf(listOf("23", "55"))
        assertTrue(
            characteristic?.description?.endsWith("averaged over 54.764 seconds") == true,
            characteristic?.description
        )
        // One byte follows the two of the current.
        assertEquals(3, characteristic?.data?.size)
    }

    @Test
    fun testALongSensingDurationIsDescribedAsAMinuteOrMore() {
        // 1.1^43 is 60.2 seconds.
        assertTrue(TimeExponential.RawValue(107u).toString().startsWith("1m"))
        // 1.1^42 is 54.764 seconds, which is still described in seconds.
        assertEquals("54.764 seconds", TimeExponential.RawValue(106u).toString())
    }

    // MARK: - Text

    @Test
    fun testSpacesAroundANumberAreIgnored() {
        assertTrue(DeviceProperty.UV_INDEX.accepts(" 3 "))
        assertTrue(DeviceProperty.PRESENT_INPUT_CURRENT.accepts(" 12.5"))
    }

    @Test
    fun testAWholeNumberMayEndWithZeroes() {
        assertTrue(DeviceProperty.UV_INDEX.accepts("3.0"))
        assertFalse(DeviceProperty.UV_INDEX.accepts("3.1"))
    }

    @Test
    fun testBlankAndIncompleteTextIsRejected() {
        assertFalse(DeviceProperty.PRESENT_INPUT_CURRENT.accepts("   "))
        assertFalse(DeviceProperty.PRESENT_INPUT_CURRENT.accepts("."))
        assertFalse(DeviceProperty.PRESENT_INPUT_CURRENT.accepts("-"))
        assertFalse(DeviceProperty.PRESENT_INPUT_CURRENT.accepts("1.2.3"))
    }

    @Test
    fun testABooleanIgnoresCaseAndSpaces() {
        assertEquals(
            DevicePropertyCharacteristic.Bool(true),
            DeviceProperty.PRESENCE_DETECTED.characteristicOf(listOf(" TRUE "))
        )
        assertEquals(
            DevicePropertyCharacteristic.Bool(false),
            DeviceProperty.PRESENCE_DETECTED.characteristicOf(listOf("False"))
        )
        assertNull(DeviceProperty.PRESENCE_DETECTED.characteristicOf(listOf("")))
    }

    // MARK: - Fields

    @Test
    fun testEveryFieldHasANameAndARangeUnlessItIsABoolean() {
        DeviceProperty.entries.mapNotNull { it.inputFields }.flatten().forEach { field ->
            assertTrue(field.name.isNotBlank())
            when (field.type) {
                PropertyFieldType.BOOLEAN -> assertNull(field.range)
                else -> assertNotNull(field.range, "${field.name} has no range")
            }
        }
    }
}
