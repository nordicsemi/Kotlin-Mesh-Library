@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageInitializer
import no.nordicsemi.kotlin.mesh.core.messages.ConfigMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.ConfigNetKeyMessage
import no.nordicsemi.kotlin.mesh.core.messages.ConfigResponse
import no.nordicsemi.kotlin.mesh.core.messages.ConfigStatusMessage
import no.nordicsemi.kotlin.mesh.core.model.CountLog
import no.nordicsemi.kotlin.mesh.core.model.Feature
import no.nordicsemi.kotlin.mesh.core.model.Features
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatPublication
import no.nordicsemi.kotlin.mesh.core.model.HeartbeatPublicationDestination
import no.nordicsemi.kotlin.mesh.core.model.KeyIndex
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.RemainingHeartbeatPublicationCount
import no.nordicsemi.kotlin.mesh.core.model.UnassignedAddress
import no.nordicsemi.kotlin.mesh.core.model.toRemainingPublicationCount
import no.nordicsemi.kotlin.mesh.core.model.toUShort
import java.nio.ByteOrder

/**
 * This message contains the Heartbeat Publication status of an element. This is sent in response to
 * a [ConfigHeartbeatPublicationGet] or [ConfigHeartbeatPublicationSet] message.
 *
 * @param destination Destination address of the Heartbeat Publication.
 * @param countLog    Number of Heartbeat messages remaining to be sent.
 * @param periodLog   Period between publication of two consecutive periodic heartbeat transport
 *                    control messages.
 * @param ttl         TTL value used when sending Heartbeat messages.
 * @param features    Features that trigger Heartbeat messages.
 * @constructor Creates a ConfigHeartbeatPublicationStatus message.
 */
class ConfigHeartbeatPublicationStatus(
    override val status: ConfigMessageStatus = ConfigMessageStatus.SUCCESS,
    override val networkKeyIndex: KeyIndex = 0u,
    val destination: HeartbeatPublicationDestination = UnassignedAddress,
    val countLog: CountLog = 0x00u,
    val periodLog: UByte = 0x00u,
    val ttl: UByte = 0x00u,
    val features: List<Feature> = emptyList(),
) : ConfigResponse, ConfigStatusMessage, ConfigNetKeyMessage {
    override val opCode: UInt = Initializer.opCode

    override val parameters: ByteArray
        get() = byteArrayOf(status.value.toByte()) +
                destination.address.toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                countLog.toByte() +
                periodLog.toByte() +
                ttl.toByte() +
                features.toUShort().toByteArray(order = ByteOrder.LITTLE_ENDIAN) +
                networkKeyIndex.toByteArray(order = ByteOrder.LITTLE_ENDIAN)

    val count: RemainingHeartbeatPublicationCount
        get() = countLog.toRemainingPublicationCount()

    val isEnabled: Boolean
        get() = destination != UnassignedAddress

    val isPeriodicPublicationEnabled: Boolean
        get() = isEnabled && periodLog > 0u

    val isFeatureTriggeredPublishingEnabled: Boolean
        get() = isEnabled && features.isNotEmpty()

    /**
     * Convenience constructor to create the ConfigHeartbeatPublicationStatus message.
     *
     * @param publication [HeartbeatPublication] object that this is a response to.
     */
    constructor(publication: HeartbeatPublication?) : this(
        destination = publication?.address ?: UnassignedAddress,
        countLog = publication?.state?.countLog ?: 0u,
        periodLog = publication?.periodLog ?: 0u,
        ttl = publication?.ttl ?: 0u,
        features = publication?.features ?: emptyList(),
        networkKeyIndex = publication?.index ?: 0u
    )

    /**
     * Convenience constructor to create the ConfigHeartbeatPublicationStatus message.
     *
     * @param request [ConfigHeartbeatPublicationSet] message that this is a response to.
     * @param status  Status of the request.
     */
    constructor(request: ConfigHeartbeatPublicationSet, status: ConfigMessageStatus) : this(
        destination = request.destination,
        countLog = request.countLog,
        periodLog = request.periodLog,
        ttl = request.ttl,
        features = request.features,
        networkKeyIndex = request.networkKeyIndex,
        status = status
    )

    /**
     * Convenience constructor to create the ConfigHeartbeatPublicationStatus message.
     *
     * @param request [ConfigHeartbeatPublicationSet] message that this is a response to.
     */
    constructor(request: ConfigHeartbeatPublicationSet) : this(
        destination = request.destination,
        countLog = request.countLog,
        periodLog = request.periodLog,
        ttl = request.ttl,
        features = request.features,
        networkKeyIndex = request.networkKeyIndex
    )

    override fun toString() = "ConfigHeartbeatPublicationStatus(destination: $destination, " +
            "countLog: $countLog, periodLog: $periodLog, ttl: $ttl, features: ${
                features.joinToString()
            }, " + "networkKeyIndex: $networkKeyIndex, status: $status)"

    companion object Initializer : ConfigMessageInitializer {
        override val opCode: UInt = 0x06u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 10
        }?.let {
            ConfigMessageStatus.from(parameters[0].toUByte())?.let {
                ConfigHeartbeatPublicationStatus(
                    status = it,
                    destination = MeshAddress.create(
                        parameters.getUShort(offset = 1, order = ByteOrder.LITTLE_ENDIAN)
                    ) as HeartbeatPublicationDestination,
                    countLog = parameters[3].toUByte(),
                    periodLog = parameters[4].toUByte(),
                    ttl = parameters[5].toUByte(),
                    features = Features(
                        rawValue = parameters.getUShort(
                            offset = 6,
                            order = ByteOrder.LITTLE_ENDIAN
                        )
                    ).toList(),
                    networkKeyIndex = parameters.getUShort(
                        offset = 8,
                        order = ByteOrder.LITTLE_ENDIAN
                    )
                )
            }
        }
    }
}