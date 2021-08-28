package org.simbrain.network.gui.dialogs

import com.sun.java.accessibility.util.AWTEventMonitor.addActionListener
import net.miginfocom.swing.MigLayout
import org.jetbrains.kotlinx.dl.api.core.layer.core.Dense
import org.jetbrains.kotlinx.dl.api.core.layer.core.Input
import org.simbrain.network.core.Network
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.kotlindl.DeepNet
import org.simbrain.network.kotlindl.TFDenseLayer
import org.simbrain.network.kotlindl.TFFlattenLayer
import org.simbrain.network.kotlindl.TFLayer
import org.simbrain.util.ResourceManager
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

    fun getEditor(obj: CopyableObject): JPanel {
        return ObjectTypeEditor.createEditor(
            listOf(obj), "getTypes", "Layer",
            false
        )
    }

    val layerList = EditableList(arrayListOf(getEditor(TFDenseLayer()), getEditor(TFDenseLayer()))).apply {
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
        layerList.components.filterIsInstance<ObjectTypeEditor>().forEach { it.commitChanges() }
        val deepNet = creator.create(network,
            layerList.components.filterIsInstance<ObjectTypeEditor>().map { it.value }
                .filterIsInstance<TFLayer<*>>().map { it.create() }.toMutableList()
        )
        network.addNetworkModel(deepNet)
    }
    dialog.pack()
    dialog.setLocationRelativeTo(null)
    dialog.title = "Create Deep Network"
    dialog.isVisible = true
}

/**
 * Show dialog for Smile classifier creation
 */
fun showDeepNetTrainingDialog(deepNet: DeepNet) {
    val dialog = StandardDialog()
    val executor = Executors.newSingleThreadExecutor()

    dialog.contentPane = JPanel().apply {

        layout = MigLayout()

        // Data Panels
        val inputs = DataPanel().apply {
            table.setData(deepNet.inputs)
        }
        val targets = DataPanel().apply {
            table.setData(deepNet.targets.map { floatArrayOf(it) }.toTypedArray())
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
            deepNet.inputs = inputs.table.as2DFloatArray()
            deepNet.targets = targets.table.as2DFloatArray().map { it[0] }.toFloatArray();
            deepNet.initializeDatasets()
        }
        trainerPanel.add(JButton("Train").apply {
            addActionListener {
                executor.submit{
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
                deepNet.deepNetLayers.init()
            }
        }
        val stats = JEditorPane()
        stats.preferredSize = Dimension(300,100)
        deepNet.trainerEvents.onBeginTraining {
            stats.text += "Begin training\n"
            stats.caretPosition = stats.text.length
        }
        deepNet.trainerEvents.onEndTraining {
            stats.text += "End training\n"
            stats.text += "Loss = ${SimbrainMath.roundDouble(deepNet.lossValue, 3)}\n"
            stats.caretPosition = stats.text.length
        }
        trainerPanel.add(JScrollPane(stats))
        add(trainerPanel, "span 2, wrap")
        add(JSeparator(), "growx, span 2, wrap")

        // TODO: Move this to datapanel somehow
        val addRemoveRows = JToolBar().apply {
            // Add row
            add(JButton().apply {
                icon = ResourceManager.getImageIcon("menu_icons/AddTableRow.png")
                toolTipText = "Insert a row"
                addActionListener {
                    inputs.table.insertRow(inputs.jTable.selectedRow)
                    targets.table.insertRow(inputs.jTable.selectedRow)
                }
            })
            // Delete row
            // TODO: Delete selected rows. For that abstract out table code
            add(JButton().apply {
                icon = ResourceManager.getImageIcon("menu_icons/DeleteRowTable.png")
                toolTipText = "Delete last row"
                addActionListener {
                    inputs.table.removeRow(inputs.jTable.rowCount - 1)
                    targets.table.removeRow(targets.jTable.rowCount - 1)
                }
            })
        }
        add(addRemoveRows, "wrap")

        // Add the data panels
        add(inputs, "growx")
        add(targets, "wrap")

        dialog.addClosingTask {
            inputs.applyData()
            targets.applyData()
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
    val dn = DeepNet(Network(), 3,
        listOf(Input(2), Dense(2), Dense(1)), 4)
    dn.inputs = arrayOf(floatArrayOf(0f, 0f), floatArrayOf(1f, 0f), floatArrayOf(0f, 1f), floatArrayOf(1f, 1f))
    dn.targets = floatArrayOf(0f, 1f, 1f, 0f)
    dn.initializeDatasets()
    showDeepNetTrainingDialog(dn)
}


