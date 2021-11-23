package org.simbrain.network.gui.dialogs

import net.miginfocom.swing.MigLayout
import org.simbrain.network.core.Network
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.kotlindl.*
import org.simbrain.util.StandardDialog
import org.simbrain.util.math.ProbDistributions.UniformDistribution
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.ObjectTypeEditor
import org.simbrain.util.table.*
import org.simbrain.util.widgets.EditableList
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.util.concurrent.Executors
import javax.swing.*

/**
 * Show dialog for deep net creation
 */
fun NetworkPanel.showDeepNetCreationDialog() {

    val creator = DeepNet.DeepNetCreator(network.idManager.getProposedId(DeepNet::class.java))
    val dialog = StandardDialog()
    val ape: AnnotatedPropertyEditor

    val layerList = LayerEditor(
        arrayListOf(TFInputLayer(), TFDenseLayer(), TFDenseLayer())
    )

    dialog.contentPane = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
        ape = AnnotatedPropertyEditor(creator)
        add(ape)
        add(layerList)
    }

    dialog.setClosingCheck { layerList.checkValidLayers() }
    dialog.addClosingTask {
        ape.commitChanges()
        layerList.commitChanges()
        val deepNet = creator.create(network, layerList.layers)
        network.addNetworkModel(deepNet)
    }
    dialog.pack()
    dialog.setLocationRelativeTo(null)
    dialog.title = "Create Deep Network"
    dialog.isVisible = true
}

fun getDeepNetEditDialog(deepNet: DeepNet): StandardDialog {
    val editor = AnnotatedPropertyEditor(deepNet)
    val panel = JPanel(MigLayout())
    panel.add(editor, "wrap")
    val layerEditor = LayerEditor(deepNet.tfLayers, false)
    panel.add(layerEditor)
    val dialog = StandardDialog()
    dialog.contentPane = panel
    dialog.addClosingTask {
        editor.commitChanges()
        layerEditor.commitChanges()
        // TODO: Rebuild deep net, but only if layer editor changed. Yulin.
        deepNet.events.fireUpdated()
    }
    return dialog
}

fun getEditor(obj: CopyableObject): JPanel {
    return ObjectTypeEditor.createEditor(
        listOf(obj), "getTypes", "Layer",
        false
    )
}

/**
 * Assumes first layer is an input layer. Populated with [TFLayer] objects which can then be used to create a [DeepNet].
 */
class LayerEditor(
    val layers: ArrayList<TFLayer<*>>,
    // True  only at creation when layers can be added or removed.
    val addRemove: Boolean = true
) : EditableList(addRemove) {

    init {
        // By default add dense layers
        newElementTask = {
            addElement(getEditor(TFDenseLayer()))
        }
        layers.forEach() {
            addElement(getEditor(it))
        }
    }

    override fun removeElement() {
        // Don't allow input layer and one layer after that to be removed
        if (components.size > 2) {
            removeLast();
        }
    }

    val inputLayer: TFInputLayer
        get() = layers.first() as TFInputLayer

    /**
     * Check that the layers currently in the list are a valid kotlindl network.
     */
    fun checkValidLayers(): Boolean {
        var validLayerSequence = true
        var errorMessage = "Invalid sequence of layers"
        components.filterIsInstance<ObjectTypeEditor>().map { it.value }.windowed(2) { (first, second) ->
            if (second is TFDenseLayer) {
                if (first !is TFFlattenLayer && first !is TFDenseLayer && first !is TFInputLayer) {
                    errorMessage = "Dense layers must come after input, flatten, or dense layers"
                    validLayerSequence = false
                }
            }
        }
        if (!validLayerSequence) {
            JOptionPane.showMessageDialog(null, "Invalid layer sequence: " + errorMessage)
        }
        return validLayerSequence
    }

    fun commitChanges() {
        layers.clear();
        components.filterIsInstance<ObjectTypeEditor>().forEach {
            it.commitChanges()
            layers.add(it.value as TFLayer<*>)
        }
    }

}

/**
 * Show dialog for deep net training
 */
fun showDeepNetTrainingDialog(deepNet: DeepNet) {

    val dialog = StandardDialog()
    val executor = Executors.newSingleThreadExecutor()

    dialog.contentPane = JPanel().apply {

        layout = MigLayout("wrap 3")

        fun SimbrainDataViewer.addFixedColumnActions() {
            addAction(table.importCSVAction(true))
            addAction(table.zeroFillAction)
            addAction(table.randomizeColumnAction)
            addAction(table.editRandomizerAction)
            addAction(table.editColumnAction)
        }

        // Data Panels
        val inputPanel = SimbrainDataViewer(
            createFromFloatArray(deepNet.inputData), useDefaultToolbarAndMenu =
            false
        ).apply {
            addFixedColumnActions()
            addAction(table.randomizeAction)
        }

        val targetPanel = SimbrainDataViewer(
            createFromColumn(deepNet.targetData), useDefaultToolbarAndMenu =
            false
        ).apply {
            addFixedColumnActions()
            val numClasses = deepNet.deepNetLayers.numberOfClasses.toInt()
            if (numClasses != -1) {
                table.model.columns[0].type = Column.DataType.IntType
                table.model.columns[0].columnRandomizer.probabilityDistribution =
                    UniformDistribution.builder().upperBound(numClasses.toDouble()).lowerBound(0.0).build()
            }
        }

        // Optimizer
        val optimizerParams = AnnotatedPropertyEditor(deepNet.optimizerParams)

        // Trainer
        val trainingParams = AnnotatedPropertyEditor(deepNet.trainingParams)

        // Helper to commit data from data tables
        fun commitData() {
            deepNet.inputData = inputPanel.model.getRowMajorFloatArray()
            deepNet.targetData = targetPanel.model.getFloatColumn(0)
            deepNet.initializeDatasets()
        }

        // Trainer States
        val console = JEditorPane().apply {
            preferredSize = Dimension(300, 100)
        }
        val trainButton = JButton("Train").apply {
            addActionListener {
                // TODO
                executor.submit {
                    commitData()
                    trainingParams.commitChanges()
                    optimizerParams.commitChanges()
                    deepNet.buildNetwork() // for optimizer
                    deepNet.train()
                }
            }
        }
        val resetButton = JButton("Reset").apply {
            addActionListener {
                if (!deepNet.deepNetLayers.isModelCompiled) {
                    deepNet.deepNetLayers.init()
                }
            }
        }
        val clearWindow = JButton("Clear").apply {
            addActionListener {
                console.text = ""
            }
        }
        val statsPanel = JPanel().apply {
            layout = BorderLayout()
            add(JScrollPane(console), BorderLayout.CENTER)
            add(JPanel().apply {
                layout = FlowLayout(FlowLayout.LEFT)
                add(trainButton)
                add(resetButton)
                add(clearWindow)
            }, BorderLayout.SOUTH)
        }

        // Register events
        deepNet.trainerEvents.onBeginTraining {
            console.text += "Begin training\n"
            console.caretPosition = console.text.length
        }
        deepNet.trainerEvents.onEndTraining {
            console.text += "End training\n"
            console.text += "Loss = ${SimbrainMath.roundDouble(deepNet.lossValue, 10)}\n"
            console.caretPosition = console.text.length
        }

        // Add components to miglayout
        add(optimizerParams, "span 1, wrap")
        add(JSeparator(), "grow, span, wrap")
        add(trainingParams)
        add(statsPanel, "wrap")
        add(JSeparator(), "grow, span, wrap")
        add(inputPanel, "span 1, w 200:300:400, h 200!,")
        add(targetPanel, "span 1, w 150:250:300,h 200!, wrap")

        dialog.addClosingTask {
            commitData()
        }
    }

    dialog.pack()
    dialog.setLocationRelativeTo(null)
    dialog.title = "Train Deep Network"
    dialog.isVisible = true
}

fun main() {
    // TODO: Move some of this to test classes
    testLayerList()
    // testTrainingDialog()
}

fun testTrainingDialog() {
    val dn = DeepNet(
        Network(),
        arrayListOf(TFInputLayer(2), TFDenseLayer(2), TFDenseLayer(1)),
        4
    )
    dn.inputData = arrayOf(floatArrayOf(0f, 0f), floatArrayOf(1f, 0f), floatArrayOf(0f, 1f), floatArrayOf(1f, 1f))
    dn.targetData = floatArrayOf(0f, 1f, 1f, 0f)
    dn.initializeDatasets()
    showDeepNetTrainingDialog(dn)
}

fun testLayerList() {

    StandardDialog().apply {
        val layerEditor = LayerEditor(arrayListOf(TFInputLayer(), TFConv2DLayer(), TFFlattenLayer(), TFDenseLayer()))
        setClosingCheck { layerEditor.checkValidLayers() }
        addClosingTask {
            println("Closing..")
            layerEditor.commitChanges()
            val dn = DeepNet(Network(), layerEditor.layers)
            println(dn.tfLayers.map { it.getRank() }.joinToString(","))
            // println(dn.deepNetLayers.summary())

        }
        contentPane = layerEditor
        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }

}
