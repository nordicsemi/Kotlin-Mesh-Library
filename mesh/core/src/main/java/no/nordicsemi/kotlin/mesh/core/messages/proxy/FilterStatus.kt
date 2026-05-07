@file:Suppress("MemberVisibilityCanBePrivate")

package no.nordicsemi.kotlin.mesh.core.messages.proxy

import no.nordicsemi.kotlin.data.getUShort
import no.nordicsemi.kotlin.data.toByteArray
import no.nordicsemi.kotlin.mesh.core.ProxyFilterType

/**
 * The Filter Status message is sent by a Proxy Server to report the status of the Proxy Filter.
 *
 * @property filterType Filter type.
 * @property listSize   Size of the filter list.
 */

class FilterStatus(
    val filterType: ProxyFilterType,
    val listSize: UShort
) : ProxyConfigurationMessage {
    override val opCode: UByte = Initializer.opCode
    override val parameters: ByteArray
        get() = byteArrayOf(filterType.type.toByte()) + listSize.toByteArray()

    override fun toString(): String =
        "FilterStatus(filterType: $filterType, listSize: $listSize)"

    companion object Initializer : ProxyConfigurationMessageInitializer {
        override val opCode: UByte = 0x03u

        override fun init(parameters: ByteArray?) = parameters?.takeIf {
            it.size == 3
        }?.let {
            val type = ProxyFilterType.from(it[0].toUByte())
            FilterStatus(filterType = type, listSize = it.getUShort(offset = 1))
        }
    }
}
