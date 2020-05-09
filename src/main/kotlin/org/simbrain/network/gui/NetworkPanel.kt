package org.simbrain.network.gui

import org.piccolo2d.PCamera
import org.piccolo2d.PCanvas
import org.piccolo2d.event.PMouseWheelZoomEventHandler
import org.piccolo2d.util.PBounds
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.NetworkModel
import org.simbrain.network.connections.QuickConnectionManager
import org.simbrain.network.core.Network
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronUpdateRule
import org.simbrain.network.core.Synapse
import org.simbrain.network.desktop.NetworkDesktopComponent
import org.simbrain.network.dl4j.ArrayConnectable
import org.simbrain.network.dl4j.MultiLayerNet
import org.simbrain.network.dl4j.NeuronArray
import org.simbrain.network.dl4j.WeightMatrix
import org.simbrain.network.groups.NeuronCollection
import org.simbrain.network.groups.NeuronGroup
import org.simbrain.network.groups.Subnetwork
import org.simbrain.network.groups.SynapseGroup
import org.simbrain.network.gui.actions.edit.ToggleAutoZoom
import org.simbrain.network.gui.actions.synapse.AddSynapseGroupAction
import org.simbrain.network.gui.nodes.*
import org.simbrain.network.gui.nodes.neuronGroupNodes.CompetitiveGroupNode
import org.simbrain.network.gui.nodes.neuronGroupNodes.SOMGroupNode
import org.simbrain.network.gui.nodes.subnetworkNodes.*
import org.simbrain.network.subnetworks.*
import org.simbrain.util.complement
import org.simbrain.util.genericframe.GenericJDialog
import org.simbrain.util.widgets.EditablePanel
import org.simbrain.util.widgets.ToggleButton
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JInternalFrame
import javax.swing.JPanel
import javax.swing.ToolTipManager
import javax.swing.event.InternalFrameAdapter
import javax.swing.event.InternalFrameEvent

/**
 * Todo: Should eventually replace NetworkPanel and NetworkPanelDesktop
 */
class NetworkPanel(val component: NetworkDesktopComponent?, val network: Network) : JPanel() {

    // TODO: Think about null component
    //TODO: Change javadocs to single line

    val canvas = PCanvas()

    var editMode: EditMode = EditMode.SELECTION

    /**
     * Manage selection events where the "green handle" is added to nodes and other [NetworkModel]s
     * when the lasso is pulled over them.  Also keeps track of source nodes (but those events are
     * handled by keybindings).
     */
    val selectionManager = NetworkSelectionManager(this).apply {
        events.onSelection { old, new ->
            val (removed, added) = old complement new
            removed.forEach { NodeHandle.removeSelectionHandleFrom(it) }
            added.forEach {
                if (it is InteractionBox) {
                    NodeHandle.addSelectionHandleTo(it, NodeHandle.INTERACTION_BOX_SELECTION_STYLE)
                } else {
                    NodeHandle.addSelectionHandleTo(it)
                }
            }
        }
    }

    val networkActions = NetworkActions(this)

    @Deprecated("Consider removing")
    val neuronNodeMapping: Map<Neuron, NeuronNode> = HashMap()

    val timeLabel = TimeLabel(this).apply { update() }

    var autoZoom = true
        set(value) {
            field = value
            repaint()
        }
    var showTime = true

    val mainToolBar = createMainToolBar()
    val runToolBar = createRunToolBar()
    val editToolBar = createEditToolBar()

    var backgroundColor = Color.white

    val isRunning
            get()  = network.isRunning

    /** How much to nudge objects per key click. */
    var nudgeAmount = 2.0

    /**
     * Text object event handler.
     */
    val textHandle: TextEventHandler = TextEventHandler(this)

    private val toolbars: JPanel = JPanel(BorderLayout())

    /**
     * Manages keyboard-based connections.
     */
     val quickConnector = QuickConnectionManager()

    /**
     * Manages placement of new nodes, groups, etc.
     */
    val placementManager = PlacementManager()

    /**
     * Set to 3 since update neurons, synapses, and groups each decrement it by 1. If 0, update is complete.
     */
    private val updateComplete = AtomicInteger(0)

    /**
     * Whether loose synapses are visible or not.
     */
    var looseWeightsVisible = true
        set(value) {
            field = value
            getScreenElements<SynapseNode>().forEach { it.visible = value }
        }

    /**
     * Whether to display update priorities.
     */
    var prioritiesVisible = false
        set(value) {
            field = value
            getScreenElements<NeuronNode>().forEach { it.setPriorityView(value) }
        }

    /**
     * Turn GUI on or off.
     */
    var guiOn = true
        set(guiOn) {
            if (guiOn) {
                this.setUpdateComplete(false)
                //this.updateSynapseNodes()
                updateComplete.decrementAndGet()
            }
            field = guiOn
        }

    /**
     * Main initialization of the network panel.
     */
    init {
        super.setLayout(BorderLayout())

        canvas.apply {
            // Always render in high quality
            setDefaultRenderQuality(PPaintContext.HIGH_QUALITY_RENDERING)
            animatingRenderQuality = PPaintContext.HIGH_QUALITY_RENDERING
            interactingRenderQuality = PPaintContext.HIGH_QUALITY_RENDERING

            // Remove default event listeners
            removeInputEventListener(panEventHandler)
            removeInputEventListener(zoomEventHandler)

            // Event listeners
            addInputEventListener(MouseEventHandler(this@NetworkPanel))
            addInputEventListener(ContextMenuEventHandler(this@NetworkPanel))
            addInputEventListener(PMouseWheelZoomEventHandler().apply { zoomAboutMouse() })
            addInputEventListener(TextEventHandler(this@NetworkPanel))
            addInputEventListener(TextEventHandler(this@NetworkPanel))

            // Don't show text when the canvas is sufficiently zoomed in
            camera.addPropertyChangeListener(PCamera.PROPERTY_VIEW_TRANSFORM) {
                getScreenElements<NeuronNode>().forEach { it.updateTextVisibility() }
            }
        }

        // Init network change listeners
        // addNetworkListeners()

        toolbars.apply {
            cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)

            val flowLayout = FlowLayout().apply { hgap = 0; vgap = 0 }

            add("Center", JPanel(flowLayout).apply {
                add(mainToolBar)
                add(runToolBar)
                add(editToolBar)
            })
        }

//        add("North", toolbars)
        add("Center", canvas)
//        add("South", JToolBar().apply { add(timeLabel) })

        // Register support for tool tips
        // TODO: might be a memory leak, if not unregistered when the parent frame is removed
        // TODO: copy from old code. Re-verify.
        ToolTipManager.sharedInstance().registerComponent(this)

        KeyBindings.addBindings(this)

        // Repaint whenever window is opened or changed.
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(arg0: ComponentEvent) {
                repaint()
            }
        })

    }

    @Deprecated("Use selectionManager instead.", ReplaceWith("selectionManager.selection"))
    val selectedNodes
        get() = selectionManager.selection

    @Deprecated("User selectionManager instead", ReplaceWith("selectionManager.selectionOf(clazz)"))
    fun <T : ScreenElement> getSelectedNodes(clazz: Class<T>) =
            selectionManager.selectionOf(clazz)

    @Deprecated("Use selectionManager instead", ReplaceWith("selectionManager.selectedModels"))
    val selectedModels
        get() = selectionManager.selection.map { it.model!! }

    @Deprecated("Use selectionManager instead", ReplaceWith("selectionManager.selectedModelsOf(clazz)"))
    fun <T : NetworkModel> getSelectedModels(clazz: Class<T>) =
            selectionManager.selectedModels.filterIsInstance(clazz)

    /** TODO: Javadoc. */
    fun setUpdateComplete(updateComplete: Boolean) {
        if (!updateComplete && this.updateComplete.get() != 0) {
            return
        }
        this.updateComplete.set(if (updateComplete) 0 else 3)
    }

    /**
     * Rescales the camera so that all objects in the canvas can be seen. Compare "zoom to fit page" in draw programs.
     *
     * @param forceZoom if true force the zoom to happen
     */
    fun zoomToFitPage(forceZoom: Boolean) {
        // TODO: Add a check to see if network is running
        if (autoZoom && editMode.isSelection || forceZoom) {
            val filtered = canvas.layer.getUnionOfChildrenBounds(null)
            val adjustedFiltered = PBounds(filtered.getX() - 10, filtered.getY() - 10,
                    filtered.getWidth() + 20, filtered.getHeight() + 20)
            canvas.camera.setViewBounds(adjustedFiltered)
        }
    }

    inline fun <reified T : ScreenElement> getScreenElements() = canvas.layer.allNodes.filterIsInstance<T>()

    fun <T : ScreenElement> getScreenElements(clazz: Class<T>) =
            canvas.layer.allNodes.filterIsInstance(clazz)

    private inline fun <T : ScreenElement> addScreenElement(block: () -> T) = block().also {
        canvas.layer.addChild(it)
        repaint()
    }

    @Deprecated("Consider removing / add from Network instead")
    fun addNeuron(updateRule: NeuronUpdateRule) {
        val neuron = Neuron(network, updateRule)
        placementManager.addNewModelObject(neuron)
        neuron.forceSetActivation(0.0)
        network.addLooseNeuron(neuron)
    }

    fun add(neuron: Neuron) = addScreenElement {
        NeuronNode(this, neuron).also {
            (neuronNodeMapping as HashMap)[neuron] = it
            selectionManager.set(it)
        }
    }

    fun add(synapse: Synapse) = addScreenElement {
        val source = neuronNodeMapping[synapse.source] ?: throw IllegalStateException("Neuron node does not exist")
        val target = neuronNodeMapping[synapse.target] ?: throw IllegalStateException("Neuron node does not exist")
        SynapseNode(this, source, target, synapse)
    }.also { it.lowerToBottom() }

    fun add(neuronGroup: NeuronGroup) = addScreenElement {

        fun createNeuronGroupNode() = when (neuronGroup) {
            is SOMGroup -> SOMGroupNode(this, neuronGroup)
            is CompetitiveGroup -> CompetitiveGroupNode(this, neuronGroup)
            else -> NeuronGroupNode(this, neuronGroup)
        }

        neuronGroup.applyLayout()

        val neuronNodes = neuronGroup.neuronList.map { neuron -> add(neuron) }

        createNeuronGroupNode().apply { addNeuronNodes(neuronNodes) }
    }

    fun add(neuronArray: NeuronArray) = addScreenElement { NeuronArrayNode(this, neuronArray) }

    fun add(multiLayerNet: MultiLayerNet) = addScreenElement {
        MultiLayerNetworkNode(this, multiLayerNet)
    }

    fun add(neuronCollection: NeuronCollection) = addScreenElement {
        val neuronNodes = neuronCollection.neuronList.map {
            neuronNodeMapping[it] ?: throw IllegalStateException("Neuron node does not exist")
        }
        NeuronCollectionNode(this, neuronCollection).apply { addNeuronNodes(neuronNodes) }
    }

    fun add(synapseGroup: SynapseGroup) = addScreenElement {
        SynapseGroupNode(this, synapseGroup)
    }.also { it.lowerToBottom() }

    fun add(subnetwork: Subnetwork) = addScreenElement {
        fun createSubNetwork() = when (subnetwork) {
            is Hopfield -> HopfieldNode(this, subnetwork)
            is CompetitiveNetwork -> CompetitiveNetworkNode(this, subnetwork)
            is SOMNetwork -> SOMNetworkNode(this, subnetwork)
            is EchoStateNetwork -> ESNNetworkNode(this, subnetwork)
            is SimpleRecurrentNetwork -> SRNNetworkNode(this, subnetwork)
            is BackpropNetwork -> BackpropNetworkNode(this, subnetwork)
            is LMSNetwork -> LMSNetworkNode(this, subnetwork)
            is BPTTNetwork -> BPTTNode(this, subnetwork)
            else -> SubnetworkNode(this, subnetwork)
        }

        val neuronGroupNodes = subnetwork.neuronGroupList.map { group -> add(group) }
        val synapseGroupNodes = subnetwork.synapseGroupList.map { group -> add(group) }

        createSubNetwork().apply {
            neuronGroupNodes.forEach { addNode(it) }
            synapseGroupNodes.forEach { addNode(it) }
        }

    }

    // TODO: refactor network remove model
    // better to have a series of remove methods, similar to the add methods
    fun deleteSelectedObjects() {

        fun deleteGroup(interactionBox: InteractionBox) {
            with(network) {
                interactionBox.parent.let { groupNode ->
                    when (groupNode) {
                        is NeuronGroupNode -> removeNeuronGroup(groupNode.neuronGroup)
                        is NeuronCollectionNode -> removeNeuronCollection(groupNode.neuronCollection)
                        is SynapseGroupNode -> removeSynapseGroup(groupNode.synapseGroup)
                        is SubnetworkNode -> removeSubnetwork(groupNode.subnetwork)
                    }
                }
            }
        }

        fun delete(screenElement: ScreenElement) {
            with(network) {
                when (screenElement) {
                    is NeuronNode -> removeNeuron(screenElement.neuron, true)
                    is SynapseNode -> removeSynapse(screenElement.synapse)
                    is NeuronArrayNode -> removeNeuronArray(screenElement.neuronArray)
                    is WeightMatrixNode -> removeWeightMatrix(screenElement.model)
                    is TextNode -> deleteText(screenElement.textObject)
                    is InteractionBox -> deleteGroup(screenElement)
                }
            }
        }

        selectedNodes.forEach { delete(it) }

        // Zoom events are costly so only zoom after main deletion events
        zoomToFitPage(true)
    }

    private fun createEditToolBar() = CustomToolBar().apply {
        with(networkActions) {
            networkEditingActions.forEach { add(it) }
            add(clearNodeActivationsAction)
            add(randomizeObjectsAction)
        }
    }

    fun copy() {
        if (selectionManager.isEmpty) return

        Clipboard.clear()
        Clipboard.add(selectionManager.selectedModels)
        placementManager.setNewCopy()
    }

    fun cut() {
        copy()
        deleteSelectedObjects()
    }

    fun paste() {
        Clipboard.paste(this)
    }

    fun duplicate() {
        if (selectionManager.isEmpty) return

        copy()
        paste()
    }

    fun alignHorizontal() {
        val neurons = selectionManager.selectedModelsOf<Neuron>()
        val minY = neurons.map { it.y }.min() ?: Double.MAX_VALUE
        neurons.forEach { it.y = minY }
        repaint()
    }

    fun alignVertical() {
        val neurons = selectionManager.selectedModelsOf<Neuron>()
        val minX = neurons.map { it.x }.min() ?: Double.MAX_VALUE
        neurons.forEach { it.x = minX }
        repaint()
    }

    fun spaceHorizontal() {
        val neurons = selectionManager.selectedModelsOf<Neuron>()
        if (neurons.size > 1) {
            val sortedNeurons = neurons.sortedBy { it.x }
            val min = neurons.first().x
            val max = neurons.last().x
            val spacing = (max - min) / neurons.size - 1

            sortedNeurons.forEachIndexed { i, neuron -> neuron.x = min + spacing * i }
        }
        repaint()
    }

    fun spaceVertical() {
        val neurons = selectionManager.selectedModelsOf<Neuron>()
        if (neurons.size > 1) {
            val sortedNeurons = neurons.sortedBy { it.y }
            val min = neurons.first().y
            val max = neurons.last().y
            val spacing = (max - min) / neurons.size - 1

            sortedNeurons.forEachIndexed { i, neuron -> neuron.y = min + spacing * i }
        }
        repaint()
    }

    fun nudge(dx: Int, dy: Int) {
        selectionManager.selectedModelsOf<Neuron>()
                .forEach { it.offset(dx * nudgeAmount, dy * nudgeAmount) }
    }

    fun toggleClamping() {
        selectionManager.selectedModelsOf<Neuron>().forEach { it.isClamped = !it.isClamped }
        selectionManager.selectionOf<SynapseNode>().forEach {
            with(it.synapse) { isFrozen = !isFrozen }

            // TODO: this should happen via an event
            //   but firing events from setFrozen causes problems
            //   when opening saved networks
            it.updateClampStatus()
        }
        revalidate()
    }

    fun incrementSelectedObjects() {
        with(selectionManager) {
            selectedModelsOf<Neuron>().forEach { it.updateRule.incrementActivation(it) }
            selectionOf<SynapseNode>().forEach {
                it.synapse.incrementWeight()
                it.updateColor()
                it.updateDiameter()
            }
            selectedModelsOf<NeuronArray>().forEach { it.increment() }
            selectedModelsOf<WeightMatrix>().forEach { it.increment() }
        }
    }

    fun decrementSelectedObjects() {
        with(selectionManager) {
            selectedModelsOf<Neuron>().forEach { it.updateRule.decrementActivation(it) }
            selectionOf<SynapseNode>().forEach {
                it.synapse.decrementWeight()
                it.updateColor()
                it.updateDiameter()
            }
            selectedModelsOf<NeuronArray>().forEach { it.decrement() }
            selectedModelsOf<WeightMatrix>().forEach { it.decrement() }
        }
    }

    fun contextualIncrementSelectedObjects() {
        selectionManager.selectedModelsOf<Neuron>().forEach { it.updateRule.contextualIncrement(it) }
    }

    fun contextualDecrementSelectedObjects() {
        selectionManager.selectedModelsOf<Neuron>().forEach { it.updateRule.contextualDecrement(it) }
    }

    fun clearSelectedObjects() {
        with(selectionManager) {
            selectedModelsOf<Neuron>().forEach { it.clear() }
            selectedModelsOf<Synapse>().forEach { it.forceSetStrength(0.0) }
            selectedModelsOf<NeuronArray>().forEach { it.clear() }
            selectedModelsOf<NeuronGroup>().forEach { it.clearActivations() }
        }
    }

    fun selectNeuronsInNeuronGroups() {
        selectionManager.selectionOf<NeuronGroupNode>().forEach { it.selectNeurons() }
    }


    fun clearNeurons() {
        getScreenElements<NeuronNode>().forEach { it.neuron.clear() }
    }

    /**
     * Connect source and target model items.
     * <br></br>
     * If a either source or target model items are ND4J, connect with a weight matrix
     * <br></br>
     * If a pair of source and target items are neuron groups, connect with a synapse group
     * <br></br>
     * If either member of a pair is a neuron collection or a set of loose neurons, then connect using neurons on both
     * sides, using quick connect (e.g. if connecting neuron collection to neuron group, connect to the neurons "inside"
     * of neuron group).
     */
    fun connectSelectedModels() {

        // Handle adding synapse groups between neuron groups
        if (AddSynapseGroupAction.displaySynapseGroupDialog(this)) {
            // TODO: Document, think about the boolean return on that.
            return
        }

        with(selectionManager) {
            val sourceNeurons = sourceModelsOf<Neuron>() +
                    sourceModelsOf<NeuronCollection>().flatMap { it.neuronList } +
                    sourceModelsOf<NeuronGroup>().flatMap { it.neuronList }
            val targetNeurons = selectedModelsOf<Neuron>() +
                    selectedModelsOf<NeuronCollection>().flatMap { it.neuronList } +
                    selectedModelsOf<NeuronGroup>().flatMap { it.neuronList }

            quickConnector.applyCurrentConnection(network, sourceNeurons, targetNeurons)
        }
    }

    /**
     * Connect all selected [ArrayConnectable]s with [WeightMatrix] objects.
     */
    fun connectWithWeightMatrix() {
        with(selectionManager) {
            val sources = sourceModelsOf<ArrayConnectable>()
            val targets = selectedModelsOf<ArrayConnectable>()

            for (source in sources) {
                for (target in targets) {
                    network.createWeightMatrix(source, target)
                }
            }
        }
    }

    // TODO: Move to NetworkDialogs.kt
    @Deprecated("Consider removing or refactor out of NetworkPanel")
    fun displayPanel(panel: JPanel, title: String) = GenericJDialog().apply {
        if (this is JInternalFrame) {
            addInternalFrameListener(object : InternalFrameAdapter() {
                override fun internalFrameClosed(e: InternalFrameEvent) {
                    if (panel is EditablePanel) {
                        panel.commitChanges()
                    }
                }
            })
        }
        this.title = title
        contentPane = panel
        pack()
        isVisible = true
    }

    // TODO: Move to NetworkDialogs.kt
    @Deprecated("Consider removing or refactor out of NetworkPanel")
    fun displayPanelInWindow(panel: JPanel, title: String) = GenericJDialog().apply {
        this.title = title
        contentPane = panel
        pack()
        isVisible = true
    }

    private fun createRunToolBar() = CustomToolBar().apply {
        with(networkActions) {
            add(iterateNetworkAction)
            add(ToggleButton(networkControlActions))
        }
    }

    private fun createMainToolBar() = CustomToolBar().apply {
        with(networkActions) {
            networkModeActions.forEach { add(it) }
            addSeparator()
            add(ToggleAutoZoom(this@NetworkPanel))
        }
    }

}