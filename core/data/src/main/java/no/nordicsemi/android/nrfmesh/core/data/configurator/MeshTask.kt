package no.nordicsemi.android.nrfmesh.core.data.configurator

import androidx.compose.ui.graphics.vector.ImageVector
import no.nordicsemi.kotlin.mesh.core.messages.AcknowledgedConfigMessage
import no.nordicsemi.kotlin.mesh.core.messages.ApplicationMessage
import no.nordicsemi.kotlin.mesh.core.messages.MeshMessage
import no.nordicsemi.kotlin.mesh.core.model.ApplicationKey
import no.nordicsemi.kotlin.mesh.core.model.Model
import no.nordicsemi.kotlin.mesh.core.model.Node

/**
 * Represents a mesh messaging task.
 */
sealed class MeshTask(
    open val icon: ImageVector,
    open val label: String,
    open val applicationKey: ApplicationKey? = null,
    open val message: MeshMessage,
    open val status: TaskStatus = TaskStatus.Idle,
)

/**
 * Represents a config message task.
 */
data class ConfigTask(
    override val icon: ImageVector,
    override val label: String,
    override val message: AcknowledgedConfigMessage,
    override val status: TaskStatus = TaskStatus.Idle,
    val target : Node
) : MeshTask(
    icon = icon,
    label = label,
    message = message,
    status = status,
)

/**
 * Represents an application message task.
 */
data class AppTask(
    override val icon: ImageVector,
    override val label: String,
    override val message: ApplicationMessage,
    override val applicationKey: ApplicationKey,
    override val status: TaskStatus = TaskStatus.Idle,
    val model: Model,
) : MeshTask(
    icon = icon,
    label = label,
    applicationKey = applicationKey,
    message = message,
    status = status
)

/**
 * Represents a task status.
 */
sealed class TaskStatus {
    /**
     * The task is idle.
     */
    object Idle : TaskStatus()

    /**
     * The task is in progress.
     */
    object InProgress : TaskStatus()

    /**
     * The task has been skipped.
     */
    object Skipped : TaskStatus()

    /**
     * The task has been completed.
     */
    object Completed : TaskStatus()

    /**
     * The task has failed.
     */
    data class Error(val error: String) : TaskStatus()
}