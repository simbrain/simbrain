package org.simbrain.network.gui.nodes.subnetworkNodes

import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.getCnnTrainingDialog
import org.simbrain.network.gui.nodes.SubnetworkNode
import org.simbrain.network.subnetworks.ConvolutionalNeuralNetwork
import org.simbrain.util.StandardDialog
import org.simbrain.util.createAction
import javax.swing.JPopupMenu

/**
 * PNode representation of a convolutional neural network subnetwork.
 */
class ConvolutionalNeuralNetworkNode(
    networkPanel: NetworkPanel,
    private val cnn: ConvolutionalNeuralNetwork
) : SubnetworkNode(networkPanel, cnn) {

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            addDefaultSubnetActions()
            addSeparator()
            add(createAction("Train...") {
                propertyDialog.run {
                    pack()
                    setLocationRelativeTo(null)
                    isVisible = true
                }
            })
            addSeparator()
            add(createAction("Add current data to training set") {
                cnn.trainingSet.inputs.add(cnn.inputTensorLayer.activations.copyOf())
                cnn.trainingSet.targets.add(cnn.outputArray.activationArray.copyOf())
            })
            add(createAction("Add current data to testing set") {
                cnn.testingSet.inputs.add(cnn.inputTensorLayer.activations.copyOf())
                cnn.testingSet.targets.add(cnn.outputArray.activationArray.copyOf())
            })
        }

    override val propertyDialog: StandardDialog
        get() = with(networkPanel) { cnn.getCnnTrainingDialog() }
}
