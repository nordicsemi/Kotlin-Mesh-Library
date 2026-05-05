@file:Suppress("ClassName", "unused", "MemberVisibilityCanBePrivate")

package no.nordicsemi.kotlin.mesh.provisioning


/**
 * Defines possible state of provisioning process.
 */
sealed class ProvisioningState {

    /**
     * The manager is requesting Provisioning Capabilities from the device.
     */
    object RequestingCapabilities : ProvisioningState()

    /**
     * Provisioning Capabilities were received.
     *
     * @property capabilities           Capabilities of the device.
     * @property defaultParameters      Default configuration parameters of the device.
     * @property start                  Lambda func to invoke to start provisioning with the
     *                                  capabilities.
     * @property cancel                 Lambda func to invoke to cancel the provisioning.
     */
    data class CapabilitiesReceived(
        val capabilities: ProvisioningCapabilities,
        val defaultParameters: ProvisioningParameters,
        val start: (parameters: ProvisioningParameters) -> Unit,
        val cancel: () -> Unit
    ) : ProvisioningState()

    /**
     * Provisioning has been started.
     */
    object Provisioning : ProvisioningState()

    /**
     * Authentication action is required. The user provide an authentication value.
     *
     * @property action Lambda func to invoke to authenticate provisioning process.
     */
    data class AuthActionRequired(val action: AuthAction) : ProvisioningState()

    /**
     * Sent by the device once the user has provided an authentication value.
     */
    object InputComplete : ProvisioningState()

    /**
     * The provisioning process is complete.
     */
    object Complete : ProvisioningState()

    /**
     * The provisioning has failed because of a local error.
     *
     * @property error Remote provisioning error
     */
    data class Failed(val error: Throwable) : ProvisioningState()

    override fun toString() = when (this) {
        RequestingCapabilities -> "Requesting provisioning capabilities"
        is CapabilitiesReceived -> "Provisioning capabilities received"
        Provisioning -> "Provisioning started"
        is AuthActionRequired -> "Requesting authentication action"
        InputComplete -> "Input complete"
        Complete -> "Provisioning complete"
        is Failed -> "Provisioning failed: $error"
    }
}

/**
 * Defines a set of authentication methods aimed at strengthening the provisioning process.
 */
sealed class AuthAction {
    /**
     * The user must provide a 16-byte static authentication value.
     *
     * @property length       Length of the static authentication value.
     * @property authenticate Lambda func to invoke to authenticate provisioning process.
     */
    data class ProvideStaticKey(
        val length: Int,
        val authenticate: (ByteArray) -> Unit
    ) : AuthAction()

    /**
     * The user shall provide a number.
     *
     * @property maxNumberOfDigits Maximum number of digits of the authentication number to provide.
     * @property action            Action to perform after the number is provided.
     * @property authenticate      Lambda func to invoke provide the number.
     */
    data class ProvideNumeric(
        val maxNumberOfDigits: UByte,
        val action: OutputAction,
        val authenticate: (UInt) -> Unit
    ) : AuthAction()

    /**
     * The user shall provide an alphanumeric text.
     *
     * @property maxNumberOfCharacters Maximum number of characters of the alphanumeric text to
     *                                 provide.
     * @property authenticate          Lambda func to invoke provide the alphanumeric text.
     */
    data class ProvideAlphaNumeric(
        val maxNumberOfCharacters: UByte,
        val authenticate: (String) -> Unit
    ) : AuthAction()

    /**
     * The application should display this number to the user. The user should perform the selected
     * action given number of times.
     *
     * @property number Number to display.
     * @property action Input action to perform.
     */
    data class DisplayNumber(val number: UInt, val action: InputAction) : AuthAction()

    /**
     * The application should display this alphanumeric text to the user. The user must enter this
     * text on the provisioning device.
     */
    data class DisplayAlphaNumeric(val text: String) : AuthAction()
}
