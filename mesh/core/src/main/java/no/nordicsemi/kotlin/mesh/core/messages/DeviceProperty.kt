@file:Suppress("unused")
@file:OptIn(ExperimentalTime::class)

package no.nordicsemi.kotlin.mesh.core.messages

import no.nordicsemi.kotlin.data.FloatFormat
import no.nordicsemi.kotlin.data.IntFormat
import no.nordicsemi.kotlin.data.getFloat
import no.nordicsemi.kotlin.data.getShort
import no.nordicsemi.kotlin.data.getUInt
import no.nordicsemi.kotlin.data.getUShort
import java.math.BigDecimal
import java.nio.ByteOrder
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Device Properties specified in
 * [Mesh Device Properties](https://www.bluetooth.com/specifications/specs/device-properties/).
 *
 * Each property has a corresponding [DevicePropertyCharacteristic].
 *
 * Not all properties have their corresponding characteristics implemented in this library. For
 * those [valueLength] is `null` and [DevicePropertyCharacteristic.Other] is returned when the
 * value is read.
 *
 * @property id          The 16-bit Property ID assigned by Bluetooth SIG.
 * @property valueLength Length of the characteristic value in octets, or `null` when the
 *                       characteristic is not supported by this library.
 */
enum class DeviceProperty(val id: UShort, val valueLength: Int?, val propertyName: String) {
    AVERAGE_AMBIENT_TEMPERATURE_IN_A_PERIOD_OF_DAY(
        id = 0x0001u,
        valueLength = null,
        propertyName = "Average Ambient Temperature In A Period Of Day",
    ),
    AVERAGE_INPUT_CURRENT(id = 0x0002u, valueLength = 3, propertyName = "Average Input Current"),
    AVERAGE_INPUT_VOLTAGE(id = 0x0003u, valueLength = 3, propertyName = "Average Input Voltage"),
    AVERAGE_OUTPUT_CURRENT(id = 0x0004u, valueLength = 3, propertyName = "Average Output Current"),
    AVERAGE_OUTPUT_VOLTAGE(id = 0x0005u, valueLength = 3, propertyName = "Average Output Voltage"),
    CENTER_BEAM_INTENSITY_AT_FULL_POWER(
        id = 0x0006u,
        valueLength = null,
        propertyName = "Center Beam Intensity At Full Power",
    ),
    CHROMATICITY_TOLERANCE(
        id = 0x0007u,
        valueLength = null,
        propertyName = "Chromaticity Tolerance",
    ),
    COLOR_RENDERING_INDEX_R9(
        id = 0x0008u,
        valueLength = null,
        propertyName = "Color Rendering Index R9",
    ),
    COLOR_RENDERING_INDEX_RA(
        id = 0x0009u,
        valueLength = null,
        propertyName = "Color Rendering Index Ra",
    ),
    DEVICE_APPEARANCE(id = 0x000Au, valueLength = null, propertyName = "Device Appearance"),
    DEVICE_COUNTRY_OF_ORIGIN(
        id = 0x000Bu,
        valueLength = null,
        propertyName = "Device Country Of Origin",
    ),
    DEVICE_DATE_OF_MANUFACTURE(
        id = 0x000Cu,
        valueLength = 3,
        propertyName = "Device Date Of Manufacture",
    ),
    DEVICE_ENERGY_USE_SINCE_TURN_ON(
        id = 0x000Du,
        valueLength = 3,
        propertyName = "Device Energy Use Since Turn On",
    ),
    DEVICE_FIRMWARE_REVISION(
        id = 0x000Eu,
        valueLength = 8,
        propertyName = "Device Firmware Revision",
    ),
    DEVICE_GLOBAL_TRADE_ITEM_NUMBER(
        id = 0x000Fu,
        valueLength = null,
        propertyName = "Device Global Trade Item Number",
    ),
    DEVICE_HARDWARE_REVISION(
        id = 0x0010u,
        valueLength = 16,
        propertyName = "Device Hardware Revision",
    ),
    DEVICE_MANUFACTURER_NAME(
        id = 0x0011u,
        valueLength = 36,
        propertyName = "Device Manufacturer Name",
    ),
    DEVICE_MODEL_NUMBER(id = 0x0012u, valueLength = 24, propertyName = "Device Model Number"),
    DEVICE_OPERATING_TEMPERATURE_RANGE_SPECIFICATION(
        id = 0x0013u,
        valueLength = null,
        propertyName = "Device Operating Temperature Range Specification",
    ),
    DEVICE_OPERATING_TEMPERATURE_STATISTICAL_VALUES(
        id = 0x0014u,
        valueLength = null,
        propertyName = "Device Operating Temperature Statistical Values",
    ),
    DEVICE_OVER_TEMPERATURE_EVENT_STATISTICS(
        id = 0x0015u,
        valueLength = 6,
        propertyName = "Device Over Temperature Event Statistics",
    ),
    DEVICE_POWER_RANGE_SPECIFICATION(
        id = 0x0016u,
        valueLength = null,
        propertyName = "Device Power Range Specification",
    ),
    DEVICE_RUNTIME_SINCE_TURN_ON(
        id = 0x0017u,
        valueLength = 3,
        propertyName = "Device Runtime Since Turn On",
    ),
    DEVICE_RUNTIME_WARRANTY(
        id = 0x0018u,
        valueLength = 3,
        propertyName = "Device Runtime Warranty",
    ),
    DEVICE_SERIAL_NUMBER(id = 0x0019u, valueLength = 16, propertyName = "Device Serial Number"),
    DEVICE_SOFTWARE_REVISION(
        id = 0x001Au,
        valueLength = 8,
        propertyName = "Device Software Revision",
    ),
    DEVICE_UNDER_TEMPERATURE_EVENT_STATISTICS(
        id = 0x001Bu,
        valueLength = 6,
        propertyName = "Device Under Temperature Event Statistics",
    ),
    INDOOR_AMBIENT_TEMPERATURE_STATISTICAL_VALUES(
        id = 0x001Cu,
        valueLength = null,
        propertyName = "Indoor Ambient Temperature Statistical Values",
    ),
    INITIAL_CIE_1931_CHROMATICITY_COORDINATES(
        id = 0x001Du,
        valueLength = null,
        propertyName = "Initial CIE 1931 Chromaticity Coordinates",
    ),
    INITIAL_CORRELATED_COLOR_TEMPERATURE(
        id = 0x001Eu,
        valueLength = null,
        propertyName = "Initial Correlated Color Temperature",
    ),
    INITIAL_LUMINOUS_FLUX(id = 0x001Fu, valueLength = null, propertyName = "Initial Luminous Flux"),
    INITIAL_PLANCKIAN_DISTANCE(
        id = 0x0020u,
        valueLength = null,
        propertyName = "Initial Planckian Distance",
    ),
    INPUT_CURRENT_RANGE_SPECIFICATION(
        id = 0x0021u,
        valueLength = null,
        propertyName = "Input Current Range Specification",
    ),
    INPUT_CURRENT_STATISTICS(
        id = 0x0022u,
        valueLength = null,
        propertyName = "Input Current Statistics",
    ),
    INPUT_OVER_CURRENT_EVENT_STATISTICS(
        id = 0x0023u,
        valueLength = 6,
        propertyName = "Input Over Current Event Statistics",
    ),
    INPUT_OVER_RIPPLE_VOLTAGE_EVENT_STATISTICS(
        id = 0x0024u,
        valueLength = 6,
        propertyName = "Input Over Ripple Voltage Event Statistics",
    ),
    INPUT_OVER_VOLTAGE_EVENT_STATISTICS(
        id = 0x0025u,
        valueLength = 6,
        propertyName = "Input Over Voltage Event Statistics",
    ),
    INPUT_UNDER_CURRENT_EVENT_STATISTICS(
        id = 0x0026u,
        valueLength = 6,
        propertyName = "Input Under Current Event Statistics",
    ),
    INPUT_UNDER_VOLTAGE_EVENT_STATISTICS(
        id = 0x0027u,
        valueLength = 6,
        propertyName = "Input Under Voltage Event Statistics",
    ),
    INPUT_VOLTAGE_RANGE_SPECIFICATION(
        id = 0x0028u,
        valueLength = null,
        propertyName = "Input Voltage Range Specification",
    ),
    INPUT_VOLTAGE_RIPPLE_SPECIFICATION(
        id = 0x0029u,
        valueLength = 1,
        propertyName = "Input Voltage Ripple Specification",
    ),
    INPUT_VOLTAGE_STATISTICS(
        id = 0x002Au,
        valueLength = null,
        propertyName = "Input Voltage Statistics",
    ),
    LIGHT_CONTROL_AMBIENT_LUX_LEVEL_ON(
        id = 0x002Bu,
        valueLength = 3,
        propertyName = "Light Control Ambient LuxLevel On",
    ),
    LIGHT_CONTROL_AMBIENT_LUX_LEVEL_PROLONG(
        id = 0x002Cu,
        valueLength = 3,
        propertyName = "Light Control Ambient LuxLevel Prolong",
    ),
    LIGHT_CONTROL_AMBIENT_LUX_LEVEL_STANDBY(
        id = 0x002Du,
        valueLength = 3,
        propertyName = "Light Control Ambient LuxLevel Standby",
    ),
    LIGHT_CONTROL_LIGHTNESS_ON(
        id = 0x002Eu,
        valueLength = 2,
        propertyName = "Light Control Lightness On",
    ),
    LIGHT_CONTROL_LIGHTNESS_PROLONG(
        id = 0x002Fu,
        valueLength = 2,
        propertyName = "Light Control Lightness Prolong",
    ),
    LIGHT_CONTROL_LIGHTNESS_STANDBY(
        id = 0x0030u,
        valueLength = 2,
        propertyName = "Light Control Lightness Standby",
    ),
    LIGHT_CONTROL_REGULATOR_ACCURACY(
        id = 0x0031u,
        valueLength = 1,
        propertyName = "Light Control Regulator Accuracy",
    ),
    LIGHT_CONTROL_REGULATOR_KID(
        id = 0x0032u,
        valueLength = 4,
        propertyName = "Light Control Regulator Kid",
    ),
    LIGHT_CONTROL_REGULATOR_KIU(
        id = 0x0033u,
        valueLength = 4,
        propertyName = "Light Control Regulator Kiu",
    ),
    LIGHT_CONTROL_REGULATOR_KPD(
        id = 0x0034u,
        valueLength = 4,
        propertyName = "Light Control Regulator Kpd",
    ),
    LIGHT_CONTROL_REGULATOR_KPU(
        id = 0x0035u,
        valueLength = 4,
        propertyName = "Light Control Regulator Kpu",
    ),
    LIGHT_CONTROL_TIME_FADE(
        id = 0x0036u,
        valueLength = 3,
        propertyName = "Light Control Time Fade",
    ),
    LIGHT_CONTROL_TIME_FADE_ON(
        id = 0x0037u,
        valueLength = 3,
        propertyName = "Light Control Time Fade On",
    ),
    LIGHT_CONTROL_TIME_FADE_STANDBY_AUTO(
        id = 0x0038u,
        valueLength = 3,
        propertyName = "Light Control Time Fade Standby Auto",
    ),
    LIGHT_CONTROL_TIME_FADE_STANDBY_MANUAL(
        id = 0x0039u,
        valueLength = 3,
        propertyName = "Light Control Time Fade Standby Manual",
    ),
    LIGHT_CONTROL_TIME_OCCUPANCY_DELAY(
        id = 0x003Au,
        valueLength = 3,
        propertyName = "Light Control Time Occupancy Delay",
    ),
    LIGHT_CONTROL_TIME_PROLONG(
        id = 0x003Bu,
        valueLength = 3,
        propertyName = "Light Control Time Prolong",
    ),
    LIGHT_CONTROL_TIME_RUN_ON(
        id = 0x003Cu,
        valueLength = 3,
        propertyName = "Light Control Time Run On",
    ),
    LUMEN_MAINTENANCE_FACTOR(
        id = 0x003Du,
        valueLength = 1,
        propertyName = "Lumen Maintenance Factor",
    ),
    LUMINOUS_EFFICACY(id = 0x003Eu, valueLength = null, propertyName = "Luminous Efficacy"),
    LUMINOUS_ENERGY_SINCE_TURN_ON(
        id = 0x003Fu,
        valueLength = null,
        propertyName = "Luminous Energy Since Turn On",
    ),
    LUMINOUS_EXPOSURE(id = 0x0040u, valueLength = null, propertyName = "Luminous Exposure"),
    LUMINOUS_FLUX_RANGE(id = 0x0041u, valueLength = null, propertyName = "Luminous Flux Range"),
    MOTION_SENSED(id = 0x0042u, valueLength = 1, propertyName = "Motion Sensed"),
    MOTION_THRESHOLD(id = 0x0043u, valueLength = 1, propertyName = "Motion Threshold"),
    OPEN_CIRCUIT_EVENT_STATISTICS(
        id = 0x0044u,
        valueLength = 6,
        propertyName = "Open Circuit Event Statistics",
    ),
    OUTDOOR_STATISTICAL_VALUES(
        id = 0x0045u,
        valueLength = null,
        propertyName = "Outdoor Statistical Values",
    ),
    OUTPUT_CURRENT_RANGE(id = 0x0046u, valueLength = null, propertyName = "Output Current Range"),
    OUTPUT_CURRENT_STATISTICS(
        id = 0x0047u,
        valueLength = null,
        propertyName = "Output Current Statistics",
    ),
    OUTPUT_RIPPLE_VOLTAGE_SPECIFICATION(
        id = 0x0048u,
        valueLength = 1,
        propertyName = "Output Ripple Voltage Specification",
    ),
    OUTPUT_VOLTAGE_RANGE(id = 0x0049u, valueLength = null, propertyName = "Output Voltage Range"),
    OUTPUT_VOLTAGE_STATISTICS(
        id = 0x004Au,
        valueLength = null,
        propertyName = "Output Voltage Statistics",
    ),
    OVER_OUTPUT_RIPPLE_VOLTAGE_EVENT_STATISTICS(
        id = 0x004Bu,
        valueLength = 6,
        propertyName = "Over Output Ripple Voltage Event Statistics",
    ),
    PEOPLE_COUNT(id = 0x004Cu, valueLength = 2, propertyName = "People Count"),
    PRESENCE_DETECTED(id = 0x004Du, valueLength = 1, propertyName = "Presence Detected"),
    PRESENT_AMBIENT_LIGHT_LEVEL(
        id = 0x004Eu,
        valueLength = 3,
        propertyName = "Present Ambient Light Level",
    ),
    PRESENT_AMBIENT_TEMPERATURE(
        id = 0x004Fu,
        valueLength = 1,
        propertyName = "Present Ambient Temperature",
    ),
    PRESENT_CIE_1931_CHROMATICITY_COORDINATES(
        id = 0x0050u,
        valueLength = null,
        propertyName = "Present CIE 1931 Chromaticity Coordinates",
    ),
    PRESENT_CORRELATED_COLOR_TEMPERATURE(
        id = 0x0051u,
        valueLength = null,
        propertyName = "Present Correlated Color Temperature",
    ),
    PRESENT_DEVICE_INPUT_POWER(
        id = 0x0052u,
        valueLength = 3,
        propertyName = "Present Device Input Power",
    ),
    PRESENT_DEVICE_OPERATING_EFFICIENCY(
        id = 0x0053u,
        valueLength = 1,
        propertyName = "Present Device Operating Efficiency",
    ),
    PRESENT_DEVICE_OPERATING_TEMPERATURE(
        id = 0x0054u,
        valueLength = 2,
        propertyName = "Present Device Operating Temperature",
    ),
    PRESENT_ILLUMINANCE(id = 0x0055u, valueLength = 3, propertyName = "Present Illuminance"),
    PRESENT_INDOOR_AMBIENT_TEMPERATURE(
        id = 0x0056u,
        valueLength = 1,
        propertyName = "Present Indoor Ambient Temperature",
    ),
    PRESENT_INPUT_CURRENT(id = 0x0057u, valueLength = 2, propertyName = "Present Input Current"),
    PRESENT_INPUT_RIPPLE_VOLTAGE(
        id = 0x0058u,
        valueLength = 1,
        propertyName = "Present Input Ripple Voltage",
    ),
    PRESENT_INPUT_VOLTAGE(id = 0x0059u, valueLength = 2, propertyName = "Present Input Voltage"),
    PRESENT_LUMINOUS_FLUX(id = 0x005Au, valueLength = null, propertyName = "Present Luminous Flux"),
    PRESENT_OUTDOOR_AMBIENT_TEMPERATURE(
        id = 0x005Bu,
        valueLength = 1,
        propertyName = "Present Outdoor Ambient Temperature",
    ),
    PRESENT_OUTPUT_CURRENT(id = 0x005Cu, valueLength = 2, propertyName = "Present Output Current"),
    PRESENT_OUTPUT_VOLTAGE(id = 0x005Du, valueLength = 2, propertyName = "Present Output Voltage"),
    PRESENT_PLANCKIAN_DISTANCE(
        id = 0x005Eu,
        valueLength = null,
        propertyName = "Present Planckian Distance",
    ),
    PRESENT_RELATIVE_OUTPUT_RIPPLE_VOLTAGE(
        id = 0x005Fu,
        valueLength = 1,
        propertyName = "Present Relative Output Ripple Voltage",
    ),
    RELATIVE_DEVICE_ENERGY_USE_IN_A_PERIOD_OF_DAY(
        id = 0x0060u,
        valueLength = null,
        propertyName = "Relative Device Energy Use In A Period Of Day",
    ),
    RELATIVE_DEVICE_RUNTIME_IN_A_GENERIC_LEVEL_RANGE(
        id = 0x0061u,
        valueLength = null,
        propertyName = "Relative Device Runtime In A Generic Level Range",
    ),
    RELATIVE_EXPOSURE_TIME_IN_AN_ILLUMINANCE_RANGE(
        id = 0x0062u,
        valueLength = null,
        propertyName = "Relative Exposure Time In An Illuminance Range",
    ),
    RELATIVE_RUNTIME_IN_A_CORRELATED_COLOR_TEMPERATURE_RANGE(
        id = 0x0063u,
        valueLength = null,
        propertyName = "Relative Runtime In A Correlated Color Temperature Range",
    ),
    RELATIVE_RUNTIME_IN_A_DEVICE_OPERATING_TEMPERATURE_RANGE(
        id = 0x0064u,
        valueLength = null,
        propertyName = "Relative Runtime In A Device Operating Temperature Range",
    ),
    RELATIVE_RUNTIME_IN_AN_INPUT_CURRENT_RANGE(
        id = 0x0065u,
        valueLength = null,
        propertyName = "Relative Runtime In An Input Current Range",
    ),
    RELATIVE_RUNTIME_IN_AN_INPUT_VOLTAGE_RANGE(
        id = 0x0066u,
        valueLength = null,
        propertyName = "Relative Runtime In An Input Voltage Range",
    ),
    SHORT_CIRCUIT_EVENT_STATISTICS(
        id = 0x0067u,
        valueLength = 6,
        propertyName = "Short Circuit Event Statistics",
    ),
    TIME_SINCE_MOTION_SENSED(
        id = 0x0068u,
        valueLength = 3,
        propertyName = "Time Since Motion Sensed",
    ),
    TIME_SINCE_PRESENCE_DETECTED(
        id = 0x0069u,
        valueLength = 2,
        propertyName = "Time Since Presence Detected",
    ),
    TOTAL_DEVICE_ENERGY_USE(
        id = 0x006Au,
        valueLength = 3,
        propertyName = "Total Device Energy Use",
    ),
    TOTAL_DEVICE_OFF_ON_CYCLES(
        id = 0x006Bu,
        valueLength = 3,
        propertyName = "Total Device Off On Cycles",
    ),
    TOTAL_DEVICE_POWER_ON_CYCLES(
        id = 0x006Cu,
        valueLength = 3,
        propertyName = "Total Device Power On Cycles",
    ),
    TOTAL_DEVICE_POWER_ON_TIME(
        id = 0x006Du,
        valueLength = 3,
        propertyName = "Total Device Power On Time",
    ),
    TOTAL_DEVICE_RUNTIME(id = 0x006Eu, valueLength = 3, propertyName = "Total Device Runtime"),
    TOTAL_LIGHT_EXPOSURE_TIME(
        id = 0x006Fu,
        valueLength = 3,
        propertyName = "Total Light Exposure Time",
    ),
    TOTAL_LUMINOUS_ENERGY(id = 0x0070u, valueLength = null, propertyName = "Total Luminous Energy"),
    DESIRED_AMBIENT_TEMPERATURE(
        id = 0x0071u,
        valueLength = 1,
        propertyName = "Desired Ambient Temperature",
    ),
    PRECISE_TOTAL_DEVICE_ENERGY_USE(
        id = 0x0072u,
        valueLength = 4,
        propertyName = "Precise Total Device Energy Use",
    ),
    POWER_FACTOR(id = 0x0073u, valueLength = null, propertyName = "Power Factor"),
    SENSOR_GAIN(id = 0x0074u, valueLength = 4, propertyName = "Sensor Gain"),
    PRECISE_PRESENT_AMBIENT_TEMPERATURE(
        id = 0x0075u,
        valueLength = 2,
        propertyName = "Precise Present Ambient Temperature",
    ),
    PRESENT_AMBIENT_RELATIVE_HUMIDITY(
        id = 0x0076u,
        valueLength = 2,
        propertyName = "Present Ambient Relative Humidity",
    ),
    PRESENT_AMBIENT_CARBON_DIOXIDE_CONCENTRATION(
        id = 0x0077u,
        valueLength = 2,
        propertyName = "Present Ambient Carbon Dioxide Concentration",
    ),
    PRESENT_AMBIENT_VOLATILE_ORGANIC_COMPOUNDS_CONCENTRATION(
        id = 0x0078u,
        valueLength = 2,
        propertyName = "Present Ambient Volatile Organic Compounds Concentration",
    ),
    PRESENT_AMBIENT_NOISE(id = 0x0079u, valueLength = null, propertyName = "Present Ambient Noise"),
    ACTIVE_ENERGY_LOADSIDE(id = 0x0080u, valueLength = 4, propertyName = "Active Energy Loadside"),
    ACTIVE_POWER_LOADSIDE(id = 0x0081u, valueLength = 3, propertyName = "Active Power Loadside"),
    AIR_PRESSURE(id = 0x0082u, valueLength = 4, propertyName = "Air Pressure"),
    APPARENT_ENERGY(id = 0x0083u, valueLength = 4, propertyName = "Apparent Energy"),
    APPARENT_POWER(id = 0x0084u, valueLength = 3, propertyName = "Apparent Power"),
    APPARENT_WIND_DIRECTION(
        id = 0x0085u,
        valueLength = null,
        propertyName = "Apparent Wind Direction",
    ),
    APPARENT_WIND_SPEED(id = 0x0086u, valueLength = null, propertyName = "Apparent Wind Speed"),
    DEW_POINT(id = 0x0087u, valueLength = null, propertyName = "Dew Point"),
    EXTERNAL_SUPPLY_VOLTAGE(
        id = 0x0088u,
        valueLength = null,
        propertyName = "External Supply Voltage",
    ),
    EXTERNAL_SUPPLY_VOLTAGE_FREQUENCY(
        id = 0x0089u,
        valueLength = null,
        propertyName = "External Supply Voltage Frequency",
    ),
    GUST_FACTOR(id = 0x008Au, valueLength = null, propertyName = "Gust Factor"),
    HEAT_INDEX(id = 0x008Bu, valueLength = null, propertyName = "Heat Index"),
    LIGHT_DISTRIBUTION(id = 0x008Cu, valueLength = null, propertyName = "Light Distribution"),
    LIGHT_SOURCE_CURRENT(id = 0x008Du, valueLength = 3, propertyName = "Light Source Current"),
    LIGHT_SOURCE_ON_TIME_NOT_RESETTABLE(
        id = 0x008Eu,
        valueLength = 4,
        propertyName = "Light Source On Time Not Resettable",
    ),
    LIGHT_SOURCE_ON_TIME_RESETTABLE(
        id = 0x008Fu,
        valueLength = 4,
        propertyName = "Light Source On Time Resettable",
    ),
    LIGHT_SOURCE_OPEN_CIRCUIT_STATISTICS(
        id = 0x0090u,
        valueLength = 6,
        propertyName = "Light Source Open Circuit Statistics",
    ),
    LIGHT_SOURCE_OVERALL_FAILURES_STATISTICS(
        id = 0x0091u,
        valueLength = 6,
        propertyName = "Light Source Overall Failures Statistics",
    ),
    LIGHT_SOURCE_SHORT_CIRCUIT_STATISTICS(
        id = 0x0092u,
        valueLength = 6,
        propertyName = "Light Source Short Circuit Statistics",
    ),
    LIGHT_SOURCE_START_COUNTER_RESETTABLE(
        id = 0x0093u,
        valueLength = 3,
        propertyName = "Light Source Start Counter Resettable",
    ),
    LIGHT_SOURCE_TEMPERATURE(
        id = 0x0094u,
        valueLength = null,
        propertyName = "Light Source Temperature",
    ),
    LIGHT_SOURCE_THERMAL_DERATING_STATISTICS(
        id = 0x0095u,
        valueLength = 6,
        propertyName = "Light Source Thermal Derating Statistics",
    ),
    LIGHT_SOURCE_THERMAL_SHUTDOWN_STATISTICS(
        id = 0x0096u,
        valueLength = 6,
        propertyName = "Light Source Thermal Shutdown Statistics",
    ),
    LIGHT_SOURCE_TOTAL_POWER_ON_CYCLES(
        id = 0x0097u,
        valueLength = 3,
        propertyName = "Light Source Total Power On Cycles",
    ),
    LIGHT_SOURCE_VOLTAGE(id = 0x0098u, valueLength = 3, propertyName = "Light Source Voltage"),
    LUMINAIRE_COLOR(id = 0x0099u, valueLength = 24, propertyName = "Luminaire Color"),
    LUMINAIRE_IDENTIFICATION_NUMBER(
        id = 0x009Au,
        valueLength = 24,
        propertyName = "Luminaire Identification Number",
    ),
    LUMINAIRE_MANUFACTURER_GTIN(
        id = 0x009Bu,
        valueLength = null,
        propertyName = "Luminaire Manufacturer GTIN",
    ),
    LUMINAIRE_NOMINAL_INPUT_POWER(
        id = 0x009Cu,
        valueLength = 3,
        propertyName = "Luminaire Nominal Input Power",
    ),
    LUMINAIRE_NOMINAL_MAXIMUM_AC_MAINS_VOLTAGE(
        id = 0x009Du,
        valueLength = 2,
        propertyName = "Luminaire Nominal Maximum AC Mains Voltage",
    ),
    LUMINAIRE_NOMINAL_MINIMUM_AC_MAINS_VOLTAGE(
        id = 0x009Eu,
        valueLength = 2,
        propertyName = "Luminaire Nominal Minimum AC Mains Voltage",
    ),
    LUMINAIRE_POWER_AT_MINIMUM_DIM_LEVEL(
        id = 0x009Fu,
        valueLength = 3,
        propertyName = "Luminaire Power At Minimum Dim Level",
    ),
    LUMINAIRE_TIME_OF_MANUFACTURE(
        id = 0x00A0u,
        valueLength = 3,
        propertyName = "Luminaire Time Of Manufacture",
    ),
    MAGNETIC_DECLINATION(id = 0x00A1u, valueLength = null, propertyName = "Magnetic Declination"),
    MAGNETIC_FLUX_DENSITY_2D(
        id = 0x00A2u,
        valueLength = null,
        propertyName = "Magnetic Flux Density - 2D",
    ),
    MAGNETIC_FLUX_DENSITY_3D(
        id = 0x00A3u,
        valueLength = null,
        propertyName = "Magnetic Flux Density - 3D",
    ),
    NOMINAL_LIGHT_OUTPUT(id = 0x00A4u, valueLength = null, propertyName = "Nominal Light Output"),
    OVERALL_FAILURE_CONDITION(
        id = 0x00A5u,
        valueLength = 6,
        propertyName = "Overall Failure Condition",
    ),
    POLLEN_CONCENTRATION(id = 0x00A6u, valueLength = null, propertyName = "Pollen Concentration"),
    PRESENT_INDOOR_RELATIVE_HUMIDITY(
        id = 0x00A7u,
        valueLength = 2,
        propertyName = "Present Indoor Relative Humidity",
    ),
    PRESENT_OUTDOOR_RELATIVE_HUMIDITY(
        id = 0x00A8u,
        valueLength = 2,
        propertyName = "Present Outdoor Relative Humidity",
    ),
    PRESSURE(id = 0x00A9u, valueLength = 4, propertyName = "Pressure"),
    RAINFALL(id = 0x00AAu, valueLength = 2, propertyName = "Rainfall"),
    RATED_MEDIAN_USEFUL_LIFE_OF_LUMINAIRE(
        id = 0x00ABu,
        valueLength = 3,
        propertyName = "Rated Median Useful Life Of Luminaire",
    ),
    RATED_MEDIAN_USEFUL_LIGHT_SOURCE_STARTS(
        id = 0x00ACu,
        valueLength = 3,
        propertyName = "Rated Median Useful Light Source Starts",
    ),
    REFERENCE_TEMPERATURE(id = 0x00ADu, valueLength = null, propertyName = "Reference Temperature"),
    TOTAL_DEVICE_STARTS(id = 0x00AEu, valueLength = 3, propertyName = "Total Device Starts"),
    TRUE_WIND_DIRECTION(id = 0x00AFu, valueLength = null, propertyName = "True Wind Direction"),
    TRUE_WIND_SPEED(id = 0x00B0u, valueLength = null, propertyName = "True Wind Speed"),
    UV_INDEX(id = 0x00B1u, valueLength = 1, propertyName = "UV Index"),
    WIND_CHILL(id = 0x00B2u, valueLength = null, propertyName = "Wind Chill"),
    LIGHT_SOURCE_TYPE(id = 0x00B3u, valueLength = null, propertyName = "Light Source Type"),
    LUMINAIRE_IDENTIFICATION_STRING(
        id = 0x00B4u,
        valueLength = 64,
        propertyName = "Luminaire Identification String",
    ),
    OUTPUT_POWER_LIMITATION(
        id = 0x00B5u,
        valueLength = 6,
        propertyName = "Output Power Limitation",
    ),
    THERMAL_DERATING(id = 0x00B6u, valueLength = 6, propertyName = "Thermal Derating"),
    OUTPUT_CURRENT_PERCENT(id = 0x00B7u, valueLength = 1, propertyName = "Output Current Percent");

    override fun toString() = propertyName

    /**
     * Parses the characteristic from the given data.
     *
     * If the given length does not match [valueLength], the characteristic is returned with a
     * default value (`false`, 0, `null`, etc.).
     *
     * This method does not verify that the length of the data is sufficient.
     *
     * @param data   The data to read from.
     * @param offset The offset to read from.
     * @param length Expected length of the value.
     * @return The characteristic value.
     */
    internal fun read(data: ByteArray, offset: Int, length: Int): DevicePropertyCharacteristic {
        val valid = length == valueLength
        return when (this) {
            // UInt8 -> UInt8
            UV_INDEX ->
                if (!valid) DevicePropertyCharacteristic.UvIndex(0u)
                else DevicePropertyCharacteristic.UvIndex(data[offset].toUByte())

            // UInt8 -> Bool
            PRESENCE_DETECTED ->
                if (!valid) DevicePropertyCharacteristic.Bool(false)
                else DevicePropertyCharacteristic.Bool(data[offset] != 0x00.toByte())

            // 2 x UInt16 + 2 x UInt8 -> Event Statistics
            DEVICE_OVER_TEMPERATURE_EVENT_STATISTICS, DEVICE_UNDER_TEMPERATURE_EVENT_STATISTICS,
            INPUT_OVER_CURRENT_EVENT_STATISTICS, INPUT_OVER_RIPPLE_VOLTAGE_EVENT_STATISTICS,
            INPUT_OVER_VOLTAGE_EVENT_STATISTICS, INPUT_UNDER_CURRENT_EVENT_STATISTICS,
            INPUT_UNDER_VOLTAGE_EVENT_STATISTICS, LIGHT_SOURCE_OPEN_CIRCUIT_STATISTICS,
            LIGHT_SOURCE_OVERALL_FAILURES_STATISTICS, LIGHT_SOURCE_SHORT_CIRCUIT_STATISTICS,
            LIGHT_SOURCE_THERMAL_DERATING_STATISTICS, LIGHT_SOURCE_THERMAL_SHUTDOWN_STATISTICS,
            OPEN_CIRCUIT_EVENT_STATISTICS, OUTPUT_POWER_LIMITATION,
            OVER_OUTPUT_RIPPLE_VOLTAGE_EVENT_STATISTICS, OVERALL_FAILURE_CONDITION,
            SHORT_CIRCUIT_EVENT_STATISTICS, THERMAL_DERATING,
                ->
                if (!valid) DevicePropertyCharacteristic.EventStatistics(
                    count = null,
                    averageEventDuration = null,
                    timeElapsedSinceLastEvent = null,
                    sensingDuration = null,
                ) else DevicePropertyCharacteristic.EventStatistics(
                    count = data
                        .getUShort(offset = offset, order = ByteOrder.LITTLE_ENDIAN)
                        .unlessUnknown(UNKNOWN_16),
                    averageEventDuration = data
                        .getUShort(offset = offset + 2, order = ByteOrder.LITTLE_ENDIAN)
                        .unlessUnknown(UNKNOWN_16),
                    timeElapsedSinceLastEvent = TimeExponential
                        .from(rawValue = data[offset + 4].toUByte()),
                    sensingDuration = TimeExponential.from(rawValue = data[offset + 5].toUByte()),
                )

            // UInt8 -> BigDecimal?
            LIGHT_CONTROL_REGULATOR_ACCURACY, OUTPUT_RIPPLE_VOLTAGE_SPECIFICATION,
            INPUT_VOLTAGE_RIPPLE_SPECIFICATION, OUTPUT_CURRENT_PERCENT,
            LUMEN_MAINTENANCE_FACTOR, MOTION_SENSED, MOTION_THRESHOLD,
            PRESENT_DEVICE_OPERATING_EFFICIENCY, PRESENT_RELATIVE_OUTPUT_RIPPLE_VOLTAGE,
            PRESENT_INPUT_RIPPLE_VOLTAGE,
                ->
                if (!valid) DevicePropertyCharacteristic.Percentage8(null)
                else DevicePropertyCharacteristic.Percentage8(
                    value = data[offset]
                        .toUByte()
                        .toLong()
                        .toDecimal(
                            range = ZERO..HUNDRED,
                            resolution = HALF,
                            unknownValue = 0xFF,
                        )
                )

            // Int8 -> BigDecimal?
            DESIRED_AMBIENT_TEMPERATURE, PRESENT_AMBIENT_TEMPERATURE,
            PRESENT_INDOOR_AMBIENT_TEMPERATURE, PRESENT_OUTDOOR_AMBIENT_TEMPERATURE,
                ->
                if (!valid) DevicePropertyCharacteristic.Temperature8(null)
                else DevicePropertyCharacteristic.Temperature8(
                    value = data[offset]
                        .toLong()
                        .toDecimal(
                            range = BigDecimal("-64.0")..BigDecimal("63.0"),
                            resolution = HALF,
                            unknownValue = 0x7F,
                        )
                )

            // UInt16 -> UInt16
            LIGHT_CONTROL_LIGHTNESS_ON, LIGHT_CONTROL_LIGHTNESS_PROLONG,
            LIGHT_CONTROL_LIGHTNESS_STANDBY,
                ->
                if (!valid) DevicePropertyCharacteristic.PerceivedLightness(0u)
                else DevicePropertyCharacteristic.PerceivedLightness(
                    data.getUShort(
                        offset = offset,
                        order = ByteOrder.LITTLE_ENDIAN
                    )
                )

            RAINFALL ->
                if (!valid) DevicePropertyCharacteristic.Rainfall(0u)
                else DevicePropertyCharacteristic.Rainfall(
                    data.getUShort(
                        offset = offset,
                        order = ByteOrder.LITTLE_ENDIAN
                    )
                )

            // UInt16 -> UInt16?
            PEOPLE_COUNT ->
                if (!valid) DevicePropertyCharacteristic.Count16(null)
                else DevicePropertyCharacteristic.Count16(
                    data.getUShort(offset = offset, order = ByteOrder.LITTLE_ENDIAN)
                        .unlessUnknown(UNKNOWN_16)
                )

            TIME_SINCE_PRESENCE_DETECTED ->
                if (!valid) DevicePropertyCharacteristic.TimeSecond16(null)
                else DevicePropertyCharacteristic.TimeSecond16(
                    value = data
                        .getUShort(offset = offset, order = ByteOrder.LITTLE_ENDIAN)
                        .unlessUnknown(UNKNOWN_16)
                )

            PRESENT_AMBIENT_CARBON_DIOXIDE_CONCENTRATION ->
                if (!valid) DevicePropertyCharacteristic.Co2Concentration(null)
                else DevicePropertyCharacteristic.Co2Concentration(
                    value = data
                        .getUShort(offset = offset, order = ByteOrder.LITTLE_ENDIAN)
                        .unlessUnknown(UNKNOWN_16)
                )

            PRESENT_AMBIENT_VOLATILE_ORGANIC_COMPOUNDS_CONCENTRATION ->
                if (!valid) DevicePropertyCharacteristic.VocConcentration(null)
                else DevicePropertyCharacteristic.VocConcentration(
                    value = data
                        .getUShort(offset = offset, order = ByteOrder.LITTLE_ENDIAN)
                        .unlessUnknown(UNKNOWN_16)
                )

            // UInt16 -> BigDecimal?
            PRESENT_AMBIENT_RELATIVE_HUMIDITY, PRESENT_INDOOR_RELATIVE_HUMIDITY,
            PRESENT_OUTDOOR_RELATIVE_HUMIDITY,
                ->
                if (!valid) DevicePropertyCharacteristic.Humidity(null)
                else DevicePropertyCharacteristic.Humidity(
                    value = data
                        .getUShort(offset = offset, order = ByteOrder.LITTLE_ENDIAN).toLong()
                        .toDecimal(
                            range = ZERO..HUNDRED,
                            resolution = CENTI,
                            unknownValue = 0xFFFF,
                        )
                )

            PRESENT_OUTPUT_CURRENT, PRESENT_INPUT_CURRENT ->
                if (!valid) DevicePropertyCharacteristic.ElectricCurrent(null)
                else DevicePropertyCharacteristic.ElectricCurrent(
                    value = data
                        .getUShort(offset = offset, order = ByteOrder.LITTLE_ENDIAN)
                        .toLong()
                        .toDecimal(
                            range = ZERO..MAX_CURRENT,
                            resolution = CENTI,
                            unknownValue = 0xFFFF,
                        )
                )

            AVERAGE_INPUT_CURRENT, AVERAGE_OUTPUT_CURRENT, LIGHT_SOURCE_CURRENT ->
                if (!valid) DevicePropertyCharacteristic.AverageCurrent(null, null)
                else DevicePropertyCharacteristic.AverageCurrent(
                    value = data
                        .getUShort(offset = offset, order = ByteOrder.LITTLE_ENDIAN)
                        .toLong()
                        .toDecimal(
                            range = ZERO..MAX_CURRENT,
                            resolution = CENTI,
                            unknownValue = 0xFFFF,
                        ),
                    sensingDuration = TimeExponential.from(rawValue = data[offset + 2].toUByte()),
                )

            LUMINAIRE_NOMINAL_MAXIMUM_AC_MAINS_VOLTAGE,
            LUMINAIRE_NOMINAL_MINIMUM_AC_MAINS_VOLTAGE, PRESENT_INPUT_VOLTAGE,
            PRESENT_OUTPUT_VOLTAGE,
                ->
                if (!valid) DevicePropertyCharacteristic.Voltage(null)
                else DevicePropertyCharacteristic.Voltage(
                    value = data
                        .getUShort(offset = offset, order = ByteOrder.LITTLE_ENDIAN)
                        .toLong()
                        .toDecimal(
                            range = ZERO..MAX_VOLTAGE,
                            resolution = VOLTAGE_RESOLUTION,
                            unknownValue = 0xFFFF,
                        )
                )

            AVERAGE_INPUT_VOLTAGE, AVERAGE_OUTPUT_VOLTAGE, LIGHT_SOURCE_VOLTAGE ->
                if (!valid) DevicePropertyCharacteristic.AverageVoltage(null, null)
                else DevicePropertyCharacteristic.AverageVoltage(
                    value = data
                        .getUShort(offset = offset, order = ByteOrder.LITTLE_ENDIAN)
                        .toLong()
                        .toDecimal(
                            range = ZERO..MAX_VOLTAGE,
                            resolution = VOLTAGE_RESOLUTION,
                            unknownValue = 0xFFFF,
                        ),
                    sensingDuration = TimeExponential.from(rawValue = data[offset + 2].toUByte()),
                )

            // Int16 -> BigDecimal?
            PRECISE_PRESENT_AMBIENT_TEMPERATURE, PRESENT_DEVICE_OPERATING_TEMPERATURE ->
                if (!valid) DevicePropertyCharacteristic.Temperature(null)
                else DevicePropertyCharacteristic.Temperature(
                    value = data
                        .getShort(offset = offset, order = ByteOrder.LITTLE_ENDIAN)
                        .toLong()
                        .toDecimal(
                            range = BigDecimal("-273.15")..BigDecimal("327.67"),
                            resolution = CENTI,
                            unknownValue = -32768,
                        )
                )

            // UInt24 -> BigDecimal?
            LIGHT_CONTROL_AMBIENT_LUX_LEVEL_ON, LIGHT_CONTROL_AMBIENT_LUX_LEVEL_PROLONG,
            LIGHT_CONTROL_AMBIENT_LUX_LEVEL_STANDBY, PRESENT_AMBIENT_LIGHT_LEVEL,
            PRESENT_ILLUMINANCE,
                ->
                if (!valid) DevicePropertyCharacteristic.Illuminance(null)
                else DevicePropertyCharacteristic.Illuminance(
                    value = data
                        .getUInt(
                            offset = offset,
                            format = IntFormat.UINT24,
                            order = ByteOrder.LITTLE_ENDIAN
                        )
                        .toLong()
                        .toDecimal(resolution = CENTI, unknownValue = 0xFFFFFF)
                )

            ACTIVE_POWER_LOADSIDE, LUMINAIRE_NOMINAL_INPUT_POWER,
            LUMINAIRE_POWER_AT_MINIMUM_DIM_LEVEL, PRESENT_DEVICE_INPUT_POWER,
                ->
                if (!valid) DevicePropertyCharacteristic.Power(null)
                else DevicePropertyCharacteristic.Power(
                    value = data
                        .getUInt(
                            offset = offset,
                            format = IntFormat.UINT24,
                            order = ByteOrder.LITTLE_ENDIAN
                        )
                        .toLong()
                        .toDecimal(resolution = DECI, unknownValue = 0xFFFFFF)
                )

            // UInt24 -> UInt24?
            LIGHT_SOURCE_START_COUNTER_RESETTABLE, LIGHT_SOURCE_TOTAL_POWER_ON_CYCLES,
            RATED_MEDIAN_USEFUL_LIGHT_SOURCE_STARTS, TOTAL_DEVICE_OFF_ON_CYCLES,
            TOTAL_DEVICE_POWER_ON_CYCLES, TOTAL_DEVICE_STARTS,
                ->
                if (!valid) DevicePropertyCharacteristic.Count24(null)
                else DevicePropertyCharacteristic.Count24(
                    value = data
                        .getUInt(
                            offset = offset,
                            format = IntFormat.UINT24,
                            order = ByteOrder.LITTLE_ENDIAN
                        )
                        .unlessUnknown(UNKNOWN_24)
                )

            DEVICE_RUNTIME_SINCE_TURN_ON, DEVICE_RUNTIME_WARRANTY,
            RATED_MEDIAN_USEFUL_LIFE_OF_LUMINAIRE, TOTAL_DEVICE_POWER_ON_TIME,
            TOTAL_DEVICE_RUNTIME, TOTAL_LIGHT_EXPOSURE_TIME,
                ->
                if (!valid) DevicePropertyCharacteristic.TimeHour24(null)
                else DevicePropertyCharacteristic.TimeHour24(
                    value = data
                        .getUInt(
                            offset = offset,
                            format = IntFormat.UINT24,
                            order = ByteOrder.LITTLE_ENDIAN
                        )
                        .unlessUnknown(UNKNOWN_24)
                )

            LIGHT_CONTROL_TIME_FADE, LIGHT_CONTROL_TIME_FADE_ON,
            LIGHT_CONTROL_TIME_FADE_STANDBY_AUTO, LIGHT_CONTROL_TIME_FADE_STANDBY_MANUAL,
            LIGHT_CONTROL_TIME_OCCUPANCY_DELAY, LIGHT_CONTROL_TIME_PROLONG,
            LIGHT_CONTROL_TIME_RUN_ON, TIME_SINCE_MOTION_SENSED,
                ->
                if (!valid) DevicePropertyCharacteristic.TimeMillisecond24(null)
                else DevicePropertyCharacteristic.TimeMillisecond24(
                    value = data
                        .getUInt(
                            offset = offset,
                            format = IntFormat.UINT24,
                            order = ByteOrder.LITTLE_ENDIAN
                        )
                        .unlessUnknown(UNKNOWN_24)
                )

            DEVICE_ENERGY_USE_SINCE_TURN_ON, TOTAL_DEVICE_ENERGY_USE ->
                if (!valid) DevicePropertyCharacteristic.Energy(null)
                else DevicePropertyCharacteristic.Energy(
                    value = data
                        .getUInt(
                            offset = offset,
                            format = IntFormat.UINT24,
                            order = ByteOrder.LITTLE_ENDIAN
                        )
                        .unlessUnknown(UNKNOWN_24)
                )

            // UInt24 -> Instant?
            DEVICE_DATE_OF_MANUFACTURE, LUMINAIRE_TIME_OF_MANUFACTURE -> when {
                !valid -> DevicePropertyCharacteristic.DateUtc(null)
                else -> {
                    val numberOfDays = data
                        .getUInt(
                            offset = offset,
                            format = IntFormat.UINT24,
                            order = ByteOrder.LITTLE_ENDIAN
                        )
                    DevicePropertyCharacteristic.DateUtc(
                        if (numberOfDays == 0u) null
                        else Instant.fromEpochSeconds(numberOfDays.toLong() * SECONDS_PER_DAY)
                    )
                }
            }

            // UInt32 -> BigDecimal
            PRESSURE, AIR_PRESSURE ->
                if (!valid) DevicePropertyCharacteristic.Pressure(ZERO)
                else DevicePropertyCharacteristic.Pressure(
                    value = data
                        .getUInt(
                            offset = offset,
                            format = IntFormat.UINT32,
                            order = ByteOrder.LITTLE_ENDIAN
                        )
                        .toLong()
                        .toDecimal(resolution = DECI)
                )

            // UInt24 -> ValidDecimal?
            APPARENT_POWER -> when {
                !valid -> DevicePropertyCharacteristic.ApparentPower(null)
                else -> {
                    val value = data
                        .getUInt(
                            offset = offset,
                            format = IntFormat.UINT24,
                            order = ByteOrder.LITTLE_ENDIAN
                        )
                    DevicePropertyCharacteristic.ApparentPower(
                        value = when (value) {
                            UNKNOWN_24 -> null
                            UNKNOWN_24 - 1u -> ValidDecimal.Invalid
                            else -> ValidDecimal.Valid(
                                value.toLong().toDecimal(resolution = DECI)
                            )
                        }
                    )
                }
            }

            // UInt32 -> UInt32?
            LIGHT_SOURCE_ON_TIME_RESETTABLE, LIGHT_SOURCE_ON_TIME_NOT_RESETTABLE ->
                if (!valid) DevicePropertyCharacteristic.TimeSecond32(null)
                else DevicePropertyCharacteristic.TimeSecond32(
                    value = data.getUInt(
                        offset = offset,
                        format = IntFormat.UINT32,
                        order = ByteOrder.LITTLE_ENDIAN
                    ).unlessUnknown(UNKNOWN_32)
                )

            // UInt32 -> ValidDecimal?
            PRECISE_TOTAL_DEVICE_ENERGY_USE, ACTIVE_ENERGY_LOADSIDE -> when {
                !valid -> DevicePropertyCharacteristic.Energy32(null)
                else -> DevicePropertyCharacteristic.Energy32(
                    value = data.getUInt(
                        offset = offset,
                        format = IntFormat.UINT32,
                        order = ByteOrder.LITTLE_ENDIAN
                    ).toValidDecimal(resolution = MILLI)
                )
            }

            APPARENT_ENERGY -> when {
                !valid -> DevicePropertyCharacteristic.ApparentEnergy32(null)
                else -> DevicePropertyCharacteristic.ApparentEnergy32(
                    value = data.getUInt(
                        offset = offset,
                        format = IntFormat.UINT32,
                        order = ByteOrder.LITTLE_ENDIAN
                    ).toValidDecimal(resolution = MILLI)
                )
            }

            // Float32 (IEEE 754)
            LIGHT_CONTROL_REGULATOR_KID, LIGHT_CONTROL_REGULATOR_KIU,
            LIGHT_CONTROL_REGULATOR_KPD, LIGHT_CONTROL_REGULATOR_KPU, SENSOR_GAIN,
                ->
                if (!valid) DevicePropertyCharacteristic.Coefficient(value = 0.0f)
                else DevicePropertyCharacteristic.Coefficient(
                    value = data.getFloat(
                        offset = offset,
                        format = FloatFormat.IEEE_11073_32_BIT,
                        order = ByteOrder.LITTLE_ENDIAN
                    )
                )

            // String
            DEVICE_FIRMWARE_REVISION, DEVICE_SOFTWARE_REVISION ->
                DevicePropertyCharacteristic.FixedString8(
                    value = data.string(
                        offset = offset,
                        length = 8,
                        valid = valid
                    )
                )

            DEVICE_HARDWARE_REVISION, DEVICE_SERIAL_NUMBER ->
                DevicePropertyCharacteristic.FixedString16(
                    value = data.string(
                        offset = offset,
                        length = 16,
                        valid = valid
                    )
                )

            DEVICE_MODEL_NUMBER, LUMINAIRE_COLOR, LUMINAIRE_IDENTIFICATION_NUMBER ->
                DevicePropertyCharacteristic.FixedString24(
                    value = data.string(
                        offset = offset,
                        length = 24,
                        valid = valid
                    )
                )

            DEVICE_MANUFACTURER_NAME ->
                DevicePropertyCharacteristic.FixedString36(
                    value = data.string(
                        offset = offset,
                        length = 36,
                        valid = valid
                    )
                )

            LUMINAIRE_IDENTIFICATION_STRING ->
                DevicePropertyCharacteristic.FixedString64(
                    value = data.string(
                        offset = offset,
                        length = 64,
                        valid = valid
                    )
                )

            // Other
            else -> DevicePropertyCharacteristic.Other(
                value = data.copyOfRange(fromIndex = offset, toIndex = offset + length)
            )
        }
    }

    companion object {

        /**
         * Returns the Device Property with the given Property ID.
         *
         * @param id The 16-bit Property ID.
         * @return The Device Property, or `null` when the ID is not known.
         */
        fun from(id: UShort): DeviceProperty? = entries.find { it.id == id }

        private val ZERO: BigDecimal = BigDecimal.ZERO
        private val HUNDRED: BigDecimal = BigDecimal("100")
        private val HALF: BigDecimal = BigDecimal("0.5")
        private val DECI: BigDecimal = BigDecimal("0.1")
        private val CENTI: BigDecimal = BigDecimal("0.01")
        private val MILLI: BigDecimal = BigDecimal("0.001")
        private val VOLTAGE_RESOLUTION: BigDecimal = BigDecimal("0.015625")
        private val MAX_CURRENT: BigDecimal = BigDecimal("655.34")
        private val MAX_VOLTAGE: BigDecimal = BigDecimal("1022")

        private val UNKNOWN_16 = 0xFFFF.toUShort()
        private val UNKNOWN_24 = 0xFFFFFFu
        private val UNKNOWN_32 = 0xFFFFFFFFu
        private const val SECONDS_PER_DAY = 86_400L
    }
}

/**
 * Parses the characteristic of a Device Property which may not be known.
 *
 * Unknown properties are returned as [DevicePropertyCharacteristic.Other] holding the raw value,
 * which mirrors the behavior of an unknown Property ID in the mesh specification.
 *
 * @param data   The data to read from.
 * @param offset The offset to read from.
 * @param length Expected length of the value.
 * @return The characteristic value.
 */
internal fun DeviceProperty?.read(
    data: ByteArray,
    offset: Int,
    length: Int,
): DevicePropertyCharacteristic = this
    ?.read(data = data, offset = offset, length = length)
    ?: DevicePropertyCharacteristic.Other(
        value = data.copyOfRange(
            fromIndex = offset,
            toIndex = offset + length
        )
    )

/**
 * Reads a UTF-8 string of the given length, or a string of spaces when the value is not valid.
 */
private fun ByteArray.string(offset: Int, length: Int, valid: Boolean): String =
    if (!valid) " ".repeat(length)
    else String(this, offset, length, Charsets.UTF_8)

/**
 * Returns the value, or `null` when it is equal to the unknown value.
 */
private fun UShort.unlessUnknown(unknownValue: UShort): UShort? =
    if (this == unknownValue) null else this

/**
 * Returns the value, or `null` when it is equal to the unknown value.
 */
private fun UInt.unlessUnknown(unknownValue: UInt): UInt? =
    if (this == unknownValue) null else this

/**
 * Converts the value to a [BigDecimal] with the given resolution, clamped to the given range.
 */
private fun Long.toDecimal(
    range: ClosedRange<BigDecimal>? = null,
    resolution: BigDecimal,
): BigDecimal {
    val value = BigDecimal(this).multiply(resolution)
    return range?.let { value.coerceIn(it.start, it.endInclusive) } ?: value
}

/**
 * Converts the value to a [BigDecimal], or `null` when it is equal to the unknown value.
 */
private fun Long.toDecimal(
    range: ClosedRange<BigDecimal>? = null,
    resolution: BigDecimal,
    unknownValue: Long,
): BigDecimal? = if (this == unknownValue) null else toDecimal(range, resolution)

/**
 * Converts a 32-bit value to a [ValidDecimal], where 0xFFFFFFFE means invalid and 0xFFFFFFFF
 * that the value is not known.
 */
private fun UInt.toValidDecimal(resolution: BigDecimal): ValidDecimal? = when (this) {
    0xFFFFFFFFu -> null
    0xFFFFFFFEu -> ValidDecimal.Invalid
    else -> ValidDecimal.Valid(BigDecimal(toLong()).multiply(resolution))
}