@file:Suppress("MemberVisibilityCanBePrivate", "unused", "PropertyName")

package no.nordicsemi.kotlin.mesh.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import no.nordicsemi.kotlin.mesh.core.exception.SecurityException
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyGet
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyList
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigAppKeyStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigCompositionDataStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.ConfigNetKeyStatus
import no.nordicsemi.kotlin.mesh.core.messages.foundation.configuration.Page0
import no.nordicsemi.kotlin.mesh.core.model.serialization.KeySerializer
import no.nordicsemi.kotlin.mesh.core.model.serialization.UShortAsStringSerializer
import no.nordicsemi.kotlin.mesh.core.model.serialization.UuidSerializer
import no.nordicsemi.kotlin.mesh.crypto.Crypto
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The node represents a configured state of a mesh node.
 *
 * @property uuid                       Unique 128-bit UUID of the node.
 * @property deviceKey                  128-bit device key. When importing a partially exported
 *                                      network configuration, the device key might not be present
 *                                      in the Mesh Network Configuration Database.
 * @property security                   Represents the level of [Security] for the subnet on which
 *                                      the node has been originally provisioned.
 * @property netKeys                    Array of [NodeKey] that includes information about the
 *                                      network keys known to this node.
 * @property configComplete             True if the Mesh Manager determines that this node’s
 *                                      configuration process is completed; otherwise,
 *                                      the property’s value is set to false.
 * @property name                       Human-readable name that can identify this node within the
 *                                      mesh network.
 * @property companyIdentifier          16-bit Company Identifier (CID) assigned by the Bluetooth
 *                                      SIG. The value of this property is obtained from node
 *                                      composition data.
 * @property productIdentifier          16-bit, vendor-assigned Product Identifier (PID). The value
 *                                      of this property is obtained from node composition data.
 * @property versionIdentifier          16-bit, vendor-assigned product Version Identifier (VID).
 *                                      The value of this property is obtained from node composition
 *                                      data
 * @property replayProtectionCount      16-bit value indicating the minimum number of Replay
 *                                      Protection List (RPL) entries for this node. The value of
 *                                      this property is obtained from node composition data. RPL
 *                                      implementation handles a multi-segment message transaction
 *                                      which is under a replay attack. The sequence number of the
 *                                      last segment that has been received for this message is
 *                                      stored for that peer node in the replay protection list.
 * @property features                   [Features] supported by the node.
 * @property secureNetworkBeacon        Represents whether the node is configured to send Secure
 *                                      Network beacons.
 * @property defaultTTL                 0 to 127 that represents the default Time to Live (TTL)
 *                                      value used when sending messages.
 * @property networkTransmit            [NetworkTransmit] represents the parameters of the
 *                                      transmissions of network layer messages originating from a
 *                                      mesh node.
 * @property relayRetransmit            [RelayRetransmit] represents the parameters of the
 *                                      retransmissions of network layer messages relayed by a mesh
 *                                      node.
 * @property appKeys                    Array of [NodeKey] that includes information about the
 *                                      [ApplicationKey]s known to this node.
 * @property elements                   Array of elements contained in the Node.
 * @property excluded                   True if the node is in the process of being deleted and is
 *                                      excluded from the new network key distribution during the
 *                                      Key Refresh procedure; otherwise, it is set to “false”.
 * @property elementsCount              Number of elements belonging to this node.
 * @property addresses                  List of addresses used by this node.
 * @property unicastRange               Address range used by this node.
 * @property lastUnicastAddress         Address of the last element in the node.
 * @property primaryUnicastAddress      Address of the primary element in the node.
 * @property configComplete             True if the node is configured.
 * @property networkKeys                List of network keys known to this node.
 * @property applicationKeys            List of application keys known to this node.
 * @property primaryElement             The primary element of the node.
 * @property network                    The mesh network to which this node belongs.
 * @property provisioner                The provisioner that provisioned this node.
 * @property isProvisioner              True if the node is a provisioner.
 * @property isLocalProvisioner         True if the node is the local provisioner.
 * @property provisioner                The provisioner that provisioned this node.
 * @property isCompositionDataReceived  True if the node has received composition data.
 *
 * @constructor                         Creates a mesh node.
 */
@ConsistentCopyVisibility
@OptIn(ExperimentalUuidApi::class)
@Serializable
data class Node internal constructor(
    @SerialName(value = "UUID")
    @Serializable(with = UuidSerializer::class)
    val uuid: Uuid,
    @SerialName(value = "name")
    private var _name: String = "nRF Mesh Node",
    @Serializable(with = KeySerializer::class)
    val deviceKey: ByteArray?,
    @SerialName(value = "unicastAddress")
    internal var _primaryUnicastAddress: UnicastAddress,
    @SerialName(value = "elements")
    private var _elements: MutableList<Element>,
    @SerialName(value = "netKeys")
    private var _netKeys: MutableList<NodeKey>,
    @SerialName(value = "appKeys")
    private var _appKeys: MutableList<NodeKey>,
) {

    val primaryUnicastAddress: UnicastAddress
        get() = _primaryUnicastAddress

    var name: String
        get() = _name
        set(value) {
            require(value = value.isNotBlank()) { "Name cannot be empty!" }
            MeshNetwork.onChange(oldValue = _name, newValue = value) {
                _name = value
                network?.updateTimestamp()
            }
        }

    val netKeys: List<NodeKey>
        get() = _netKeys

    val appKeys: List<NodeKey>
        get() = _appKeys

    val elements: List<Element>
        get() = _elements

    var security: Security = Insecure
        internal set

    var configComplete: Boolean = false
        internal set(value) {
            field = value
            network?.updateTimestamp()
        }

    val networkKeys: List<NetworkKey>
        get() = network?.networkKeys?.knownTo(node = this) ?: emptyList()

    val applicationKeys: List<ApplicationKey>
        get() = network?.applicationKeys?.knownTo(node = this) ?: emptyList()

    @Serializable(UShortAsStringSerializer::class)
    @SerialName(value = "cid")
    var companyIdentifier: UShort? = null
        internal set

    @Serializable(UShortAsStringSerializer::class)
    @SerialName(value = "pid")
    var productIdentifier: UShort? = null
        internal set

    @Serializable(UShortAsStringSerializer::class)
    @SerialName(value = "vid")
    var versionIdentifier: UShort? = null
        internal set

    @Serializable(UShortAsStringSerializer::class)
    @SerialName(value = "crpl")
    var replayProtectionCount: UShort? = null
        internal set

    var features: Features = Features(
        _relay = null,
        _proxy = null,
        _friend = null,
        _lowPower = null
    )
        internal set

    var secureNetworkBeacon: Boolean? = null
        internal set(value) {
            field = value
            network?.updateTimestamp()
        }

    var networkTransmit: NetworkTransmit? = null
        internal set(value) {
            field = value
            network?.updateTimestamp()
        }

    var relayRetransmit: RelayRetransmit? = null
        internal set(value) {
            field = value
            network?.updateTimestamp()
        }

    var defaultTTL: UByte? = null
        internal set(value) {
            field = value
            network?.updateTimestamp()
        }

    var excluded: Boolean = false
        set(value) {
            field = value
            network?.updateTimestamp()
        }

    @SerialName(value = "heartbeatPub")
    var heartbeatPublication: HeartbeatPublication? = null
        internal set(value) {
            field = value
            network?.updateTimestamp()
        }

    @SerialName(value = "heartbeatSub")
    var heartbeatSubscription: HeartbeatSubscription? = null
        internal set(value) {
            field = value
            network?.updateTimestamp()
        }

    val primaryElement: Element
        get() = elements.first()

    val elementsCount: Int
        get() = elements.size

    val addresses: List<UnicastAddress>
        get() = List(elementsCount) { index -> _primaryUnicastAddress + index }

    val unicastRange: UnicastRange
        get() = UnicastRange(_primaryUnicastAddress, elementsCount)

    val lastUnicastAddress: UnicastAddress
        get() = _primaryUnicastAddress + when (elementsCount > 0) {
            true -> elementsCount
            false -> 1 // TODO should we throw here?
        } - 1

    val isCompositionDataReceived: Boolean
        get() = companyIdentifier != null

    val isProvisioner: Boolean
        get() = network?.provisioner(uuid) != null

    val isLocalProvisioner: Boolean
        get() = network?.localProvisioner?.uuid == uuid

    val provisioner: Provisioner?
        get() = network?.provisioner(uuid)

    /**
     * Convenience constructor to initialize a node of a provisioner.
     *
     * @param provisioner               Provisioner.
     * @param unicastAddress            Unicast address that was assigned during provisioning.
     */
    internal constructor(
        provisioner: Provisioner,
        unicastAddress: UnicastAddress,
    ) : this(
        provisioner = provisioner,
        unicastAddress = unicastAddress,
        deviceKey = Crypto.generateRandomKey(),
        elements = emptyList(),
        netKeys = emptyList<NetworkKey>(),
        appKeys = emptyList<ApplicationKey>(),
    )

    /**
     * Convenience constructor to initialize a node of a provisioner.
     *
     * @param provisioner               Provisioner.
     * @param unicastAddress            Unicast address that was assigned during provisioning.
     * @param deviceKey                 Device key.
     * @param elements                  List of elements belonging to this node.
     * @param netKeys                   List of network keys known to this node.
     * @param appKeys                   List of application keys known to this node.
     */
    internal constructor(
        provisioner: Provisioner,
        unicastAddress: UnicastAddress,
        deviceKey: ByteArray = Crypto.generateRandomKey(),
        elements: List<Element> = emptyList(),
        netKeys: List<NetworkKey>,
        appKeys: List<ApplicationKey>,
    ) : this(
        uuid = provisioner.uuid,
        deviceKey = deviceKey,
        _primaryUnicastAddress = unicastAddress,
        _elements = elements.toMutableList(),
        _netKeys = netKeys.map { NodeKey(it.index, false) }.toMutableList(),
        _appKeys = appKeys.map { NodeKey(it.index, false) }.toMutableList()
    ) {
        this._name = provisioner.name
    }

    /**
     * Convenience constructor to initialize a node for tests.
     *
     * @param address                   Unicast address that was assigned during provisioning.
     * @param elements                  Number of elements.
     * @throws SecurityException        If the security level of the network key does not match the
     *                                  security level used when provisioning the node.
     */
    @Throws(SecurityException::class)
    internal constructor(name: String, address: Int, elements: Int) : this(
        uuid = Uuid.random(),
        deviceKey = Crypto.generateRandomKey(),
        _primaryUnicastAddress = UnicastAddress(address),
        _elements = MutableList(elements) { Element(location = Location.UNKNOWN) },
        _netKeys = mutableListOf(NodeKey(index = 0u, _updated = false)),
        _appKeys = mutableListOf()
    ) {
        this._name = name
    }

    /**
     * Convenience constructor to initialize a node from an unprovisioned device.
     *
     * @param uuid                      Unprovisioned device uuid.
     * @param deviceKey                 Device key.
     * @param unicastAddress            Unicast address that was assigned during provisioning.
     * @param elementCount              Number of elements.
     * @param assignedNetworkKey        Network key that was assigned during provisioning.
     * @param security                  Security level.
     * @throws SecurityException        If the security level of the network key does not match the
     *                                  security level used when provisioning the node.
     */
    @Throws(SecurityException::class)
    constructor(
        name: String = "nRF Mesh Node",
        uuid: Uuid,
        deviceKey: ByteArray,
        unicastAddress: UnicastAddress,
        elementCount: Int,
        assignedNetworkKey: NetworkKey,
        security: Security,
    ) : this(
        uuid = uuid,
        _name = name,
        deviceKey = deviceKey,
        _primaryUnicastAddress = unicastAddress,
        _elements = MutableList(elementCount) {
            Element(
                location = Location.UNKNOWN
            )
        },
        _netKeys = mutableListOf(NodeKey(assignedNetworkKey)),
        _appKeys = mutableListOf()
    ) {
        // TODO this should be uncommented once the demo is over.
        /*require(assignedNetworkKey.security == security) {
            throw SecurityException
        }*/
        this.security = security
    }

    init {
        // TODO is this really needed.
        /*require(elements.isNotEmpty()) {
            throw IllegalArgumentException("At least one element is mandatory!")
        }*/
        elements.forEachIndexed { index, element ->
            // Assigns the index based on position in the list of elements.
            element.index = index
            // Assigns the current node as the parent node of the element.
            element.parentNode = this
        }
    }

    @Transient
    var network: MeshNetwork? = null
        internal set

    /**
     * Returns the network key with the given index added to the node.
     *
     * @param index Network Key index.
     * @return Network Key with the given index or null if not found.
     */
    fun networkKey(index: KeyIndex) = networkKeys.firstOrNull {
        it.index == index
    }

    /**
     * Adds a network key to a node.
     *
     * @param key     Network key to be added.
     * @return        True if success or false if the key already exists.
     */
    internal fun add(key: NetworkKey) = NodeKey(key = key).run {
        when {
            _netKeys.contains(this) -> false
            else -> {
                _netKeys.add(this)
                network?.updateTimestamp()
                true
            }
        }
    }

    /**
     * Adds a network key to the node. Invoked only when a [ConfigNetKeyStatus] is received with a
     * success status.
     *
     * @param index Network Key index.
     */
    internal fun addNetKey(index: KeyIndex) {
        if (!_netKeys.has(index = index))
            _netKeys.add(NodeKey(index = index, _updated = false))
        network?.let {
            if (security is Insecure) {
                it.networkKey(index = index)?.lowerSecurity()
            }
            it.updateTimestamp()
        }
    }

    /**
     * Marks the given Network Key as updated.
     *
     * @param index Network Key index.
     */
    internal fun updateNetKey(index: KeyIndex) {
        _netKeys.get(index)?.apply {
            update(true)
        }
        network?.updateTimestamp()
    }

    /**
     * Removes a network key to the node. Invoked only when a [ConfigNetKeyStatus] is received with
     * a success status.
     *
     * Note: When invoked all application keys bound to the network key will be removed as well.
     *
     * @param index Network Key index.
     */
    internal fun removeNetKey(index: KeyIndex) {
        _netKeys.get(index)?.let { netKey ->
            _netKeys.remove(netKey)
            applicationKeys
                .filter { it.boundNetKeyIndex == index }
                .forEach { boundAppKey ->
                    _appKeys
                        .get(boundAppKey.index)
                        ?.let { _appKeys.remove(it) }
                }
            // Remove the heartbeat publication, if set to use the removed network key.
            if (heartbeatPublication?.index == index) heartbeatPublication = null
            network?.updateTimestamp()
        }
    }

    /**
     * Sets the given list of Network Keys to the node.
     *
     * This method overwrites any existing keys.
     *
     * @param netKeyIndexes List of Network Keys to set.
     */
    internal fun setNetKeys(netKeyIndexes: List<KeyIndex>) {
        _netKeys = netKeyIndexes.map { NodeKey(it, false) }
            .toMutableList()
            .apply { sortBy { it.index } }
        // Remove any Application Keys bound to Network Keys which are not in the provided
        // netKeyIndexes list.
        _appKeys = _appKeys.filter { nodeKey ->
            applicationKey(index = nodeKey.index)
                ?.let { netKeyIndexes.contains(it.boundNetKeyIndex) }
            // Keep the app key if it cannot be found, if used with "== true" unknown keys  will
            // be removed.
                ?: true
        }.toMutableList()
        // if an insecure Node received a Network Key, make sure to lower the minSecurity field of
        // all the keys in it.
        if (security is Insecure) {
            networkKeys.forEach { it.lowerSecurity() }
        }
        network?.updateTimestamp()
    }

    /**
     * Sets the given list of Network Keys to the Node.
     *
     * Note: This is over write any existing keys.
     * @param keys List of Network Keys to set.
     */
    internal fun assignNetKeys(keys: List<NetworkKey>) {
        _netKeys = keys.map { NodeKey(it.index, false) }.toMutableList()
        network?.updateTimestamp()
    }

    /**
     * Sets the given list of Network Keys to the Node.
     *
     * Note: This is over write any existing keys.
     * @param keys List of Network Keys to set.
     */
    internal fun assignNetKeyIndexes(keys: List<KeyIndex>) {
        _netKeys = keys.map { NodeKey(it, false) }.toMutableList()
        // If an insecure Node received a Network Key, all network keys of the node should be
        // downgraded to lower security.
        if (security is Insecure) {
            networkKeys.forEach { it.lowerSecurity() }
        }
        network?.updateTimestamp()
    }

    /**
     * Returns the application key with the given index added to the node.
     *
     * @param index Application Key index.
     * @return Application Key with the given index or null if not found.
     */
    fun applicationKey(index: KeyIndex) = applicationKeys.firstOrNull {
        it.index == index
    }

    /**
     * Adds an application key to a node.
     *
     * @param key     Application key to be added.
     * @return        True if success or false if the key already exists.
     */
    internal fun add(key: ApplicationKey): Boolean {
        val k = NodeKey(key = key)
        return when (appKeys.contains(k)) {
            true -> false
            false -> {
                _appKeys.add(k)
                network?.updateTimestamp()
                true
            }
        }
    }

    /**
     * Adds an application key to the node. Invoked only when a [ConfigAppKeyStatus] is received
     * with a success status.
     *
     * @param index Network Key index.
     */
    internal fun addAppKey(index: KeyIndex) {
        if (!_appKeys.has(index = index)) {
            _appKeys.add(NodeKey(index, false))
        }
        network?.updateTimestamp()
    }

    /**
     * Mark the given application key in node as updated.
     *
     * @param index Application Key index.
     */
    internal fun updateAppKey(index: KeyIndex) {
        _appKeys.get(index)?.apply {
            update(true)
            network?.updateTimestamp()
        }
    }

    /**
     * Removes an application key from the node. Invoked only when a [ConfigNetKeyStatus] is
     * received with a success status.
     *
     * @param index Network Key index.
     */
    internal fun removeAppKey(index: KeyIndex) {
        _appKeys.get(index)?.let { appKey ->
            _appKeys.remove(appKey)
            // Remove all bindings to the application key. Removing any app key bindings will also
            // remove any publications using the same app key.
            elements
                .flatMap { it.models }
                .forEach { model ->
                    model.unbind(index)
                }
            network?.updateTimestamp()
        }
    }

    /**
     * Replaces the existing set of assigned application keys with the given list of [appKeyIndexes]
     * that's bound to the given [netKeyIndex]. This is invoked by [ConfigAppKeyList] message that's
     * in response to [ConfigAppKeyGet] message.
     *
     * @param appKeyIndexes      List of Application Keys to set.
     * @param netKeyIndex        Key index network key bound to [appKeyIndexes].
     */
    internal fun setAppKeys(appKeyIndexes: List<KeyIndex>, netKeyIndex: KeyIndex) {
        // Replace only the keys that are bound to the given network key.
        _appKeys = _appKeys.filter { nodeKey ->
            applicationKey(index = nodeKey.index)?.boundNetKeyIndex != netKeyIndex
        }.toMutableList()
        _appKeys.addAll(elements = appKeyIndexes.map { NodeKey(index = it, _updated = false) })
        _appKeys.sortBy { it.index }
        network?.updateTimestamp()
    }

    /**
     * Sets the given list of Application Keys to the Node.
     *
     * Note: This is over write any existing keys.
     * @param keys List of Application Keys to set.
     */
    internal fun assignAppKeys(keys: List<ApplicationKey>) {
        assignAppKeyIndexes(keys.map { it.index }.toMutableList())
    }


    /**
     * Sets the given list of Application Keys to the Node.
     *
     * Note: This overwrites any existing keys.
     * @param keys List of Application Keys to set.
     */
    internal fun assignAppKeyIndexes(keys: List<KeyIndex>) {
        _appKeys = keys.map { NodeKey(it, false) }
            .toMutableList()
            .apply { sortBy { it.index } }
        network?.updateTimestamp()
    }


    /**
     * Adds an Element to a node.
     *
     * @param element Element to be added.
     */
    internal fun add(element: Element) {
        val index = elements.size
        _elements.add(element)
        element.parentNode = this
        element.index = index
    }

    /**
     * Adds given list of Elements to the Node.
     *
     * @param elements List of Elements to be added.
     */
    internal fun add(elements: List<Element>) {
        elements.forEach(::add)
    }

    /**
     * Sets the given list of Elements to the Node.
     *
     * Apart from simply replacing the Elements, this method copies properties of matching models
     * from the old model to the new one. If at least one Model in the new Element was found in the
     * new Element, the name of the Element is also copied.
     *
     * @param elements List of Elements to set.
     */
    internal fun set(elements: List<Element>) {
        for (e in 0 until minOf(this.elements.size, elements.size)) {
            val oldElement = this.elements[e]
            val newElement = elements[e]
            for (m in 0 until minOf(oldElement.models.size, newElement.models.size)) {
                val oldModel = oldElement.models[m]
                val newModel = newElement.models[m]
                if (oldModel.modelId.id == newModel.modelId.id) {
                    newModel.copyProperties(oldModel)
                    // If at least one Model matches, assume the Element didn't change much and copy
                    // the name of it.
                    oldElement.name?.let { newElement.name = it }
                }
            }
        }
        _elements.forEach { element ->
            element.parentNode = null
            element.index = 0
        }
        _elements.clear()
        add(elements)
    }

    /**
     * Applies the result of Composition Data Status message to the Node.
     *
     * This method does nothing if the Node already was configured or the Composition Data Status
     * does not have Page 0.
     *
     * @param compositionData The result of Config Composition Data get with Page 0.
     */
    internal fun apply(compositionData: ConfigCompositionDataStatus) {
        val page0 = requireNotNull(compositionData.page as? Page0)
        companyIdentifier = page0.companyIdentifier
        productIdentifier = page0.productIdentifier
        versionIdentifier = page0.versionIdentifier
        replayProtectionCount = page0.minimumNumberOfReplayProtectionList
        // Don't override features if they already were known.
        // Accurate features states could have been acquired by reading each feature state, while
        // the Page 0 of the Composition Data contains only Supported/Not supported

        // And set the Elements received.
        set(page0.elements)
        network?.updateTimestamp()
    }

    /**
     * Checks if the given addresses used by the specified number of elements overlaps with the
     * address range used by the node.
     *
     * @param address       Desired unicast address.
     * @param count         Number of elements.
     * @return true if the address range is in use.
     */
    fun overlaps(address: UnicastAddress, count: Int) = try {
        !(_primaryUnicastAddress + (elementsCount - 1) < address ||
                _primaryUnicastAddress > address + (count - 1))
    } catch (e: IllegalArgumentException) {
        true
    }

    /**
     * Checks if an element in the node uses this address.
     *
     * @param address Unicast address.
     * @return true if the given address is in use by any of the elements
     */
    fun containsElementWithAddress(address: Address) = elements.any {
        it.unicastAddress.address == address
    }

    /**
     * Checks if an element in the node uses this address.
     *
     * @param address Unicast address.
     * @return true if the given address is in use by any of the elements or false if the address is
     *         not a unicast address or the address is not in use by any of the elements.
     */
    fun containsElementWithAddress(address: MeshAddress) = if (address is UnicastAddress)
        containsElementWithAddress(address)
    else false

    /**
     * Checks if an element in the node uses this address.
     *
     * @param address Unicast address.
     * @return true if the given address is in use by any of the elements
     */
    fun containsElementWithAddress(address: UnicastAddress) = elements.any {
        it.unicastAddress == address
    }

    /**
     * Checks if an element in the node has a Unicast Address from the given range.
     *
     * @param range Unicast Range.
     * @return true if given range overlaps with the node's address range.
     */
    fun containsElementsWithAddress(range: UnicastRange) = unicastRange.overlaps(range)

    /**
     * Returns the element with the given address.
     *
     * @param address Address of the element.
     * @return Element or null if not found.
     * @throws IllegalArgumentException If the address is invalid.
     */
    fun element(address: UShort) = elements.firstOrNull { it.unicastAddress.address == address }

    /**
     * Returns the element with the given address
     *
     * @param address Address of the element.
     * @return Element or null if not found.
     */
    fun element(address: UnicastAddress) = element(address.address)

    /**
     * Returns the element with the given address
     *
     * @param address Address of the element.
     * @return Element or null if not found.
     */
    fun element(address: MeshAddress) = if (address is UnicastAddress)
        element(address.address)
    else null

    /**
     * Returns the model with the given model ID.
     *
     * @param modelId Model ID.
     * @return Model or null if not found.
     */
    fun model(modelId: ModelId) = model(modelId = modelId.id)

    /**
     * Returns the model with the given model ID.
     *
     * @param modelId Model ID.
     * @return Model or null if not found.
     */
    fun model(modelId: UInt) = elements
        .flatMap { it.models }
        .firstOrNull { it.modelId.id == modelId }

    /**
     * Checks if the given Application Key known by the node.
     *
     * Note: This is based on the key index.
     *
     * @param key Application Key.
     * @return true if the key is known by the node or false otherwise.
     */
    fun knows(key: ApplicationKey) = knowsApplicationKeyIndex(index = key.index)

    /**
     * Checks if the given Application Key index known by the node.
     *
     * @param index Application Key index.
     * @return true if the key is known by the node or false otherwise.
     */
    fun knowsApplicationKeyIndex(index: KeyIndex) = appKeys.any { it.index == index }

    /**
     * Checks if the given Network Key known by the node.
     *
     * Note: This is based on the key index.
     *
     * @param key Network Key.
     * @return true if the key is known by the node or false otherwise.
     */
    fun knows(key: NetworkKey) = knowsNetworkKeyIndex(index = key.index)

    /**
     * Checks if the given Network Key index known by the node.
     *
     * @param index Network Key index.
     * @return true if the key is known by the node or false otherwise.
     */
    fun knowsNetworkKeyIndex(index: KeyIndex) = netKeys.any { it.index == index }

    /**
     *  Returns whether the Node has at least one Application Key bound
     *  to the given Network Key.
     *
     *  @param key Network Key to check binding.
     *  @returns true if at least one Application Key known to this Node is bound to the given
     *  Network Key.
     */
    fun containsApplicationKeyBoundToNetworkKey(key: NetworkKey) =
        applicationKeys.contains(networkKey = key)

    /**
     * Checks if the given Application Key is contained in any of the elements of the node.
     * This allows to find out if a particular key is bound to any of its models.
     *
     * @param key Application Key.
     * @return true if the key is known by the node or false otherwise.
     */
    fun containsModelsBoundToApplicationKey(key: ApplicationKey) = elements.any {
        it.contains(key = key)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Node

        if (uuid != other.uuid) return false
        if (!deviceKey.contentEquals(other.deviceKey)) return false
        if (_primaryUnicastAddress != other._primaryUnicastAddress) return false
        if (_elements != other._elements) return false
        if (_netKeys != other._netKeys) return false
        if (_appKeys != other._appKeys) return false
        if (name != other.name) return false
        if (security != other.security) return false
        if (configComplete != other.configComplete) return false
        if (companyIdentifier != other.companyIdentifier) return false
        if (productIdentifier != other.productIdentifier) return false
        if (versionIdentifier != other.versionIdentifier) return false
        if (replayProtectionCount != other.replayProtectionCount) return false
        if (features != other.features) return false
        if (secureNetworkBeacon != other.secureNetworkBeacon) return false
        if (networkTransmit != other.networkTransmit) return false
        if (relayRetransmit != other.relayRetransmit) return false
        if (defaultTTL != other.defaultTTL) return false
        if (excluded != other.excluded) return false
        if (heartbeatPublication != other.heartbeatPublication) return false
        if (heartbeatSubscription != other.heartbeatSubscription) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uuid.hashCode()
        result = 31 * result + deviceKey.contentHashCode()
        result = 31 * result + _primaryUnicastAddress.hashCode()
        result = 31 * result + _elements.hashCode()
        result = 31 * result + _netKeys.hashCode()
        result = 31 * result + _appKeys.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + security.hashCode()
        result = 31 * result + configComplete.hashCode()
        result = 31 * result + (companyIdentifier?.hashCode() ?: 0)
        result = 31 * result + (productIdentifier?.hashCode() ?: 0)
        result = 31 * result + (versionIdentifier?.hashCode() ?: 0)
        result = 31 * result + (replayProtectionCount?.hashCode() ?: 0)
        result = 31 * result + features.hashCode()
        result = 31 * result + (secureNetworkBeacon?.hashCode() ?: 0)
        result = 31 * result + (networkTransmit?.hashCode() ?: 0)
        result = 31 * result + (relayRetransmit?.hashCode() ?: 0)
        result = 31 * result + defaultTTL.hashCode()
        result = 31 * result + excluded.hashCode()
        result = 31 * result + (heartbeatPublication?.hashCode() ?: 0)
        result = 31 * result + (heartbeatSubscription?.hashCode() ?: 0)
        return result
    }
}

/**
 * Returns the list of elements from a list of nodes.
 *
 * @receiver List of nodes.
 * @return List of elements.
 */
fun List<Node>.elements() = flatMap { it.elements }

/**
 * Returns an element with the given address from a list of nodes.
 *
 * @receiver List of nodes.
 * @param address Address of the element.
 * @return Element or null if not found.
 */
fun List<Node>.element(address: UnicastAddress) = elements().firstOrNull {
    it.unicastAddress == address
}

/**
 * Returns the list of addresses from a list of nodes.
 *
 * @receiver List of nodes.
 * @return List of addresses.
 */
fun List<Node>.addresses() = flatMap { it.addresses }