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

import org.piccolo2d.PNode
import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.simbrain.network.NetworkModel
import org.simbrain.network.core.Neuron
import org.simbrain.network.core.NeuronUpdateRule
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.neuronContextMenu
import org.simbrain.network.gui.neuronDialog
import org.simbrain.network.neuron_update_rules.interfaces.ActivityGenerator
import org.simbrain.util.SimbrainConstants
import org.simbrain.util.Utils
import org.simbrain.util.math.SimbrainMath
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.geom.Point2D
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.JDialog
import javax.swing.JPopupMenu

/**
 * **NeuronNode** is a Piccolo PNode corresponding to a Neuron in the neural
 * network model.
 */
class NeuronNode(net: NetworkPanel?, val neuron: Neuron) : ScreenElement(net), PropertyChangeListener {

    private val mainShape: PPath
        get() = if (neuron is ActivityGenerator) square else circle

    /**
     * Circle shape for representing neurons.
     */
    private val circle = createEllipse(
        (0 - DIAMETER / 2).toFloat(),
        (0 - DIAMETER / 2).toFloat(),
        DIAMETER.toFloat(),
        DIAMETER.toFloat()
    )

    /**
     * Square shape for representing activity generators.
     */
    private val square = createRectangle(
        (0 - DIAMETER / 2).toFloat(),
        (0 - DIAMETER / 2).toFloat(),
        DIAMETER.toFloat(),
        DIAMETER.toFloat()
    )

    /**
     * A list of SynapseNodes connected to this NeuronNode; used for updating.
     */
    private val connectedSynapses = HashSet<SynapseNode>()

    /**
     * Number text inside neuron.
     */
    private val activationText = PText()

    /**
     * Text corresponding to neuron's (optional) label.
     */
    private val labelText = PText()

    /**
     * Text corresponding to neuron's update priority.
     */
    private val priorityText = PText()

    /**
     * Background for label text, so that background objects don't show up.
     */
    private val labelBackground = PNode()

    /**
     * Whether text should be visible (when zoomed out, it should be
     * invisible).
     */
    private var currentTextVisibility = false

    /**
     * If true then a custom color is being used for stroke.
     */
    private var customStrokeColor = false

    /**
     * Create a new neuron node.
     *
     * @param net    Reference to NetworkPanel
     * @param neuron reference to model neuron
     */
    init {

        // Set up label text
        //priorityText.setFont(PRIORITY_FONT);
        labelBackground.paint = networkPanel.background
        labelBackground.setBounds(labelText.bounds)
        labelBackground.addChild(labelText)
        addChild(labelBackground)

        // Set graphics of node based on neuron propertiess
        updateShape()
        updateColor()
        updateText()
        updateTextLabel()
        updateClampStatus()
        centerFullBoundsOnPoint(neuron.x, neuron.y)
        pickable = true
        addPropertyChangeListener(PROPERTY_FULL_BOUNDS, this)

        // Handle events
        val events = neuron.events
        events.onDeleted { n: NetworkModel? -> removeFromParent() }
        events.onActivationChange { o: kotlin.Double?, n: kotlin.Double? ->
            updateColor()
            updateText()
        }
        events.onSpiked { updateSpikeColor() }
        events.onColorChange { updateColor() }
        events.onLabelChange { _, _ ->
            updateTextLabel()
            networkPanel.zoomToFitPage()
        }
        events.onClampChanged { updateClampStatus() }
        events.onLocationChange { pullViewPositionFromModel() }
        events.onUpdateRuleChange { _, _ -> updateShape() }
    }

    /**
     * Update the shape (square or circle) of the neuron based on whether it's an activity generator or not.
     */
    private fun updateShape() {
        if (neuron.updateRule is ActivityGenerator) {
            removeChild(circle)
            addChild(square)
        } else {
            removeChild(square)
            addChild(circle)
        }
        mainShape.lowerToBottom()
    }

    /**
     * Update the stroke of a node based on whether it is clamped or not.
     */
    private fun updateClampStatus() {
        if (customStrokeColor) {
            return
        }
        if (neuron.isClamped) {
            circle.stroke = CLAMPED_STROKE
        } else {
            circle.stroke = DEFAULT_STROKE
        }
    }

    /**
     * Determine what font to use for this neuron based in its activation level.
     * TODO: Redo by scaling the text object.
     */
    private fun updateText() {
        if (!currentTextVisibility) {
            return
        }
        // Todo: a bit of a performance drain.
        val act = neuron.activation
        activationText.scale = 1.0
        setActivationTextPosition()
        priorityText.scale = 1.0
        setPriorityTextPosition()
        priorityText.text = "" + neuron.updatePriority // todo: respond
        // to listener
        if (java.lang.Double.isNaN(neuron.activation)) {
            activationText.text = "NaN"
            activationText.scale(.7)
            activationText.translate(-4.0, 3.0)
        } else if (act > 0 && neuron.activation < 1) { // Between 0 and
            // 1
            activationText.font = NEURON_FONT_BOLD
            var text = Utils.round(act, 1)
            if (text.startsWith("0.")) {
                text = text.replace("0.".toRegex(), ".")
                if (text == ".0") {
                    text = "0"
                }
            } else {
                text = text.replace(".0$".toRegex(), "")
            }
            activationText.text = text
        } else if (act > -1 && act < 0) { // Between -1 and 0
            activationText.font = NEURON_FONT_BOLD
            activationText.text =
                Utils.round(act, 1).replace("^-0*".toRegex(), "-").replace(".0$".toRegex(), "")
        } else {
            // greater than 1 or less than -1
            activationText.font = NEURON_FONT_BOLD
            if (Math.abs(act) < 10) {
                activationText.scale(.9)
            } else if (Math.abs(act) < 100) {
                activationText.scale(.8)
                activationText.translate(1.0, 1.0)
            } else {
                activationText.scale(.7)
                activationText.translate(-1.0, 2.0)
            }
            activationText.text = Math.round(act).toInt().toString()
        }
    }

    /**
     * Update the visibility of all text nodes depending on view scale. When
     * "zoomed in" show all text; when zoomed out, don't.
     */
    fun updateTextVisibility() {
        val scale = networkPanel.canvas.camera.viewScale
        if (scale > TEXT_VISIBILITY_THRESHOLD) {
            if (!currentTextVisibility) {
                setDisplayText(true)
                currentTextVisibility = true
            }
            updateText()
            updateTextLabel()
        } else {
            if (currentTextVisibility) {
                setDisplayText(false)
                currentTextVisibility = false
            }
        }
    }

    /**
     * Support text visibility toggling by adding and removing text pnodes.
     *
     * @param displayText whether text should be displayed or not.
     */
    private fun setDisplayText(displayText: Boolean) {
        if (displayText) {
            addChild(activationText)
            addChild(labelBackground)
            setPriorityView(networkPanel.prioritiesVisible)
            resetToDefault()
            updateText()
            updateTextLabel()
        } else {
            removeChild(activationText)
            removeChild(labelText)
            removeChild(priorityText)
            removeChild(labelBackground)
        }
    }

    /**
     * Sets the color of this neuron based on its activation level.
     */
    private fun updateColor() {
        val activation = neuron.updateRule.getGraphicalValue(neuron)
        // Force to blank if 0 (or close to it)
        val gLow = neuron.updateRule.graphicalLowerBound
        val gUp = neuron.updateRule.graphicalUpperBound

        // A "graphical zero point" that shows as white
        var gZeroPoint = 0.0
        if (NeuronUpdateRule.usesCustomZeroPoint(neuron.updateRule)) {
            // Current custom choice is between upper and lower bounds.
            // For example useful to capture whether a biological neuron is
            // depolarized or hyperpolarized
            gZeroPoint = (gUp - gLow) / 2 + gLow
        }
        if (Math.abs(activation - gZeroPoint) < 0.001) {
            mainShape.paint = Color.white
        } else if (activation > gZeroPoint) {
            val saturation = SimbrainMath.rescale(activation, 0.0, gUp, 0.0, 1.0)
            mainShape.paint = Color.getHSBColor(hotColor, saturation.toFloat(), 1f)
        } else if (activation < gZeroPoint) {
            val saturation = SimbrainMath.rescale(activation, 0.0, gLow, 0.0, 1.0)
            mainShape.paint = Color.getHSBColor(coolColor, saturation.toFloat(), 1f)
        }
        if (!customStrokeColor) {

            // Color stroke paint based on Polarity
            if (neuron.polarity === SimbrainConstants.Polarity.EXCITATORY) {
                circle.strokePaint = Color.red
            } else if (neuron.polarity === SimbrainConstants.Polarity.INHIBITORY) {
                circle.strokePaint = Color.blue
            } else {
                circle.strokePaint = DEFAULT_STROKE_PAINT
            }
        }
    }

    /**
     * When spiking change the color of the line around the node.
     */
    private fun updateSpikeColor() {
        if (!customStrokeColor) {
            if (neuron.isSpike) {
                mainShape.strokePaint = spikingColor
                mainShape.paint = spikingColor
            } else {
                // "Erase" the spike color
                // TODO: Interaction with polarity based coloring not tested.
                mainShape.strokePaint = SynapseNode.getLineColor()
            }
        }
    }

    /**
     * Update the text label.
     */
    fun updateTextLabel() {
        if (currentTextVisibility && neuron.label != null) {
            // Set label text
            if (!neuron.label.equals("", ignoreCase = true) || !neuron.label.equals(
                    SimbrainConstants.NULL_STRING,
                    ignoreCase = true
                )
            ) {
                labelText.font = NEURON_FONT
                labelText.text = "" + neuron.label
                labelText.setOffset(mainShape.x - labelText.width / 2 + DIAMETER / 2, mainShape.y - DIAMETER / 2 - 1)
                labelBackground.setBounds(labelText.fullBounds)
            }

            // update bounds to include text
            val bounds = mainShape.bounds
            bounds.add(labelText.localToParent(labelText.bounds))
            setBounds(bounds)
        }
    }

    /**
     * Set basic position of text in the PNode, which is then adjusted depending
     * on the size of the text.
     */
    private fun setActivationTextPosition() {
        activationText.setOffset(
            mainShape.x + DIAMETER / 4 + 2,
            mainShape.y + DIAMETER / 4 + 1
        )
    }

    /**
     * Toggles the visibility of the priority view text label.
     *
     * @param makePriorityTextVisible whether the priority text label should be
     * visible or not
     */
    fun setPriorityView(makePriorityTextVisible: Boolean) {
        if (makePriorityTextVisible) {
            setPriorityTextPosition()
            addChild(priorityText)
        } else {
            priorityText?.let { removeChild(it) }
        }
    }

    /**
     * Set position of priority label.
     */
    private fun setPriorityTextPosition() {
        if (priorityText == null || !currentTextVisibility) {
            return
        }
        priorityText.setOffset(mainShape.bounds.centerX, mainShape.bounds.centerY + DIAMETER - 10)
    }

    /**
     * @return screen element selectable
     * @see ScreenElement
     */
    override fun isSelectable(): Boolean {
        return true
    }

    override fun isDraggable(): Boolean {
        return true
    }

    override fun getToolTipText(): String? {
        var ret: String? = String()
        ret += neuron.toolTipText
        return ret
    }

    /**
     * Return the center of this node (the circle) in global coordinates.
     *
     * @return the center point of this node.
     */
    val center: Point2D
        get() = mainShape.globalBounds.center2D

    override fun getContextMenu(): JPopupMenu {
        return networkPanel.neuronContextMenu
    }

    override fun getPropertyDialog(): JDialog? {
        return networkPanel.neuronDialog
    }

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

    override fun propertyChange(event: PropertyChangeEvent) {
        updateSynapseNodePositions()
    }

    override fun offset(dx: kotlin.Double, dy: kotlin.Double) {
        super.offset(dx, dy)
        pushViewPositionToModel()
    }

    /**
     * Update the position of the model neuron based on the global coordinates
     * of this pnode.
     */
    fun pushViewPositionToModel() {
        val p = this.globalTranslation
        neuron.location = p
    }

    /**
     * Updates the position of the view neuron based on the position of the
     * model neuron.
     */
    fun pullViewPositionFromModel() {
        // This is not necessarily a performance drain.  These updates do not automatically cause the
        // canvas to repaint.  See PRoot#processInputs
        val p: Point2D = Point2D.Double(neuron.x, neuron.y)
        this.globalTranslation = p
    }

    /**
     * @return Connected synapses.
     */
    fun getConnectedSynapses(): Set<SynapseNode> {
        return connectedSynapses
    }

    /**
     * Update connected synapse node positions.
     */
    fun updateSynapseNodePositions() {
        for (synapseNode in connectedSynapses) {
            synapseNode.updatePosition()
        }
    }
    /**
     * @return Returns the xpos.
     */
    /**
     * @param xpos The xpos to set.
     */
    var xpos: kotlin.Double
        get() = this.globalBounds.getX()
        set(xpos) {
            val p: Point2D = Point2D.Double(xpos, ypos)
            globalToLocal(p)
            this.setBounds(p.x, p.y, this.width, this.height)
        }
    /**
     * @return Returns the ypos.
     */
    /**
     * @param ypos The ypos to set.
     */
    var ypos: kotlin.Double
        get() = this.globalBounds.getY()
        set(ypos) {
            val p: Point2D = Point2D.Double(xpos, ypos)
            globalToLocal(p)
            this.setBounds(p.x, p.y, this.width, this.height)
        }

    override fun resetToDefault() {
        if (!customStrokeColor) {
            mainShape!!.strokePaint = SynapseNode.getLineColor()
        }
        // TODO: Check if change only?
        labelBackground.paint = networkPanel.backgroundColor
        updateColor()
    }

    //@Override
//public void setGrouped(final boolean isGrouped) {
//    super.setGrouped(isGrouped);
//    for (SynapseNode synapseNode : connectedSynapses) {
//        synapseNode.setGrouped(isGrouped);
//    }
//}
    override fun getModel(): Neuron {
        return neuron
    }

    /**
     * Set a custom color for the circle stroke (not the fill).
     *
     * @param color Color to use
     */
    fun setCustomStrokeColor(color: Color?) {
        // TODO:Perhaps at some point make it possible to define
        // custom extension of neuron node with custom color schemes
        // This feature hasn't been used much so if it is to stay
        // in the main code it might need some refinement.
        customStrokeColor = true
        circle.strokePaint = color
        // Custom colors more visible with the clamped stroke
        circle.stroke = CLAMPED_STROKE
    }

    fun setUsingCustomStrokeColor(customColor: Boolean) {
        customStrokeColor = customColor
    }

    override fun acceptsSourceHandle(): Boolean {
        return true
    }

    companion object {
        /**
         * Default text visibility threshold.
         */
        private const val TEXT_VISIBILITY_THRESHOLD = 0.5

        /**
         * Diameter of neuron.
         */
        const val DIAMETER = 24

        /**
         * Font for input and output labels.
         */
        val IN_OUT_FONT = Font("Arial", Font.PLAIN, 9)

        /**
         * Heavy stroke for clamped nodes.
         */
        private val CLAMPED_STROKE = BasicStroke(2f)

        /**
         * Neuron Font.
         */
        val NEURON_FONT = Font("Arial", Font.PLAIN, 11)

        /**
         * Priority Font.
         */
        val PRIORITY_FONT = Font("Courier", Font.PLAIN, 9)
        // TODO: These should be replaced with actual scaling of the text object.
        /**
         * Neuron font bold.
         */
        val NEURON_FONT_BOLD = Font("Arial", Font.BOLD, 11)

        /**
         * Neuron font small.
         */
        val NEURON_FONT_SMALL = Font("Arial", Font.PLAIN, 9)

        /**
         * Neuron font very small.
         */
        val NEURON_FONT_VERYSMALL = Font("Arial", Font.PLAIN, 7)

        /**
         * Color of "active" neurons, with positive values.
         */
        @JvmStatic
        var hotColor = Color.RGBtoHSB(255, 0, 0, null)[0]

        /**
         * Color of "inhibited" neurons, with negative values.
         */
        @JvmStatic
        var coolColor = Color.RGBtoHSB(0, 0, 255, null)[0]

        /**
         * Color of "spiking" synapse.
         */
        @JvmStatic
        var spikingColor = Color.yellow
    }
}