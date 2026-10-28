package no.nordicsemi.android.nrfmesh.feature.dfu.pager

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.mcumgr.McuMgrTransport
import no.nordicsemi.android.mcumgr.exception.McuMgrException
import no.nordicsemi.android.mcumgr.ktx.managers.DefaultManagerKtx
import no.nordicsemi.android.mcumgr.ktx.managers.ImageManagerKtx
import no.nordicsemi.android.mcumgr.ktx.managers.ShellManagerKtx
import no.nordicsemi.android.mcumgr.ktx.transfer.TransferState
import no.nordicsemi.android.nrfmesh.core.common.firmwareDistributionServer
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.ProxyConnectionState
import no.nordicsemi.android.nrfmesh.feature.dfu.DfuSession
import no.nordicsemi.android.nrfmesh.feature.dfu.util.Receiver
import no.nordicsemi.android.nrfmesh.feature.dfu.util.UpdatePackage
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionMessageStatus
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareDistributionPhase
import no.nordicsemi.kotlin.mesh.core.messages.FirmwareUpdatePolicy
import no.nordicsemi.kotlin.mesh.core.messages.RetrievedUpdatePhase
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionApply
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionCancel
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionReceiversGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionReceiversList
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionStart
import no.nordicsemi.kotlin.mesh.core.messages.foundation.dfu.FirmwareDistributionStatus
import no.nordicsemi.kotlin.mesh.core.model.FixedGroupAddress
import no.nordicsemi.kotlin.mesh.core.model.GroupAddress
import no.nordicsemi.kotlin.mesh.core.model.MeshNetwork
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import no.nordicsemi.kotlin.mesh.core.model.UnassignedAddress
import no.nordicsemi.kotlin.mesh.core.model.UnicastAddress
import no.nordicsemi.kotlin.mesh.core.model.VirtualAddress
import javax.inject.Inject
import kotlin.math.max
import kotlin.time.Duration.Companion.seconds

@HiltViewModel
internal class DistributionViewModel @Inject internal constructor(
    private val repository: CoreDataRepository,
) : ViewModel() {
    val uiState: StateFlow<DistributionScreenUiState>
        field = MutableStateFlow(DistributionScreenUiState())
    private lateinit var network: MeshNetwork
    private var startedSession: DfuSession? = null
    private var uploadJob: Job? = null
    private var applyJob: Job? = null

    // The node that the image is uploaded to. It is kept as the connection to it is lost, when it
    // restarts to apply the update on itself.
    private var distributor: Node? = null

    init {
        observeNetwork()
    }

    private fun observeNetwork() {
        repository.networkEvents
            .mapNotNull { repository.meshNetwork }
            .onEach { network = it }
            .launchIn(scope = viewModelScope)
    }

    /**
     * Starts the firmware distribution of the given session, or follows the one that is already in
     * progress.
     *
     * If the session has a firmware package, its image is uploaded to the Distributor and then
     * distributed to the receivers. If it does not, but the Distributor was found to be distributing
     * an image already, see [DfuSession.ongoingFirmwareSize], the progress of that distribution is
     * followed.
     *
     * Nothing is done if the same session has already been started, i.e. after a configuration
     * change. The work is canceled with the view model.
     */
    internal fun start(session: DfuSession) {
        val updatePackage = session.updatePackage
        val ongoingFirmwareSize = session.ongoingFirmwareSize
        if (updatePackage == null && ongoingFirmwareSize == null) return
        if (session == startedSession) return
        startedSession = session
        distributor = repository.proxyFilter.proxy
        uiState.update { it.copy(distributorName = distributor?.name) }
        // The receivers are known before the distribution starts, so they can be shown right away.
        if (updatePackage != null) {
            val network = repository.meshNetwork
            uiState.update {
                it.copy(
                    receivers = session.receivers,
                    receiverNames = session.receivers.associate { receiver ->
                        receiver.address.address to
                                (network?.node(address = receiver.address)?.name ?: "Unknown")
                    }
                )
            }
        }
        uploadJob?.cancel()
        uploadJob = viewModelScope.launch {
            when {
                updatePackage != null -> uploadAndDistribute(session, updatePackage)
                else -> monitorDistribution(firmwareSize = ongoingFirmwareSize ?: return@launch)
            }
        }
    }

    /**
     * Cancels the firmware update.
     *
     * The upload of the image and the polling of the progress are stopped, and, if the distribution
     * was started, the Distributor is asked to cancel it.
     *
     * @return True if the firmware update was canceled, false if the Distributor could not cancel
     *         the distribution, see [DistributionScreenUiState.cancelError].
     */
    internal suspend fun cancel(): Boolean {
        uiState.update { it.copy(isCancelling = true, cancelError = null) }
        // Stopping the collection of the upload cancels the upload.
        uploadJob?.cancelAndJoin()
        uploadJob = null
        applyJob?.cancelAndJoin()
        applyJob = null
        return try {
            // The distribution may have been started even if the response was not received yet.
            if (uiState.value.distribution != DistributionState.Idle) {
                val response = repository.send(
                    model = distributionServer(),
                    ackedMessage = FirmwareDistributionCancel()
                ) as? FirmwareDistributionStatus
                    ?: throw IllegalStateException("No response received")
                if (response.status != FirmwareDistributionMessageStatus.SUCCESS) {
                    throw IllegalStateException(response.status.toString())
                }
            }
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            uiState.update { it.copy(cancelError = e.message ?: "$e") }
            false
        } finally {
            uiState.update { it.copy(isCancelling = false) }
        }
    }

    /**
     * Applies the firmware update on the receivers, that have verified the image.
     *
     * This is only needed if the update policy is verify only, see [DistributionScreenUiState.updatePolicy].
     * Once the Distributor has accepted the request, the progress of the receivers is polled until
     * all of them have applied the update or failed.
     */
    internal fun apply() {
        if (uiState.value.apply is ApplyState.Applying) return
        applyJob = viewModelScope.launch {
            uiState.update { it.copy(apply = ApplyState.Applying) }
            try {
                val response = repository.send(
                    model = distributionServer(),
                    ackedMessage = FirmwareDistributionApply()
                ) as? FirmwareDistributionStatus
                    ?: throw IllegalStateException("No response received")
                if (response.status != FirmwareDistributionMessageStatus.SUCCESS) {
                    throw IllegalStateException(response.status.toString())
                }
                uiState.update { it.copy(apply = ApplyState.Applied) }
                // The receivers are verified until they start applying the update.
                do {
                    delay(POLL_INTERVAL)
                    val _ = pollDistributionProgress(imageSize = 0, start = null)
                } while (uiState.value.receivers.any { it.status is Receiver.Status.Verified })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                uiState.update { it.copy(apply = ApplyState.Failed(message = e.message ?: "$e")) }
            }
        }
    }

    /**
     * Uploads the firmware image of the given package to the Distributor, starts the distribution
     * and polls its progress until it is over.
     */
    private suspend fun uploadAndDistribute(session: DfuSession, updatePackage: UpdatePackage) {
        // First, create a slot and upload the image.
        val slot = try {
            performUpload(updatePackage = updatePackage).also { slot ->
                uiState.update { it.copy(upload = UploadState.Completed(slot = slot)) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            uiState.update { it.copy(upload = UploadState.Failed(message = e.message ?: "$e")) }
            return
        }
        // Then, start the distribution and poll its progress until it is over.
        try {
            startDistribution(slot = slot, session = session)
            pollDistribution(
                session = session,
                imageSize = updatePackage.zipPackage.getBinaries().images.first().image.data.size
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onDistributionError(e)
        }
    }

    /**
     * Updates the state after the distribution could not be started or followed.
     *
     * If the Distributor is one of the receivers, it restarts to apply the update on itself, so the
     * connection to it is lost and the progress of the distribution can no longer be followed. This
     * is not a failure of the distribution, unless the Distributor rejected it. The distribution is
     * assumed to be completed and all the receivers that did not finish are shown at 100%.
     */
    private fun onDistributionError(e: Exception) {
        val isDistributorReceiver = { receiver: Receiver ->
            distributor?.let { network.node(address = receiver.address)?.uuid == it.uuid } == true
        }
        val progressLost = uiState.value.receivers.any(isDistributorReceiver) &&
                e !is DistributionRejectedException
        uiState.update {
            when (progressLost) {
                // The distribution is assumed to be completed, so the receivers that were still
                // receiving the image are shown as done, instead of stopping at the last progress.
                true -> it.copy(
                    distribution = DistributionState.ProgressLost,
                    receivers = it.receivers.map { receiver ->
                        when (receiver.status) {
                            Receiver.Status.Verified,
                            Receiver.Status.Applied,
                            Receiver.Status.Failure -> receiver

                            else -> receiver.copy(
                                status = Receiver.Status.Distribution(
                                    progress = 100,
                                    speedBytesPerSecond = 0f
                                )
                            )
                        }
                    }
                )

                false -> it.copy(distribution = DistributionState.Failed(message = e.message ?: "$e"))
            }
        }
    }

    /**
     * Follows a distribution that was started earlier, i.e. before the screen was opened.
     *
     * The image has already been uploaded and the distribution has already been started, so only
     * its progress is polled. The receivers are not known, they are read from the Distributor.
     *
     * @param firmwareSize The estimated size of the image being distributed in bytes.
     */
    private suspend fun monitorDistribution(firmwareSize: Int) {
        try {
            uiState.update { it.copy(upload = UploadState.Completed(slot = null)) }
            val response = repository.send(
                model = distributionServer(),
                ackedMessage = FirmwareDistributionGet()
            ) as? FirmwareDistributionStatus
                ?: throw IllegalStateException("No response received")
            uiState.update {
                it.copy(
                    distribution = DistributionState.Started(phase = response.phase),
                    updatePolicy = response.updatePolicy
                )
            }

            // The speed is not known, until the progress is received. The distribution to a group
            // is much faster per receiver than the one to every receiver separately.
            val isMulticast = response.multicastAddress?.let { it != UnassignedAddress.address } == true
            val estimatedSpeed = if (isMulticast) 40f else 130f
            // The start time is not known. It is calculated based on the progress of the first
            // receiver, which is known after the first poll.
            var start: Long? = null
            while (true) {
                val hasTransfer = pollDistributionProgress(
                    imageSize = firmwareSize,
                    start = start,
                    estimatedSpeed = estimatedSpeed
                )
                if (!hasTransfer) break
                if (start == null) {
                    uiState.value.receivers
                        .firstOrNull { it.status.progress > 0 }
                        ?.let { receiver ->
                            val received = firmwareSize * receiver.status.progress / 100f
                            val elapsedMillis = (received / estimatedSpeed * 1000).toLong()
                            start = SystemClock.elapsedRealtime() - elapsedMillis
                        }
                }
                delay(POLL_INTERVAL)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onDistributionError(e)
        }
    }

    /**
     * Sends the Firmware Distribution Start message to the Distributor to start the distribution
     * of the image uploaded to the given slot.
     *
     * @param slot    Index of the image on the Distributor, returned by the upload.
     * @param session The session with the application key and the distribution parameters.
     * @throws IllegalStateException if the message could not be sent or the Distributor rejected
     *                               the distribution.
     */
    private suspend fun startDistribution(slot: Int, session: DfuSession) {
        uiState.update { it.copy(distribution = DistributionState.Starting) }
        val model = distributionServer()
        val key = repository.meshNetwork?.applicationKey(index = session.keyIndex.toUShort())
            ?: throw IllegalStateException("Application key not found")
        val parameters = session.dfuParameters
        val imageIndex = slot.toUShort()
        val message = when (val address = parameters.address) {
            is GroupAddress -> FirmwareDistributionStart(
                firmwareImageIndex = imageIndex,
                groupAddress = address,
                applicationKeyIndex = key.index,
                ttl = parameters.ttl,
                distributionTransferMode = parameters.mode,
                updatePolicy = parameters.policy,
                distributionTimeoutBase = parameters.timeoutBase
            )

            is VirtualAddress -> FirmwareDistributionStart(
                firmwareImageIndex = imageIndex,
                virtualAddress = address,
                applicationKeyIndex = key.index,
                ttl = parameters.ttl,
                distributionTransferMode = parameters.mode,
                updatePolicy = parameters.policy,
                distributionTimeoutBase = parameters.timeoutBase
            )

            is FixedGroupAddress -> FirmwareDistributionStart(
                firmwareImageIndex = imageIndex,
                fixedGroupAddress = address,
                applicationKeyIndex = key.index,
                ttl = parameters.ttl,
                distributionTransferMode = parameters.mode,
                updatePolicy = parameters.policy,
                distributionTimeoutBase = parameters.timeoutBase
            )

            is UnassignedAddress -> FirmwareDistributionStart(
                firmwareImageIndex = imageIndex,
                unassignedAddress = address,
                applicationKeyIndex = key.index,
                ttl = parameters.ttl,
                distributionTransferMode = parameters.mode,
                updatePolicy = parameters.policy,
                distributionTimeoutBase = parameters.timeoutBase
            )

            is UnicastAddress -> FirmwareDistributionStart(
                firmwareImageIndex = imageIndex,
                unicastAddress = address,
                applicationKeyIndex = key.index,
                ttl = parameters.ttl,
                distributionTransferMode = parameters.mode,
                updatePolicy = parameters.policy,
                distributionTimeoutBase = parameters.timeoutBase
            )
        }
        val response = repository.send(model = model, ackedMessage = message)
                as? FirmwareDistributionStatus
            ?: throw IllegalStateException("No response received")
        if (response.status != FirmwareDistributionMessageStatus.SUCCESS) {
            throw DistributionRejectedException(response.status.toString())
        }
        // The receivers are now being updated by the Distributor.
        uiState.update {
            it.copy(
                distribution = DistributionState.Started(phase = response.phase),
                updatePolicy = response.updatePolicy ?: parameters.policy,
                receivers = session.receivers.map { receiver ->
                    receiver.copy(status = Receiver.Status.Distribution(progress = 0, speedBytesPerSecond = 0f))
                },
                receiverNames = session.receivers.associate { receiver ->
                    receiver.address.address to
                            (network.node(address = receiver.address)?.name ?: "Unknown")
                }
            )
        }
    }

    /**
     * Returns the Firmware Distribution Server model of the Distributor.
     */
    private fun distributionServer(): Model {
        val distributor = repository.proxyFilter.proxy
            ?: throw IllegalStateException("Not connected to the Distributor")
        return distributor.model(modelId = firmwareDistributionServer)
            ?: throw IllegalStateException("Firmware Distribution Server model not found")
    }

    /**
     * Polls the progress of the distribution until none of the receivers is receiving the image
     * anymore, i.e. the transfer is over, and updates the state of the receivers.
     *
     * @param session   The session, which holds the receivers.
     * @param imageSize The size of the image being distributed in bytes.
     */
    private suspend fun pollDistribution(session: DfuSession, imageSize: Int) {
        val start = SystemClock.elapsedRealtime()
        // Give the Distributor some time to start the transfer.
        delay(POLL_INTERVAL)
        while (pollDistributionProgress(imageSize = imageSize, start = start)) {
            delay(POLL_INTERVAL)
        }
    }

    /**
     * Reads the status of the receivers from the Distributor, and updates the state.
     *
     * @param imageSize The size of the image being distributed in bytes.
     * @param start     The time when the distribution started, see [SystemClock.elapsedRealtime],
     *                  or null if it is not known.
     * @param estimatedSpeed The speed in bytes per second used when the start time is not known.
     * @return True if at least one of the receivers is receiving the image.
     */
    private suspend fun pollDistributionProgress(
        imageSize: Int,
        start: Long?,
        estimatedSpeed: Float = 0f,
    ): Boolean {
        val model = distributionServer()
        var atLeastOneTransfer = false
        // The request allows for paging, 10 records are read at a time.
        var index = 0
        while (true) {
            val list = repository.send(
                model = model,
                ackedMessage = FirmwareDistributionReceiversGet(
                    firstIndex = index.toUShort(),
                    entriesLimit = PAGE_SIZE.toUShort()
                )
            ) as? FirmwareDistributionReceiversList
                ?: throw IllegalStateException("No response received")
            addMissingReceivers(statuses = list.receivers)
            updateReceiverStatuses(
                statuses = list.receivers,
                imageSize = imageSize,
                start = start,
                estimatedSpeed = estimatedSpeed
            )
            atLeastOneTransfer = atLeastOneTransfer ||
                    list.receivers.any { it.phase == RetrievedUpdatePhase.TRANSFER_ACTIVE }
            index += PAGE_SIZE
            if (list.totalCount.toInt() < index) break
        }
        checkIfCompleted()
        return atLeastOneTransfer
    }

    /**
     * Adds the receivers reported by the Distributor, which are not known yet, to the state. This
     * is needed when following a distribution that was started earlier.
     */
    private fun addMissingReceivers(statuses: List<FirmwareDistributionReceiversList.ReceiverStatus>) {
        uiState.update { state ->
            val missing = statuses.filter { status ->
                state.receivers.none {
                    it.address.address == status.address && it.imageIndex == status.imageIndex
                }
            }
            when (missing.isEmpty()) {
                true -> state
                false -> state.copy(
                    receivers = state.receivers + missing.map {
                        Receiver(address = UnicastAddress(address = it.address), imageIndex = it.imageIndex)
                    },
                    receiverNames = state.receiverNames + missing.associate {
                        it.address to (network.node(address = UnicastAddress(address = it.address))
                            ?.name ?: "Unknown")
                    }
                )
            }
        }
    }

    /**
     * Updates the statuses of the receivers based on the list received from the Distributor.
     */
    private fun updateReceiverStatuses(
        statuses: List<FirmwareDistributionReceiversList.ReceiverStatus>,
        imageSize: Int,
        start: Long?,
        estimatedSpeed: Float,
    ) {
        uiState.update { state ->
            state.copy(
                receivers = state.receivers.map { receiver ->
                    val status = statuses.firstOrNull {
                        it.address == receiver.address.address && it.imageIndex == receiver.imageIndex
                    } ?: return@map receiver
                    when (status.phase) {
                        RetrievedUpdatePhase.TRANSFER_ACTIVE ->
                            // The progress is a percentage, 0-100 in multiples of 2.
                            if (receiver.status.progress < status.transferProgress) {
                                receiver.copy(
                                    status = Receiver.Status.Distribution(
                                        progress = status.transferProgress,
                                        speedBytesPerSecond = calculateSpeed(
                                            progress = status.transferProgress,
                                            imageSize = imageSize,
                                            start = start,
                                            estimatedSpeed = estimatedSpeed
                                        )
                                    )
                                )
                            } else receiver

                        // The whole image has been received, the receiver is verifying it.
                        RetrievedUpdatePhase.VERIFYING_UPDATE ->
                            receiver.copy(
                                status = Receiver.Status.Distribution(
                                    progress = 100,
                                    speedBytesPerSecond = calculateSpeed(
                                        progress = 100,
                                        imageSize = imageSize,
                                        start = start,
                                        estimatedSpeed = estimatedSpeed
                                    )
                                )
                            )

                        RetrievedUpdatePhase.VERIFICATION_SUCCEEDED ->
                            receiver.copy(status = Receiver.Status.Verified)

                        RetrievedUpdatePhase.APPLYING_UPDATE,
                        RetrievedUpdatePhase.APPLY_SUCCESS ->
                            receiver.copy(status = Receiver.Status.Applied)

                        RetrievedUpdatePhase.VERIFICATION_FAILED,
                        RetrievedUpdatePhase.APPLY_FAILED,
                        RetrievedUpdatePhase.TRANSFER_CANCELED ->
                            receiver.copy(status = Receiver.Status.Failure)

                        RetrievedUpdatePhase.IDLE ->
                            receiver.copy(status = Receiver.Status.Idle)

                        else -> receiver
                    }
                }
            )
        }
    }

    /**
     * Calculates the transfer speed in bytes per second based on the progress and the start time.
     */
    private fun calculateSpeed(
        progress: Int,
        imageSize: Int,
        start: Long?,
        estimatedSpeed: Float,
    ): Float {
        if (start == null) return estimatedSpeed
        val bytesReceived = imageSize * progress / 100f
        val elapsedSeconds = (SystemClock.elapsedRealtime() - start) / 1000f
        // The coefficient was calculated during tests, and is applied to take into account that
        // the progress is only updated after a page (4096 bytes) is received, not continuously.
        return if (elapsedSeconds > 0f) 1.13f * bytesReceived / elapsedSeconds else 0f
    }

    /**
     * Checks if the distribution is over, that is when all the receivers are verified, applied or
     * have failed.
     */
    private fun checkIfCompleted() {
        val receivers = uiState.value.receivers
        if (receivers.isEmpty()) return
        when {
            receivers.all { it.status is Receiver.Status.Failure } ->
                uiState.update { it.copy(distribution = DistributionState.Failed("All the receivers failed")) }

            receivers.all { it.status is Receiver.Status.Applied || it.status is Receiver.Status.Verified } ->
                uiState.update { it.copy(distribution = DistributionState.Completed) }
        }
    }

    /**
     * Creates a new slot for the image on the Distributor and uploads the image to it.
     *
     * The Mcu Manager transport of the repository, which is connected to the Distributor, is used,
     * and it is not released afterwards so that it can be used by the next steps.
     *
     * @param updatePackage The package with the image to be uploaded.
     * @return The index of the slot created for the image on the Distributor.
     */
    private suspend fun performUpload(updatePackage: UpdatePackage): Int {
        val image = updatePackage.zipPackage.getBinaries().images.firstOrNull()
            ?: throw IllegalStateException("No images available")
        val transport = repository.mcuMgrTransport()
            ?: throw IllegalStateException("Not connected to the Distributor")
        val metadata = updatePackage.metadata

        uiState.update { it.copy(upload = UploadState.CreatingSlot) }
        val slot = createSlot(
            transport = transport,
            imageSize = image.image.data.size,
            firmwareId = metadata.firmwareIdString,
            metadata = metadata.metadataString
        )

        uiState.update { it.copy(upload = UploadState.ReadingParameters) }
        // The number of buffers on the device gives the number of packets that can be sent without
        // waiting for a response. One buffer is left free. If the parameters cannot be read, the
        // packets are sent one by one.
        val windowCapacity = try {
            DefaultManagerKtx(transport).params().bufCount.takeIf { it > 0 }
                ?.let { max(1, it - 1) } ?: 1
        } catch (e: McuMgrException) {
            1
        }

        uiState.update { it.copy(upload = UploadState.Uploading(progress = 0f)) }
        val start = SystemClock.elapsedRealtime()
        ImageManagerKtx(transport)
            .upload(
                data = image.image.data,
                image = image.imageIndex,
                windowCapacity = windowCapacity
            )
            .collect { state ->
                when (state) {
                    is TransferState.InProgress -> {
                        val elapsedSeconds = (SystemClock.elapsedRealtime() - start) / 1000f
                        uiState.update {
                            it.copy(
                                upload = UploadState.Uploading(
                                    progress = state.progress,
                                    speedBytesPerSecond = when (elapsedSeconds > 0f) {
                                        true -> state.current / elapsedSeconds
                                        false -> 0f
                                    }
                                )
                            )
                        }
                    }

                    TransferState.Cancelled -> throw IllegalStateException("Upload cancelled")
                    else -> Unit
                }
            }
        return slot
    }

    /**
     * Creates a new slot for the image on the Distributor.
     *
     * @param transport   The transport to use.
     * @param imageSize   The size of the image in bytes.
     * @param firmwareId  The firmware ID, as a hexadecimal string.
     * @param metadata    The optional metadata, as a hexadecimal string.
     * @return The index of the newly created slot.
     */
    private suspend fun createSlot(
        transport: McuMgrTransport,
        imageSize: Int,
        firmwareId: String,
        metadata: String?,
    ): Int {
        val arguments = listOfNotNull(
            "models", "dfu", "slot", "add", "$imageSize", firmwareId, metadata
        )
        val response = ShellManagerKtx(transport)
            .exec(cmd = "mesh", argv = arguments.toTypedArray())
        // The response has the following format:
        //
        //    Adding slot (size: <Size>)
        //
        //    Slot added. Index: <Index>
        //
        // The index of the image is needed to start the distribution.
        val output = response.o
        return output
            ?.let { Regex("""Index:\s*(\d+)""").find(it) }
            ?.groupValues?.get(1)?.toIntOrNull()
            ?: throw IllegalStateException("Adding slot failed. Invalid response: ${output ?: "Empty"}")
    }

    private companion object {
        /** The interval of reading the distribution progress. */
        val POLL_INTERVAL = 10.seconds

        /** The number of receivers read from the Distributor at a time. */
        const val PAGE_SIZE = 10
    }
}

/** The state of the distribution of the firmware image from the Distributor to the receivers. */
internal sealed interface DistributionState {
    /** The distribution has not been started. */
    data object Idle : DistributionState

    /** The Distributor is being asked to start the distribution. */
    data object Starting : DistributionState

    /**
     * The Distributor accepted the distribution.
     *
     * @property phase The phase of the distribution reported by the Distributor.
     */
    data class Started(val phase: FirmwareDistributionPhase) : DistributionState

    /** All the receivers received the image and verified or applied it. */
    data object Completed : DistributionState

    /** The distribution could not be started, or it failed. */
    data class Failed(val message: String) : DistributionState

    /**
     * The distribution was started, but its progress is no longer known. This happens if the
     * Distributor is one of the receivers, as it restarts to apply the update on itself and the
     * connection to it is lost.
     */
    data object ProgressLost : DistributionState
}

/** Thrown when the Distributor rejects the request to start the distribution. */
private class DistributionRejectedException(message: String) : IllegalStateException(message)

/** The state of the upload of the firmware image to the Distributor. */
internal sealed interface UploadState {
    /** The upload has not started. */
    data object Idle : UploadState

    /** A slot for the image is being created on the Distributor. */
    data object CreatingSlot : UploadState

    /** The Mcu Manager parameters are being read from the Distributor. */
    data object ReadingParameters : UploadState

    /** The image is being uploaded. */
    data class Uploading(
        val progress: Float,
        val speedBytesPerSecond: Float = 0f,
    ) : UploadState

    /**
     * The image was uploaded.
     *
     * @property slot Index of the image on the Distributor, used to start the distribution, or null
     *                if the image was uploaded earlier.
     */
    data class Completed(val slot: Int?) : UploadState

    /** The upload failed. */
    data class Failed(val message: String) : UploadState
}

internal data class DistributionScreenUiState(
    val proxyConnectionState: ProxyConnectionState = ProxyConnectionState(),
    val upload: UploadState = UploadState.Idle,
    val distribution: DistributionState = DistributionState.Idle,
    val receivers: List<Receiver> = emptyList(),
    val receiverNames: Map<UShort, String> = emptyMap(),
    // The name of the Distributor, which the image is uploaded to.
    val distributorName: String? = null,
    val isCancelling: Boolean = false,
    val cancelError: String? = null,
    // The update policy of the distribution, as reported by the Distributor.
    val updatePolicy: FirmwareUpdatePolicy? = null,
    val apply: ApplyState = ApplyState.Idle,
)

/** The state of applying the firmware update on the receivers. */
internal sealed interface ApplyState {

    /** The update has not been applied. */
    data object Idle : ApplyState

    /** The Distributor is asked to apply the update. */
    data object Applying : ApplyState

    /** The Distributor has accepted the request to apply the update. */
    data object Applied : ApplyState

    /** Applying the update failed. */
    data class Failed(val message: String) : ApplyState
}
