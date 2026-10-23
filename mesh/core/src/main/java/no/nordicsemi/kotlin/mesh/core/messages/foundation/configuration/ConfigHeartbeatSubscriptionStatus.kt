@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.ConfigStatusMessage
import no.nordicsemi.kotlin.mesh.core.model.CountLog
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatSubscription
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatSubscriptionCount
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatSubscriptionDestination
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatSubscriptionSource
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.PeriodLog
import no.nordicsemi.kotlin.mesh.core.model.RemainingHeartbeatSubscriptionPeriod
import no.nordicsemi.kotlin.mesh.core.model.UnassignedAddress
import no.nordicsemi.kotlin.mesh.core.model.toHeartbeatSubscriptionCount
import no.nordicsemi.kotlin.mesh.core.model.toRemainingHeartbeatSubscriptionPeriod
import no.nordicsemi.kotlin.data.ByteOrder

/**
 * This message contains the Heartbeat Subscription status of an element. This is sent in response
 * to a [ConfigHeartbeatPublicationGet] or [ConfigHeartbeatPublicationSet] message.
 *
 * @property status         Status of the message.
 * @property source         Source address for Heartbeat messages.
 * @property destination    Destination address for Heartbeat messages.
 * @property periodLog      Remaining Period for processing Heartbeat messages.
 * @property countLog       Number of Heartbeat messages received.
 * @property minHops        Minimum hops for Heartbeat messages.
 * @property maxHops        Maximum hops for Heartbeat messages.
 * @property isEnabled      Returns true if the processing of Heartbeat subscription is enabled.
 * @property isComplete     Returns true if the subscription is complete.
 * @constructor Creates a ConfigHeartbeatPublicationStatus message.
 */
class ConfigHeartbeatSubscriptionStatus(
    override val status: ConfigMessageStatus,
    val source: HeartbeatSubscriptionSource,
    val destination: HeartbeatSubscriptionDestination,
    val periodLog: PeriodLog,
    val countLog: CountLog,
    val minHops: UByte,
    val maxHops: UByte,
) : ConfigResponse, ConfigStatusMessage {
    override val opCode: UInt = Initializer.opCode

    override val parameters: ByteArray = status.value.toByteArray() +
            source.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            destination.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
            periodLog.toByte() +
            countLog.toByte() +
            minHops.toByte() +
            maxHops.toByte()

    val count: HeartbeatSubscriptionCount
        get() = countLog.toHeartbeatSubscriptionCount()

    val period: RemainingHeartbeatSubscriptionPeriod
        get() = periodLog.toRemainingHeartbeatSubscriptionPeriod()

    val isEnabled: Boolean
        get() = source != UnassignedAddress && destination != UnassignedAddress

    val isComplete: Boolean
        get() = isEnabled && periodLog != 0.toUByte()

    /**
     * Convenience constructor to either enable or disable the subscription. Passing null will
     * disable the heartbeat subscription.
     *
     * @param subscription Heartbeat subscription
     */
    constructor(subscription: HeartbeatSubscription? = null) : this(
        source = subscription?.source ?: UnassignedAddress,
        destination = subscription?.destination ?: UnassignedAddress,
        countLog = subscription?.state?.countLog ?: 0u,
        periodLog = subscription?.state?.periodLog ?: 0u,
        minHops = subscription?.state?.minHops ?: 0u,
        maxHops = subscription?.state?.maxHops ?: 0u,
        status = ConfigMessageStatus.SUCCESS
    )

    /**
     * Convenience constructor to create the ConfigHeartbeatSubscriptionStatus message.
     *
     * @param request The [ConfigHeartbeatSubscriptionSet] message that this is a response to.
     * @param status  Status of the request.
     */
    constructor(request: ConfigHeartbeatSubscriptionSet, status: ConfigMessageStatus) : this(
        status = status,
        source = request.source,
        destination = request.destination,
        periodLog = request.periodLog,
        countLog = 0u,
        minHops = 0x7Fu,
        maxHops = 0x00u
    )

    override fun toString(): String {
        return "ConfigHeartbeatSubscriptionStatus(state: $status,  source: 0x${source.toHexString()}, " +
                "destination: 0x${destination.toHexString()}, periodLog: $periodLog, countLog: $countLog, " +
                "minHops: $minHops, maxHops: $maxHops)"
    }

    companion object Initializer : ConfigMessageInitializer {
        override val opCode: UInt = 0x803Cu

        override fun init(parameters: ByteArray?) = parameters
            ?.takeIf { it.size == 9 }
            ?.let {
                ConfigMessageStatus.from(it.first().toUByte())?.let { status ->
                    ConfigHeartbeatSubscriptionStatus(
                        status = status,
                        source = MeshAddress.create(
                            address = it.getUShort(offset = 1, order = ByteOrder.LITTLE_ENDIAN)
                        ) as HeartbeatSubscriptionSource,
                        destination = MeshAddress.create(
                            address = it.getUShort(offset = 3, order = ByteOrder.LITTLE_ENDIAN)
                        ) as HeartbeatSubscriptionDestination,
                        periodLog = it[5].toUByte(),
                        countLog = it[6].toUByte(),
                        minHops = it[7].toUByte(),
                        maxHops = it[8].toUByte()
                    )
                }
            }
    }
}