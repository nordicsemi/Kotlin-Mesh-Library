package no.nordicsemi.kotlin.mesh.core.messages.generic

import no.nordicsemi.kotlin.mesh.core.messages.ApplicationResponse
import no.nordicsemi.kotlin.mesh.core.messages.GenericMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedApplicationMessage

/** Presence of the battery. */
enum class BatteryPresence(val value: Int) {
    NOT_PRESENT(0b00),
    REMOVABLE(0b01),
    NOT_REMOVABLE(0b10),
    UNKNOWN(0b11);

    companion object {
        /** Returns the [BatteryPresence] for the lowest 2 bits of the given value. */
        fun from(value: Int) = entries.first { it.value == value and 0x03 }
    }
}

/** Charge level indicator of the battery. */
enum class BatteryIndicator(val value: Int) {
    CRITICALLY_LOW(0b00),
    LOW(0b01),
    GOOD(0b10),
    UNKNOWN(0b11);

    companion object {
        /** Returns the [BatteryIndicator] for the lowest 2 bits of the given value. */
        fun from(value: Int) = entries.first { it.value == value and 0x03 }
    }
}

/** Charging state of the battery. */
enum class BatteryChargingState(val value: Int) {
    NOT_CHARGEABLE(0b00),
    NOT_CHARGING(0b01),
    CHARGING(0b10),
    UNKNOWN(0b11);

    companion object {
        /** Returns the [BatteryChargingState] for the lowest 2 bits of the given value. */
        fun from(value: Int) = entries.first { it.value == value and 0x03 }
    }
}

/** Serviceability of the battery. */
enum class BatteryServiceability(val value: Int) {
    RESERVED(0b00),
    SERVICE_NOT_REQUIRED(0b01),
    SERVICE_REQUIRED(0b10),
    UNKNOWN(0b11);

    companion object {
        /** Returns the [BatteryServiceability] for the lowest 2 bits of the given value. */
        fun from(value: Int) = entries.first { it.value == value and 0x03 }
    }
}

/**
 * This message is a response to a [GenericBatteryGet] that contains the current status of a
 * GenericBatteryServer.
 *
 * @property batteryLevel    Battery level in percent (0-100). 0xFF means unknown.
 * @property timeToDischarge Remaining time to discharge in minutes (24-bit). 0xFFFFFF means
 *                           unknown.
 * @property timeToCharge    Remaining time to charge in minutes (24-bit). 0xFFFFFF means unknown.
 * @property flags           Raw flags: presence (bits 0-1), indicator (bits 2-3), charging
 *                           (bits 4-5) and serviceability (bits 6-7).
 */
class GenericBatteryStatus(
    val batteryLevel: UByte,
    val timeToDischarge: UInt,
    val timeToCharge: UInt,
    val flags: UByte,
) : ApplicationResponse, UnacknowledgedApplicationMessage {
    override val opCode = Initializer.opCode
    override val parameters: ByteArray
        get() = byteArrayOf(batteryLevel.toByte()) +
                timeToDischarge.to24Bit() +
                timeToCharge.to24Bit() +
                byteArrayOf(flags.toByte())

    /** Whether the battery level is known. */
    val isBatteryLevelKnown: Boolean get() = batteryLevel != UNKNOWN_LEVEL

    /** Whether the time to discharge is known. */
    val isTimeToDischargeKnown: Boolean get() = timeToDischarge != UNKNOWN_TIME

    /** Whether the time to charge is known. */
    val isTimeToChargeKnown: Boolean get() = timeToCharge != UNKNOWN_TIME

    /** Presence of the battery. */
    val batteryPresence: BatteryPresence get() = BatteryPresence.from(flags.toInt())

    /** Charge level indicator of the battery. */
    val batteryIndicator: BatteryIndicator get() = BatteryIndicator.from(flags.toInt() shr 2)

    /** Charging state of the battery. */
    val batteryChargingState: BatteryChargingState
        get() = BatteryChargingState.from(flags.toInt() shr 4)

    /** Serviceability of the battery. */
    val batteryServiceability: BatteryServiceability
        get() = BatteryServiceability.from(flags.toInt() shr 6)

    /**
     * Convenience constructor that builds the flags from their components. The level is limited to
     * 100 (unless 0xFF) and the times to 0xFFFFFE (unless 0xFFFFFF).
     */
    @Suppress("unused")
    constructor(
        batteryLevel: UByte,
        timeToDischarge: UInt,
        timeToCharge: UInt,
        presence: BatteryPresence,
        indicator: BatteryIndicator,
        chargingState: BatteryChargingState,
        serviceability: BatteryServiceability,
    ) : this(
        batteryLevel = if (batteryLevel != UNKNOWN_LEVEL) minOf(batteryLevel, 100.toUByte())
        else batteryLevel,
        timeToDischarge = if (timeToDischarge != UNKNOWN_TIME) minOf(timeToDischarge, 0xFFFFFEu)
        else timeToDischarge,
        timeToCharge = if (timeToCharge != UNKNOWN_TIME) minOf(timeToCharge, 0xFFFFFEu)
        else timeToCharge,
        flags = ((serviceability.value shl 6) or
                (chargingState.value shl 4) or
                (indicator.value shl 2) or
                presence.value).toUByte()
    )

    override fun toString() = "GenericBatteryStatus(batteryLevel: " +
            (if (isBatteryLevelKnown) "$batteryLevel%" else "unknown") +
            ", timeToDischarge: " +
            (if (isTimeToDischargeKnown) "$timeToDischarge min" else "unknown") +
            ", timeToCharge: " +
            (if (isTimeToChargeKnown) "$timeToCharge min" else "unknown") +
            ", presence: $batteryPresence, indicator: $batteryIndicator" +
            ", chargingState: $batteryChargingState, serviceability: $batteryServiceability)"

    companion object Initializer : GenericMessageInitializer {
        override val opCode = 0x8224u

        /** Battery level value meaning unknown. */
        val UNKNOWN_LEVEL: UByte = 0xFFu

        /** Time value (24-bit) meaning unknown. */
        const val UNKNOWN_TIME = 0xFFFFFFu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 8 }
            ?.let { params ->
                GenericBatteryStatus(
                    batteryLevel = params[0].toUByte(),
                    timeToDischarge = params.get24Bit(offset = 1),
                    timeToCharge = params.get24Bit(offset = 4),
                    flags = params[7].toUByte()
                )
            }

        private fun ByteArray.get24Bit(offset: Int) =
            (this[offset].toUInt() and 0xFFu) or
                    ((this[offset + 1].toUInt() and 0xFFu) shl 8) or
                    ((this[offset + 2].toUInt() and 0xFFu) shl 16)
    }
}

private fun UInt.to24Bit() = byteArrayOf(
    (this and 0xFFu).toByte(),
    ((this shr 8) and 0xFFu).toByte(),
    ((this shr 16) and 0xFFu).toByte()
)
