package org.simbrain.util.uisnapshot

import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.*
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.getCnnTrainingDialog
import java.awt.Component
import javax.swing.SwingUtilities

/**
 * The CNN training dialog (Train CNN), used as the reference for the spacing + table-grid
 * polishing pass. Builds a minimal CNN pipeline (Tensor -> Flatten -> Dense) so the dialog's
 * data-set panel, trainer controls, and tables render exactly as in the app.
 */
class CnnTrainerDialogSnapshot : UiSnapshotDef {
    override val name = "cnn_trainer_dialog"

    override fun build(): Component {
        val network = Network()
        val component = NetworkComponent("snapshot", network)
        val panel = NetworkPanel(component)

        val inputTensorLayer = TensorLayer(TensorShape(2, 2, 1)).apply { isClamped = true }
        val flatArray = NeuronArray(4)
        val outputArray = NeuronArray(2).apply { biases.fill(0.0) }
        FlattenConnector(inputTensorLayer, flatArray)
        WeightMatrix(flatArray, outputArray)
        val cnn = network.addConvolutionalNeuralNetwork(inputTensorLayer, outputArray)

        lateinit var dialog: Component
        SwingUtilities.invokeAndWait {
            dialog = with(panel) { cnn.getCnnTrainingDialog() }
        }
        return dialog
    }
}
