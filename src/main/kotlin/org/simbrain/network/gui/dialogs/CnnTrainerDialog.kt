package org.simbrain.network.gui.dialogs

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.swing.Swing
import net.miginfocom.swing.MigLayout
import org.simbrain.network.events.TrainingStats
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.subnetworks.ConvolutionalNeuralNetwork
import org.simbrain.network.trainers.CnnTrainer
import org.simbrain.network.trainers.TrainingDataset
import org.simbrain.plot.timeseries.TimeSeriesPlotActions
import org.simbrain.util.*
import org.simbrain.util.widgets.ToggleButton
import java.awt.Cursor
import java.awt.Dialog
import javax.swing.*

/**
 * Creates a training dialog for a [ConvolutionalNeuralNetwork], analogous to [getSupervisedTrainingDialog].
 */
context(NetworkPanel)
fun ConvolutionalNeuralNetwork.getCnnTrainingDialog(): StandardDialog {
    val cnnNetwork = this
    val trainer = CnnTrainer(this@NetworkPanel.network, cnnNetwork.inputTensorLayer, cnnNetwork.outputArray, cnnNetwork.trainerConfig).apply {
        trainingData = cnnNetwork.trainingSet
        testingData = cnnNetwork.testingSet
    }
    val parentWindow = SwingUtilities.getWindowAncestor(this@NetworkPanel)
    return StandardDialog(parentWindow as? JFrame, "Train CNN").apply {

        isModal = true
        isAlwaysOnTop = false
        modalityType = Dialog.ModalityType.APPLICATION_MODAL
        addWindowFocusListener(object : java.awt.event.WindowAdapter() {
            override fun windowGainedFocus(e: java.awt.event.WindowEvent?) {
                toFront()
            }
        })

        val runControls = JPanel()
        runControls.layout = MigLayout("ins 0, gap 0px 8px")
        val trainerControls = CnnTrainerControls(trainer)

        fun createDataSetPanel(dataSet: TrainingDataset) = dataSet.createDataSetPanel(this@apply) { selectedRow ->
            // Apply input to the network and run forward pass
            val input = inputDataFrame.getRow<Double>(selectedRow).toDoubleArray()
            trainer.syncFromNetwork()
            val output = trainer.forwardPass(input)

            // Sync output activations back to the actual network NeuronArrays so GUI updates
            cnnNetwork.outputArray.setActivations(output)
            // Also sync the flatten layer
            val flattenTarget = trainer.flattenConnector.target
            val flatSrc = if (trainer.tensorLayerStages.isNotEmpty()) trainer.tensorLayerStages.last().activations else cnnNetwork.inputTensorLayer.activations
            flattenTarget.setActivations(flatSrc.copyOf())

            // Fire update events so the GUI repaints
            cnnNetwork.inputTensorLayer.events.updated.fire()
            trainer.tensorLayerStages.forEach { it.events.updated.fire() }
            flattenTarget.events.updated.fire()
            cnnNetwork.outputArray.events.updated.fire()

            // Compute and display per-row loss
            val target = targetDataFrame.getRow<Double>(selectedRow).toDoubleArray()
            val loss = trainer.config.lossFunction.loss(output, target)
            showRowError(selectedRow, trainer.config.lossFunction.shortName, loss)
        }

        val trainingDataSetPanel = createDataSetPanel(cnnNetwork.trainingSet)
        val testingDataSetPanel = createDataSetPanel(cnnNetwork.testingSet)

        fun updateOverallValidation() {
            val trainingValid = trainingDataSetPanel.inputDataFrame.rowCount == trainingDataSetPanel.targetDataFrame.rowCount
            val testingValid = testingDataSetPanel.inputDataFrame.rowCount == testingDataSetPanel.targetDataFrame.rowCount
            trainerControls.updateValidationState(
                trainingInputRows = trainingDataSetPanel.inputDataFrame.rowCount,
                trainingTargetRows = trainingDataSetPanel.targetDataFrame.rowCount,
                testingInputRows = testingDataSetPanel.inputDataFrame.rowCount,
                testingTargetRows = testingDataSetPanel.targetDataFrame.rowCount,
                trainingValid = trainingValid,
                testingValid = testingValid
            )
        }

        trainingDataSetPanel.onRowCountChanged = { _, _ -> updateOverallValidation() }
        testingDataSetPanel.onRowCountChanged = { _, _ -> updateOverallValidation() }

        fun DataSetPanel.exportMatrixDataSet() = TrainingDataset(
            inputs.table.model.get2DDoubleList().toMutableListOfLists(),
            targets.table.model.get2DDoubleList().toMutableListOfLists(),
            inputSize = inputDataFrame.columnNames.size,
            targetSize = targetDataFrame.columnNames.size,
            inputRowNames = inputDataFrame.rowNames.map { it.toString() } as List<String>?,
            targetRowNames = targetDataFrame.rowNames.map { it.toString() } as List<String>?,
            inputColumnNames = inputDataFrame.columnNames,
            targetColumnNames = targetDataFrame.columnNames
        )

        fun syncDataSet() {
            cnnNetwork.trainingSet = trainingDataSetPanel.exportMatrixDataSet()
            cnnNetwork.testingSet = testingDataSetPanel.exportMatrixDataSet()
            trainer.trainingData = cnnNetwork.trainingSet
            trainer.testingData = cnnNetwork.testingSet
        }

        val dataSetTabPane = JTabbedPane().apply {
            addTab("Training data", trainingDataSetPanel)
            addTab("Testing data", testingDataSetPanel)
        }

        trainer.events.beginTraining.on(Dispatchers.Default) { syncDataSet() }
        updateOverallValidation()
        runControls.add(trainerControls, "span, growx, wrap")
        runControls.add(dataSetTabPane, "grow, push, wrap")

        addCommitTask { syncDataSet() }

        contentPane = runControls
        setAsDoneDialog()
    }
}

/**
 * Controls for CNN training: Step/Run/Stop buttons, Randomize, iterations, error plot.
 */
class CnnTrainerControls(
    internal val trainer: CnnTrainer
) : JPanel(), CoroutineScope {

    private val controlsJob = SupervisorJob()

    override val coroutineContext = Dispatchers.Swing + controlsJob

    private var isValidationEnabled = true
    private val runActionRef: Action
    private val stepActionRef: Action

    private val statsPanel = TrainingStatsPanel(
        scope = this,
        events = trainer.events,
        iteration = { trainer.iteration },
        resetIteration = {
            trainer.iteration = 0
            trainer.events.iterationReset.fire()
        },
        initialStats = TrainingStats(
            trainingError = trainer.lastTrainingError,
            trainingAccuracy = trainer.lastTrainingAccuracy,
            testingAccuracy = trainer.lastTestingAccuracy,
            effectiveStepSize = trainer.lastEffectiveStepSize
        )
    ) {
        with(trainer.config) {
            TrainingStatsSettings(
                lossName = lossFunction.shortName,
                testingEnabled = testConfiguration.enabled,
                computeAccuracy = computeAccuracy,
                showStepSize = showEffectiveStepSize,
                stoppingCondition = stoppingCondition
            )
        }
    }

    init {
        val errorPlot = ErrorTimeSeries(trainer.events) { trainer.iteration }

        // Cancel coroutine scopes when this component is disposed
        onWindowClose {
            trainer.job.cancel()
            controlsJob.cancel()
            errorPlot.dispose()
        }

        val errorPlotPanel = JPanel().apply {
            layout = MigLayout("ins 0, gap 0px 0px, fillx, wrap")
            add(errorPlot, "growx, wrap")

            val buttonPanel = JPanel(MigLayout("ins 0, gap 8px"))
            buttonPanel.add(JButton(TimeSeriesPlotActions.getClearGraphAction(errorPlot.graphPanel)))
            buttonPanel.add(JButton(TimeSeriesPlotActions.getPropertiesDialogAction(errorPlot.graphPanel)))
            add(buttonPanel, "wrap, align center, gapbottom 20px")
        }

        val runAction = createAction(
            name = "Run",
            iconPath = "menu_icons/Play.png",
            description = "Iterate training until stop button is pressed"
        ) {
            trainer.startTraining()
        }
        runActionRef = runAction

        val stopAction = createAction(
            name = "Stop",
            iconPath = "menu_icons/Stop.png",
            description = "Stop training",
        ) {
            trainer.stopTraining()
        }

        val stepAction = createAction(
            description = "Iterate training once",
            iconPath = "menu_icons/Step.png",
            initBlock = {
                trainer.events.beginTraining.on(Dispatchers.Swing) { isEnabled = false }
                trainer.events.endTraining.on { isEnabled = isValidationEnabled }
            }
        ) {
            trainer.trainOnce()
        }
        stepActionRef = stepAction

        val initializeParameters = createAction(
            name = "Init parameters",
            description = "Re-initialize CNN kernels and dense weights",
            iconPath = "menu_icons/Rand.png",
        ) {
            trainer.randomize()
            trainer.iteration = 0
        }

        val trainerPropsAction = createAction(
            name = "Trainer properties",
            description = "Edit CNN trainer properties",
            iconPath = "menu_icons/Tools.png",
        ) {
            trainer.config.createEditorDialog { statsPanel.refresh() }.display()
        }

        val runTools = JPanel().apply { layout = MigLayout("nogrid, ins 4, gap 8px") }
        runTools.add(JButton(stepAction))
        runTools.add(ToggleButton(listOf(runAction, stopAction)).apply {
            setAction("Run")
            trainer.events.beginTraining.on(Dispatchers.Swing) {
                this@CnnTrainerControls.cursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)
                setAction("Stop")
            }
            trainer.events.endTraining.on {
                this@CnnTrainerControls.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                setAction("Run")
            }
        })
        val initParamsButton = JButton(initializeParameters)
        initParamsButton.hideActionText = true
        runTools.add(initParamsButton)
        runTools.add(JButton(trainerPropsAction), "wrap")

        runTools.add(statsPanel)

        layout = MigLayout("ins 0, gap 12px 0px")
        add(runTools)
        add(errorPlotPanel, "grow, gapbottom 0px")
    }

    fun updateValidationState(
        trainingInputRows: Int,
        trainingTargetRows: Int,
        testingInputRows: Int,
        testingTargetRows: Int,
        trainingValid: Boolean,
        testingValid: Boolean
    ) {
        val isValid = trainingValid && testingValid
        isValidationEnabled = isValid

        stepActionRef.isEnabled = isValid && !trainer.isRunning
        runActionRef.isEnabled = isValid

        val stepValidationMessage = when {
            isValid -> "Iterate training once"
            !trainingValid && !testingValid ->
                "Cannot train: Training (${trainingInputRows}/${trainingTargetRows}) and testing (${testingInputRows}/${testingTargetRows}) row counts do not match."
            !trainingValid ->
                "Cannot train: Training inputs (${trainingInputRows}) and targets (${trainingTargetRows}) row counts do not match."
            else ->
                "Cannot train: Testing inputs (${testingInputRows}) and targets (${testingTargetRows}) row counts do not match."
        }
        stepActionRef.putValue(Action.SHORT_DESCRIPTION, stepValidationMessage)

        val runValidationMessage = when {
            isValid -> "Iterate training until stop button is pressed"
            !trainingValid && !testingValid -> "Cannot train: Both training and testing data have mismatched row counts."
            !trainingValid -> "Cannot train: Training input and target row counts must match."
            else -> "Cannot train: Testing input and target row counts must match."
        }
        runActionRef.putValue(Action.SHORT_DESCRIPTION, runValidationMessage)
    }
}

