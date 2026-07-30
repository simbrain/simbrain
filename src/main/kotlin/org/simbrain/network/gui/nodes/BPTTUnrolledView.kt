/**
 * The unrolled-over-time view of a [BPTTNetwork]: real [NeuronArray] layers, rendered by the ordinary
 * [NeuronArrayNode], extending rightward from the rolled-up network.
 *
 * The rolled network is the first timestep, so a truncation depth of four adds three columns.
 *
 * The layers here are real model objects but are deliberately not registered with the network. That
 * keeps them out of serialization, out of the update loop, out of selection and deletion, and out of
 * training, while still giving genuine activation rendering rather than a hand-drawn approximation.
 * Connections are drawn as plain arrows rather than [WeightMatrixNode]s, because that node resolves its
 * endpoints through the panel's model-to-node map, which an unregistered layer is absent from.
 *
 * Unrolling during training is virtual: there is one of each weight matrix however many columns are
 * drawn, and every column's gradient is summed into it.
 */
package org.simbrain.network.gui.nodes

import org.piccolo2d.PNode
import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.util.NetworkTheme
import org.simbrain.util.Theme
import org.simbrain.util.point
import org.simbrain.util.toPolygon
import java.awt.BasicStroke
import java.awt.Color
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class BPTTUnrolledView(
    private val networkPanel: NetworkPanel,
    private val bptt: BPTTNetwork
) : PNode() {

    /** One unrolled timestep after the first. */
    class Column(val input: NeuronArray, val hidden: NeuronArray, val output: NeuronArray)

    val columns = mutableListOf<Column>()

    private val layerNodes = mutableMapOf<NeuronArray, PNode>()

    init {
        rebuild()
    }

    /**
     * Rebuild from scratch. Needed when the truncation depth changes, since that changes the number of
     * columns, and on a theme switch, since arrow colors are baked into the drawn shapes.
     */
    fun rebuild() {
        removeAllChildren()
        columns.clear()
        layerNodes.clear()

        val extraSteps = (bptt.trainerConfig.truncationDepth - 1).coerceIn(0, MAX_EXTRA_COLUMNS)
        if (extraSteps == 0) return

        val theme = NetworkTheme.current

        repeat(extraSteps) { k ->
            val offset = point(COLUMN_PITCH * (k + 1), 0.0)
            val column = Column(
                input = standInFor(bptt.inputLayer, offset),
                hidden = standInFor(bptt.hiddenLayer, offset),
                output = standInFor(bptt.outputLayer, offset)
            )
            columns.add(column)
            listOf(column.input, column.hidden, column.output).forEach { layer ->
                layerNodes[layer] = NeuronArrayNode(networkPanel, layer).also { addChild(it) }
            }
        }

        // Drawn after the layer nodes exist so their bounds are available to aim at.
        addStepLabel(bptt.inputLayer, "t", theme.valueText)
        columns.forEachIndexed { index, column ->
            val previousHidden = if (index == 0) bptt.hiddenLayer else columns[index - 1].hidden
            connect(column.input, column.hidden, theme.connectorArrow)
            connect(column.hidden, column.output, theme.connectorArrow)
            connect(previousHidden, column.hidden, theme.receptiveFieldTrace)
            addStepLabel(column.input, "t+${index + 1}", theme.valueText)
        }
    }

    /**
     * An unregistered stand-in for one of the real layers at a later timestep. Clamped so nothing
     * perturbs it, and positioned to line up with the layer it stands in for.
     */
    private fun standInFor(layer: NeuronArray, offset: Point2D) = NeuronArray(layer.size).apply {
        label = layer.label
        isClamped = true
        // Zeroed rather than left at whatever a fresh array starts with, so an empty column reads as
        // empty instead of as plausible-looking timestep values.
        fillActivations(0.0)
        gridMode = layer.gridMode
        verticalLayout = layer.verticalLayout
        location = point(layer.location.x + offset.x, layer.location.y + offset.y)
    }

    /**
     * A layer's drawn rectangle in this node's coordinates, taken from the model rather than from the
     * layer node's bounds. A node's own path bounds are empty because its content sits in a child, and
     * its full bounds would include the interaction box, which would push arrow endpoints out into
     * empty space beside the layer. [ArrayLayerNode] pushes its border box size back onto the model,
     * so these are the dimensions actually drawn.
     */
    private fun boundsOf(layer: NeuronArray): Rectangle2D {
        val width = if (layer.width > 0) layer.width else FALLBACK_LAYER_WIDTH
        val height = if (layer.height > 0) layer.height else FALLBACK_LAYER_HEIGHT
        return globalToLocal(
            Rectangle2D.Double(
                layer.location.x - width / 2, layer.location.y - height / 2, width, height
            ) as Rectangle2D
        )
    }

    private fun connect(from: NeuronArray, to: NeuronArray, color: Color) {
        val fromBounds = boundsOf(from)
        val toBounds = boundsOf(to)
        val start = point(fromBounds.centerX, fromBounds.centerY)
        val end = point(toBounds.centerX, toBounds.centerY)
        addArrow(edgePoint(fromBounds, start, end), edgePoint(toBounds, end, start), color)
    }

    /**
     * Where the segment between the two centers leaves [bounds], so an arrow stops at a layer's edge
     * instead of running underneath it.
     */
    private fun edgePoint(bounds: Rectangle2D, from: Point2D, towards: Point2D): Point2D {
        val dx = towards.x - from.x
        val dy = towards.y - from.y
        if (dx == 0.0 && dy == 0.0) return from
        val halfWidth = bounds.width / 2 + EDGE_PAD
        val halfHeight = bounds.height / 2 + EDGE_PAD
        val scale = minOf(
            if (dx == 0.0) Double.MAX_VALUE else halfWidth / abs(dx),
            if (dy == 0.0) Double.MAX_VALUE else halfHeight / abs(dy)
        )
        return point(from.x + dx * scale, from.y + dy * scale)
    }

    private fun addStepLabel(inputLayer: NeuronArray, text: String, color: Color) {
        val bounds = boundsOf(inputLayer)
        addChild(PText(text).apply {
            font = Theme.label
            textPaint = color
            centerFullBoundsOnPoint(bounds.centerX, bounds.maxY + STEP_LABEL_GAP)
        })
    }

    private fun addArrow(from: Point2D, to: Point2D, color: Color) {
        addChild(PPath.createLine(from.x.toFloat(), from.y.toFloat(), to.x.toFloat(), to.y.toFloat()).apply {
            strokePaint = color
            stroke = BasicStroke(3f)
        })
        val angle = atan2(to.y - from.y, to.x - from.x)
        val head = listOf(
            to,
            point(to.x - ARROW_HEAD * cos(angle - ARROW_SPREAD), to.y - ARROW_HEAD * sin(angle - ARROW_SPREAD)),
            point(to.x - ARROW_HEAD * cos(angle + ARROW_SPREAD), to.y - ARROW_HEAD * sin(angle + ARROW_SPREAD))
        ).toPolygon()
        addChild(PPath.Double(head, null).apply { paint = color })
    }

    companion object {
        private const val COLUMN_PITCH = 300.0
        private const val ARROW_HEAD = 12.0
        private const val ARROW_SPREAD = 0.45
        private const val EDGE_PAD = 6.0
        private const val STEP_LABEL_GAP = 22.0
        private const val FALLBACK_LAYER_WIDTH = 100.0
        private const val FALLBACK_LAYER_HEIGHT = 40.0

        /**
         * A deep truncation window would draw a strip too wide to read, so the drawing stops here
         * rather than silently implying the network unrolls less far than it does.
         */
        const val MAX_EXTRA_COLUMNS = 7
    }
}
