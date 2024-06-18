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
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PBounds
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.Synapse
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.dialogs.NetworkPreferences.excitatorySynapseColor
import org.simbrain.network.gui.dialogs.NetworkPreferences.inhibitorySynapseColor
import org.simbrain.network.gui.dialogs.NetworkPreferences.lineColor
import org.simbrain.network.gui.dialogs.NetworkPreferences.maxWeightSize
import org.simbrain.network.gui.dialogs.NetworkPreferences.minWeightSize
import org.simbrain.network.gui.dialogs.NetworkPreferences.zeroWeightColor
import org.simbrain.network.gui.nodes.NeuronNode.Companion.spikingColor
import org.simbrain.network.gui.synapseContextMenu
import org.simbrain.network.gui.synapseDialog
import java.awt.Color
import java.awt.geom.Arc2D
import java.awt.geom.Area
import java.awt.geom.Line2D
import java.awt.geom.Point2D
import javax.swing.JDialog
import javax.swing.JPopupMenu
import kotlin.math.abs
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.sin

/**
 * **SynapseNode** is a Piccolo PNode corresponding to a Neuron in the neural
 * network model.
 */
class SynapseNode(
    net: NetworkPanel?,
    /**
     * Reference to source neuron.
     */
    var source: NeuronNode,
    /**
     * Reference to target neuron.
     */
    var target: NeuronNode,
    /**
     * The logical synapse this screen element represents.
     */
    var synapse: Synapse
) : ScreenElement(net!!) {
    /**
     * @return Returns the synapse.
     */
    /**
     * @param synapse The synapse to set.
     */

    /**
     * Location of circle relative to target node.
     */
    private val offset = 7.0

    /**
     * Main circle of synapse.
     */
    private var circle: PPath? = null

    /**
     * @return the line
     */
    /**
     * Line connecting nodes. More of a loop for self connections.
     */
    var line: Float? = null
        private set

    /**
     * The line for bound checking.
     */
    val lineBound: Line2D.Float = Line2D.Float()

    /**
     * The arc for bound checking when the synapse is a self connection.
     */
    var arcBound: Arc2D.Float = Arc2D.Float()
        private set

    /**
     * @return Returns the source.
     */
    /**
     * @param source The source to set.
     */

    /**
     * @return Returns the target.
     */
    /**
     * @param target The target to set.
     */

    /**
     * Create a new synapse node connecting a source and target neuron.
     *
     * @param net     Reference to NetworkPanel
     * @param source  source neuronnode
     * @param target  target neuronmode
     * @param synapse the model synapse this PNode represents
     */
    init {
        updatePosition()
        this.addChild(circle)
        this.addChild(line)
        line!!.strokePaint = lineColor
        line!!.lowerToBottom()
        line!!.paint = null

        updateColor()
        updateDiameter()

        pickable = true
        circle!!.pickable = true
        line!!.pickable = false

        val events = synapse.events

        events.deleted.on(dispatcher = Dispatchers.Swing) { s: NetworkModel? -> removeFromParent() }
        events.strengthUpdated.on(dispatcher = Dispatchers.Swing) {
            updateColor()
            updateDiameter()
        }
        events.colorPreferencesChanged.on(dispatcher = Dispatchers.Swing) {
            updateColor()
            updateDiameter()
            updateSpikeColor()
        }
        events.visbilityChanged.on(dispatcher = Dispatchers.Swing) { _, newVisibility -> visible = newVisibility }
        visible = synapse.isVisible
        events.clampChanged.on { this.updateClampStatus() }
        updateClampStatus()

        events.locationChanged.on(dispatcher = Dispatchers.Swing) { this.updatePosition() }

        // Respond to spiking events
        source.neuron.events.spiked.on(dispatcher = Dispatchers.Swing) { updateSpikeColor() }
    }

    /**
     * Update position of synapse.
     */
    fun updatePosition() {
        // Position the synapse
        val synapseCenter = if (isSelfConnection) {
            globalToLocal(Point2D.Double(target.neuron.x + offset + 3, target.neuron.y + offset + 3))
        } else {
            globalToLocal(calcCenter(source.neuron.location, target.neuron.location))
        }
        this.offset(synapseCenter.x - offset, synapseCenter.y - offset)

        // Create the circle
        if (circle == null) {
            circle = createEllipse(0f, 0f, offset.toFloat() * 2, offset.toFloat() * 2)
            circle!!.strokePaint = null
        }
        val ea = 1.0
        val ea2 = ea * 2
        val tempBounds = circle!!.fullBounds
        setBounds(tempBounds.x - ea, tempBounds.y - ea, tempBounds.width + ea2, tempBounds.height + ea2)

        // Create the line
        if (line == null) {
            line = createLine(globalToLocal(synapseCenter))
        }

        // Update the line (unless it's a self connection)
        if (!isSelfConnection) {
            line!!.reset()
            line!!.append(Line2D.Double(globalToLocal(source.neuron.location), synapseCenter), false)
            lineBound.setLine(source.neuron.location, localToGlobal(synapseCenter))
        } else {
            arcBound =
                Arc2D.Float(
                    globalBounds.getX().toFloat(),
                    globalBounds.getY().toFloat() - 7,
                    22f,
                    15f,
                    1f,
                    355f,
                    Arc2D.OPEN
                )
        }
    }

    /**
     * of a synapse based on whether it is clamped or not.
     */
    fun updateClampStatus() {
        if (synapse.frozen) {
            circle!!.strokePaint = Color.black
        } else {
            circle!!.strokePaint = null
        }
    }

    val isSelfConnection: Boolean
        /**
         * Whether this synapse connects a neuron to itself or not.
         *
         * @return true if this synapse connects a neuron to itself.
         */
        get() = (source.neuron == target.neuron)

    /**
     * Create the line depending on whether this is self connected or not.
     *
     * @param center the center of the synapse
     * @return the line
     */
    private fun createLine(center: Point2D): Float {
        return if (isSelfConnection) {
            Float(Arc2D.Float(x.toFloat(), y.toFloat() - 7, 22f, 15f, 1f, 355f, Arc2D.OPEN))
        } else {
            Float(Line2D.Float(globalToLocal(source.center), center))
        }
    }

    /**
     * Calculates the color for a weight, based on its current strength.
     * Positive values are (for example) red, negative values blue.
     */
    fun updateColor() {
        if (synapse.strength < 0) {
            circle!!.paint = inhibitoryColor
        } else if (synapse.strength == 0.0) {
            circle!!.paint = zeroWeightColor
        } else {
            circle!!.paint = excitatoryColor
        }
    }

    /**
     * When spiking change the color of the line.
     */
    private fun updateSpikeColor() {
        if (with(networkPanel.network) { source.neuron.isSpike }) {
            line!!.strokePaint = spikingColor
        } else {
            line!!.strokePaint = lineColor
        }
    }

    /**
     * Update the diameter of the drawn weight based on the logical weight's
     * strength.
     */
    fun updateDiameter() {
        val diameter: kotlin.Double

        var upperBound = synapse.upperBound
        var lowerBound = synapse.lowerBound
        var strength = synapse.strength

        // If upper or lower bound are set to zero use a proxy to prevent
        // division errors
        if (upperBound == 0.0) {
            upperBound = ZERO_PROXY
        }
        if (lowerBound == 0.0) {
            lowerBound = ZERO_PROXY
        }

        // If strength is out of bounds (which is allowed in the model), set it
        // to those bounds for the
        // sake of the GUI representation
        if (strength < lowerBound) {
            strength = lowerBound
        }
        if (strength > upperBound) {
            strength = upperBound
        }

        diameter = if (synapse.strength == 0.0) {
            minDiameter.toDouble()
        } else if (synapse.strength > 0) {
            (maxDiameter - minDiameter) * (strength / upperBound) + minDiameter
        } else {
            ((maxDiameter - minDiameter) * (abs(
                strength / lowerBound
            ))) + minDiameter
        }

        val delta = (circle!!.bounds.getWidth() - diameter) / 2

        circle!!.setWidth(diameter)
        circle!!.setHeight(diameter)
        // offset properly moves circle, but this is not reflected in bounds
        circle!!.offset(delta, delta)
        setBounds(circle!!.fullBounds)
    }

    /**
     * Calculates the position of the synapse circle based on the positions of
     * the source and target NeuronNodes.
     *
     * @param src Source NeuronNode
     * @param tar Target NeuronNode
     * @return the appropriate position for the synapse circle
     */
    fun calcCenter(src: Point2D, tar: Point2D): Point2D {
        val sourceX = src.x
        val sourceY = src.y
        val targetX = tar.x
        val targetY = tar.y

        if (sourceX == targetX && sourceY == targetY) {
            return Point2D.Double(0.0, 0.0)
        }

        val x = abs(sourceX - targetX)
        val y = abs(sourceY - targetY)
        val alpha = atan(y / x)

        var weightX = 0.0
        var weightY = 0.0

        val neuronOffset = NeuronNode.DIAMETER / 2

        weightX = if (sourceX < targetX) {
            targetX - (neuronOffset * cos(alpha))
        } else {
            targetX + (neuronOffset * cos(alpha))
        }

        weightY = if (sourceY < targetY) {
            targetY - (neuronOffset * sin(alpha))
        } else {
            targetY + (neuronOffset * sin(alpha))
        }

        return Point2D.Double(weightX, weightY)
    }

    override val isDraggable: Boolean
        /**
         * @return
         * @see ScreenElement
         */
        get() = false

    /**
     * @see ScreenElement
     */
    protected fun hasToolTipText(): Boolean {
        return true
    }

    override val toolTipText: String
        get() = synapse.toolTipText.toString()

    override val contextMenu: JPopupMenu
        get() = // JPopupMenu contextMenu = new JPopupMenu();
        //
        // contextMenu.add(new CutAction(getNetworkPanel()));
        // contextMenu.add(new CopyAction(getNetworkPanel()));
        // contextMenu.add(new PasteAction(getNetworkPanel()));
        // contextMenu.addSeparator();
        //
        // contextMenu.add(new DeleteAction(getNetworkPanel()));
        // contextMenu.addSeparator();
        //
        // contextMenu.add(getNetworkPanel().getActionManager().getNeuronCollectionAction());
        // contextMenu.addSeparator();
        //
        // // Workspace workspace = getNetworkPanel().getWorkspace();
        // // if (workspace.getGaugeList().size() > 0) {
        // // contextMenu.add(workspace.getGaugeMenu(getNetworkPanel()));
        // // contextMenu.addSeparator();
        // // }
        //
        // contextMenu.add(new SetSynapsePropertiesAction(getNetworkPanel()));
        //
            // return contextMenu;
            networkPanel.synapseContextMenu

    override val propertyDialog: JDialog
        get() = networkPanel.synapseDialog

    /**
     * Returns String representation of this NeuronNode.
     *
     * @return String representation of this node.
     */
    override fun toString(): String {
        var ret = ""
        ret += "SynapseNode: (" + this.globalFullBounds.x + ")(" + globalFullBounds.y + ")\n"
        return ret
    }

    override val model: Synapse
        get() = synapse


    override fun isIntersecting(bound: PBounds?): Boolean {
        if (isSelfConnection) {
            val boundArea = Area(bound)
            boundArea.intersect(Area((arcBound)))
            return !boundArea.isEmpty
        } else {
            return bound!!.intersectsLine(lineBound)
        }
    }

    companion object {
        /**
         * Used to approximate zero to prevent divide-by-zero errors.
         */
        private const val ZERO_PROXY = .001

        /**
         * @return the excitatoryColor
         */
        /**
         * @param excitatoryColor the excitatoryColor to set
         */
        /**
         * Color of "excitatory" synapses, with positive values.
         */
        var excitatoryColor: Color = excitatorySynapseColor

        /**
         * @return the inhibitoryColor
         */
        /**
         * @param inhibitoryColor the inhibitoryColor to set
         */
        /**
         * Color of "inhibitory" synapses, with negative values.
         */
        var inhibitoryColor: Color = inhibitorySynapseColor

        /**
         * @return the zeroWeightColor
         */
        /**
         * @param zeroWeightColor the zeroWeightColor to set
         */
        /**
         * Color of "zero" weights.
         */
        var zeroWeightColor: Color = NetworkPreferences.zeroWeightColor

        /**
         * @return the maxDiameter
         */
        /**
         * @param maxDiameter the maxDiameter to set
         */
        /**
         * Maximum diameter of the circle representing the synapse.
         */
        var maxDiameter: Int = maxWeightSize

        /**
         * @return the minDiameter
         */
        /**
         * @param minDiameter the minDiameter to set
         */
        /**
         * Minimum diameter of the circle representing the synapse.
         */
        var minDiameter: Int = minWeightSize

        /**
         * @return the lineColor
         */
        /**
         * @param lineColor the lineColor to set
         */
        /**
         * Color of lines in synapse representation. Also used for node representation.
         */
        var lineColor: Color = NetworkPreferences.lineColor
    }
}