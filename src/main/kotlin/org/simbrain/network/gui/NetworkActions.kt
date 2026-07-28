package org.simbrain.network.gui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.simbrain.network.connections.*
import org.simbrain.network.core.*
import org.simbrain.network.gui.dialogs.*
import org.simbrain.network.gui.dialogs.NetworkPreferences.excitatoryRandomizer
import org.simbrain.network.gui.dialogs.NetworkPreferences.inhibitoryRandomizer
import org.simbrain.network.gui.dialogs.NetworkPreferences.wandPalette
import org.simbrain.network.gui.dialogs.NetworkPreferences.weightRandomizer
import org.simbrain.network.gui.dialogs.neuron.AddNeuronsDialog
import org.simbrain.network.gui.nodes.*
import org.simbrain.network.layouts.GridLayout
import org.simbrain.network.subnetworks.ConvolutionalNeuralNetwork
import org.simbrain.network.subnetworks.RestrictedBoltzmannMachine
import org.simbrain.network.subnetworks.Subnetwork
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.network.trainers.computeOrderedUpdatePath
import org.simbrain.network.util.Alignment
import org.simbrain.util.*
import org.simbrain.util.decayfunctions.DecayFunction
import org.simbrain.util.propertyeditor.AnnotatedPropertyEditor
import org.simbrain.util.propertyeditor.objectWrapper
import org.simbrain.util.stats.ProbabilityDistribution
import org.simbrain.util.stats.distributions.TwoValued
import org.simbrain.workspace.couplings.getConsumer
import org.simbrain.workspace.couplings.getProducer
import org.simbrain.workspace.gui.SimbrainDesktop.actionManager
import java.awt.BorderLayout
import java.awt.event.KeyEvent
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.swing.AbstractAction
import javax.swing.Action.SHORT_DESCRIPTION
import javax.swing.JCheckBoxMenuItem
import javax.swing.JLabel
import javax.swing.JPanel

class NetworkActions(val networkPanel: NetworkPanel) {
    // For testing purposes only
    var fileChooserForTesting: SFileChooser? = null
    val addNeuronsAction = networkPanel.createAction(
        name = "Add neurons...",
        description = "Add a set of neurons to the network",
    ) {
        AddNeuronsDialog(networkPanel).display()
    }
    val alignHorizontalAction = networkPanel.createConditionallyEnabledAction(
        name = "Align horizontal",
        description = "Align selected nodes horizontally",
        iconPath = "menu_icons/AlignHorizontal.png",
        enablingCondition = EnablingConditions.LOCATABLE_MODELS
    ) {
        alignHorizontal()
    }
    val alignVerticalAction = networkPanel.createConditionallyEnabledAction(
        name = "Align vertical",
        description = "Align selected nodes vertically",
        iconPath = "menu_icons/AlignVertical.png",
        enablingCondition = EnablingConditions.LOCATABLE_MODELS
    ) {
        alignVertical()
    }
    val clearNodeActivationsAction = networkPanel.createConditionallyEnabledAction(
        name = "Clear activations of all nodes",
        description = "Clear all node activations (c)",
        iconPath = "menu_icons/Eraser.png",
        enablingCondition = EnablingConditions.ALLITEMS,
        keyboardShortcuts = KeyCombination('C')
    ) {
        clearSelectedObjects()
    }
    val clearSourceNeurons = networkPanel.createConditionallyEnabledAction(
        name = "Clear source neurons",
        description = "Remove all source neurons (neurons with red squares around them)",
        enablingCondition = EnablingConditions.SOURCE_NEURONS
    ) {
        selectionManager.clearAllSource()
    }
    val copyAction = networkPanel.createConditionallyEnabledAction(
        name = "Copy",
        description = "Copy selected neurons, (connected) synapses, and neuron groups",
        keyboardShortcuts = CmdOrCtrl + 'C',
        iconPath = "menu_icons/Copy.png",
        enablingCondition = EnablingConditions.LOCATABLE_MODELS
    ) {
        copy()
    }
    val cutAction = networkPanel.createConditionallyEnabledAction(
        name = "Cut",
        description = "Cut selected neurons, (connected) synapses, and neuron groups",
        keyboardShortcuts = CmdOrCtrl + 'X',
        iconPath = "menu_icons/Cut.png",
        enablingCondition = EnablingConditions.LOCATABLE_MODELS
    ) {
        cut()
    }
    val pasteAction = networkPanel.createConditionallyEnabledAction(
        name = "Paste",
        description = "Paste copied neurons, (connected) synapses, and neuron groups (Cmd/Ctrl-V)",
        keyboardShortcuts = CmdOrCtrl + 'V',
        iconPath = "menu_icons/Paste.png",
        enablingCondition = EnablingConditions.CLIPBOARD_NOT_EMPTY
    ) {
        paste()
    }
    val duplicateAction = networkPanel.createConditionallyEnabledAction(
        name = "Duplicate",
        description = "Duplicate selected neurons, (connected) synapses, and neuron groups (Cmd/Ctrl-D)",
        keyboardShortcuts = CmdOrCtrl + 'D',
        iconPath = "menu_icons/Copy.png",
        enablingCondition = EnablingConditions.LOCATABLE_MODELS
    ) {
        duplicate()
    }
    val addNeuronArrayAction = networkPanel.createAction(
        name = "Add neuron array...",
        description = "Add a neuron array to the network (y)",
        keyboardShortcut = KeyCombination('Y')
    ) {
        showNeuronArrayCreationDialog()
    }
    val addTensorAction = networkPanel.createAction(
        name = "Add tensor layer...",
        description = "Add a tensor (CNN layer) to the network",
        keyboardShortcut = Shift + 'T'
    ) {
        showTensorCreationDialog()
    }
    val addActivationSequenceAction = networkPanel.createAction(
        name = "Add activation sequence...",
        description = "Add an activation sequence to the network",
    ) {
        showActivationSequenceCreationDialog()
    }
    val addLanguageModelAction = networkPanel.createAction(
        name = "Add language model...",
        description = "Add a language model (LFM2.5-230M) to the network (shift-l)",
        keyboardShortcut = Shift + 'L',
    ) {
        showLanguageModelCreationDialog()
    }
    val addTeachingTransformerAction = networkPanel.createAction(
        name = "Add teaching transformer...",
        description = "Add a small trainable transformer with an op-level teaching interior (shift-m)",
        keyboardShortcut = Shift + 'M',
    ) {
        showTeachingTransformerCreationDialog()
    }
    val addClassifierAction = createAction("Add classifier") {
        networkPanel.showClassifierCreationDialog()
    }

    val deleteAction = networkPanel.createConditionallyEnabledAction(
        name = "Delete",
        description = "Delete selected node(s) (Backspace or Delete)",
        enablingCondition = EnablingConditions.ALLITEMS,
        iconPath = "menu_icons/minus.png",
        keyboardShortcuts = listOf(KeyCombination(KeyEvent.VK_DELETE), KeyCombination(KeyEvent.VK_BACK_SPACE))
    ) {
        launch { deleteSelectedObjects() }
    }

    val neuronCollectionAction = networkPanel.createConditionallyEnabledAction(
        name = "Add neurons to collection",
        description = "Add selected neurons to a neuron collection (Cmd/Ctrl-G)",
        enablingCondition = EnablingConditions.NEURONS,
        keyboardShortcuts = CmdOrCtrl + 'G'
    ) {
        val neuronList = selectionManager.filterSelectedModels<Neuron>()
        if (neuronList.isNotEmpty()) {
            val nc = NeuronCollection(neuronList)
            if (with(network) { nc.shouldAdd() }) {
                network.addNetworkModelAsync(nc)
            }
            undoManager.addUndoableAction(
                description = "Add neurons to collection",
                undo = { nc.delete() },
                redo = {
                    nc.neuronList.clear()
                    nc.neuronList.addAll(neuronList)
                    network.addNetworkModel(nc, usePlacementManager = false, useAutoAssignedId = false)
                }
            )
        }
    }
    val newNeuronAction = networkPanel.createAction(
        name = "Add neuron",
        description = "Add or put a new node (p)",
        iconPath = "menu_icons/plus.png",
        keyboardShortcut = KeyCombination('P')
    ) {
        val neuron = Neuron()
        network.addNetworkModelAsync(neuron)
        network.selectModels(listOf(neuron))
        undoManager.addUndoableAction(
            description = "Add neuron ${neuron.id}",
            undo = { neuron.delete() },
            redo = { network.addNetworkModel(neuron, usePlacementManager = false, useAutoAssignedId = false) }
        )
    }
    val randomizeObjectsAction = networkPanel.createConditionallyEnabledAction(
        name = "Randomize selection",
        description = "Randomize selected elements (r)",
        enablingCondition = EnablingConditions.ALLITEMS,
        iconPath = "menu_icons/Rand.png",
        keyboardShortcuts = KeyCombination('R')
    ) {
        if (networkPanel.hasAnyPixelSelection()) {
            networkPanel.randomizeSelectedPixels()
            return@createConditionallyEnabledAction
        }
        with(network) {
            selectionManager.selectedModels.map { it.randomize() }
        }
    }
    val randomizeBiasesAction = networkPanel.createAction(
        name = "Randomize biases",
        description = "Randomize biases of selected nodes (Cmd/Ctrl-B)",
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
        name = "Select all",
        description = "Select all network items (a or Cmd/Ctrl-A)",
        keyboardShortcuts = listOf(KeyCombination('A'), CmdOrCtrl + 'A')
    ) {
        selectionManager.selectAll()
    }

    val selectAllNeuronsAction = networkPanel.createAction(
        name = "Select all neurons",
        description = "Select all neurons (n)",
        keyboardShortcut = KeyCombination('N')
    ) {
        selectionManager.clear()
        selectionManager.set(filterScreenElements<NeuronNode>())
        selectionManager.add(filterScreenElements<NeuronArrayNode>())
    }


    val selectAllWeightsAction = networkPanel.createAction(
        name = "Select all weights",
        description = "Select all weights (w)",
        keyboardShortcut = KeyCombination('W')
    ) {
        selectionManager.clear()
        selectionManager.set(filterScreenElements<SynapseNode>())
        selectionManager.add(filterScreenElements<WeightMatrixNode>())
    }

    val selectIncomingWeightsAction = networkPanel.createAction(
        name = "Select incoming weights",
        description = "Select all incoming weights",
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
        name = "Select outgoing weights",
        description = "Select all outgoing weights",
    ) {
        val selectedNeurons = selectionManager.filterSelectedModels<Neuron>()
        selectionManager.clear()
        selectedNeurons.forEach { neuron ->
            neuron.fanOut.values.forEach { synapse ->
                synapse.select()
            }
        }
    }

    val editSelectedModelsAction = networkPanel.createAction(
        name = "Edit selected models",
        keyboardShortcut = CmdOrCtrl + 'E',
        initBlock = {
            fun updateAction() {
                isEnabled = networkPanel.selectionManager.selection.isNotEmpty()
            }
            updateAction()
            networkPanel.selectionManager.events.selection.on(Dispatchers.Default) { _, _ -> updateAction() }
        }
    ) {
        networkPanel.showEditDialogsForSelectedModels()
    }

    val setNeuronPropertiesAction get() = run {
        val selectedNeurons = networkPanel.selectionManager.filterSelectedModels<Neuron>()
        val count = selectedNeurons.size
        networkPanel.createAction(
            name = "Edit $count ${if (count == 1) "neuron" else "neurons"}...",
            description = "Set the properties of selected neurons (Cmd/Ctrl-E)",
            keyboardShortcut = CmdOrCtrl + 'E',
        ) {
            networkPanel.filterSelectedNodeByClass<NeuronNode>().firstOrNull()?.createEditDialog()?.display()
        }
    }
    val setSourceNeurons get() = networkPanel.createConditionallyEnabledAction(
        name = "Set source neurons",
        description = "Set selected neurons as source neurons. They can then be connected to target neurons using the connect commands.",
        enablingCondition = EnablingConditions.NEURONS
    ) {
        selectionManager.convertSelectedNodesToSourceNodes()
    }

    val setSynapsePropertiesAction get() = run {
        val selectedSynapses = networkPanel.selectionManager.filterSelectedModels<Synapse>()
        val count = selectedSynapses.size
        networkPanel.createAction(
            name = "Edit $count ${if (count == 1) "synapse" else "synapses"}...",
            description = "Set the properties of selected synapses (Cmd/Ctrl-E)",
            keyboardShortcut = CmdOrCtrl + 'E',
        ) {
            networkPanel.filterSelectedNodeByClass<SynapseNode>().firstOrNull()?.createEditDialog()?.display()
        }
    }
    val showEditToolBarAction = networkPanel.createAction(
        name = "Edit toolbar",
        description = "Show the edit toolbar",
    ) {
        val cb = it?.source as? JCheckBoxMenuItem
        editToolBar.isVisible = cb?.isSelected == true
    }
    val showLayoutDialogAction = networkPanel.createConditionallyEnabledAction(
        name = "Layout neurons...",
        description = "Lay out the selected neurons",
        enablingCondition = EnablingConditions.NEURONS
    ) {
        LayoutDialog(networkPanel).apply {
            pack()
            setLocationRelativeTo(null)
            isVisible = true
        }
    }

    val showWeightMatrixAction = networkPanel.createConditionallyEnabledAction(
        name = "Display/edit matrix for free weights...",
        description = "Show a weight matrix connecting source neurons (adorned with red squares) and target neurons (regular green selection)",
        iconPath = "menu_icons/grid.png",
        enablingCondition = EnablingConditions.SOURCE_AND_TARGET_NEURONS,
    ) {
        val sources = selectionManager.filterSelectedSourceModels<Neuron>()
        val targets = selectionManager.filterSelectedModels<Neuron>()
        if (sources.isNotEmpty() && targets.isNotEmpty()) {
            WeightMatrixViewer(sources, targets).displayInDialog {
                commitChanges()
            }.apply {
                title = "Weight matrix viewer"
            }
        } else {
            throw IllegalArgumentException("Must select at least one source and one target neuron.")
        }
    }

    val spaceHorizontalAction = networkPanel.createConditionallyEnabledAction(
        name = "Space horizontal",
        description = "Space selected nodes horizontally",
        iconPath = "menu_icons/SpaceHorizontal.png",
        enablingCondition = EnablingConditions.LOCATABLE_MODELS
    ) {
        spaceHorizontal()
    }

    val spaceVerticalAction = networkPanel.createConditionallyEnabledAction(
        name = "Space vertical",
        description = "Space selected nodes vertically",
        iconPath = "menu_icons/SpaceVertical.png",
        enablingCondition = EnablingConditions.LOCATABLE_MODELS
    ) {
        spaceVertical()
    }

    fun List<Neuron>.createCoupleActivationToTimeSeriesAction() = actionManager.createCoupledTimeSeriesPlotAction(
        producers = map { it.getProducer(Neuron::activation) },
    )

    fun List<Synapse>.createCoupleWeightToTimeSeriesAction() = actionManager.createCoupledTimeSeriesPlotAction(
        producers = map { it.getProducer(Synapse::strength) },
    )

    val testInputAction = networkPanel.createConditionallyEnabledAction(
        name = "Create input table...",
        description = "Create a table whose rows provide input to selected neurons",
        iconPath = "menu_icons/TestInput.png",
        enablingCondition = EnablingConditions.NEURONS
    ) {
        showInputPanel(networkPanel.selectionManager.filterSelectedModels<Neuron>())
    }


    val selectionEditModeAction = networkPanel.createAction(
        name = "Select",
        description = "Select (s)",
        iconPath = "menu_icons/Arrow.png",
        keyboardShortcut = KeyCombination('S')
    ) {
        networkPanel.mouseCursor = MouseEventHandler.MouseCursor.Selection
    }

    val wandEditModeAction = networkPanel.createAction(
        name = "Activate",
        description = "Activate nodes (d)",
        iconPath = "menu_icons/ActivationTool.png",
        keyboardShortcut = KeyCombination('D')
    ) {
        wandPalette.selectedAction?.let { action ->
            MouseEventHandler.MouseCursor.Wand.update(action.color, action.radius)
        }
        networkPanel.mouseCursor = MouseEventHandler.MouseCursor.Wand
    }

    val panEditModeAction = networkPanel.createAction(
        name = "Pan",
        description = "Pan",
        iconPath = "menu_icons/Hand.png"
    ) {
        networkPanel.mouseCursor = MouseEventHandler.MouseCursor.Pan
        autoZoom = false
    }

    val showMainToolBarAction = networkPanel.createAction(
        name = "Main toolbar",
        description = "Show the main toolbar",
    ) {
        val cb = it?.source as? JCheckBoxMenuItem
        mainToolBar.isVisible = cb?.isSelected == true
    }

    val addTextAction = networkPanel.createAction(
        name = "Add text",
        description = "Add text to the network (t)",
        iconPath = "menu_icons/Text.png",
        keyboardShortcut = KeyCombination('T')
    ) {
        textEntryDialog("", "Enter text to add to the network") {
            if (it.isNotEmpty()) {
                val textObject = NetworkTextObject(it)
                undoManager.addUndoableAction(
                    description = "Add text object",
                    undo = { textObject.delete() },
                    redo = { network.addNetworkModel(textObject, usePlacementManager = false, useAutoAssignedId = false) }
                )
                network.addNetworkModelAsync(textObject)
            }
        }.display()
    }

    val exportSimbrainWebFormatAction = networkPanel.createAction(
        name = "Export Simbrain web format",
        description = "Export Simbrain web format",
        iconPath = "menu_icons/export.png",
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
                showErrorDialog("Error saving file: ${e.message}", "Save error")
            }
        } else {
            println("[DEBUG_LOG] No file selected for save")
        }


    }

    val showNetworkUpdaterDialog = networkPanel.createAction(
        name = "Edit update sequence...",
        description = "Edit the update sequence for this network",
        iconPath = "menu_icons/Sequence.png"
    ) {
        NetworkUpdateManagerPanel(networkPanel.network).displayInDialog()
    }

    val showNetworkDefaultsAction = networkPanel.createAction(
        name = "Network defaults...",
        description = "Set default properties that apply to all networks in the Simbrain workspace (Cmd/Ctrl-,)",
        keyboardShortcut = CmdOrCtrl + ','
    ) {
        getPreferenceDialog(NetworkPreferences).display()
    }

    val showNetworkPropertiesAction = networkPanel.createAction(
        name = "${networkPanel.networkComponent.name} properties...",
        description = "Properties that are different for each network in the Simbrain workspace."
    ) {
        network.createEditorDialog(networkPanel.networkComponent.name).display()
    }

    val iterateNetworkAction = networkPanel.createAction(
        name = "Iterate network",
        description = "Step network update algorithm (spacebar)",
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
        name = "Toggle weight visibility",
        description = "Toggle visibility of free weights (5)",
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

    /**
     * Toggle synapse spiking-only display mode.
     */
    val toggleSynapseSpikingOnlyVisibility = networkPanel.createAction(
        name = "Only show synapses when spiking",
        description = "When enabled, synapses are only drawn while their source neuron is spiking"
    ) { event ->
        event?.source?.let {
            networkPanel.synapseSpikingOnlyVisible = if (it is JCheckBoxMenuItem) {
                it.state
            } else {
                !networkPanel.synapseSpikingOnlyVisible
            }
        }
    }

    val toggleAutoZoom = networkPanel.createAction(
        name = "Auto-zoom",
        description = "Automatically fit all network objects in the window"
    ) { event ->
        event?.source?.let {
            autoZoom = if (it is JCheckBoxMenuItem) {
                it.state
            } else {
                !autoZoom
            }
        }
    }

    fun canConnectSelectedModels(): Pair<Boolean, String> {
        val reasons = mutableListOf<String>()
        val sourceNeurons = networkPanel.selectionManager.filterSelectedSourceModels<Neuron>()
        val targetNeurons = networkPanel.selectionManager.filterSelectedModels<Neuron>()
        val sourceLayers = networkPanel.selectionManager.filterSelectedSourceModels<Layer>()
        val targetLayers = networkPanel.selectionManager.filterSelectedModels<Layer>()
        val sourceTensors = networkPanel.selectionManager.filterSelectedSourceModels<TensorLayer>()
        val targetTensors = networkPanel.selectionManager.filterSelectedModels<TensorLayer>()

        val hasValidNeuronConnection = sourceNeurons.isNotEmpty() && targetNeurons.isNotEmpty()
        val hasValidLayerConnection = sourceLayers.isNotEmpty() && targetLayers.isNotEmpty()

        if (!hasValidNeuronConnection && !hasValidLayerConnection) {
            reasons.add("No valid source and target selections")
        }

        if (sourceTensors.isNotEmpty() || targetTensors.isNotEmpty()) {
            reasons.add("TensorLayers require special connectors (use CNN creation dialog)")
        }

        return reasons.isEmpty() to reasons.joinToString(", ")
    }

    val connectSelectedModels = networkPanel.createAction(
        name = "Connect selected objects...",
        description = "Creates synapse, weight matrix, etc. between selected source and target entities",
    ) {
        connectSelectedModelsDefault()
    }.also {
        fun updateAction() {
            val (canConnect, reason) = canConnectSelectedModels()
            it.isEnabled = canConnect
            it.putValue(
                SHORT_DESCRIPTION,
                "Create appropriate connection between selected source and target entities"
                        + if (!canConnect) " (Disabled: $reason)" else ""
            )
        }

        updateAction()

        networkPanel.selectionManager.events.selection.on(Dispatchers.Default) { _, _ -> updateAction() }
        networkPanel.selectionManager.events.sourceSelection.on(Dispatchers.Default) { _, _ -> updateAction() }
    }

    val connectWithWeightMatrix = networkPanel.createAction(
        name = "Connect selected objects with weight matrix",
    ) {
        // This will automatically connect arrays (which is all this action should be called for) with weight matrices
        connectSelectedModelsDefault()
    }.also {
        fun updateAction() {
            val (canConnect, reason) = canConnectSelectedModels()
            it.isEnabled = canConnect
            it.putValue(
                SHORT_DESCRIPTION,
                "Connect selected objects with weight matrix"
                        + if (!canConnect) " (Disabled: $reason)" else ""
            )
        }

        updateAction()

        networkPanel.selectionManager.events.selection.on(Dispatchers.Default) { _, _ -> updateAction() }
        networkPanel.selectionManager.events.sourceSelection.on(Dispatchers.Default) { _, _ -> updateAction() }
    }

    val connectWithSynapseGroup = networkPanel.createAction(
        name = "Connect selected neuron groups with synapse group",
    ) {
        selectionManager.connectNeuronCollections()
    }

    val addGroupAction = addNeuronCollectionAction()

    val clipboardActions
        get() = listOf(copyAction, cutAction, pasteAction, duplicateAction)

    val networkEditingActions
        get() = listOf(newNeuronAction, deleteAction)

    val networkModeActions
        get() = listOf(
            selectionEditModeAction,
            panEditModeAction
        )

    val showSynapseAdjustmentPanel = networkPanel.createConditionallyEnabledAction(
        iconPath = "menu_icons/Rand.png",
        name = "Weight randomization dialog...",
        keyboardShortcuts = CmdOrCtrl + 'R',
        enablingCondition = EnablingConditions.SYNAPSES
    ) {
        createSynapseAdjustmentPanel(
            networkPanel.selectionManager.filterSelectedModels<Synapse>(),
            weightRandomizer,
            excitatoryRandomizer,
            inhibitoryRandomizer
        )?.displayInDialog()
    }

    val showWeightMatrixAdjustmentPanel = networkPanel.createConditionallyEnabledAction(
        name = "Histogram and custom randomization...",
        enablingCondition = EnablingConditions.WEIGHT_MATRICES
    ) {
        val selectedMatrices = networkPanel.selectionManager.filterSelectedModels<WeightMatrix>()
        if (selectedMatrices.isNotEmpty()) {
            WeightMatrixAdjustmentPanel(selectedMatrices.first()).displayInDialog()
        }
    }

    val showPriorityTableAction = networkPanel.createAction(
        name = "Show priority table...",
        description = "Show a table of all network models and their update priorities",
        iconPath = "menu_icons/grid.png"
    ) {
        networkPanel.showPriorityTableDialog()
    }

    val showNetworkDebugInfoAction = networkPanel.createAction(
        name = "Show network debug info...",
        description = "Show debug information about all network models"
    ) {
        networkPanel.showNetworkDebugInfoDialog()
    }

    private fun addSubnetAction(name: String, createDialog: (NetworkPanel) -> StandardDialog) = networkPanel.createAction(
        name = "$name...",
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

    private fun addNeuronCollectionAction() = networkPanel.createAction(
        name = "Add neuron collection...",
        description = "Add a neuron collection to network (g)",
        keyboardShortcut = 'G'
    ) {
        val numNeurons = showNumericInputDialog("Number of neurons", 10) ?: return@createAction
        val neurons = List(numNeurons) { Neuron() }
        neurons.forEach { network.addNetworkModelAsync(it) }
        val group = NeuronCollection(neurons)
        group.setLayoutBasedOnSize()
        group.applyLayout()
        network.addNetworkModelAsync(group)
        undoManager.addUndoableAction(
            description = "Add neuron collection ${group.id}",
            undo = { group.delete() },
            redo = {
                group.neuronList.clear()
                group.neuronList.addAll(neurons)
                neurons.forEach { network.addNetworkModelAsync(it, usePlacementManager = false, useAutoAssignedId = false) }
                network.addNetworkModel(group, usePlacementManager = false, useAutoAssignedId = false)
            }
        )
    }

    fun NeuronCollection.showApplyLayoutDialogAction() = networkPanel.createAction(
        name = "Apply layout...",
        description = "Apply a layout to this neuron group (Cmd/Ctrl-L)",
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
            addSubnetAction("Competitive network") { CompetitiveCreationDialog(networkPanel) },
            addSubnetAction("Convolutional neural network") { CnnCreationDialog(networkPanel) },
            addSubnetAction("Feed forward network") { FeedForwardCreationDialog(networkPanel) },
            addSubnetAction("Hopfield") { HopfieldCreationDialog(networkPanel) },
            addSubnetAction("Restricted Boltzmann machine") {
                RestrictedBoltzmannMachine.RBMCreator().createEditorDialog("Create Restricted Boltzmann machine") {
                    addSubnetworkAction(networkPanel) { it.create() }
                }
            },
            addSubnetAction("SOM network") { SOMCreationDialog(networkPanel) },
            addSubnetAction("SRN (simple recurrent network)") { networkPanel.showSRNCreationDialog() }
        )

    fun applyConnectionAction(strategy: ConnectionStrategy): AbstractAction {
        return networkPanel.createConditionallyEnabledAction(
            name = "Connect ${strategy.name}...",
            enablingCondition = EnablingConditions.SOURCE_AND_TARGET_NEURONS
        ) {
            ConnectionStrategyPanel(strategy).displayInDialog {
                commitChanges()
                val synapses = connectionStrategy.connectNeurons(
                    selectionManager.filterSelectedSourceModels<Neuron>(),
                    selectionManager.filterSelectedModels<Neuron>()
                )
                synapses.addToNetworkAsync(network)
                // Select synapses after creating them so it is easy to edit the new synapses
                // When using key commands we don't do this, to facilitae quick creation using the "1-2 trick"
                networkPanel.selectionManager.clear()
                networkPanel.selectionManager.clearAllSource()
                network.selectModels(synapses)
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
            applyConnectionAction(Sparse())
        )

    fun createSynapseGroupVisibilityAction() = networkPanel.createAction(
        name = "Toggle visibility of selected synapse groups",
        keyboardShortcut = CmdOrCtrl + 'T',
    ) {
        val (groupsAboveThreshold, groupsBelowThreshold) = selectionManager.filterSelectedModels<SynapseGroup>().partition {
            it.synapses.size > NetworkPreferences.synapseVisibilityThreshold
        }
        if (groupsAboveThreshold.isNotEmpty()) {
            showWarningDialog("Current visibility threshold: ${NetworkPreferences.synapseVisibilityThreshold}. \nCould not toggle visibility of:\n${groupsAboveThreshold.joinToString("\n") { "  ${it.displayName} (size: ${it.size()})" }}")
        }
        // A manual toggle pins visibility: stop auto-tracking the threshold so the user's choice sticks.
        groupsBelowThreshold.forEach {
            it.autoVisibility = false
            it.displaySynapses = !it.displaySynapses
        }
    }

    /**
     * Decay selected weights using a [DecayFunction] selected by the user.
     */
    val decayWeightsAction = networkPanel.createConditionallyEnabledAction(
        name = "Decay selected weights based on axon length",
        enablingCondition = EnablingConditions.SYNAPSES
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
        enablingCondition = EnablingConditions.SYNAPSES
    ) {
        val threshold = showInputDialog("Pruning threshold:", ".5") ?: return@createConditionallyEnabledAction
        selectionManager.filterSelectedModels<Synapse>()
            .filter { Math.abs(it.strength) < threshold.toDouble() }
            .forEach { it.delete() }
    }

    /**
     * Prune selected weights. Weights whose absolute value is less then the threshold are removed.
     */
    val fastSparseAction  = networkPanel.createConditionallyEnabledAction(
        name = "Create sparse connection",
        enablingCondition = EnablingConditions.SOURCE_AND_TARGET_NEURONS
    ) {
        val sparsity = showInputDialog("Sparsity (0 to 1):", ".1") ?: return@createConditionallyEnabledAction
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
        enablingCondition = EnablingConditions.NEURONS
    ) {
        val wrapper = objectWrapper<ProbabilityDistribution>(
            "Randomize polarity",
            TwoValued(lowerValue = 0.0, upperValue = 1.0, p = 0.8)
        )
        val editor = AnnotatedPropertyEditor(listOf(wrapper))
        val instructions = JLabel(
            "<html><b>Mapping:</b> sample &gt; 0.5 = excitatory, sample &le; 0.5 = inhibitory.<br>" +
                "Suggest using two valued distribution with lower=0 and upper=1, so that probability is for percent excitatory.</html>"
        )
        StandardDialog().apply {
            title = "Randomize polarity"
            contentPane = JPanel(BorderLayout()).apply {
                add(instructions, BorderLayout.NORTH)
                add(editor, BorderLayout.CENTER)
            }
            addCommitTask {
                editor.commitChanges()
                selectionManager.filterSelectedModels<Neuron>().forEach { n ->
                    if (wrapper.editingObject.sampleDouble() > .5) n.polarity = SimbrainConstants.Polarity.EXCITATORY
                    else n.polarity = SimbrainConstants.Polarity.INHIBITORY
                }
            }
        }.display()
    }

    /**
     * Quick action to apply a grid layout to selected nodes
     */
    val fastGridAction = networkPanel.createConditionallyEnabledAction(
        name = "Apply grid layout to selected nodes",
        keyboardShortcuts = CmdOrCtrl + 'L',
        enablingCondition = EnablingConditions.NEURONS
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
            network.addNetworkModelsAsync(this)
            GridLayout().layoutNeurons(this)
        }.also { network.selectModels(it) }
        network.events.zoomToFitPage.fire()
    }


    fun createLayeredFreeNeurons() = networkPanel.createAction(
        name = "Add layered free neurons...",
        description = "Add a set of free neurons to the network",
    ) {
        val alignment = Alignment.VERTICAL

        val topologyString = showInputDialog("Enter the number of neurons in each layer separated by commas", "5,3,5") ?: return@createAction

        val topology = topologyString.split(",").map { it.toInt() }

        network.createLayeredFreeNeurons(topology, alignment = alignment)
    }

    fun createTestInputPanelAction(layer: Layer) = networkPanel.createAction(
        name = "Input data...",
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
        name = "Text properties...",
        description = "Set the properties of this text, e.g. font and size",
        iconPath = "menu_icons/Properties.png"
    ) {
        networkPanel.showTextPropertyDialog(textNodes)
    }

    // Note that zoom to fit page is handled in NetworkPanel.createMainToolBar()

    fun resetZoomAction() = networkPanel.createAction(
        "Reset zoom",
        description = "Scale to 100%",
        iconPath = "menu_icons/ZoomReset.png",
        keyboardShortcut = CmdOrCtrl + KeyEvent.VK_0
    ) {
        scalingFactor = 1.0
        autoZoom = false
    }

    fun zoomInAction() = networkPanel.createAction(
        "Zoom in",
        iconPath = "menu_icons/ZoomIn.png",
        description = "Zoom in",
        keyboardShortcuts = listOf(CmdOrCtrl + KeyEvent.VK_ADD, CmdOrCtrl + KeyEvent.VK_EQUALS)
    ) {
        scalingFactor *= 1.1
        autoZoom = false
    }

    fun zoomOutAction() = networkPanel.createAction(
        "Zoom out",
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
        if (source == target) {
            reasons.add("Source and target cannot be the same")
        }
        if (source is ActivationSequence || target is ActivationSequence) {
            reasons.add("Activation sequences are not currently supported")
        }
        if (source != null && target != null) {
            try {
                computeOrderedUpdatePath(setOf(source), target)
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
        name = "Create supervised model",
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
                network.addNetworkModelAsync(supervisedModel)
                undoManager.addUndoableAction(
                    description = "Add supervised model to collection",
                    undo = { supervisedModel.delete() },
                    redo = {
                        supervisedModel.layers.clear()
                        supervisedModel.layers.addAll(layers)
                        supervisedModel.weightMatrices.clear()
                        supervisedModel.weightMatrices.addAll(weightMatrices)
                        network.addNetworkModel(supervisedModel, usePlacementManager = false, useAutoAssignedId = false)
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

        networkPanel.network.events.modelAdded.on(Dispatchers.Default) { updateAction() }
        networkPanel.selectionManager.events.selection.on(Dispatchers.Default) { _, _ -> updateAction() }
        networkPanel.selectionManager.events.sourceSelection.on(Dispatchers.Default) { _, _ -> updateAction() }

    }

    fun canCreateConvolutionalNeuralNetwork(): Pair<Boolean, String> {
        val reasons = mutableListOf<String>()
        val source = networkPanel.selectionManager.filterSelectedSourceModels<TensorLayer>().firstOrNull()
        val target = networkPanel.selectionManager.filterSelectedModels<NeuronArray>().firstOrNull()
        if (source == null) {
            reasons.add("No source Tensor selected (Ctrl+click a Tensor to set as source)")
        }
        if (target == null) {
            reasons.add("No target NeuronArray selected")
        }
        if (source != null && target != null) {
            // Check that a path exists through the CNN pipeline
            try {
                // Verify pipeline can be discovered
                var currentTensorLayer: TensorLayer = source
                var foundFlatten = false
                val pipelineModels = mutableListOf<NetworkModel>(source)
                while (true) {
                    if (currentTensorLayer.outgoingFlattenConnectors.isNotEmpty()) {
                        val flatten = currentTensorLayer.outgoingFlattenConnectors.first()
                        pipelineModels.add(flatten)
                        pipelineModels.add(flatten.target)
                        foundFlatten = true
                        break
                    }
                    val outgoing = currentTensorLayer.outgoingTensorConnectors
                    if (outgoing.isEmpty()) break
                    val connector = outgoing.first()
                    pipelineModels.add(connector)
                    pipelineModels.add(connector.target)
                    currentTensorLayer = connector.target
                }
                if (!foundFlatten) {
                    reasons.add("No Flatten connector found in pipeline from source Tensor")
                } else {
                    // Walk the dense chain from flatten target to verify it reaches the selected target
                    val flattenTarget = pipelineModels.last() as Layer
                    var currentLayer: Layer = flattenTarget
                    var denseLayerCount = 0
                    while (currentLayer != target) {
                        val wm = currentLayer.outgoingConnectors
                            .filterIsInstance<WeightMatrix>().firstOrNull() ?: break
                        pipelineModels.add(wm)
                        pipelineModels.add(wm.target)
                        currentLayer = wm.target
                        denseLayerCount++
                    }
                    val reachesTarget = currentLayer == target
                    if (!reachesTarget) {
                        reasons.add("Dense chain from Flatten does not reach the selected target NeuronArray")
                    }
                    if (denseLayerCount == 0) {
                        reasons.add("No dense layers found in pipeline")
                    }

                    val owned = pipelineModels
                        .distinct()
                        .filter { networkPanel.network.childToParentMap[it] != null }
                    if (owned.isNotEmpty()) {
                        reasons.add("Some pipeline models are already owned by another parent")
                    }
                }
            } catch (e: Exception) {
                reasons.add("Error discovering pipeline: ${e.message}")
            }
        }
        if (source != null && networkPanel.network.getModels<ConvolutionalNeuralNetwork>().any {
                it.inputTensorLayer == source
            }) {
            reasons.add("A convolutional neural network already exists for this input Tensor")
        }
        return reasons.isEmpty() to reasons.joinToString(", ")
    }

    val createConvolutionalNeuralNetworkAction = networkPanel.createAction(
        name = "Create convolutional neural network",
        description = "Create a convolutional neural network from selected source Tensor to target NeuronArray"
    ) {
        val (canCreate) = canCreateConvolutionalNeuralNetwork()

        if (canCreate) {
            val input = selectionManager.sourceModels.firstOrNull() as? TensorLayer
            val output = selectionManager.selectedModels.firstOrNull() as? NeuronArray

            if (input != null && output != null) {
                addSubnetworkAction(networkPanel) {
                    ConvolutionalNeuralNetwork(input, output)
                }
            }
        }
    }.also {
        fun updateAction() {
            val (canCreate, reason) = canCreateConvolutionalNeuralNetwork()
            it.isEnabled = canCreate
            it.putValue(
                SHORT_DESCRIPTION,
                "Create a convolutional neural network from selected source Tensor to target NeuronArray"
                        + if (!canCreate) " (Disabled: $reason)" else ""
            )
        }

        updateAction()

        networkPanel.network.events.modelAdded.on(Dispatchers.Default) { updateAction() }
        networkPanel.selectionManager.events.selection.on(Dispatchers.Default) { _, _ -> updateAction() }
        networkPanel.selectionManager.events.sourceSelection.on(Dispatchers.Default) { _, _ -> updateAction() }
    }

    fun createRecordActivationAction(source: Layer) = actionManager.createCoupledDataWorldAction(
        name = "Record activations",
        source.getProducer(Layer::activationArray),
        sourceName = "${source.displayName} Activations",
        source.size
    )

    fun createNeuronCollectionCoupledImageWorld(collection: NeuronCollection) = actionManager.createImageInput(
        collection.getConsumer(NeuronCollection::activationArray),
        collection.size,
        menuTitle = "Add coupled image world",
        postActionBlock = { collection.isClamped = true }
    )

}

fun addSubnetworkAction(networkPanel: NetworkPanel, block: () -> Subnetwork) {
    val subnetwork = block()
    networkPanel.network.addNetworkModelAsync(subnetwork)
    val models = subnetwork.modelList.all
    val neuronCollections = models.filterIsInstance<NeuronCollection>()
    val synapseGroups = models.filterIsInstance<SynapseGroup>()

    val neuronCollectionNeuronsMap = neuronCollections.associateWith { it.neuronList.toList() }
    val synapseGroupSynapsesMap = synapseGroups.associateWith { it.synapses.toList() }

    networkPanel.undoManager.addUndoableAction(
        description = "Add subnetwork ${subnetwork.id}",
        undo = { subnetwork.delete() },
        redo = {

            neuronCollectionNeuronsMap.forEach { (collection, neurons) ->
                collection.neuronList.clear()
                collection.neuronList.addAll(neurons)
            }

            synapseGroupSynapsesMap.forEach { (group, synapses) ->
                group.synapses.clear()
                group.synapses.addAll(synapses)
            }

            subnetwork.modelList.addAll(models)
            networkPanel.network.addNetworkModel(subnetwork, usePlacementManager = false, useAutoAssignedId = false)
            subnetwork.afterRestore()
        }
    )
}
