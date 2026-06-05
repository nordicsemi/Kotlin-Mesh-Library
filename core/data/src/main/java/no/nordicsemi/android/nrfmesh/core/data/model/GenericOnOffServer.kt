package no.nordicsemi.android.nrfmesh.core.data.model

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.di.IoDispatcher
import no.nordicsemi.android.nrfmesh.core.data.storage.GenericOnOffStateStorage
import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.StoredWithSceneModelEventHandler
import no.nordicsemi.kotlin.mesh.core.TransactionHelper
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnOffGet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnOffSet
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnOffSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.generic.GenericOnOffStatus
import no.nordicsemi.kotlin.mesh.core.model.SceneNumber
import no.nordicsemi.kotlin.mesh.core.model.TransitionTime
import no.nordicsemi.kotlin.mesh.core.model.or
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration
import kotlin.uuid.ExperimentalUuidApi

class GenericOnOffServer(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val storage: GenericOnOffStateStorage,
    private val defaultTransitionTimeServer: GenericDefaultTransitionTimeServer,
) : StoredWithSceneModelEventHandler() {
    private val scope = CoroutineScope(context = SupervisorJob() + ioDispatcher)
    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        GenericOnOffGet.opCode to GenericOnOffGet.Initializer,
        GenericOnOffSet.opCode to GenericOnOffSet.Initializer,
        GenericOnOffSetUnacknowledged.opCode to GenericOnOffSetUnacknowledged.Initializer
    )
    override val isSubscriptionSupported = true
    @OptIn(ExperimentalTime::class)
    override val publicationMessageComposer: MessageComposer
        get() = {
            if (state.transition?.remainingTime?.let { it > 0.0.milliseconds } == true) {
                GenericOnOffStatus(
                    isOn = state.value,
                    targetState = state.transition!!.targetValue,
                    remainingTime = TransitionTime.init(state.transition!!.remainingTime)
                )
            } else {
                GenericOnOffStatus(state.value)
            }
        }

    @OptIn(ExperimentalTime::class)
    private var state: GenericState<Boolean> = GenericState(value = false)
        set(newValue) {
            if (!newValue.storedWithScene) {
                networkDidExitStoredWithSceneState(network = meshNetwork)
            }

            field = newValue

            state.transition?.takeIf { it.remainingTime > 0.milliseconds }?.let { transition ->
                scope.launch {
                    delay(duration = transition.remainingTime)
                    // If state hasn't changed since
                    if (state.transition?.start == transition.start) {
                        state = GenericState(state.transition?.targetValue ?: state.value)
                    }
                }
            }

            observer?.invoke(state)
        }
    private val transactionHelper = TransactionHelper()
    var observer: ((GenericState<Boolean>) -> Unit)? = null
        set(value) {
            field = value
            value?.invoke(state)
        }

    @OptIn(ExperimentalUuidApi::class)
    override fun store(scene: SceneNumber) {
        model.parentElement?.index?.let { index ->
            storage.storeGenericOnOffState(
                uuid = model.parentElement?.parentNode?.network?.uuid!!,
                sceneNumber = scene,
                elementIndex = index,
                on = state.value
            )
        }
    }

    @OptIn(ExperimentalUuidApi::class, ExperimentalTime::class)
    override fun recall(
        scene: SceneNumber,
        transitionTime: TransitionTime?,
        delay: UByte?,
    ) {
        model.parentElement?.index?.let { index ->
            scope.launch {
                val isOn = storage.readGenericOnOffState(
                    uuid = model.parentElement?.parentNode?.network?.uuid!!,
                    sceneNumber = scene,
                    elementIndex = index
                )
                when {
                    delay != null && transitionTime != null -> GenericState.transitionFrom(
                        transitionFrom = state,
                        to = isOn,
                        delay = (delay.toDouble() * 0.005).toDuration(DurationUnit.MILLISECONDS),
                        duration = transitionTime.interval
                    )

                    else -> state = GenericState(value = isOn, storedWithScene = true)
                }

            }
        }
    }

    override suspend fun handle(event: ModelEvent) = when (event) {
        is ModelEvent.AcknowledgedMessageReceived -> handleRequest(event = event)

        is ModelEvent.ResponseReceived -> throw ModelError.InvalidMessage(msg = event.request)

        is ModelEvent.UnacknowledgedMessageReceived -> {
            // The status message may be received here if the Generic OnOff Server model
            // has been configured to publish. Ignore this message.
            handleRequest(event = event)
            null
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun handleRequest(event: ModelEvent.AcknowledgedMessageReceived)
            : GenericOnOffStatus? {
        with(event) {
            when (val req = request) {
                is GenericOnOffSet -> {
                    // Ignore a repeated request (with the same TID) from the same source
                    // and sent to the same destination when it was received within 6 seconds.
                    require(
                        transactionHelper.isNewTransaction(
                            message = req,
                            source = source,
                            destination = destination
                        )
                    ) {
                        // Equivalent to Swift's `break` in switch-case context
                        return null
                    }

                    // Message execution delay in 5 millisecond steps. By default 0.
                    val delay = (req.delay ?: 0u).toDouble() * 0.005

                    // The time that an element will take to transition to the target
                    // state from the present state. If not set, use the default.
                    val transitionTime = req.transitionTime.or(
                        defaultTransitionTime = defaultTransitionTimeServer.defaultTransitionTime
                    )

                    // Start a new transition.
                    state = GenericState.transitionFrom(
                        transitionFrom = state,
                        to = req.on,
                        delay = delay.toDuration(DurationUnit.MILLISECONDS),
                        duration = transitionTime.interval
                    )
                }

                is GenericOnOffGet -> {
                    // No action needed, just fall through to response below.
                }

                else -> throw ModelError.InvalidMessage(msg = req)
            }

            val transition = state.transition
            return if (transition != null && transition.remainingTime > 0.milliseconds) {
                GenericOnOffStatus(
                    remainingTime = TransitionTime.init(duration = transition.remainingTime),
                    isOn = state.value,
                    targetState = transition.targetValue
                )
            } else {
                GenericOnOffStatus(isOn = state.value)
            }
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun handleRequest(event: ModelEvent.UnacknowledgedMessageReceived) {
        with(event) {
            when (val req = message) {
                is GenericOnOffSetUnacknowledged -> {
                    // Ignore a repeated request (with the same TID) from the same source
                    // and sent to the same destination when it was received within 6 seconds.
                    require(
                        transactionHelper.isNewTransaction(
                            message = req,
                            source = source,
                            destination = destination
                        )
                    ) {
                        // Equivalent to Swift's `break` in switch-case context
                        return
                    }

                    // Message execution delay in 5 millisecond steps. By default 0.
                    val delay = (req.delay ?: 0u).toDouble() * 0.005

                    // The time that an element will take to transition to the target
                    // state from the present state. If not set, use the default.
                    val transitionTime = req.transitionTime.or(
                        defaultTransitionTime = defaultTransitionTimeServer.defaultTransitionTime
                    )

                    // Start a new transition.
                    state = GenericState.transitionFrom(
                        transitionFrom = state,
                        to = req.on,
                        delay = delay.toDuration(DurationUnit.MILLISECONDS),
                        duration = transitionTime.interval
                    )
                }

                else -> throw ModelError.InvalidMessage(msg = req)
            }
        }
    }
}