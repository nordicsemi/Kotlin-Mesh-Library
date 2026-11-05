package no.nordicsemi.android.nrfmesh.feature.nodes.node

import no.nordicsemi.kotlin.mesh.core.model.Address

/**
 * ClickableNodeInfoItem is a sealed class that represents the clickable settings item in the node
 * screen. This is used to highlight the selected item in the nodes info list ui.
 */
sealed class ClickableNodeInfoItem {
    data object NetworkKeys : ClickableNodeInfoItem()
    data object ApplicationKeys : ClickableNodeInfoItem()
    data class Element(val address: Address) : ClickableNodeInfoItem()
    data object Scenes : ClickableNodeInfoItem()
}