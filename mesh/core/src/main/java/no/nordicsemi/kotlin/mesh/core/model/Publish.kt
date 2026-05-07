@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.serialization.Serializable
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigModelPublicationSet

/**
 * The publish object represents parameters that define how the messages are published by a mesh
 * model.
 *
 * @property address                               The address property contains the publication
 *                                                 address for the model containing a [Address].
 * @property index                                 The index property contains an integer that
 *                                                 represents an [ApplicationKey] index, indicating
 *                                                 which Application Key to use for the publication.
 *                                                 The application key index corresponds to the
 *                                                 index value of one of the application key entries
 *                                                 in the node’s appKeys array.
 * @property ttl                                   The ttl property contains an integer from 0 to
 *                                                 127 that represents the Time to Live (TTL) value
 *                                                 for published messages or an integer with a value
 *                                                 of 255 that indicates that the node’s default TTL
 *                                                 is used for publication.
 * @property period                                The period property refers to [PublishPeriod]
 *                                                 that describes the interval between subsequent
 *                                                 publications. If the value of this property is 0,
 *                                                 the periodic publication is disabled.
 * @property credentials                           The [Credentials] property contains an integer of
 *                                                 0 or 1 that represents whether managed flooding
 *                                                 security material (0) or friendship security
 *                                                 material (1) is used.
 * @property retransmit                            The [Retransmit] property describes the number of
 *                                                 times a message is published and the interval
 *                                                 between retransmissions of the published messages.
 * @property isCanceled                            Returns true if the publication is canceled.
 * @property isUsingMasterSecurityMaterial         Returns whether master security materials are
 *                                                 used.
 * @property isUsingFriendshipSecurityMaterial     Returns whether friendship security materials are
 *                                                 used.
 */
@Serializable
data class Publish(
    val address: PublicationAddress,
    val index: KeyIndex,
    val ttl: UByte,
    val period: PublishPeriod,
    val credentials: Credentials,
    val retransmit: Retransmit,
) {
    val isCanceled: Boolean
        get() = address is UnassignedAddress

    // Returns whether master security materials are used.
    val isUsingMasterSecurityMaterial: Boolean
        get() = credentials == MasterSecurity

    // Returns whether friendship security materials are used.
    val isUsingFriendshipSecurityMaterial: Boolean
        get() = credentials == FriendshipSecurity

    /**
     * Publish constructor to be used when sending a [ConfigModelPublicationSet] to disable
     * publications.
     */
    constructor() : this(
        address = UnassignedAddress,
        index = 0.toUShort(),
        ttl = 0.toUByte(),
        period = PublishPeriod.disabled,
        credentials = MasterSecurity,
        retransmit = Retransmit.disabled
    )

    override fun toString() = when (address) {
        is UnassignedAddress -> "Disabled"
        else -> "Publish(address: ${address.toHexString()}, applicationKeyIndex: $index, " +
                "ttl: $ttl, period: $period, " +
                "credentials: $credentials, retransmit: $retransmit)"
    }
}