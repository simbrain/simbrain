package org.simbrain.network.gui

//import org.simbrain.network.gui.dialogs.dl4j.MultiLayerNetCreationDialog
import net.miginfocom.swing.MigLayout
import org.simbrain.network.NetworkComponent
import org.simbrain.network.core.Layer
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.Synapse
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.network.groups.SynapseGroup
import org.simbrain.network.gui.dialogs.DataPanel
import org.simbrain.network.gui.dialogs.TestInputPanel
import org.simbrain.network.gui.dialogs.group.ConnectorDialog
import org.simbrain.network.gui.dialogs.group.NeuronGroupDialog
import org.simbrain.network.gui.dialogs.group.SynapseGroupDialog
import org.simbrain.network.gui.dialogs.neuron.NeuronDialog
import org.simbrain.network.gui.dialogs.synapse.SynapseDialog
import org.simbrain.network.gui.dialogs.text.TextDialog
import org.simbrain.network.gui.nodes.TextNode
import org.simbrain.network.kotlindl.DeepNet
import org.simbrain.network.kotlindl.TFDenseLayer
import org.simbrain.network.kotlindl.TFFlattenLayer
import org.simbrain.network.kotlindl.TFLayer
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.smile.SmileClassifier
import org.simbrain.network.trainers.LMSIterative
import org.simbrain.util.ResourceManager
import org.simbrain.util.StandardDialog
import org.simbrain.util.piccolo.SceneGraphBrowser
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.CopyableObject
import org.simbrain.util.propertyeditor.ObjectTypeEditor
import org.simbrain.util.widgets.EditableList
import java.awt.Dialog
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.*
import javax.swing.*

fun NetworkPanel.showTextPropertyDialog(textNodes: List<TextNode>) {
    TextDialog(textNodes).apply {
        setLocationRelativeTo(this@showTextPropertyDialog)
        isVisible = true
    }
}

fun NetworkPanel.showSelectedNeuronProperties() {
    NeuronDialog(selectionManager.filterSelectedModels<Neuron>()).apply {
        modalityType = Dialog.ModalityType.MODELESS
        pack()
        setLocationRelativeTo(this@showSelectedNeuronProperties)
        isVisible = true
    }
}

fun NetworkPanel.showSelectedSynapseProperties() {
    SynapseDialog.createSynapseDialog(selectionManager.filterSelectedModels<Synapse>()).apply {
        modalityType = Dialog.ModalityType.MODELESS
        pack()
        setLocationRelativeTo(this@showSelectedSynapseProperties)
        isVisible = true
    }
}


fun NetworkPanel.showNeuronArrayCreationDialog() {
    val template = NeuronArray.CreationTemplate(network.idManager.getProposedId(NeuronArray::class.java))
    AnnotatedPropertyEditor(template).dialog.apply {
        addClosingTask {
            val neuronArray = template.create(network)
            network.addNetworkModel(neuronArray)
        }
        pack()
        setLocationRelativeTo(this@showNeuronArrayCreationDialog)
        isVisible = true
    }
}

fun NetworkPanel.showMultiLayerNetworkCreationDialog() {
//    MultiLayerNetCreationDialog(this).apply {
//        pack()
//        setLocationRelativeTo(this)
//        isVisible = true
//    }
}

val NetworkPanel.neuronDialog
    get() = selectionManager.filterSelectedModels<Neuron>().let { neurons ->
        if (neurons.isEmpty()) {
            null
        } else {
            NeuronDialog(neurons).apply { modalityType = Dialog.ModalityType.MODELESS }
        }
    }

val NetworkPanel.synapseDialog
    get() =
        SynapseDialog.createSynapseDialog(selectionManager.filterSelectedModels<Synapse>())

fun NetworkPanel.createNeuronGroupDialog(neuronGroup: NeuronGroup) =
    NeuronGroupDialog(this, neuronGroup).apply {
        title = "Neuron Group Dialog"
        setAsDoneDialog()
        modalityType = Dialog.ModalityType.MODELESS
    }

fun NetworkPanel.createSynapseGroupDialog(synapseGroup: SynapseGroup) =
    SynapseGroupDialog(this, synapseGroup).apply {
        modalityType = Dialog.ModalityType.MODELESS
    }

/**
 * Display the provided network in a dialog
 *
 * @param network the model network to show
 */
fun showNetwork(networkComponent: NetworkComponent) {
    // TODO: Creation outside of desktop lacks menus
    val frame = JFrame()
    val np = NetworkPanel(networkComponent)
    // component?.getDesktop()?.addInternalFrame(frame)
    //np.initScreenElements()
    frame.contentPane = np
    frame.preferredSize = Dimension(500, 500)
    frame.pack()
    frame.isVisible = true
    frame.addWindowListener(object : WindowAdapter() {
        override fun windowClosing(we: WindowEvent) {
            System.exit(0)
        }
    })
    //System.out.println(np.debugString());
}

fun NetworkPanel.showPiccoloDebugger() {
    StandardDialog().apply {
        contentPane = SceneGraphBrowser(canvas.root)
        title = "Piccolo Scenegraph Browser"
        isModal = false
        pack()
        setLocationRelativeTo(null)
        isVisible = true
    }
}

/**
 * Display the add synapse group dialog. Assumes the enabling condition (at
 * least one source and target neuron group designated) is in effect.
 *
 * @param networkPanel the network panel in which to add the group.
 */
fun displaySynapseGroupDialog(networkPanel: NetworkPanel?, src: NeuronGroup?, tar: NeuronGroup?): Boolean {
    val dialog: JDialog = SynapseGroupDialog(networkPanel, src, tar)
    dialog.setLocationRelativeTo(null)
    dialog.pack()
    dialog.isVisible = true
    return true
}

/**
 * Shows a dialog that allows the user to send inputs from a [SimbrainDataTable] to the provided neurons.
 */
fun NetworkPanel.showInputPanel(neurons: List<Neuron>) {
    TestInputPanel.createTestInputPanel(this, neurons).apply {
        val dialog = StandardDialog()
        dialog.contentPane = this
        dialog.setLocationRelativeTo(null)
        dialog.pack()
        dialog.isVisible = true
    }
}

/**
 * Show weight matrix panel for weights connecting current source (red) and target (green) nodes.
 */
fun NetworkPanel.showWeightMatrix() {
    WeightMatrixViewer.getWeightMatrixPanel(WeightMatrixViewer(this)).apply {
        val dialog = StandardDialog()
        dialog.contentPane = this
        dialog.setLocationRelativeTo(null)
        dialog.pack()
        dialog.title = "Weight Matrix Viewer"
        dialog.isVisible = true
    }
}


/**
 * Show dialog for LMS training
 */
fun NetworkPanel.showLMSDialog(lms: LMSIterative) {
//    LMSEditorDialog2(this, lms).apply {
//        modalityType = Dialog.ModalityType.MODELESS
//        pack()
//        isVisible = true
//    }
}

/**
 * Connect all selected [Layer]s with [WeightMatrix] objects.
 */
fun NetworkPanel.createConnector() {
    with(selectionManager) {
        val sources = filterSelectedSourceModels<Layer>()
        val targets = filterSelectedModels<Layer>()
        val dialog = ConnectorDialog(this.networkPanel, sources, targets)
        dialog.setLocationRelativeTo(null)
        dialog.pack()
        dialog.isVisible = true
    }
}

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
fun NetworkPanel.showDeepNetTrainingDialog(deepNet: DeepNet) {
    val dialog = StandardDialog()
    val ape: AnnotatedPropertyEditor

    dialog.contentPane = JPanel().apply {

        layout = MigLayout()

        // Data Panels
        val inputs = DataPanel().apply {
            table.setData(deepNet.inputs)
        }
        val targets = DataPanel().apply {
            table.setData(deepNet.targets.map { floatArrayOf(it) }.toTypedArray())
        }

        fun commitData() {
            deepNet.inputs = inputs.table.as2DFloatArray()
            deepNet.targets = targets.table.as2DFloatArray().map { it[0] }.toFloatArray();
            deepNet.initializeDatasets()
        }

        dialog.addClosingTask {
            inputs.applyData()
            targets.applyData()
            commitData()
        }

        val toolbar = JToolBar().apply {
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
        add(toolbar, "wrap")

        // // Stats label
        // add(statsLabel, "wrap")

        // Training Button
        add(JButton("Train").apply {
            addActionListener {
                commitData()
                deepNet.train()
                // smileClassifier.train(inputs.table.as2DDoubleArray(), targets.table.firstColumnAsIntArray())
                // statsLabel.text = "Stats: " + smileClassifier.classifier.stats
            }
        }, "wrap")

        // Add the data panels
        add(inputs, "growx")
        add(targets, "growx")
    }

    dialog.addClosingTask {
        // ape.commitChanges()
        // network.addNetworkModel(creator.create(network))
    }
    dialog.pack()
    dialog.setLocationRelativeTo(null)
    dialog.title = "Train Deep Network"
    dialog.isVisible = true
}

/**
 * Show dialog for Smile classifier creation
 */
fun NetworkPanel.showClassifierCreationDialog() {
    val creator = SmileClassifier.ClassifierCreator(network.idManager.getProposedId(SmileClassifier::class.java))
    val dialog = StandardDialog()
    val ape: AnnotatedPropertyEditor

    dialog.contentPane = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.PAGE_AXIS)
        ape = AnnotatedPropertyEditor(creator)
        add(ape)
    }

    dialog.addClosingTask {
        ape.commitChanges()
        network.addNetworkModel(creator.create(network))
    }
    dialog.pack()
    dialog.setLocationRelativeTo(null)
    dialog.title = "Create Smile Classifier"
    dialog.isVisible = true
}