package org.simbrain.network.gui.dialogs

import com.sun.java.accessibility.util.AWTEventMonitor.addActionListener
import net.miginfocom.swing.MigLayout
import org.simbrain.network.core.Network
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.kotlindl.*
import org.simbrain.util.StandardDialog
import org.simbrain.util.math.SimbrainMath
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.ObjectTypeEditor
import org.simbrain.util.widgets.EditableList
import java.awt.Dimension
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
    ).apply {
        // Custom action for the "+" Button. Adds a flatten layer by default.
        addElementTask = {
            addElement(getEditor(TFFlattenLayer()))
        }
    }

    dialog.contentPane = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
        ape = AnnotatedPropertyEditor(creator)
        add(ape)
        add(layerList)
    }

    dialog.addClosingTask {
        ape.commitChanges()
        layerList.commitChanges()
        val deepNet = creator.create(network, layerList.inputLayer().n_in, layerList.layers)
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
    val layerEditor = LayerEditor(deepNet.editableLayers, false)
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


// TODO: Yulin review
fun getEditor(obj: CopyableObject): JPanel {
    return ObjectTypeEditor.createEditor(
        listOf(obj), "getTypes", "Layer",
        false
    )
}

/**
 * Assumes first layer is an input layer
 */
class LayerEditor(
    val layers: ArrayList<TFLayer<*>>,
    val addRemove: Boolean = true
) : EditableList(addRemove) {

    init {
        layers.forEach() {
            if (it is TFInputLayer) {
                addElement(AnnotatedPropertyEditor(it))
            } else {
                addElement(getEditor(it))
            }
        }
    }

    fun addLayer(layer: TFLayer<*>) {
        layers.add(layer)
        addElement(getEditor(layer))
    }

    override fun removeElement() {
        // Don't allow input layer and one layer after that to be removed
        if (components.size > 2) {
            removeLast();
        }
    }

    // Yulin: Naming conventions
    fun inputLayer(): TFInputLayer {
        return layers.first() as TFInputLayer
    }

    fun mainLayers(): List<TFLayer<*>> {
        return layers.subList(1, layers.size)
    }

    fun commitChanges() {
        // Yulin
        layers.clear();
        (components.first() as AnnotatedPropertyEditor).commitChanges()
        layers.add((components.first() as AnnotatedPropertyEditor).editedObject as TFLayer<*>)
        components.subList(1, components.size).filterIsInstance<ObjectTypeEditor>().forEach {
            it.commitChanges()
            layers.add(it.value as TFLayer<*>)
        }
    }

}


/**
 * Show dialog for deep net traiing
 */
fun showDeepNetTrainingDialog(deepNet: DeepNet) {

    val dialog = StandardDialog()
    val executor = Executors.newSingleThreadExecutor()

    dialog.contentPane = JPanel().apply {

        layout = MigLayout()

        // Data Panels
        val dataPanels = InputTargetDataPanel().apply {
            inputs.table.setData(deepNet.inputData)
            targets.table.setIntegerMode(true); // TODO: Temp / for class label case only
            targets.table.setData(deepNet.targetData.map { floatArrayOf(it) }.toTypedArray())
        }

        // Optimizer
        val optimizerParams = AnnotatedPropertyEditor(deepNet.optimizerParams)
        add(optimizerParams, "growx, span 2, wrap")
        add(JSeparator(), "growx, span 2, wrap")

        // Trainer
        val trainerPanel = JPanel()
        val trainingParams = AnnotatedPropertyEditor(deepNet.trainingParams)
        trainerPanel.add(trainingParams)
        fun commitData() {
            deepNet.inputData = dataPanels.inputs.table.as2DFloatArray()
            deepNet.targetData = dataPanels.targets.table.as2DFloatArray().map { it[0] }.toFloatArray();
            deepNet.initializeDatasets()
        }
        trainerPanel.add(JButton("Train").apply {
            addActionListener {
                executor.submit {
                    commitData()
                    trainingParams.commitChanges()
                    optimizerParams.commitChanges()
                    deepNet.buildNetwork() // for optimizer
                    deepNet.train()
                }
            }
        })
        trainerPanel.add(JButton("Reset")).apply {
            addActionListener {
                deepNet.deepNetLayers.init() // TODO: Does not work
            }
        }
        val stats = JEditorPane()
        stats.preferredSize = Dimension(300, 100)
        deepNet.trainerEvents.onBeginTraining {
            stats.text += "Begin training\n"
            stats.caretPosition = stats.text.length
        }
        deepNet.trainerEvents.onEndTraining {
            stats.text += "End training\n"
            stats.text += "Loss = ${SimbrainMath.roundDouble(deepNet.lossValue, 10)}\n"
            stats.caretPosition = stats.text.length
        }
        trainerPanel.add(JScrollPane(stats))
        add(trainerPanel, "span 2, wrap")
        add(JSeparator(), "growx, span 2, wrap")

        add(dataPanels, "wrap")

        dialog.addClosingTask {
            dataPanels.inputs.applyData()
            dataPanels.targets.applyData()
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
    // testLayerList()
    testTrainingDialog()
}

fun testTrainingDialog() {
    val dn = DeepNet(
        Network(), 3,
        arrayListOf(TFInputLayer(2), TFDenseLayer(2), TFDenseLayer(1)), 4
    )
    dn.inputData = arrayOf(floatArrayOf(0f, 0f), floatArrayOf(1f, 0f), floatArrayOf(0f, 1f), floatArrayOf(1f, 1f))
    dn.targetData = floatArrayOf(0f, 1f, 1f, 0f)
    dn.initializeDatasets()
    showDeepNetTrainingDialog(dn)
}

fun testLayerList() {

    StandardDialog().apply {
        val layerEditor = LayerEditor(arrayListOf(TFInputLayer(), TFDenseLayer())).apply {
            addElementTask = {
                addLayer(TFFlattenLayer())
            }
        }
        addClosingTask {
            println("Closing..")
            layerEditor.commitChanges()
            println("Input layer: ${layerEditor.inputLayer().create()}")
            layerEditor.mainLayers().forEach { l -> println("Layer: ${l.create()}") }
        }
        contentPane = layerEditor
        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }

}
