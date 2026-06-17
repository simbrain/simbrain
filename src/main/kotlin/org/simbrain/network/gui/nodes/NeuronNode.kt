package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.simbrain.network.core.Neuron
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.createNeuronContextMenu
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.dialogs.neuron.NeuronDialog
import org.simbrain.network.gui.filterSelectedModelByClass
import org.simbrain.network.updaterules.interfaces.ActivityGenerator
import org.simbrain.util.SimbrainConstants
import org.simbrain.util.StandardDialog
import org.simbrain.util.plus
import org.simbrain.util.point
import java.awt.geom.Point2D
import javax.swing.JPopupMenu

/**
 * A Piccolo PNode representation of a [Neuron]
 */
class NeuronNode(net: NetworkPanel, val neuron: Neuron) : ScreenElement(net) {

    private val neuronCircleNode = NeuronCircleNode(net).also { addChild(it) }

    init {
        updateShape()
        updateActivation()
        updatePolarity()
        updateTextLabel()
        updateClampStatus()
        pullViewPositionFromModel()
        pickable = true

        val events = neuron.events
        events.activationChanged.on(Dispatchers.Swing) { _, _ ->
            updateActivation()
            updatePolarity()
        }
        events.spiked.on(Dispatchers.Swing) { updateSpikeColor() }
        events.colorChanged.on(Dispatchers.Swing) { updatePolarity() }
        events.labelChanged.on(Dispatchers.Swing) { _, _ ->
            updateTextLabel()
            networkPanel.network.events.zoomToFitPage.fire()
        }
        events.clampChanged.on(Dispatchers.Swing)  { updateClampStatus() }
        events.locationChanged.on(Dispatchers.Swing) { pullViewPositionFromModel() }
        events.updateRuleChanged.on(Dispatchers.Swing) { _, _ -> updateShape() }
    }

    /**
     * Update the shape (square or circle) of the neuron based on whether it's an activity generator or not.
     */
    private fun updateShape() {
        neuronCircleNode.useSquare = neuron.updateRule is ActivityGenerator
    }

    /**
     * Update the stroke of a node based on whether it is clamped or not.
     */
    private fun updateClampStatus() {
        neuronCircleNode.setClamped(neuron.clamped)
    }

    private fun updateActivation() {
        neuronCircleNode.drawActivation(neuron.activation, neuron.updateRule.graphicalBounds)
    }

    fun forceUpdateActivationText() {
        neuronCircleNode.forceUpdateActivationText()
    }

    private fun updatePolarity() {
        neuronCircleNode.customStrokeColor = when (neuron.polarity) {
            SimbrainConstants.Polarity.EXCITATORY -> NetworkPreferences.hotNodeColor
            SimbrainConstants.Polarity.INHIBITORY -> NetworkPreferences.coolNodeColor
            else -> null
        }
    }

    /**
     * When spiking change the color of the line around the node.
     */
    private fun updateSpikeColor() {
        neuronCircleNode.isSpiking = with(networkPanel.network) { neuron.isSpike }
    }

    /**
     * Update the text label.
     */
    fun updateTextLabel() {
        neuronCircleNode.setLabel(neuron.label)
    }

    override val isDraggable: Boolean = true

    override val toolTipText: String?
        get() = neuron.toolTipText

    /**
     * Return the center of this node (the circle or square) in global coordinates.
     *
     * @return the center point of this node.
     */
    val center: Point2D
        get() = neuron.location

    override val contextMenu: JPopupMenu
        get() = networkPanel.createNeuronContextMenu(neuron)

    override fun createEditDialog(): StandardDialog? {
        return networkPanel.filterSelectedModelByClass<Neuron>().let { neurons ->
            if (neurons.isEmpty()) return null

            NeuronDialog(neurons)
        }
    }

    override val propertyDialog get() = createEditDialog()

    /**
     * Returns String representation of this NeuronNode.
     *
     * @return String representation of this node.
     */
    override fun toString(): String {
        var ret = String()
        ret += """
             NeuronNode: (${this.globalFullBounds.x})(${globalFullBounds.y})
             
             """.trimIndent()
        return ret
    }

    protected fun createContextMenu(): JPopupMenu {
        return contextMenu
    }

    override fun offset(dx: kotlin.Double, dy: kotlin.Double) {
        neuron.location += point(dx, dy)
        pullViewPositionFromModel()
    }

    /**
     * Updates the position of the view neuron based on the position of the
     * model neuron.
     */
    fun pullViewPositionFromModel() {
        // This is not necessarily a performance drain.  These updates do not automatically cause the
        // canvas to repaint.  See PRoot#processInputs
        val p = neuron.location
        this.globalTranslation = p
        setBounds(neuronCircleNode.bounds)
    }

    override val model: Neuron
        get() = neuron

    override fun acceptsSourceHandle(): Boolean {
        return true
    }

}