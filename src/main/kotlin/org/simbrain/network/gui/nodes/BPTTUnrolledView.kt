/**
 * The unrolled-over-time picture of a [BPTTNetwork], drawn beside the rolled-up network.
 *
 * This is an illustration rather than a view of model objects: unrolling in Simbrain is virtual, so
 * there is exactly one of each weight matrix no matter how many timesteps are drawn. Every column
 * here refers back to the same three matrices, which is the point the picture needs to make.
 */
package org.simbrain.network.gui.nodes

import org.piccolo2d.PNode
import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.util.NetworkTheme
import org.simbrain.util.Theme
import org.simbrain.util.point
import org.simbrain.util.toPolygon
import java.awt.BasicStroke
import java.awt.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class BPTTUnrolledView(private val bptt: BPTTNetwork) : PNode() {

    init {
        rebuild()
    }

    /**
     * Redraw from scratch. Called when the truncation depth changes, since that changes how many
     * columns there are, and on a theme switch, since the colors are baked into the shapes.
     */
    fun rebuild() {
        removeAllChildren()

        val theme = NetworkTheme.current
        val steps = bptt.trainerConfig.truncationDepth.coerceIn(1, MAX_DRAWN_STEPS)

        repeat(steps) { t ->
            val x = t * COLUMN_PITCH

            addBox(x, OUTPUT_Y, "Output", theme)
            addBox(x, HIDDEN_Y, "Hidden", theme)
            addBox(x, INPUT_Y, "Input", theme)

            // Within a timestep the signal runs input to hidden to output, as in the rolled network.
            addArrow(x + BOX_W / 2, INPUT_Y, x + BOX_W / 2, HIDDEN_Y + BOX_H, theme.connectorArrow)
            addArrow(x + BOX_W / 2, HIDDEN_Y, x + BOX_W / 2, OUTPUT_Y + BOX_H, theme.connectorArrow)

            // The one connection that crosses a timestep boundary. Highlighted because it is the
            // weights whose gradient is summed over every column rather than computed once.
            if (t > 0) {
                addArrow(
                    x - COLUMN_PITCH + BOX_W, HIDDEN_Y + BOX_H / 2,
                    x, HIDDEN_Y + BOX_H / 2,
                    theme.receptiveFieldTrace
                )
            }

            addCaption(x, TIME_LABEL_Y, "t${if (t == 0) "" else "+$t"}", theme.valueText, BOX_W)
        }

        val truncated = bptt.trainerConfig.truncationDepth > MAX_DRAWN_STEPS
        val note = buildString {
            append("Every step shares the same three weight matrices")
            if (truncated) {
                append(" — showing $MAX_DRAWN_STEPS of ${bptt.trainerConfig.truncationDepth} steps")
            }
        }
        addCaption(0.0, NOTE_Y, note, theme.valueText, steps * COLUMN_PITCH - COLUMN_GAP)
    }

    private fun addBox(x: Double, y: Double, label: String, theme: NetworkTheme.Palette) {
        addChild(PPath.createRoundRectangle(x.toFloat(), y.toFloat(), BOX_W.toFloat(), BOX_H.toFloat(), 8f, 8f).apply {
            paint = theme.tabFill
            strokePaint = theme.nodeOutline
            stroke = BasicStroke(1f)
        })
        addChild(PText(label).apply {
            font = Theme.body
            textPaint = theme.tabText
            centerFullBoundsOnPoint(x + BOX_W / 2, y + BOX_H / 2)
        })
    }

    private fun addCaption(x: Double, y: Double, text: String, color: Color, width: Double) {
        addChild(PText(text).apply {
            font = Theme.label
            textPaint = color
            centerFullBoundsOnPoint(x + width / 2, y)
        })
    }

    private fun addArrow(x1: Double, y1: Double, x2: Double, y2: Double, color: Color) {
        addChild(PPath.createLine(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat()).apply {
            strokePaint = color
            stroke = BasicStroke(3f)
        })
        val angle = atan2(y2 - y1, x2 - x1)
        val head = listOf(
            point(x2, y2),
            point(x2 - ARROW_HEAD * cos(angle - ARROW_SPREAD), y2 - ARROW_HEAD * sin(angle - ARROW_SPREAD)),
            point(x2 - ARROW_HEAD * cos(angle + ARROW_SPREAD), y2 - ARROW_HEAD * sin(angle + ARROW_SPREAD))
        ).toPolygon()
        addChild(PPath.Double(head, null).apply { paint = color })
    }

    companion object {
        private const val BOX_W = 104.0
        private const val BOX_H = 38.0
        private const val COLUMN_GAP = 74.0
        private const val COLUMN_PITCH = BOX_W + COLUMN_GAP
        private const val ROW_PITCH = 190.0
        private const val OUTPUT_Y = 0.0
        private const val HIDDEN_Y = ROW_PITCH
        private const val INPUT_Y = 2 * ROW_PITCH
        private const val TIME_LABEL_Y = INPUT_Y + BOX_H + 20.0
        private const val NOTE_Y = TIME_LABEL_Y + 30.0
        private const val ARROW_HEAD = 12.0
        private const val ARROW_SPREAD = 0.45

        /**
         * A deep truncation window would draw a picture too wide to read, so the drawing stops here
         * and says so rather than silently showing fewer steps than the network actually uses.
         */
        const val MAX_DRAWN_STEPS = 8
    }
}
