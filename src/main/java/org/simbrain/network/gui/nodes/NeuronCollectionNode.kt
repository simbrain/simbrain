package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import org.piccolo2d.util.PBounds
import org.simbrain.network.core.*
import org.simbrain.network.gui.*
import org.simbrain.util.*
import org.simbrain.util.piccolo.Outline
import org.simbrain.workspace.gui.SimbrainDesktop.actionManager
import java.awt.Color
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.util.concurrent.CopyOnWriteArraySet
import javax.swing.*

/**
 * PNode representation of a [NeuronCollection]. Contains an interaction box and
 * outlined neuron nodes as children. Compare [SubnetworkNode].
 */
class NeuronCollectionNode(
    networkPanel: NetworkPanel,
    override val model: NeuronCollection
) : ScreenElement(networkPanel) {

    val outlinedObjects = Outline()

    private var interactionBox: InteractionBox

    val neuronNodes: MutableSet<NeuronNode> = CopyOnWriteArraySet()

    private var customInfoNode: ScreenElement? = null

    private val customMenuItems = mutableListOf<JMenuItem>()

    init {
        addChild(outlinedObjects)

        interactionBox = NeuronCollectionInteractionBox(networkPanel).apply {
            setText(model.displayName)
        }
        addChild(interactionBox)

        model.events.apply {
            labelChanged.on(swingDispatcher) { _, _ -> interactionBox.setText(model.displayName) }
            locationChanged.on(swingDispatcher) {
                pullPositionFromModel()
                outlinedObjects.updateBounds()
            }
            shouldUpdateOutline.on(swingDispatcher) { updateOutline() }
        }
    }

    // Neuron node management

    fun addNeuronNodes(nodes: Collection<NeuronNode>) {
        neuronNodes.addAll(nodes)
        for (node in nodes) {
            val events = node.neuron.events
            events.deleted.on(swingDispatcher) { _ ->
                neuronNodes.remove(node)
                fireUpdateOutline()
            }
            events.locationChanged.on(Dispatchers.Default) { fireUpdateOutline() }
            events.labelChanged.on(Dispatchers.Default) { _, _ -> fireUpdateOutline() }
        }
        fireUpdateOutline()
    }

    fun removeNeuronNode(neuronNode: NeuronNode) {
        neuronNodes.remove(neuronNode)
    }

    fun setCustomInfoNode(info: ScreenElement) {
        customInfoNode = info
        updateCustomInfoLocation()
        model.events.customInfoUpdated.on(swingDispatcher) { fireUpdateOutline() }
        fireUpdateOutline()
    }

    fun addCustomMenuItem(item: JMenuItem) {
        customMenuItems.add(item)
    }

    // Position sync

    fun pullPositionFromModel() {
        for (node in neuronNodes) {
            node.pullViewPositionFromModel()
        }
        fireUpdateOutline()
    }

    override fun offset(dx: kotlin.Double, dy: kotlin.Double) {
        for (node in neuronNodes) {
            node.offset(dx, dy)
        }
        customInfoNode?.offset(dx, dy)
    }

    // Selection

    fun selectNeurons() {
        neuronNodes.forEach { it.neuron.select() }
    }

    fun editNeurons() {
        neuronNodes.map { it.neuron }.toList().createEditorDialog<Neuron>().display()
    }

    // Outline

    private fun updateOutline() {
        val nodes = HashSet<ScreenElement>(neuronNodes)
        customInfoNode?.let {
            nodes.add(it)
            updateCustomInfoLocation()
        }
        outlinedObjects.resetOutlinedNodes(nodes)
    }

    private fun fireUpdateOutline() {
        model.events.shouldUpdateOutline.fire()
    }

    private fun updateCustomInfoLocation() {
        customInfoNode?.let { info ->
            val center = model.neuronList.centerLocation
            val minY = model.neuronList.minY
            (info.model as LocatableModel).setLocation(center.x, minY - 40)
        }
    }

    // Layout

    override fun layoutChildren() {
        if (visible) {
            val bounds = outlinedObjects.fullBounds
            interactionBox.centerFullBoundsOnPoint(
                bounds.centerX,
                bounds.y - interactionBox.fullBounds.height / 2 + 0.5
            )
        }
    }

    override val isDraggable = true

    override fun isIntersecting(bound: PBounds?) = interactionBox.isIntersecting(bound)

    override fun acceptsSourceHandle() = true

    fun getInteractionBox() = interactionBox

    // Property dialog

    override val propertyDialog: StandardDialog?
        get() = networkPanel.createNeuronCollectionDialog(model)

    // Context menu

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            add(renameAction)
            add(removeAction)
            addSeparator()

            add(networkPanel.createEditNeuronCollectionAction(model) { propertyDialog })
            addSeparator()
            with(networkPanel.networkActions) {
                add(model.showApplyLayoutDialogAction())
            }

            // Selection
            addSeparator()
            add(object : AbstractAction("Select neurons") {
                init { putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, 0)) }
                override fun actionPerformed(e: ActionEvent?) = selectNeurons()
            })
            add(object : AbstractAction("Edit neurons...") {
                override fun actionPerformed(e: ActionEvent?) = editNeurons()
            })
            addSeparator()
            add(object : AbstractAction("Select Incoming Synapses") {
                override fun actionPerformed(e: ActionEvent?) {
                    networkPanel.selectionManager.clear()
                    model.incomingWeights.forEach { it.select() }
                }
            })
            add(object : AbstractAction("Select Outgoing Synapses") {
                override fun actionPerformed(e: ActionEvent?) {
                    networkPanel.selectionManager.clear()
                    model.outgoingWeights.forEach { it.select() }
                }
            })

            // Clamping
            addSeparator()
            clampNeuronsAction.isEnabled = !model.isAllClamped
            unclampNeuronsAction.isEnabled = !model.isAllUnclamped
            add(clampNeuronsAction)
            add(unclampNeuronsAction)

            // Connect
            addSeparator()
            add(networkPanel.networkActions.connectWithWeightMatrix)
            add(networkPanel.networkActions.connectWithSynapseGroup)

            // Set as source
            addSeparator()
            add(object : AbstractAction("Set as Source") {
                override fun actionPerformed(e: ActionEvent?) {
                    networkPanel.selectionManager.clear()
                    networkPanel.selectionManager.set(interactionBox)
                    networkPanel.selectionManager.convertSelectedNodesToSourceNodes()
                }
            })
            add(object : AbstractAction("Clear Source") {
                override fun actionPerformed(e: ActionEvent?) {
                    networkPanel.selectionManager.clearAllSource()
                }
            })

            // Custom menu items (from subnetwork specializations)
            if (customMenuItems.isNotEmpty()) {
                addSeparator()
                customMenuItems.forEach { add(it) }
            }

            // Training / input
            addSeparator()
            add(networkPanel.networkActions.createSupervisedModelAction)
            add(networkPanel.networkActions.createTestInputPanelAction(model))
            add(networkPanel.networkActions.createAddActivationToInputAction(model))

            // Plots
            addSeparator()
            with(org.simbrain.workspace.gui.SimbrainDesktop.workspace.couplingManager) {
                val plotAction = actionManager.createCoupledPlotMenu(
                    model.getProducer("getActivationArray"),
                    "${model.displayName} Activations",
                    "Plot"
                )
                add(plotAction)
                if (model.neuronList.any { it.updateRule is SpikingNeuronUpdateRule<*, *> }) {
                    plotAction.addSeparator()
                    plotAction.add(
                        actionManager.createCoupledRasterPlotAction(
                            model.getProducer("getSpikes"),
                            "${model.displayName} Spikes"
                        )
                    )
                }
            }
            add(networkPanel.networkActions.createNeuronCollectionCoupledImageWorld(model))
            add(networkPanel.networkActions.createRecordActivationAction(model))

            // Couplings
            addSeparator()
            networkPanel.networkComponent.createCouplingMenu(model)?.let { add(it) }
        }

    // Interaction box

    private inner class NeuronCollectionInteractionBox(net: NetworkPanel) : InteractionBox(net) {
        init { setPaint(Color(209, 255, 204)) }

        override val propertyDialog get() = this@NeuronCollectionNode.propertyDialog
        override val model get() = this@NeuronCollectionNode.model
        override val contextMenu get() = this@NeuronCollectionNode.contextMenu
        override val toolTipText: String
            get() = createTooltipTextWithLocation(this@NeuronCollectionNode.model) {
                "Name: ${(it as NeuronCollection).displayName} (${it.size} neurons)"
            }
    }

    // Actions

    private val renameAction = object : AbstractAction("Rename Neuron Collection...") {
        override fun actionPerformed(e: ActionEvent?) {
            JOptionPane.showInputDialog("Name:", model.label)?.let { model.label = it }
        }
    }

    private val removeAction = object : AbstractAction() {
        init {
            putValue(SMALL_ICON, ResourceManager.getSmallIcon("menu_icons/minus.png"))
            putValue(NAME, "Remove Neuron Collection")
            putValue(SHORT_DESCRIPTION, "Remove neuron collection")
        }
        override fun actionPerformed(e: ActionEvent?) = model.deleteBlocking()
    }

    private val clampNeuronsAction = object : AbstractAction() {
        init {
            putValue(SMALL_ICON, ResourceManager.getSmallIcon("menu_icons/Clamp.png"))
            putValue(NAME, "Clamp Neurons")
            putValue(SHORT_DESCRIPTION, "Clamp all neurons in this collection.")
        }
        override fun actionPerformed(e: ActionEvent?) { model.isClamped = true }
    }

    private val unclampNeuronsAction = object : AbstractAction() {
        init {
            putValue(SMALL_ICON, ResourceManager.getSmallIcon("menu_icons/Clamp.png"))
            putValue(NAME, "Unclamp Neurons")
            putValue(SHORT_DESCRIPTION, "Unclamp all neurons in this collection.")
        }
        override fun actionPerformed(e: ActionEvent?) { model.isClamped = false }
    }
}
