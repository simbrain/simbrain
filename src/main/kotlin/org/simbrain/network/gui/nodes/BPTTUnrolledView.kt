/**
 * The unrolled-over-time view of a [BPTTNetwork], drawn extending leftward from the rolled-up network.
 *
 * The columns are the steps that led to the present one, so time reads left to right and the rolled
 * network is the newest step, at the right. A truncation depth of four therefore draws three columns,
 * labelled t-3 through t-1, with the live network as t.
 *
 * Looking backward rather than forward is what lets one picture serve both cases. Training computes a
 * whole window at once and leaves the layers at its last step; ordinary iteration advances one step at
 * a time and can only ever know the past. Both end with the live network holding the newest step, so
 * both fill these columns the same way.
 *
 * Columns are drawn rather than built from model objects. An earlier version used real [NeuronArray]s
 * that were never registered with the network, which made them draggable and selectable, and any
 * model-to-node lookup they triggered blocked for ten seconds before throwing, because
 * [org.simbrain.network.gui.NetworkPanel.getNode] awaits a deferred that is never completed for an
 * unregistered model. Mimicking a layer's appearance, the way circle mode mimics neurons, avoids all of
 * that: activation strips go through the same colour-mapping a real layer uses, so they still show
 * genuine values once fed, and the whole view is non-pickable.
 *
 * Unrolling during training is virtual: there is one of each weight matrix however many columns are
 * drawn, and every column's gradient is summed into it.
 */
package org.simbrain.network.gui.nodes

import org.piccolo2d.PNode
import org.piccolo2d.nodes.PText
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.util.NetworkTheme
import org.simbrain.util.Theme
import org.simbrain.util.outlines
import org.simbrain.util.point
import org.simbrain.util.piccolo.SimbrainImage
import org.simbrain.util.piccolo.addBorder
import org.simbrain.util.toSimbrainColorImage
import org.simbrain.util.widgets.BezierArrow
import org.simbrain.util.widgets.bezierArrow
import java.awt.geom.Rectangle2D

class BPTTUnrolledView(private val bptt: BPTTNetwork) : PNode() {

    /**
     * One drawn activation strip. Holds its own rectangle because assigning to [SimbrainImage.image]
     * resets a PImage's bounds to the pixel dimensions of the new image, which for a one-row strip would
     * collapse it to a few pixels. A real layer node re-sets bounds after every image assignment for the
     * same reason.
     */
    class Strip(private val image: SimbrainImage, private val rect: Rectangle2D) {
        fun show(values: DoubleArray) {
            image.image = values.toSimbrainColorImage(values.size, 1)
            image.setBounds(rect.x, rect.y, rect.width, rect.height)
        }
    }

    /** The activation strips of one unrolled timestep, so earlier steps can be fed real values. */
    class Column(val input: Strip, val hidden: Strip, val output: Strip)

    /**
     * The three weight matrices, each of which appears once per column even though only one of each
     * exists. Naming them is what lets every drawn instance of one be lit at once.
     */
    enum class SharedWeights(val description: String) {
        INPUT_TO_HIDDEN("Input to hidden"),
        HIDDEN_TO_OUTPUT("Hidden to output"),
        RECURRENT("Hidden to hidden")
    }

    val columns = mutableListOf<Column>()

    private val arrowsByWeights = mutableMapOf<SharedWeights, MutableList<BezierArrow>>()

    private var caption: PText? = null

    private var highlighted: SharedWeights? = null

    private var captionAnchor: java.awt.geom.Point2D? = null

    init {
        // Nothing here stands for a model object, so it should not respond to clicks, selection or
        // dragging the way a real layer node does.
        pickable = false
        childrenPickable = false
        rebuild()
        syncPosition()
    }

    /**
     * Rebuild from scratch. Needed when the truncation depth changes, since that changes the number of
     * columns, and on a theme switch, since colours are baked into the drawn shapes.
     */
    fun rebuild() {
        removeAllChildren()
        columns.clear()
        arrowsByWeights.clear()
        caption = null

        val priorSteps = (bptt.trainerConfig.truncationDepth - 1).coerceIn(0, MAX_EXTRA_COLUMNS)
        if (priorSteps == 0) return

        val theme = NetworkTheme.current
        val realLayers = listOf(bptt.inputLayer, bptt.hiddenLayer, bptt.outputLayer)

        // Oldest first, so the last entry is the rolled network. Prior steps sit to its left, which is
        // why their offsets are negative.
        val columnRects = mutableListOf<List<Rectangle2D>>()

        repeat(priorSteps) { k ->
            val stepsBack = priorSteps - k
            val rects = realLayers.map { rectFor(it, -(COLUMN_PITCH * stepsBack + ROLLED_NETWORK_CLEARANCE)) }
            columnRects.add(rects)

            val strips = realLayers.zip(rects).map { (layer, rect) -> addLayerStandIn(layer, rect, theme) }
            columns.add(Column(strips[0], strips[1], strips[2]))

            addStepLabel(rects[0], "t-$stepsBack", theme)
        }

        columnRects.add(realLayers.map { rectFor(it, 0.0) })
        addStepLabel(columnRects.last()[0], "t", theme)

        // Drawn last so they sit above the strips, and once every rectangle is known.
        columnRects.forEachIndexed { index, rects ->
            // The rolled network draws its own feed-forward matrices, so only the prior steps need them.
            if (index < columns.size) {
                addArrow(rects[0], rects[1], SharedWeights.INPUT_TO_HIDDEN)
                addArrow(rects[1], rects[2], SharedWeights.HIDDEN_TO_OUTPUT)
            }
            // The connection that crosses a timestep boundary. The last one carries the most recent prior
            // step into the live network, which is what ties the drawing to the model.
            if (index > 0) {
                addArrow(columnRects[index - 1][1], rects[1], SharedWeights.RECURRENT)
            }
        }

        val lastInput = columnRects.last()[0]
        val firstInput = columnRects.first()[0]
        caption = PText("").apply {
            font = Theme.label
            textPaint = theme.backwardTrace
        }.also { addChild(it) }
        captionAnchor = point((firstInput.centerX + lastInput.centerX) / 2, firstInput.maxY + CAPTION_GAP)

        highlight(highlighted)
    }

    /**
     * Light every drawn instance of one weight matrix, so that the columns read as repeated uses of a
     * single matrix rather than as separate ones. Passing null returns everything to normal.
     */
    fun highlight(weights: SharedWeights?) {
        highlighted = weights
        val theme = NetworkTheme.current
        arrowsByWeights.forEach { (role, arrows) ->
            // backwardTrace rather than receptiveFieldTrace: the latter is defined as the same value as
            // connectorArrow in both themes, so highlighting with it changes nothing on screen.
            val color = if (role == weights) theme.backwardTrace else theme.connectorArrow
            arrows.forEach { it.updateColor(color) }
        }
        caption?.apply {
            text = if (weights == null) {
                ""
            } else {
                val steps = columns.size + 1
                "${weights.description}: one matrix, used at all $steps steps, gradients summed"
            }
            captionAnchor?.let { centerFullBoundsOnPoint(it.x, it.y) }
        }
    }

    /** How many arrows stand for [weights]. One per column if the connection was tagged correctly. */
    fun arrowCount(weights: SharedWeights) = arrowsByWeights[weights]?.size ?: 0

    /**
     * Keep the drawing aligned with the rolled network as it is dragged. Children are laid out relative
     * to the hidden layer, so following the network is a translation rather than a rebuild.
     */
    fun syncPosition() {
        val anchor = bptt.hiddenLayer.location
        setOffset(anchor.x, anchor.y)
    }

    /**
     * How many timesteps the drawing covers, counting the rolled network as the last of them. Callers
     * need this to line a trace up against the columns, since a trace shorter than the window fills the
     * newest columns and leaves the oldest empty.
     */
    val stepCount get() = columns.size + 1

    /**
     * Feed one timestep's activations into the drawing. Steps are numbered from zero at the oldest drawn
     * column; the last step is the rolled network itself, which renders its own values and is ignored
     * here.
     */
    fun showActivations(step: Int, input: DoubleArray, hidden: DoubleArray, output: DoubleArray) {
        val column = columns.getOrNull(step) ?: return
        column.input.show(input)
        column.hidden.show(hidden)
        column.output.show(output)
    }

    /**
     * Blank every column. Called before each refresh so that a shortened history leaves the steps that
     * have not happened yet empty rather than showing what used to be in them.
     */
    fun clearColumns() {
        columns.forEach { column ->
            column.input.show(DoubleArray(bptt.inputLayer.size))
            column.hidden.show(DoubleArray(bptt.hiddenLayer.size))
            column.output.show(DoubleArray(bptt.outputLayer.size))
        }
    }

    /**
     * A layer's drawn rectangle, relative to the hidden layer and shifted horizontally by [dx], which is
     * negative for the steps that precede the rolled network. Taken from the
     * model rather than from node bounds: a layer node's own path bounds are empty because its content
     * sits in a child, and its full bounds would include the interaction box.
     */
    private fun rectFor(layer: NeuronArray, dx: Double): Rectangle2D {
        val anchor = bptt.hiddenLayer.location
        val width = if (layer.width > 0) layer.width else FALLBACK_WIDTH
        val height = if (layer.height > 0) layer.height else FALLBACK_HEIGHT
        return Rectangle2D.Double(
            layer.location.x - anchor.x - width / 2 + dx,
            layer.location.y - anchor.y - height / 2,
            width,
            height
        )
    }

    /**
     * The appearance of a layer without a model behind it: the same colour-mapped activation strip a
     * real layer draws, plus a border and a label. Starts empty rather than showing whatever an
     * uninitialised array holds, so an unfed column reads as empty instead of as plausible values.
     */
    private fun addLayerStandIn(layer: NeuronArray, rect: Rectangle2D, theme: NetworkTheme.Palette): Strip {
        val strip = SimbrainImage().apply {
            image = DoubleArray(layer.size).toSimbrainColorImage(layer.size, 1)
            setBounds(rect.x, rect.y, rect.width, rect.height)
        }
        addChild(strip)
        addChild(strip.addBorder())
        addChild(PText(layer.label).apply {
            font = Theme.label
            textPaint = theme.valueText
            centerFullBoundsOnPoint(rect.centerX, rect.y - LABEL_GAP)
        })
        return Strip(strip, rect)
    }

    private fun addStepLabel(inputRect: Rectangle2D, text: String, theme: NetworkTheme.Palette) {
        addChild(PText(text).apply {
            font = Theme.label
            textPaint = theme.valueText
            centerFullBoundsOnPoint(inputRect.centerX, inputRect.maxY + STEP_LABEL_GAP)
        })
    }

    private fun addArrow(from: Rectangle2D, to: Rectangle2D, weights: SharedWeights) {
        val arrow = bezierArrow { color = NetworkTheme.current.connectorArrow }
        addChild(arrow)
        arrow.layout(from.outlines, to.outlines, false)
        arrowsByWeights.getOrPut(weights) { mutableListOf() }.add(arrow)
    }

    companion object {
        private const val COLUMN_PITCH = 300.0

        /**
         * Extra room between the rolled network and the nearest prior column. The rolled network is far
         * wider than a drawn column, since it carries its weight matrix nodes and a recurrent arrow whose
         * circle bulges out to the left, which is the side the prior steps are on. Without this the
         * nearest column lands inside the subnetwork outline and under the recurrent matrix's label.
         */
        private const val ROLLED_NETWORK_CLEARANCE = 220.0
        private const val LABEL_GAP = 14.0
        private const val STEP_LABEL_GAP = 24.0
        private const val CAPTION_GAP = 52.0
        private const val FALLBACK_WIDTH = 110.0
        private const val FALLBACK_HEIGHT = 30.0

        /**
         * A deep truncation window would draw a strip too wide to read, so the drawing stops here
         * rather than silently implying the network unrolls less far than it does. The steps that are
         * dropped are the oldest ones, since those are the ones the gradient reaches most weakly.
         */
        const val MAX_EXTRA_COLUMNS = 7
    }
}
