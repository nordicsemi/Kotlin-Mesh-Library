package no.nordicsemi.kotlin.mesh.core.messages

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DevicePropertyListTest {

    @Test
    fun testEveryPropertyHasItsOwnId() {
        val ids = DeviceProperty.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun testPropertiesAddedInTheLatestSpecificationAreKnown() {
        assertEquals("Motion Threshold Steps", DeviceProperty.from(0x00B8u)?.propertyName)
        assertEquals("Building Floor", DeviceProperty.from(0x00B9u)?.propertyName)
        assertEquals("Subsensor Weights 2", DeviceProperty.from(0x00C4u)?.propertyName)
        assertEquals("Subsensor Weights 8", DeviceProperty.from(0x00CAu)?.propertyName)
        assertEquals("Door/Window Status", DeviceProperty.from(0x00CFu)?.propertyName)
        assertEquals("Pushbutton Status", DeviceProperty.from(0x00D1u)?.propertyName)
        // The Property ID 0x0000 is prohibited and the first one after the last is not assigned.
        assertNull(DeviceProperty.from(0x0000u))
        assertNull(DeviceProperty.from(0x00D2u))
    }

    @Test
    fun testTheNewStringPropertiesAreDecodedAsStrings() {
        listOf(
            DeviceProperty.LOCAL_ASSET_IDENTIFIER,
            DeviceProperty.LOCAL_ASSET_NAME,
            DeviceProperty.SPATIAL_UNIT_CATEGORY,
            DeviceProperty.SPATIAL_UNIT_NAME,
        ).forEach { property ->
            assertEquals(36, property.valueLength)
            val data = "Room 1".padEnd(36, ' ').toByteArray()
            val value = property.read(data, 0, 36)
            assertIs<DevicePropertyCharacteristic.FixedString36>(value, property.name)
            assertEquals("Room 1", value.description.trim())
        }
    }

    @Test
    fun testTheNewTimePropertiesAreSeconds() {
        listOf(
            DeviceProperty.HVAC_OCCUPANCY_MINIMUM_RUN_TIME,
            DeviceProperty.HVAC_OCCUPANCY_THRESHOLD_TIME,
            DeviceProperty.HVAC_VACANCY_THRESHOLD_TIME,
        ).forEach { property ->
            assertEquals(2, property.valueLength)
            assertIs<DevicePropertyCharacteristic.TimeSecond16>(
                property.read(byteArrayOf(0x1E, 0x00), 0, 2), property.name
            )
            // 30 seconds is entered as a number and sent as 0x001E.
            assertContentEquals(
                byteArrayOf(0x1E, 0x00),
                property.characteristicOf(listOf("30"))?.data
            )
        }
    }

    @Test
    fun testTheNewPropertiesHaveTheLengthOfTheirCharacteristic() {
        val lengths = mapOf(
            DeviceProperty.MOTION_THRESHOLD_STEPS to 1,
            DeviceProperty.BUILDING_FLOOR to 1,
            DeviceProperty.COARSE_AMBIENT_RELATIVE_HUMIDITY to 1,
            DeviceProperty.COARSE_PRESENT_AMBIENT_LIGHT_LEVEL to 2,
            DeviceProperty.PRESENT_AMBIENT_TEMPERATURE_AND_COARSE_AMBIENT_RELATIVE_HUMIDITY to 2,
            DeviceProperty.PRESENT_PRECISE_AMBIENT_TEMPERATURE_AND_RELATIVE_HUMIDITY to 4,
            DeviceProperty.SUB_SENSOR_WEIGHTS_2 to 3,
            DeviceProperty.SUB_SENSOR_WEIGHTS_8 to 9,
            DeviceProperty.ACCELERATION_3D_SENSOR_STATUS to 3,
            DeviceProperty.PRECISE_ACCELERATION_3D_SENSOR_STATUS to 6,
            DeviceProperty.ACCELERATION_DETECTION_STATUS to 1,
            DeviceProperty.CONTACT_STATUS to 1,
            DeviceProperty.DOOR_WINDOW_STATUS to 1,
            DeviceProperty.PUSH_BUTTON_STATUS to 1,
        )
        lengths.forEach { (property, length) -> assertEquals(length, property.valueLength, property.name) }
    }

    @Test
    fun testNoPropertyIsLeftWithoutADecoderThatTheLibraryCouldHave() {
        // Every property of the latest specification is decoded to a typed characteristic.
        (0x00B8..0x00D1).map { DeviceProperty.from(it.toUShort())!! }.forEach { property ->
            val length = assertNotNull(property.valueLength, property.name)
            val value = property.read(ByteArray(length), 0, length)
            assertFalse(value is DevicePropertyCharacteristic.Other, property.name)
        }
    }

    @Test
    fun testEveryPropertyIsEncodedBackToTheBytesItWasDecodedFrom() {
        val decoded = DeviceProperty.entries.filter { it.valueLength != null }
        assertTrue(decoded.size > 100, "Only ${decoded.size} properties have a decoder")
        val failures = mutableListOf<String>()
        decoded.forEach { property ->
            val length = assertNotNull(property.valueLength)
            listOf(0x00, 0x01).forEach { fill ->
                val bytes = ByteArray(length) { fill.toByte() }
                val value = property.read(bytes, 0, length)
                if (!bytes.contentEquals(value.data)) {
                    failures += "${property.name} (0x%04X) with ${bytes.toList()} gave ${value.data.toList()}"
                        .format(property.id.toInt())
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }
}
