package org.simbrain.network.gui


import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.PCamera
import org.piccolo2d.PCanvas
import org.piccolo2d.event.PMouseWheelZoomEventHandler
import org.piccolo2d.util.PBounds
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.*
import org.simbrain.network.connections.AllToAll
import org.simbrain.network.core.*
import org.simbrain.network.groups.*
import org.simbrain.network.gui.UndoManager.UndoableAction
import org.simbrain.network.gui.actions.edit.ToggleAutoZoom
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.dialogs.group.ConnectorDialog
import org.simbrain.network.gui.nodes.*
import org.simbrain.network.gui.nodes.neuronGroupNodes.CompetitiveGroupNode
import org.simbrain.network.gui.nodes.neuronGroupNodes.SOMGroupNode
import org.simbrain.network.gui.nodes.subnetworkNodes.*
import org.simbrain.network.kotlindl.DeepNet
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.matrix.ZoeLayer
import org.simbrain.network.smile.SmileClassifier
import org.simbrain.network.subnetworks.*
import org.simbrain.network.trainers.trainCurrentOutputLMS
import org.simbrain.util.complement
import org.simbrain.util.genericframe.GenericJDialog
import org.simbrain.util.piccolo.unionOfGlobalFullBounds
import org.simbrain.util.widgets.EditablePanel
import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.prefs.PreferenceChangeListener
import javax.swing.*
import javax.swing.event.InternalFrameAdapter
import javax.swing.event.InternalFrameEvent

/**
 * Main GUI representation of a [Network].
 */
class NetworkPanel constructor(val networkComponent: NetworkComponent) : JPanel(), CoroutineScope {

    /**
     * Main Piccolo canvas object.
     *
     * @see https://github.com/piccolo2d/piccolo2d.java
     */
    val canvas = PCanvas()

    /**
     * Reference to the model network
     */
    val network: Network = networkComponent.network

    override val coroutineContext get() = network.coroutineContext

    /**
     * Manage selection events where the "green handle" is added to nodes and other [NetworkModel]s
     * when the lasso is pulled over them.  Also keeps track of source nodes (but those events are
     * handled by keybindings).
     */
    val selectionManager = NetworkSelectionManager(this).apply {
        setUpSelectionEvents()
    }

    /**
     * Holder for all actions, which are unique and can be accessed from multiple places.
     */
    val networkActions = NetworkActions(this)

    /**
     * Associates network models with screen elements
     */
    private val modelNodeMap = HashMap<NetworkModel, ScreenElement>()

    val timeLabel = TimeLabel(this).apply { update() }

    var autoZoom = true
        set(value) {
            field = value
            network.events.zoomToFitPage.fireAndForget()
        }

    var editMode: EditMode = EditMode.SELECTION
        set(newEditMode) {
            val oldEditMode = editMode
            field = newEditMode
            if (editMode == EditMode.WAND) {
                editMode.resetWandCursor()
            }
            firePropertyChange("editMode", oldEditMode, editMode)
            cursor = editMode.cursor
        }

    var showTime = true

    private val toolbars: JPanel = JPanel(BorderLayout())

    val mainToolBar = createMainToolBar()

    val editToolBar = createEditToolBar()

    /**
     * How much to nudge objects per key click.
     */
    var nudgeAmount = NetworkPreferences.nudgeAmount

    /**
     * Undo Manager
     */
    val undoManager = UndoManager()

    /**
     * Whether to display update priorities.
     */
    var prioritiesVisible = false
        set(value) {
            field = value
            filterScreenElements<NeuronNode>().forEach { it.setPriorityView(value) }
        }

    /**
     * Whether to display free weights (those not in a synapse group) or not.
     */
    var freeWeightsVisible = true
        set(value) {
            field = value
            network.freeSynapses.forEach { it.isVisible = value }
            network.events.freeWeightVisibilityChanged.fireAndForget(value)
        }

    /**
     * Turn GUI on or off.
     */
    var guiOn = true

    private val forceZoomToFitPage = PreferenceChangeListener { network.events.zoomToFitPage.fireAndBlock() }

    val preferenceLoader = {
        canvas.background = NetworkPreferences.backgroundColor
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

            NetworkPreferences.registerChangeListener(preferenceLoader)
            preferenceLoader()

            // Remove default event listeners
            removeInputEventListener(panEventHandler)
            removeInputEventListener(zoomEventHandler)

            // Event listeners
            addInputEventListener(MouseEventHandler(this@NetworkPanel))
            addInputEventListener(ContextMenuEventHandler(this@NetworkPanel))
            addInputEventListener(PMouseWheelZoomEventHandler().apply { zoomAboutMouse() })
            addInputEventListener(WandEventHandler(this@NetworkPanel));

            // Don't show text when the canvas is sufficiently zoomed in
            camera.addPropertyChangeListener(PCamera.PROPERTY_VIEW_TRANSFORM) {
                launch(Dispatchers.Main) {
                    filterScreenElements<NeuronNode>().forEach {
                        it.updateTextVisibility()
                    }
                }
            }
        }

        initEventHandlers()

        toolbars.apply {

            cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
            val flowLayout = FlowLayout(FlowLayout.LEFT).apply { hgap = 0; vgap = 0 }
            add("Center", JPanel(flowLayout).apply {
                add(mainToolBar)
                add(editToolBar)
            })
        }

        add("North", toolbars)
        add("Center", canvas)
        add("South", JToolBar().apply { add(timeLabel) })

        // Register support for tool tips
        // TODO: might be a memory leak, if not unregistered when the parent frame is removed
        // TODO: copy from old code. Re-verify.
        ToolTipManager.sharedInstance().registerComponent(this)

        addKeyBindings()

        // Repaint whenever window is opened or changed.
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(arg0: ComponentEvent) {
                network.events.zoomToFitPage.fireAndForget()
            }
        })

        // Add all network elements (important for de-serializing)
        network.modelsInReconstructionOrder.forEach { createNode(it) }

    }

    /**
     * Returns all nodes in the canvas.
     */
    val screenElements get() = canvas.layer.allNodes.filterIsInstance<ScreenElement>()

    /**
     * Filter [ScreenElement]s using a generic type.
     */
    inline fun <reified T : ScreenElement> filterScreenElements() = canvas.layer.allNodes.filterIsInstance<T>()
    fun <T : ScreenElement> filterScreenElements(clazz: Class<T>) =
        canvas.layer.allNodes.filterIsInstance(clazz)

    /**
     * Add a screen element to the network panel and rezoom the page.
     */
    private inline fun <T : ScreenElement> addScreenElement(block: () -> T) = block().also { node ->
        canvas.layer.addChild(node)
        node.model.events.selected.on {
            if (node is NeuronGroupNode) {
                selectionManager.add(node.interactionBox)
            } else {
                selectionManager.add(node)
            }
        }
        network.events.zoomToFitPage.fireAndForget()
    }

    private fun createNode(model: NetworkModel): ScreenElement {
        return when (model) {
            is Neuron -> createNode(model)
            is Synapse -> createNode(model)
            is SmileClassifier -> createNode(model)
            is NeuronArray -> createNode(model)
            is NeuronCollection -> createNode(model)
            is NeuronGroup -> createNode(model)
            is SynapseGroup2 -> createNode(model)
            is Connector -> createNode(model)
            is Subnetwork -> createNode(model)
            is NetworkTextObject -> createNode(model)
            is ZoeLayer -> createNode(model)
            is DeepNet -> createNode(model)
            else -> throw IllegalArgumentException()
        }.also { modelNodeMap[model] = it }
    }

    fun createNode(neuron: Neuron) = addScreenElement {
        undoManager.addUndoableAction(object : UndoableAction {
            override fun undo() {
                neuron.delete()
            }

            override fun redo() {
                network.addNetworkModelAsync(neuron)
            }
        })
        NeuronNode(this, neuron)
    }

    fun createNode(synapse: Synapse) = addScreenElement {
        val source = modelNodeMap[synapse.source] as? NeuronNode ?: throw IllegalStateException("Neuron node does not exist")
        val target = modelNodeMap[synapse.target] as? NeuronNode ?: throw IllegalStateException("Neuron node does not exist")
        SynapseNode(this, source, target, synapse)
    }.also { it.lowerToBottom() }

    fun createNode(neuronGroup: NeuronGroup) = addScreenElement {

        fun createNeuronGroupNode() = when (neuronGroup) {
            is SOMGroup -> SOMGroupNode(this, neuronGroup)
            is CompetitiveGroup -> CompetitiveGroupNode(this, neuronGroup)
            else -> NeuronGroupNode(this, neuronGroup)
        }

        val neuronNodes = neuronGroup.neuronList.map { neuron -> createNode(neuron).also { modelNodeMap[neuron] = it } }
        // neuronGroup.applyLayout()
        createNeuronGroupNode().apply { addNeuronNodes(neuronNodes) }
    }

    fun createNode(neuronArray: NeuronArray) = addScreenElement { NeuronArrayNode(this, neuronArray) }

    fun createNode(classifier: SmileClassifier) = addScreenElement {
        SmileClassifierNode(this, classifier)
    }

    fun createNode(layer: ZoeLayer) = addScreenElement {
        ZoeLayerNode(this, layer)
    }

    fun createNode(dn: DeepNet) = addScreenElement {
        DeepNetNode(this, dn)
    }

    fun createNode(neuronCollection: NeuronCollection) = addScreenElement {
        val neuronNodes = neuronCollection.neuronList.map {
            modelNodeMap[it] as? NeuronNode ?: throw IllegalStateException("Neuron node does not exist")
        }
        NeuronCollectionNode(this, neuronCollection).apply { addNeuronNodes(neuronNodes) }
    }

    fun createNode(synapseGroup: SynapseGroup2) = addScreenElement {
        with(synapseGroup.synapses) {
            if (size < network.synapseGroupExpendedVisibilityThreshold) {
                forEach {
                    createNode(it)
                }
            }
        }
        SynapseGroup2Node(this, synapseGroup)
    }

    fun createNode(weightMatrix: Connector) = addScreenElement {
        WeightMatrixNode(this, weightMatrix).also { it.lower() }
    }

    fun createNode(text: NetworkTextObject) = addScreenElement {
        TextNode(this, text)
    }

    fun createNode(subnetwork: Subnetwork) = addScreenElement {

        fun createSubNetwork() = when (subnetwork) {
            is Hopfield -> HopfieldNode(this, subnetwork)
            is CompetitiveNetwork -> CompetitiveNetworkNode(this, subnetwork)
            is SOMNetwork -> SOMNetworkNode(this, subnetwork)
            // is EchoStateNetwork -> ESNNetworkNode(this, subnetwork)
            is SRNNetwork -> SRNNode(this, subnetwork)
            is BackpropNetwork -> BackpropNetworkNode(this, subnetwork)
            is LMSNetwork -> LMSNetworkNode(this, subnetwork)
            else -> SubnetworkNode(this, subnetwork)
        }

        val subnetworkNodes = subnetwork.modelList.allInReconstructionOrder.map {
            createNode(it)
        }
        createSubNetwork().apply {
            // Add "sub-nodes" to subnetwork node
            subnetworkNodes.forEach { addNode(it) }
        }

    }

    fun deleteSelectedObjects() {

        fun deleteGroup(interactionBox: InteractionBox) {
            interactionBox.parent.let { groupNode ->
                if (groupNode is ScreenElement) {
                    groupNode.model.delete()
                }
            }
        }

        fun delete(screenElement: ScreenElement) {
            when (screenElement) {
                is NeuronNode -> {
                    screenElement.model.delete()

                    undoManager.addUndoableAction(object : UndoableAction {
                        override fun undo() {
                            network.addNetworkModelAsync(screenElement.model)
                        }

                        override fun redo() {
                            screenElement.model.delete()
                        }
                    })
                }
                is InteractionBox -> deleteGroup(screenElement)
                else -> screenElement.model.delete()
            }
        }

        selectionManager.selection.forEach { delete(it) }

        network.events.zoomToFitPage.fireAndForget()
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
        network.placementManager.anchorPoint =
            selectionManager.filterSelectedModels<LocatableModel>().topLeftLocation
        Clipboard.clear()
        Clipboard.add(selectionManager.selectedModels)
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
        val neurons = selectionManager.filterSelectedModels<Neuron>()
        val minY = neurons.map { it.y }.minOrNull() ?: Double.MAX_VALUE
        neurons.forEach { it.y = minY }
        repaint()
    }

    fun alignVertical() {
        val neurons = selectionManager.filterSelectedModels<Neuron>()
        val minX = neurons.map { it.x }.minOrNull() ?: Double.MAX_VALUE
        neurons.forEach { it.x = minX }
        repaint()
    }

    fun spaceHorizontal() {
        val neurons = selectionManager.filterSelectedModels<Neuron>()
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
        val neurons = selectionManager.filterSelectedModels<Neuron>()
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
        selectionManager.filterSelectedModels<LocatableModel>()
            .translate(dx * nudgeAmount, dy * nudgeAmount)
    }

    fun toggleClamping() {
        selectionManager.filterSelectedModels<NetworkModel>().forEach { it.toggleClamping() }
    }

    fun incrementSelectedObjects() {
        selectionManager.filterSelectedModels<NetworkModel>().forEach { it.increment() }
    }

    fun decrementSelectedObjects() {
        selectionManager.filterSelectedModels<NetworkModel>().forEach { it.decrement() }
    }

    fun clearSelectedObjects() {
        selectionManager.filterSelectedModels<NetworkModel>().forEach { it.clear() }
    }

    fun hardClearSelectedObjects() {
        clearSelectedObjects();
        selectionManager.filterSelectedModels<Synapse>().forEach { it.hardClear() }
        selectionManager.filterSelectedModels<WeightMatrix>().forEach { it.hardClear() }
        selectionManager.filterSelectedModels<SynapseGroup>().forEach { it.hardClear() }
    }

    fun selectNeuronsInNeuronGroups() {
        selectionManager.filterSelectedNodes<NeuronGroupNode>().forEach { it.selectNeurons() }
    }

    /**
     * Connect source and target model items using a default action.
     *
     * For free weights, use "all to all"
     *
     * For neuron groups or arrays, use a weight matrix.
     */
    fun connectSelectedModelsDefault() {

        with(selectionManager) {

            // Re-enable when new synapse groups are done
            // if (connectNeuronGroups()) {
            //     return
            // }

            if (connectLayers()) {
                return
            }

            connectFreeWeights()
        }
    }

    /**
     * Connect source and target model items using a more custom action.
     *
     * For free weights, use the current connection manager
     *
     * For neuron groups use a synapse group
     *
     * For neuron arrays, open a dialog allowing selection (later when we have choices)
     */
    fun connectSelectedModelsCustom() {

        // For neuron groups
        selectionManager.connectNeuronGroups()

        // TODO: Neuron Array case

        // Apply network connection manager to free weights
        applyConnectionStrategy()
    }

    /**
     * Connect free neurons using a potentially customized [ConnectionStrategy]
     */
    fun applyConnectionStrategy() {
        with(selectionManager) {
            val sourceNeurons = filterSelectedSourceModels<Neuron>()
            val targetNeurons = filterSelectedModels<Neuron>()
            network.neuronConnector.cs.connectNeurons(network, sourceNeurons, targetNeurons)
        }
    }

    /**
     * Connect free weights using all to all. An important default case.
     */
    fun connectFreeWeights() {
        // TODO: For large numbers of connections maybe pop up a warning and depending on button pressed make the
        // weights automatically be invisible
        with(selectionManager) {
            val sourceNeurons = filterSelectedSourceModels<Neuron>() +
                    filterSelectedSourceModels<NeuronCollection>().flatMap { it.neuronList } +
                    filterSelectedSourceModels<NeuronGroup>().flatMap { it.neuronList }
            val targetNeurons = filterSelectedModels<Neuron>() +
                    filterSelectedModels<NeuronCollection>().flatMap { it.neuronList } +
                    filterSelectedModels<NeuronGroup>().flatMap { it.neuronList }
            AllToAll().connectNeurons(network, sourceNeurons, targetNeurons)
        }

    }

    /**
     * Connect [Layer] objects.
     *
     * @retrun false if the source and target selections did not have a [Layer]
     */
    private fun NetworkSelectionManager.connectLayers(useDialog: Boolean = false): Boolean {
        val sources = filterSelectedSourceModels(Layer::class.java)
        val targets = filterSelectedModels(Layer::class.java)
        if (sources.isNotEmpty() && targets.isNotEmpty()) {
            if (useDialog) {
                val dialog = ConnectorDialog(this.networkPanel, sources, targets)
                dialog.setLocationRelativeTo(null)
                dialog.pack()
                dialog.isVisible = true
            } else {
                // TODO: Ability to set defaults for weight matrix that is added
                sources.zip(targets) { s, t ->
                    network.addNetworkModelAsync(WeightMatrix(network, s, t))
                }
            }
            return true
        }
        return false
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
     * Connect first selected neuron groups with a synapse group, if any are selected.
     *
     * @retrun false if there source and target neurons did not have a neuron group.
     */
    fun NetworkSelectionManager.connectNeuronGroups(): Boolean {
        val src = filterSelectedSourceModels(AbstractNeuronCollection::class.java)
        val tar = filterSelectedModels(AbstractNeuronCollection::class.java)
        if (src.isNotEmpty() && tar.isNotEmpty()) {
            network.addNetworkModelAsync(SynapseGroup2(src.first(), tar.first()))
            return true;
        }
        return false
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

    private fun createMainToolBar() = CustomToolBar().apply {
        with(networkActions) {
            networkModeActions.forEach { add(it) }
            addSeparator()
            add(ToggleAutoZoom(this@NetworkPanel))
        }
    }

    private fun initEventHandlers() {
        network.events.apply {
            modelAdded.on(Dispatchers.Swing) { list ->
                list.forEach { createNode(it) }
            }
            modelRemoved.on(Dispatchers.Swing) {
                zoomToFitPage.fireAndForget()
                modelNodeMap.remove(it)
            }
            updateActionsChanged.on(Dispatchers.Swing) { timeLabel.update() }
            updated.on(Dispatchers.Swing, wait = true) {
                repaint()
                timeLabel.update()
            }
            zoomToFitPage.on(Dispatchers.Swing) {
                if (autoZoom) {
                    val filtered = screenElements.unionOfGlobalFullBounds()
                    val adjustedFiltered = PBounds(
                        filtered.getX() - 10, filtered.getY() - 10,
                        filtered.getWidth() + 20, filtered.getHeight() + 20
                    )
                    canvas.camera.setViewBounds(adjustedFiltered)
                }
            }
            selected.on { list ->
                selectionManager.set(list.mapNotNull { modelNodeMap[it] })
            }
        }

    }

    private fun NetworkSelectionManager.setUpSelectionEvents() {
        events.apply {
            selection.on(Dispatchers.Swing) { old, new ->
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
            sourceSelection.on(Dispatchers.Swing) { old, new ->
                val (removed, added) = old complement new
                removed.forEach { NodeHandle.removeSourceHandleFrom(it) }
                added.forEach {
                    if (it is InteractionBox) {
                        NodeHandle.addSourceHandleTo(it, NodeHandle.INTERACTION_BOX_SOURCE_STYLE)
                    } else {
                        NodeHandle.addSourceHandleTo(it)
                    }
                }
            }
        }
    }

    // fun showLMS() {
    //     val sources = selectionManager.filterSelectedSourceModels<Neuron>()
    //     val targets = selectionManager.filterSelectedModels<Neuron>()
    //     val sourceActivations = arrayOf(sources.activations.toDoubleArray())
    //     val targetActivations = arrayOf(targets.activations.toDoubleArray())
    //     val ts = TrainingSet(sourceActivations, targetActivations)
    //     val lms = LMSIterative(sources, targets, ts)
    //     showLMSDialog(lms)
    // }

    /**
     * TODO: Work in progress.
     */
    fun undo() {
        println("Initial testing on undo...")
        undoManager.undo()
    }

    /**
     * TODO: Work in progress.
     */
    fun redo() {
        println("Initial testing on redo...")
        undoManager.redo()
    }


    /**
     * Apply "hot key" based one-shot learning, which uses current activations (which may have been hand-set) to
     * train (for now) selected weight matrices.
     */
    fun applyImmediateLearning() {
        filterScreenElements<WeightMatrixNode>().forEach {
            it.model.let { wm ->
                if (wm is WeightMatrix) {
                    wm.trainCurrentOutputLMS()
                }
            }
        }
    }

    fun getNode(model: NetworkModel) = modelNodeMap[model]

}