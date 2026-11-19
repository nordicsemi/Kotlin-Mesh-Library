package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.mesh.core.model.SceneNumber
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime

/**
 * Base interface for the initializers of the Scheduler messages.
 */
interface SchedulerMessageInitializer : BaseMeshMessageInitializer, HasOpCode

/**
 * Months of the year, as bits of the Scheduler Month field.
 *
 * @property mask Bit mask of the month.
 */
enum class Month(val mask: UShort) {
    JANUARY(0x0001u),
    FEBRUARY(0x0002u),
    MARCH(0x0004u),
    APRIL(0x0008u),
    MAY(0x0010u),
    JUNE(0x0020u),
    JULY(0x0040u),
    AUGUST(0x0080u),
    SEPTEMBER(0x0100u),
    OCTOBER(0x0200u),
    NOVEMBER(0x0400u),
    DECEMBER(0x0800u)
}

/**
 * Days of the week, as bits of the Scheduler Day of Week field.
 *
 * @property mask Bit mask of the day.
 */
enum class WeekDay(val mask: UByte) {
    MONDAY(0x01u),
    TUESDAY(0x02u),
    WEDNESDAY(0x04u),
    THURSDAY(0x08u),
    FRIDAY(0x10u),
    SATURDAY(0x20u),
    SUNDAY(0x40u)
}

/**
 * Action to be executed by a Scheduler at the scheduled time.
 *
 * @property value Raw 4-bit value of the action.
 */
enum class SchedulerAction(val value: UByte) {
    TURN_OFF(0x00u),
    TURN_ON(0x01u),
    SCENE_RECALL(0x02u),
    NO_ACTION(0x0Fu);

    companion object {
        /** Returns the action for the given raw value or null if it is reserved for future use. */
        fun from(value: UByte) = entries.firstOrNull { it.value == value }
    }
}

/**
 * Year of a scheduled event, 7 bits. 0x00-0x63 is the year 2000-2099 (last two digits), 0x64 is any
 * year.
 */
@JvmInline
value class SchedulerYear(val value: UByte) {
    init {
        require(value <= 0x7Fu) { "Year must fit in 7 bits!" }
    }

    companion object {
        /** Any year. */
        fun any() = SchedulerYear(0x64u)

        /** A specific year, 0-99 (2000-2099). */
        fun specific(year: Int): SchedulerYear {
            require(year in 0..99) { "Year must be in range 0..99!" }
            return SchedulerYear(year.toUByte())
        }
    }
}

/**
 * Months of a scheduled event, a 12-bit bitmap where each bit represents a month.
 */
@JvmInline
value class SchedulerMonth(val value: UShort) {
    init {
        require(value <= 0x0FFFu) { "Month must fit in 12 bits!" }
    }

    /** Set of selected months. */
    val months: Set<Month>
        get() = Month.entries.filter { value and it.mask != 0.toUShort() }.toSet()

    companion object {
        /** Any of the given months. An empty list selects no month. */
        fun any(of: Collection<Month>) = SchedulerMonth(
            of.fold(0) { acc, month -> acc or month.mask.toInt() }.toUShort()
        )

        /** Every month. */
        fun every() = any(Month.entries)
    }
}

/**
 * Day of the month of a scheduled event, 5 bits. 0x00 is any day, 0x01-0x1F is a specific day.
 */
@JvmInline
value class SchedulerDay(val value: UByte) {
    init {
        require(value <= 0x1Fu) { "Day must fit in 5 bits!" }
    }

    companion object {
        /** Any day. */
        fun any() = SchedulerDay(0x00u)

        /** A specific day of month, 1-31. */
        fun specific(day: Int): SchedulerDay {
            require(day in 1..31) { "Day must be in range 1..31!" }
            return SchedulerDay(day.toUByte())
        }
    }
}

/**
 * Hour of a scheduled event, 5 bits. 0x00-0x17 is a specific hour, 0x18 is any hour and 0x19 is a
 * random hour.
 */
@JvmInline
value class SchedulerHour(val value: UByte) {
    init {
        require(value <= 0x1Fu) { "Hour must fit in 5 bits!" }
    }

    companion object {
        /** Any hour. */
        fun any() = SchedulerHour(0x18u)

        /** Random hour. */
        fun random() = SchedulerHour(0x19u)

        /** A specific hour, 0-23. */
        fun specific(hour: Int): SchedulerHour {
            require(hour in 0..23) { "Hour must be in range 0..23!" }
            return SchedulerHour(hour.toUByte())
        }
    }
}

/**
 * Minute of a scheduled event, 6 bits. 0x00-0x3B is a specific minute, 0x3C is any minute, 0x3D is
 * every 15 minutes, 0x3E is every 20 minutes and 0x3F is a random minute.
 */
@JvmInline
value class SchedulerMinute(val value: UByte) {
    init {
        require(value <= 0x3Fu) { "Minute must fit in 6 bits!" }
    }

    companion object {
        /** Any minute. */
        fun any() = SchedulerMinute(0x3Cu)

        /** Every 15 minutes. */
        fun every15() = SchedulerMinute(0x3Du)

        /** Every 20 minutes. */
        fun every20() = SchedulerMinute(0x3Eu)

        /** Random minute. */
        fun random() = SchedulerMinute(0x3Fu)

        /** A specific minute, 0-59. */
        fun specific(minute: Int): SchedulerMinute {
            require(minute in 0..59) { "Minute must be in range 0..59!" }
            return SchedulerMinute(minute.toUByte())
        }
    }
}

/**
 * Second of a scheduled event, 6 bits. 0x00-0x3B is a specific second, 0x3C is any second, 0x3D is
 * every 15 seconds, 0x3E is every 20 seconds and 0x3F is a random second.
 */
@JvmInline
value class SchedulerSecond(val value: UByte) {
    init {
        require(value <= 0x3Fu) { "Second must fit in 6 bits!" }
    }

    companion object {
        /** Any second. */
        fun any() = SchedulerSecond(0x3Cu)

        /** Every 15 seconds. */
        fun every15() = SchedulerSecond(0x3Du)

        /** Every 20 seconds. */
        fun every20() = SchedulerSecond(0x3Eu)

        /** Random second. */
        fun random() = SchedulerSecond(0x3Fu)

        /** A specific second, 0-59. */
        fun specific(second: Int): SchedulerSecond {
            require(second in 0..59) { "Second must be in range 0..59!" }
            return SchedulerSecond(second.toUByte())
        }
    }
}

/**
 * Days of the week of a scheduled event, a 7-bit bitmap where each bit represents a day.
 */
@JvmInline
value class SchedulerDayOfWeek(val value: UByte) {
    init {
        require(value <= 0x7Fu) { "Day of week must fit in 7 bits!" }
    }

    /** Set of selected days of the week. */
    val days: Set<WeekDay>
        get() = WeekDay.entries.filter { value and it.mask != 0.toUByte() }.toSet()

    companion object {
        /** Any of the given days. An empty list selects no day. */
        fun any(of: Collection<WeekDay>) = SchedulerDayOfWeek(
            of.fold(0) { acc, day -> acc or day.mask.toInt() }.toUByte()
        )

        /** Every day of the week. */
        fun every() = any(WeekDay.entries)
    }
}

/**
 * An entry of the Schedule Register, defining when and what action is to be performed.
 *
 * @property year            Year of the event.
 * @property month           Months of the event.
 * @property day             Day of the month of the event.
 * @property hour            Hour of the event.
 * @property minute          Minute of the event.
 * @property second          Second of the event.
 * @property dayOfWeek       Days of the week of the event.
 * @property action          Action to be performed.
 * @property transitionTime  Transition time for the action.
 * @property sceneNumber     Scene to be recalled when the action is [SchedulerAction.SCENE_RECALL].
 */
data class SchedulerRegistryEntry(
    val year: SchedulerYear = SchedulerYear.any(),
    val month: SchedulerMonth = SchedulerMonth.any(of = emptyList()),
    val day: SchedulerDay = SchedulerDay.any(),
    val hour: SchedulerHour = SchedulerHour.any(),
    val minute: SchedulerMinute = SchedulerMinute.any(),
    val second: SchedulerSecond = SchedulerSecond.any(),
    val dayOfWeek: SchedulerDayOfWeek = SchedulerDayOfWeek.any(of = emptyList()),
    val action: SchedulerAction = SchedulerAction.NO_ACTION,
    val transitionTime: TransitionTime = TransitionTime(),
    val sceneNumber: SceneNumber = 0u,
) {

    /**
     * Encodes the entry together with its index into the 10 byte Schedule Register structure
     * used by the Scheduler Action Set and Status messages.
     *
     * Layout (LSB first): Index 4, Year 7, Month 12, Day 5, Hour 5, Minute 6, Second 6,
     * Day of Week 7, Action 4 (56 bits), Transition Time 8, Scene Number 16.
     */
    fun toByteArray(index: UByte): ByteArray {
        require(index <= MAX_INDEX) { "Index must be in range 0..15!" }
        var bits = 0L
        var offset = 0
        fun put(value: Int, width: Int) {
            bits = bits or ((value.toLong() and ((1L shl width) - 1)) shl offset)
            offset += width
        }
        put(index.toInt(), 4)
        put(year.value.toInt(), 7)
        put(month.value.toInt(), 12)
        put(day.value.toInt(), 5)
        put(hour.value.toInt(), 5)
        put(minute.value.toInt(), 6)
        put(second.value.toInt(), 6)
        put(dayOfWeek.value.toInt(), 7)
        put(action.value.toInt(), 4)
        val data = ByteArray(10)
        for (i in 0 until 7) data[i] = (bits shr (8 * i)).toByte()
        data[7] = transitionTime.rawValue.toByte()
        data[8] = sceneNumber.toByte()
        data[9] = (sceneNumber.toInt() shr 8).toByte()
        return data
    }

    companion object {
        /** Maximum index of the Schedule Register. */
        val MAX_INDEX: UByte = 0x0Fu

        /**
         * Decodes the 10 byte Schedule Register structure into the index and the entry. Returns
         * null if the length is wrong or the action is reserved for future use.
         */
        fun from(data: ByteArray): Pair<UByte, SchedulerRegistryEntry>? {
            if (data.size != 10) return null
            var bits = 0L
            for (i in 0 until 7) bits = bits or ((data[i].toLong() and 0xFF) shl (8 * i))
            var offset = 0
            fun get(width: Int): Int {
                val value = ((bits shr offset) and ((1L shl width) - 1)).toInt()
                offset += width
                return value
            }

            val index = get(4).toUByte()
            val year = SchedulerYear(get(7).toUByte())
            val month = SchedulerMonth(get(12).toUShort())
            val day = SchedulerDay(get(5).toUByte())
            val hour = SchedulerHour(get(5).toUByte())
            val minute = SchedulerMinute(get(6).toUByte())
            val second = SchedulerSecond(get(6).toUByte())
            val dayOfWeek = SchedulerDayOfWeek(get(7).toUByte())
            val action = SchedulerAction.from(get(4).toUByte()) ?: return null
            val scene = ((data[8].toInt() and 0xFF) or
                    ((data[9].toInt() and 0xFF) shl 8)).toUShort()
            return index to SchedulerRegistryEntry(
                year = year,
                month = month,
                day = day,
                hour = hour,
                minute = minute,
                second = second,
                dayOfWeek = dayOfWeek,
                action = action,
                transitionTime = TransitionTime(rawValue = data[7].toUByte()),
                sceneNumber = scene
            )
        }
    }
}
