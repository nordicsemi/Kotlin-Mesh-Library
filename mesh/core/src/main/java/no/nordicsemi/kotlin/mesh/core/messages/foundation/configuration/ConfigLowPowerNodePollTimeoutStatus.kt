@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getUInt
import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import java.nio.ByteOrder
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The Config Low Power Node PollTimeout Status is an unacknowledged message used to report the
 * current value of the PollTimeout timer of the Low Power Node within a Friend Node.
 *
 * @property address              Address of the Low Power node.
 * @property pollTimeout          Poll timeout value.
 * @property pollTimeOutInterval  Poll time out interval. This is 24-bit value, where:
 *                                0x000000 -            The Node is no longer a Friend node of the
 *                                                      Low Power Node identified by the LPNAddress.
 *                                0x000001 - 0x000009 - Prohibited.
 *                                0x00000A - 0x34BBFF - The PollTimeout timer value in units of  100
 *                                                      milliseconds, which represents a range from
 *                                                      1 second to 3 days 23 hours 59 seconds 900
 *                                                      milliseconds.
 *                                0x34BC00 - 0xFFFFFF - Prohibited.
 */
class ConfigLowPowerNodePollTimeoutStatus(
    val address: Address,
    val pollTimeout: UInt,
) : ConfigResponse {
    override val opCode: UInt = Initializer.opCode
    override val parameters: ByteArray = address.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    val pollTimeOutInterval: Duration?
        get() = when {
            pollTimeout.toLong() in 0x00000A..0x34BBFF ->
                (pollTimeout.toLong() * 100L).milliseconds
            else -> null
        }

    /**
     * Constructs a [ConfigLowPowerNodePollTimeoutStatus] message for a given low power node address.
     *
     * @param address     The address of the Low Power node.
     * @param pollTimeout The PollTimeout value.
     */
    constructor(address: UnicastAddress, pollTimeout: UInt) : this(
        address = address.address,
        pollTimeout = pollTimeout
    )

    /**
     * Constructs a [ConfigLowPowerNodePollTimeoutStatus] message for a given low power node
     * address.
     *
     * @param request The [ConfigLowPowerNodePollTimeoutGet] request.
     */
    constructor(request: ConfigLowPowerNodePollTimeoutGet) : this(
        address = request.address,
        pollTimeout = 0u
    )

    @OptIn(ExperimentalStdlibApi::class)
    override fun toString() =
        "ConfigLowPowerNodePollTimeoutStatus(address: 0x${address.toHexString(HexFormat.UpperCase)})"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode = 0x802Eu

        /**
         * Initializes the [ConfigLowPowerNodePollTimeoutStatus] based on the given parameters.
         *
         * @param parameters Message parameters.
         * @return ConfigLowPowerNodePollTimeoutStatus or null if the parameters are invalid.
         */
        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 5 }
            ?.let {
                ConfigLowPowerNodePollTimeoutStatus(
                    address = it.getUShort(
                        offset = 0,
                        order = ByteOrder.LITTLE_ENDIAN
                    ),
                    // Extend the parameters by 1 byte and read pollTimeout as UInt.
                    // The parameters on Access Layer are encoded using Little Endian.
                    pollTimeout = (it + byteArrayOf(0)).getUInt(
                        offset = 2,
                        order = ByteOrder.LITTLE_ENDIAN
                    )
                )
            }
    }
}