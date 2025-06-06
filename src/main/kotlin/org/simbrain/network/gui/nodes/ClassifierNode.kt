package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.dialogs.getTrainingDialog
import org.simbrain.network.smile.ClassifierNetwork
import org.simbrain.util.StandardDialog
import org.simbrain.util.createAction
import org.simbrain.util.createClassifierProjectionPlot
import org.simbrain.util.display
import org.simbrain.util.widgets.bezierArrow
import org.simbrain.workspace.gui.SimbrainDesktop
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

class SmileClassifierNode(networkPanel: NetworkPanel, private val smileClassifier: ClassifierNetwork):
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
            smileClassifier.inputNeuronGroup.sides,
            smileClassifier.outputNeuronGroup.sides,
            false
        )
    }
}

