package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.messages.foundation.scheduler.SchedulerActionGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scheduler.SchedulerActionSet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scheduler.SchedulerActionSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scheduler.SchedulerActionStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scheduler.SchedulerGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scheduler.SchedulerStatus
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SchedulerMessagesTest {

    private fun bytes(vararg v: Int) = ByteArray(v.size) { v[it].toByte() }

    /** An entry with all fields set to their maximum bit value. */
    private val maxEntry = SchedulerRegistryEntry(
        year = SchedulerYear(0x7Fu),
        month = SchedulerMonth(0x0FFFu),
        day = SchedulerDay(0x1Fu),
        hour = SchedulerHour(0x1Fu),
        minute = SchedulerMinute(0x3Fu),
        second = SchedulerSecond(0x3Fu),
        dayOfWeek = SchedulerDayOfWeek(0x7Fu),
        action = SchedulerAction.NO_ACTION,
        transitionTime = TransitionTime(rawValue = 0xFFu),
        sceneNumber = 0xFFFFu
    )

    /** Entry where all the fields are 0 except those given. */
    private fun single(
        year: Int = 0, month: Int = 0, day: Int = 0, hour: Int = 0, minute: Int = 0,
        second: Int = 0, dow: Int = 0, action: SchedulerAction = SchedulerAction.TURN_OFF,
    ) = SchedulerRegistryEntry(
        year = SchedulerYear(year.toUByte()),
        month = SchedulerMonth(month.toUShort()),
        day = SchedulerDay(day.toUByte()),
        hour = SchedulerHour(hour.toUByte()),
        minute = SchedulerMinute(minute.toUByte()),
        second = SchedulerSecond(second.toUByte()),
        dayOfWeek = SchedulerDayOfWeek(dow.toUByte()),
        action = action,
        transitionTime = TransitionTime(rawValue = 0u),
        sceneNumber = 0u
    )

    private fun enc(entry: SchedulerRegistryEntry, index: Int = 0) =
        entry.toByteArray(index.toUByte())

    @Test
    fun testOpCodes() {
        assertEquals(0x8249u, SchedulerGet.opCode)
        assertEquals(0x824Au, SchedulerStatus.opCode)
        assertEquals(0x8248u, SchedulerActionGet.opCode)
        assertEquals(0x5Fu, SchedulerActionStatus.opCode)
        assertEquals(0x60u, SchedulerActionSet.opCode)
        assertEquals(0x61u, SchedulerActionSetUnacknowledged.opCode)
    }

    @Test
    fun testResponseOpCodes() {
        assertEquals(SchedulerStatus.opCode, SchedulerGet().responseOpCode)
        assertEquals(SchedulerActionStatus.opCode, SchedulerActionGet(0u).responseOpCode)
        assertEquals(
            SchedulerActionStatus.opCode,
            SchedulerActionSet(0u, SchedulerRegistryEntry()).responseOpCode
        )
    }

    @Test
    fun testSchedulerGet() {
        assertNull(SchedulerGet().parameters)
        assertNotNull(SchedulerGet.init(byteArrayOf()))
        assertNull(SchedulerGet.init(null))
        assertNull(SchedulerGet.init(byteArrayOf(0)))
    }

    @Test
    fun testSchedulerStatus() {
        val status = SchedulerStatus(0x8001u)
        assertContentEquals(bytes(0x01, 0x80), status.parameters)
        val decoded = SchedulerStatus.init(status.parameters) as SchedulerStatus
        assertEquals(0x8001u.toUShort(), decoded.schedules)
        assertTrue(decoded.isEntryInUse(0))
        assertTrue(decoded.isEntryInUse(15))
        assertFalse(decoded.isEntryInUse(1))
        assertNull(SchedulerStatus.init(byteArrayOf(0)))
        assertNull(SchedulerStatus.init(ByteArray(3)))
        assertNull(SchedulerStatus.init(null))
    }

    @Test
    fun testSchedulerActionGet() {
        assertContentEquals(bytes(0x0F), SchedulerActionGet(0x0Fu).parameters)
        val decoded = SchedulerActionGet.init(bytes(0x0F)) as SchedulerActionGet
        assertEquals(0x0Fu.toUByte(), decoded.index)
        assertNull(SchedulerActionGet.init(bytes(0x10)))
        assertNull(SchedulerActionGet.init(byteArrayOf()))
        assertNull(SchedulerActionGet.init(bytes(0, 0)))
        assertNull(SchedulerActionGet.init(null))
        assertFailsWith<IllegalArgumentException> { SchedulerActionGet(0x10u) }
    }

    @Test
    fun testAllOnesPacking() {
        // 4+7+12+5+5+6+6+7+4 = 56 bits, all ones, then transition time and scene number.
        val set = SchedulerActionSet(0x0Fu, maxEntry)
        assertContentEquals(
            bytes(0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF),
            set.parameters
        )
    }

    @Test
    fun testFieldOffsets() {
        // Index: bits 0..3
        assertContentEquals(bytes(0x01, 0, 0, 0, 0, 0, 0, 0, 0, 0), enc(single(), index = 1))
        assertContentEquals(bytes(0x0F, 0, 0, 0, 0, 0, 0, 0, 0, 0), enc(single(), index = 15))
        // Year: bits 4..10
        assertContentEquals(bytes(0x10, 0, 0, 0, 0, 0, 0, 0, 0, 0), enc(single(year = 1)))
        assertContentEquals(bytes(0xF0, 0x07, 0, 0, 0, 0, 0, 0, 0, 0), enc(single(year = 0x7F)))
        // Month: bits 11..22
        assertContentEquals(bytes(0, 0x08, 0, 0, 0, 0, 0, 0, 0, 0), enc(single(month = 1)))
        assertContentEquals(bytes(0, 0xF8, 0x7F, 0, 0, 0, 0, 0, 0, 0), enc(single(month = 0xFFF)))
        // Day: bits 23..27
        assertContentEquals(bytes(0, 0, 0x80, 0, 0, 0, 0, 0, 0, 0), enc(single(day = 1)))
        assertContentEquals(bytes(0, 0, 0x80, 0x0F, 0, 0, 0, 0, 0, 0), enc(single(day = 0x1F)))
        // Hour: bits 28..32
        assertContentEquals(bytes(0, 0, 0, 0x10, 0, 0, 0, 0, 0, 0), enc(single(hour = 1)))
        assertContentEquals(bytes(0, 0, 0, 0xF0, 0x01, 0, 0, 0, 0, 0), enc(single(hour = 0x1F)))
        // Minute: bits 33..38
        assertContentEquals(bytes(0, 0, 0, 0, 0x02, 0, 0, 0, 0, 0), enc(single(minute = 1)))
        assertContentEquals(bytes(0, 0, 0, 0, 0x7E, 0, 0, 0, 0, 0), enc(single(minute = 0x3F)))
        // Second: bits 39..44
        assertContentEquals(bytes(0, 0, 0, 0, 0x80, 0, 0, 0, 0, 0), enc(single(second = 1)))
        assertContentEquals(bytes(0, 0, 0, 0, 0x80, 0x1F, 0, 0, 0, 0), enc(single(second = 0x3F)))
        // Day of week: bits 45..51
        assertContentEquals(bytes(0, 0, 0, 0, 0, 0x20, 0, 0, 0, 0), enc(single(dow = 1)))
        assertContentEquals(bytes(0, 0, 0, 0, 0, 0xE0, 0x0F, 0, 0, 0), enc(single(dow = 0x7F)))
        // Action: bits 52..55
        assertContentEquals(
            bytes(0, 0, 0, 0, 0, 0, 0x10, 0, 0, 0),
            enc(single(action = SchedulerAction.TURN_ON))
        )
        assertContentEquals(
            bytes(0, 0, 0, 0, 0, 0, 0xF0, 0, 0, 0),
            enc(single(action = SchedulerAction.NO_ACTION))
        )
        // Transition time and scene number (little endian)
        val e = single().copy(
            transitionTime = TransitionTime(rawValue = 0x45u),
            sceneNumber = 0x1234u
        )
        assertContentEquals(bytes(0, 0, 0, 0, 0, 0, 0, 0x45, 0x34, 0x12), enc(e))
    }

    @Test
    fun testIndexOutOfRange() {
        assertFailsWith<IllegalArgumentException> { SchedulerActionSet(0x10u, maxEntry) }
        assertFailsWith<IllegalArgumentException> { SchedulerActionStatus(0x10u, maxEntry) }
        assertFailsWith<IllegalArgumentException> {
            SchedulerActionSetUnacknowledged(0x10u, maxEntry)
        }
    }

    @Test
    fun testRealisticEntryRoundTrip() {
        // 18:30:00 in 2025 in January and December on Mondays and Sundays: recall scene 7.
        val entry = SchedulerRegistryEntry(
            year = SchedulerYear.specific(25),
            month = SchedulerMonth.any(listOf(Month.JANUARY, Month.DECEMBER)),
            day = SchedulerDay.any(),
            hour = SchedulerHour.specific(18),
            minute = SchedulerMinute.specific(30),
            second = SchedulerSecond.specific(0),
            dayOfWeek = SchedulerDayOfWeek.any(listOf(WeekDay.MONDAY, WeekDay.SUNDAY)),
            action = SchedulerAction.SCENE_RECALL,
            transitionTime = TransitionTime(rawValue = 0x45u),
            sceneNumber = 0x0007u
        )
        val set = SchedulerActionSet(index = 3u, entry = entry)
        val decoded = SchedulerActionSet.init(set.parameters) as SchedulerActionSet
        assertEquals(3u.toUByte(), decoded.index)
        assertEquals(entry, decoded.entry)
        assertEquals(setOf(Month.JANUARY, Month.DECEMBER), decoded.entry.month.months)
        assertEquals(setOf(WeekDay.MONDAY, WeekDay.SUNDAY), decoded.entry.dayOfWeek.days)
    }

    @Test
    fun testStatusAndUnackedShareEncoding() {
        val set = SchedulerActionSet(5u, maxEntry)
        val unack = SchedulerActionSetUnacknowledged(5u, maxEntry)
        val status = SchedulerActionStatus(5u, maxEntry)
        assertContentEquals(set.parameters, unack.parameters)
        assertContentEquals(set.parameters, status.parameters)

        val u = SchedulerActionSetUnacknowledged.init(unack.parameters)
                as SchedulerActionSetUnacknowledged
        assertEquals(5u.toUByte(), u.index)
        assertEquals(maxEntry, u.entry)
        val s = SchedulerActionStatus.init(status.parameters) as SchedulerActionStatus
        assertEquals(5u.toUByte(), s.index)
        assertEquals(maxEntry, s.entry)
    }

    @Test
    fun testDecodingInvalid() {
        val initializers = listOf<(ByteArray?) -> Any?>(
            SchedulerActionSet::init,
            SchedulerActionSetUnacknowledged::init,
            SchedulerActionStatus::init
        )
        for (init in initializers) {
            assertNull(init(null))
            assertNull(init(ByteArray(9)))
            assertNull(init(ByteArray(11)))
            assertNull(init(ByteArray(0)))
        }
        // Reserved action value 0x03 (bits 52..55).
        assertNull(SchedulerActionStatus.init(bytes(0, 0, 0, 0, 0, 0, 0x30, 0, 0, 0)))
        assertNotNull(SchedulerActionStatus.init(bytes(0, 0, 0, 0, 0, 0, 0x20, 0, 0, 0)))
    }

    @Test
    fun testFieldHelpers() {
        assertEquals(0x64u.toUByte(), SchedulerYear.any().value)
        assertEquals(0x18u.toUByte(), SchedulerHour.any().value)
        assertEquals(0x19u.toUByte(), SchedulerHour.random().value)
        assertEquals(0x3Cu.toUByte(), SchedulerMinute.any().value)
        assertEquals(0x3Du.toUByte(), SchedulerMinute.every15().value)
        assertEquals(0x3Eu.toUByte(), SchedulerSecond.every20().value)
        assertEquals(0x3Fu.toUByte(), SchedulerSecond.random().value)
        assertEquals(0x0FFFu.toUShort(), SchedulerMonth.every().value)
        assertEquals(0x7Fu.toUByte(), SchedulerDayOfWeek.every().value)
        assertFailsWith<IllegalArgumentException> { SchedulerHour.specific(24) }
        assertFailsWith<IllegalArgumentException> { SchedulerDay.specific(0) }
        assertFailsWith<IllegalArgumentException> { SchedulerMinute(0x40u) }
        assertFailsWith<IllegalArgumentException> { SchedulerMonth(0x1000u) }
        assertFailsWith<IllegalArgumentException> { SchedulerDayOfWeek(0x80u) }
    }
}
