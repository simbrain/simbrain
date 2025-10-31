package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.dialogs.getTrainingDialog
import org.simbrain.network.smile.ClassifierNetwork
import org.simbrain.util.*
import org.simbrain.util.widgets.bezierArrow
import org.simbrain.workspace.gui.SimbrainDesktop
import javax.swing.JPopupMenu

class SmileClassifierNode(networkPanel: NetworkPanel, private val smileClassifier: ClassifierNetwork):
    SubnetworkNode(networkPanel, smileClassifier) {

    val arrow =  bezierArrow {
        color = NetworkPreferences.weightMatrixArrowColor
        padding {
            head = arrowSize
            tail = 0.0
        }
    }.also { addChild(it) }

    val sourceNode by lazy { networkPanel.getNode(smileClassifier.inputNeuronGroup) }
    val targetNode by lazy { networkPanel.getNode(smileClassifier.outputNeuronGroup) }

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            add(networkPanel.createAction(name = "Edit ${smileClassifier.displayName}...") {
                propertyDialog.display()
            })
            add(networkPanel.createAction(name = "Train...") {
                propertyDialog.display()
            })
            addSeparator()
            add(createAction("Visualize Classifier") {
                SimbrainDesktop.workspace.launch(Dispatchers.Default) {
                    SimbrainDesktop.createClassifierProjectionPlot(smileClassifier)
                }
            })
        }

    override val propertyDialog: StandardDialog
        get() = with(networkPanel) {smileClassifier.getTrainingDialog()}

    override fun layoutChildren() {
        super.layoutChildren()
        arrow.layout(
            sourceNode.globalFullBounds.outlines,
            targetNode.globalFullBounds.outlines,
            false
        )
    }
}

