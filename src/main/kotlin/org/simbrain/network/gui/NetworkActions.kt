package org.simbrain.network.gui

import kotlinx.coroutines.launch
import org.simbrain.network.connections.*
import org.simbrain.network.core.*
import org.simbrain.network.gui.ConditionallyEnabledAction.EnablingCondition
import org.simbrain.network.gui.dialogs.*
import org.simbrain.network.gui.dialogs.NetworkPreferences.excitatoryRandomizer
import org.simbrain.network.gui.dialogs.NetworkPreferences.inhibitoryRandomizer
import org.simbrain.network.gui.dialogs.NetworkPreferences.weightRandomizer
import org.simbrain.network.gui.dialogs.neuron.AddNeuronsDialog.createAddNeuronsDialog
import org.simbrain.network.gui.nodes.*
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.neurongroups.BasicNeuronGroupParams
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.neurongroups.NeuronGroupParams
import org.simbrain.network.subnetworks.RestrictedBoltzmannMachine
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.network.trainers.computeOrderedUpdatePath
import org.simbrain.network.util.Alignment
import org.simbrain.util.*
import org.simbrain.util.decayfunctions.DecayFunction
import org.simbrain.util.propertyeditor.objectWrapper
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.gui.SimbrainDesktop
import org.simbrain.workspace.gui.SimbrainDesktop.actionManager
import java.awt.event.KeyEvent
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.swing.AbstractAction
import javax.swing.Action.SHORT_DESCRIPTION
import javax.swing.JCheckBoxMenuItem
import javax.swing.JOptionPane

class NetworkActions(val networkPanel: NetworkPanel) {
    // For testing purposes only
    var fileChooserForTesting: SFileChooser? = null
    val addNeuronsAction = networkPanel.createAction(
        name = "Add Neurons...",
        description = "Add a set of neurons to the network",
        keyboardShortcut = KeyCombination('N')
    ) {
        createAddNeuronsDialog(networkPanel).apply {
            pack()
            setLocationRelativeTo(null)
            isVisible = true
        }
    }
    val alignHorizontalAction = networkPanel.createConditionallyEnabledAction(
        name = "Align Horizontal",
        description = "Align selected nodes horizontally",
        iconPath = "menu_icons/AlignHorizontal.png",
        enablingCondition = EnablingCondition.ALLITEMS
    ) {
        alignHorizontal()
    }
    val alignVerticalAction = networkPanel.createConditionallyEnabledAction(
        name = "Align Vertical",
        description = "Align selected nodes vertically",
        iconPath = "menu_icons/AlignVertical.png",
        enablingCondition = EnablingCondition.ALLITEMS
    ) {
        alignVertical()
    }
    val clearNodeActivationsAction = networkPanel.createConditionallyEnabledAction(
        name = "Clear activations of all nodes",
        description = "Clear all node activations (c)",
        iconPath = "menu_icons/Eraser.png",
        enablingCondition = EnablingCondition.ALLITEMS,
        keyboardShortcuts = KeyCombination('C')
    ) {
        clearSelectedObjects()
    }
    val clearSourceNeurons = networkPanel.createConditionallyEnabledAction(
        name = "Clear Source Neurons",
        description = "Remove all source neurons (neurons with red squares around them)",
        enablingCondition = EnablingCondition.SOURCE_NEURONS
    ) {
        selectionManager.clearAllSource()
    }
    val copyAction = networkPanel.createAction(
        name = "Copy",
        description = "Copy selected neurons, (connected) synapses, and neuron groups",
        keyboardShortcut = CmdOrCtrl + 'C',
        iconPath = "menu_icons/Copy.png"
    ) {
        copy()
    }
    val cutAction = networkPanel.createAction(
        name = "Cut",
        description = "Cut selected neurons, (connected) synapses, and neuron groups",
        keyboardShortcut = CmdOrCtrl + 'X',
        iconPath = "menu_icons/Cut.png"
    ) {
        cut()
    }
    val pasteAction = networkPanel.createAction(
        name = "Paste",
        description = "Paste copied neurons, (connected) synapses, and neuron groups",
        keyboardShortcut = CmdOrCtrl + 'V',
        iconPath = "menu_icons/Paste.png"
    ) {
        paste()
    }
    val duplicateAction = networkPanel.createAction(
        name = "Duplicate",
        description = "Duplicate selected neurons, (connected) synapses, and neuron groups",
        keyboardShortcut = CmdOrCtrl + 'D',
        iconPath = "menu_icons/Copy.png"
    ) {
        duplicate()
    }
    val addNeuronArrayAction = networkPanel.createAction(
        name = "Add Neuron Array...",
        description = "Add a neuron array to the network",
        keyboardShortcut = KeyCombination('Y')
    ) {
        showNeuronArrayCreationDialog()
    }
    val addActivationSequenceAction = networkPanel.createAction(
        name = "Add Activation Sequence...",
        description = "Add an activation sequence to the network",
    ) {
        showActivationSequenceCreationDialog()
    }
    val addTransformerBlockAction = networkPanel.createAction(
        name = "Add Transformer Block...",
        description = "Add a transformer block to the network",
    ) {
        showTransformerBlockCreationDialog()
    }
    val deleteAction = networkPanel.createConditionallyEnabledAction(
        name = "Delete",
        description = """Delete selected node(s) ("Backspace" or "Delete")""",
        enablingCondition = EnablingCondition.ALLITEMS,
        iconPath = "menu_icons/minus.png",
        keyboardShortcuts = listOf(KeyCombination(KeyEvent.VK_DELETE), KeyCombination(KeyEvent.VK_BACK_SPACE))
    ) {
        launch { deleteSelectedObjects() }
    }

    val neuronCollectionAction = networkPanel.createConditionallyEnabledAction(
        name = "Add Neurons to Collection",
        description = "Add selected neurons to a neuron collection (Cmd-G)",
        enablingCondition = EnablingCondition.NEURONS,
        keyboardShortcuts = CmdOrCtrl + 'G'
    ) {
        val neuronList = selectionManager.filterSelectedModels<Neuron>()
        if (neuronList.isNotEmpty()) {
            val nc = NeuronCollection(neuronList)
            if (with(network) { nc.shouldAdd() }) {
                network.addNetworkModel(nc)
            }
            undoManager.addUndoableAction(
                description = "Add neurons to collection",
                undo = { nc.delete() },
                redo = {
                    nc.neuronList.clear()
                    nc.neuronList.addAll(neuronList)
                    network.addNetworkModel(nc, usePlacementManager = false, useAutoAssignedId = false)?.await()
                }
            )
        }
    }
    val newNeuronAction = networkPanel.createAction(
        name = "Add Neuron",
        description = """Add or "put" new node (p)""",
        iconPath = "menu_icons/plus.png",
        keyboardShortcut = KeyCombination('P')
    ) {
        val neuron = Neuron()
        network.addNetworkModel(neuron)
        network.selectModels(listOf(neuron))
        undoManager.addUndoableAction(
            description = "Add neuron ${neuron.id}",
            undo = { neuron.delete() },
            redo = { network.addNetworkModel(neuron, usePlacementManager = false, useAutoAssignedId = false)?.await() }
        )
    }
    val randomizeObjectsAction = networkPanel.createConditionallyEnabledAction(
        name = "Randomize selection",
        description = "Randomize Selected Elements (r)",
        enablingCondition = EnablingCondition.ALLITEMS,
        iconPath = "menu_icons/Rand.png",
        keyboardShortcuts = KeyCombination('R')
    ) {
        with(network) {
            selectionManager.selectedModels.map { it.randomize() }
        }
    }
    val randomizeBiasesAction = networkPanel.createAction(
        name = "Randomize Biases",
        description = "Randomize biases of selected nodes",
        iconPath = "menu_icons/Rand.png",
        keyboardShortcut = CmdOrCtrl + 'B'
    ) {
        with(network) {
            selectionManager.selectedModels
                .filterIsInstance<Neuron>()
                .map { it.randomizeBias() }
            selectionManager.selectedModels
                .filterIsInstance<NeuronArray>()
                .map { it.randomizeBiases() }
        }
    }
    val selectAllAction = networkPanel.createAction(
        name = "Select All",
        description = "Select all network items",
        keyboardShortcuts = listOf(KeyCombination('A'), CmdOrCtrl + 'A')
    ) {
        selectionManager.selectAll()
    }

    val selectAllNeuronsAction = networkPanel.createAction(
        name = "Select All Neurons",
        description = "Select all neurons (n)",
        keyboardShortcut = KeyCombination('N')
    ) {
        selectionManager.clear()
        selectionManager.set(filterScreenElements<NeuronNode>())
        selectionManager.add(filterScreenElements<NeuronArrayNode>())
    }


    val selectAllWeightsAction = networkPanel.createAction(
        name = "Select All Weights",
        description = "Select all weights (w)",
        keyboardShortcut = KeyCombination('W')
    ) {
        selectionManager.clear()
        selectionManager.set(filterScreenElements<SynapseNode>())
        selectionManager.add(filterScreenElements<WeightMatrixNode>())
    }

    val selectIncomingWeightsAction = networkPanel.createAction(
        name = "Select Incoming Weights",
        description = "Select All Incoming Weights",
    ) {
        val selectedNeurons = selectionManager.filterSelectedModels<Neuron>()
        selectionManager.clear()
        selectedNeurons.forEach { neuron ->
            neuron.fanIn.forEach { synapse ->
                synapse.select()
            }
        }
    }
    val selectOutgoingWeightsAction = networkPanel.createAction(
        name = "Select Outgoing Weights",
        description = "Select All Outgoing Weights",
    ) {
        val selectedNeurons = selectionManager.filterSelectedModels<Neuron>()
        selectionManager.clear()
        selectedNeurons.forEach { neuron ->
            neuron.fanOut.values.forEach { synapse ->
                synapse.select()
            }
        }
    }

    val selectionEditModeAction = networkPanel.createAction(
        name = "Selection",
        description = "Selection mode",
        iconPath = "menu_icons/Arrow.png"
    ) {
        networkPanel.mouseCursor = MouseEventHandler.MouseCursor.Selection
    }

    val editSelectedModelsAction = networkPanel.createAction(
        name = "Edit selected models",
        keyboardShortcut = CmdOrCtrl + 'E',
    ) {
        networkPanel.showEditDialogsForSelectedModels()
    }

    val setNeuronPropertiesAction = networkPanel.createAction(
        name = "Neuron Properties...",
        description = "Set the properties of selected neurons",
        keyboardShortcut = CmdOrCtrl + 'E',
    ) {
        networkPanel.filterSelectedNodeByClass<NeuronNode>().firstOrNull()?.createEditDialog()?.display()
    }
    val setSourceNeurons get() = networkPanel.createConditionallyEnabledAction(
        name = "Set Source Neurons",
        description = "Set selected neurons as source neurons.  They can then be connected to target neurons using the connect commands.",
        enablingCondition = EnablingCondition.NEURONS
    ) {
        selectionManager.convertSelectedNodesToSourceNodes()
    }

    val setSynapsePropertiesAction get() = networkPanel.createAction(
        name = "Synapse Properties...",
        description = "Set the properties of selected synapses",
        keyboardShortcut = CmdOrCtrl + 'E',
    ) {
        networkPanel.filterSelectedNodeByClass<SynapseNode>().firstOrNull()?.createEditDialog()?.display()
    }
    val showEditToolBarAction = networkPanel.createAction(
        name = "Edit Toolbar",
        description = "Show the edit toolbar",
    ) {
        val cb = it?.source as? JCheckBoxMenuItem
        editToolBar.isVisible = cb?.isSelected == true
    }
    val showLayoutDialogAction = networkPanel.createConditionallyEnabledAction(
        name = "Layout Neurons...",
        description = "Lay out the selected neurons",
        enablingCondition = EnablingCondition.NEURONS
    ) {
        LayoutDialog(networkPanel).apply {
            pack()
            setLocationRelativeTo(null)
            isVisible = true
        }
    }

    val showWeightMatrixAction = networkPanel.createConditionallyEnabledAction(
        name = "Display / Edit Weight Matrix...",
        description = "Show a weight matrix connecting source neurons (adorned with red squares) and target neurons (regular green selection)",
        iconPath = "menu_icons/grid.png",
        enablingCondition = EnablingCondition.SOURCE_AND_TARGET_NEURONS,
    ) {
        val sources = selectionManager.filterSelectedSourceModels<Neuron>()
        val targets = selectionManager.filterSelectedModels<Neuron>()
        if (sources.isNotEmpty() && targets.isNotEmpty()) {
            WeightMatrixViewer(sources, targets).displayInDialog {
                commitChanges()
            }.apply {
                title = "Weight Matrix Viewer"
            }
        } else {
            throw IllegalArgumentException("Must select at least one source and one target neuron.")
        }
    }

    val spaceHorizontalAction = networkPanel.createConditionallyEnabledAction(
        name = "Space Horizontal",
        description = "Space selected nodes horizontally",
        iconPath = "menu_icons/SpaceHorizontal.png",
        enablingCondition = EnablingCondition.ALLITEMS
    ) {
        spaceHorizontal()
    }

    val spaceVerticalAction = networkPanel.createConditionallyEnabledAction(
        name = "Space Vertical",
        description = "Space selected nodes vertically",
        iconPath = "menu_icons/SpaceVertical.png",
        enablingCondition = EnablingCondition.ALLITEMS
    ) {
        spaceVertical()
    }

    fun List<Neuron>.createCoupleActivationToTimeSeriesAction() = SimbrainDesktop.actionManager.createCoupledTimeSeriesPlotAction(
        producers = map { it.getProducer(Neuron::activation) },
    )

    fun List<Synapse>.createCoupleWeightToTimeSeriesAction() = SimbrainDesktop.actionManager.createCoupledTimeSeriesPlotAction(
        producers = map { it.getProducer(Synapse::strength) },
    )

    val testInputAction = networkPanel.createConditionallyEnabledAction(
        name = "Create Input Table...",
        description = "Create a table whose rows provide input to selected neurons",
        iconPath = "menu_icons/TestInput.png",
        enablingCondition = EnablingCondition.NEURONS
    ) {
        showInputPanel(networkPanel.selectionManager.filterSelectedModels<Neuron>())
    }

    val wandEditModeAction = networkPanel.createAction(
        name = "Wand",
        description = "Wand Mode (I)",
        iconPath = "menu_icons/Wand.png",
        keyboardShortcut = KeyCombination('I')
    ) {
        networkPanel.mouseCursor = MouseEventHandler.MouseCursor.Wand
    }

    val panEditModeAction = networkPanel.createAction(
        name = "Pan",
        description = "Pan Mode",
        iconPath = "menu_icons/Hand.png"
    ) {
        networkPanel.mouseCursor = MouseEventHandler.MouseCursor.Pan
        autoZoom = false
    }

    val showMainToolBarAction = networkPanel.createAction(
        name = "Main Toolbar",
        description = "Show the main toolbar",
    ) {
        val cb = it?.source as? JCheckBoxMenuItem
        mainToolBar.isVisible = cb?.isSelected == true
    }

    val addTextAction = networkPanel.createAction(
        name = "Add Text",
        description = """Add text to the network""",
        iconPath = "menu_icons/Text.png",
        keyboardShortcut = KeyCombination('T')
    ) {
        textEntryDialog("", "Enter text to add to the network") {
            if (it.isNotEmpty()) {
                val textObject = NetworkTextObject(it)
                undoManager.addUndoableAction(
                    description = "Add text object",
                    undo = { textObject.delete() },
                    redo = { network.addNetworkModel(textObject, usePlacementManager = false, useAutoAssignedId = false)?.await() }
                )
                network.addNetworkModel(textObject)
            }
        }.display()
    }

    val exportSimbrainWebFormatAction = networkPanel.createAction(
        name = "Export Simbrain Web Format",
        description = "Export Simbrain Web Format",
        iconPath = "menu_icons/Export.png",
    ) {

        class NeuronData(val x: Double, val y: Double, val activation: Double)
        class SynapseData(val sourceIndex: Int, val targetIndex: Int, val strength: Double)

        val neuronIndexMap = network.flatNeuronList.mapIndexed { index, neuron -> neuron to index }.toMap()
        val neuronData = network.flatNeuronList.map {  neuron ->
            NeuronData(neuron.x, neuron.y, neuron.activation)
        }
        val synapseData = network.flatSynapseList.mapNotNull { synapse ->
            val sourceIndex = neuronIndexMap[synapse.source]
            val targetIndex = neuronIndexMap[synapse.target]
            val strength = synapse.strength
            if (sourceIndex != null && targetIndex != null) {
                SynapseData(sourceIndex, targetIndex, strength)
            } else {
                null
            }
        }

        val json = """
            {
                "neurons": [
                    ${neuronData.joinToString(",\n        ") { """{"x": ${it.x}, "y": ${it.y}, "activation": ${it.activation} }""" }}
                ],
                "synapses": [
                    ${synapseData.joinToString(",\n        ") { """{"sourceIndex": ${it.sourceIndex}, "targetIndex": ${it.targetIndex}, "strength": ${it.strength}}"""} }
                ]
            }
        """.trimIndent()

        // Allow test injection of file chooser
        val chooser = fileChooserForTesting ?: SFileChooser("", "Simbrain Web Format", "json")
        val file = chooser.showSaveDialog(File("network.json"))
        if (file != null) {
            try {
                println("[DEBUG_LOG] Attempting to save to file: ${file.absolutePath}")
                println("[DEBUG_LOG] File canonical path: ${file.canonicalPath}")
                println("[DEBUG_LOG] Parent directory exists: ${file.parentFile?.exists()}")

                // Ensure parent directories exist
                file.parentFile?.mkdirs()
                println("[DEBUG_LOG] Created parent directories")
                println("[DEBUG_LOG] Parent directory exists after creation: ${file.parentFile?.exists()}")

                FileOutputStream(file).use { stream ->
                    val bytes = json.toByteArray()
                    println("[DEBUG_LOG] Writing ${bytes.size} bytes to file")
                    stream.write(bytes)
                    stream.flush()
                    println("[DEBUG_LOG] File written and flushed")
                }
                println("[DEBUG_LOG] File exists after write: ${file.exists()}")
                println("[DEBUG_LOG] File size after write: ${file.length()} bytes")
            } catch (e: IOException) {
                println("[DEBUG_LOG] Error saving file: ${e.message}")
                e.printStackTrace()
                JOptionPane.showMessageDialog(
                    networkPanel,
                    "Error saving file: ${e.message}",
                    "Save Error",
                    JOptionPane.ERROR_MESSAGE
                )
            }
        } else {
            println("[DEBUG_LOG] No file selected for save")
        }


    }

    val showNetworkUpdaterDialog = networkPanel.createAction(
        name = "Edit Update Sequence...",
        description = "Edit the update sequence for this network",
        iconPath = "menu_icons/Sequence.png"
    ) {
        NetworkUpdateManagerPanel(networkPanel.network).displayInDialog()
    }

    val showNetworkDefaultsAction = networkPanel.createAction(
        name = "Network Defaults...",
        description = "Set default properties that apply to all networks in the Simbrain workspace.",
        keyboardShortcut = CmdOrCtrl + ','
    ) {
        getPreferenceDialog(NetworkPreferences).display()
    }

    val showNetworkPropertiesAction = networkPanel.createAction(
        name = "${networkPanel.networkComponent.name} Properties...",
        description = "Properties that are different for each network in the Simbrain workspace."
    ) {
        network.createEditorDialog(networkPanel.networkComponent.name).display()
    }

    val iterateNetworkAction = networkPanel.createAction(
        name = "Iterate network",
        description = "Step network update algorithm (\"spacebar\")",
        iconPath = "menu_icons/Step.png",
        keyboardShortcut = KeyCombination(KeyEvent.VK_SPACE)
    ) {
        networkPanel.network.update()
    }

    // val addDeepNetAction = if (Utils.isM1Mac()) {
    //     networkPanel.createAction(
    //         name = "Add Deep Network...",
    //         description = "Deep Network is not currently supported on M1 Macs",
    //         keyboardShortcut = CmdOrCtrl + Shift + 'D'
    //     ) {
    //         JOptionPane.showConfirmDialog(
    //             null,
    //             "Deep Network / TensorFlow for Java is not currently supported on M1 Macs."
    //         )
    //     }.also { it.isEnabled = false }
    // } else {
    //     networkPanel.createAction(
    //         name = "Add Deep Network...",
    //         description = "Create a new deep network",
    //         keyboardShortcut = CmdOrCtrl + Shift + 'D'
    //     ) {
    //         showDeepNetCreationDialog()
    //     }
    // }

    /**
     * Should be called from a combo box menu item
     */
    val toggleFreeWeightVisibility = networkPanel.createAction(
        name = "Toggle Weight Visibility",
        description = "Toggle visibilty of free weights",
        keyboardShortcut = KeyCombination('5')
    ) { event ->
        event?.source?.let {
            freeWeightsVisible = if (it is JCheckBoxMenuItem) {
                it.state
            } else {
                !freeWeightsVisible
            }
        }
    }

    val connectSelectedModels = networkPanel.createAction(
        name = "Connect selected objects...",
        description = "Creates synapse, weight matrix, etc. between selected source and target entities",
    ) {
        connectSelectedModelsDefault()
    }

    val connectWithWeightMatrix = networkPanel.createAction(
        name = "Connect selected objects with weight matrix",
    ) {
        // This will automatically connect arrays (which is all this action should be called for) with weight matrices
        connectSelectedModelsDefault()
    }

    val connectWithSynapseGroup = networkPanel.createAction(
        name = "Connect selected neuron groups with synapse group",
    ) {
        selectionManager.connectNeuronGroups()
    }

    val addGroupAction = addNeuronGroupAction()

    val clipboardActions
        get() = listOf(copyAction, cutAction, pasteAction, duplicateAction)

    val networkEditingActions
        get() = listOf(newNeuronAction, deleteAction)

    val networkModeActions
        get() = listOf(
            selectionEditModeAction,
            wandEditModeAction,
            panEditModeAction
        )

    val showSynapseAdjustmentPanel = networkPanel.createConditionallyEnabledAction(
        iconPath = "menu_icons/Rand.png",
        name = "Weight randomization dialog...",
        keyboardShortcuts = CmdOrCtrl + 'R',
        enablingCondition = EnablingCondition.SYNAPSES
    ) {
        createSynapseAdjustmentPanel(
            network.getModels<Synapse>().toList(),
            weightRandomizer,
            excitatoryRandomizer,
            inhibitoryRandomizer
        )?.displayInDialog()
    }

    val showPriorityTableAction = networkPanel.createAction(
        name = "Show Priority Table...",
        description = "Show a table of all network models and their update priorities",
        iconPath = "menu_icons/grid.png"
    ) {
        networkPanel.showPriorityTableDialog()
    }

    private fun addSubnetAction(name: String, createDialog: (NetworkPanel) -> StandardDialog) = networkPanel.createAction(
        name = name,
        description = "Add $name to network",
    ) {
        with(createDialog(networkPanel)) {
            pack()
            setLocationRelativeTo(networkPanel)
            isVisible = true

            // Not sure why call below needed, but for some reason the ok button
            // sometimes goes out of focus when creating a new dialog.
            rootPane.defaultButton = okButton
        }
    }

    private fun addNeuronGroupAction() = networkPanel.createAction(
        name = "Add Neuron Group...",
        description = "Add a neuron group to network",
        keyboardShortcut = 'G'
    ) {
        objectWrapper("Neuron Group Parameters", BasicNeuronGroupParams() as NeuronGroupParams, showLabeledBorder = false).createEditorDialog {
            it.editingObject.create().also { group ->
                group.applyLayout()
                network.addNetworkModel(group)
                val neurons = group.neuronList.toList()
                undoManager.addUndoableAction(
                    description = "Add neuron group ${group.id}",
                    undo = { group.delete() },
                    redo = {
                        group.neuronList.clear()
                        group.neuronList.addAll(neurons)
                        network.addNetworkModel(group, usePlacementManager = false, useAutoAssignedId = false)?.await()
                    }
                )
            }
        }.apply { title = "Add Neuron Group" }.display()
    }

    fun AbstractNeuronCollection.showApplyLayoutDialogAction() = networkPanel.createAction(
        name = "Apply Layout...",
        description = "Apply a layout to this neuron group",
        keyboardShortcut = CmdOrCtrl + 'L'
    ) {
        val neuronCollection = this@showApplyLayoutDialogAction
        objectWrapper("Layout", neuronCollection.layout).createEditorDialog {
            neuronCollection.layout = it.editingObject
            neuronCollection.applyLayout()
        }.display()
    }

    val newNetworkActions
        get() = listOf(
            addSubnetAction("Backprop") { BackpropCreationDialog(networkPanel) },
            createAction("Classifier") { networkPanel.showClassifierCreationDialog() },
            addSubnetAction("Competitive Network") { CompetitiveCreationDialog(networkPanel) },
            addSubnetAction("Feed Forward Network") { FeedForwardCreationDialog(networkPanel) },
            addSubnetAction("Hopfield") { HopfieldCreationDialog(networkPanel) },
            addSubnetAction("Restricted Boltzmann Machine") {
                RestrictedBoltzmannMachine.RBMCreator().createEditorDialog("Create Restricted Boltzmann Machine") {
                    addSubnetworkAction(networkPanel) { it.create() }
                }
            },
            addSubnetAction("SOM Network") { SOMCreationDialog(networkPanel) },
            addSubnetAction("SRN (Simple Recurrent Network)") { networkPanel.showSRNCreationDialog() }
        )

    fun applyConnectionAction(strategy: ConnectionStrategy): AbstractAction {
        return networkPanel.createConditionallyEnabledAction(
            name = "Connect ${strategy.name}...",
            enablingCondition = EnablingCondition.SOURCE_AND_TARGET_NEURONS
        ) {
            ConnectionStrategyPanel(strategy).displayInDialog {
                commitChanges()
                val synapses = connectionStrategy.connectNeurons(
                    selectionManager.filterSelectedSourceModels<Neuron>(),
                    selectionManager.filterSelectedModels<Neuron>()
                )
                synapses.addToNetworkAsync(network)
                undoManager.addUndoableAction(
                    description = "Connect using ${connectionStrategy.name}",
                    undo = { synapses.forEach { it.delete() } },
                    redo = { synapses.addToNetwork(network, usePlacementManager = false, useAutoAssignId = false) }
                )
            }
        }
    }

    val connectionActions
        get() = listOf(
            applyConnectionAction(AllToAll()),
            applyConnectionAction(DistanceBased()),
            applyConnectionAction(OneToOne()),
            applyConnectionAction(FixedDegree()),
            applyConnectionAction(RadialGaussian()),
            applyConnectionAction(RadialProbabilistic()),
            applyConnectionAction(Sparse())
        )

    fun createSynapseGroupVisibilityAction() = networkPanel.createAction(
        name = "Toggle visibility of selected synapse groups",
        keyboardShortcut = CmdOrCtrl + 'T',
        initBlock = {
            isEnabled = networkPanel.selectionManager.filterSelectedModels<SynapseGroup>().isNotEmpty() &&
                    networkPanel.selectionManager.filterSelectedModels<SynapseGroup>().none {
                        it.synapses.size > NetworkPreferences.synapseVisibilityThreshold
                    }
        }
    ) {
        val sgs = selectionManager.filterSelectedModels<SynapseGroup>()
        sgs.forEach {
            it.displaySynapses = !it.displaySynapses
        }
    }

    /**
     * Decay selected weights using a [DecayFunction] selected by the user.
     */
    val decayWeightsAction = networkPanel.createConditionallyEnabledAction(
        name = "Decay selected weights based on axon length",
        enablingCondition = EnablingCondition.SYNAPSES
    ) {
        DecayFunction.DecayFunctionSelector().createEditorDialog {
            selectionManager.filterSelectedModels<Synapse>()
                .decayStrengthBasedOnLength(it.decayFunction)
        }.display()
    }

    /**
     * Prune selected weights. Weights whose absolute value is less then the threshold are removed.
     */
    val pruneWeightsAction = networkPanel.createConditionallyEnabledAction(
        name = "Prune selected weights",
        enablingCondition = EnablingCondition.SYNAPSES
    ) {
        val threshold: String = JOptionPane.showInputDialog(
            null,
            "Pruning threshold:",
            ".5"
        )
        selectionManager.filterSelectedModels<Synapse>()
            .filter { Math.abs(it.strength) < threshold.toDouble() }
            .forEach { it.delete() }
    }

    /**
     * Prune selected weights. Weights whose absolute value is less then the threshold are removed.
     */
    val fastSparseAction  = networkPanel.createConditionallyEnabledAction(
        name = "Create sparse connection",
        enablingCondition = EnablingCondition.SOURCE_AND_TARGET_NEURONS
    ) {
        val sparsity: String = JOptionPane.showInputDialog(
            null,
            "Sparsity (0 to 1):",
            ".1"
        )
        val sparse = Sparse().apply {
            connectionDensity = sparsity.toDouble()
        }
        sparse.connectNeurons(
            selectionManager.filterSelectedSourceModels<Neuron>(),
            selectionManager.filterSelectedModels<Neuron>()
        ).addToNetworkAsync(network)
    }

    /**
     * Randomize the polarity of selected nodes. Note that this will change the polarity of outgoing synapses.
     */
    val randomizePolarityAction = networkPanel.createConditionallyEnabledAction(
        name = "Randomize polarity of selected neurons",
        enablingCondition = EnablingCondition.NEURONS
    ) {
        // TODO: Indicate the threshold somehow in a prompt
        objectWrapper<ProbabilityDistribution>("Randomize polarity", UniformRealDistribution()).createEditorDialog { dist ->
            selectionManager.filterSelectedModels<Neuron>().forEach { n ->
                if (dist.editingObject.sampleDouble() > .5) n.polarity = SimbrainConstants.Polarity.EXCITATORY
                else n.polarity = SimbrainConstants.Polarity.INHIBITORY
            }
        }.display()
    }

    /**
     * Quick action to apply a grid layout to selected nodes
     */
    val fastGridAction = networkPanel.createConditionallyEnabledAction(
        name = "Apply grid layout to selected nodes",
        keyboardShortcuts = CmdOrCtrl + 'L',
        enablingCondition = EnablingCondition.NEURONS
    ) {
        GridLayout().layoutNeurons(selectionManager.filterSelectedModels<Neuron>())
        network.events.zoomToFitPage.fire()
    }

    /**
     * Quick create 100 nodes
     */
    val fast100 = networkPanel.createAction(
        name = "Add 100 nodes",
    ) {
        List(100) { Neuron() }.apply {
            network.addNetworkModels(this)
            GridLayout().layoutNeurons(this)
        }.also { network.selectModels(it) }
        network.events.zoomToFitPage.fire()
    }


    fun createLayeredFreeNeurons() = networkPanel.createAction(
        name = "Add Layered Free Neurons...",
        description = "Add a set of free neurons to the network",
    ) {
        val alignment = Alignment.VERTICAL

        val topologyString = showInputDialog("Enter the number of neurons in each layer separated by commas", "5,3,5")

        val topology = topologyString.split(",").map { it.toInt() }

        network.createLayeredFreeNeurons(topology, alignment = alignment)
    }

    fun createTestInputPanelAction(layer: Layer) = networkPanel.createAction(
        name = "Input Data...",
        description = "Opens a dialog that can be used to send inputs to this layer",
        iconPath = "menu_icons/TestInput.png"
    ) {
        createTestInputPanel(layer).displayInDialog()
    }

    fun createAddActivationToInputAction(layer: Layer) = networkPanel.createAction(
        name = "Add current pattern to input data...",
        description = "Add the current activation of this layer to the input data table",
        iconPath = "menu_icons/TestInput.png"
    ) {
        layer.inputData = layer.inputData.appendRow(layer.activationArray)
    }

    fun setTextPropertiesAction(textNodes: Collection<TextNode>) = networkPanel.createAction(
        name = "Text Properties...",
        description = "Set the properties of this text, e.g. font and size",
        iconPath = "menu_icons/Properties.png"
    ) {
        networkPanel.showTextPropertyDialog(textNodes)
    }

    // Note that zoom to fit page is handled in NetworkPanel.createMainToolBar()

    fun resetZoomAction() = networkPanel.createAction(
        "Reset Zoom",
        description = "Scale to 100%",
        iconPath = "menu_icons/ZoomReset.png",
        keyboardShortcut = CmdOrCtrl + KeyEvent.VK_0
    ) {
        scalingFactor = 1.0
        autoZoom = false
    }

    fun zoomInAction() = networkPanel.createAction(
        "Zoom In",
        iconPath = "menu_icons/ZoomIn.png",
        description = "Zoom in",
        keyboardShortcuts = listOf(CmdOrCtrl + KeyEvent.VK_ADD, CmdOrCtrl + KeyEvent.VK_EQUALS)
    ) {
        scalingFactor *= 1.1
        autoZoom = false
    }

    fun zoomOutAction() = networkPanel.createAction(
        "Zoom Out",
        description = "Zoom out",
        iconPath = "menu_icons/ZoomOut.png",
        keyboardShortcuts = listOf(CmdOrCtrl + KeyEvent.VK_SUBTRACT, CmdOrCtrl + KeyEvent.VK_MINUS)
    ) {
        scalingFactor /= 1.1
        autoZoom = false
    }

    fun undoAction() = networkPanel.createAction(
        "Undo",
        description = "Undo last action",
        iconPath = "menu_icons/Undo.png",
        keyboardShortcut = CmdOrCtrl + 'Z'
    ) {
        networkPanel.undoManager.undo()
    }

    fun redoAction() = networkPanel.createAction(
        "Redo",
        description = "Redo last undone action",
        iconPath = "menu_icons/Redo.png",
        keyboardShortcuts = listOf(CmdOrCtrl + 'Y', CmdOrCtrl + Shift + 'Z')
    ) {
        networkPanel.undoManager.redo()
    }

    fun undoHistoryAction() = networkPanel.createAction(
        "Undo history...",
        description = "Show undo/redo history"
    ) {
        networkPanel.showUndoHistoryDialog()
    }

    fun canCreateSupervisedModel(): Pair<Boolean, String> {
        val reasons = mutableListOf<String>()
        val source = networkPanel.selectionManager.filterSelectedSourceModels<Layer>().firstOrNull()
        val target = networkPanel.selectionManager.filterSelectedModels<Layer>().firstOrNull()
        if (source == null) {
            reasons.add("No source model selected")
        }
        if (target == null) {
            reasons.add("No target model selected")
        }
        if (source is TransformerBlock || target is TransformerBlock) {
            reasons.add("Transformer blocks cannot be used as source or target layers")
        }
        if (source is ActivationSequence || target is ActivationSequence) {
            reasons.add("Activation sequences are not currently supported")
        }
        if (source != null && target != null) {
            try {
                computeOrderedUpdatePath(source, target)
            } catch (e: Exception) {
                reasons.add("No connection between source and target")
            }
        }
        if (networkPanel.network.getModels<SupervisedModel>().any {
            it.layers.first() == source && it.layers.last() == target
        }) {
            reasons.add("A supervised model already exists for this connection")
        }
        return reasons.isEmpty() to reasons.joinToString(", ")
    }


    val createSupervisedModelAction = networkPanel.createAction(
        name = "Create Supervised Model",
        keyboardShortcut = CmdOrCtrl + 'M',
        description = "Create supervised model with using the current activation as target for immediate training"
    ) {

        val (canCreate) = canCreateSupervisedModel()

        if (canCreate) {
            val input = selectionManager.sourceModels.firstOrNull() as? Layer
            val output = selectionManager.selectedModels.firstOrNull() as? Layer

            if (input != null && output != null) {
                val supervisedModel = SupervisedModel(input, output)
                val layers = supervisedModel.layers.toList()
                val weightMatrices = supervisedModel.weightMatrices.toList()
                network.addNetworkModel(supervisedModel)
                undoManager.addUndoableAction(
                    description = "Add supervised model to collection",
                    undo = { supervisedModel.delete() },
                    redo = {
                        supervisedModel.layers.clear()
                        supervisedModel.layers.addAll(layers)
                        supervisedModel.weightMatrices.clear()
                        supervisedModel.weightMatrices.addAll(weightMatrices)
                        network.addNetworkModel(supervisedModel, usePlacementManager = false, useAutoAssignedId = false)?.await()
                    }
                )
            }
        }

    }.also {

        fun updateAction() {
            val (canCreateSupervisedModel, cannotCreateSupervisedModelReason) = canCreateSupervisedModel()

            it.isEnabled = canCreateSupervisedModel
            it.putValue(
                SHORT_DESCRIPTION,
                "Create supervised model with using the current activation as target for immediate training"
                        + if (!canCreateSupervisedModel) " (Disabled: $cannotCreateSupervisedModelReason)" else ""
            )
        }

        updateAction()

        networkPanel.network.events.modelAdded.on { updateAction() }
        networkPanel.selectionManager.events.selection.on { _, _ -> updateAction() }
        networkPanel.selectionManager.events.sourceSelection.on { _, _ -> updateAction() }

    }

    fun createRecordActivationAction(source: Layer) = actionManager.createCoupledDataWorldAction(
        name = "Record Activations",
        source.getProducer(Layer::activationArray),
        sourceName = "${source.displayName} Activations",
        source.size
    )

}

fun addSubnetworkAction(networkPanel: NetworkPanel, block: () -> Subnetwork) {
    val subnetwork = block()
    networkPanel.network.addNetworkModel(subnetwork)
    val models = subnetwork.modelList.all
    val neuronGroups = models.filterIsInstance<NeuronGroup>()
    val neuronCollections = models.filterIsInstance<AbstractNeuronCollection>()
    val synapseGroups = models.filterIsInstance<SynapseGroup>()

    val neuronGroupNeuronsMap = neuronGroups.associateWith { it.neuronList.toList() }
    val neuronCollectionNeuronsMap = neuronCollections.associateWith { it.neuronList.toList() }
    val synapseGroupSynapsesMap = synapseGroups.associateWith { it.synapses.toList() }

    networkPanel.undoManager.addUndoableAction(
        description = "Add subnetwork ${subnetwork.id}",
        undo = { subnetwork.delete() },
        redo = {

            neuronGroupNeuronsMap.forEach { (group, neurons) ->
                group.neuronList.clear()
                group.neuronList.addAll(neurons)
            }

            neuronCollectionNeuronsMap.forEach { (collection, neurons) ->
                collection.neuronList.clear()
                collection.neuronList.addAll(neurons)
            }

            synapseGroupSynapsesMap.forEach { (group, synapses) ->
                group.synapses.clear()
                group.synapses.addAll(synapses)
            }

            subnetwork.modelList.addAll(models)
            networkPanel.network.addNetworkModel(subnetwork, usePlacementManager = false, useAutoAssignedId = false)?.await()
            subnetwork.afterRestore()
        }
    )
}
