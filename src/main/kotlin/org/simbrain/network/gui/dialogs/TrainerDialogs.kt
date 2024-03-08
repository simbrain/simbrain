package org.simbrain.network.gui.dialogs

import net.miginfocom.swing.MigLayout
import org.simbrain.network.NetworkComponent
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.nodes.SRNNode
import org.simbrain.network.subnetworks.LMSNetwork
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.trainers.SupervisedNetwork
import org.simbrain.network.trainers.UnsupervisedNetwork
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.table.MatrixDataFrame
import org.simbrain.util.table.createAdvanceRowAction
import org.simbrain.util.table.createApplyAction
import org.simbrain.util.table.createApplyAndAdvanceAction
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.JTabbedPane


/**
 * Generic training dialog for supervised learning.
 */
context(NetworkPanel)
fun SupervisedNetwork.getSupervisedTrainingDialog(): StandardDialog {
    return StandardDialog().apply {

        title = "Train Network"
        contentPane = ResizableTabbedPane()

        // Edit Trainer Properties
        val trainerProps = AnnotatedPropertyEditor(trainer)
        val trainerPropsPanel = trainerProps.createApplyPanel()
        (contentPane as JTabbedPane).addTab("Trainer Properties", trainerPropsPanel)

        // Run training algorithm
        val runControls = JPanel()
        runControls.layout = MigLayout("gap 0px 0px, ins 0")
        val trainerControls = TrainerControls(trainer, this@NetworkPanel)
        val inputs = MatrixEditor(trainingSet.inputs)
        inputs.toolbar.addSeparator()
        inputs.toolbar.add(
            inputs.table.createApplyAction("Apply Inputs") { selectedRow ->
                with(network) { trainer.applyInputs(selectedRow) }
            }
        )
        inputs.toolbar.add(inputs.table.createAdvanceRowAction())
        inputs.toolbar.add(inputs.table.createApplyAndAdvanceAction {
            with(network) { trainer.applyInputs(inputs.table.selectedRow) }
        })
        val targets = MatrixEditor(trainingSet.targets)
        val addRemoveRows = AddRemoveRows(inputs.table, targets.table)
        trainer.events.beginTraining.on {
            trainingSet = MatrixDataset(
                (inputs.table.model as MatrixDataFrame).data,
                (targets.table.model as MatrixDataFrame).data
            )
        }
        runControls.add(JSeparator(), "span, growx, wrap")
        runControls.add(trainerControls, "span, growx, wrap")
        runControls.add(JSeparator(), "span, growx, wrap")
        runControls.add(JLabel("Inputs"))
        runControls.add(JLabel("Targets"), "wrap")
        runControls.add(inputs)
        runControls.add(targets, "wrap")
        runControls.add(JLabel("Add / Remove rows:"), "split 2")
        runControls.add(addRemoveRows)
        (contentPane as JTabbedPane).addTab("Run Trainer", runControls)

        addCommitTask {
            trainerProps.commitChanges()
            trainingSet = MatrixDataset(
                (inputs.table.model as MatrixDataFrame).data,
                (targets.table.model as MatrixDataFrame).data
            )
        }
    }
}

context(NetworkPanel)
fun getUnsupervisedTrainingPanel(unsupervisedNetwork: UnsupervisedNetwork): StandardDialog {
    return StandardDialog().apply {

        title = "Train Network"

        // Run training algorithm
        val runControls = JPanel()
        runControls.layout = MigLayout("gap 0px 0px, ins 0")
        val inputs = MatrixEditor(unsupervisedNetwork.inputData)
        inputs.toolbar.addSeparator()
        inputs.toolbar.add(
            inputs.table.createApplyAction("Apply Inputs") { selectedRow ->
                unsupervisedNetwork.inputLayer.setActivations(inputs.table.model.getCurrentDoubleRow().toDoubleArray())
            }
        )
        inputs.toolbar.add(inputs.table.createAdvanceRowAction())
        inputs.toolbar.add(inputs.table.createApplyAndAdvanceAction {
            unsupervisedNetwork.inputLayer.setActivations(inputs.table.model.getCurrentDoubleRow().toDoubleArray())
            with(network) {
                unsupervisedNetwork.update()
            }
        })
        runControls.add(inputs)

        contentPane = runControls
    }
}

fun NetworkPanel.showSRNCreationDialog(): StandardDialog {

    val creator = SRNNetwork.SRNCreator(
        network.placementManager.lastClickedLocation
    )
    return creator.createEditorDialog {
        network.addNetworkModelAsync(creator.create())
    }

}

// fun main() {
//     val networkComponent = NetworkComponent("")
//     val np = NetworkPanel(networkComponent)
//     val result = with(networkComponent.network) {
//         val lmsNet = LMSNetwork(this, 5, 5)
//         addNetworkModelAsync(lmsNet)
//         lmsNet
//     }
//     LMSNetworkNode(np,result ).propertyDialog.run { makeVisible() }
// }

fun main() {
    val networkComponent = NetworkComponent("")
    val np = NetworkPanel(networkComponent)
    val result = with(networkComponent.network) {
        val srnNetwork = SRNNetwork(5, 5)
        addNetworkModelAsync(srnNetwork)
        srnNetwork
    }
    SRNNode(np, result).propertyDialog?.display()
}


/**
 * Creation dialog for [LMSNetwork]
 */
fun NetworkPanel.showLMSCreationDialog(): StandardDialog {

    val creator = LMSNetwork.LMSCreator(
        network.placementManager.lastClickedLocation
    )
    return creator.createEditorDialog {
        network.addNetworkModelAsync(creator.create())
    }

}