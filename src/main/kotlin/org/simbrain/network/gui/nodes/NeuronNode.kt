/**
 * The canvas node for a [Neuron]. Activation and spike changes, which arrive every iteration, only mark the node
 * dirty; the panel's per-frame sync pass draws the latest state. Rarer changes (label, clamp, location, rule, color)
 * update directly on the EDT.
 */
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
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JPopupMenu

class NeuronNode(net: NetworkPanel, val neuron: Neuron) : ScreenElement(net) {

    private val neuronCircleNode = NeuronCircleNode(net).also { addChild(it) }

    /** Set when a spike fires, consumed by the next sync, so a spike shorter than a frame is still drawn. */
    private val spikeSinceLastSync = AtomicBoolean(false)

    init {
        updateShape()
        updateActivation()
        updatePolarity()
        updateTextLabel()
        updateClampStatus()
        pullViewPositionFromModel()
        pickable = true

        val events = neuron.events
        // Activation and spikes change every iteration: they only mark the node, and the panel's sync pass draws the
        // latest state once per frame
        events.activationChanged.on(Dispatchers.Unconfined) { _, _ -> markDirty(ACTIVATION) }.untilDisposed()
        events.spiked.on(Dispatchers.Unconfined) { spiking ->
            // A spike can start and end between two frames; remember it so the next frame still shows it
            if (spiking) spikeSinceLastSync.set(true)
            markDirty(SPIKE)
        }.untilDisposed()
        events.colorChanged.on(Dispatchers.Swing) { updatePolarity() }.untilDisposed()
        events.labelChanged.on(Dispatchers.Swing) { _, _ ->
            updateTextLabel()
            networkPanel.network.events.zoomToFitPage.fire()
        }.untilDisposed()
        events.clampChanged.on(Dispatchers.Swing)  { updateClampStatus() }.untilDisposed()
        events.locationChanged.on(Dispatchers.Swing) { pullViewPositionFromModel() }.untilDisposed()
        events.updateRuleChanged.on(Dispatchers.Swing) { _, _ -> updateShape() }.untilDisposed()
    }

    /** The activation and spike state currently drawn, which lag the model until the next sync. */
    internal val drawnActivation get() = neuronCircleNode.activation
    internal val drawnSpiking get() = neuronCircleNode.isSpiking

    override fun syncFromModel(bits: Int) {
        if (bits and SPIKE != 0) {
            val spiking = spikeSinceLastSync.getAndSet(false) || with(networkPanel.network) { neuron.isSpike }
            if (neuronCircleNode.isSpiking != spiking) {
                neuronCircleNode.isSpiking = spiking
            }
        }
        if (bits and ACTIVATION != 0) {
            val bounds = neuron.updateRule.graphicalBounds
            if (neuron.activation != neuronCircleNode.activation || bounds != neuronCircleNode.graphicalBounds) {
                updateActivation()
            }
            updatePolarity()
        }
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

    companion object {
        private const val ACTIVATION = 1
        private const val SPIKE = 2
    }

    override fun refreshTheme() {
        updatePolarity()
        updateActivation()
        updateTextLabel()
    }

}