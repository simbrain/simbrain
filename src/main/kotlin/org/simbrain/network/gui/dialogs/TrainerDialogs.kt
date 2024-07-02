package org.simbrain.network.gui.dialogs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.nodes.SRNNode
import org.simbrain.network.subnetworks.LMSNetwork
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.network.trainers.MatrixDataset
import org.simbrain.network.trainers.SupervisedNetwork
import org.simbrain.network.trainers.SupervisedTrainer
import org.simbrain.network.trainers.UnsupervisedNetwork
import org.simbrain.util.*
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.table.MatrixDataFrame
import org.simbrain.util.table.createAdvanceRowAction
import org.simbrain.util.table.createApplyAction
import org.simbrain.util.table.createApplyAndAdvanceAction
import org.simbrain.util.widgets.ToggleButton
import java.awt.Cursor
import javax.swing.*


/**
 * Generic training dialog for supervised learning.
 */
context(NetworkPanel)
fun <SN> SN.getSupervisedTrainingDialog(): StandardDialog where SN: SupervisedNetwork, SN: NetworkModel {
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
        val trainerControls = TrainerControls(trainer as SupervisedTrainer<SN>, this@getSupervisedTrainingDialog, this@NetworkPanel)
        val inputs = MatrixEditor(trainingSet.inputs, trainingSet.inputRowNames, trainingSet.inputColumnNames)
        inputs.toolbar.addSeparator()
        inputs.toolbar.add(
            inputs.table.createApplyAction("Apply Inputs") { selectedRow ->
                with(network) {
                    inputLayer.activations = trainingSet.inputs.rowVectorTransposed(selectedRow)
                    this@SN.update()
                }
            }
        )
        inputs.toolbar.add(inputs.table.createAdvanceRowAction())
        inputs.toolbar.add(inputs.table.createApplyAndAdvanceAction {
            with(network) {
                inputLayer.activations = trainingSet.inputs.rowVectorTransposed(inputs.table.selectedRow)
                this@SN.update()
            }
        })
        val targets = MatrixEditor(trainingSet.targets, trainingSet.targetRowNames, trainingSet.targetColumnNames)
        val addRemoveRows = AddRemoveRows(inputs.table, targets.table)
        trainer.events.beginTraining.on {
            trainingSet = MatrixDataset(
                (inputs.table.model as MatrixDataFrame).data,
                (targets.table.model as MatrixDataFrame).data,
                trainingSet.inputRowNames,
                trainingSet.targetRowNames,
                trainingSet.inputColumnNames,
                trainingSet.targetColumnNames
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
                (targets.table.model as MatrixDataFrame).data,
                trainingSet.inputRowNames,
                trainingSet.targetRowNames,
                trainingSet.inputColumnNames,
                trainingSet.targetColumnNames
            )
        }
    }
}

context(NetworkPanel)
fun getUnsupervisedTrainingPanel(unsupervisedNetwork: UnsupervisedNetwork, trainAction: context(Network)() -> Unit = {}): StandardDialog {
    return StandardDialog().apply dialog@ {

        title = "Train Network"

        val mainPanel = JPanel().apply {
            layout = MigLayout("gap 0px 0px, ins 0")
        }

        val trainer = unsupervisedNetwork.trainer

        val runControls = JPanel().apply { layout = MigLayout("nogrid") }
        val trainerPanel = AnnotatedPropertyEditor(trainer)
        val trainerApplyPanel = trainerPanel.createApplyPanel()
        mainPanel.add(trainerApplyPanel, "wrap")
        val runAction = createAction(
            name = "Run",
            description = "Run training algorithm",
            iconPath = "menu_icons/Play.png",
        ) {
            with(network) { trainer.startTraining(unsupervisedNetwork) }
        }
        val stopAction = createAction(
            name = "Stop",
            description = "Stop training algorithm",
            iconPath = "menu_icons/Stop.png",
        ) {
            trainer.stopTraining()
        }
        runControls.add(ToggleButton(listOf(runAction, stopAction)).apply {
            setAction("Run")
            trainer.events.beginTraining.on {
                this@dialog.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                setAction("Stop")
            }
            trainer.events.endTraining.on {
                this@dialog.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                setAction("Run")
            }
        })

        val stepAction = createAction(
            name = "Step",
            description = "Iterate training once",
            iconPath = "menu_icons/Step.png",
        ) {
            with(network) {
                trainer.events.beginTraining.fire()
                trainer.trainOnce(unsupervisedNetwork)
                trainer.events.endTraining.fire()
            }
        }

        runControls.add(JButton(stepAction))

        val resetAction = createAction(
            name = "Reset",
            description = "Reset iteration and randomize network",
            iconPath = "menu_icons/Reset.png",
        ) {
            unsupervisedNetwork.randomize()
            trainer.iteration = 0
        }
        val resetButton = JButton(resetAction)
        resetButton.hideActionText = true
        runControls.add(resetButton)

        val labelPanel = LabelledItemPanel()
        val iterationsLabel = JLabel(trainer.iteration.toString())
        labelPanel.addItem("Iterations:", iterationsLabel)
        runControls.add(labelPanel, "wrap")

        trainer.events.progressUpdated.on(Dispatchers.Swing, wait = true) {
            iterationsLabel.text = "" + trainer.iteration
        }

        runControls.layout = MigLayout("gap 0px 0px, ins 0")

        val trainOnCurrentPatternButton = JButton(with(network) { unsupervisedNetwork.createTrainOnPatternAction()})
        trainOnCurrentPatternButton.hideActionText = true
        runControls.add(trainOnCurrentPatternButton)

        mainPanel.add(runControls, "wrap")

        // Run training algorithm
        val inputData = JPanel()
        inputData.layout = MigLayout("gap 0px 0px, ins 0")
        val inputs = MatrixEditor(unsupervisedNetwork.inputData)
        inputs.toolbar.addSeparator()
        inputs.toolbar.add(
            inputs.table.createApplyAction("Apply Inputs") { selectedRow ->
                unsupervisedNetwork.inputLayer.applyActivations(inputs.table.model.getCurrentDoubleRow().toDoubleArray())
            }
        )
        inputs.toolbar.add(inputs.table.createAdvanceRowAction())
        inputs.toolbar.add(inputs.table.createApplyAndAdvanceAction {
            unsupervisedNetwork.inputLayer.applyActivations(inputs.table.model.getCurrentDoubleRow().toDoubleArray())
        })
        inputData.add(inputs)
        mainPanel.add(inputData)

        contentPane = mainPanel
    }
}

fun NetworkPanel.showSRNCreationDialog(): StandardDialog {

    val creator = SRNNetwork.SRNCreator(
        network.placementManager.lastClickedLocation
    )
    return creator.createEditorDialog {
        network.addNetworkModel(creator.create())
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
        addNetworkModel(srnNetwork)
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
        network.addNetworkModel(creator.create())
    }

}

context(Network)
fun UnsupervisedNetwork.createTrainOnPatternAction() = createAction(
    name = "Train on current pattern...",
    description = "Train network on current pattern for specified number of iterations.",
    iconPath = "menu_icons/BatchPlay.png"
) {
    val iterations: Int? = showNumericInputDialog("Iterations: ", 100)?.toInt()
    if (iterations != null) {
        runWithProgressWindow(iterations, batchSize = 10) {
            trainOnCurrentPattern()
        }
    }
}