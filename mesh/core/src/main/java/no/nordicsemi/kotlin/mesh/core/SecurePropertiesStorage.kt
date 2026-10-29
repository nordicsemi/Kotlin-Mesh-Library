@file:Suppress("unused")

package no.nordicsemi.kotlin.mesh.core

import no.nordicsemi.kotlin.mesh.core.model.IvIndex
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import kotlin.uuid.Uuid

/**
 * Storage class used to load and store generate sequence numbers for each Unicast Address.
 *
 * This is further extended with helper methods for handling Sequence Numbers of outgoing messages
 * from the local Node. Each message must contain a unique 24-bit Sequence Number, which together
 * with 32-bit IV Index ensure that replay attacks are not possible.
 * @see [SecurePropertiesStorage.nextSequenceNumber], [SecurePropertiesStorage.resetSequenceNumber]
 */
interface SecurePropertiesStorage {

    /**
     * Returns the IV Index for the network identified by the given Uuid.
     *
     * @param uuid Uuid of the mesh network.
     * @return IV Index.
     */
    suspend fun ivIndex(uuid: Uuid): IvIndex

    /**
     * Stores the IV Index for the network identified by the given Uuid.
     *
     * @param uuid     Uuid of the mesh network.
     * @param ivIndex  IV Index.
     */
    suspend fun storeIvIndex(uuid: Uuid, ivIndex: IvIndex)

    /**
     * Returns the next SEQ number to be used to send a message from the given Unicast Address. Each
     * time this method is called returned value is incremented by 1. Size of SEQ is 24 bits.
     *
     * @param uuid    Uuid of the mesh network.
     * @param address Unicast address of the node or the element.
     * @return next SEQ number to be used.
     */
    suspend fun nextSequenceNumber(uuid: Uuid, address: UnicastAddress): UInt

    /**
     * Stores the next sequence number to be used to send a message from the given Unicast Address.
     * Invoking [nextSequenceNumber] will return the incremented value that was stored by this
     * function.
     *
     * @param uuid           Uuid of the mesh network.
     * @param address        Unicast address of the node or the element.
     * @param sequenceNumber Sequence number to be stored.
     */
    suspend fun storeNextSequenceNumber(uuid: Uuid, address: UnicastAddress, sequenceNumber: UInt)

    /**
     * Resets the SEQ associated with all Elements of the given Node to 0.
     *
     * This method should be called when the IV Index is incremented and SEQ number should be reset.
     *
     * @param address Unicast address of the node or the element.
     */
    suspend fun resetSequenceNumber(uuid: Uuid, address: UnicastAddress)

    /**
     * Returns the last received SeqAuth value for the given source address or null if no message
     * has ever been received from the given source address.
     *
     * @param source Source address.
     * @return last SeqAuth value or null if no message has ever been received from the given source
     *         address.
     */
    suspend fun lastSeqAuthValue(uuid: Uuid, source: UnicastAddress): ULong?

    /**
     * Stores the last received SeqAuth value for the given source address.
     *
     * @param uuid        Uuid of the mesh network.
     * @param lastSeqAuth Last SeqAuth value.
     * @param source      Source address.
     */
    fun storeLastSeqAuthValue(uuid: Uuid, source: UnicastAddress, lastSeqAuth: ULong)

    /**
     * Returns the previous SeqAuth value for the given source address.
     *
     * @param uuid   Uuid of the mesh network.
     * @param source Source address
     * @return previous SeqAuth value or null if no more than 1 message has ever been received from
     *         the given source address.
     */
    suspend fun previousSeqAuthValue(uuid: Uuid, source: UnicastAddress): ULong?

    /**
     * Stores the previously received SeqAuth value for the given source address.
     *
     * @param uuid    Uuid of the mesh network.
     * @param seqAuth SeqAuth value.
     * @param source  Source address.
     */
    fun storePreviousSeqAuthValue(uuid: Uuid, source: UnicastAddress, seqAuth: ULong)

    /**
     * Stores the local provisioner for a given network. This can be used to restore the
     * previously selected provisioner.
     *
     * @param uuid                 Uuid of the network.
     * @param localProvisionerUuid Local provisioner Uuid.
     */
    suspend fun storeLocalProvisioner(uuid: Uuid, localProvisionerUuid: Uuid)

    /**
     * Returns the local provisioner uuid or null if the local provisioner does not exist.
     *
     * @param uuid Uuid of the Network.
     */
    suspend fun localProvisioner(uuid: Uuid): String?
}

