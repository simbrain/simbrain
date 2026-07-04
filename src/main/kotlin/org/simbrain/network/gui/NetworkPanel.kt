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
import org.simbrain.network.core.*
import org.simbrain.network.gui.MouseEventHandler.MouseCursor
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.nodes.*
import org.simbrain.network.gui.nodes.subnetworkNodes.*
import org.simbrain.network.layouts.Layout
import org.simbrain.network.llm.LanguageModel
import org.simbrain.network.llm.TeachingTransformer
import org.simbrain.network.smile.ClassifierNetwork
import org.simbrain.network.subnetworks.*
import org.simbrain.network.trainers.SupervisedModel
import org.simbrain.util.*
import org.simbrain.util.piccolo.Outline
import org.simbrain.util.piccolo.setViewBoundsNoOverflow
import org.simbrain.util.piccolo.unionOfGlobalFullBounds
import org.simbrain.util.widgets.SimbrainToggleButton
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseWheelEvent
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.util.prefs.PreferenceChangeListener
import javax.swing.JPanel
import kotlin.math.abs
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
     * Whether to display free weights (those not in a synapse group) or not.
     */
    var freeWeightsVisible = true
        set(value) {
            field = value
            network.freeSynapses.forEach { it.isVisible = value }
            network.events.freeWeightVisibilityChanged.fire(value)
        }

    /**
     * When true, synapses are only displayed while their source neuron is spiking.
     * This does not override an individual synapse's own visibility (that remains
     * the higher-priority gate).
     */
    var synapseSpikingOnlyVisible = false
        set(value) {
            field = value
            network.events.synapseSpikingOnlyVisibilityChanged.fire(value)
            // Trigger a redraw by nudging synapse graphics via the throttled update event
            // Individual SynapseNodes will listen and adjust their own visibility accordingly
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

        SynapseNode.lineColor = NetworkPreferences.connectionLineColor
        SynapseNode.excitatoryColor = NetworkPreferences.excitatorySynapseColor
        SynapseNode.inhibitoryColor = NetworkPreferences.inhibitorySynapseColor
        SynapseNode.zeroWeightColor = NetworkPreferences.zeroWeightColor
        SynapseNode.minDiameter = NetworkPreferences.minWeightSize
        SynapseNode.maxDiameter = NetworkPreferences.maxWeightSize

        network.flatSynapseList.forEach {
            it.events.colorPreferencesChanged.fire()
        }
        network.getModels<WeightMatrix>().forEach {
            it.events.colorPreferencesChanged.fire()
            it.events.updateGraphics.fire()
        }
        // Re-apply theme-derived colors that nodes cache at construction (not driven by a model event):
        // neuron fill/outline/text, group/subnet tabs, free text, subnet outlines, arrows, and image
        // borders. A single traversal, since this runs on every preference commit and theme switch.
        canvas.layer.allNodes.forEach { node ->
            when (node) {
                is Outline -> node.refreshThemeColor()
                is ImageBox -> node.updateBorderColorFromPreferences()
                is NodeHandle -> node.refreshThemeColor()
                is WeightMatrixNode -> node.updateArrowColorFromPreferences()
                is TensorConnectorNode -> node.updateArrowColorFromPreferences()
                is FlattenConnectorNode -> node.updateArrowColorFromPreferences()
                is SmileClassifierNode -> node.updateArrowColorFromPreferences()
            }
            if (node is ScreenElement) node.refreshTheme()
        }
        canvas.repaint()
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
        bindPixelSelectionToComponentSelection()
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
        fun findIndexOfType(type: KClass<out ScreenElement>): Int {
            return canvas.layer.childrenIterator.toSequence().indexOfLast { it != null && it::class == type }
        }

        when (node) {
            is WeightMatrixNode -> {
                canvas.layer.addChild(0, node)
            }
            is SynapseNode -> {
                val index = findIndexOfType(WeightMatrixNode::class)
                canvas.layer.addChild(index + 1, node)
            }
            is SynapseGroupNode -> {
                val index = findIndexOfType(SynapseNode::class)
                canvas.layer.addChild(index + 1, node)
            }
            is TensorConnectorNode -> {
                val index = findIndexOfType(SynapseGroupNode::class)
                canvas.layer.addChild(index + 1, node)
            }
            is FlattenConnectorNode -> {
                val index = findIndexOfType(TensorConnectorNode::class)
                canvas.layer.addChild(index + 1, node)
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
        node.model.events.selected.on(Dispatchers.Default) {
            if (node is NeuronCollectionNode) {
                selectionManager.add(node.getInteractionBox())
            } else {
                selectionManager.add(node)
            }
        }
        node.model.events.deleted.on(Dispatchers.Default) {
            network.events.batchNodeRemoval.fire(node)
        }
        network.events.zoomToFitPage.fire()
    }

    private suspend fun createNode(model: NetworkModel): ScreenElement {
        return when (model) {
            is Neuron -> createNode(model)
            is Synapse -> createNode(model)
            is NeuronArray -> createNode(model)
            is ActivationSequence -> createNode(model)
            is LanguageModel -> createNode(model)
            is TeachingTransformer -> createNode(model)
            is NeuronCollection -> createNode(model)
            is SynapseGroup -> createNode(model)
            is TensorLayer -> createNode(model)
            is FlattenConnector -> createNode(model)
            is TensorConnector -> createNode(model)
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

    suspend fun createNode(neuronArray: NeuronArray) = addScreenElement { NeuronArrayNode(this, neuronArray) }

    suspend fun createNode(tensorLayer: TensorLayer) = addScreenElement { TensorNode(this, tensorLayer) }

    suspend fun createNode(tensorConnector: TensorConnector) = addScreenElement { TensorConnectorNode(this, tensorConnector) }

    suspend fun createNode(flattenConnector: FlattenConnector) = addScreenElement { FlattenConnectorNode(this, flattenConnector) }

    suspend fun createNode(activationSequence: ActivationSequence) = addScreenElement { ActivationSequenceNode(this, activationSequence) }

    suspend fun createNode(languageModel: LanguageModel) = addScreenElement { LanguageModelNode(this, languageModel) }

    suspend fun createNode(teachingTransformer: TeachingTransformer) =
        addScreenElement { TeachingTransformerNode(this, teachingTransformer) }

    suspend fun createNode(classifier: ClassifierNetwork) = addScreenElement {
        SmileClassifierNode(this, classifier)
    }

    suspend fun createNode(neuronCollection: NeuronCollection) = addScreenElement {
        val neuronNodes = neuronCollection.neuronList.map {
            modelNodeMap.getImmediately<NeuronNode>(it) ?: createNode(it)
        }
        val customInfoNode = neuronCollection.customInfo?.let { createNode(it) }
        NeuronCollectionNode(this, neuronCollection).apply {
            addNeuronNodes(neuronNodes)
            customInfoNode?.let { setCustomInfoNode(it) }
        }
    }

    suspend fun createNode(synapseGroup: SynapseGroup) = addScreenElement {
        // Loose individual synapse nodes and the collapsed arrow are mutually exclusive, both driven by
        // synapseGroup.displaySynapses (the single source of truth, kept in sync with the visibility
        // threshold by SynapseGroup.refreshVisibility). Expanded -> every group synapse has a node;
        // collapsed -> the arrow stands in, so its loose nodes are removed.
        suspend fun reconcileLooseSynapseNodes() {
            val groupSynapses = synapseGroup.synapses.toSet()
            val groupSynapseNodes = filterScreenElements<SynapseNode>().filter { it.synapse in groupSynapses }
            if (synapseGroup.displaySynapses) {
                val withNodes = groupSynapseNodes.map { it.synapse }.toSet()
                groupSynapses.filter { it !in withNodes }.forEach { synapse ->
                    createNode(synapse)
                    // Group synapses follow displaySynapses, not the free-weight visibility that
                    // createNode(synapse) applies.
                    synapse.isVisible = synapseGroup.displaySynapses
                }
            } else {
                groupSynapseNodes.forEach {
                    canvas.layer.removeChild(it)
                    modelNodeMap.remove(it.model)
                }
            }
        }
        reconcileLooseSynapseNodes()
        synapseGroup.events.visibilityChanged.on(Dispatchers.Swing) { reconcileLooseSynapseNodes() }
        synapseGroup.events.synapseListChanged.on(Dispatchers.Swing) { reconcileLooseSynapseNodes() }
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
            is ClassifierNetwork -> SmileClassifierNode(this, subnetwork)
            is ConvolutionalNeuralNetwork -> ConvolutionalNeuralNetworkNode(this, subnetwork)
            else -> SubnetworkNode(this, subnetwork)
        }

        val subnetworkNodes = subnetwork.modelList.allInUpdatingOrder.map { model ->
            modelNodeMap.getImmediately<ScreenElement>(model) ?: createNode(model)
        }
        val customInfoNode = subnetwork.customInfo?.let { model ->
            modelNodeMap.getImmediately<ScreenElement>(model) ?: createNode(model)
        }
        createSubNetwork().apply {
            // Add "sub-nodes" to subnetwork node
            subnetworkNodes.forEach { addNode(it) }
            customInfoNode?.let { setInfoTextNode(it) }
        }

    }

    suspend fun deleteSelectedObjects() {

        // A subnetwork's neuron collection is structural and cannot be dismantled piecemeal: ungrouping
        // it (deleting the collection) or emptying it (deleting all of its neurons) would leave a
        // degenerate subnetwork that the undo machinery cannot reconstruct. Such models are protected;
        // the whole subnetwork can still be deleted via its interaction box, and a collection can still
        // be resized by deleting some (not all) of its neurons. See [subnetworkProtectedModels].
        val allSelected = selectionManager.selection.map { it.model }.sortedBy { updatingOrder(it) }
        val protected = network.subnetworkProtectedModels(allSelected)
        val selectedModels = allSelected.filter { it !in protected }

        if (selectedModels.isEmpty()) {
            if (protected.isNotEmpty()) {
                withContext(Dispatchers.Swing) {
                    showWarningDialog(
                        "These are structural parts of a subnetwork and cannot be deleted individually, " +
                            "as that would leave the subnetwork in a state the undo system cannot " +
                            "reconstruct. Delete the whole subnetwork instead (via its interaction box).",
                        "Cannot delete subnetwork components"
                    )
                }
            }
            return
        }

        // Emptying a free container that self-deletes (a NeuronCollection when its last neuron goes, a
        // SynapseGroup when its last synapse goes) needs no special handling here: Network.deleteModels
        // captures it — the last member hits the isLastChildOfParent branch, which deletes and returns the
        // container — so undo restores the container and its grouping.
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
        selectionManager.filterSelectedModels<LocatableModel>().sortLeftRightTopBottom().firstOrNull()?.let {
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
        if (hasAnyPixelSelection()) {
            incrementSelectedPixels()
            return
        }
        selectionManager.filterSelectedModels<NetworkModel>().forEach { it.increment() }
    }

    fun decrementSelectedObjects() {
        if (hasAnyPixelSelection()) {
            decrementSelectedPixels()
            return
        }
        selectionManager.filterSelectedModels<NetworkModel>().forEach { it.decrement() }
    }

    fun clearSelectedObjects() {
        selectionManager.filterSelectedModels<NetworkModel>().forEach { it.clear() }
    }

    fun hardClearSelectedObjects() {
        if (hasAnyPixelSelection()) {
            clearSelectedPixels()
            return
        }
        clearSelectedObjects()
        selectionManager.filterSelectedModels<Synapse>().forEach { it.hardClear() }
        selectionManager.filterSelectedModels<NeuronArray>().forEach { it.hardClear() }
        selectionManager.filterSelectedModels<WeightMatrix>().forEach { it.hardClear() }
        selectionManager.filterSelectedModels<ConvolutionConnector>().forEach { it.hardClear() }
        selectionManager.filterSelectedModels<SynapseGroup>().forEach { it.clear() }
    }

    fun selectNeuronsInNeuronCollections() {
        selectionManager.filterSelectedModels<NeuronCollection>()
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
        selectionManager.connectNeuronCollections()

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
     * Connect free weights using AllToAll with 100% excitatory and no self-connections by default
     */
    fun connectFreeWeights(allowSelfConnection: Boolean = false) {
        with(selectionManager) {
            val sourceNeurons = filterSelectedSourceModels<Neuron>() +
                    filterSelectedSourceModels<NeuronCollection>().flatMap { it.neuronList }
            val targetNeurons = filterSelectedModels<Neuron>() +
                    filterSelectedModels<NeuronCollection>().flatMap { it.neuronList }
            val synapses = AllToAll().apply {
                this.allowSelfConnection = allowSelfConnection
                this.percentExcitatory = 100.0
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
            val addedMatrices = sources.cartesianProduct(targets)
                .filter { (s, t) ->
                    // Skip if WeightMatrix already exists between source and target
                    s.outgoingConnectors.none { connector ->
                        connector is WeightMatrix && connector.target == t
                    }
                }
                .map { (s, t) ->
                    WeightMatrix(s, t)
                }
            if (addedMatrices.isNotEmpty()) {
                network.addNetworkModelsAsync(addedMatrices)
                undoManager.addUndoableAction(
                    description = "Connect layers",
                    undo = { addedMatrices.forEach {it.delete()}},
                    redo = {
                        network.addNetworkModelsAsync(addedMatrices, usePlacementManager = false, useAutoAssignedId = false)
                            .awaitAll()
                        addedMatrices.forEach { it.afterRestore() }
                    }
                )
            }
            return true
        }
        return false
    }

    /**
     * Connect first selected neuron groups with a synapse group, if any are selected.
     *
     * @retrun false if there source and target neurons did not have a neuron group.
     */
    fun NetworkSelectionManager.connectNeuronCollections(): List<SynapseGroup> {
        val sourceCollections = filterSelectedSourceModels(NeuronCollection::class.java)
        val targetCollections = filterSelectedModels(NeuronCollection::class.java)
        val synapseGroups = (sourceCollections cartesianProduct targetCollections)
            .filter { (src, tar) ->
                // Skip if SynapseGroup already exists between source and target
                src.outgoingSg.none { synapseGroup ->
                    synapseGroup.target == tar
                }
            }
            .map { (src, tar) ->
                SynapseGroup(src, tar).also { network.addNetworkModelAsync(it) }
            }

        val synapseGroupSynapses = synapseGroups.associateWith { it.synapses.toList() }

        if (synapseGroups.isNotEmpty()) {
            undoManager.addUndoableAction(
                description = "Connect neuron groups",
                undo = { synapseGroups.map { launch { it.delete() } }.joinAll() },
                redo = {
                    synapseGroupSynapses.entries.map { (sg, synapses) ->
                        launch {
                            sg.synapses.clear()
                            sg.synapses.addAll(synapses)
                            network.addNetworkModel(sg, usePlacementManager = false, useAutoAssignedId = false)
                            sg.afterRestore()
                        }
                    }.joinAll()
                })
        }

        return synapseGroups
    }

    private fun createMainToolBar() = CustomToolBar().apply {
        with(networkActions) {
            networkModeActions.forEach { add(it) }

            // Wand palette button (handles its own color updates)
            val paletteButton = WandPaletteButton(
                NetworkPreferences.wandPalette,
                this@NetworkPanel
            )
            add(paletteButton)

            addSeparator()
            add(networkActions.zoomInAction())
            add(networkActions.zoomOutAction())
            //add(networkActions.resetZoomAction())
            add(SimbrainToggleButton(
                icon = ResourceManager.getSmallIcon("menu_icons/ZoomFitPage.png"),
                stateGetter = { autoZoom },
                stateSetter = { autoZoom = it },
                tooltipGenerator = { isOn -> "Autozoom is ${if (isOn) "on" else "off"}" }
            ).apply {
                network.events.zoomModeChanged.on(Dispatchers.Swing) {
                    updateFromExternalState()
                }
            })
        }
    }

    private fun initEventHandlers() {
        network.events.apply {
            modelAdded.on(Dispatchers.Swing) {
                createNode(it)
            }
            modelRemoved.on(Dispatchers.Default) {
                zoomToFitPage.fire()
            }
            batchNodeRemoval.on(Dispatchers.Default) { nodes ->
                val nodesUniq = nodes.toSet()
                withContext(Swing) {
                    nodesUniq.forEach {
                        canvas.layer.removeChild(it)
                    }
                }
                // Removal is asynchronous (debounced) and can land after the model was re-added with a
                // new node (undo then redo). Only clear the mapping if it still points at this node, so
                // a stale removal never wipes a freshly recreated node.
                nodesUniq.forEach { node -> modelNodeMap.removeIfValue(node.model) { it === node } }
            }
            updateActionsChanged.on(Dispatchers.Swing) { timeLabel.update() }
            updated.on(Dispatchers.Swing.immediate) {
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
            boundsChanged.on(Dispatchers.Swing) {
                zoomToFitPage.fire()
            }
            selected.on(Dispatchers.Default) { list ->
                selectionManager.set(list.map { modelNodeMap.get(it) })
            }
        }

    }

    private fun NetworkSelectionManager.setUpSelectionEvents() {
        events.apply {
            selection.on(Dispatchers.Swing) { old, new ->
                val (removed, added) = old complement new
                removed.forEach { 
                    NodeHandle.removeSelectionHandleFrom(
                        when (it) {
                            is WeightMatrixNode -> it.imageBox
                            is TensorConnectorNode -> if ((it.connector as? ConvolutionConnector)?.kernelGridMode == true) it.kernelGridGroup else it.imageBox
                            else -> it
                        }
                    )
                }
                added.forEach {
                    when {
                        it is InteractionBox -> NodeHandle.addSelectionHandleTo(it, NodeHandle.INTERACTION_BOX_SELECTION_STYLE)
                        it is WeightMatrixNode -> NodeHandle.addSelectionHandleTo(it.imageBox)
                        it is TensorConnectorNode -> {
                            val selectionTarget = if ((it.connector as? ConvolutionConnector)?.kernelGridMode == true) {
                                it.kernelGridGroup
                            } else {
                                it.imageBox
                            }
                            NodeHandle.addSelectionHandleTo(selectionTarget)
                        }
                        else -> NodeHandle.addSelectionHandleTo(it)
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
                    // Only turn off autozoom if the mouse wheel is turned more than a few clicks.
                    if (abs(swingEvent.preciseWheelRotation) > 2) {
                        autoZoom = false
                    }
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
                redo = { network.addNetworkModels(neurons, usePlacementManager = false, useAutoAssignedId = false) }
            )
        }
    }

    fun addNeuronCollectionAsync(
        numNeurons: Int,
        template: Neuron,
        layout: Layout,
        label: String
    ) {
        launch {
            val neurons = network.addNeurons(numNeurons) { updateRule = template.updateRule }
            val nc = NeuronCollection(neurons)
            nc.layout = layout
            network.addNetworkModelAsync(nc)
            nc.applyLayout()
            if (label.isNotEmpty()) {
                nc.label = label
            }
            // TODO: Undoable action
        }
    }

}

/**
 * The subset of [selection] that may not be deleted because doing so would dismantle a composite model
 * (a [Subnetwork] or a [SupervisedModel] overlay) in a way the undo machinery cannot reconstruct.
 *
 * Three things are protected:
 *  - A structural [NeuronCollection] (owned by a subnetwork's [Subnetwork.modelList], or used as a layer
 *    of a supervised overlay). Deleting it would ungroup it (leaving loose neurons); emptying it (deleting
 *    all of its neurons) destroys the collection and cascades, via asynchronous listeners, to the whole
 *    composite — a cascade deleteModels does not capture. Both the collection and (when its full neuron
 *    set is selected) all of its neurons are protected.
 *  - Any model a subnetwork declares essential via [Subnetwork.protectedChildModels] (e.g. a CNN's whole
 *    pipeline, where deleting any one component asynchronously self-deletes the network).
 *
 * Ownership is read from live container membership ([Subnetwork.modelList] / [SupervisedModel.layers])
 * rather than [Network.childToParentMap], because deleting any child wipes that subnetwork's entries from
 * the map (see [Network.deleteModels]) — so a map-based check would stop protecting after a single prior
 * deletion. A free (top-level) collection that is not a composite's layer is never protected and stays
 * freely ungroupable; deleting only some of a collection's neurons (a resize) is likewise allowed.
 */
fun Network.subnetworkProtectedModels(selection: Collection<NetworkModel>): Set<NetworkModel> {
    val selectionSet = selection.toSet()
    val structuralCollections = buildList {
        fun collect(subnet: Subnetwork) {
            addAll(subnet.modelList.all.filterIsInstance<NeuronCollection>())
            subnet.modelList.all.filterIsInstance<Subnetwork>().forEach { collect(it) }
        }
        getModels<Subnetwork>().forEach { collect(it) }
        getModels<SupervisedModel>().forEach { addAll(it.layers.filterIsInstance<NeuronCollection>()) }
    }
    val protectedChildren = buildList {
        fun collect(subnet: Subnetwork) {
            addAll(subnet.protectedChildModels)
            subnet.modelList.all.filterIsInstance<Subnetwork>().forEach { collect(it) }
        }
        getModels<Subnetwork>().forEach { collect(it) }
    }
    return buildSet {
        for (collection in structuralCollections) {
            if (collection in selectionSet) add(collection)
            if (collection.neuronList.isNotEmpty() && selectionSet.containsAll(collection.neuronList)) {
                addAll(collection.neuronList)
            }
        }
        addAll(protectedChildren.filter { it in selectionSet })
    }
}