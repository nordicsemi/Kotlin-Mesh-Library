package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDefaultTransitionTimeStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnPowerUpGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnPowerUpSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnPowerUpSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnPowerUpStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericPowerDefaultGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericPowerDefaultSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericPowerDefaultSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericPowerDefaultStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericPowerLastGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericPowerLastStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericPowerLevelGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericPowerLevelSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericPowerLevelSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericPowerLevelStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericPowerRangeGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericPowerRangeSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericPowerRangeSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericPowerRangeStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.OnPowerUp
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.util.TransitionParameters
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GenericPowerMessagesTest {

    private val transition = TransitionParameters(
        transitionTime = TransitionTime(rawValue = 0x45u),
        delay = 0x0Au
    )

    @Test
    fun testOpCodes() {
        assertEquals(0x820Du, GenericDefaultTransitionTimeGet.opCode)
        assertEquals(0x820Eu, GenericDefaultTransitionTimeSet.opCode)
        assertEquals(0x820Fu, GenericDefaultTransitionTimeSetUnacknowledged.opCode)
        assertEquals(0x8210u, GenericDefaultTransitionTimeStatus.opCode)
        assertEquals(0x8211u, GenericOnPowerUpGet.opCode)
        assertEquals(0x8212u, GenericOnPowerUpStatus.opCode)
        assertEquals(0x8213u, GenericOnPowerUpSet.opCode)
        assertEquals(0x8214u, GenericOnPowerUpSetUnacknowledged.opCode)
        assertEquals(0x8215u, GenericPowerLevelGet.opCode)
        assertEquals(0x8216u, GenericPowerLevelSet.opCode)
        assertEquals(0x8217u, GenericPowerLevelSetUnacknowledged.opCode)
        assertEquals(0x8218u, GenericPowerLevelStatus.opCode)
        assertEquals(0x8219u, GenericPowerLastGet.opCode)
        assertEquals(0x821Au, GenericPowerLastStatus.opCode)
        assertEquals(0x821Bu, GenericPowerDefaultGet.opCode)
        assertEquals(0x821Cu, GenericPowerDefaultStatus.opCode)
        assertEquals(0x821Du, GenericPowerRangeGet.opCode)
        assertEquals(0x821Eu, GenericPowerRangeStatus.opCode)
        assertEquals(0x821Fu, GenericPowerDefaultSet.opCode)
        assertEquals(0x8220u, GenericPowerDefaultSetUnacknowledged.opCode)
        assertEquals(0x8221u, GenericPowerRangeSet.opCode)
        assertEquals(0x8222u, GenericPowerRangeSetUnacknowledged.opCode)
    }

    @Test
    fun testInstanceOpCodesMatchInitializers() {
        assertEquals(0x8216u, GenericPowerLevelSet(power = 1u).opCode)
        assertEquals(0x8217u, GenericPowerLevelSetUnacknowledged(power = 1u).opCode)
        assertEquals(0x8218u, GenericPowerLevelStatus(power = 1u).opCode)
        assertEquals(0x821Fu, GenericPowerDefaultSet(power = 1u).opCode)
        assertEquals(0x8221u, GenericPowerRangeSet(min = 1u, max = 2u).opCode)
        assertEquals(0x8213u, GenericOnPowerUpSet(state = OnPowerUp.OFF).opCode)
        assertEquals(
            0x820Fu,
            GenericDefaultTransitionTimeSetUnacknowledged(
                transitionTime = TransitionTime(rawValue = 0u)
            ).opCode
        )
    }

    @Test
    fun testAcknowledgedMessagesRespondWithTheMatchingStatus() {
        assertEquals(GenericPowerLevelStatus.opCode, GenericPowerLevelGet().responseOpCode)
        assertEquals(
            GenericPowerLevelStatus.opCode,
            GenericPowerLevelSet(power = 1u).responseOpCode
        )
        assertEquals(GenericPowerLastStatus.opCode, GenericPowerLastGet().responseOpCode)
        assertEquals(GenericPowerDefaultStatus.opCode, GenericPowerDefaultGet().responseOpCode)
        assertEquals(
            GenericPowerDefaultStatus.opCode,
            GenericPowerDefaultSet(power = 1u).responseOpCode
        )
        assertEquals(GenericPowerRangeStatus.opCode, GenericPowerRangeGet().responseOpCode)
        assertEquals(
            GenericPowerRangeStatus.opCode,
            GenericPowerRangeSet(min = 1u, max = 2u).responseOpCode
        )
        assertEquals(GenericOnPowerUpStatus.opCode, GenericOnPowerUpGet().responseOpCode)
        assertEquals(
            GenericOnPowerUpStatus.opCode,
            GenericOnPowerUpSet(state = OnPowerUp.RESTORE).responseOpCode
        )
        assertEquals(
            GenericDefaultTransitionTimeStatus.opCode,
            GenericDefaultTransitionTimeGet().responseOpCode
        )
        assertEquals(
            GenericDefaultTransitionTimeStatus.opCode,
            GenericDefaultTransitionTimeSet(
                transitionTime = TransitionTime(rawValue = 0u)
            ).responseOpCode
        )
    }

    @Test
    fun testGetMessagesHaveNoParameters() {
        assertNull(GenericPowerLevelGet().parameters)
        assertNull(GenericPowerLastGet().parameters)
        assertNull(GenericPowerDefaultGet().parameters)
        assertNull(GenericPowerRangeGet().parameters)
        assertNull(GenericOnPowerUpGet().parameters)
        assertNull(GenericDefaultTransitionTimeGet().parameters)

        assertNotNull(GenericPowerLevelGet.init(null))
        assertNotNull(GenericPowerLevelGet.init(byteArrayOf()))
        assertNull(GenericPowerLevelGet.init(byteArrayOf(0x00)))
        assertNotNull(GenericPowerLastGet.init(null))
        assertNull(GenericPowerLastGet.init(byteArrayOf(0x00)))
        assertNotNull(GenericPowerDefaultGet.init(null))
        assertNull(GenericPowerDefaultGet.init(byteArrayOf(0x00)))
        assertNotNull(GenericPowerRangeGet.init(null))
        assertNull(GenericPowerRangeGet.init(byteArrayOf(0x00)))
        assertNotNull(GenericOnPowerUpGet.init(null))
        assertNull(GenericOnPowerUpGet.init(byteArrayOf(0x00)))
        assertNotNull(GenericDefaultTransitionTimeGet.init(null))
        assertNull(GenericDefaultTransitionTimeGet.init(byteArrayOf(0x00)))
    }

    @Test
    fun testPowerLevelSetParameters() {
        assertContentEquals(
            byteArrayOf(0x34, 0x12, 0x07),
            GenericPowerLevelSet(tid = 7u, power = 0x1234u).parameters
        )
        assertContentEquals(
            byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0x07, 0x45, 0x0A),
            GenericPowerLevelSetUnacknowledged(
                tid = 7u,
                power = 0xFFFFu,
                transitionParams = transition
            ).parameters
        )
        assertContentEquals(
            byteArrayOf(0x34, 0x12, 0x07, 0x45, 0x0A),
            GenericPowerLevelSet(tid = 7u, power = 0x1234u, transitionParams = transition).parameters
        )
    }

    @Test
    fun testPowerLevelSetInitializers() {
        val set = GenericPowerLevelSet.init(byteArrayOf(0x34, 0x12, 0x07))
        assertNotNull(set)
        assertEquals(0x1234u.toUShort(), set.power)
        assertEquals(7u.toUByte(), set.tid)
        assertNull(set.transitionTime)
        assertNull(set.delay)

        val unack = GenericPowerLevelSetUnacknowledged.init(
            byteArrayOf(0x34, 0x12, 0x07, 0x45, 0x0A)
        )
        assertNotNull(unack)
        assertEquals(0x1234u.toUShort(), unack.power)
        assertEquals(TransitionTime(rawValue = 0x45u), unack.transitionTime)
        assertEquals(0x0Au.toUByte(), unack.delay)

        assertNull(GenericPowerLevelSet.init(byteArrayOf(0x34, 0x12)))
        assertNull(GenericPowerLevelSet.init(byteArrayOf(0x34, 0x12, 0x07, 0x45)))
        assertNull(GenericPowerLevelSet.init(byteArrayOf(0x34, 0x12, 0x07, 0x45, 0x0A, 0x00)))
        assertNull(GenericPowerLevelSetUnacknowledged.init(null))
    }

    @Test
    fun testPowerLevelStatus() {
        assertContentEquals(
            byteArrayOf(0x34, 0x12),
            GenericPowerLevelStatus(power = 0x1234u).parameters
        )
        assertContentEquals(
            byteArrayOf(0x34, 0x12, 0x78, 0x56, 0x45),
            GenericPowerLevelStatus(
                power = 0x1234u,
                targetPower = 0x5678u,
                remainingTime = TransitionTime(rawValue = 0x45u)
            ).parameters
        )

        val short = GenericPowerLevelStatus.init(byteArrayOf(0x34, 0x12))
        assertNotNull(short)
        assertEquals(0x1234u.toUShort(), short.power)
        assertNull(short.targetPower)
        assertNull(short.remainingTime)

        val long = GenericPowerLevelStatus.init(byteArrayOf(0x34, 0x12, 0x78, 0x56, 0x45))
        assertNotNull(long)
        assertEquals(0x1234u.toUShort(), long.power)
        assertEquals(0x5678u.toUShort(), long.targetPower)
        assertEquals(TransitionTime(rawValue = 0x45u), long.remainingTime)

        assertNull(GenericPowerLevelStatus.init(null))
        assertNull(GenericPowerLevelStatus.init(byteArrayOf(0x34)))
        assertNull(GenericPowerLevelStatus.init(byteArrayOf(0x34, 0x12, 0x78)))
        assertNull(GenericPowerLevelStatus.init(byteArrayOf(0x34, 0x12, 0x78, 0x56, 0x45, 0x00)))
    }

    @Test
    fun testPowerLastAndDefault() {
        assertContentEquals(byteArrayOf(0x34, 0x12), GenericPowerLastStatus(power = 0x1234u).parameters)
        assertEquals(0x1234u.toUShort(), GenericPowerLastStatus.init(byteArrayOf(0x34, 0x12))?.power)
        assertNull(GenericPowerLastStatus.init(null))
        assertNull(GenericPowerLastStatus.init(byteArrayOf(0x34)))
        assertNull(GenericPowerLastStatus.init(byteArrayOf(0x34, 0x12, 0x00)))

        assertContentEquals(
            byteArrayOf(0x34, 0x12),
            GenericPowerDefaultSet(power = 0x1234u).parameters
        )
        assertContentEquals(
            byteArrayOf(0x34, 0x12),
            GenericPowerDefaultSetUnacknowledged(power = 0x1234u).parameters
        )
        assertContentEquals(
            byteArrayOf(0x34, 0x12),
            GenericPowerDefaultStatus(power = 0x1234u).parameters
        )
        assertEquals(0x1234u.toUShort(), GenericPowerDefaultSet.init(byteArrayOf(0x34, 0x12))?.power)
        assertEquals(
            0x1234u.toUShort(),
            GenericPowerDefaultSetUnacknowledged.init(byteArrayOf(0x34, 0x12))?.power
        )
        assertEquals(0x1234u.toUShort(), GenericPowerDefaultStatus.init(byteArrayOf(0x34, 0x12))?.power)
        assertNull(GenericPowerDefaultSet.init(byteArrayOf(0x34)))
        assertNull(GenericPowerDefaultSetUnacknowledged.init(null))
        assertNull(GenericPowerDefaultStatus.init(byteArrayOf(0x34, 0x12, 0x00)))
    }

    @Test
    fun testPowerRange() {
        assertContentEquals(
            byteArrayOf(0x34, 0x12, 0x78, 0x56),
            GenericPowerRangeSet(min = 0x1234u, max = 0x5678u).parameters
        )
        assertContentEquals(
            byteArrayOf(0x34, 0x12, 0x78, 0x56),
            GenericPowerRangeSetUnacknowledged(range = 0x1234u..0x5678u).parameters
        )
        assertEquals(0x1234u..0x5678u, GenericPowerRangeSet(min = 0x1234u, max = 0x5678u).range)

        val set = GenericPowerRangeSet.init(byteArrayOf(0x34, 0x12, 0x78, 0x56))
        assertNotNull(set)
        assertEquals(0x1234u.toUShort(), set.min)
        assertEquals(0x5678u.toUShort(), set.max)
        val unack = GenericPowerRangeSetUnacknowledged.init(byteArrayOf(0x34, 0x12, 0x78, 0x56))
        assertNotNull(unack)
        assertEquals(0x1234u.toUShort(), unack.min)
        assertEquals(0x5678u.toUShort(), unack.max)
        assertNull(GenericPowerRangeSet.init(byteArrayOf(0x34, 0x12, 0x78)))
        assertNull(GenericPowerRangeSet.init(byteArrayOf(0x34, 0x12, 0x78, 0x56, 0x00)))
        assertNull(GenericPowerRangeSetUnacknowledged.init(null))
    }

    @Test
    fun testPowerRangeStatus() {
        val success = GenericPowerRangeStatus(range = 0x1234u..0x5678u)
        assertTrue(success.isSuccess)
        assertContentEquals(byteArrayOf(0x00, 0x34, 0x12, 0x78, 0x56), success.parameters)

        val failed = GenericPowerRangeStatus(
            status = RangeMessageStatus.CANNOT_SET_RANGE_MAX,
            request = GenericPowerRangeSet(min = 0x1234u, max = 0x5678u)
        )
        assertFalse(failed.isSuccess)
        assertContentEquals(byteArrayOf(0x02, 0x34, 0x12, 0x78, 0x56), failed.parameters)

        val parsed = GenericPowerRangeStatus.init(byteArrayOf(0x01, 0x34, 0x12, 0x78, 0x56))
        assertNotNull(parsed)
        assertEquals(RangeMessageStatus.CANNOT_SET_RANGE_MIN, parsed.status)
        assertEquals(0x1234u.toUShort(), parsed.min)
        assertEquals(0x5678u.toUShort(), parsed.max)

        assertNull(GenericPowerRangeStatus.init(null))
        assertNull(GenericPowerRangeStatus.init(byteArrayOf(0x00, 0x34, 0x12, 0x78)))
        assertNull(GenericPowerRangeStatus.init(byteArrayOf(0x03, 0x34, 0x12, 0x78, 0x56)))
    }

    @Test
    fun testOnPowerUp() {
        assertContentEquals(byteArrayOf(0x00), GenericOnPowerUpSet(state = OnPowerUp.OFF).parameters)
        assertContentEquals(
            byteArrayOf(0x01),
            GenericOnPowerUpSetUnacknowledged(state = OnPowerUp.DEFAULT).parameters
        )
        assertContentEquals(
            byteArrayOf(0x02),
            GenericOnPowerUpStatus(state = OnPowerUp.RESTORE).parameters
        )

        assertEquals(OnPowerUp.OFF, GenericOnPowerUpSet.init(byteArrayOf(0x00))?.state)
        assertEquals(OnPowerUp.DEFAULT, GenericOnPowerUpSetUnacknowledged.init(byteArrayOf(0x01))?.state)
        assertEquals(OnPowerUp.RESTORE, GenericOnPowerUpStatus.init(byteArrayOf(0x02))?.state)

        // Reserved values and invalid lengths.
        assertNull(GenericOnPowerUpSet.init(byteArrayOf(0x03)))
        assertNull(GenericOnPowerUpStatus.init(byteArrayOf(0xFF.toByte())))
        assertNull(GenericOnPowerUpSet.init(byteArrayOf()))
        assertNull(GenericOnPowerUpSetUnacknowledged.init(byteArrayOf(0x00, 0x00)))
        assertNull(GenericOnPowerUpStatus.init(null))
    }

    @Test
    fun testDefaultTransitionTime() {
        val time = TransitionTime(rawValue = 0x45u)
        assertContentEquals(
            byteArrayOf(0x45),
            GenericDefaultTransitionTimeSet(transitionTime = time).parameters
        )
        assertContentEquals(
            byteArrayOf(0x45),
            GenericDefaultTransitionTimeSetUnacknowledged(transitionTime = time).parameters
        )
        assertContentEquals(
            byteArrayOf(0x45),
            GenericDefaultTransitionTimeStatus(transitionTime = time).parameters
        )

        assertEquals(time, GenericDefaultTransitionTimeSet.init(byteArrayOf(0x45))?.transitionTime)
        assertEquals(
            time,
            GenericDefaultTransitionTimeSetUnacknowledged.init(byteArrayOf(0x45))?.transitionTime
        )
        assertEquals(time, GenericDefaultTransitionTimeStatus.init(byteArrayOf(0x45))?.transitionTime)

        assertNull(GenericDefaultTransitionTimeSet.init(byteArrayOf()))
        assertNull(GenericDefaultTransitionTimeSet.init(byteArrayOf(0x45, 0x00)))
        assertNull(GenericDefaultTransitionTimeSetUnacknowledged.init(null))
        assertNull(GenericDefaultTransitionTimeStatus.init(byteArrayOf(0x45, 0x00)))
    }

    @Test
    fun testToString() {
        assertEquals("GenericPowerLevelGet", GenericPowerLevelGet().toString())
        assertEquals("GenericPowerLastStatus(power: 5)", GenericPowerLastStatus(power = 5u).toString())
        assertEquals(
            "GenericPowerLevelSet(tid: 7, power: 5)",
            GenericPowerLevelSet(tid = 7u, power = 5u).toString()
        )
        assertEquals(
            "GenericPowerRangeSet(min: 1, max: 2)",
            GenericPowerRangeSet(min = 1u, max = 2u).toString()
        )
    }
}
