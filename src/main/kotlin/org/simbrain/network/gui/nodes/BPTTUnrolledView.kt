/**
 * The unrolled-over-time view of a [BPTTNetwork], drawn extending leftward from the rolled-up network.
 *
 * The columns are the steps that led to the present one, so time reads left to right and the rolled
 * network is the newest step, at the right. A sequence length of four therefore draws three columns,
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
import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.simbrain.network.core.NeuronArray
import org.simbrain.network.core.WeightMatrix
import org.simbrain.network.gui.NetworkPanel
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.network.subnetworks.BPTTNetwork
import org.simbrain.util.*
import org.simbrain.util.piccolo.SimbrainImage
import org.simbrain.util.piccolo.addBorder
import org.simbrain.util.widgets.BezierArrow
import org.simbrain.util.widgets.bezierArrow
import java.awt.geom.Rectangle2D
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.round

/**
 * @param rolledLeftEdge the left edge of the rolled network's drawn extent, in canvas coordinates, or
 * null while that is not yet measurable. Supplied rather than read from the parent because the columns
 * have to clear whatever the rolled network happens to draw, which is more than its layers: its weight
 * matrix nodes, and a recurrent arrow whose circle bulges out to the left. Measuring it means this
 * keeps working if any of that changes.
 */
class BPTTUnrolledView(
    private val bptt: BPTTNetwork,
    private val networkPanel: NetworkPanel,
    private val rolledLeftEdge: () -> Double?
) : PNode() {

    /**
     * A layer drawn without a model behind it, following however the real layer is set to draw itself:
     * as a horizontal strip, a vertical one, a square grid, or neuron circles, with a bias image beside
     * the activations if that layer shows one. Reads the layer's own [NeuronArray.displayColumns] and
     * modes, so a column cannot end up depicting the layer differently from the layer itself.
     *
     * The rectangle is held rather than recomputed because assigning to [SimbrainImage.image] resets a
     * PImage's bounds to the pixel dimensions of the new image, which for a one-row strip would collapse
     * it to a few pixels. A real layer node re-sets bounds after every assignment for the same reason.
     */
    class LayerStandIn(
        private val layer: NeuronArray,
        rect: Rectangle2D,
        private val parent: PNode,
        networkPanel: NetworkPanel
    ) {
        /**
         * What the layer actually draws. A layer's reported bounds include the padding between its
         * contents and the border around them, so mimicking it means insetting by the same amount.
         */
        private val content = Rectangle2D.Double(
            rect.x + ArrayLayerNode.DEFAULT_MARGIN,
            rect.y + ArrayLayerNode.DEFAULT_MARGIN,
            (rect.width - 2 * ArrayLayerNode.DEFAULT_MARGIN).coerceAtLeast(1.0),
            (rect.height - 2 * ArrayLayerNode.DEFAULT_MARGIN).coerceAtLeast(1.0)
        )

        /**
         * Where the activations go, and where the biases go when shown. A layer showing biases stacks the
         * two images, along the short axis, so each takes half the space less the gap between them.
         */
        private val activationRect: Rectangle2D
        private val biasRect: Rectangle2D?

        init {
            if (!layer.isShowBias) {
                activationRect = content
                biasRect = null
            } else if (layer.verticalLayout && !layer.gridMode) {
                val w = (content.width - ArrayLayerNode.DEFAULT_MARGIN) / 2
                activationRect = Rectangle2D.Double(content.x, content.y, w, content.height)
                biasRect = Rectangle2D.Double(
                    content.x + w + ArrayLayerNode.DEFAULT_MARGIN, content.y, w, content.height
                )
            } else {
                val h = (content.height - ArrayLayerNode.DEFAULT_MARGIN) / 2
                activationRect = Rectangle2D.Double(content.x, content.y, content.width, h)
                biasRect = Rectangle2D.Double(
                    content.x, content.y + h + ArrayLayerNode.DEFAULT_MARGIN, content.width, h
                )
            }
        }

        private val circles: List<NeuronCircleNode>? = if (!layer.circleMode) null else {
            List(layer.size) { NeuronCircleNode(networkPanel).also { parent.addChild(it) } }
        }

        private val activationImage: SimbrainImage? = if (layer.circleMode) null else {
            SimbrainImage().also { parent.addChild(it) }
        }

        private val biasImage: SimbrainImage? = if (layer.circleMode || biasRect == null) null else {
            SimbrainImage().also { parent.addChild(it) }
        }

        init {
            if (circles != null) {
                layoutCircles()
                show(DoubleArray(layer.size))
            }
            biasImage?.let {
                it.image = layer.biases.toDoubleArray()
                    .toSimbrainColorImage(layer.displayColumns, layer.displayRows)
                it.setBounds(biasRect!!.x, biasRect.y, biasRect.width, biasRect.height)
                parent.addChild(it.addBorder())
            }
            activationImage?.let {
                show(DoubleArray(layer.size))
                parent.addChild(it.addBorder())
            }
        }

        /**
         * Circles sit on the same grid the real node uses, centred in the space the layer reports, since
         * that space was measured from the real node laying out the same number of circles.
         */
        private fun layoutCircles() {
            val cols = layer.displayColumns
            val rows = ceil(layer.size.toDouble() / cols).toInt()
            val originX = content.centerX - (cols - 1) * NeuronArrayNode.CIRCLE_SPACING / 2
            val originY = content.centerY - (rows - 1) * NeuronArrayNode.CIRCLE_SPACING / 2
            circles?.forEachIndexed { i, circle ->
                circle.setOffset(
                    originX + (i % cols) * NeuronArrayNode.CIRCLE_SPACING,
                    originY + (i / cols) * NeuronArrayNode.CIRCLE_SPACING
                )
            }
        }

        fun show(values: DoubleArray) {
            circles?.forEachIndexed { i, circle ->
                circle.drawActivation(values.getOrElse(i) { 0.0 }, layer.updateRule.graphicalBounds)
                circle.setClamped(layer.isClamped)
                circle.setLabel(layer.labelArray.getOrNull(i))
            }
            activationImage?.let {
                it.image = values.toSimbrainColorImage(layer.displayColumns, layer.displayRows)
                it.setBounds(activationRect.x, activationRect.y, activationRect.width, activationRect.height)
            }
        }
    }

    /** The drawn layers of one unrolled timestep, so earlier steps can be fed real values. */
    class Column(val input: LayerStandIn, val hidden: LayerStandIn, val output: LayerStandIn)

    /**
     * A drawn copy of one of the real weight matrices, sitting on the arrow that applies it. Shows the
     * matrix's actual weights, so every copy of a matrix looks identical, which is the point: they are
     * one matrix, drawn once per timestep it is used at.
     */
    private class MatrixImage(
        private val matrix: WeightMatrix,
        private val image: SimbrainImage,
        private val rect: Rectangle2D
    ) {
        lateinit var border: PPath
            private set

        fun refresh() {
            val weights = matrix.weights
            val rendered = weights.flatten().toSimbrainColorImage(weights.ncol(), weights.nrow())
            // Follows the same preference a real node does, or a copy would stop matching the matrix it
            // is a copy of the moment the user flips it.
            image.image = if (NetworkPreferences.weightMatrixTargetSource) rendered else rendered.transposed()
            image.setBounds(rect.x, rect.y, rect.width, rect.height)
        }

        fun attachBorder(): PPath = (image.addBorder() as PPath).also { border = it }
    }

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

    private val matrixImagesByWeights = mutableMapOf<SharedWeights, MutableList<MatrixImage>>()

    private var caption: PText? = null

    private var highlighted: SharedWeights? = null

    private var captionAnchor: java.awt.geom.Point2D? = null

    /**
     * Whether the last [rebuild] managed to measure the rolled network. False means the columns are not
     * drawn yet, since placing them without knowing what they have to clear would overlap it.
     */
    var laidOut = false
        private set

    private var builtFrom: List<Any> = emptyList()

    /**
     * What the drawing depends on beyond the activations it is fed: how each layer is set to draw itself,
     * and how big it therefore is. A layer's size is pushed back from its node after it lays out, so it
     * can lag a mode change by a frame, which is why this is compared rather than assumed.
     */
    private fun buildSignature(): List<Any> = listOf(bptt.inputLayer, bptt.hiddenLayer, bptt.outputLayer)
        .flatMap {
            listOf(
                it.width, it.height, it.size,
                it.gridMode, it.circleMode, it.verticalLayout, it.isShowBias
            )
        // The rolled network's own drawn extent, which the columns are placed against. Included because it
        // moves for reasons the layers alone do not predict: a wider hidden layer pushes the recurrent
        // arrow further out, and that happens a layout pass after the width itself changes. Without it the
        // columns stay where a narrower network put them and are overdrawn by the arrow.
        //
        // Safe to read here because the measurement covers the subnetwork's contents only and not this
        // drawing, so it cannot depend on the layout it is used to decide. Rounded so that sub-pixel
        // jitter cannot keep asking for rebuilds.
        } + listOfNotNull(rolledLeftEdge()?.let { round(it) })

    /** Whether anything the drawing was built from has changed since it was built. */
    fun stale() = laidOut && buildSignature() != builtFrom

    init {
        // Nothing here stands for a model object, so it should not respond to clicks, selection or
        // dragging the way a real layer node does.
        pickable = false
        childrenPickable = false
        rebuild()
        syncPosition()
    }

    /**
     * Rebuild from scratch. Needed when the sequence length changes, since that changes the number of
     * columns, when a layer changes how it draws itself, and on a theme switch, since colours are baked
     * into the drawn shapes.
     */
    fun rebuild() {
        builtFrom = buildSignature()
        removeAllChildren()
        columns.clear()
        arrowsByWeights.clear()
        matrixImagesByWeights.clear()
        caption = null

        val priorSteps = (bptt.trainerConfig.sequenceLength - 1).coerceIn(0, MAX_EXTRA_COLUMNS)
        if (priorSteps == 0) {
            laidOut = true
            return
        }

        val theme = NetworkTheme.current
        val realLayers = listOf(bptt.inputLayer, bptt.hiddenLayer, bptt.outputLayer)

        // Where the nearest prior column may reach. Nothing is drawn until the rolled network can be
        // measured, since guessing would put a column inside its outline.
        val nearestRightEdge = rolledLeftEdge()?.minus(bptt.hiddenLayer.location.x)?.minus(COLUMN_GAP)
        laidOut = nearestRightEdge != null
        if (nearestRightEdge == null) return

        val widestLayer = realLayers.maxOf { if (it.width > 0) it.width else FALLBACK_WIDTH }
        val nearestOffset = nearestRightEdge - widestLayer / 2

        // Derived rather than fixed because a layer can be much wider than any constant would anticipate:
        // eight neurons drawn as circles are wider than three drawn as a strip. Columns have to clear each
        // other and the recurrent matrix copy that sits on the arrow between them, or a wide layer ends up
        // drawn on top of its own connections.
        val columnPitch = max(MIN_COLUMN_PITCH, widestLayer + MATRIX_SIZE + 2 * MATRIX_CLEARANCE)

        // Oldest first, so the last entry is the rolled network. Prior steps sit to its left, which is
        // why their offsets are negative.
        val columnRects = mutableListOf<List<Rectangle2D>>()

        repeat(priorSteps) { k ->
            val stepsBack = priorSteps - k
            val rects = realLayers.map { rectFor(it, nearestOffset - columnPitch * (stepsBack - 1)) }
            columnRects.add(rects)

            val strips = realLayers.zip(rects).map { (layer, rect) -> addLayerStandIn(layer, rect, theme) }
            columns.add(Column(strips[0], strips[1], strips[2]))

            addStepLabel(rects[0], "t-$stepsBack", theme)
        }

        columnRects.add(realLayers.map { rectFor(it, 0.0) })
        addStepLabel(columnRects.last()[0], "t", theme)

        // Drawn last so they sit above the strips, and once every rectangle is known.
        //
        // Every arrow carries a picture of the matrix it applies, the way an unrolled diagram labels each
        // column's connections. The rolled network already draws real nodes for its own matrices, so its
        // arrows are left to those and only the drawn steps get copies.
        columnRects.forEachIndexed { index, rects ->
            if (index < columns.size) {
                addArrow(rects[0], rects[1], SharedWeights.INPUT_TO_HIDDEN, withMatrix = true)
                addArrow(rects[1], rects[2], SharedWeights.HIDDEN_TO_OUTPUT, withMatrix = true)
            }
            // The connection that crosses a timestep boundary. The last one carries the most recent prior
            // step into the live network, which is what ties the drawing to the model. That one's matrix
            // is the real recurrent node, whose image box already sits along this arrow once its loop is
            // hidden, so drawing a copy there too would show it twice.
            if (index > 0) {
                addArrow(
                    columnRects[index - 1][1], rects[1], SharedWeights.RECURRENT,
                    withMatrix = index < columns.size
                )
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
        matrixImagesByWeights.forEach { (role, images) ->
            val color = if (role == weights) theme.backwardTrace else theme.imageBorder
            images.forEach { it.border.strokePaint = color }
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

    /** How many drawn copies of [weights] there are, one on each arrow a real node does not cover. */
    fun matrixImageCount(weights: SharedWeights) = matrixImagesByWeights[weights]?.size ?: 0

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

    /** Re-read the real matrices, so the drawn copies keep up with training. */
    fun refreshMatrices() {
        matrixImagesByWeights.values.flatten().forEach { it.refresh() }
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
     * The appearance of a layer without a model behind it: whatever the real layer draws, plus the border
     * around it and a label. Starts empty rather than showing whatever an uninitialised array holds, so
     * an unfed column reads as empty instead of as plausible values.
     */
    private fun addLayerStandIn(layer: NeuronArray, rect: Rectangle2D, theme: NetworkTheme.Palette): LayerStandIn {
        // Added before the contents, because the themed border box is filled with the background colour
        // and would otherwise paint over everything inside it.
        addChild(PPath.createRectangle(rect.x, rect.y, rect.width, rect.height).also { it.applyLayerBorderTheme() })
        val standIn = LayerStandIn(layer, rect, this, networkPanel)
        addChild(PText(layer.label).apply {
            font = Theme.label
            textPaint = theme.valueText
            centerFullBoundsOnPoint(rect.centerX, rect.y - LABEL_GAP)
        })
        return standIn
    }

    private fun addStepLabel(inputRect: Rectangle2D, text: String, theme: NetworkTheme.Palette) {
        addChild(PText(text).apply {
            font = Theme.label
            textPaint = theme.valueText
            centerFullBoundsOnPoint(inputRect.centerX, inputRect.maxY + STEP_LABEL_GAP)
        })
    }

    private fun addArrow(from: Rectangle2D, to: Rectangle2D, weights: SharedWeights, withMatrix: Boolean) {
        val arrow = bezierArrow { color = NetworkTheme.current.connectorArrow }
        addChild(arrow)
        arrow.layout(from.outlines, to.outlines, false)
        arrowsByWeights.getOrPut(weights) { mutableListOf() }.add(arrow)
        if (withMatrix) {
            addMatrixImage(weights, (from.centerX + to.centerX) / 2, (from.centerY + to.centerY) / 2)
        }
    }

    /** A copy of one of the real matrices, centred on the arrow that applies it. */
    private fun addMatrixImage(weights: SharedWeights, centerX: kotlin.Double, centerY: kotlin.Double) {
        val matrix = matrixFor(weights) ?: return
        val rect = Rectangle2D.Double(
            centerX - MATRIX_SIZE / 2, centerY - MATRIX_SIZE / 2, MATRIX_SIZE, MATRIX_SIZE
        )
        val image = SimbrainImage()
        addChild(image)
        // The border reads the image's bounds, so the first render has to happen before it is added.
        val matrixImage = MatrixImage(matrix, image, rect)
        matrixImage.refresh()
        addChild(matrixImage.attachBorder())
        matrixImagesByWeights.getOrPut(weights) { mutableListOf() }.add(matrixImage)
    }

    private fun matrixFor(weights: SharedWeights) = when (weights) {
        SharedWeights.INPUT_TO_HIDDEN -> bptt.wmList.getOrNull(0)
        SharedWeights.HIDDEN_TO_OUTPUT -> bptt.wmList.getOrNull(1)
        SharedWeights.RECURRENT -> bptt.hiddenToHidden
    }

    companion object {
        /** Floor for the spacing between drawn columns, used when the layers are narrow enough not to set it. */
        private const val MIN_COLUMN_PITCH = 300.0

        /** Breathing room on either side of a matrix copy sitting between two columns. */
        private const val MATRIX_CLEARANCE = 30.0

        /** Plain visual breathing room between the rolled network's drawn extent and the newest column. */
        private const val COLUMN_GAP = 90.0

        /** Matches the image box a real [WeightMatrixNode] draws, so the copies read as the same thing. */
        private const val MATRIX_SIZE = 90.0
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
