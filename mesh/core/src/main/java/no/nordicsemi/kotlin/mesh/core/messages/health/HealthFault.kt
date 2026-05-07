@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.health

/**
 * Health Fault IDs assigned to the health models.
 *
 * @property code The fault code.
 */
sealed class HealthFault(open val code: UByte) {
    object NoFault : HealthFault(0x00u)
    object BatteryLowWarning : HealthFault(0x01u)
    object BatteryLowError : HealthFault(0x02u)
    object SupplyVoltageToLowWarning : HealthFault(0x03u)
    object SupplyVoltageToLowError : HealthFault(0x04u)
    object SupplyVoltageToHighWarning : HealthFault(0x05u)
    object SupplyVoltageToHighError : HealthFault(0x06u)
    object PowerSupplyInterruptedWarning : HealthFault(0x07u)
    object PowerSupplyInterruptedError : HealthFault(0x08u)
    object NoLoadWarning : HealthFault(0x09u)
    object NoLoadError : HealthFault(0x0Au)
    object OverloadWarning : HealthFault(0x0Bu)
    object OverloadError : HealthFault(0x0Cu)
    object OverheatWarning : HealthFault(0x0Du)
    object OverheatError : HealthFault(0x0Eu)
    object CondensationWarning : HealthFault(0x0Fu)
    object CondensationError : HealthFault(0x10u)
    object VibrationWarning : HealthFault(0x11u)
    object VibrationError : HealthFault(0x12u)
    object ConfigurationWarning : HealthFault(0x13u)
    object ConfigurationError : HealthFault(0x14u)
    object ElementNotCalibratedWarning : HealthFault(0x15u)
    object ElementNotCalibratedError : HealthFault(0x16u)
    object MemoryWarning : HealthFault(0x17u)
    object MemoryError : HealthFault(0x18u)
    object SelfTestWarning : HealthFault(0x19u)
    object SelfTestError : HealthFault(0x1Au)
    object InputTooLowWarning : HealthFault(0x1Bu)
    object InputTooLowError : HealthFault(0x1Cu)
    object InputTooHighWarning : HealthFault(0x1Du)
    object InputTooHighError : HealthFault(0x1Eu)
    object InputNoChangeWarning : HealthFault(0x1Fu)
    object InputNoChangeError : HealthFault(0x20u)
    object ActuatorBlockedWarning : HealthFault(0x21u)
    object ActuatorBlockedError : HealthFault(0x22u)
    object HousingOpenedWarning : HealthFault(0x23u)
    object HousingOpenedError : HealthFault(0x24u)
    object TamperWarning : HealthFault(0x25u)
    object TamperError : HealthFault(0x26u)
    object DeviceMovedWarning : HealthFault(0x27u)
    object DeviceMovedError : HealthFault(0x28u)
    object DeviceDroppedWarning : HealthFault(0x29u)
    object DeviceDroppedError : HealthFault(0x2Au)
    object OverflowWarning : HealthFault(0x2Bu)
    object OverflowError : HealthFault(0x2Cu)
    object EmptyWarning : HealthFault(0x2Du)
    object EmptyError : HealthFault(0x2Eu)
    object InternalBusWarning : HealthFault(0x2Fu)
    object InternalBUError : HealthFault(0x30u)
    object MechanismJammedWarning : HealthFault(0x31u)
    object MechanismJammedError : HealthFault(0x32u)
    data class Vendor(override val code: UByte) : HealthFault(code)
    data class Reserved(override val code: UByte) : HealthFault(code)


    companion object {
        /**
         * Returns the [HealthFault] for the given code.
         *
         * @param code The fault code.
         * @return The [HealthFault] or null if the code is unknown.
         */
        fun from(code: UByte): HealthFault = when (code) {
            0x00u.toUByte() -> NoFault
            0x01u.toUByte() -> BatteryLowWarning
            0x02u.toUByte() -> BatteryLowError
            0x03u.toUByte() -> SupplyVoltageToLowWarning
            0x04u.toUByte() -> SupplyVoltageToLowError
            0x05u.toUByte() -> SupplyVoltageToHighWarning
            0x06u.toUByte() -> SupplyVoltageToHighError
            0x07u.toUByte() -> PowerSupplyInterruptedWarning
            0x08u.toUByte() -> PowerSupplyInterruptedError
            0x09u.toUByte() -> NoLoadWarning
            0x0Au.toUByte() -> NoLoadError
            0x0Bu.toUByte() -> OverloadWarning
            0x0Cu.toUByte() -> OverloadError
            0x0Du.toUByte() -> OverheatWarning
            0x0Eu.toUByte() -> OverheatError
            0x0Fu.toUByte() -> CondensationWarning
            0x10u.toUByte() -> CondensationError
            0x11u.toUByte() -> VibrationWarning
            0x12u.toUByte() -> VibrationError
            0x13u.toUByte() -> ConfigurationWarning
            0x14u.toUByte() -> ConfigurationError
            0x15u.toUByte() -> ElementNotCalibratedWarning
            0x16u.toUByte() -> ElementNotCalibratedError
            0x17u.toUByte() -> MemoryWarning
            0x18u.toUByte() -> MemoryError
            0x19u.toUByte() -> SelfTestWarning
            0x1Au.toUByte() -> SelfTestError
            0x1Bu.toUByte() -> InputTooLowWarning
            0x1Cu.toUByte() -> InputTooLowError
            0x1Du.toUByte() -> InputTooHighWarning
            0x1Eu.toUByte() -> InputTooHighError
            0x1Fu.toUByte() -> InputNoChangeWarning
            0x20u.toUByte() -> InputNoChangeError
            0x21u.toUByte() -> ActuatorBlockedWarning
            0x22u.toUByte() -> ActuatorBlockedError
            0x23u.toUByte() -> HousingOpenedWarning
            0x24u.toUByte() -> HousingOpenedError
            0x25u.toUByte() -> TamperWarning
            0x26u.toUByte() -> TamperError
            0x27u.toUByte() -> DeviceMovedWarning
            0x28u.toUByte() -> DeviceMovedError
            0x29u.toUByte() -> DeviceDroppedWarning
            0x2Au.toUByte() -> DeviceDroppedError
            0x2Bu.toUByte() -> OverflowWarning
            0x2Cu.toUByte() -> OverflowError
            0x2Du.toUByte() -> EmptyWarning
            0x2Eu.toUByte() -> EmptyError
            0x2Fu.toUByte() -> InternalBusWarning
            0x30u.toUByte() -> InternalBUError
            0x31u.toUByte() -> MechanismJammedWarning
            0x32u.toUByte() -> MechanismJammedError
            in 0x80u.toUByte()..0xFFu.toUByte() -> Vendor(code)
            else -> Reserved(code) // Reserved to allow values between 0x32 - 0x79
        }
    }
}
