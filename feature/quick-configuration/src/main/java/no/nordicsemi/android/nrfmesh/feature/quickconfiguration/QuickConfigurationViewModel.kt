package no.nordicsemi.android.nrfmesh.feature.quickconfiguration

import androidx.lifecycle.ViewModel
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import no.nordicsemi.android.nrfmesh.core.common.di.IoDispatcher
import no.nordicsemi.android.nrfmesh.core.data.CoreDataRepository
import no.nordicsemi.android.nrfmesh.core.data.configurator.MeshTask
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.Group
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node
import kotlin.uuid.Uuid

/** The kind of configuration the user wants to perform. */
internal enum class QuickConfigurationMode { Bind, Subscribe, Publish }

internal data class QuickConfigurationUiState(
    val node: Node? = null,
    val applicationKeys: List<ApplicationKey> = emptyList(),
    val groups: List<Group> = emptyList(),
    val mode: QuickConfigurationMode = QuickConfigurationMode.Bind,
    val selectedKeys: Set<ApplicationKey> = emptySet(),
    val selectedGroups: Set<Group> = emptySet(),
    val publishKey: ApplicationKey? = null,
    val destination: Group? = null,
    val selectedModels: Set<Model> = emptySet(),
    val tasks: List<MeshTask> = emptyList(),
    val isExecuting: Boolean = false,
    /** The selection is already configured on the node, so there are no tasks to run. */
    val nothingToConfigure: Boolean = false,
    val alreadyConfigured: List<AlreadyConfigured> = emptyList(),
    /** Set when the node could not be reached, in which case the remaining tasks are skipped. */
    val failure: Throwable? = null,
) {
    /** Models of the node that support the operation of the current [mode]. */
    val models: List<Model>
        get() = node?.elements.orEmpty().flatMap { it.models }.filter {
            when (mode) {
                QuickConfigurationMode.Bind -> it.supportsApplicationKeyBinding
                QuickConfigurationMode.Subscribe -> it.supportsModelSubscription
                QuickConfigurationMode.Publish -> it.supportsModelPublication
            }
        }

    /** Whether the keys or groups required by the current [mode] have been selected. */
    val hasSelection: Boolean
        get() = when (mode) {
            QuickConfigurationMode.Bind -> selectedKeys.isNotEmpty()
            QuickConfigurationMode.Subscribe -> selectedGroups.isNotEmpty()
            QuickConfigurationMode.Publish -> publishKey != null && destination != null
        }
}

/**
 * Builds configuration tasks for a node and runs them using a detached [no.nordicsemi.android.nrfmesh.core.data.configurator.Messenger],
 * so they don't interfere with the messenger of the node screen.
 */
@HiltViewModel(assistedFactory = QuickConfigurationViewModel.Factory::class)
internal class QuickConfigurationViewModel @AssistedInject constructor(
    private val repository: CoreDataRepository,
    @param:IoDispatcher ioDispatcher: CoroutineDispatcher,
    @Assisted uuid: String,
) : ViewModel() {
    private val scope = CoroutineScope(context = SupervisorJob() + ioDispatcher)
    private val messenger = repository.messengers.createDetached()
    private var job: Job? = null

    val uiState: StateFlow<QuickConfigurationUiState>
        field = MutableStateFlow(QuickConfigurationUiState())

    init {
        val network = repository.meshNetwork
        uiState.update {
            it.copy(
                node = network?.node(uuid = Uuid.parse(uuidString = uuid)),
                applicationKeys = network?.applicationKeys.orEmpty(),
                groups = network?.groups.orEmpty()
            )
        }
        messenger.meshTaskFlow
            .onEach { tasks -> uiState.update { it.copy(tasks = tasks) } }
            .launchIn(scope = scope)
        messenger.failure
            .onEach { failure -> uiState.update { it.copy(failure = failure) } }
            .launchIn(scope = scope)
    }

    override fun onCleared() {
        scope.cancel()
        messenger.clear()
    }

    fun onModeSelected(mode: QuickConfigurationMode) = uiState.update {
        it.copy(mode = mode, selectedModels = emptySet())
    }

    fun onKeyToggled(key: ApplicationKey) = uiState.update {
        it.copy(selectedKeys = it.selectedKeys.toggle(key))
    }

    fun onGroupToggled(group: Group) = uiState.update {
        it.copy(selectedGroups = it.selectedGroups.toggle(group))
    }

    fun onAllKeysToggled() = uiState.update {
        it.copy(selectedKeys = if (it.selectedKeys.size == it.applicationKeys.size) emptySet() else it.applicationKeys.toSet())
    }

    fun onAllGroupsToggled() = uiState.update {
        it.copy(selectedGroups = if (it.selectedGroups.size == it.groups.size) emptySet() else it.groups.toSet())
    }

    /** Creates a new Application Key bound to the first Network Key, and selects it. */
    fun onAddKeyClicked() {
        val network = repository.meshNetwork ?: return
        val netKey = network.networkKeys.firstOrNull() ?: return
        runCatching { repository.addApplicationKey(boundNetworkKey = netKey) }
            .onSuccess { key ->
                uiState.update {
                    it.copy(
                        applicationKeys = network.applicationKeys,
                        selectedKeys = it.selectedKeys + key,
                        // The publication uses a single key, so the new key is selected for it.
                        publishKey = if (it.mode == QuickConfigurationMode.Publish) key
                        else it.publishKey
                    )
                }
            }
    }

    /** Creates a new Group with the next available address, and selects it. */
    fun onAddGroupClicked() {
        val network = repository.meshNetwork ?: return
        runCatching {
            val provisioner = network.provisioners.first()
            val address = network.nextAvailableGroup(provisioner = provisioner)
                ?: error("No available group address")
            Group(_name = "Group ${network.groups.size + 1}", address = address)
                .also { network.add(group = it) }
        }.onSuccess { group ->
            repository.save()
            uiState.update {
                it.copy(groups = network.groups, selectedGroups = it.selectedGroups + group)
            }
        }
    }

    fun onPublishKeySelected(key: ApplicationKey) = uiState.update { it.copy(publishKey = key) }

    fun onDestinationSelected(group: Group) = uiState.update { it.copy(destination = group) }

    fun onModelToggled(model: Model) = uiState.update {
        it.copy(selectedModels = it.selectedModels.toggle(model))
    }

    fun onAllModelsToggled() = uiState.update {
        val all = it.models.toSet()
        it.copy(selectedModels = if (it.selectedModels == all) emptySet() else all)
    }

    /** Enqueues the tasks for the current selection and executes them. */
    fun onApplyPressed() {
        val state = uiState.value
        val node = state.node ?: return
        val models = state.models.filter { it in state.selectedModels }
        val tasks = when (state.mode) {
            QuickConfigurationMode.Bind -> bindTasks(
                node = node,
                keys = state.applicationKeys.filter { it in state.selectedKeys },
                models = models
            )

            QuickConfigurationMode.Subscribe -> subscribeTasks(
                node = node,
                groups = state.groups.filter { it in state.selectedGroups },
                models = models
            )

            QuickConfigurationMode.Publish -> publishTasks(
                node = node,
                key = state.publishKey ?: return,
                destination = state.destination ?: return,
                models = models
            )
        }
        val alreadyConfigured = when (state.mode) {
            QuickConfigurationMode.Bind -> alreadyBound(
                keys = state.applicationKeys.filter { it in state.selectedKeys },
                models = models
            )

            QuickConfigurationMode.Subscribe -> alreadySubscribed(
                groups = state.groups.filter { it in state.selectedGroups },
                models = models
            )

            QuickConfigurationMode.Publish -> emptyList()
        }
        messenger.clear()
        tasks.forEach(messenger::enqueueTask)
        uiState.update {
            it.copy(
                isExecuting = true,
                nothingToConfigure = tasks.isEmpty(),
                alreadyConfigured = if (tasks.isEmpty()) alreadyConfigured else emptyList()
            )
        }
        run()
    }

    fun onRetryPressed() = run(retry = true)

    /** Called when the user has acknowledged the failure dialog. */
    fun onFailureDismissed() = uiState.update { it.copy(failure = null) }

    /** Called when the tasks sheet is closed, either completed or cancelled. */
    fun onTasksClosed() {
        job?.cancel()
        messenger.clear()
        uiState.update { it.copy(
                isExecuting = false,
                selectedModels = emptySet(),
                nothingToConfigure = false,
                alreadyConfigured = emptyList()
            ) }
    }

    private fun run(retry: Boolean = false) {
        job?.cancel()
        job = scope.launch { if (retry) messenger.retry() else messenger.execute() }
    }

    private fun <T> Set<T>.toggle(item: T) = if (item in this) this - item else this + item

    @AssistedFactory
    interface Factory {
        fun create(uuid: String): QuickConfigurationViewModel
    }
}
