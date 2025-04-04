package org.simbrain.network.gui.dialogs

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Network
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.addSubnetworkAction
import org.simbrain.network.gui.nodes.subnetworkNodes.BackpropNetworkNode
import org.simbrain.network.subnetworks.BackpropNetwork
import org.simbrain.network.subnetworks.SRNNetwork
import org.simbrain.network.trainers.*
import org.simbrain.util.*
import org.simbrain.util.table.MatrixDataFrame
import org.simbrain.util.table.createAdvanceRowAction
import org.simbrain.util.table.createApplyAction
import org.simbrain.util.table.createApplyAndAdvanceAction
import org.simbrain.util.widgets.ToggleButton
import java.awt.Cursor
import javax.swing.*

class DataSetPanel(val dataSet: MatrixDataset, applyAction: suspend DataSetPanel.(selectedRow: Int) -> Unit, applyAndAdvanceAction: suspend DataSetPanel.(selectedRow: Int) -> Unit): JPanel() {

    val rowErrorJLabel = JLabel("")

    val inputs = MatrixEditor(dataSet.inputs, dataSet.inputRowNames, dataSet.inputColumnNames).apply {
        toolbar.addSeparator()
        toolbar.add(
            table.createApplyAction("Apply Inputs") { applyAction(it) }
        )
        toolbar.add(table.createAdvanceRowAction())
        toolbar.add(table.createApplyAndAdvanceAction { applyAndAdvanceAction(it) })
        toolbar.addSeparator()
        toolbar.add(rowErrorJLabel)
    }

    val inputData get() = (inputs.table.model as MatrixDataFrame).data

    val targets = MatrixEditor(dataSet.targets, dataSet.targetRowNames, dataSet.targetColumnNames)

    val targetData get() = (targets.table.model as MatrixDataFrame).data

    val addRemoveRows = AddRemoveRows(inputs.table, targets.table)

    init {
        layout = MigLayout("gap 0px 0px, ins 0")
        add(JSeparator(), "span, growx, wrap")
        add(JLabel("Inputs"))
        add(JLabel("Targets"), "wrap")
        add(inputs)
        add(targets, "wrap")
        add(JLabel("Add / Remove rows:"), "split 2")
        add(addRemoveRows)
    }

    fun exportMatrixDataSet() = MatrixDataset(
        (inputs.table.model as MatrixDataFrame).data,
        (targets.table.model as MatrixDataFrame).data,
        dataSet.inputRowNames,
        dataSet.targetRowNames,
        dataSet.inputColumnNames,
        dataSet.targetColumnNames
    )

}


/**
 * Generic training dialog for supervised learning.
 */
context(NetworkPanel)
fun <SN> SN.getSupervisedTrainingDialog(): StandardDialog where SN: SupervisedNetwork, SN: NetworkModel {
    val supervisedNetwork = this
    return StandardDialog().apply {

        title = "Train Network"

        // Run training algorithm
        val runControls = JPanel()
        runControls.layout = MigLayout("gap 0px 0px, ins 0")
        val trainer = when (supervisedNetwork) {
            is SRNNetwork -> SRNTrainer(network, supervisedNetwork)
            is BackpropNetwork -> BackpropTrainer(network, supervisedNetwork)
            is SupervisedModel -> SupervisedModelTrainer(network, supervisedNetwork)
            else -> throw IllegalArgumentException("Unsupported network type: ${supervisedNetwork::class.simpleName}")
        } as SupervisedTrainer<SN>
        val trainerControls = TrainerControls(trainer, supervisedNetwork, this@NetworkPanel)

        suspend fun DataSetPanel.commonApplyAction(selectedRow: Int) {
            with(network) {
                inputLayer.setActivations(inputData.row(selectedRow))
                this@SN.forwardPass()
                trainerConfig.lossFunction.scalarLoss(
                    outputLayer.activations,
                    targetData.row(selectedRow).toMatrix()
                ).also { rowErrorJLabel.text = "${trainerConfig.lossFunction.shortName}: ${it.format(4)}" }
            }
        }

        fun createDataSetPanel(dataSet: MatrixDataset) = DataSetPanel(
            dataSet,
            applyAction = { selectedRow -> commonApplyAction(selectedRow) },
            applyAndAdvanceAction = { selectedRow -> commonApplyAction(selectedRow) }
        )

        val trainingDataSetPanel = createDataSetPanel(trainingSet)
        val testingDataSetPanel = createDataSetPanel(testingSet)

        fun syncDataSet() {
            trainingSet = trainingDataSetPanel.exportMatrixDataSet()
            testingSet = testingDataSetPanel.exportMatrixDataSet()
        }

        val dataSetTabPane = JTabbedPane().apply {
            addTab("Training Set", trainingDataSetPanel)
            addTab("Testing Set", testingDataSetPanel)
        }

        trainer.events.beginTraining.on(Dispatchers.Default) { syncDataSet() }
        runControls.add(trainerControls, "span, growx, wrap")
        runControls.add(JSeparator(), "span, growx, wrap")
        runControls.add(dataSetTabPane, "wrap")

        addCommitTask { syncDataSet() }

        contentPane = runControls
    }
}

context(NetworkPanel)
fun getUnsupervisedTrainingPanel(unsupervisedNetwork: UnsupervisedNetwork, trainAction: context(Network)() -> Unit = {}): StandardDialog {
    return StandardDialog().apply dialog@ {

        title = "Train Network"

        print("unsupervisedNetwork: $unsupervisedNetwork")

        val mainPanel = JPanel().apply {
            layout = MigLayout("gap 0px 0px, ins 0")
        }

        val trainer = unsupervisedNetwork.trainer

        val runControls = JPanel().apply { layout = MigLayout("nogrid") }
        val runAction = createAction(
            name = "Run",
            description = "Run training algorithm",
            iconPath = "menu_icons/Play.png",
        ) {
            with(network) {
                launch {
                    trainer.startTraining(unsupervisedNetwork)
                }
            }
        }
        val stopAction = createAction(
            name = "Stop",
            description = "Stop training algorithm",
            iconPath = "menu_icons/Stop.png",
        ) {
            launch {
                trainer.stopTraining()
            }
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
                launch {
                    trainer.events.beginTraining.fire().await()
                    trainer.trainOnce(unsupervisedNetwork)
                    trainer.events.endTraining.fire()
                }
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

        val preferencesButton = JButton(createAction(
            name = "Preferences",
            description = "Edit trainer preferences",
            iconPath = "menu_icons/Tools.png",
        ) {
            trainer.createEditorDialog().display()
        })
        runControls.add(preferencesButton)

        mainPanel.add(runControls, "wrap")

        // Run training algorithm
        val inputData = JPanel()
        inputData.layout = MigLayout("gap 0px 0px, ins 0")
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
        })
        inputData.add(inputs)
        mainPanel.add(inputData)

        contentPane = mainPanel
    }
}


context(NetworkPanel)
fun UnsupervisedNetwork.makeTrainerPanel(): StandardDialog = getUnsupervisedTrainingPanel(this) {
    this.trainOnCurrentPattern()
}

fun NetworkPanel.showSRNCreationDialog(): StandardDialog {

    val creator = SRNNetwork.SRNCreator(
        network.placementManager.lastClickedLocation
    )
    return creator.createEditorDialog {
        addSubnetworkAction(this@NetworkPanel) { creator.create() }
    }

}

fun main() {
    val networkComponent = NetworkComponent("")
    val np = NetworkPanel(networkComponent)
    val result = with(networkComponent.network) {
        val backpropNetwork = BackpropNetwork(intArrayOf(50, 20, 50))
        addNetworkModel(backpropNetwork)
        backpropNetwork
    }
    BackpropNetworkNode(np, result).propertyDialog.display()
}

context(Network)
fun UnsupervisedNetwork.createTrainOnPatternAction() = createAction(
    name = "Train on current pattern...",
    description = "Train network on current pattern for specified number of iterations.",
    iconPath = "menu_icons/BatchPlay.png"
) {
    val iterations: Int? = showNumericInputDialog("Iterations: ", NetworkPreferences.numberOfIterations)?.toInt()
    if (iterations != null) {
        NetworkPreferences.numberOfIterations = iterations
        runWithProgressWindow(iterations, batchSize = 10) {
            trainOnCurrentPattern()
        }
    }
}
