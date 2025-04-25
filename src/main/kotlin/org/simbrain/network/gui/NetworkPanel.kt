package org.simbrain.network.gui

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.PCanvas
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.util.PBounds
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.NetworkComponent
import org.simbrain.network.connections.AllToAll
import org.simbrain.network.connections.FixedDegree
import org.simbrain.network.connections.RadialProbabilistic
import org.simbrain.network.connections.Sparse
import org.simbrain.network.core.*
import org.simbrain.network.gui.MouseEventHandler.MouseCursor
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.nodes.*
import org.simbrain.network.gui.nodes.neuronGroupNodes.SOMGroupNode
import org.simbrain.network.gui.nodes.subnetworkNodes.*
import org.simbrain.network.layouts.Layout
import org.simbrain.network.neurongroups.NeuronGroup
import org.simbrain.network.neurongroups.SOMGroup
import org.simbrain.network.smile.SmileClassifier
import org.simbrain.network.subnetworks.*
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.util.*
import org.simbrain.util.piccolo.setViewBoundsNoOverflow
import org.simbrain.util.piccolo.unionOfGlobalFullBounds
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseWheelEvent
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.util.prefs.PreferenceChangeListener
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.JToggleButton
import kotlin.math.pow
import kotlin.reflect.KClass

/**
 * Main GUI representation of a [Network].
 */
class NetworkPanel(val networkComponent: NetworkComponent) : JPanel(), CoroutineScope {

    /**
     * Main Piccolo canvas object.
     *
     * @see https://github.com/piccolo2d/piccolo2d.java
     */
    val canvas = NetworkCanvas()

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
    val modelNodeMap = CompletableDeferredHashMap<NetworkModel, ScreenElement>()

    val timeLabel = TimeLabel(this).apply { update() }

    var autoZoom = true
        set(value) {
            field = value
            network.events.zoomModeChanged.fire(value)
            if (value) {
                network.events.zoomToFitPage.fire()
            }
        }

    /**
     * The current zoom level of the canvas.
     *
     * For example:
     * 0.5 means the canvas is rendered (zoomed out) at 0.5 of its normal size, and 2 means it is rendered (zoomed in) to twice its size
     *
     * The setter rescales the canvas.
     */
    var scalingFactor: Double
        get() = canvas.camera.viewScale
        set(scalingFactor) {
            val currentScalingFactor = canvas.camera.viewScale
            val scalingFactorRatio = scalingFactor / currentScalingFactor
            canvas.scale(scalingFactorRatio)
            repaint()
        }

    var mouseCursor: MouseCursor = MouseCursor.Selection
        set(newCursor) {
            val oldCursor = field
            field = newCursor
            firePropertyChange("editMode", oldCursor, newCursor)
            cursor = newCursor.cursor
        }

    var showTime = true

    private val toolbars: JPanel = JPanel(BorderLayout())

    val mainToolBar = createMainToolBar()

    val editToolBar = createEditToolBar()

    /**
     * How much to nudge objects per key click.
     */
    var nudgeAmount = NetworkPreferences.nudgeAmount

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
            network.events.freeWeightVisibilityChanged.fire(value)
        }

    /**
     * Turn GUI on or off.
     */
    var guiOn = true

    private val forceZoomToFitPage = PreferenceChangeListener { network.events.zoomToFitPage.fire() }

    /**
     * Called when preferences are updated. Ensures preference changes are applied immediately.
     */
    val preferenceLoader = {

        canvas.background = NetworkPreferences.backgroundColor
        nudgeAmount = NetworkPreferences.nudgeAmount

        SynapseNode.lineColor = NetworkPreferences.lineColor
        SynapseNode.excitatoryColor = NetworkPreferences.excitatorySynapseColor
        SynapseNode.inhibitoryColor = NetworkPreferences.inhibitorySynapseColor
        SynapseNode.zeroWeightColor = NetworkPreferences.zeroWeightColor
        SynapseNode.minDiameter = NetworkPreferences.minWeightSize
        SynapseNode.maxDiameter = NetworkPreferences.maxWeightSize
        SynapseNode.zeroWeightColor = NetworkPreferences.zeroWeightColor

        network.flatNeuronList.map {
            it.events.colorChanged.fire()
        }
        network.flatSynapseList.forEach {
            it.events.colorPreferencesChanged.fire()
        }
        network.getModels<WeightMatrix>().forEach {
            it.events.colorPreferencesChanged.fire()
        }

    }


    /**
     * Main initialization of the network panel.
     */
    init {
        super.setLayout(BorderLayout())

        NetworkPreferences.registerChangeListener(preferenceLoader)
        preferenceLoader()

        toolbars.apply {
            cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
            val flowLayout = FlowLayout(FlowLayout.LEFT).apply { hgap = 0; vgap = 0 }
            add("Center", JPanel(flowLayout).apply {
                add(editToolBar)
                add(mainToolBar)
            })
        }

        add("North", toolbars)
        add("Center", canvas)
        add("South", JPanel().apply { add(timeLabel) })

        addKeyBindings()

        // Repaint whenever window is opened or changed.
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(arg0: ComponentEvent) {
                network.events.zoomToFitPage.fire()
            }
        })

        // Add all network elements (important for de-serializing)
        runBlocking {
            network.modelsInReconstructionOrder.forEach { createNode(it) }
        }

        initEventHandlers()
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
     * Screenelements follow a standard pattern to be displayed properly.
     * For example, Synapse nodes are always at the bottom, then SynapseGroupNodes right above them.
     * Calls to lowerToBottom and raiseToTop should be avoided for top level screen elements in favor of using this function.
     */
    private fun addNodeOrdered(node: ScreenElement) {
        fun addNodeToIndex(node: ScreenElement, index: Int) {
            if (index > 0) {
                canvas.layer.addChild(index, node)
            } else {
                canvas.layer.addChild(node)
            }
        }

        fun findIndexOfType(type: KClass<out ScreenElement>): Int {
            return canvas.layer.childrenIterator.toSequence().indexOfLast { it != null && it::class == type }
        }

        when (node) {
            is SynapseNode -> {
                canvas.layer.addChild(0, node)
            }
            is SynapseGroupNode -> {
                val index = findIndexOfType(SynapseNode::class)
                addNodeToIndex(node, index)
            }
            is WeightMatrixNode -> {
                val index = findIndexOfType(SynapseGroupNode::class)
                addNodeToIndex(node, index)
            }
            else -> {
                canvas.layer.addChild(node)
            }
        }
    }

    /**
     * Add a screen element to the network panel and rezoom the page.
     */
    private inline fun <T : ScreenElement> addScreenElement(block: () -> T) = block().also { node ->
        modelNodeMap[node.model] = node
        addNodeOrdered(node)
        node.model.events.selected.on {
            if (node is NeuronGroupNode) {
                selectionManager.add(node.interactionBox)
            } else {
                selectionManager.add(node)
            }
        }
        node.model.events.deleted.on {
            network.events.batchNodeRemoval.fire(it)
        }
        network.events.zoomToFitPage.fire()
    }

    private suspend fun createNode(model: NetworkModel): ScreenElement {
        return when (model) {
            is Neuron -> createNode(model)
            is Synapse -> createNode(model)
            is NeuronArray -> createNode(model)
            is ActivationSequence -> createNode(model)
            is TransformerBlock -> createNode(model)
            is NeuronCollection -> createNode(model)
            is NeuronGroup -> createNode(model)
            is AbstractNeuronCollection -> createNode(model)
            is SynapseGroup -> createNode(model)
            is Connector -> createNode(model)
            is Subnetwork -> createNode(model)
            is SupervisedModel -> createNode(model)
            is InfoText -> createNode(model)
            is NetworkTextObject -> createNode(model)
            // is DeepNet -> createNode(model)
            else -> throw IllegalArgumentException()
        }
    }

    suspend fun createNode(neuron: Neuron) = addScreenElement {
        NeuronNode(this, neuron)
    }

    suspend fun createNode(synapse: Synapse) = addScreenElement {
        synapse.isVisible = freeWeightsVisible
        val source = modelNodeMap.get<NeuronNode>(synapse.source)
        val target = modelNodeMap.get<NeuronNode>(synapse.target)
        SynapseNode(this, source, target, synapse)
    }

    suspend fun createNode(neuronGroup: AbstractNeuronCollection) = addScreenElement {

        fun createNeuronGroupNode() = when (neuronGroup) {
            is SOMGroup -> SOMGroupNode(this, neuronGroup)
            else -> NeuronGroupNode(this, neuronGroup)
        }

        val neuronNodes = neuronGroup.neuronList.map { neuron -> createNode(neuron) }
        val customInfoNode = neuronGroup.customInfo?.let { createNode(it) }
        createNeuronGroupNode().apply {
            addNeuronNodes(neuronNodes)
            customInfoNode?.let { setCustomInfoNode(it) }
        }
    }

    suspend fun createNode(neuronArray: NeuronArray) = addScreenElement { NeuronArrayNode(this, neuronArray) }

    suspend fun createNode(activationSequence: ActivationSequence) = addScreenElement { ActivationSequenceNode(this, activationSequence) }

    suspend fun createNode(transformerBlock: TransformerBlock) = addScreenElement { TransformerBlockNode(this, transformerBlock) }

    suspend fun createNode(classifier: SmileClassifier) = addScreenElement {
        SmileClassifierNode(this, classifier)
    }

    suspend fun createNode(neuronCollection: NeuronCollection) = addScreenElement {
        val neuronNodes = neuronCollection.neuronList.map {
            modelNodeMap.get<NeuronNode>(it)
        }
        NeuronCollectionNode(this, neuronCollection).apply { addNeuronNodes(neuronNodes) }
    }

    suspend fun createNode(synapseGroup: SynapseGroup) = addScreenElement {
        with(synapseGroup.synapses) {
            if (size < NetworkPreferences.synapseVisibilityThreshold) {
                forEach {
                    createNode(it)
                }
            }
        }
        SynapseGroupNode(this, synapseGroup)
    }

    suspend fun createNode(weightMatrix: Connector) = addScreenElement {
        WeightMatrixNode(this, weightMatrix)
    }

    suspend fun createNode(supervisedModel: SupervisedModel) = addScreenElement {
        val arrayNodes = supervisedModel.layers.map { modelNodeMap.get<ScreenElement>(it) }
        val weightMatrixNodes = supervisedModel.weightMatrices.map { modelNodeMap.get<ScreenElement>(it) }
        SupervisedModelNode(this, supervisedModel).apply {
            arrayNodes.forEach { addNode(it) }
            weightMatrixNodes.forEach { addNode(it) }
        }
    }

    suspend fun createNode(text: NetworkTextObject) = addScreenElement {
        TextNode(this, text)
    }

    suspend fun createNode(infoText: InfoText) = addScreenElement {
        TextInfoNode(this, infoText)
    }

    suspend fun createNode(subnetwork: Subnetwork) = addScreenElement {

        fun createSubNetwork() = when (subnetwork) {
            is Hopfield -> HopfieldNode(this, subnetwork)
            is CompetitiveNetwork -> CompetitiveNetworkNode(this, subnetwork)
            is SOMNetwork -> SOMNetworkNode(this, subnetwork)
            is SRNNetwork -> SRNNode(this, subnetwork)
            is RestrictedBoltzmannMachine -> RBMNode(this, subnetwork)
            is BackpropNetwork -> BackpropNetworkNode(this, subnetwork)
            is SmileClassifier -> SmileClassifierNode(this, subnetwork)
            else -> SubnetworkNode(this, subnetwork)
        }

        val subnetworkNodes = subnetwork.modelList.allInUpdatingOrder.map {
            createNode(it)
        }
        val customInfoNode = subnetwork.customInfo?.let { createNode(it) }
        createSubNetwork().apply {
            // Add "sub-nodes" to subnetwork node
            subnetworkNodes.forEach { addNode(it) }
            customInfoNode?.let { setInfoTextNode(it) }
        }

    }

    suspend fun deleteSelectedObjects() {

        val selectedModels = selectionManager.selection.map { it.model }.sortedBy { updatingOrder(it) }

        val undeleteContext = UndeleteContext(this, selectedModels)

        val deletedModels = network.deleteModels(selectedModels.reversed())

        selectionManager.clear()

        undoManager.addUndoableAction(
            description = "Delete selected objects",
            undo = {
                undeleteContext.restore(deletedModels)
            },
            redo = {
                network.deleteModels(selectedModels.reversed())
            }
        )

        network.events.zoomToFitPage.fire()
    }

    private fun createEditToolBar() = CustomToolBar().apply {
        with(networkActions) {
            add(undoAction())
            add(redoAction())
            addSeparator()
            networkEditingActions.forEach { add(it) }
            addSeparator()
            add(clearNodeActivationsAction)
            add(randomizeObjectsAction)
        }
    }

    fun copy() {
        if (selectionManager.isEmpty) return
        selectionManager.filterSelectedModels<LocatableModel>().sortTopBottom().firstOrNull()?.let {
            network.placementManager.lastSelectedModel = it
        }
        Clipboard.clear()
        Clipboard.add(selectionManager.selectedModels)
    }

    suspend fun cut() {
        copy()
        deleteSelectedObjects()
    }

    suspend fun paste() {
        Clipboard.paste(this)
    }

    suspend fun duplicate() {
        if (selectionManager.isNotEmpty) {
            copy()
        }
        network.placementManager.useLastClickedLocation = false
        paste()
    }

    private fun updateLocationsWithUndoableAction(
        description: String,
        models: List<LocatableModel>,
        modification: (index: Int, model: LocatableModel) -> Unit
    ) {
        val previousLocations = models.associateWith { it.location.copy() }

        models.forEachIndexed { index, model -> modification(index, model) }

        val updatedLocations = models.associateWith { it.location.copy() }

        undoManager.addUndoableAction(
            description = description,
            undo = {
                previousLocations.forEach { (model, previousLocation) -> model.location = previousLocation }
            },
            redo = {
                updatedLocations.forEach { (model, updatedLocation) -> model.location = updatedLocation }
            }
        )
    }

    fun alignHorizontal() {
        val models = selectionManager.filterSelectedModels<LocatableModel>()

        if (models.isEmpty()) return

        val minY = models.minOf { it.locationY }
        updateLocationsWithUndoableAction("Align horizontal", models) { _, model -> model.locationY = minY }

        repaint()
    }

    fun alignVertical() {
        val models = selectionManager.filterSelectedModels<LocatableModel>()

        if (models.isEmpty()) return

        val minX = models.minOf { it.locationX }
        updateLocationsWithUndoableAction("Align vertical", models) { _, model -> model.locationX = minX }

        repaint()
    }

    fun spaceHorizontal() {
        val models = selectionManager.filterSelectedModels<LocatableModel>()

        if (models.isEmpty()) return

        val sortedModels = models.sortedBy { it.locationX }
        val min = sortedModels.first().locationX
        val max = sortedModels.last().locationX
        val spacing = (max - min) / (models.size - 1)

        updateLocationsWithUndoableAction("Space horizontal", sortedModels) { i, model -> model.locationX = min + spacing * i }
        repaint()
    }

    fun spaceVertical() {
        val models = selectionManager.filterSelectedModels<LocatableModel>()

        if (models.isEmpty()) return

        val sortedModels = models.sortedBy { it.locationY }
        val min = sortedModels.first().locationY
        val max = sortedModels.last().locationY
        val spacing = (max - min) / (models.size - 1)

        updateLocationsWithUndoableAction("Space vertical", sortedModels) { i, model -> model.locationY = min + spacing * i }
        repaint()
    }

    fun nudge(dx: Int, dy: Int) {
        val models = selectionManager.filterSelectedModels<LocatableModel>()
        updateLocationsWithUndoableAction("Nudge", models) { _, model -> model.location += point(dx * nudgeAmount, dy * nudgeAmount) }
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
        clearSelectedObjects()
        selectionManager.filterSelectedModels<Synapse>().forEach { it.hardClear() }
        selectionManager.filterSelectedModels<NeuronArray>().forEach { it.hardClear() }
        selectionManager.filterSelectedModels<WeightMatrix>().forEach { it.hardClear() }
        selectionManager.filterSelectedModels<SynapseGroup>().forEach { it.clear() }
    }

    fun selectNeuronsInNeuronGroups() {
        selectionManager.filterSelectedModels<AbstractNeuronCollection>()
            .flatMap { it.neuronList }
            .forEach { it.select() }
        selectionManager.filterSelectedNodes<InteractionBox>().forEach {selectionManager.remove(it) }
    }

    /**
     * Connect source and target model items using a default action.
     *
     * For neuron groups or arrays, uses a weight matrix.
     */
    fun connectSelectedModelsDefault(allowSelfConnection: Boolean = false) {

        with(selectionManager) {

            if (connectLayers()) {
                return
            }

            connectFreeWeights(allowSelfConnection)
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
            NetworkPreferences.connectionStrategy.connectNeurons(sourceNeurons, targetNeurons).addToNetworkAsync(network)
        }
    }

    /**
     * Connect free weights using the default connection strategy
     */
    fun connectFreeWeights(allowSelfConnection: Boolean = false) {
        // TODO: For large numbers of connections maybe pop up a warning and depending on button pressed make the
        // weights automatically be invisible
        with(selectionManager) {
            val sourceNeurons = filterSelectedSourceModels<Neuron>() +
                    filterSelectedSourceModels<NeuronCollection>().flatMap { it.neuronList } +
                    filterSelectedSourceModels<NeuronGroup>().flatMap { it.neuronList }
            val targetNeurons = filterSelectedModels<Neuron>() +
                    filterSelectedModels<NeuronCollection>().flatMap { it.neuronList } +
                    filterSelectedModels<NeuronGroup>().flatMap { it.neuronList }
            val synapses = NetworkPreferences.connectionStrategy.copy().also {
                it.percentExcitatory = 100.0
                when (it) {
                    is AllToAll -> it.allowSelfConnection = allowSelfConnection
                    is Sparse -> it.allowSelfConnection = allowSelfConnection
                    is FixedDegree -> it.allowSelfConnections = allowSelfConnection
                    is RadialProbabilistic -> it.allowSelfConnections = allowSelfConnection
                }
            }.connectNeurons(sourceNeurons, targetNeurons)
            synapses.addToNetworkAsync(network)
            undoManager.addUndoableAction(
                description = "Connect nodes",
                undo = { synapses.forEach { it.delete() } },
                redo = { synapses.addToNetwork(network, usePlacementManager = false, useAutoAssignId = false) }
            )
        }

    }

    /**
     * Connect [Layer] objects.
     *
     * @retrun false if the source and target selections did not have a [Layer]
     */
    private fun NetworkSelectionManager.connectLayers(): Boolean {
        val sources = filterSelectedSourceModels(Layer::class.java)
        val targets = filterSelectedModels(Layer::class.java)
        if (sources.isNotEmpty() && targets.isNotEmpty()) {
            // TODO: Ability to set defaults for weight matrix that is added
            val addedMatrices = sources.cartesianProduct(targets).mapNotNull { (s, t) ->
                WeightMatrix(s, t)
            }
            network.addNetworkModels(addedMatrices)
            undoManager.addUndoableAction(
                description = "Connect layers",
                undo = { addedMatrices.forEach {it.delete()}},
                redo = {
                    network.addNetworkModels(addedMatrices, usePlacementManager = false, useAutoAssignedId = false)
                        .awaitAll()
                    addedMatrices.forEach { it.afterRestore() }
                }
            )
            return true
        }
        return false
    }

    /**
     * Connect first selected neuron groups with a synapse group, if any are selected.
     *
     * @retrun false if there source and target neurons did not have a neuron group.
     */
    fun NetworkSelectionManager.connectNeuronGroups(): List<SynapseGroup> {
        val sourceCollections = filterSelectedSourceModels(AbstractNeuronCollection::class.java)
        val targetCollections = filterSelectedModels(AbstractNeuronCollection::class.java)
        val synapseGroups = (sourceCollections cartesianProduct targetCollections).map { (src, tar) ->
            SynapseGroup(src, tar).also { network.addNetworkModel(it) }
        }

        val synapseGroupSynapses = synapseGroups.associateWith { it.synapses.toList() }

        undoManager.addUndoableAction(
            description = "Connect neuron groups",
            undo = { synapseGroups.map { launch { it.delete() } }.joinAll() },
            redo = {
                synapseGroupSynapses.entries.map { (sg, synapses) ->
                    launch {
                        sg.synapses.clear()
                        sg.synapses.addAll(synapses)
                        network.addNetworkModel(sg, usePlacementManager = false, useAutoAssignedId = false)?.await()
                        sg.afterRestore()
                    }
                }.joinAll()
            })

        return synapseGroups
    }

    private fun createMainToolBar() = CustomToolBar().apply {
        with(networkActions) {
            networkModeActions.forEach { add(it) }
            addSeparator()
            add(networkActions.zoomInAction())
            add(networkActions.zoomOutAction())
            add(networkActions.resetZoomAction())
            add(JToggleButton().apply {
                icon = ResourceManager.getSmallIcon("menu_icons/ZoomFitPage.png")
                fun updateButton() {
                    isSelected = autoZoom
                    border = if (autoZoom) BorderFactory.createLoweredBevelBorder() else BorderFactory.createEmptyBorder()
                    val onOff = if (autoZoom) "on" else "off"
                    toolTipText = "Autozoom is $onOff"
                }
                updateButton()
                network.events.zoomModeChanged.on {
                    updateButton()
                }
                addActionListener { e ->
                    val button = e.source as JToggleButton
                    autoZoom = button.isSelected
                    updateButton()
                }
            })
        }
    }

    private fun initEventHandlers() {
        network.events.apply {
            modelAdded.on(Dispatchers.Swing, wait = true) {
                createNode(it)
            }
            modelRemoved.on {
                zoomToFitPage.fire()
            }
            batchNodeRemoval.on { models ->
                val modelsUniq = models.toSet()
                val nodes = modelsUniq.map {
                    modelNodeMap.getImmediately<ScreenElement>(it)
                }
                withContext(Swing) { canvas.layer.removeChildren(nodes) }
                modelsUniq.forEach { modelNodeMap.remove(it) }
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
                    launch(Dispatchers.Swing) {
                        canvas.camera.setViewBounds(adjustedFiltered)
                        repaint()
                    }
                }
                launch(Dispatchers.Swing) {
                    canvas.repaint()
                }
            }
            selected.on { list ->
                selectionManager.set(list.map { modelNodeMap.get(it) })
            }
        }

    }

    private fun NetworkSelectionManager.setUpSelectionEvents() {
        events.apply {
            selection.on(Dispatchers.Swing) { old, new ->
                val (removed, added) = old complement new
                removed.forEach { NodeHandle.removeSelectionHandleFrom(if (it is WeightMatrixNode) it.imageBox else it) }
                added.forEach {
                    if (it is InteractionBox) {
                        NodeHandle.addSelectionHandleTo(it, NodeHandle.INTERACTION_BOX_SELECTION_STYLE)
                    } else if (it is WeightMatrixNode) {
                        NodeHandle.addSelectionHandleTo(it.imageBox)
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

    fun getNode(model: NetworkModel) = runBlocking { modelNodeMap.get<ScreenElement>(model) }

    inner class NetworkCanvas : PCanvas() {
        init {
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
            addInputEventListener(object : PBasicInputEventHandler() {
                override fun mouseWheelRotated(event: PInputEvent) {
                    val swingEvent = (event.sourceSwingEvent as MouseWheelEvent)
                    val newScale = 1.1.pow(swingEvent.preciseWheelRotation)
                    autoZoom = false
                    scale(1 / newScale)
                }
            })
            addInputEventListener(WandEventHandler(this@NetworkPanel))
        }

        /**
         * Change the current zoom level up or down by the scaling factor.
         *
         * For example:
         * 1.1 zooms in by ~10%
         * 0.9 zooms out by ~10%
         */
        fun scale(scalingFactor: Double) {
            val canvasCenter: Point2D = camera.bounds.center
            val (x, y) = camera.localToView(canvasCenter)
            val newWidth = camera.viewBounds.width / scalingFactor
            val newHeight = camera.viewBounds.height / scalingFactor
            val newX = x - newWidth / 2
            val newY = y - newHeight / 2
            camera.setViewBoundsNoOverflow(Rectangle2D.Double(newX, newY, newWidth, newHeight))
        }
    }

    fun addNeuronsAsync(
        numNeurons: Int,
        template: Neuron,
        layout: Layout
    ) {
        launch {
            val neurons = network.addNeurons(numNeurons) { updateRule = template.updateRule }
            layout.layoutNeurons(neurons)
            undoManager.addUndoableAction(
                description = "Add $numNeurons neuron(s)",
                undo = { neurons.forEach{it.delete()} },
                redo = { network.addNetworkModels(neurons, usePlacementManager = false, useAutoAssignedId = false).awaitAll() }
            )
        }
    }

    fun addNeuronGroupAsync(
        numNeurons: Int,
        template: Neuron,
        layout: Layout,
        label: String
    ) {
        launch {
            val neurons = network.addNeurons(numNeurons) { updateRule = template.updateRule }
            val ng = NeuronGroup(neurons)
            ng.layout = layout
            network.addNetworkModel(ng)
            ng.applyLayout()
            if (label.isNotEmpty()) {
                ng.label = label
            }
            // TODO: Undoable action
        }
    }

}