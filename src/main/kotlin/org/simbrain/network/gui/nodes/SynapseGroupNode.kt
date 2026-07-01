package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.*


class SynapseGroupNode(networkPanel: NetworkPanel, val synapseGroup: SynapseGroup) :
    ScreenElement(networkPanel) {

    /**
     * Reference to the currently used PNode type.
     */
    private var currentNode: Arrow? = null

    /**
     * PNode that represents an aggregate of visible "loose" [SynapseNode]s.
     * when [SynapseGroup.isDisplaySynapses] is true.
     */
    var expandedNode: SynapseGroupNodeExpanded? = null
        private set

    /**
     * PNode that represents a single one-directional green arrow from
     * one neuron group to another.
     */
    private var directedNode: SynapseGroupNodeDirected? = null

    var interactionBox: SynapseGroupInteractionBox

    val sourceNode by lazy { networkPanel.getNode(synapseGroup.source) as NeuronCollectionNode }
    val targetNode by lazy { networkPanel.getNode(synapseGroup.target) as NeuronCollectionNode }

    init {
        // Note the children pnodes to outlined objects are created in
        // networkpanel and added externally to outlined objects
        interactionBox = SynapseGroupInteractionBox(networkPanel, synapseGroup, this)
        interactionBox.setText(synapseGroup.displayName)
        addChild(interactionBox)
        interactionBox.addPropertyChangeListener { evt ->
            if (evt.propertyName == "width") {
                updateInteractionBoxLocation()
            }
        }
        fun invalidateArrow() {
            when (currentNode) {
                directedNode -> directedNode?.invalidateFullBounds()
                expandedNode -> expandedNode?.invalidateFullBounds()
            }
        }
        synapseGroup.source.events.locationChanged.on(Dispatchers.Swing) {
            invalidateArrow()
            updateInteractionBoxLocation()
        }
        synapseGroup.target.events.locationChanged.on(Dispatchers.Swing) {
            invalidateArrow()
            updateInteractionBoxLocation()
        }
        
        // Listen to individual neuron movements for recurrent groups
        if (synapseGroup.isRecurrent()) {
            synapseGroup.source.neuronList.forEach { neuron ->
                val cleanup = neuron.events.locationChanged.on(Dispatchers.Swing) {
                    updateInteractionBoxLocation()
                }
                synapseGroup.events.deleted.on(Dispatchers.Default) {
                    cleanup.cancel()
                }
            }
        }
        updateInteractionBoxLocation()

        // Handle events
        val events = synapseGroup.events
        events.labelChanged.on(Dispatchers.Swing) { _, _ -> updateText() }
        events.visibilityChanged.on(Dispatchers.Swing) { setVisibility() }
        events.synapseRemoved.on(dispatcher = Dispatchers.Swing) {
            setVisibility()
        }
        events.synapseListChanged.on(dispatcher = Dispatchers.Swing) {
            setVisibility()
        }
        setVisibility()
        interactionBox.invalidateFullBounds()

        // call once to make sure all the actions are registered
        interactionBox.contextMenu
    }

    private fun removeEverythingButInteractionBox() {
        removeChild(directedNode)
        removeChild(expandedNode)
    }

    fun updateInteractionBoxLocation() {
        val (x, y) = ((synapseGroup.target.location - synapseGroup.source.location) / 2) + synapseGroup.source.location
        interactionBox.centerFullBoundsOnPoint(x, y)

        // Only apply smart positioning logic when synapses are visible (expanded mode)
        // When in directed mode (arrow only), position should stay on the arrow
        if (synapseGroup.isRecurrent() && synapseGroup.displaySynapses) {
            // Use source node since source == target for recurrent groups
            val neurons = sourceNode.neuronNodes
            val ibBounds = interactionBox.globalFullBounds
            val overlapsNeuron = neurons.any { neuronNode -> ibBounds.intersects(neuronNode.globalFullBounds)}

            if (overlapsNeuron) {
                val outlineBounds = sourceNode.outlinedObjects.fullBounds
                val bx = outlineBounds.centerX
                val by = outlineBounds.maxY + interactionBox.fullBounds.height / 2
                interactionBox.centerFullBoundsOnPoint(bx, by)
            }
        }
    }

    private fun setVisibility() {
        if (synapseGroup.displaySynapses) {
            removeEverythingButInteractionBox()
            if (expandedNode == null) {
                expandedNode = SynapseGroupNodeExpanded(networkPanel, this)
            }
            addChild(expandedNode)
            interactionBox.raiseAbove(expandedNode)
            currentNode = expandedNode
        } else {
            removeEverythingButInteractionBox()
            if (directedNode == null) {
                directedNode = SynapseGroupNodeDirected(this)
            }
            addChild(directedNode)
            interactionBox.raiseAbove(directedNode)
            currentNode = directedNode
        }
    }

    /**
     * Update the text in the interaction box.
     */
    fun updateText() {
        interactionBox.setText(synapseGroup.displayName)
    }

    override fun refreshTheme() {
        directedNode?.updateColorFromPreferences()
    }

    override val isDraggable: Boolean = false

    override val model: SynapseGroup
        get() = synapseGroup

    /**
     * Interface for all PNodes used in as the main representation for a synapse group.
     */
    interface Arrow {
        fun layoutChildren()
    }

}