package org.simbrain.util.uisnapshot

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkTextObject
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.text.TextDialog
import org.simbrain.network.gui.nodes.TextNode
import java.awt.Component

class TextDialogSnapshot : UiSnapshotDef {
    override val name = "text_dialog"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component)
        val textObject = NetworkTextObject("Sample").apply {
            fontName = "SansSerif"
            fontSize = 18
            isBold = true
        }
        val node = TextNode(panel, textObject)
        return TextDialog(listOf(node))
    }
}
