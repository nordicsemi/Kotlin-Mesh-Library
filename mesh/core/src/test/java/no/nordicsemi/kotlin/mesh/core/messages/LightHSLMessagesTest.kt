package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLDefaultGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLDefaultSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLDefaultSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLDefaultStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLHueGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLHueSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLHueSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLHueStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLRangeGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLRangeSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLRangeSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLRangeStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLSaturationGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLSaturationSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLSaturationSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLSaturationStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLTargetGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightHSLTargetStatus
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LightHSLMessagesTest {

    private val transition = TransitionParameters(
        transitionTime = TransitionTime(rawValue = 0x45u),
        delay = 0x0Au
    )

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    @Test
    fun testOpCodes() {
        assertEquals(0x826Du, LightHSLGet.opCode)
        assertEquals(0x826Eu, LightHSLHueGet.opCode)
        assertEquals(0x826Fu, LightHSLHueSet.opCode)
        assertEquals(0x8270u, LightHSLHueSetUnacknowledged.opCode)
        assertEquals(0x8271u, LightHSLHueStatus.opCode)
        assertEquals(0x8272u, LightHSLSaturationGet.opCode)
        assertEquals(0x8273u, LightHSLSaturationSet.opCode)
        assertEquals(0x8274u, LightHSLSaturationSetUnacknowledged.opCode)
        assertEquals(0x8275u, LightHSLSaturationStatus.opCode)
        assertEquals(0x8276u, LightHSLSet.opCode)
        assertEquals(0x8277u, LightHSLSetUnacknowledged.opCode)
        assertEquals(0x8278u, LightHSLStatus.opCode)
        assertEquals(0x8279u, LightHSLTargetGet.opCode)
        assertEquals(0x827Au, LightHSLTargetStatus.opCode)
        assertEquals(0x827Bu, LightHSLDefaultGet.opCode)
        assertEquals(0x827Cu, LightHSLDefaultStatus.opCode)
        assertEquals(0x827Du, LightHSLRangeGet.opCode)
        assertEquals(0x827Eu, LightHSLRangeStatus.opCode)
        assertEquals(0x827Fu, LightHSLDefaultSet.opCode)
        assertEquals(0x8280u, LightHSLDefaultSetUnacknowledged.opCode)
        assertEquals(0x8281u, LightHSLRangeSet.opCode)
        assertEquals(0x8282u, LightHSLRangeSetUnacknowledged.opCode)
    }

    @Test
    fun testResponseOpCodes() {
        assertEquals(LightHSLStatus.opCode, LightHSLGet().responseOpCode)
        assertEquals(LightHSLStatus.opCode, LightHSLSet(1u, 2u, 3u).responseOpCode)
        assertEquals(LightHSLHueStatus.opCode, LightHSLHueGet().responseOpCode)
        assertEquals(LightHSLHueStatus.opCode, LightHSLHueSet(1u).responseOpCode)
        assertEquals(LightHSLSaturationStatus.opCode, LightHSLSaturationGet().responseOpCode)
        assertEquals(LightHSLSaturationStatus.opCode, LightHSLSaturationSet(1u).responseOpCode)
        assertEquals(LightHSLTargetStatus.opCode, LightHSLTargetGet().responseOpCode)
        assertEquals(LightHSLDefaultStatus.opCode, LightHSLDefaultGet().responseOpCode)
        assertEquals(LightHSLDefaultStatus.opCode, LightHSLDefaultSet(1u, 2u, 3u).responseOpCode)
        assertEquals(LightHSLRangeStatus.opCode, LightHSLRangeGet().responseOpCode)
        assertEquals(
            LightHSLRangeStatus.opCode,
            LightHSLRangeSet(1u, 2u, 3u, 4u).responseOpCode
        )
    }

    @Test
    fun testGetMessages() {
        assertNull(LightHSLGet().parameters)
        assertNull(LightHSLHueGet().parameters)
        assertNull(LightHSLSaturationGet().parameters)
        assertNull(LightHSLTargetGet().parameters)
        assertNull(LightHSLDefaultGet().parameters)
        assertNull(LightHSLRangeGet().parameters)
        assertNotNull(LightHSLGet.init(null))
        assertNotNull(LightHSLTargetGet.init(byteArrayOf()))
        assertNull(LightHSLGet.init(byteArrayOf(0x00)))
        assertNull(LightHSLRangeGet.init(byteArrayOf(0x00)))
    }

    @Test
    fun testLightHSLSet() {
        assertContentEquals(
            bytes(0x34, 0x12, 0x78, 0x56, 0xBC, 0x9A, 0x07),
            LightHSLSet(tid = 7u, lightness = 0x1234u, hue = 0x5678u, saturation = 0x9ABCu).parameters
        )
        assertContentEquals(
            bytes(0x34, 0x12, 0x78, 0x56, 0xBC, 0x9A, 0x07, 0x45, 0x0A),
            LightHSLSetUnacknowledged(
                tid = 7u, lightness = 0x1234u, hue = 0x5678u, saturation = 0x9ABCu,
                transitionParams = transition
            ).parameters
        )
        val set = LightHSLSet.init(bytes(0x34, 0x12, 0x78, 0x56, 0xBC, 0x9A, 0x07))
        assertNotNull(set)
        assertEquals(0x1234u.toUShort(), set.lightness)
        assertEquals(0x5678u.toUShort(), set.hue)
        assertEquals(0x9ABCu.toUShort(), set.saturation)
        assertEquals(7u.toUByte(), set.tid)
        assertNull(set.transitionTime)
        assertNull(set.delay)

        val unack = LightHSLSetUnacknowledged.init(
            bytes(0x34, 0x12, 0x78, 0x56, 0xBC, 0x9A, 0x07, 0x45, 0x0A)
        )
        assertNotNull(unack)
        assertEquals(0x9ABCu.toUShort(), unack.saturation)
        assertEquals(TransitionTime(rawValue = 0x45u), unack.transitionTime)
        assertEquals(0x0Au.toUByte(), unack.delay)

        assertNull(LightHSLSet.init(bytes(0x34, 0x12, 0x78, 0x56, 0xBC, 0x9A)))
        assertNull(LightHSLSet.init(bytes(0x34, 0x12, 0x78, 0x56, 0xBC, 0x9A, 0x07, 0x45)))
        assertNull(LightHSLSet.init(bytes(0, 0, 0, 0, 0, 0, 0, 0, 0, 0)))
        assertNull(LightHSLSetUnacknowledged.init(null))
    }

    @Test
    fun testLightHSLHueSet() {
        assertContentEquals(
            bytes(0x34, 0x12, 0x07),
            LightHSLHueSet(tid = 7u, hue = 0x1234u).parameters
        )
        assertContentEquals(
            bytes(0x34, 0x12, 0x07, 0x45, 0x0A),
            LightHSLHueSetUnacknowledged(tid = 7u, hue = 0x1234u, transitionParams = transition).parameters
        )
        val set = LightHSLHueSet.init(bytes(0x34, 0x12, 0x07))
        assertNotNull(set)
        assertEquals(0x1234u.toUShort(), set.hue)
        assertNull(set.transitionTime)
        val unack = LightHSLHueSetUnacknowledged.init(bytes(0x34, 0x12, 0x07, 0x45, 0x0A))
        assertNotNull(unack)
        assertEquals(TransitionTime(rawValue = 0x45u), unack.transitionTime)
        assertNull(LightHSLHueSet.init(bytes(0x34, 0x12)))
        assertNull(LightHSLHueSet.init(bytes(0x34, 0x12, 0x07, 0x45)))
        assertNull(LightHSLHueSetUnacknowledged.init(null))
    }

    @Test
    fun testLightHSLSaturationSet() {
        assertContentEquals(
            bytes(0x34, 0x12, 0x07),
            LightHSLSaturationSet(tid = 7u, saturation = 0x1234u).parameters
        )
        assertContentEquals(
            bytes(0x34, 0x12, 0x07, 0x45, 0x0A),
            LightHSLSaturationSetUnacknowledged(
                tid = 7u, saturation = 0x1234u, transitionParams = transition
            ).parameters
        )
        val set = LightHSLSaturationSet.init(bytes(0x34, 0x12, 0x07, 0x45, 0x0A))
        assertNotNull(set)
        assertEquals(0x1234u.toUShort(), set.saturation)
        assertEquals(0x0Au.toUByte(), set.delay)
        assertNotNull(LightHSLSaturationSetUnacknowledged.init(bytes(0x34, 0x12, 0x07)))
        assertNull(LightHSLSaturationSet.init(bytes(0x34)))
        assertNull(LightHSLSaturationSetUnacknowledged.init(bytes(0, 0, 0, 0, 0, 0)))
    }

    @Test
    fun testLightHSLStatus() {
        assertContentEquals(
            bytes(0x34, 0x12, 0x78, 0x56, 0xBC, 0x9A),
            LightHSLStatus(lightness = 0x1234u, hue = 0x5678u, saturation = 0x9ABCu).parameters
        )
        assertContentEquals(
            bytes(0x34, 0x12, 0x78, 0x56, 0xBC, 0x9A, 0x45),
            LightHSLStatus(
                lightness = 0x1234u, hue = 0x5678u, saturation = 0x9ABCu,
                remainingTime = TransitionTime(rawValue = 0x45u)
            ).parameters
        )
        val short = LightHSLStatus.init(bytes(0x34, 0x12, 0x78, 0x56, 0xBC, 0x9A))
        assertNotNull(short)
        assertEquals(0x1234u.toUShort(), short.lightness)
        assertEquals(0x5678u.toUShort(), short.hue)
        assertEquals(0x9ABCu.toUShort(), short.saturation)
        assertNull(short.remainingTime)
        val long = LightHSLStatus.init(bytes(0x34, 0x12, 0x78, 0x56, 0xBC, 0x9A, 0x45))
        assertNotNull(long)
        assertEquals(TransitionTime(rawValue = 0x45u), long.remainingTime)
        assertNull(LightHSLStatus.init(bytes(0x34, 0x12, 0x78, 0x56, 0xBC)))
        assertNull(LightHSLStatus.init(bytes(0, 0, 0, 0, 0, 0, 0, 0)))
        assertNull(LightHSLStatus.init(null))
    }

    @Test
    fun testLightHSLTargetStatus() {
        assertContentEquals(
            bytes(0x34, 0x12, 0x78, 0x56, 0xBC, 0x9A),
            LightHSLTargetStatus(
                targetLightness = 0x1234u, targetHue = 0x5678u, targetSaturation = 0x9ABCu
            ).parameters
        )
        assertContentEquals(
            bytes(0x34, 0x12, 0x78, 0x56, 0xBC, 0x9A, 0x45),
            LightHSLTargetStatus(
                targetLightness = 0x1234u, targetHue = 0x5678u, targetSaturation = 0x9ABCu,
                remainingTime = TransitionTime(rawValue = 0x45u)
            ).parameters
        )
        val short = LightHSLTargetStatus.init(bytes(0x34, 0x12, 0x78, 0x56, 0xBC, 0x9A))
        assertNotNull(short)
        assertEquals(0x1234u.toUShort(), short.targetLightness)
        assertEquals(0x5678u.toUShort(), short.targetHue)
        assertEquals(0x9ABCu.toUShort(), short.targetSaturation)
        assertNull(short.remainingTime)
        val long = LightHSLTargetStatus.init(bytes(0x34, 0x12, 0x78, 0x56, 0xBC, 0x9A, 0x45))
        assertNotNull(long)
        assertEquals(TransitionTime(rawValue = 0x45u), long.remainingTime)
        assertNull(LightHSLTargetStatus.init(bytes(0x34, 0x12)))
        assertNull(LightHSLTargetStatus.init(null))
    }

    @Test
    fun testHueAndSaturationStatus() {
        assertContentEquals(bytes(0x34, 0x12), LightHSLHueStatus(hue = 0x1234u).parameters)
        assertContentEquals(
            bytes(0x34, 0x12, 0x78, 0x56, 0x45),
            LightHSLHueStatus(
                hue = 0x1234u, targetHue = 0x5678u,
                remainingTime = TransitionTime(rawValue = 0x45u)
            ).parameters
        )
        assertContentEquals(
            bytes(0x34, 0x12, 0x78, 0x56, 0x45),
            LightHSLSaturationStatus(
                saturation = 0x1234u, targetSaturation = 0x5678u,
                remainingTime = TransitionTime(rawValue = 0x45u)
            ).parameters
        )
        val short = LightHSLHueStatus.init(bytes(0x34, 0x12))
        assertNotNull(short)
        assertEquals(0x1234u.toUShort(), short.hue)
        assertNull(short.targetHue)
        assertNull(short.remainingTime)
        val long = LightHSLSaturationStatus.init(bytes(0x34, 0x12, 0x78, 0x56, 0x45))
        assertNotNull(long)
        assertEquals(0x1234u.toUShort(), long.saturation)
        assertEquals(0x5678u.toUShort(), long.targetSaturation)
        assertEquals(TransitionTime(rawValue = 0x45u), long.remainingTime)
        assertNull(LightHSLHueStatus.init(bytes(0x34)))
        assertNull(LightHSLHueStatus.init(bytes(0x34, 0x12, 0x78)))
        assertNull(LightHSLSaturationStatus.init(bytes(0, 0, 0, 0, 0, 0)))
        assertNull(LightHSLSaturationStatus.init(null))
    }

    @Test
    fun testDefaultMessages() {
        val expected = bytes(0x34, 0x12, 0x78, 0x56, 0xBC, 0x9A)
        assertContentEquals(expected, LightHSLDefaultSet(0x1234u, 0x5678u, 0x9ABCu).parameters)
        assertContentEquals(
            expected, LightHSLDefaultSetUnacknowledged(0x1234u, 0x5678u, 0x9ABCu).parameters
        )
        assertContentEquals(expected, LightHSLDefaultStatus(0x1234u, 0x5678u, 0x9ABCu).parameters)
        val set = LightHSLDefaultSet.init(expected)
        assertNotNull(set)
        assertEquals(0x1234u.toUShort(), set.lightness)
        assertEquals(0x5678u.toUShort(), set.hue)
        assertEquals(0x9ABCu.toUShort(), set.saturation)
        assertNotNull(LightHSLDefaultSetUnacknowledged.init(expected))
        assertNotNull(LightHSLDefaultStatus.init(expected))
        assertNull(LightHSLDefaultSet.init(bytes(0x34, 0x12, 0x78, 0x56, 0xBC)))
        assertNull(LightHSLDefaultSetUnacknowledged.init(bytes(0, 0, 0, 0, 0, 0, 0)))
        assertNull(LightHSLDefaultStatus.init(null))
    }

    @Test
    fun testRangeMessages() {
        val expected = bytes(0x01, 0x00, 0x02, 0x00, 0x03, 0x00, 0x04, 0x01)
        val set = LightHSLRangeSet(minHue = 1u, maxHue = 2u, minSaturation = 3u, maxSaturation = 0x104u)
        assertContentEquals(expected, set.parameters)
        assertEquals(1u..2u, set.hueRange)
        assertEquals(3u..0x104u, set.saturationRange)
        assertContentEquals(
            expected,
            LightHSLRangeSetUnacknowledged(hueRange = 1u..2u, saturationRange = 3u..0x104u).parameters
        )
        val parsed = LightHSLRangeSet.init(expected)
        assertNotNull(parsed)
        assertEquals(0x104u.toUShort(), parsed.maxSaturation)
        assertNotNull(LightHSLRangeSetUnacknowledged.init(expected))
        assertNull(LightHSLRangeSet.init(expected.copyOf(7)))
        assertNull(LightHSLRangeSetUnacknowledged.init(expected + 0))
        assertNull(LightHSLRangeSet.init(null))
    }

    @Test
    fun testRangeStatus() {
        val status = LightHSLRangeStatus(
            status = RangeMessageStatus.CANNOT_SET_RANGE_MAX,
            minHue = 1u, maxHue = 2u, minSaturation = 3u, maxSaturation = 4u
        )
        assertContentEquals(
            bytes(0x02, 0x01, 0x00, 0x02, 0x00, 0x03, 0x00, 0x04, 0x00),
            status.parameters
        )
        assertFalse(status.isSuccess)
        assertTrue(LightHSLRangeStatus(hueRange = 1u..2u, saturationRange = 3u..4u).isSuccess)
        val fromRequest = LightHSLRangeStatus(
            RangeMessageStatus.CANNOT_SET_RANGE_MIN,
            LightHSLRangeSet(1u, 2u, 3u, 4u)
        )
        assertEquals(4u.toUShort(), fromRequest.maxSaturation)

        val parsed = LightHSLRangeStatus.init(
            bytes(0x01, 0x01, 0x00, 0x02, 0x00, 0x03, 0x00, 0x04, 0x00)
        )
        assertNotNull(parsed)
        assertEquals(RangeMessageStatus.CANNOT_SET_RANGE_MIN, parsed.status)
        assertEquals(1u..2u, parsed.hueRange)
        assertEquals(3u..4u, parsed.saturationRange)
        // Invalid status code, short and long parameters.
        assertNull(LightHSLRangeStatus.init(bytes(0x03, 1, 0, 2, 0, 3, 0, 4, 0)))
        assertNull(LightHSLRangeStatus.init(bytes(0x00, 1, 0, 2, 0, 3, 0, 4)))
        assertNull(LightHSLRangeStatus.init(bytes(0x00, 1, 0, 2, 0, 3, 0, 4, 0, 0)))
        assertNull(LightHSLRangeStatus.init(null))
    }
}
