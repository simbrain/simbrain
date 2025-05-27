/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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
import java.awt.Dialog
import java.awt.geom.Point2D
import javax.swing.JPopupMenu

/**
 * **NeuronNode** is a Piccolo PNode corresponding to a Neuron in the neural
 * network model.
 */
class NeuronNode(net: NetworkPanel, val neuron: Neuron) : ScreenElement(net) {

    private val neuronCircleNode = NeuronCircleNode(net).also { addChild(it) }

    /**
     * Create a new neuron node.
     */
    init {


        // Set graphics of node based on neuron properties
        updateShape()
        updateActivation()
        updatePolarity()
        updateTextLabel()
        updateClampStatus()
        pullViewPositionFromModel()
        pickable = true

        // Handle events
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
        events.locationChanged.on(wait = true) { pullViewPositionFromModel() }
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
        get() = networkPanel.createNeuronContextMenu()

    override fun createEditDialog(): StandardDialog? {
        return networkPanel.filterSelectedModelByClass<Neuron>().let { neurons ->
            if (neurons.isEmpty()) return null

            NeuronDialog(neurons).apply { modalityType = Dialog.ModalityType.MODELESS }
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