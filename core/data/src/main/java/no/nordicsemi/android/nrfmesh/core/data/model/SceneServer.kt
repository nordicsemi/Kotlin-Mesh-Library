package no.nordicsemi.android.nrfmesh.core.data.model

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import no.nordicsemi.android.nrfmesh.core.common.di.IoDispatcher
import no.nordicsemi.android.nrfmesh.core.data.storage.SceneRegisterStorage
import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.SceneServerModelEventHandler
import no.nordicsemi.kotlin.mesh.core.StoredWithSceneModelEventHandler
import no.nordicsemi.kotlin.mesh.core.TransactionHelper
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRecall
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRecallUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRegisterGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneRegisterStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.scene.SceneStatusCode
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.model.or
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Scene Server model of the local node.
 *
 * The Scene Server keeps the Scene Register, which holds the numbers of the scenes stored on the
 * local node, and the number of the current scene. It handles recalling of the scenes. Storing and
 * deleting scenes is handled by the [SceneSetupServer], which extends this model.
 *
 * Recalling a scene is forwarded to all the models on the local node that extend
 * [StoredWithSceneModelEventHandler].
 */
@OptIn(ExperimentalTime::class)
class SceneServer(
    @IoDispatcher ioDispatcher: CoroutineDispatcher,
    private val storage: SceneRegisterStorage,
    private val defaultTransitionTimeServer: GenericDefaultTransitionTimeServer,
) : SceneServerModelEventHandler() {
    private val scope = CoroutineScope(context = SupervisorJob() + ioDispatcher)
    private val stateMutex = Mutex()
    private val transactionHelper = TransactionHelper()
    private var transitionJob: Job? = null
    private var isLoaded = false

    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        SceneGet.opCode to SceneGet,
        SceneRegisterGet.opCode to SceneRegisterGet,
        SceneRecall.opCode to SceneRecall,
        SceneRecallUnacknowledged.opCode to SceneRecallUnacknowledged,
    )
    override val isSubscriptionSupported = true
    override val publicationMessageComposer: MessageComposer
        get() = { statusMessage() }

    /**
     * Scenes stored in the Scene Register.
     */
    var storedScenes: List<SceneNumber> = emptyList()
        private set

    /**
     * Currently presenting scene or [NO_SCENE] if there is none.
     */
    var currentScene: SceneNumber = NO_SCENE
        internal set

    /**
     * Target scene and the time at which the transition to it completes.
     */
    private var targetScene: Pair<SceneNumber, Instant>? = null

    override fun networkDidExitStoredWithSceneState() {
        currentScene = NO_SCENE
    }

    override suspend fun handle(event: ModelEvent): MeshResponse? = when (event) {
        is ModelEvent.AcknowledgedMessageReceived -> handleRequest(event = event)

        is ModelEvent.ResponseReceived -> throw ModelError.InvalidMessage(msg = event.request)

        is ModelEvent.UnacknowledgedMessageReceived -> {
            handleRequest(event = event)
            null
        }
    }

    private suspend fun handleRequest(
        event: ModelEvent.AcknowledgedMessageReceived,
    ): MeshResponse = stateMutex.withLock {
            loadScenes()
            when (val request = event.request) {
                // When a Scene Server receives a Scene Register Get message, it shall respond with
                // a Scene Register Status message, setting the Status Code field to Success.
                is SceneRegisterGet -> SceneRegisterStatus(
                    status = SceneStatusCode.SUCCESS,
                    currentScene = currentScene,
                    scenes = storedScenes
                )

                is SceneRecall -> {
                    // Ignore a repeated request (with the same TID) from the same source and sent
                    // to the same destination when it was received within 6 seconds.
                    val isNewTransaction = transactionHelper.isNewTransaction(
                        message = request,
                        source = event.source,
                        destination = event.destination
                    )
                    (if (isNewTransaction) {
                        recall(
                            scene = request.sceneNumber,
                            transitionTime = request.transitionTime,
                            delay = request.delay
                        )
                    } else null) ?: statusMessage()
                }

                is SceneGet -> statusMessage()

                else -> throw ModelError.InvalidMessage(msg = request)
            }
        }

    private suspend fun handleRequest(event: ModelEvent.UnacknowledgedMessageReceived) {
        stateMutex.withLock {
            loadScenes()
            when (val message = event.message) {
                is SceneRecallUnacknowledged -> {
                    // Ignore a repeated request (with the same TID) from the same source and sent
                    // to the same destination when it was received within 6 seconds.
                    val isNewTransaction = transactionHelper.isNewTransaction(
                        message = message,
                        source = event.source,
                        destination = event.destination
                    )
                    if (isNewTransaction) {
                        recall(
                            scene = message.sceneNumber,
                            transitionTime = message.transitionTime,
                            delay = message.delay
                        )
                    }
                }

                // The status messages may be received here if the Scene Server model has been
                // configured to publish. Ignore them.
                is SceneStatus, is SceneRegisterStatus -> Unit

                else -> throw ModelError.InvalidMessage(msg = message)
            }
        }
    }

    /**
     * Recalls a scene.
     *
     * @return a [SceneStatus] if the scene was not recalled or null if the recall was started.
     */
    private fun recall(
        scene: SceneNumber,
        transitionTime: TransitionTime?,
        delay: UByte?,
    ): SceneStatus? {
        // When a Scene Server receives a Scene Recall message with a Scene Number value that does
        // not match a Scene Number stored within the Scene Register state, it shall respond with
        // the Scene Status message, setting the Status Code field to Scene Not Found.
        if (scene !in storedScenes) {
            return SceneStatus(status = SceneStatusCode.SCENE_NOT_FOUND, currentScene = currentScene)
        }
        // If the target state is equal to the current state, the transition shall not be started
        // and is considered complete.
        if (scene == currentScene) {
            return SceneStatus(status = SceneStatusCode.SUCCESS, currentScene = currentScene)
        }

        // The time that an element will take to transition to the target state from the present
        // state. If not set, the default transition time is used.
        val transition = transitionTime.or(
            defaultTransitionTime = defaultTransitionTimeServer.defaultTransitionTime
        )
        // Message execution delay in 5 millisecond steps. By default 0.
        val executionDelay = delay ?: 0u

        transitionJob?.cancel()
        targetScene = null
        val duration = transition.duration
        if (duration == Duration.ZERO) {
            currentScene = scene
        } else {
            val complete = Clock.System.now() + duration + (executionDelay.toInt() * 5).milliseconds
            targetScene = scene to complete
            currentScene = NO_SCENE
            transitionJob = scope.launch {
                delay(complete - Clock.System.now())
                stateMutex.withLock {
                    if (targetScene?.second == complete) {
                        currentScene = scene
                        targetScene = null
                    }
                }
            }
        }

        // Recall the scene on all the models that support scenes.
        storedWithSceneHandlers().forEach { it.recall(scene, transition, executionDelay) }
        return null
    }

    /**
     * Stores the states of all models that support scenes with the given scene number and makes
     * it the current scene.
     *
     * @param scene Scene number.
     * @return false if the Scene Register is full, true otherwise.
     */
    internal suspend fun store(scene: SceneNumber) = stateMutex.withLock {
        loadScenes()
        // Overwriting an existing scene is allowed.
        if (scene !in storedScenes) {
            if (storedScenes.size >= MAX_SCENES) return@withLock false
            storedScenes = (storedScenes + scene).sorted()
            storage.storeScene(uuid = meshNetwork.uuid, sceneNumber = scene)
        }
        transitionJob?.cancel()
        targetScene = null
        currentScene = scene

        // Store the scene on all the models that support scenes.
        storedWithSceneHandlers().forEach { it.store(scene) }
        true
    }

    /**
     * Removes a scene from the Scene Register.
     *
     * @param scene Scene number.
     * @return false if the scene was not found, true otherwise.
     */
    internal suspend fun delete(scene: SceneNumber) = stateMutex.withLock {
        loadScenes()
        if (scene !in storedScenes) return@withLock false
        storedScenes = storedScenes - scene
        storage.deleteScene(uuid = meshNetwork.uuid, sceneNumber = scene)
        if (currentScene == scene) currentScene = NO_SCENE
        if (targetScene?.first == scene) {
            transitionJob?.cancel()
            targetScene = null
        }
        true
    }

    /**
     * Returns the Scene Register Status reporting the current state of the Scene Register.
     */
    internal suspend fun registerStatus(status: SceneStatusCode = SceneStatusCode.SUCCESS) =
        stateMutex.withLock {
            SceneRegisterStatus(
                status = status,
                currentScene = currentScene,
                scenes = storedScenes
            )
        }

    private suspend fun loadScenes() {
        if (!isLoaded) {
            storedScenes = storage.readScenes(uuid = meshNetwork.uuid)
            isLoaded = true
        }
    }

    private fun statusMessage(): SceneStatus {
        val target = targetScene
        val remaining = target?.second?.minus(Clock.System.now())
        return if (target != null && remaining != null && remaining > Duration.ZERO) {
            // While a transition is in progress the current scene is reported as 0.
            SceneStatus(
                status = SceneStatusCode.SUCCESS,
                currentScene = NO_SCENE,
                targetScene = target.first,
                remainingTime = TransitionTime.init(duration = remaining)
            )
        } else {
            SceneStatus(status = SceneStatusCode.SUCCESS, currentScene = currentScene)
        }
    }

    private val TransitionTime.duration: Duration
        get() = if (isKnown) stepResolution.toMilliseconds(steps) else Duration.ZERO

    companion object {
        /**
         * Value of the current scene when no scene is active.
         */
        const val NO_SCENE: SceneNumber = 0x0000u

        /**
         * Maximum number of scenes in the Scene Register.
         */
        const val MAX_SCENES = 16
    }
}
