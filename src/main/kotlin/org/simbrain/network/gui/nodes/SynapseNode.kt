package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PBounds
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.core.Synapse
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.createSynapseContextMenu
import org.simbrain.network.gui.createTooltipText
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.gui.dialogs.NetworkPreferences.excitatorySynapseColor
import org.simbrain.network.gui.dialogs.NetworkPreferences.inhibitorySynapseColor
import org.simbrain.network.gui.dialogs.NetworkPreferences.maxWeightSize
import org.simbrain.network.gui.dialogs.NetworkPreferences.minWeightSize
import org.simbrain.network.gui.dialogs.NetworkPreferences.spikingColor
import org.simbrain.network.gui.dialogs.synapse.SynapseDialog
import org.simbrain.util.*
import java.awt.Color
import java.awt.geom.Arc2D
import java.awt.geom.Area
import java.awt.geom.Line2D
import java.awt.geom.Point2D
import javax.swing.JPopupMenu
import kotlin.math.*

/**
 *  Piccolo representation of [Synapse]
 */
class SynapseNode(
    net: NetworkPanel?,
    var source: NeuronNode,
    var target: NeuronNode,
    var synapse: Synapse
) : ScreenElement(net!!) {

    /**
     * Location of circle relative to target node.
     */
    private val offset = 7.0

    /**
     * Main circle of the synapse.
     */
    private var circle: PPath? = null

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

        events.strengthUpdated.on(dispatcher = Dispatchers.Swing) {
            updateColor()
            updateDiameter()
        }
        events.colorPreferencesChanged.on(dispatcher = Dispatchers.Swing) {
            updateColor()
            updateDiameter()
            updateSpikeColor()
        }
        events.visbilityChanged.on(dispatcher = Dispatchers.Swing) { _, newVisibility ->
            updateVisibility(newVisibility) }
        updateVisibility(synapse.isVisible)
        events.clampChanged.on(dispatcher = Dispatchers.Swing) { this.updateClampStatus() }
        updateClampStatus()

        events.locationChanged.on(dispatcher = Dispatchers.Swing) { this.updatePosition() }

        // Respond to spiking events
        source.neuron.events.spiked.on(dispatcher = Dispatchers.Swing) {
            updateSpikeColor()
            // If spiking-only mode is on, adjust visibility in response to spike changes
            if (networkPanel.synapseSpikingOnlyVisible) {
                applySpikingOnlyVisibility()
            }
        }

        // Respond to global toggles for spiking-only visibility
        networkPanel.network.events.synapseSpikingOnlyVisibilityChanged.on(dispatcher = Dispatchers.Swing) {
            applySpikingOnlyVisibility()
        }
    }


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


    fun updateClampStatus() {
        if (synapse.clamped) {
            circle!!.strokePaint = NetworkTheme.current.nodeOutline
        } else {
            circle!!.strokePaint = null
        }
    }

    override fun refreshTheme() {
        super.refreshTheme()
        updateClampStatus()
    }

    val isSelfConnection: Boolean
        get() = (source.neuron == target.neuron)

    /**
     * Create the line depending on whether this is self connected or not.
     *
     * @param center the center of the synapse
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
     * Apply combined logic for visibility:
     * - If the model's own visibility is false, this node must remain hidden.
     * - If spiking-only mode is enabled, only show while the source neuron is spiking.
     * - Otherwise reflect the model's own visibility.
     */
    private fun applySpikingOnlyVisibility() {
        // Old synapse visibility has higher priority
        if (!synapse.isVisible) {
            super.setVisible(false)
            return
        }
        if (networkPanel.synapseSpikingOnlyVisible) {
            val show = with(networkPanel.network) { source.neuron.isSpike }
            super.setVisible(show)
        } else {
            super.setVisible(true)
        }
    }

    private fun updateVisibility(modelVisibility: Boolean) {
        if (!modelVisibility) {
            super.setVisible(false)
            return
        }
        if (networkPanel.synapseSpikingOnlyVisible) {
            applySpikingOnlyVisibility()
        } else {
            super.setVisible(true)
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

        circle!!.width = diameter
        circle!!.height = diameter
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

        val neuronOffset = NEURON_DIAMETER / 2

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
        get() = false


    protected fun hasToolTipText(): Boolean {
        return true
    }

    override val toolTipText: String
        get() = createTooltipText(synapse) {
            synapse.toString()
        }

    override val contextMenu: JPopupMenu
        get() = networkPanel.createSynapseContextMenu(synapse)

    override fun createEditDialog(): StandardDialog? {
        return SynapseDialog.createSynapseDialog(networkPanel.selectionManager.filterSelectedModels<Synapse>())
    }

    override val propertyDialog: StandardDialog? get() = createEditDialog()

    override fun toString(): String {
        var ret = ""
        ret += "SynapseNode: (" + this.globalFullBounds.x + ")(" + globalFullBounds.y + ")\n"
        return ret
    }

    override val model: Synapse
        get() = synapse


    override fun paintAfterChildren(paintContext: PPaintContext) {
        super.paintAfterChildren(paintContext)
        if (!NetworkPreferences.showSynapseStrengthLabels) return
        if (isSelfConnection) return

        val diameter = circle?.width ?: return
        if (diameter * networkPanel.scalingFactor < NetworkPreferences.synapseStrengthLabelMinScreenSize) return

        // Visible center of the circle in this node's local coords (fullBounds includes circle.offset).
        val centerRef = circle!!.fullBoundsReference
        val circleCx = centerRef.centerX
        val circleCy = centerRef.centerY

        // The synapse circle sits on the target neuron's perimeter, so the half toward the target is
        // covered. Offset the label toward the source (visible half). Direction is preserved under
        // translation, so we can compute it directly from the global neuron locations.
        val dx = source.neuron.x - target.neuron.x
        val dy = source.neuron.y - target.neuron.y
        val mag = sqrt(dx * dx + dy * dy)
        if (mag < 1e-6) return
        val labelX = circleCx + (dx / mag) * (diameter / 4)
        val labelY = circleCy + (dy / mag) * (diameter / 4)

        val decimals = NetworkPreferences.synapseStrengthDecimalPlaces
        val text = synapse.strength.format(decimals)
        val refString = "-9." + "9".repeat(decimals)
        val g2 = paintContext.graphics
        val font = computeCellFont(diameter * 0.6, diameter * 0.3, refString, g2.fontRenderContext)
        g2.drawCenteredOutlinedLabel(text, font, labelX, labelY)
    }

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

        var excitatoryColor: Color = excitatorySynapseColor

        var inhibitoryColor: Color = inhibitorySynapseColor

        var zeroWeightColor: Color = NetworkPreferences.zeroWeightColor

        var maxDiameter: Int = maxWeightSize

        var minDiameter: Int = minWeightSize

        var lineColor: Color = NetworkPreferences.connectionLineColor
    }
}