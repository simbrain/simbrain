package org.simbrain.network.gui.nodes

import org.simbrain.network.core.InfoText
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.StandardDialog
import javax.swing.JPopupMenu

class TextInfoNode(netPanel: NetworkPanel, text: InfoText) : TextNode(netPanel, text) {

    override val isDraggable = false

    override val propertyDialog: StandardDialog? = null

    override val contextMenu: JPopupMenu? = null
}
