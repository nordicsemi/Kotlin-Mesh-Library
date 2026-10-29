package no.nordicsemi.kotlin.mesh.core.messages

/**
 * The base interface for application layer messages, i.e. messages that are encrypted using an
 * application key and are sent to a model. This includes SIG defined messages such as Generic,
 * Sensor, Health and Firmware Update messages as well as vendor messages.
 *
 * Configuration messages, which are encrypted using a device key, are defined by [ConfigMessage].
 */
interface ApplicationMessage : MeshMessage

/**
 * The base interface for unacknowledged application messages.
 */
interface UnacknowledgedApplicationMessage : ApplicationMessage, UnacknowledgedMeshMessage

/**
 * The base interface for acknowledged application messages.
 */
interface AcknowledgedApplicationMessage : ApplicationMessage, AcknowledgedMeshMessage

/**
 * The base interface for application response messages.
 */
interface ApplicationResponse : MeshResponse, UnacknowledgedApplicationMessage
