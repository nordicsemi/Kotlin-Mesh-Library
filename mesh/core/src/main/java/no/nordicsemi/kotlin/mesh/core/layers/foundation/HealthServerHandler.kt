package no.nordicsemi.kotlin.mesh.core.layers.foundation

import no.nordicsemi.kotlin.mesh.core.MessageComposer
import no.nordicsemi.kotlin.mesh.core.ModelError
import no.nordicsemi.kotlin.mesh.core.ModelEvent
import no.nordicsemi.kotlin.mesh.core.ModelEventHandler
import no.nordicsemi.kotlin.mesh.core.messages.HasInitializer
import no.nordicsemi.kotlin.mesh.core.messages.MeshResponse
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionGet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionSet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionStatus
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthAttentionTimer
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFault
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultClear
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultClearUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultGet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultStatus
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultTest
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthFaultTestUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthPeriodGet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthPeriodSet
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthPeriodSetUnacknowledged
import no.nordicsemi.kotlin.mesh.core.messages.health.HealthPeriodStatus
import no.nordicsemi.kotlin.mesh.logger.LogCategory
import no.nordicsemi.kotlin.mesh.logger.Logger
import java.util.Timer
import java.util.TimerTask
import kotlin.concurrent.schedule
import kotlin.time.Duration

@Suppress("ConvertArgumentToSet")
class HealthServerHandler(
    val triggerAttentionTimer: (HealthAttentionTimer) -> Unit,
    val logger: Logger? = null,
) : ModelEventHandler() {
    override val messageTypes: Map<UInt, HasInitializer> = mapOf(
        HealthFaultGet.opCode to HealthFaultGet,
        HealthFaultClear.opCode to HealthFaultClear,
        HealthFaultClearUnacknowledged.opCode to HealthFaultClearUnacknowledged,
        HealthFaultTest.opCode to HealthFaultTest,
        HealthFaultTestUnacknowledged.opCode to HealthFaultTestUnacknowledged,
        HealthAttentionGet.opCode to HealthAttentionGet,
        HealthAttentionSet.opCode to HealthAttentionSet,
        HealthAttentionSetUnacknowledged.opCode to HealthAttentionSetUnacknowledged,
        HealthPeriodGet.opCode to HealthPeriodGet,
        HealthPeriodSet.opCode to HealthPeriodSet,
        HealthPeriodSetUnacknowledged.opCode to HealthPeriodSetUnacknowledged,
    )
    override val isSubscriptionSupported: Boolean = false
    override val publicationMessageComposer: MessageComposer? = null
    private var mostRecentTestId: UByte = 0u
    private var remainingTime: Duration = Duration.ZERO
    private var attentionTimer: Timer = Timer()
    private var attentionTimerTask: TimerTask? = null
    private var currentFaultState = listOf<HealthFault>()
        set(value) {
            field = value
            publish()
            // Whenever a fault condition has been present in the
            // Current Fault state, the corresponding record is added
            // to the Registered Fault state.
            if (value.isEmpty()) {
                triggerAttentionTimer(HealthAttentionTimer.Stop)
            }
        }
    private var registeredFaultState = listOf<HealthFault>()
    private val nordicSemiconductor: UShort = 0x0059u
    private val companyIdentifier: UShort
        get() = meshNetwork.localProvisioner?.node?.companyIdentifier ?: nordicSemiconductor

    override suspend fun handle(event: ModelEvent): MeshResponse? = when (event) {
        is ModelEvent.AcknowledgedMessageReceived -> handleRequest(event = event)

        is ModelEvent.ResponseReceived -> throw ModelError.InvalidMessage(msg = event.request)

        is ModelEvent.UnacknowledgedMessageReceived -> {
            handleRequest(event = event)
            null
        }
    }

    private fun handleRequest(event: ModelEvent.AcknowledgedMessageReceived) : MeshResponse? =
        when (val request = event.request) {
            is HealthAttentionGet -> HealthAttentionStatus(duration = remainingTime)
            is HealthAttentionSet -> {
                invalidateTimer()
                remainingTime = Duration.ZERO
                val duration = request.duration
                if (duration > Duration.ZERO) {
                    triggerAttentionTimer(HealthAttentionTimer.Start(duration = duration))
                    attentionTimerTask = attentionTimer
                        .schedule(delay = duration.inWholeMilliseconds) {
                            invalidateTimer()
                            triggerAttentionTimer(HealthAttentionTimer.Stop)
                        }
                } else {
                    invalidateTimer()
                    triggerAttentionTimer(HealthAttentionTimer.Stop)
                }
                HealthAttentionStatus(duration = duration)
            }

            is HealthPeriodGet,
            is HealthPeriodSet,
                -> {
                // This library does not support Fast Period Divisor.
                // Value 0 means, that the publishing will be using
                // Publish Period without any divisor.
                HealthPeriodStatus(fastPeriodDivisor = 0u)
            }

            is HealthFaultGet -> {
                require(companyIdentifier == request.companyIdentifier) {
                    logger?.e(category = LogCategory.FOUNDATION_MODEL) {
                        "Company Identifier mismatch"
                    }
                    throw ModelError.InvalidMessage(msg = request)
                }
                HealthFaultStatus(
                    testId = mostRecentTestId,
                    companyIdentifier = companyIdentifier,
                    faults = registeredFaultState.sortedBy { it.code }
                )
            }

            is HealthFaultTest -> {
                // This code allows testing Faults.
                // When HealthFaultTest is sent with Company ID = Nordic Semiconductor (0059),
                // and the "testID" is grater than 0, the fault with the ID equal to the testID
                // is added to the Registered Fault state.
                if (request.companyIdentifier == nordicSemiconductor) {
                    if (request.testId > 0u) {
                        val fault = HealthFault.from(code = request.testId)
                        currentFaultState = currentFaultState + fault
                    } else {
                        currentFaultState = currentFaultState - currentFaultState
                    }
                } else {
                    // When the company ID isn't Nordic, check that it matches the CID of the Node.
                    require(request.companyIdentifier == companyIdentifier) {
                        logger?.e(category = LogCategory.FOUNDATION_MODEL) {
                            "Company Identifier mismatch"
                        }
                        throw ModelError.InvalidMessage(msg = request)
                    }
                }
                mostRecentTestId = request.testId
                HealthFaultStatus(
                    testId = mostRecentTestId,
                    companyIdentifier = companyIdentifier,
                    faults = registeredFaultState.sortedBy { it.code }
                )
            }

            is HealthFaultClear -> {
                require(request.companyIdentifier == companyIdentifier) {
                    logger?.e(category = LogCategory.FOUNDATION_MODEL) {
                        "Company Identifier mismatch"
                    }
                    throw ModelError.InvalidMessage(msg = request)
                }
                registeredFaultState = registeredFaultState - registeredFaultState
                HealthFaultStatus(
                    testId = mostRecentTestId,
                    companyIdentifier = companyIdentifier
                )
            }

            else -> null
        }

    @Suppress("ConvertArgumentToSet")
    private fun handleRequest(event: ModelEvent.UnacknowledgedMessageReceived): Unit {
        when (val request = event.message) {
            is HealthAttentionSetUnacknowledged -> {
                invalidateTimer()
                val duration = request.duration
                if (duration > Duration.ZERO) {
                    triggerAttentionTimer(HealthAttentionTimer.Start(duration = duration))
                    attentionTimerTask =
                        attentionTimer.schedule(delay = duration.inWholeMilliseconds) {
                            invalidateTimer()
                            triggerAttentionTimer(HealthAttentionTimer.Stop)
                        }
                } else {
                    triggerAttentionTimer(HealthAttentionTimer.Stop)
                    invalidateTimer()
                }
            }

            is HealthPeriodSetUnacknowledged -> {
                // This library does not support Period divider.
            }

            is HealthFaultTestUnacknowledged -> {
                // This code allows testing Faults.
                // When HealthFaultTest is sent with Company ID = Nordic Semiconductor (0059),
                // and the "testID" is grater than 0, the fault with the ID equal to the testID
                // is added to the Registered Fault state.
                if (request.companyIdentifier == nordicSemiconductor) {
                    if (request.testId > 0u) {
                        val fault = HealthFault.from(code = request.testId)
                        currentFaultState =
                            currentFaultState + HealthFault.from(code = request.testId)
                    } else {
                        currentFaultState = currentFaultState - currentFaultState
                    }
                } else {
                    // When the company ID isn't Nordic, check that it matches the CID of the Node.
                    require(request.companyIdentifier == companyIdentifier) {
                        logger?.e(category = LogCategory.FOUNDATION_MODEL) {
                            "Company Identifier mismatch"
                        }
                        return
                    }
                }
                mostRecentTestId = request.testId
            }

            is HealthFaultClearUnacknowledged -> {
                require(request.companyIdentifier == companyIdentifier) {
                    logger?.e(category = LogCategory.FOUNDATION_MODEL) {
                        "Company Identifier mismatch"
                    }
                    return
                }
                registeredFaultState = registeredFaultState - registeredFaultState
            }
        }
    }

    private fun invalidateTimer() {
        attentionTimerTask?.cancel()
        attentionTimerTask = null
        attentionTimer.cancel()
        attentionTimer.purge()
    }
}