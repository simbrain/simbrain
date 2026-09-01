/**
 * Piccolo node for a [GapJunction]: a line between the two endpoint neurons broken at its midpoint by a
 * paired-bars channel glyph, with no arrowheads since the junction has no direction. Bows into a slight
 * curve when a chemical synapse also connects the pair, and renders dashed when the junction is inert
 * because an endpoint rule exposes no membrane potential.
 */
package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import org.piccolo2d.PNode
import org.piccolo2d.nodes.PPath
import org.piccolo2d.util.PBounds
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.core.GapJunction
import org.simbrain.network.core.NetworkModel
import org.simbrain.network.core.Synapse
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.createTooltipText
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.StandardDialog
import org.simbrain.util.computeCellFont
import org.simbrain.util.createAction
import org.simbrain.util.createEditorDialog
import org.simbrain.util.display
import org.simbrain.util.drawCenteredOutlinedLabel
import org.simbrain.util.format
import java.awt.BasicStroke
import java.awt.geom.AffineTransform
import java.awt.geom.Line2D
import java.awt.geom.Point2D
import java.awt.geom.QuadCurve2D
import java.awt.geom.Rectangle2D
import javax.swing.JPopupMenu
import kotlin.math.atan2

class GapJunctionNode(
    networkPanel: NetworkPanel,
    val junction: GapJunction
) : ScreenElement(networkPanel) {

    private val line = PPath.Float()

    private val glyph = PNode()

    private val glyphBackground = PPath.Float(
        Rectangle2D.Float(-(GAP_HALF + BAR_THICKNESS + 1.5f), -BAR_HALF_LENGTH - 1f,
            2 * (GAP_HALF + BAR_THICKNESS + 1.5f), 2 * (BAR_HALF_LENGTH + 1f))
    )

    private val bar1 = PPath.Float(
        Rectangle2D.Float(-GAP_HALF - BAR_THICKNESS, -BAR_HALF_LENGTH, BAR_THICKNESS, 2 * BAR_HALF_LENGTH)
    )

    private val bar2 = PPath.Float(
        Rectangle2D.Float(GAP_HALF, -BAR_HALF_LENGTH, BAR_THICKNESS, 2 * BAR_HALF_LENGTH)
    )

    private val lineBound = Line2D.Double()

    private var labelCenter = Point2D.Double()

    private var glyphScale = 1.0

    private val subscriptionRemovers = mutableListOf<() -> Unit>()

    init {
        addChild(line)
        glyph.addChild(glyphBackground)
        glyph.addChild(bar1)
        glyph.addChild(bar2)
        addChild(glyph)

        pickable = true
        line.pickable = false
        glyphBackground.pickable = true
        bar1.pickable = true
        bar2.pickable = true
        line.paint = null

        updateAppearance()
        updatePosition()

        fun track(job: Job) {
            subscriptionRemovers += { job.cancel() }
        }
        track(junction.events.locationChanged.on(dispatcher = Dispatchers.Swing) { updatePosition() })
        track(junction.events.conductanceUpdated.on(dispatcher = Dispatchers.Swing) {
            updatePosition()
            updateAppearance()
        })
        listOf(junction.neuron1, junction.neuron2).forEach { endpoint ->
            track(endpoint.events.updateRuleChanged.on(dispatcher = Dispatchers.Swing) { _, _ -> updateAppearance() })
        }
        fun connectsEndpoints(model: NetworkModel) = model is Synapse &&
            ((model.source === junction.neuron1 && model.target === junction.neuron2) ||
                (model.source === junction.neuron2 && model.target === junction.neuron1))
        // modelAdded/modelRemoved are awaitable barrier events; subscribing them on the Swing
        // dispatcher would let an EDT-blocking fire await an invokeLater that can never run.
        subscriptionRemovers += networkPanel.network.events.modelAdded.on(dispatcher = Dispatchers.Default) { model ->
            if (connectsEndpoints(model)) withContext(Dispatchers.Swing) { updatePosition() }
        }
        subscriptionRemovers += networkPanel.network.events.modelRemoved.on(dispatcher = Dispatchers.Default) { model ->
            if (connectsEndpoints(model)) withContext(Dispatchers.Swing) { updatePosition() }
        }
        junction.events.deleted.on(dispatcher = Dispatchers.Default) {
            subscriptionRemovers.forEach { it() }
            subscriptionRemovers.clear()
        }
    }

    private fun updatePosition() {
        val p1 = junction.neuron1.location
        val p2 = junction.neuron2.location
        val hasParallelSynapse = junction.neuron1.fanOut[junction.neuron2] != null ||
            junction.neuron2.fanOut[junction.neuron1] != null

        val midX = (p1.x + p2.x) / 2
        val midY = (p1.y + p2.y) / 2
        val dx = p2.x - p1.x
        val dy = p2.y - p1.y
        val length = kotlin.math.hypot(dx, dy)

        line.reset()
        val glyphCenter: Point2D.Double
        if (hasParallelSynapse && length > 1e-6) {
            val normalX = -dy / length
            val normalY = dx / length
            val controlX = midX + normalX * 2 * CURVE_OFFSET
            val controlY = midY + normalY * 2 * CURVE_OFFSET
            line.append(QuadCurve2D.Double(p1.x, p1.y, controlX, controlY, p2.x, p2.y), false)
            glyphCenter = Point2D.Double(midX + normalX * CURVE_OFFSET, midY + normalY * CURVE_OFFSET)
        } else {
            line.append(Line2D.Double(p1.x, p1.y, p2.x, p2.y), false)
            glyphCenter = Point2D.Double(midX, midY)
        }
        lineBound.setLine(p1, p2)

        glyphScale = MIN_GLYPH_SCALE +
            (MAX_GLYPH_SCALE - MIN_GLYPH_SCALE) * (junction.conductance / junction.upperBound).coerceIn(0.0, 1.0)
        glyph.setTransform(AffineTransform().apply {
            translate(glyphCenter.x, glyphCenter.y)
            rotate(atan2(dy, dx))
            scale(glyphScale, glyphScale)
        })
        labelCenter = glyphCenter
        setBounds(glyph.fullBounds)
    }

    private fun updateAppearance() {
        val color = NetworkPreferences.gapJunctionColor
        val inert = !junction.isActive
        line.strokePaint = color
        line.stroke = if (inert) {
            BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, floatArrayOf(4f, 4f), 0f)
        } else {
            BasicStroke(1f)
        }
        glyphBackground.paint = NetworkPreferences.backgroundColor
        glyphBackground.strokePaint = null
        val barColor = if (inert || junction.conductance == 0.0) NetworkPreferences.zeroWeightColor else color
        listOf(bar1, bar2).forEach {
            it.paint = barColor
            it.strokePaint = null
        }
        repaint()
    }

    override fun refreshTheme() {
        super.refreshTheme()
        updateAppearance()
    }

    override fun paintAfterChildren(paintContext: PPaintContext) {
        super.paintAfterChildren(paintContext)
        if (!NetworkPreferences.showSynapseStrengthLabels) return
        if (GLYPH_SIZE * networkPanel.scalingFactor < NetworkPreferences.synapseStrengthLabelMinScreenSize) return

        val decimals = NetworkPreferences.synapseStrengthDecimalPlaces
        val text = "g = " + junction.conductance.format(decimals)
        val refString = "g = 9." + "9".repeat(decimals)
        val g2 = paintContext.graphics
        val font = computeCellFont(GLYPH_SIZE * 2.2, GLYPH_SIZE * 0.7, refString, g2.fontRenderContext)
        g2.drawCenteredOutlinedLabel(text, font, labelCenter.x, labelCenter.y + BAR_HALF_LENGTH * glyphScale + 8.0)
    }

    override val isDraggable: Boolean = false

    override val toolTipText: String
        get() = createTooltipText(junction) {
            if (junction.isActive) {
                junction.toString()
            } else {
                junction.toString() + "\nInert: both endpoint rules must expose a membrane potential."
            }
        }

    override val contextMenu: JPopupMenu
        get() = JPopupMenu().apply {
            add(networkPanel.networkActions.deleteAction)
            addSeparator()
            add(networkPanel.createAction(name = "Edit...") {
                propertyDialog?.display()
            })
        }

    override fun createEditDialog(): StandardDialog {
        val selected = networkPanel.selectionManager.filterSelectedModels<GapJunction>()
        return selected.ifEmpty { listOf(junction) }.createEditorDialog()
    }

    override val propertyDialog: StandardDialog get() = createEditDialog()

    override val model: GapJunction get() = junction

    override fun isIntersecting(bound: PBounds?): Boolean {
        if (bound == null) return false
        return bound.intersectsLine(lineBound) || glyph.globalFullBounds.intersects(bound)
    }

    companion object {
        private const val BAR_HALF_LENGTH = 7f
        private const val BAR_THICKNESS = 3f
        private const val GAP_HALF = 2.5f
        private const val CURVE_OFFSET = 25.0
        private const val GLYPH_SIZE = (2 * BAR_HALF_LENGTH).toDouble()
        private const val MIN_GLYPH_SCALE = 0.55
        private const val MAX_GLYPH_SCALE = 1.7
    }
}
