/**
 * Displays a synapse group as a label box with either a single arrow between its two collections or, when its
 * synapses are showing, nothing more, since each synapse then has a node of its own. The arrow is the group's
 * connection drawing, and the panel's connection layout places the label box.
 */
package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.simbrain.network.core.SynapseGroup
import org.simbrain.network.gui.LaidOutConnector
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.util.*
import org.simbrain.util.piccolo.centerOn
import java.awt.geom.Rectangle2D


class SynapseGroupNode(networkPanel: NetworkPanel, val synapseGroup: SynapseGroup) :
    ScreenElement(networkPanel), LaidOutConnector {

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

    /** Holds whichever of the two drawings of the group is in use. */
    override val connectionDrawing = ConnectionDrawing(this)

    /** Whether the group is drawn as a single arrow rather than as its synapses. */
    private val drawnAsArrow get() = !synapseGroup.displaySynapses

    override fun layoutArrow(layers: List<Rectangle2D>) {
        if (drawnAsArrow) directedNode?.layout(layers)
    }

    override fun arrowAffectedBy(layers: List<Rectangle2D>) = drawnAsArrow && directedNode?.affectedBy(layers) == true

    /** The label box, to be placed on the arrow's shaft when the group is drawn as an arrow that has one. */
    override fun drawingToPlace(): ShaftItem? {
        val arrow = directedNode?.takeIf { drawnAsArrow } ?: return null
        val curve = arrow.shaft ?: return null
        return ShaftItem(
            curve,
            interactionBox.asShaftDrawing(),
            ownLayers = listOf(sourceNode.footprint, targetNode.footprint),
            preferred = arrow.labelPosition
        )
    }

    /**
     * Put the label box where [placement] says. Without one it goes on the loop of a group from a collection to
     * itself, or between the two collections when the synapses are showing.
     */
    override fun placeDrawing(placement: ShaftPlacement?): Rectangle2D {
        val offShaft = directedNode?.takeIf { drawnAsArrow }?.labelPoint
        when {
            placement != null -> interactionBox.centerOn(placement.centerX, placement.centerY)
            offShaft != null -> interactionBox.centerOn(offShaft.x, offShaft.y)
            else -> updateInteractionBoxLocation()
        }
        return interactionBox.layoutBoundsCentered
    }

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
                requestLayout()
            }
        }
        synapseGroup.source.events.locationChanged.on(Dispatchers.Swing) { requestLayout() }.untilDisposed()
        synapseGroup.target.events.locationChanged.on(Dispatchers.Swing) { requestLayout() }.untilDisposed()

        // Listen to individual neuron movements for recurrent groups
        if (synapseGroup.isRecurrent()) {
            synapseGroup.source.neuronList.forEach { neuron ->
                neuron.events.locationChanged.on(Dispatchers.Swing) { requestLayout() }.untilDisposed()
            }
        }
        updateInteractionBoxLocation()

        // Handle events
        val events = synapseGroup.events
        events.labelChanged.on(Dispatchers.Swing) { _, _ -> updateText() }.untilDisposed()
        events.visibilityChanged.on(Dispatchers.Swing) { setVisibility() }.untilDisposed()
        events.synapseRemoved.on(dispatcher = Dispatchers.Swing) {
            setVisibility()
        }.untilDisposed()
        events.synapseListChanged.on(dispatcher = Dispatchers.Swing) {
            setVisibility()
        }.untilDisposed()
        setVisibility()
        interactionBox.invalidateFullBounds()

        // call once to make sure all the actions are registered
        interactionBox.contextMenu
    }

    private fun removeBothDrawings() {
        directedNode?.let { connectionDrawing.removeChild(it) }
        expandedNode?.let { connectionDrawing.removeChild(it) }
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
            removeBothDrawings()
            if (expandedNode == null) {
                expandedNode = SynapseGroupNodeExpanded(networkPanel, this)
            }
            connectionDrawing.addChild(expandedNode)
        } else {
            removeBothDrawings()
            if (directedNode == null) {
                directedNode = SynapseGroupNodeDirected(this)
            }
            connectionDrawing.addChild(directedNode)
        }
        requestLayout()
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

    /** The label box. */
    override fun measureFootprint() = footprintOf(interactionBox.layoutBoundsCentered)

    override val model: SynapseGroup
        get() = synapseGroup
}
