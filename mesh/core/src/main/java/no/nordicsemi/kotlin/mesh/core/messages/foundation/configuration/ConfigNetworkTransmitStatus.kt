package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.shl
import no.nordicsemi.kotlin.data.ushr
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.model.NetworkTransmit
import no.nordicsemi.kotlin.mesh.core.model.Node
import kotlin.experimental.and
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * This message is the status message sent as a response to [ConfigNetworkTransmitGet] and
 * [ConfigNetworkTransmitSet].
 *
 * @property count    Number of transmissions of Network PDU originating from the node. Possible
 *                    value are 0...7, which correspond to 1 - 8 transmissions in total.
 * @property steps    Number of 10-millisecond steps between transmissions, decremented by 1.
 *                    Possible values are 0...31, which correspond to 10 ms to 320 ms in 10 ms
 *                    steps.
 */
@Suppress("MemberVisibilityCanBePrivate")
class ConfigNetworkTransmitStatus(val count: UByte, val steps: UByte) : ConfigResponse {
    override val opCode = Initializer.opCode
    override val parameters = byteArrayOf(((count and 0x07.toUByte()) or (steps shl 3)).toByte())

    /**
     * Interval between transmissions.
     */
    val interval: Duration = ((steps + 1u).toInt() * 10).milliseconds

    /**
     * Convenience constructor.
     *
     * @param count    Number of transmissions of Network PDU originating from the node.
     *                 This must be in range 1 - 8 transmissions.
     * @param interval Interval between transmissions, in milliseconds.
     *                 This must be in range 10 - 320 milliseconds, in 10 ms steps.
     */
    constructor(count: Int, interval: Int) : this(
        count = (count - 1).toUByte(),
        steps = ((interval / 10) - 1).toUByte(),
    )

    /**
     * Convenience constructor.
     *
     * @param networkTransmit Network Transmit to report.
     */
    constructor(networkTransmit: NetworkTransmit) : this(networkTransmit.count, networkTransmit.interval)

    /**
     * Convenience constructor.
     *
     * @param node The Node, which Network Transmit is to be reported.
     */
    constructor(node: Node) : this(
        count = node.networkTransmit?.count ?: 1,
        interval = node.networkTransmit?.interval ?: 10
    )

    init {
        require(count in 0u..7u) { "Count must be in range 0..7" }
        require(steps in 0u..31u) { "Steps must be in range 0..31" }
    }

    override fun toString() = "ConfigNetworkTransmitStatus(count: $count, steps: $steps)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x8025u

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 1 }
            ?.let { params ->
                ConfigNetworkTransmitStatus(
                    count = (params[0] and 0x07).toUByte(),
                    steps = (params[0] ushr 3).toUByte()
                )
            }
    }
}