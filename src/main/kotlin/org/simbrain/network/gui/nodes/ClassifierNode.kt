package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.simbrain.network.core.NeuronCollection
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.dialogs.createTreeViewer
import org.simbrain.network.gui.dialogs.getTrainingDialog
import org.simbrain.network.smile.ClassifierNetwork
import org.simbrain.network.smile.classifiers.RandomForestClassifier
import org.simbrain.util.*
import org.simbrain.util.widgets.bezierArrow
import org.simbrain.workspace.gui.SimbrainDesktop
import javax.swing.JPopupMenu

class SmileClassifierNode(networkPanel: NetworkPanel, private val smileClassifier: ClassifierNetwork):
    SubnetworkNode(networkPanel, smileClassifier) {

    val arrow =  bezierArrow {
        color = NetworkPreferences.connectorArrowColor
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
            (smileClassifier.classifier as? RandomForestClassifier)?.let { forest ->
                addSeparator()
                add(createAction("Show trees...") {
                    if (forest.model == null) {
                        showMessage("Train the random forest first.")
                        return@createAction
                    }
                    fun names(group: NeuronCollection, fallback: String) = group.neuronList.mapIndexed { i, neuron ->
                        neuron.label?.takeIf { it.isNotBlank() }?.replace('\n', ' ') ?: "$fallback ${i + 1}"
                    }
                    forest.createTreeViewer(
                        names(smileClassifier.inputNeuronGroup, "input"),
                        names(smileClassifier.outputNeuronGroup, "class")
                    ).display()
                })
            }
            addSeparator()
            val plotName = if (smileClassifier.inputSize == 2) "Plot Decision Regions..." else "Plot Data..."
            add(createAction(plotName) {
                SimbrainDesktop.workspace.launch(Dispatchers.Default) {
                    SimbrainDesktop.createClassifierProjectionPlot(smileClassifier)
                }
            })
        }

    override val propertyDialog: StandardDialog
        get() = with(networkPanel) {smileClassifier.getTrainingDialog()}

    fun updateArrowColorFromPreferences() {
        arrow.updateColorFromPreferences()
        layoutChildren()
    }

    override fun layoutChildren() {
        super.layoutChildren()
        arrow.layout(
            sourceNode.globalFullBounds.outlines,
            targetNode.globalFullBounds.outlines,
            false
        )
    }
}

