package no.nordicsemi.kotlin.mesh.core.messages

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DevicePropertyInputTest {

    @Test
    fun testCurrentIsEnteredAsAmperes() {
        val characteristic = DeviceProperty.PRESENT_INPUT_CURRENT.characteristicOf(listOf("100.0"))
        // 100.0 A with a resolution of 0.01 A is 10000 = 0x2710, sent least significant byte first.
        assertContentEquals(byteArrayOf(0x10, 0x27), characteristic?.data)
    }

    @Test
    fun testAverageCurrentTakesAValueAndASensingDuration() {
        val property = DeviceProperty.AVERAGE_INPUT_CURRENT
        assertEquals(listOf("Value", "Sensing duration"), property.inputFields?.map { it.name })
        assertEquals(listOf("A", "s"), property.inputFields?.map { it.unit })
        val characteristic = property.characteristicOf(listOf("100.0", "0"))
        // 0 seconds is the raw value 0.
        assertContentEquals(byteArrayOf(0x10, 0x27, 0x00), characteristic?.data)
    }

    @Test
    fun testSensingDurationIsDescribedInSeconds() {
        val current = DeviceProperty.AVERAGE_INPUT_CURRENT
        assertEquals(
            "100.0 A averaged over 0 seconds",
            current.characteristicOf(listOf("100.0", "0"))?.description
        )
        // 1.1^0 = 1 second is the raw value 64.
        assertEquals("1 second", TimeExponential.RawValue(64u).toString())
        assertEquals("1.1 seconds", TimeExponential.RawValue(65u).toString())
        assertEquals("the total device lifetime", TimeExponential.DeviceLifetime.toString())
    }

    @Test
    fun testAverageValueWithAnUnknownDuration() {
        val characteristic = DevicePropertyCharacteristic.AverageVoltage(BigDecimal("12"), null)
        assertEquals("12.0 V averaged over an unknown time", characteristic.description)
    }

    @Test
    fun testUvIndexAndPresenceAreSimpleValues() {
        assertEquals(
            DevicePropertyCharacteristic.UvIndex(3u),
            DeviceProperty.UV_INDEX.characteristicOf(listOf("3"))
        )
        assertEquals(
            DevicePropertyCharacteristic.Bool(true),
            DeviceProperty.PRESENCE_DETECTED.characteristicOf(listOf("true"))
        )
        assertEquals(
            PropertyFieldType.BOOLEAN,
            DeviceProperty.PRESENCE_DETECTED.inputFields?.single()?.type
        )
    }

    @Test
    fun testValuesOutsideTheRangeAreRejected() {
        assertNull(DeviceProperty.PRESENT_INPUT_CURRENT.characteristicOf(listOf("700")))
        assertNull(DeviceProperty.PRESENT_INPUT_CURRENT.characteristicOf(listOf("-1")))
        assertNull(DeviceProperty.UV_INDEX.characteristicOf(listOf("256")))
        assertNull(DeviceProperty.PEOPLE_COUNT.characteristicOf(listOf("65535")))
    }

    @Test
    fun testTextThatIsNotANumberIsRejected() {
        assertNull(DeviceProperty.PRESENT_INPUT_CURRENT.characteristicOf(listOf("abc")))
        assertNull(DeviceProperty.PRESENT_INPUT_CURRENT.characteristicOf(listOf("")))
        assertNull(DeviceProperty.UV_INDEX.characteristicOf(listOf("1.5")))
        assertNull(DeviceProperty.PRESENCE_DETECTED.characteristicOf(listOf("yes")))
    }

    @Test
    fun testTheNumberOfValuesMustMatchTheFields() {
        assertNull(DeviceProperty.AVERAGE_INPUT_CURRENT.characteristicOf(listOf("100.0")))
        assertNull(DeviceProperty.UV_INDEX.characteristicOf(listOf("1", "2")))
        assertNull(DeviceProperty.UV_INDEX.characteristicOf(emptyList()))
    }

    @Test
    fun testPropertiesWithoutATypedCharacteristicHaveNoFields() {
        val property = DeviceProperty.AVERAGE_AMBIENT_TEMPERATURE_IN_A_PERIOD_OF_DAY
        assertNull(property.valueLength)
        assertNull(property.inputFields)
        assertNull(property.characteristicOf(listOf("1")))
    }

    @Test
    fun testFieldsHaveTheRangeOfTheirCharacteristic() {
        val field = DeviceProperty.PRESENT_INPUT_CURRENT.inputFields?.single()
        assertEquals(BigDecimal("0")..BigDecimal("655.34"), field?.range)
        assertEquals("A", field?.unit)
    }

    @Test
    fun testEverySupportedPropertyIsEncodedToItsLength() {
        val supported = DeviceProperty.entries.filter { it.inputFields != null }
        assertTrue(supported.size > 20, "Only ${supported.size} properties are supported")
        supported.forEach { property ->
            val fields = assertNotNull(property.inputFields)
            val values = fields.map {
                if (it.type == PropertyFieldType.BOOLEAN) "false"
                else assertNotNull(it.range).start.toPlainString()
            }
            val characteristic = assertNotNull(
                property.characteristicOf(values),
                "${property.name} was not created from $values"
            )
            assertEquals(
                property.valueLength,
                characteristic.data.size,
                "${property.name} was not encoded to its length"
            )
            // Decoding what was encoded gives the same value.
            val decoded = property.read(characteristic.data, 0, characteristic.data.size)
            assertEquals(characteristic.description, decoded.description, property.name)
        }
    }
}
