package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightCTLDefaultGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightCTLDefaultSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightCTLDefaultSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightCTLDefaultStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightCTLGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightCTLSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightCTLSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightCTLStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightCTLTemperatureGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightCTLTemperatureRangeGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightCTLTemperatureRangeSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightCTLTemperatureRangeSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightCTLTemperatureRangeStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightCTLTemperatureSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightCTLTemperatureSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightCTLTemperatureStatus
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LightCTLMessagesTest {

    private val transition = TransitionParameters(
        transitionTime = TransitionTime(rawValue = 0x45u),
        delay = 0x0Au
    )

    @Test
    fun testOpCodes() {
        assertEquals(0x825Du, LightCTLGet.opCode)
        assertEquals(0x825Eu, LightCTLSet.opCode)
        assertEquals(0x825Fu, LightCTLSetUnacknowledged.opCode)
        assertEquals(0x8260u, LightCTLStatus.opCode)
        assertEquals(0x8261u, LightCTLTemperatureGet.opCode)
        assertEquals(0x8262u, LightCTLTemperatureRangeGet.opCode)
        assertEquals(0x8263u, LightCTLTemperatureRangeStatus.opCode)
        assertEquals(0x8264u, LightCTLTemperatureSet.opCode)
        assertEquals(0x8265u, LightCTLTemperatureSetUnacknowledged.opCode)
        assertEquals(0x8266u, LightCTLTemperatureStatus.opCode)
        assertEquals(0x8267u, LightCTLDefaultGet.opCode)
        assertEquals(0x8268u, LightCTLDefaultStatus.opCode)
        assertEquals(0x8269u, LightCTLDefaultSet.opCode)
        assertEquals(0x826Au, LightCTLDefaultSetUnacknowledged.opCode)
        assertEquals(0x826Bu, LightCTLTemperatureRangeSet.opCode)
        assertEquals(0x826Cu, LightCTLTemperatureRangeSetUnacknowledged.opCode)
    }

    @Test
    fun testAcknowledgedMessagesRespondWithTheMatchingStatus() {
        assertEquals(LightCTLStatus.opCode, LightCTLGet().responseOpCode)
        assertEquals(
            LightCTLStatus.opCode,
            LightCTLSet(lightness = 1u, temperature = 800u, deltaUV = 0).responseOpCode
        )
        assertEquals(LightCTLTemperatureStatus.opCode, LightCTLTemperatureGet().responseOpCode)
        assertEquals(
            LightCTLTemperatureStatus.opCode,
            LightCTLTemperatureSet(temperature = 800u, deltaUV = 0).responseOpCode
        )
        assertEquals(
            LightCTLTemperatureRangeStatus.opCode,
            LightCTLTemperatureRangeGet().responseOpCode
        )
        assertEquals(
            LightCTLTemperatureRangeStatus.opCode,
            LightCTLTemperatureRangeSet(min = 800u, max = 20000u).responseOpCode
        )
        assertEquals(LightCTLDefaultStatus.opCode, LightCTLDefaultGet().responseOpCode)
        assertEquals(
            LightCTLDefaultStatus.opCode,
            LightCTLDefaultSet(lightness = 1u, temperature = 800u, deltaUV = 0).responseOpCode
        )
    }

    @Test
    fun testGetMessagesHaveNoParameters() {
        assertNull(LightCTLGet().parameters)
        assertNull(LightCTLTemperatureGet().parameters)
        assertNull(LightCTLTemperatureRangeGet().parameters)
        assertNull(LightCTLDefaultGet().parameters)
        assertNotNull(LightCTLGet.init(null))
        assertNotNull(LightCTLTemperatureGet.init(byteArrayOf()))
        assertNotNull(LightCTLTemperatureRangeGet.init(null))
        assertNotNull(LightCTLDefaultGet.init(null))
        assertNull(LightCTLGet.init(byteArrayOf(0x00)))
        assertNull(LightCTLTemperatureGet.init(byteArrayOf(0x00)))
        assertNull(LightCTLTemperatureRangeGet.init(byteArrayOf(0x00)))
        assertNull(LightCTLDefaultGet.init(byteArrayOf(0x00)))
    }

    @Test
    fun testSetParameters() {
        // lightness 0x1234, temperature 0x1388 (5000 K), deltaUV -2 (0xFFFE), tid 7
        assertContentEquals(
            byteArrayOf(0x34, 0x12, 0x88.toByte(), 0x13, 0xFE.toByte(), 0xFF.toByte(), 0x07),
            LightCTLSet(tid = 7u, lightness = 0x1234u, temperature = 5000u, deltaUV = -2).parameters
        )
        assertContentEquals(
            byteArrayOf(
                0x34, 0x12, 0x88.toByte(), 0x13, 0xFE.toByte(), 0xFF.toByte(), 0x07, 0x45, 0x0A
            ),
            LightCTLSetUnacknowledged(
                tid = 7u,
                lightness = 0x1234u,
                temperature = 5000u,
                deltaUV = -2,
                transitionParams = transition
            ).parameters
        )
        assertContentEquals(
            byteArrayOf(0x88.toByte(), 0x13, 0x00, 0x80.toByte(), 0x07),
            LightCTLTemperatureSet(
                tid = 7u,
                temperature = 5000u,
                deltaUV = Short.MIN_VALUE
            ).parameters
        )
        assertContentEquals(
            byteArrayOf(0x88.toByte(), 0x13, 0xFF.toByte(), 0x7F, 0x07, 0x45, 0x0A),
            LightCTLTemperatureSetUnacknowledged(
                tid = 7u,
                temperature = 5000u,
                deltaUV = Short.MAX_VALUE,
                transitionParams = transition
            ).parameters
        )
    }

    @Test
    fun testTemperatureIsCoercedToTheValidRange() {
        assertEquals(800u.toUShort(), LightCTLSet(lightness = 0u, temperature = 0u, deltaUV = 0).temperature)
        assertEquals(
            20000u.toUShort(),
            LightCTLTemperatureSet(temperature = 0xFFFFu, deltaUV = 0).temperature
        )
        assertEquals(
            800u.toUShort(),
            LightCTLStatus(lightness = 0u, temperature = 1u).temperature
        )
    }

    @Test
    fun testSetInitializers() {
        val set = LightCTLSet.init(
            byteArrayOf(0x34, 0x12, 0x88.toByte(), 0x13, 0xFE.toByte(), 0xFF.toByte(), 0x07)
        )
        assertNotNull(set)
        assertEquals(0x1234u.toUShort(), set.lightness)
        assertEquals(5000u.toUShort(), set.temperature)
        assertEquals((-2).toShort(), set.deltaUV)
        assertEquals(7u.toUByte(), set.tid)
        assertNull(set.transitionTime)
        assertNull(set.delay)

        val unack = LightCTLSetUnacknowledged.init(
            byteArrayOf(
                0x34, 0x12, 0x88.toByte(), 0x13, 0xFE.toByte(), 0xFF.toByte(), 0x07, 0x45, 0x0A
            )
        )
        assertNotNull(unack)
        assertEquals((-2).toShort(), unack.deltaUV)
        assertEquals(TransitionTime(rawValue = 0x45u), unack.transitionTime)
        assertEquals(0x0Au.toUByte(), unack.delay)

        val temp = LightCTLTemperatureSet.init(
            byteArrayOf(0x88.toByte(), 0x13, 0x00, 0x80.toByte(), 0x07)
        )
        assertNotNull(temp)
        assertEquals(5000u.toUShort(), temp.temperature)
        assertEquals(Short.MIN_VALUE, temp.deltaUV)
        assertEquals(7u.toUByte(), temp.tid)
        assertNull(temp.transitionTime)

        val tempUnack = LightCTLTemperatureSetUnacknowledged.init(
            byteArrayOf(0x88.toByte(), 0x13, 0xFF.toByte(), 0x7F, 0x07, 0x45, 0x0A)
        )
        assertNotNull(tempUnack)
        assertEquals(Short.MAX_VALUE, tempUnack.deltaUV)
        assertEquals(TransitionTime(rawValue = 0x45u), tempUnack.transitionTime)
        assertEquals(0x0Au.toUByte(), tempUnack.delay)

        assertNull(LightCTLSet.init(null))
        assertNull(LightCTLSet.init(ByteArray(6)))
        assertNull(LightCTLSet.init(ByteArray(8)))
        assertNull(LightCTLSet.init(ByteArray(10)))
        assertNull(LightCTLSetUnacknowledged.init(ByteArray(8)))
        assertNull(LightCTLTemperatureSet.init(null))
        assertNull(LightCTLTemperatureSet.init(ByteArray(4)))
        assertNull(LightCTLTemperatureSet.init(ByteArray(6)))
        assertNull(LightCTLTemperatureSet.init(ByteArray(8)))
        assertNull(LightCTLTemperatureSetUnacknowledged.init(ByteArray(6)))
    }

    @Test
    fun testCtlStatus() {
        assertContentEquals(
            byteArrayOf(0x34, 0x12, 0x88.toByte(), 0x13),
            LightCTLStatus(lightness = 0x1234u, temperature = 5000u).parameters
        )
        assertContentEquals(
            byteArrayOf(0x34, 0x12, 0x88.toByte(), 0x13, 0x78, 0x56, 0x20, 0x4E, 0x45),
            LightCTLStatus(
                lightness = 0x1234u,
                temperature = 5000u,
                targetLightness = 0x5678u,
                targetTemperature = 20000u,
                remainingTime = TransitionTime(rawValue = 0x45u)
            ).parameters
        )

        val short = LightCTLStatus.init(byteArrayOf(0x34, 0x12, 0x88.toByte(), 0x13))
        assertNotNull(short)
        assertEquals(0x1234u.toUShort(), short.lightness)
        assertEquals(5000u.toUShort(), short.temperature)
        assertNull(short.targetLightness)
        assertNull(short.targetTemperature)
        assertNull(short.remainingTime)

        val long = LightCTLStatus.init(
            byteArrayOf(0x34, 0x12, 0x88.toByte(), 0x13, 0x78, 0x56, 0x20, 0x4E, 0x45)
        )
        assertNotNull(long)
        assertEquals(0x5678u.toUShort(), long.targetLightness)
        assertEquals(20000u.toUShort(), long.targetTemperature)
        assertEquals(TransitionTime(rawValue = 0x45u), long.remainingTime)

        assertNull(LightCTLStatus.init(null))
        assertNull(LightCTLStatus.init(ByteArray(3)))
        assertNull(LightCTLStatus.init(ByteArray(5)))
        assertNull(LightCTLStatus.init(ByteArray(8)))
        assertNull(LightCTLStatus.init(ByteArray(10)))
    }

    @Test
    fun testTemperatureStatus() {
        assertContentEquals(
            byteArrayOf(0x88.toByte(), 0x13, 0xFE.toByte(), 0xFF.toByte()),
            LightCTLTemperatureStatus(temperature = 5000u, deltaUV = -2).parameters
        )
        assertContentEquals(
            byteArrayOf(
                0x88.toByte(), 0x13, 0xFE.toByte(), 0xFF.toByte(),
                0x20, 0x4E, 0x00, 0x80.toByte(), 0x45
            ),
            LightCTLTemperatureStatus(
                temperature = 5000u,
                deltaUV = -2,
                targetTemperature = 20000u,
                targetDeltaUV = Short.MIN_VALUE,
                remainingTime = TransitionTime(rawValue = 0x45u)
            ).parameters
        )

        val short = LightCTLTemperatureStatus.init(
            byteArrayOf(0x88.toByte(), 0x13, 0xFE.toByte(), 0xFF.toByte())
        )
        assertNotNull(short)
        assertEquals(5000u.toUShort(), short.temperature)
        assertEquals((-2).toShort(), short.deltaUV)
        assertNull(short.targetTemperature)
        assertNull(short.targetDeltaUV)
        assertNull(short.remainingTime)

        val long = LightCTLTemperatureStatus.init(
            byteArrayOf(
                0x88.toByte(), 0x13, 0xFE.toByte(), 0xFF.toByte(),
                0x20, 0x4E, 0x00, 0x80.toByte(), 0x45
            )
        )
        assertNotNull(long)
        assertEquals(20000u.toUShort(), long.targetTemperature)
        assertEquals(Short.MIN_VALUE, long.targetDeltaUV)
        assertEquals(TransitionTime(rawValue = 0x45u), long.remainingTime)

        assertNull(LightCTLTemperatureStatus.init(null))
        assertNull(LightCTLTemperatureStatus.init(ByteArray(3)))
        assertNull(LightCTLTemperatureStatus.init(ByteArray(5)))
        assertNull(LightCTLTemperatureStatus.init(ByteArray(10)))
    }

    @Test
    fun testRangeSetMessages() {
        assertContentEquals(
            byteArrayOf(0x20, 0x03, 0x20, 0x4E),
            LightCTLTemperatureRangeSet(min = 800u, max = 20000u).parameters
        )
        assertContentEquals(
            byteArrayOf(0x20, 0x03, 0x20, 0x4E),
            LightCTLTemperatureRangeSetUnacknowledged(range = 800u..20000u).parameters
        )

        val set = LightCTLTemperatureRangeSet.init(byteArrayOf(0x20, 0x03, 0x20, 0x4E))
        assertNotNull(set)
        assertEquals(800u.toUShort(), set.min)
        assertEquals(20000u.toUShort(), set.max)
        assertEquals(800u..20000u, set.range)

        val unack = LightCTLTemperatureRangeSetUnacknowledged.init(byteArrayOf(0x20, 0x03, 0x20, 0x4E))
        assertNotNull(unack)
        assertEquals(800u.toUShort(), unack.min)
        assertEquals(20000u.toUShort(), unack.max)

        assertNull(LightCTLTemperatureRangeSet.init(null))
        assertNull(LightCTLTemperatureRangeSet.init(ByteArray(3)))
        assertNull(LightCTLTemperatureRangeSet.init(ByteArray(5)))
        assertNull(LightCTLTemperatureRangeSetUnacknowledged.init(ByteArray(3)))
        assertNull(LightCTLTemperatureRangeSetUnacknowledged.init(ByteArray(5)))
    }

    @Test
    fun testRangeStatus() {
        assertContentEquals(
            byteArrayOf(0x01, 0x20, 0x03, 0x20, 0x4E),
            LightCTLTemperatureRangeStatus(
                status = RangeMessageStatus.CANNOT_SET_RANGE_MIN,
                min = 800u,
                max = 20000u
            ).parameters
        )
        assertContentEquals(
            byteArrayOf(0x00, 0x20, 0x03, 0x20, 0x4E),
            LightCTLTemperatureRangeStatus(range = 800u..20000u).parameters
        )
        assertContentEquals(
            byteArrayOf(0x02, 0x05, 0x00, 0x06, 0x00),
            LightCTLTemperatureRangeStatus(
                status = RangeMessageStatus.CANNOT_SET_RANGE_MAX,
                request = LightCTLTemperatureRangeSet(min = 5u, max = 6u)
            ).parameters
        )

        val ok = LightCTLTemperatureRangeStatus.init(byteArrayOf(0x00, 0x20, 0x03, 0x20, 0x4E))
        assertNotNull(ok)
        assertEquals(RangeMessageStatus.SUCCESS, ok.status)
        assertTrue(ok.isSuccess)
        assertEquals(800u.toUShort(), ok.min)
        assertEquals(20000u.toUShort(), ok.max)

        val failed = LightCTLTemperatureRangeStatus.init(byteArrayOf(0x02, 0x20, 0x03, 0x20, 0x4E))
        assertNotNull(failed)
        assertEquals(RangeMessageStatus.CANNOT_SET_RANGE_MAX, failed.status)
        assertFalse(failed.isSuccess)

        assertNull(LightCTLTemperatureRangeStatus.init(null))
        assertNull(LightCTLTemperatureRangeStatus.init(ByteArray(4)))
        assertNull(LightCTLTemperatureRangeStatus.init(ByteArray(6)))
        // Unknown status code
        assertNull(LightCTLTemperatureRangeStatus.init(byteArrayOf(0x03, 0x20, 0x03, 0x20, 0x4E)))
    }

    @Test
    fun testDefaultMessages() {
        val expected = byteArrayOf(0x34, 0x12, 0x88.toByte(), 0x13, 0xFE.toByte(), 0xFF.toByte())
        assertContentEquals(
            expected,
            LightCTLDefaultSet(lightness = 0x1234u, temperature = 5000u, deltaUV = -2).parameters
        )
        assertContentEquals(
            expected,
            LightCTLDefaultSetUnacknowledged(
                lightness = 0x1234u,
                temperature = 5000u,
                deltaUV = -2
            ).parameters
        )
        assertContentEquals(
            expected,
            LightCTLDefaultStatus(lightness = 0x1234u, temperature = 5000u, deltaUV = -2).parameters
        )

        val set = LightCTLDefaultSet.init(expected)
        assertNotNull(set)
        assertEquals(0x1234u.toUShort(), set.lightness)
        assertEquals(5000u.toUShort(), set.temperature)
        assertEquals((-2).toShort(), set.deltaUV)

        val unack = LightCTLDefaultSetUnacknowledged.init(expected)
        assertNotNull(unack)
        assertEquals((-2).toShort(), unack.deltaUV)

        val status = LightCTLDefaultStatus.init(expected)
        assertNotNull(status)
        assertEquals(0x1234u.toUShort(), status.lightness)
        assertEquals(5000u.toUShort(), status.temperature)
        assertEquals((-2).toShort(), status.deltaUV)

        assertNull(LightCTLDefaultSet.init(null))
        assertNull(LightCTLDefaultSet.init(ByteArray(5)))
        assertNull(LightCTLDefaultSet.init(ByteArray(7)))
        assertNull(LightCTLDefaultSetUnacknowledged.init(ByteArray(5)))
        assertNull(LightCTLDefaultStatus.init(ByteArray(7)))
    }
}
