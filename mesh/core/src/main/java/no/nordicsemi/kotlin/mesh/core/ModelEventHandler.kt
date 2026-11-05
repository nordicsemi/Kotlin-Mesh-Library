@file:Suppress("MemberVisibilityCanBePrivate", "unused", "PropertyName")

package no.nordicsemi.kotlin.mesh.core

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nordicsemi.kotlin.mesh.core.layers.MessageHandle
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.TransactionMessage
import no.nordicsemi.kotlin.mesh.core.messages.UnacknowledgedMeshMessage
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import kotlin.time.Instant

sealed class ModelError : Exception() {
    data class InvalidMessage(val msg: MeshMessage) : ModelError()
}

/**
 * A functional interface containing a message composer for an [UnacknowledgedMeshMessage].
 */
typealias MessageComposer = () -> UnacknowledgedMeshMessage

/**
 * Defines a set of events that are handled by the [ModelEventHandler].
 */
sealed class ModelEvent {

    /**
     * Event used to notify when an acknowledged message has been received from the mesh network.
     *
     * @property model          Model that received the message.
     * @property request        Request that was sent.
     * @property source         Address of the Element from which the message was sent.
     * @property destination    Address to which the message was sent.
     * @property reply          Lambda function to be invoked to respond to the received message.
     */
    data class AcknowledgedMessageReceived(
        val model: Model,
        val request: AcknowledgedMeshMessage,
        val source: Address,
        val destination: MeshAddress,
        // val reply: suspend (MeshResponse) -> Unit,
    ) : ModelEvent()

    /**
     * Event used to notify when an unacknowledged message has been received from the mesh network.
     *
     * @property model          Model that received the message.
     * @property message        Message that was received by the model.
     * @property source         Address of the Element from which the message was sent.
     * @property destination    Address to which the message was sent.
     */
    data class UnacknowledgedMessageReceived(
        val model: Model,
        val message: UnacknowledgedMeshMessage,
        val source: Address,
        val destination: MeshAddress,
    ) : ModelEvent()

    /**
     * Event used to notify when a response to a given request was received by the mesh network.
     *
     * @property model          Model that received the message.
     * @property response       Response that was received by the model.
     * @property request        Request that was sent.
     * @property source         Address of the Element from which the message was sent.
     */
    data class ResponseReceived(
        val model: Model,
        val response: MeshResponse,
        val request: AcknowledgedMeshMessage,
        val source: Address,
    ) : ModelEvent()
}

interface Publisher {
    /**
     * This method tries to publish the given message using the publication information set in the
     * [Model].
     *
     * If the retransmission is set to a value greater than 0, and the message is unacknowledged,
     * this method will retransmit it number of times with the count and interval specified in the
     * retransmission object.
     *
     * If the publication is not configured for the given Model, this method does nothing.
     *
     * Note: This method does not check whether the given Model does support the given message. It
     *       will publish whatever message is given using the publication configuration of the given
     *       Model.
     *
     * An appropriate callback of the ``MeshNetworkDelegate`` will be called when
     * the message has been sent successfully or a problem occurred.
     *
     * @param message: The message to be sent.
     * @param model:   The model from which to send the message.
     */
    fun publish(message: UnacknowledgedMeshMessage, model: Model)
}

/**
 * Defines the functionality of a [Model] on the Local Node.
 *
 * This event handler is assigned to the models when setting up the
 * [MeshNetworkManager.localElements].
 *
 * The event handler must declare a map of mesh message type supported by this Model. Whenever a
 * message matching any of the declared op codes is received, and the model is bound to an
 * Application Key used to encrypt the message. Upon receiving a message, the [handle] with
 * the [ModelEvent] will be invoked.
 *
 *
 * @property messageTypes                 Map of supported message types.
 * @property isSubscriptionSupported      Defines the model supports subscription.
 * @property publicationMessageComposer   Lambda function that returns a [MeshMessage] to be
 *                                        published.
 * @property meshNetwork                  Mesh network to which the model belongs.
 * @property model                        Model to which the event handler is assigned.
 * @property publisher                    Publisher used to publish messages.
 * @property mutex                        Mutex used to synchronize access to the model.
 */
abstract class ModelEventHandler {

    abstract val messageTypes: Map<UInt, HasInitializer>

    abstract val isSubscriptionSupported: Boolean

    abstract val publicationMessageComposer: MessageComposer?

    lateinit var meshNetwork: MeshNetwork
        internal set

    lateinit var model: Model
        internal set

    internal lateinit var publisher: Publisher

    internal val mutex = Mutex()

    /**
     * Publishes a single message created by Model's message composer using the Publish information
     * set in the underlying model.
     *
     * @return a nullable [MessageHandle] that can be used to cancel the message.
     */
    fun publish() = publicationMessageComposer?.let { composer ->
        publisher.publish(message = composer(), model = model)
    }

    /**
     * Invoked when a model event is published.
     *
     * @param event Model event.
     */
    @Throws(ModelError.InvalidMessage::class)
    abstract suspend fun handle(event: ModelEvent) : MeshResponse?
}

/**
 * This event handler should be used when defining Scene Server model. In addition to handling
 * messages, the Scene Server delegate should also clear the current whenever
 * [StoredWithSceneModelEventHandler.store] and
 * [StoredWithSceneModelEventHandler.recall] are called.
 *
 * Whenever the state changes due toa ny other reason than receiving a Scene Recall message, the
 * delegate should call [StoredWithSceneModelEventHandler.networkDidExitStoredWithSceneState] to clear
 * the current scene.
 */
abstract class SceneServerModelEventHandler : ModelEventHandler() {

    /**
     * This method should be called whenever the State of the model changes for any reason other
     * than receiving Scene Recall message.
     *
     * The invocation of this method should eb consumed by the Scene Server model, which should
     * clear the Current Scene.
     */
    abstract fun networkDidExitStoredWithSceneState()

    /**
     * Returns the event handlers of all the models on the local node that store their states with
     * a scene.
     */
    protected fun storedWithSceneHandlers() = meshNetwork.localElements
        .flatMap { it.models }
        .mapNotNull { it.eventHandler as? StoredWithSceneModelEventHandler }
}

abstract class StoredWithSceneModelEventHandler : ModelEventHandler() {

    /**
     * This method should store the current States of the Model and associate them with the given
     * Scene number.
     *
     * @param scene Scene number.
     */
    abstract fun store(scene: SceneNumber)

    /**
     * This method should recall the States of the Model that were stored with the given Scene
     * number.
     *
     * @param scene          Scene number.
     * @param transitionTime Identifies that an element will take to transition to the target state
     *                       from the present state.
     * @param delay          Message execution delay in 5 millisecond steps.
     */
    abstract fun recall(scene: SceneNumber, transitionTime: TransitionTime?, delay: UByte?)

    /**
     * This method should be called whenever the State of a local model changes due to a different
     * action than recalling a scene.
     *
     * This method will invalidate the current scene state in Scene Server model.
     *
     * @param network Mesh network to which the model belongs to.
     */
    fun networkDidExitStoredWithSceneState(network: MeshNetwork) {
        network.localElements
            .flatMap { it.models }
            .map { model -> model.eventHandler as? SceneServerModelEventHandler }
            .forEach { handler -> handler?.networkDidExitStoredWithSceneState() }
    }
}

/**
 * The Transaction is used for Transaction Messages, for example.
 *
 * @param source        Source address of the transaction.
 * @param destination   Destination address of the transaction.
 * @param tid           Transaction ID of the message.
 * @param timestamp     Timestamp of the last transaction message sent.
 */
private data class Transaction(
    val source: Address,
    val destination: MeshAddress,
    val tid: UByte,
    val timestamp: Instant,
)

class TransactionHelper {

    private val mutex = Mutex()

    private var lastTransactions = mutableMapOf<Address, Transaction>()

    /**
     * Checks whether the given Transaction Message was sent as a new transaction or is part of the
     * previously started transaction.
     *
     * @param message          Received message.
     * @param source           Source Unicast Address.
     * @param destination      destination address.
     * @return True, if the message starts a new transaction or false otherwise.
     */
    suspend fun isNewTransaction(
        message: TransactionMessage,
        source: Address,
        destination: MeshAddress,
    ) = mutex.withLock {
        val lastTransaction = lastTransactions[source]
        (lastTransaction == null) || (lastTransaction.source != source) ||
                (lastTransaction.destination != destination) ||
                message.isNewTransaction(
                    previousTid = lastTransaction.tid,
                    timestamp = lastTransaction.timestamp
                )
    }

    /**
     * Checks whether the given Transaction Message was sent as a continuation of the previous
     * transaction.
     *
     * @param message          Received message.
     * @param source           Source Unicast Address.
     * @param destination      destination address.
     * @return True, if the message continues the last transaction; false otherwise.
     */
    suspend fun isTransactionContinuation(
        message: TransactionMessage,
        source: Address,
        destination: MeshAddress,
    ) = !isNewTransaction(message, source, destination)
}
