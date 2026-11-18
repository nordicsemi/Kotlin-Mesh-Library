package no.nordicsemi.kotlin.mesh.core.messages

/**
 * Base interface for the initializers of the Time messages.
 */
interface TimeMessageInitializer : BaseMeshMessageInitializer, HasOpCode

/**
 * Time zone offset as defined by the Bluetooth Mesh Model specification. The offset is encoded in
 * 15 minute steps, where 0x00 is UTC-16:00, 0x40 is UTC+00:00 and 0xFF is UTC+47:45.
 *
 * @property rawValue Raw encoded value.
 */
@JvmInline
value class TimeZoneOffset(val rawValue: UByte) {

    /** Offset from UTC in minutes. */
    val minutes: Int
        get() = (rawValue.toInt() - ZERO_OFFSET) * MINUTES_PER_STEP

    override fun toString() = "TimeZoneOffset($minutes min)"

    companion object {
        private const val ZERO_OFFSET = 0x40
        private const val MINUTES_PER_STEP = 15

        /** UTC+00:00. */
        val UTC = TimeZoneOffset(rawValue = 0x40u)

        /**
         * Creates a [TimeZoneOffset] from an offset in minutes. The offset is rounded towards zero
         * to a multiple of 15 minutes and clamped to the supported range.
         */
        fun ofMinutes(minutes: Int) = TimeZoneOffset(
            rawValue = (minutes / MINUTES_PER_STEP + ZERO_OFFSET).coerceIn(0, 255).toUByte()
        )
    }
}

/**
 * Time in International Atomic Time (TAI) with the additional fields used by the Time messages.
 *
 * @property seconds         Current TAI time in seconds since the epoch 2000-01-01 00:00:00 TAI
 *                           (40 bits). 0 means the time is unknown.
 * @property subSecond       Sub-second time in units of 1/256 second.
 * @property uncertainty     Estimated uncertainty in 10 millisecond steps.
 * @property timeAuthority   True if the node has a reliable source of time (e.g. GPS).
 * @property taiUtcDelta     Difference between TAI and UTC in seconds, in range -255..32512.
 * @property timeZoneOffset  Local time zone offset.
 */
data class TaiTime(
    val seconds: ULong = 0uL,
    val subSecond: UByte = 0u,
    val uncertainty: UByte = 0u,
    val timeAuthority: Boolean = false,
    val taiUtcDelta: Int = 0,
    val timeZoneOffset: TimeZoneOffset = TimeZoneOffset.UTC,
) {
    init {
        require(seconds <= MAX_SECONDS) { "TAI seconds must fit in 40 bits!" }
        require(taiUtcDelta in MIN_DELTA..MAX_DELTA) {
            "TAI-UTC Delta must be in range $MIN_DELTA..$MAX_DELTA!"
        }
    }

    /** True if the time is known, i.e. the TAI seconds is not 0. */
    val isKnown: Boolean
        get() = seconds > 0uL

    /**
     * Encodes the time into 10 bytes.
     */
    fun toByteArray(): ByteArray {
        val data = ByteArray(10)
        writeSeconds(seconds).copyInto(data)
        data[5] = subSecond.toByte()
        data[6] = uncertainty.toByte()
        val delta = ((taiUtcDelta + DELTA_OFFSET) shl 1) or (if (timeAuthority) 1 else 0)
        data[7] = delta.toByte()
        data[8] = (delta shr 8).toByte()
        data[9] = timeZoneOffset.rawValue.toByte()
        return data
    }

    companion object {
        const val MIN_DELTA = -255
        const val MAX_DELTA = 32512
        private const val DELTA_OFFSET = 255
        internal const val MAX_SECONDS = 0xFF_FFFF_FFFFuL

        /**
         * Decodes a 40 bit TAI seconds in little endian.
         */
        internal fun readSeconds(data: ByteArray, offset: Int): ULong {
            var value = 0uL
            for (i in 0 until 5) {
                value = value or ((data[offset + i].toULong() and 0xFFuL) shl (8 * i))
            }
            return value
        }

        /**
         * Encodes a 40 bit TAI seconds in little endian.
         */
        internal fun writeSeconds(seconds: ULong) = ByteArray(5) { (seconds shr (8 * it)).toByte() }

        /**
         * Decodes a TAI time from 10 bytes or returns null if the length is wrong.
         */
        fun from(data: ByteArray): TaiTime? {
            if (data.size != 10) return null
            val raw = (data[7].toInt() and 0xFF) or ((data[8].toInt() and 0xFF) shl 8)
            return TaiTime(
                seconds = readSeconds(data, 0),
                subSecond = data[5].toUByte(),
                uncertainty = data[6].toUByte(),
                timeAuthority = raw and 1 == 1,
                taiUtcDelta = (raw shr 1) - DELTA_OFFSET,
                timeZoneOffset = TimeZoneOffset(data[9].toUByte())
            )
        }
    }
}
