package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessDefaultGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessDefaultSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessDefaultSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessDefaultStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessLastGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessLastStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessLinearGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessLinearSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessLinearSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessLinearStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessRangeGet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessRangeSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessRangeSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessRangeStatus
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessSet
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.lighting.LightLightnessStatus
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LightLightnessMessagesTest {

    private val transition = TransitionParameters(
        transitionTime = TransitionTime(rawValue = 0x45u),
        delay = 0x0Au
    )

    @Test
    fun testOpCodes() {
        assertEquals(0x824Bu, LightLightnessGet.opCode)
        assertEquals(0x824Cu, LightLightnessSet.opCode)
        assertEquals(0x824Du, LightLightnessSetUnacknowledged.opCode)
        assertEquals(0x824Eu, LightLightnessStatus.opCode)
        assertEquals(0x824Fu, LightLightnessLinearGet.opCode)
        assertEquals(0x8250u, LightLightnessLinearSet.opCode)
        assertEquals(0x8251u, LightLightnessLinearSetUnacknowledged.opCode)
        assertEquals(0x8252u, LightLightnessLinearStatus.opCode)
        assertEquals(0x8253u, LightLightnessLastGet.opCode)
        assertEquals(0x8254u, LightLightnessLastStatus.opCode)
        assertEquals(0x8255u, LightLightnessDefaultGet.opCode)
        assertEquals(0x8256u, LightLightnessDefaultStatus.opCode)
        assertEquals(0x8257u, LightLightnessRangeGet.opCode)
        assertEquals(0x8258u, LightLightnessRangeStatus.opCode)
        assertEquals(0x8259u, LightLightnessDefaultSet.opCode)
        assertEquals(0x825Au, LightLightnessDefaultSetUnacknowledged.opCode)
        assertEquals(0x825Bu, LightLightnessRangeSet.opCode)
        assertEquals(0x825Cu, LightLightnessRangeSetUnacknowledged.opCode)
    }

    @Test
    fun testAcknowledgedMessagesRespondWithTheMatchingStatus() {
        assertEquals(LightLightnessStatus.opCode, LightLightnessGet().responseOpCode)
        assertEquals(
            LightLightnessStatus.opCode,
            LightLightnessSet(lightness = 1u).responseOpCode
        )
        assertEquals(LightLightnessLinearStatus.opCode, LightLightnessLinearGet().responseOpCode)
        assertEquals(
            LightLightnessLinearStatus.opCode,
            LightLightnessLinearSet(lightness = 1u).responseOpCode
        )
        assertEquals(LightLightnessLastStatus.opCode, LightLightnessLastGet().responseOpCode)
        assertEquals(
            LightLightnessDefaultStatus.opCode,
            LightLightnessDefaultGet().responseOpCode
        )
        assertEquals(
            LightLightnessDefaultStatus.opCode,
            LightLightnessDefaultSet(lightness = 1u).responseOpCode
        )
        assertEquals(LightLightnessRangeStatus.opCode, LightLightnessRangeGet().responseOpCode)
        assertEquals(
            LightLightnessRangeStatus.opCode,
            LightLightnessRangeSet(min = 1u, max = 2u).responseOpCode
        )
    }

    @Test
    fun testGetMessagesHaveNoParameters() {
        assertNull(LightLightnessGet().parameters)
        assertNull(LightLightnessLinearGet().parameters)
        assertNull(LightLightnessLastGet().parameters)
        assertNull(LightLightnessDefaultGet().parameters)
        assertNull(LightLightnessRangeGet().parameters)
        assertNotNull(LightLightnessGet.init(null))
        assertNull(LightLightnessGet.init(byteArrayOf(0x00)))
    }

    @Test
    fun testSetParameters() {
        assertContentEquals(
            byteArrayOf(0x34, 0x12, 0x07),
            LightLightnessSet(tid = 7u, lightness = 0x1234u).parameters
        )
        assertContentEquals(
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0x07, 0x45, 0x0A),
            LightLightnessSetUnacknowledged(
                tid = 7u,
                lightness = 0xFFFFu,
                transitionParams = transition
            ).parameters
        )
        assertContentEquals(
            byteArrayOf(0x34, 0x12, 0x07),
            LightLightnessLinearSet(tid = 7u, lightness = 0x1234u).parameters
        )
        assertContentEquals(
            byteArrayOf(0x34, 0x12, 0x07, 0x45, 0x0A),
            LightLightnessLinearSetUnacknowledged(
                tid = 7u,
                lightness = 0x1234u,
                transitionParams = transition
            ).parameters
        )
    }

    @Test
    fun testSetInitializers() {
        val set = LightLightnessSet.init(byteArrayOf(0x34, 0x12, 0x07))
        assertNotNull(set)
        assertEquals(0x1234u.toUShort(), set.lightness)
        assertEquals(7u.toUByte(), set.tid)
        assertNull(set.transitionTime)
        assertNull(set.delay)

        val unack = LightLightnessLinearSetUnacknowledged.init(
            byteArrayOf(0x34, 0x12, 0x07, 0x45, 0x0A)
        )
        assertNotNull(unack)
        assertEquals(0x1234u.toUShort(), unack.lightness)
        assertEquals(TransitionTime(rawValue = 0x45u), unack.transitionTime)
        assertEquals(0x0Au.toUByte(), unack.delay)

        assertNull(LightLightnessSet.init(byteArrayOf(0x34, 0x12)))
        assertNull(LightLightnessSet.init(byteArrayOf(0x34, 0x12, 0x07, 0x45)))
        assertNull(LightLightnessSetUnacknowledged.init(null))
    }

    @Test
    fun testStatusParameters() {
        assertContentEquals(
            byteArrayOf(0x34, 0x12),
            LightLightnessStatus(lightness = 0x1234u).parameters
        )
        assertContentEquals(
            byteArrayOf(0x34, 0x12, 0x78, 0x56, 0x45),
            LightLightnessStatus(
                lightness = 0x1234u,
                targetLightness = 0x5678u,
                remainingTime = TransitionTime(rawValue = 0x45u)
            ).parameters
        )
        assertContentEquals(
            byteArrayOf(0x34, 0x12, 0x78, 0x56, 0x45),
            LightLightnessLinearStatus(
                lightness = 0x1234u,
                targetLightness = 0x5678u,
                remainingTime = TransitionTime(rawValue = 0x45u)
            ).parameters
        )
    }

    @Test
    fun testStatusInitializers() {
        val short = LightLightnessStatus.init(byteArrayOf(0x34, 0x12))
        assertNotNull(short)
        assertEquals(0x1234u.toUShort(), short.lightness)
        assertNull(short.targetLightness)
        assertNull(short.remainingTime)

        val long = LightLightnessLinearStatus.init(byteArrayOf(0x34, 0x12, 0x78, 0x56, 0x45))
        assertNotNull(long)
        assertEquals(0x1234u.toUShort(), long.lightness)
        assertEquals(0x5678u.toUShort(), long.targetLightness)
        assertEquals(TransitionTime(rawValue = 0x45u), long.remainingTime)

        assertNull(LightLightnessStatus.init(byteArrayOf(0x34)))
        assertNull(LightLightnessStatus.init(byteArrayOf(0x34, 0x12, 0x78)))
        assertNull(LightLightnessLinearStatus.init(null))
    }

    @Test
    fun testLastAndDefaultMessages() {
        assertContentEquals(
            byteArrayOf(0x34, 0x12),
            LightLightnessLastStatus(lightness = 0x1234u).parameters
        )
        assertEquals(
            0x1234u.toUShort(),
            LightLightnessLastStatus.init(byteArrayOf(0x34, 0x12))?.lightness
        )
        assertNull(LightLightnessLastStatus.init(byteArrayOf(0x34)))

        assertContentEquals(
            byteArrayOf(0x34, 0x12),
            LightLightnessDefaultSet(lightness = 0x1234u).parameters
        )
        assertContentEquals(
            byteArrayOf(0x34, 0x12),
            LightLightnessDefaultSetUnacknowledged(lightness = 0x1234u).parameters
        )
        assertContentEquals(
            byteArrayOf(0x34, 0x12),
            LightLightnessDefaultStatus(lightness = 0x1234u).parameters
        )
        assertEquals(
            0x1234u.toUShort(),
            LightLightnessDefaultSet.init(byteArrayOf(0x34, 0x12))?.lightness
        )
        assertEquals(
            0x1234u.toUShort(),
            LightLightnessDefaultSetUnacknowledged.init(byteArrayOf(0x34, 0x12))?.lightness
        )
        assertEquals(
            0x1234u.toUShort(),
            LightLightnessDefaultStatus.init(byteArrayOf(0x34, 0x12))?.lightness
        )
        assertNull(LightLightnessDefaultSet.init(byteArrayOf(0x34, 0x12, 0x00)))
    }

    @Test
    fun testRangeSetParameters() {
        assertContentEquals(
            byteArrayOf(0x01, 0x00, 0x34, 0x12),
            LightLightnessRangeSet(min = 1u, max = 0x1234u).parameters
        )
        assertContentEquals(
            byteArrayOf(0x01, 0x00, 0x34, 0x12),
            LightLightnessRangeSetUnacknowledged(range = 1u..0x1234u)
                .parameters
        )
        val set = LightLightnessRangeSet.init(byteArrayOf(0x01, 0x00, 0x34, 0x12))
        assertNotNull(set)
        assertEquals(1u..0x1234u, set.range)
        assertNull(LightLightnessRangeSet.init(byteArrayOf(0x01, 0x00, 0x34)))
        assertNotNull(LightLightnessRangeSetUnacknowledged.init(byteArrayOf(1, 0, 2, 0)))
    }

    @Test
    fun testRangeStatusParameters() {
        assertContentEquals(
            byteArrayOf(0x01, 0x01, 0x00, 0x34, 0x12),
            LightLightnessRangeStatus(
                status = RangeMessageStatus.CANNOT_SET_RANGE_MIN,
                min = 1u,
                max = 0x1234u
            ).parameters
        )
        assertContentEquals(
            byteArrayOf(0x00, 0x01, 0x00, 0x34, 0x12),
            LightLightnessRangeStatus(range = 1u..0x1234u).parameters
        )
        assertContentEquals(
            byteArrayOf(0x02, 0x05, 0x00, 0x06, 0x00),
            LightLightnessRangeStatus(
                status = RangeMessageStatus.CANNOT_SET_RANGE_MAX,
                request = LightLightnessRangeSet(min = 5u, max = 6u)
            ).parameters
        )
    }

    @Test
    fun testRangeStatusInitializer() {
        val status = LightLightnessRangeStatus.init(byteArrayOf(0x00, 0x01, 0x00, 0x34, 0x12))
        assertNotNull(status)
        assertEquals(RangeMessageStatus.SUCCESS, status.status)
        assertTrue(status.isSuccess)
        assertEquals(1u..0x1234u, status.range)

        val failed = LightLightnessRangeStatus.init(byteArrayOf(0x01, 0x01, 0x00, 0x34, 0x12))
        assertNotNull(failed)
        assertFalse(failed.isSuccess)
        assertEquals("Cannot Set Range Min", failed.message)

        // Unknown status code and wrong length.
        assertNull(LightLightnessRangeStatus.init(byteArrayOf(0x03, 0x01, 0x00, 0x34, 0x12)))
        assertNull(LightLightnessRangeStatus.init(byteArrayOf(0x00, 0x01, 0x00, 0x34)))
    }
}
