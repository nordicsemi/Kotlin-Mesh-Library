package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCLightOnOffGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCLightOnOffSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCLightOnOffSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCLightOnOffStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCModeGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCModeSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCModeSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCModeStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCOccupancyModeGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCOccupancyModeSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCOccupancyModeSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCOccupancyModeStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCPropertyGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCPropertySet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCPropertySetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLCPropertyStatus
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LightLCMessagesTest {

    private val transition = TransitionParameters(
        transitionTime = TransitionTime(rawValue = 0x45u),
        delay = 0x0Au
    )

    private val lightLcProperties = listOf(
        DeviceProperty.LIGHT_CONTROL_AMBIENT_LUX_LEVEL_ON,
        DeviceProperty.LIGHT_CONTROL_AMBIENT_LUX_LEVEL_PROLONG,
        DeviceProperty.LIGHT_CONTROL_AMBIENT_LUX_LEVEL_STANDBY,
        DeviceProperty.LIGHT_CONTROL_LIGHTNESS_ON,
        DeviceProperty.LIGHT_CONTROL_LIGHTNESS_PROLONG,
        DeviceProperty.LIGHT_CONTROL_LIGHTNESS_STANDBY,
        DeviceProperty.LIGHT_CONTROL_REGULATOR_ACCURACY,
        DeviceProperty.LIGHT_CONTROL_REGULATOR_KID,
        DeviceProperty.LIGHT_CONTROL_REGULATOR_KIU,
        DeviceProperty.LIGHT_CONTROL_REGULATOR_KPD,
        DeviceProperty.LIGHT_CONTROL_REGULATOR_KPU,
        DeviceProperty.LIGHT_CONTROL_TIME_FADE,
        DeviceProperty.LIGHT_CONTROL_TIME_FADE_ON,
        DeviceProperty.LIGHT_CONTROL_TIME_FADE_STANDBY_AUTO,
        DeviceProperty.LIGHT_CONTROL_TIME_FADE_STANDBY_MANUAL,
        DeviceProperty.LIGHT_CONTROL_TIME_OCCUPANCY_DELAY,
        DeviceProperty.LIGHT_CONTROL_TIME_PROLONG,
        DeviceProperty.LIGHT_CONTROL_TIME_RUN_ON,
    )

    @Test
    fun testOpCodes() {
        assertEquals(0x8291u, LightLCModeGet.opCode)
        assertEquals(0x8292u, LightLCModeSet.opCode)
        assertEquals(0x8293u, LightLCModeSetUnacknowledged.opCode)
        assertEquals(0x8294u, LightLCModeStatus.opCode)
        assertEquals(0x8295u, LightLCOccupancyModeGet.opCode)
        assertEquals(0x8296u, LightLCOccupancyModeSet.opCode)
        assertEquals(0x8297u, LightLCOccupancyModeSetUnacknowledged.opCode)
        assertEquals(0x8298u, LightLCOccupancyModeStatus.opCode)
        assertEquals(0x8299u, LightLCLightOnOffGet.opCode)
        assertEquals(0x829Au, LightLCLightOnOffSet.opCode)
        assertEquals(0x829Bu, LightLCLightOnOffSetUnacknowledged.opCode)
        assertEquals(0x829Cu, LightLCLightOnOffStatus.opCode)
        assertEquals(0x829Du, LightLCPropertyGet.opCode)
        assertEquals(0x62u, LightLCPropertySet.opCode)
        assertEquals(0x63u, LightLCPropertySetUnacknowledged.opCode)
        assertEquals(0x64u, LightLCPropertyStatus.opCode)
    }

    @Test
    fun testAcknowledgedMessagesRespondWithTheStatus() {
        assertEquals(LightLCModeStatus.opCode, LightLCModeGet().responseOpCode)
        assertEquals(LightLCModeStatus.opCode, LightLCModeSet(true).responseOpCode)
        assertEquals(LightLCOccupancyModeStatus.opCode, LightLCOccupancyModeGet().responseOpCode)
        assertEquals(
            LightLCOccupancyModeStatus.opCode,
            LightLCOccupancyModeSet(true).responseOpCode
        )
        assertEquals(LightLCLightOnOffStatus.opCode, LightLCLightOnOffGet().responseOpCode)
        assertEquals(
            LightLCLightOnOffStatus.opCode,
            LightLCLightOnOffSet(on = true).responseOpCode
        )
        val property = DeviceProperty.LIGHT_CONTROL_LIGHTNESS_ON
        assertEquals(
            LightLCPropertyStatus.opCode,
            LightLCPropertyGet(property).responseOpCode
        )
        assertEquals(
            LightLCPropertyStatus.opCode,
            LightLCPropertySet(property, DevicePropertyCharacteristic.PerceivedLightness(1u))
                .responseOpCode
        )
    }

    @Test
    fun testGetMessagesHaveNoParameters() {
        assertNull(LightLCModeGet().parameters)
        assertNull(LightLCOccupancyModeGet().parameters)
        assertNull(LightLCLightOnOffGet().parameters)
        assertNotNull(LightLCModeGet.init(null))
        assertNotNull(LightLCOccupancyModeGet.init(null))
        assertNotNull(LightLCLightOnOffGet.init(null))
        assertNull(LightLCModeGet.init(byteArrayOf(0x00)))
        assertNull(LightLCOccupancyModeGet.init(byteArrayOf(0x00)))
        assertNull(LightLCLightOnOffGet.init(byteArrayOf(0x00)))
    }

    @Test
    fun testModeParameters() {
        assertContentEquals(byteArrayOf(0x01), LightLCModeSet(controllerStatus = true).parameters)
        assertContentEquals(byteArrayOf(0x00), LightLCModeSet(controllerStatus = false).parameters)
        assertContentEquals(
            byteArrayOf(0x01),
            LightLCModeSetUnacknowledged(controllerStatus = true).parameters
        )
        assertContentEquals(
            byteArrayOf(0x00),
            LightLCModeStatus(controllerStatus = false).parameters
        )
    }

    @Test
    fun testModeIsParsedFromParameters() {
        assertTrue(LightLCModeSet.init(byteArrayOf(0x01))!!.controllerStatus)
        assertFalse(LightLCModeSet.init(byteArrayOf(0x00))!!.controllerStatus)
        assertTrue(LightLCModeSetUnacknowledged.init(byteArrayOf(0x01))!!.controllerStatus)
        assertFalse(LightLCModeStatus.init(byteArrayOf(0x00))!!.controllerStatus)
        assertTrue(LightLCModeStatus.init(byteArrayOf(0x01))!!.controllerStatus)
        assertNull(LightLCModeSet.init(null))
        assertNull(LightLCModeSet.init(byteArrayOf()))
        assertNull(LightLCModeSetUnacknowledged.init(byteArrayOf(0x01, 0x00)))
        assertNull(LightLCModeStatus.init(byteArrayOf()))
    }

    @Test
    fun testOccupancyModeParameters() {
        assertContentEquals(
            byteArrayOf(0x01),
            LightLCOccupancyModeSet(occupancyMode = true).parameters
        )
        assertContentEquals(
            byteArrayOf(0x00),
            LightLCOccupancyModeSetUnacknowledged(occupancyMode = false).parameters
        )
        assertContentEquals(
            byteArrayOf(0x01),
            LightLCOccupancyModeStatus(occupancyMode = true).parameters
        )
    }

    @Test
    fun testOccupancyModeIsParsedFromParameters() {
        assertTrue(LightLCOccupancyModeSet.init(byteArrayOf(0x01))!!.occupancyMode)
        assertFalse(LightLCOccupancyModeSetUnacknowledged.init(byteArrayOf(0x00))!!.occupancyMode)
        assertTrue(LightLCOccupancyModeStatus.init(byteArrayOf(0x01))!!.occupancyMode)
        assertNull(LightLCOccupancyModeSet.init(byteArrayOf()))
        assertNull(LightLCOccupancyModeSetUnacknowledged.init(byteArrayOf(0x01, 0x01)))
        assertNull(LightLCOccupancyModeStatus.init(null))
    }

    @Test
    fun testLightOnOffSetParameters() {
        assertContentEquals(
            byteArrayOf(0x01, 0x05),
            LightLCLightOnOffSet(on = true, tid = 5u).parameters
        )
        assertContentEquals(
            byteArrayOf(0x00, 0x05, 0x45, 0x0A),
            LightLCLightOnOffSet(tid = 5u, on = false, transitionParams = transition).parameters
        )
        assertContentEquals(
            byteArrayOf(0x01, 0x06, 0x45, 0x0A),
            LightLCLightOnOffSetUnacknowledged(
                tid = 6u,
                on = true,
                transitionParams = transition
            ).parameters
        )
    }

    @Test
    fun testLightOnOffSetIsParsedFromParameters() {
        val set = LightLCLightOnOffSet.init(byteArrayOf(0x01, 0x07, 0x45, 0x0A))
        assertNotNull(set)
        assertTrue(set.on)
        assertEquals(7u.toUByte(), set.tid)
        assertEquals(TransitionTime(rawValue = 0x45u), set.transitionTime)
        assertEquals(0x0Au.toUByte(), set.delay)

        val unacknowledged = LightLCLightOnOffSetUnacknowledged.init(byteArrayOf(0x00, 0x08))
        assertNotNull(unacknowledged)
        assertFalse(unacknowledged.on)
        assertEquals(8u.toUByte(), unacknowledged.tid)
        assertNull(unacknowledged.transitionTime)
        assertNull(unacknowledged.delay)

        assertNull(LightLCLightOnOffSet.init(byteArrayOf(0x01)))
        assertNull(LightLCLightOnOffSet.init(byteArrayOf(0x01, 0x07, 0x45)))
        assertNull(LightLCLightOnOffSetUnacknowledged.init(null))
    }

    @Test
    fun testParametersAreNotBuiltBeforeTheTidIsSet() {
        val set = LightLCLightOnOffSet(on = true)
        val unacknowledged = LightLCLightOnOffSetUnacknowledged(on = true)
        assertNull(set.tid)
        assertNull(unacknowledged.tid)
        set.tid = 0x07u
        assertContentEquals(byteArrayOf(0x01, 0x07), set.parameters)
    }

    @Test
    fun testLightOnOffStatusParameters() {
        assertContentEquals(byteArrayOf(0x01), LightLCLightOnOffStatus(isOn = true).parameters)
        assertContentEquals(
            byteArrayOf(0x00, 0x01, 0x45),
            LightLCLightOnOffStatus(
                isOn = false,
                remainingTime = TransitionTime(rawValue = 0x45u),
                targetState = true
            ).parameters
        )
    }

    @Test
    fun testLightOnOffStatusIsParsedFromParameters() {
        val idle = LightLCLightOnOffStatus.init(byteArrayOf(0x01))
        assertNotNull(idle)
        assertTrue(idle.isOn)
        assertNull(idle.targetState)
        assertNull(idle.remainingTime)

        val transitioning = LightLCLightOnOffStatus.init(byteArrayOf(0x00, 0x01, 0x45))
        assertNotNull(transitioning)
        assertFalse(transitioning.isOn)
        assertEquals(true, transitioning.targetState)
        assertEquals(TransitionTime(rawValue = 0x45u), transitioning.remainingTime)

        assertNull(LightLCLightOnOffStatus.init(byteArrayOf()))
        assertNull(LightLCLightOnOffStatus.init(byteArrayOf(0x01, 0x01)))
    }

    @Test
    fun testPropertyGetParameters() {
        // Property IDs are little endian.
        assertContentEquals(
            byteArrayOf(0x2E, 0x00),
            LightLCPropertyGet(DeviceProperty.LIGHT_CONTROL_LIGHTNESS_ON).parameters
        )
        val parsed = LightLCPropertyGet.init(byteArrayOf(0x2E, 0x00))
        assertNotNull(parsed)
        assertEquals(DeviceProperty.LIGHT_CONTROL_LIGHTNESS_ON, parsed.property)
        assertNull(LightLCPropertyGet.init(byteArrayOf(0x2E)))
        assertNull(LightLCPropertyGet.init(byteArrayOf(0x2E, 0x00, 0x00)))
        assertNull(LightLCPropertyGet.init(null))
    }

    @Test
    fun testPropertySetParameters() {
        val message = LightLCPropertySet(
            property = DeviceProperty.LIGHT_CONTROL_LIGHTNESS_ON,
            value = DevicePropertyCharacteristic.PerceivedLightness(0x1234u)
        )
        assertContentEquals(byteArrayOf(0x2E, 0x00, 0x34, 0x12), message.parameters)
        assertContentEquals(
            byteArrayOf(0x2E, 0x00, 0x34, 0x12),
            LightLCPropertySetUnacknowledged(
                property = DeviceProperty.LIGHT_CONTROL_LIGHTNESS_ON,
                value = DevicePropertyCharacteristic.PerceivedLightness(0x1234u)
            ).parameters
        )
        assertContentEquals(
            byteArrayOf(0x2E, 0x00, 0x34, 0x12),
            LightLCPropertyStatus(
                property = DeviceProperty.LIGHT_CONTROL_LIGHTNESS_ON,
                value = DevicePropertyCharacteristic.PerceivedLightness(0x1234u)
            ).parameters
        )
    }

    @Test
    fun testPropertyMessagesAreParsedFromParameters() {
        val params = byteArrayOf(0x2E, 0x00, 0x34, 0x12)
        val set = LightLCPropertySet.init(params)
        assertNotNull(set)
        assertEquals(DeviceProperty.LIGHT_CONTROL_LIGHTNESS_ON, set.property)
        assertEquals(DevicePropertyCharacteristic.PerceivedLightness(0x1234u), set.propertyValue)

        val unacknowledged = LightLCPropertySetUnacknowledged.init(params)
        assertNotNull(unacknowledged)
        assertEquals(
            DevicePropertyCharacteristic.PerceivedLightness(0x1234u),
            unacknowledged.propertyValue
        )

        val status = LightLCPropertyStatus.init(params)
        assertNotNull(status)
        assertEquals(0x002Eu.toUShort(), status.propertyId)
        assertEquals(DevicePropertyCharacteristic.PerceivedLightness(0x1234u), status.propertyValue)
    }

    @Test
    fun testPropertyMessagesRejectInvalidParameters() {
        assertNull(LightLCPropertySet.init(null))
        assertNull(LightLCPropertySet.init(byteArrayOf(0x2E, 0x00)))
        // Light Control Lightness On is 2 octets long.
        assertNull(LightLCPropertySet.init(byteArrayOf(0x2E, 0x00, 0x34)))
        assertNull(LightLCPropertySetUnacknowledged.init(byteArrayOf(0x2E, 0x00, 0x34, 0x12, 0x00)))
        assertNull(LightLCPropertyStatus.init(byteArrayOf(0x2E, 0x00, 0x34)))
    }

    @Test
    fun testUnknownPropertyIsKeptAsRawValue() {
        val status = LightLCPropertyStatus.init(byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0x01))
        assertNotNull(status)
        assertNull(status.property)
        assertIs<DevicePropertyCharacteristic.Other>(status.propertyValue)
        assertContentEquals(byteArrayOf(0x01), status.propertyValue.data)
    }

    @Test
    fun testAllLightControlPropertiesRoundTrip() {
        lightLcProperties.forEach { property ->
            val length = property.valueLength
            assertNotNull(length, "$property has no value length")
            // The regulator coefficients are IEEE-754 floats, 1.0f is used as a valid value.
            val value = when (length) {
                4 -> byteArrayOf(0x00, 0x00, 0x80.toByte(), 0x3F)
                else -> ByteArray(length) { (it + 1).toByte() }
            }
            val params = property.id.let {
                byteArrayOf((it.toInt() and 0xFF).toByte(), (it.toInt() shr 8).toByte())
            } + value

            val status = LightLCPropertyStatus.init(params)
            assertNotNull(status, "$property could not be parsed")
            assertEquals(property, status.property)
            assertFalse(
                status.propertyValue is DevicePropertyCharacteristic.Other,
                "$property has no characteristic"
            )
            assertEquals(length, status.propertyValue.data.size, "$property value length")
            assertContentEquals(params, status.parameters, "$property round trip")
        }
    }
}
