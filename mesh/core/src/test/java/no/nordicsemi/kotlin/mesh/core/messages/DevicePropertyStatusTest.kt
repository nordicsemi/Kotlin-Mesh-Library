package no.nordicsemi.kotlin.mesh.core.messages

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The characteristics of the properties added by the latest specification. */
class DevicePropertyStatusTest {

    private fun read(property: DeviceProperty, vararg bytes: Int): DevicePropertyCharacteristic =
        property.read(bytes.map { it.toByte() }.toByteArray(), 0, bytes.size)

    private fun assertRoundTrip(property: DeviceProperty, vararg bytes: Int) {
        val value = read(property, *bytes)
        assertContentEquals(bytes.map { it.toByte() }.toByteArray(), value.data, property.name)
    }

    private fun assertDecimal(expected: String, actual: BigDecimal?) {
        assertNotNull(actual)
        assertEquals(0, BigDecimal(expected).compareTo(actual), "expected $expected but was $actual")
    }

    // MARK: - Acceleration

    @Test
    fun testAccelerationHasThreeAxes() {
        // 1.00 g, -1.00 g, out of range and unknown would be 0x64, 0x9C, 0x7E and 0x7F.
        val value = read(DeviceProperty.ACCELERATION_3D_SENSOR_STATUS, 0x64, 0x9C, 0x7E)
        assertIs<DevicePropertyCharacteristic.Acceleration3d>(value)
        assertDecimal("1.00", (value.x as ValidDecimal.Valid).value)
        assertDecimal("-1.00", (value.y as ValidDecimal.Valid).value)
        assertEquals(ValidDecimal.Invalid, value.z)
        assertRoundTrip(DeviceProperty.ACCELERATION_3D_SENSOR_STATUS, 0x64, 0x9C, 0x7E)
    }

    @Test
    fun testAnAccelerationAxisCanBeUnknownOrOutOfRange() {
        val value = read(DeviceProperty.ACCELERATION_3D_SENSOR_STATUS, 0x00, 0x7E, 0x7F)
        assertIs<DevicePropertyCharacteristic.Acceleration3d>(value)
        assertNull(value.z)
        assertTrue(value.description.contains("out of range"))
        assertTrue(value.description.contains("Z: unknown"))
        assertRoundTrip(DeviceProperty.ACCELERATION_3D_SENSOR_STATUS, 0x00, 0x7E, 0x7F)
    }

    @Test
    fun testPreciseAccelerationUsesTwoOctetsPerAxis() {
        // 1.000 g is 1000 = 0x03E8, -1.000 g is 0xFC18, then out of range and unknown.
        val bytes = intArrayOf(0xE8, 0x03, 0x18, 0xFC, 0xFE, 0x7F)
        val value = read(DeviceProperty.PRECISE_ACCELERATION_3D_SENSOR_STATUS, *bytes)
        assertIs<DevicePropertyCharacteristic.PreciseAcceleration3d>(value)
        assertDecimal("1.000", (value.x as ValidDecimal.Valid).value)
        assertDecimal("-1.000", (value.y as ValidDecimal.Valid).value)
        assertEquals(ValidDecimal.Invalid, value.z)
        assertRoundTrip(DeviceProperty.PRECISE_ACCELERATION_3D_SENSOR_STATUS, *bytes)
    }

    @Test
    fun testAccelerationDetection() {
        assertEquals(
            DevicePropertyCharacteristic.AccelerationDetection(true),
            read(DeviceProperty.ACCELERATION_DETECTION_STATUS, 0x01)
        )
        assertEquals(
            DevicePropertyCharacteristic.AccelerationDetection(false),
            read(DeviceProperty.ACCELERATION_DETECTION_STATUS, 0x00)
        )
        assertRoundTrip(DeviceProperty.ACCELERATION_DETECTION_STATUS, 0x01)
    }

    // MARK: - Status

    @Test
    fun testContactsAreBitsStartingWithContactZero() {
        // Bits 0 and 3 are set, so contacts 0 and 3 are closed.
        val value = read(DeviceProperty.CONTACT_STATUS, 0b0000_1001)
        assertIs<DevicePropertyCharacteristic.ContactStatus>(value)
        assertEquals(listOf(true, false, false, true, false, false, false, false), value.contacts)
        assertEquals("Closed: 0, 3", value.description)
        assertRoundTrip(DeviceProperty.CONTACT_STATUS, 0b0000_1001)
        assertEquals("All contacts are open", read(DeviceProperty.CONTACT_STATUS, 0).description)
    }

    @Test
    fun testButtonsUseTwoBitsEachStartingWithButtonZero() {
        // Button 0 is not actuated (0), button 1 pressed (1), button 2 released (2), button 3 held (3).
        val value = read(DeviceProperty.PUSH_BUTTON_STATUS, 0b11_10_01_00)
        assertIs<DevicePropertyCharacteristic.PushButtonStatus>(value)
        assertEquals(
            listOf(ButtonStatus.NOT_ACTUATED, ButtonStatus.PRESSED, ButtonStatus.RELEASED, ButtonStatus.HELD),
            value.buttons
        )
        assertTrue(value.description.contains("Button 1: pressed"))
        assertTrue(value.description.contains("Button 3: held"))
        assertRoundTrip(DeviceProperty.PUSH_BUTTON_STATUS, 0b11_10_01_00)
    }

    @Test
    fun testDoorAndWindowStates() {
        assertEquals(DoorWindowState.OPEN, (read(DeviceProperty.DOOR_WINDOW_STATUS, 0) as DevicePropertyCharacteristic.DoorWindowStatus).state)
        assertEquals(DoorWindowState.CLOSED, (read(DeviceProperty.DOOR_WINDOW_STATUS, 1) as DevicePropertyCharacteristic.DoorWindowStatus).state)
        assertEquals("Tilted/Ajar", read(DeviceProperty.DOOR_WINDOW_STATUS, 2).description)
        // The values from 3 are reserved, and are sent back as they were received.
        val reserved = read(DeviceProperty.DOOR_WINDOW_STATUS, 3)
        assertNull((reserved as DevicePropertyCharacteristic.DoorWindowStatus).state)
        assertEquals("Reserved (0x03)", reserved.description)
        assertRoundTrip(DeviceProperty.DOOR_WINDOW_STATUS, 3)
        assertEquals(
            DevicePropertyCharacteristic.DoorWindowStatus(0x02u),
            DevicePropertyCharacteristic.DoorWindowStatus(DoorWindowState.TILTED)
        )
    }

    // MARK: - Numbers

    @Test
    fun testCoarseHumidityIsInStepsOfHalfAPercent() {
        val full = read(DeviceProperty.COARSE_AMBIENT_RELATIVE_HUMIDITY, 0xC8)
        assertDecimal("100", (full as DevicePropertyCharacteristic.Humidity8).value)
        assertDecimal("0.5", (read(DeviceProperty.COARSE_AMBIENT_RELATIVE_HUMIDITY, 0x01) as DevicePropertyCharacteristic.Humidity8).value)
        assertNull((read(DeviceProperty.COARSE_AMBIENT_RELATIVE_HUMIDITY, 0xFF) as DevicePropertyCharacteristic.Humidity8).value)
        assertRoundTrip(DeviceProperty.COARSE_AMBIENT_RELATIVE_HUMIDITY, 0xC8)
        assertRoundTrip(DeviceProperty.COARSE_AMBIENT_RELATIVE_HUMIDITY, 0xFF)
    }

    @Test
    fun testCoarseLightLevelIsInWholeLux() {
        val value = read(DeviceProperty.COARSE_PRESENT_AMBIENT_LIGHT_LEVEL, 0xFE, 0xFF)
        assertEquals(DevicePropertyCharacteristic.Illuminance16(65534u), value)
        assertEquals("65534 lux", value.description)
        assertNull((read(DeviceProperty.COARSE_PRESENT_AMBIENT_LIGHT_LEVEL, 0xFF, 0xFF) as DevicePropertyCharacteristic.Illuminance16).value)
        assertRoundTrip(DeviceProperty.COARSE_PRESENT_AMBIENT_LIGHT_LEVEL, 0xFE, 0xFF)
    }

    @Test
    fun testMotionThresholdStepsAreCountedFromOne() {
        assertEquals(
            DevicePropertyCharacteristic.Percentage8Steps(200u),
            read(DeviceProperty.MOTION_THRESHOLD_STEPS, 0xC8)
        )
        assertEquals(
            DevicePropertyCharacteristic.Percentage8Steps(null),
            read(DeviceProperty.MOTION_THRESHOLD_STEPS, 0xFF)
        )
        assertRoundTrip(DeviceProperty.MOTION_THRESHOLD_STEPS, 0x05)
    }

    @Test
    fun testBuildingFloorUsesTheFloorNumberEncoding() {
        val ground = read(DeviceProperty.BUILDING_FLOOR, 0xFD)
        assertEquals("Ground floor (0)", ground.description)
        assertEquals("Not configured", read(DeviceProperty.BUILDING_FLOOR, 0xFF).description)
        assertEquals("-20 or below", read(DeviceProperty.BUILDING_FLOOR, 0x00).description)
        assertEquals("232 or above", read(DeviceProperty.BUILDING_FLOOR, 0xFC).description)
        // The floor is the raw value less 20.
        assertEquals("3", read(DeviceProperty.BUILDING_FLOOR, 23).description)
        assertRoundTrip(DeviceProperty.BUILDING_FLOOR, 0xFD)
    }

    // MARK: - Combined

    @Test
    fun testTemperatureAndHumidityOnOneOctetEach() {
        // 22.0 degrees is 44 steps of 0.5, and 50% is 100 steps of 0.5.
        val value = read(
            DeviceProperty.PRESENT_AMBIENT_TEMPERATURE_AND_COARSE_AMBIENT_RELATIVE_HUMIDITY, 0x2C, 0x64
        )
        assertIs<DevicePropertyCharacteristic.TemperatureAndHumidity8>(value)
        assertDecimal("22.0", value.temperature)
        assertDecimal("50", value.humidity)
        assertRoundTrip(
            DeviceProperty.PRESENT_AMBIENT_TEMPERATURE_AND_COARSE_AMBIENT_RELATIVE_HUMIDITY, 0x2C, 0x64
        )
    }

    @Test
    fun testTemperatureAndHumidityOnTwoOctetsEach() {
        // 23.00 degrees is 2300 = 0x08FC and 50.00% is 5000 = 0x1388.
        val bytes = intArrayOf(0xFC, 0x08, 0x88, 0x13)
        val value = read(DeviceProperty.PRESENT_PRECISE_AMBIENT_TEMPERATURE_AND_RELATIVE_HUMIDITY, *bytes)
        assertIs<DevicePropertyCharacteristic.TemperatureAndHumidity>(value)
        assertDecimal("23.00", value.temperature)
        assertDecimal("50.00", value.humidity)
        assertRoundTrip(DeviceProperty.PRESENT_PRECISE_AMBIENT_TEMPERATURE_AND_RELATIVE_HUMIDITY, *bytes)
    }

    @Test
    fun testSubSensorWeightsFollowTheNumberOfSteps() {
        // 4 steps, then weights of 50%, 100% and an unknown one.
        val value = read(DeviceProperty.SUB_SENSOR_WEIGHTS_3, 0x04, 0x64, 0xC8, 0xFF)
        assertIs<DevicePropertyCharacteristic.SubSensorWeights>(value)
        assertEquals(4.toUByte(), value.steps)
        assertEquals(3, value.weights.size)
        assertDecimal("50", value.weights[0])
        assertDecimal("100", value.weights[1])
        assertNull(value.weights[2])
        assertRoundTrip(DeviceProperty.SUB_SENSOR_WEIGHTS_3, 0x04, 0x64, 0xC8, 0xFF)
    }

    @Test
    fun testALengthThatDoesNotMatchGivesAnEmptyValue() {
        val value = DeviceProperty.SUB_SENSOR_WEIGHTS_3.read(ByteArray(2), 0, 2)
        assertEquals(DevicePropertyCharacteristic.SubSensorWeights(null, emptyList()), value)
        assertFalse(DeviceProperty.CONTACT_STATUS.read(ByteArray(2), 0, 2)
            .let { (it as DevicePropertyCharacteristic.ContactStatus).contacts.any { closed -> closed } })
    }

    // MARK: - Typed input

    @Test
    fun testTheAccelerationIsEnteredPerAxis() {
        val property = DeviceProperty.ACCELERATION_3D_SENSOR_STATUS
        assertEquals(listOf("X", "Y", "Z"), property.inputFields?.map { it.name })
        assertContentEquals(
            byteArrayOf(0x64, 0x9C.toByte(), 0x00),
            property.characteristicOf(listOf("1", "-1", "0"))?.data
        )
        assertNull(property.characteristicOf(listOf("1.26", "0", "0")))
        assertNull(property.characteristicOf(listOf("0", "-1.29", "0")))
    }

    @Test
    fun testSubSensorWeightsAskForTheStepsAndAWeightPerProbe() {
        val property = DeviceProperty.SUB_SENSOR_WEIGHTS_3
        assertEquals(
            listOf("Steps", "Weight 1", "Weight 2", "Weight 3"),
            property.inputFields?.map { it.name }
        )
        assertContentEquals(
            byteArrayOf(0x04, 0x64, 0xC8.toByte(), 0x00),
            property.characteristicOf(listOf("4", "50", "100", "0"))?.data
        )
        // There is at least one step.
        assertNull(property.characteristicOf(listOf("0", "50", "100", "0")))
        assertNull(property.characteristicOf(listOf("201", "50", "100", "0")))
        assertNull(property.characteristicOf(listOf("4", "50", "100", "101")))
    }

    @Test
    fun testNumbersOfTheCoarsePropertiesAreEntered() {
        assertContentEquals(
            byteArrayOf(0x64),
            DeviceProperty.COARSE_AMBIENT_RELATIVE_HUMIDITY.characteristicOf(listOf("50"))?.data
        )
        assertContentEquals(
            byteArrayOf(0xFE.toByte(), 0xFF.toByte()),
            DeviceProperty.COARSE_PRESENT_AMBIENT_LIGHT_LEVEL.characteristicOf(listOf("65534"))?.data
        )
        assertNull(DeviceProperty.COARSE_PRESENT_AMBIENT_LIGHT_LEVEL.characteristicOf(listOf("65535")))
        assertContentEquals(
            byteArrayOf(0xC8.toByte()),
            DeviceProperty.MOTION_THRESHOLD_STEPS.characteristicOf(listOf("200"))?.data
        )
        assertNull(DeviceProperty.MOTION_THRESHOLD_STEPS.characteristicOf(listOf("0")))
    }

    @Test
    fun testTemperatureAndHumidityAreEnteredTogether() {
        val property = DeviceProperty.PRESENT_PRECISE_AMBIENT_TEMPERATURE_AND_RELATIVE_HUMIDITY
        assertContentEquals(
            byteArrayOf(0xFC.toByte(), 0x08, 0x88.toByte(), 0x13),
            property.characteristicOf(listOf("23", "50"))?.data
        )
        assertNull(property.characteristicOf(listOf("23", "101")))
    }

    @Test
    fun testStatusesAndTheFloorAreEnteredAsHex() {
        listOf(
            DeviceProperty.CONTACT_STATUS,
            DeviceProperty.PUSH_BUTTON_STATUS,
            DeviceProperty.DOOR_WINDOW_STATUS,
            DeviceProperty.BUILDING_FLOOR,
        ).forEach { assertNull(it.inputFields, it.name) }
    }
}
