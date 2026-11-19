package no.nordicsemi.kotlin.mesh.core.messages.lighting

import no.nordicsemi.kotlin.data.ByteOrder
import no.nordicsemi.kotlin.data.getShort
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.LightingMessageInitializer

/**
 * Sets the Light CTL Default state of a Light CTL Setup Server.
 *
 * The response to this message is a [LightCTLDefaultStatus].
 *
 * @property lightness   Lightness value.
 * @property temperature Temperature in Kelvin, coerced to 800 to 20000.
 * @property deltaUV     Delta UV value (signed).
 */
class LightCTLDefaultSet(
    val lightness: UShort,
    temperature: UShort,
    val deltaUV: Short,
) : AcknowledgedApplicationMessage {
    val temperature: UShort = temperature.coerceCtlTemperature()
    override val opCode = Initializer.opCode
    override val responseOpCode = LightCTLDefaultStatus.opCode
    override val parameters: ByteArray = lightness.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            this.temperature.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            deltaUV.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    override fun toString() = "LightCTLDefaultSet(lightness: $lightness, temperature: $temperature, deltaUV: $deltaUV)"

    companion object Initializer : LightingMessageInitializer {
        override val opCode = 0x8269u

        override fun init(parameters: ByteArray?) = parameters?.takeIf { it.size == 6 }?.let {
            LightCTLDefaultSet(
                lightness = it.getUShort(offset = 0, order = ByteOrder.LITTLE_ENDIAN),
                temperature = it.getUShort(offset = 2, order = ByteOrder.LITTLE_ENDIAN),
                deltaUV = it.getShort(offset = 4, order = ByteOrder.LITTLE_ENDIAN)
            )
        }
    }
}
