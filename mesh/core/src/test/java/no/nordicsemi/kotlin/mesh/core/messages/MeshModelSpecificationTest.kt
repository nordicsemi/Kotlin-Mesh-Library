package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertiesGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertyGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertySet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertySetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericAdminPropertyStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericClientPropertiesGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericClientPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationGlobalStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLocationLocalStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertiesGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertyGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertySet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertySetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericManufacturerPropertyStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertiesGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertiesStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertyGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertySet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertySetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericUserPropertyStatus
import kotlin.math.floor
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Checks the Generic Location and Generic Property messages against the Bluetooth Mesh Model
 * specification v1.0.1: the opcodes of table 7.1, the formulas of section 3.1.7 and the field
 * tables of section 3.1.7.7.
 */
class MeshModelSpecificationTest {

    private val opCodes: Map<String, Pair<UInt, UInt>> = mapOf(
        "GenericLocationGlobalGet" to (GenericLocationGlobalGet.Initializer.opCode to 0x8225u),
        "GenericLocationGlobalStatus" to (GenericLocationGlobalStatus.Initializer.opCode to 0x40u),
        "GenericLocationLocalGet" to (GenericLocationLocalGet.Initializer.opCode to 0x8226u),
        "GenericLocationLocalStatus" to (GenericLocationLocalStatus.Initializer.opCode to 0x8227u),
        "GenericLocationGlobalSet" to (GenericLocationGlobalSet.Initializer.opCode to 0x41u),
        "GenericLocationGlobalSetUnacknowledged" to (GenericLocationGlobalSetUnacknowledged.Initializer.opCode to 0x42u),
        "GenericLocationLocalSet" to (GenericLocationLocalSet.Initializer.opCode to 0x8228u),
        "GenericLocationLocalSetUnacknowledged" to (GenericLocationLocalSetUnacknowledged.Initializer.opCode to 0x8229u),
        "GenericManufacturerPropertiesGet" to (GenericManufacturerPropertiesGet.Initializer.opCode to 0x822Au),
        "GenericManufacturerPropertiesStatus" to (GenericManufacturerPropertiesStatus.Initializer.opCode to 0x43u),
        "GenericManufacturerPropertyGet" to (GenericManufacturerPropertyGet.Initializer.opCode to 0x822Bu),
        "GenericManufacturerPropertySet" to (GenericManufacturerPropertySet.Initializer.opCode to 0x44u),
        "GenericManufacturerPropertySetUnacknowledged" to (GenericManufacturerPropertySetUnacknowledged.Initializer.opCode to 0x45u),
        "GenericManufacturerPropertyStatus" to (GenericManufacturerPropertyStatus.Initializer.opCode to 0x46u),
        "GenericAdminPropertiesGet" to (GenericAdminPropertiesGet.Initializer.opCode to 0x822Cu),
        "GenericAdminPropertiesStatus" to (GenericAdminPropertiesStatus.Initializer.opCode to 0x47u),
        "GenericAdminPropertyGet" to (GenericAdminPropertyGet.Initializer.opCode to 0x822Du),
        "GenericAdminPropertySet" to (GenericAdminPropertySet.Initializer.opCode to 0x48u),
        "GenericAdminPropertySetUnacknowledged" to (GenericAdminPropertySetUnacknowledged.Initializer.opCode to 0x49u),
        "GenericAdminPropertyStatus" to (GenericAdminPropertyStatus.Initializer.opCode to 0x4Au),
        "GenericUserPropertiesGet" to (GenericUserPropertiesGet.Initializer.opCode to 0x822Eu),
        "GenericUserPropertiesStatus" to (GenericUserPropertiesStatus.Initializer.opCode to 0x4Bu),
        "GenericUserPropertyGet" to (GenericUserPropertyGet.Initializer.opCode to 0x822Fu),
        "GenericUserPropertySet" to (GenericUserPropertySet.Initializer.opCode to 0x4Cu),
        "GenericUserPropertySetUnacknowledged" to (GenericUserPropertySetUnacknowledged.Initializer.opCode to 0x4Du),
        "GenericUserPropertyStatus" to (GenericUserPropertyStatus.Initializer.opCode to 0x4Eu),
        "GenericClientPropertiesGet" to (GenericClientPropertiesGet.Initializer.opCode to 0x4Fu),
        "GenericClientPropertiesStatus" to (GenericClientPropertiesStatus.Initializer.opCode to 0x50u),
    )

    @Test
    fun testOpCodesMatchTheSpecification() {
        opCodes.forEach { (name, codes) ->
            assertEquals(codes.second, codes.first, "opcode of $name")
        }
    }

    @Test
    fun testEveryOpCodeIsUnique() {
        assertEquals(opCodes.size, opCodes.values.map { it.second }.toSet().size)
    }

    @Test
    fun testOpCodesUseTheSpecifiedNumberOfOctets() {
        // Opcodes 0x40 to 0x7F have a single octet, and the others are 0x82 0xNN.
        opCodes.forEach { (name, codes) ->
            val opCode = codes.second
            assertEquals(true, opCode in 0x40u..0x7Fu || opCode in 0x8200u..0x82FFu, name)
        }
    }

    // Latitude N = floor(X / 90 * (2^31 - 1)), longitude N = floor(X / 180 * (2^31 - 1)).

    private fun specLatitude(degrees: Double) = floor(degrees / 90.0 * Int.MAX_VALUE).toInt()
    private fun specLongitude(degrees: Double) = floor(degrees / 180.0 * Int.MAX_VALUE).toInt()

    @Test
    fun testLatitudeFollowsTheSpecificationFormula() {
        listOf(0.0, 27.988056, -27.988056, 59.9139, -33.8688, 89.999999, 0.000001, -0.000001)
            .forEach { degrees ->
                assertEquals(
                    specLatitude(degrees),
                    assertNotNull(Latitude.fromDegrees(degrees)).raw,
                    "latitude $degrees"
                )
            }
    }

    @Test
    fun testLongitudeFollowsTheSpecificationFormula() {
        listOf(0.0, 86.925278, -86.925278, 10.7522, -122.4194, 179.999999, 0.000001, -0.000001)
            .forEach { degrees ->
                assertEquals(
                    specLongitude(degrees),
                    assertNotNull(Longitude.fromDegrees(degrees)).raw,
                    "longitude $degrees"
                )
            }
    }

    @Test
    fun testSummitOfMountEverestIsEncodedLittleEndian() {
        val message = GenericLocationGlobalSet(
            latitude = assertNotNull(Latitude.fromDegrees(27.988056)),
            longitude = assertNotNull(Longitude.fromDegrees(86.925278)),
            altitude = Altitude(8849)
        )
        val latitude = specLatitude(27.988056)
        val longitude = specLongitude(86.925278)
        val expected = byteArrayOf(
            latitude.toByte(), (latitude shr 8).toByte(), (latitude shr 16).toByte(),
            (latitude shr 24).toByte(),
            longitude.toByte(), (longitude shr 8).toByte(), (longitude shr 16).toByte(),
            (longitude shr 24).toByte(),
            (8849 and 0xFF).toByte(), (8849 shr 8).toByte()
        )
        assertContentEquals(expected, message.parameters)
    }

    @Test
    fun testCoordinateResolutionIsFinerThanAMillimeter() {
        // One step of latitude is 90 / (2^31 - 1) degrees, which is about 4.7 mm.
        val stepMeters = 90.0 / Int.MAX_VALUE * 111_320.0
        assertEquals(true, stepMeters < 0.005)
    }

    // Uncertainty: update time t = 2^(x - 3) seconds and precision = 2^(y - 3) meters.

    @Test
    fun testUncertaintyFormulas() {
        for (x in 0..15) {
            val uncertainty = LocationUncertainty.of(
                isMobile = false, updateTimeRaw = x, precisionRaw = x
            )
            val expectedSeconds = 2.0.pow(x - 3)
            assertEquals(expectedSeconds, uncertainty.updateTimeMillis / 1000.0, "update time $x")
            assertEquals(expectedSeconds, uncertainty.precisionMillimeters / 1000.0, "precision $x")
        }
    }

    @Test
    fun testUncertaintyRangeIsFrom0Point125To4096() {
        val smallest = LocationUncertainty.of(false, updateTimeRaw = 0, precisionRaw = 0)
        val largest = LocationUncertainty.of(false, updateTimeRaw = 15, precisionRaw = 15)
        assertEquals(125L, smallest.updateTimeMillis)
        assertEquals(125L, smallest.precisionMillimeters)
        assertEquals(4_096_000L, largest.updateTimeMillis)
        assertEquals(4_096_000L, largest.precisionMillimeters)
    }

    @Test
    fun testUncertaintyBitPositions() {
        // Bit 0 is stationary (0) or mobile (1), bits 8-11 update time, bits 12-15 precision.
        assertEquals(0x0001u.toUShort(), LocationUncertainty.of(true, 0, 0).raw)
        assertEquals(0x0100u.toUShort(), LocationUncertainty.of(false, 1, 0).raw)
        assertEquals(0x1000u.toUShort(), LocationUncertainty.of(false, 0, 1).raw)
        assertEquals(0xFF01u.toUShort(), LocationUncertainty.of(true, 15, 15).raw)
    }

    // Floor number table 3.22.

    @Test
    fun testFloorNumberTable() {
        assertEquals(-20, FloorNumber(0x00u).number)
        assertEquals(true, FloorNumber(0x00u).isMinOrBelow)
        assertEquals(-19, FloorNumber(0x01u).number)
        assertEquals(231, FloorNumber(0xFBu).number)
        assertEquals(232, FloorNumber(0xFCu).number)
        assertEquals(true, FloorNumber(0xFCu).isMaxOrAbove)
        assertEquals(0, FloorNumber(0xFDu).number)
        assertEquals(1, FloorNumber(0xFEu).number)
        assertEquals(false, FloorNumber(0xFFu).isKnown)
    }

    @Test
    fun testFloorEncodingIsTheFloorPlus20() {
        for (floor in -19..231) {
            assertEquals((floor + 20).toUByte(), assertNotNull(FloorNumber.of(floor)).raw)
        }
    }
}
