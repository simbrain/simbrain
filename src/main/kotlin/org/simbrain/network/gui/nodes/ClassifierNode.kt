package org.simbrain.network.gui.nodes

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.dialogs.getTrainingDialog
import org.simbrain.network.smile.SmileClassifier
import org.simbrain.util.StandardDialog
import org.simbrain.util.display
import org.simbrain.util.widgets.bezierArrow
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

class SmileClassifierNode(networkPanel: NetworkPanel, private val smileClassifier: SmileClassifier):
    SubnetworkNode(networkPanel, smileClassifier) {

    val arrow =  bezierArrow {
        color = NetworkPreferences.weightMatrixArrowColor
    }.also { addChild(it) }

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            add(JMenuItem("Set Properties / Train ...").apply {
                addActionListener {
                    propertyDialog.display()
                }
            })
            addSeparator()
        }

    override val propertyDialog: StandardDialog
        get() = smileClassifier.getTrainingDialog()

    override fun layoutChildren() {
        super.layoutChildren()
        arrow.layout(
            smileClassifier.inputNeuronGroup.sides,
            smileClassifier.outputNeuronGroup.sides,
            false
        )
    }
}

