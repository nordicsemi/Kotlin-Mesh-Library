package no.nordicsemi.android.nrfmesh.core.data.model

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.di.IoDispatcher
import no.nordicsemi.android.nrfmesh.core.data.storage.GenericLevelStateStorage
import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.StoredWithSceneModelEventHandler
import no.nordicsemi.kotlin.mesh.core.TransactionHelper
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.TransactionMessage
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDeltaSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericDeltaSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericLevelStatus
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericMoveSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericMoveSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.model.Address
import no.nordicsemi.kotlin.mesh.core.model.MeshAddress
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.model.or
import kotlin.time.Clock.System
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit

/**
 * Generic Level Server model event handler for the local node.
 */
class GenericLevelServer(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val storage: GenericLevelStateStorage,
    private val defaultTransitionTimeServer: GenericDefaultTransitionTimeServer,
) : StoredWithSceneModelEventHandler() {
    private val scope = CoroutineScope(context = SupervisorJob() + ioDispatcher)
    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        GenericLevelGet.opCode to GenericLevelGet.Initializer,
        GenericLevelSet.opCode to GenericLevelSet.Initializer,
        GenericLevelSetUnacknowledged.opCode to GenericLevelSetUnacknowledged.Initializer,
        GenericDeltaSet.opCode to GenericDeltaSet.Initializer,
        GenericDeltaSetUnacknowledged.opCode to GenericDeltaSetUnacknowledged.Initializer,
        GenericMoveSet.opCode to GenericMoveSet.Initializer,
        GenericMoveSetUnacknowledged.opCode to GenericMoveSetUnacknowledged.Initializer
    )
    override val isSubscriptionSupported = true
    override val publicationMessageComposer: MessageComposer
        get() = { status() }

    private var state: GenericState<Short> = GenericState(value = Short.MIN_VALUE)
        set(newValue) {
            if (!newValue.storedWithScene) {
                networkDidExitStoredWithSceneState(network = meshNetwork)
            }
            field = newValue

            newValue.transition?.takeIf { it.remainingTime > 0.milliseconds }?.let { transition ->
                scope.launch {
                    delay(duration = transition.remainingTime)
                    // If the state hasn't changed since, remove the transition.
                    if (state.transition?.start == transition.start) {
                        state = GenericState(
                            value = state.transition?.targetValue ?: state.value,
                            storedWithScene = state.storedWithScene
                        )
                    }
                }
            }
            observer?.invoke(newValue)
        }
    private val transactionHelper = TransactionHelper()
    var observer: ((GenericState<Short>) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(state)
        }

    override fun store(scene: SceneNumber) {
        model.parentElement?.index?.let { index ->
            storage.storeGenericLevelState(
                uuid = model.parentElement?.parentNode?.network?.uuid!!,
                sceneNumber = scene,
                elementIndex = index,
                level = state.value.toUShort()
            )
        }
    }

    override fun recall(scene: SceneNumber, transitionTime: TransitionTime?, delay: UByte?) {
        model.parentElement?.index?.let { index ->
            scope.launch {
                val level = storage.readGenericLevelState(
                    uuid = model.parentElement?.parentNode?.network?.uuid!!,
                    sceneNumber = scene,
                    elementIndex = index
                )?.toShort() ?: return@launch // Nothing stored for this scene.
                state = when {
                    delay != null && transitionTime != null -> GenericState.transitionFrom(
                        transitionFrom = state.withCurrentValue(),
                        to = level,
                        delay = (delay.toInt() * 5).milliseconds,
                        duration = transitionTime.interval,
                        storedWithScene = true
                    )

                    else -> GenericState(value = level, storedWithScene = true)
                }
            }
        }
    }

    override suspend fun handle(event: ModelEvent) = when (event) {
        is ModelEvent.AcknowledgedMessageReceived -> handleRequest(event = event)

        is ModelEvent.ResponseReceived -> throw ModelError.InvalidMessage(msg = event.request)

        is ModelEvent.UnacknowledgedMessageReceived -> {
            handleRequest(event = event)
            null
        }
    }

    private suspend fun handleRequest(event: ModelEvent.AcknowledgedMessageReceived)
            : GenericLevelStatus {
        with(event) {
            when (val req = request) {
                is GenericLevelSet -> if (isNew(req, source, destination)) {
                    setLevel(req.level, req.delay, req.transitionTime)
                }

                is GenericDeltaSet -> delta(
                    message = req,
                    delta = req.delta,
                    delay = req.delay,
                    transitionTime = req.transitionTime,
                    source = source,
                    destination = destination
                )

                is GenericMoveSet -> if (isNew(req, source, destination)) {
                    move(
                        deltaLevel = req.deltaLevel,
                        delay = req.delay,
                        transitionTime = req.transitionTime
                    )
                }

                is GenericLevelGet -> {
                    // No action needed, just respond with the current state.
                }

                else -> throw ModelError.InvalidMessage(msg = req)
            }
            return status()
        }
    }

    private suspend fun handleRequest(event: ModelEvent.UnacknowledgedMessageReceived) {
        with(event) {
            when (val req = message) {
                is GenericLevelSetUnacknowledged -> if (isNew(req, source, destination)) {
                    setLevel(
                        level = req.level,
                        delay = req.delay,
                        transitionTime = req.transitionTime
                    )
                }

                is GenericDeltaSetUnacknowledged -> delta(
                    message = req,
                    delta = req.delta,
                    delay = req.delay,
                    transitionTime = req.transitionTime,
                    source = source,
                    destination = destination
                )

                is GenericMoveSetUnacknowledged -> if (isNew(req, source, destination)) {
                    move(
                        deltaLevel = req.deltaLevel,
                        delay = req.delay,
                        transitionTime = req.transitionTime
                    )
                }

                else -> throw ModelError.InvalidMessage(msg = req)
            }
        }
    }

    private suspend fun isNew(
        message: TransactionMessage,
        source: Address,
        destination: MeshAddress,
    ) = transactionHelper.isNewTransaction(
        message = message,
        source = source,
        destination = destination
    )

    private fun setLevel(level: Short, delay: UByte?, transitionTime: TransitionTime?) {
        state = GenericState.transitionFrom(
            transitionFrom = state.withCurrentValue(),
            to = level,
            delay = delay.toDuration(),
            duration = transitionTime.or(defaultTransitionTimeServer.defaultTransitionTime).interval
        )
    }

    private suspend fun delta(
        message: TransactionMessage,
        delta: Int,
        delay: UByte?,
        transitionTime: TransitionTime?,
        source: Address,
        destination: MeshAddress,
    ) {
        val targetLevel = (state.value.toInt() + delta).toShort()
        val continuation = transactionHelper.isTransactionContinuation(
            message = message,
            source = source,
            destination = destination
        )
        val duration = transitionTime.or(defaultTransitionTimeServer.defaultTransitionTime).interval
        val current = state.withCurrentValue()
        state = if (continuation && state.transition?.remainingTime?.let { it > Duration.ZERO } == true) {
            // Continue the same transition.
            GenericState.continueTransitionFrom(
                state = current,
                targetValue = targetLevel,
                delay = delay.toDuration(),
                duration = duration
            )
        } else {
            GenericState.transitionFrom(
                transitionFrom = current,
                to = targetLevel,
                delay = delay.toDuration(),
                duration = duration
            )
        }
    }

    private fun move(deltaLevel: Short, delay: UByte?, transitionTime: TransitionTime?) {
        val duration = transitionTime.or(defaultTransitionTimeServer.defaultTransitionTime).interval
        val current = state.withCurrentValue()
        state = if (duration == null || deltaLevel.toInt() == 0 || duration <= Duration.ZERO) {
            current.copy(transition = null, animation = null)
        } else {
            // Change of value per second.
            val speed = deltaLevel / duration.toDouble(DurationUnit.SECONDS)
            current.copy(
                transition = null,
                animation = GenericState.Move(
                    start = System.now(),
                    delay = delay.toDuration(),
                    speed = speed.toInt().toShort()
                )
            )
        }
    }

    private fun status() = state.transition
        ?.takeIf { it.remainingTime > 0.milliseconds }
        ?.let {
            GenericLevelStatus(
                level = state.value,
                targetLevel = it.targetValue,
                remainingTime = TransitionTime.init(duration = it.remainingTime)
            )
        } ?: GenericLevelStatus(level = state.value)

    /** Delay in 5 millisecond steps. */
    private fun UByte?.toDuration() = ((this ?: 0u).toInt() * 5).milliseconds

    /**
     * Returns the state with its value set to the level at this moment, taking an ongoing
     * transition or move into account.
     */
    private fun GenericState<Short>.withCurrentValue(): GenericState<Short> {
        animation?.let { anim ->
            val sinceStart = System.now() - anim.startTime
            return if (sinceStart <= Duration.ZERO) this else copy(
                value = (value + sinceStart.toDouble(DurationUnit.SECONDS) * anim.speed)
                    .toInt().toShort()
            )
        }
        transition?.let { trans ->
            val sinceStart = System.now() - trans.startTime
            return when {
                sinceStart <= Duration.ZERO -> this
                trans.remainingTime == Duration.ZERO -> copy(value = trans.targetValue)
                else -> {
                    val progress = trans.remainingTime / trans.duration
                    val diff = value - trans.targetValue
                    copy(value = (trans.targetValue + (diff * progress).toInt()).toShort())
                }
            }
        }
        return this
    }
}
