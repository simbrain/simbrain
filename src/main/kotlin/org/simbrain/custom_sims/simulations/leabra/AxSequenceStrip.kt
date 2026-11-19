/**
 * GUI overlays drawn on the 1-2-AX network, not part of the saved network: a strip showing the sequence since memory
 * was cleared, a badge for the dopamine on each rewarded step, and the lines linking each stripe's Go/NoGo pair to its stripe.
 * Each overlay calls [PNode.repaint] when it changes, which asks the canvas to redraw its area directly, so changes
 * show up even when nothing else on the canvas is changing.
 */
package org.simbrain.custom_sims.simulations.leabra

import org.piccolo2d.PNode
import org.piccolo2d.util.PPaintContext
import org.simbrain.util.NetworkTheme
import java.awt.BasicStroke
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Line2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import javax.swing.SwingUtilities

/**
 * One step in the strip. [response] is null while the network is still settling.
 */
data class AxStepView(val input: Int, val target: Int, val response: Int?)

/**
 * The strip's left edge lines up with [leftOf] and its top with [topOf], and it follows them when they move, so it
 * stays inside the area the network view zooms to fit.
 */
class AxSequenceStrip(private val leftOf: PNode, private val topOf: PNode) : PNode() {

    var steps: List<AxStepView> = emptyList()
        set(value) {
            field = value
            repaint()
        }

    private var left = 0.0
    private var top = 0.0

    init {
        pickable = false
        leftOf.onMoved(::place)
        topOf.onMoved(::place)
        place()
    }

    private fun place() {
        repaint()
        left = leftOf.globalFullBounds.minX
        top = topOf.globalFullBounds.minY
        setBounds(left, top, LABEL_WIDTH + MAX_COLUMNS * COLUMN_WIDTH, BOX_HEIGHT + MARK_HEIGHT)
        repaint()
    }

    override fun paint(paintContext: PPaintContext) {
        val g = paintContext.graphics
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        val textColor = NetworkTheme.current.valueText
        val plain = Font(Font.SANS_SERIF, Font.PLAIN, FONT_SIZE)
        val bold = plain.deriveFont(Font.BOLD)
        g.color = textColor
        g.font = plain
        ROW_LABELS.forEachIndexed { row, label -> g.drawString(label, left.toFloat(), rowBaseline(row)) }
        if (steps.isEmpty()) {
            g.drawString("Click 1 or 2 to start", (left + LABEL_WIDTH).toFloat(), rowBaseline(0))
            return
        }
        val shown = steps.takeLast(MAX_COLUMNS)
        shown.forEachIndexed { i, step ->
            val x = left + LABEL_WIDTH + i * COLUMN_WIDTH
            val right = step.response?.let { it == step.target }
            val newest = i == shown.lastIndex
            g.stroke = BasicStroke(if (newest) 6f else 3f)
            g.color = when (right) {
                true -> CORRECT_COLOR
                false -> WRONG_COLOR
                null -> NetworkTheme.current.nodeOutline
            }
            g.drawRoundRect(x.toInt(), top.toInt(), (COLUMN_WIDTH - GAP).toInt(), BOX_HEIGHT.toInt(), 12, 12)
            val cells = listOf(AX_INPUTS[step.input], AX_OUTPUTS[step.target], step.response?.let { AX_OUTPUTS[it] } ?: "…")
            cells.forEachIndexed { row, text ->
                g.color = textColor
                g.font = if (row == 0) bold else plain
                g.drawCentered(text, x + (COLUMN_WIDTH - GAP) / 2, rowBaseline(row))
            }
            if (right != null) {
                g.color = if (right) CORRECT_COLOR else WRONG_COLOR
                g.font = bold
                g.drawCentered(if (right) "✓" else "✗", x + (COLUMN_WIDTH - GAP) / 2, (top + BOX_HEIGHT + MARK_HEIGHT - 8).toFloat())
            }
        }
    }

    private fun rowBaseline(row: Int) = (top + PADDING + (row + 1) * ROW_HEIGHT - 8).toFloat()

    private fun Graphics2D.drawCentered(text: String, centerX: Double, baseline: Float) {
        drawString(text, (centerX - fontMetrics.stringWidth(text) / 2.0).toFloat(), baseline)
    }

    companion object {
        const val MAX_COLUMNS = 6
        private val ROW_LABELS = listOf("Input", "Correct", "Network")
        private const val FONT_SIZE = 32
        private const val ROW_HEIGHT = 42.0
        private const val PADDING = 8.0
        private const val BOX_HEIGHT = 3 * ROW_HEIGHT + 2 * PADDING
        private const val MARK_HEIGHT = 48.0
        private const val LABEL_WIDTH = 160.0
        private const val COLUMN_WIDTH = 90.0
        private const val GAP = 10.0
    }
}

/**
 * What dopamine did on a rewarded step.
 */
enum class DopamineChange(val text: String, val fill: java.awt.Color) {
    BURST("DOPAMINE BURST", java.awt.Color(255, 214, 0)),
    DIP("DOPAMINE DIP", java.awt.Color(173, 205, 240)),
    NONE("NO DOPAMINE CHANGE", java.awt.Color(0xdd, 0xdd, 0xdd)),
}

/**
 * A badge drawn above [anchor], the dopamine unit, with their right edges lined up so that it stays inside the area
 * the network view zooms to fit. It shows [change] on a fill that matches it: yellow for a burst, light blue for a
 * dip, and gray for no change. The text is dark so it keeps strong contrast on every fill. It follows [anchor] when
 * it moves.
 */
class DopamineBadge(private val anchor: PNode) : PNode() {

    /**
     * What dopamine did, or null to hide the badge.
     */
    var change: DopamineChange? = null
        set(value) {
            field = value
            repaint()
        }

    init {
        pickable = false
        anchor.onMoved(::place)
        place()
    }

    private fun place() {
        val a = anchor.globalFullBounds
        repaint()
        setBounds(a.maxX - WIDTH, a.minY - GAP - HEIGHT, WIDTH, HEIGHT)
        repaint()
    }

    override fun paint(paintContext: PPaintContext) {
        val shown = change ?: return
        val g = paintContext.graphics
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        val left = x
        val top = y
        g.color = shown.fill
        g.fillRoundRect(left.toInt(), top.toInt(), WIDTH.toInt(), HEIGHT.toInt(), 16, 16)
        g.color = TEXT
        g.font = Font(Font.SANS_SERIF, Font.BOLD, 32)
        val text = shown.text
        val textX = left + (WIDTH - g.fontMetrics.stringWidth(text)) / 2
        val textY = top + (HEIGHT + g.fontMetrics.ascent - g.fontMetrics.descent) / 2
        g.drawString(text, textX.toFloat(), textY.toFloat())
    }

    companion object {
        private const val WIDTH = 420.0
        private const val HEIGHT = 60.0
        private const val GAP = 20.0
        private val TEXT = java.awt.Color(0x22, 0x22, 0x22)
    }
}

/**
 * A line from one stripe's Go and NoGo units to the stripe, standing for the loop through the thalamus by which the
 * basal ganglia gate the stripe. It is dashed while the gate is closed, and solid, thick, and the active color while
 * it is [open]. It follows the nodes in [watched] when they move. It keeps the nodes it was given, so if the network
 * is rebuilt, for example by reopening the simulation, new lines are needed.
 */
class GateLine(private val from: List<PNode>, private val to: List<PNode>, watched: List<PNode>) : PNode() {

    var open = false
        set(value) {
            field = value
            repaint()
        }

    private var start = Point2D.Double()
    private var end = Point2D.Double()

    init {
        pickable = false
        watched.forEach { it.onMoved(::updateEnds) }
        updateEnds()
    }

    private fun updateEnds() {
        val fromBounds = from.map { it.globalFullBounds }
        val toBounds = to.map { it.globalFullBounds }
        start = Point2D.Double(fromBounds.map { it.centerX }.average(), fromBounds.minOf { it.minY } - END_GAP)
        end = Point2D.Double(toBounds.map { it.centerX }.average(), toBounds.maxOf { it.maxY } + END_GAP)
        val box = Rectangle2D.Double().apply { setFrameFromDiagonal(start, end) }
        repaint()
        setBounds(box.x - OPEN_WIDTH, box.y - OPEN_WIDTH, box.width + 2 * OPEN_WIDTH, box.height + 2 * OPEN_WIDTH)
        repaint()
    }

    override fun paint(paintContext: PPaintContext) {
        val g = paintContext.graphics
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        if (open) {
            g.color = NetworkTheme.current.hotNode
            g.stroke = BasicStroke(OPEN_WIDTH.toFloat(), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
        } else {
            g.color = NetworkTheme.current.nodeOutline
            g.stroke = BasicStroke(
                CLOSED_WIDTH, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, floatArrayOf(24f, 16f), 0f
            )
        }
        g.draw(Line2D.Double(start, end))
    }

    companion object {
        private const val OPEN_WIDTH = 12.0
        private const val CLOSED_WIDTH = 6f
        private const val END_GAP = 12.0
    }
}

/**
 * Call [action] after this node moves or changes size. It runs later on the Swing thread, so that Piccolo has finished
 * laying out the scene.
 */
private fun PNode.onMoved(action: () -> Unit) {
    addPropertyChangeListener(PNode.PROPERTY_FULL_BOUNDS) { SwingUtilities.invokeLater { action() } }
    addPropertyChangeListener(PNode.PROPERTY_TRANSFORM) { SwingUtilities.invokeLater { action() } }
}
