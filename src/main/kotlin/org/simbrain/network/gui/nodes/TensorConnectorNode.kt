/**
 * Displays tensor connections and edits selected connectors of the same underlying model type. Convolutions also
 * show their kernels, one at a time or as a grid sized to the space between the two layers, and let individual
 * kernel weights be selected and edited through [PixelSelectionHost].
 */
package org.simbrain.network.gui.nodes

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.swing.Swing
import org.piccolo2d.PNode
import org.piccolo2d.event.PBasicInputEventHandler
import org.piccolo2d.event.PInputEvent
import org.piccolo2d.nodes.PPath
import org.piccolo2d.nodes.PText
import org.piccolo2d.util.PBounds
import org.piccolo2d.util.PPaintContext
import org.simbrain.network.core.ConvolutionConnector
import org.simbrain.network.core.PoolingConnector
import org.simbrain.network.core.TensorConnector
import org.simbrain.network.gui.*
import org.simbrain.network.gui.dialogs.NetworkPreferences
import org.simbrain.util.*
import org.simbrain.util.piccolo.SimbrainImage
import org.simbrain.util.widgets.BezierArrow
import org.simbrain.util.widgets.bezierArrow
import java.awt.BasicStroke
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.CubicCurve2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import javax.swing.JPopupMenu
import kotlin.math.abs
import kotlin.math.ceil

/**
 * GUI node for a [TensorConnector]. Draws an arrow between source and target TensorNodes.
 * For [ConvolutionConnector], also shows a kernel heatmap with filter/channel navigation.
 */
class TensorConnectorNode(networkPanel: NetworkPanel, val connector: TensorConnector) :
    ScreenElement(networkPanel), PixelSelectionHost {

    val sourceNode by lazy { networkPanel.getNode(connector.source) }
    val targetNode by lazy { networkPanel.getNode(connector.target) }

    /**
     * Kernel weights the user has selected for quick-edit, as indices into [ConvolutionConnector.kernels]. Empty
     * means no pixel selection is active; in that case increment/decrement/clear/randomize fall through to
     * whole-connector behavior.
     *
     * The single kernel view only outlines the selected weights of the kernel it shows. The selection is dropped
     * when the user steps to another kernel, but kept when a receptive field trace does so temporarily.
     */
    var pixelSelection: Set<Int> = emptySet()
        set(value) {
            field = value
            if (value.isNotEmpty()) networkPanel.hideCanvasTooltip()
            repaint()
        }

    private val imgSize = 60
    val imageBox = ImageBox(imgSize, imgSize, 1f).apply {
        (connector as? ConvolutionConnector)?.let { conv ->
            makeKernelPixelSurface(conv) { currentFilter to currentInputChannel }
        }
    }

    val interactionBox = TensorConnectorInteractionBox(networkPanel)

    /** Detail label for filter/channel info (ConvolutionConnector only). */
    private val detailLabel = PText("").apply {
        font = Theme.small
        textPaint = NetworkTheme.current.valueText
    }

    /** Kernel shape caption (ConvolutionConnector only). */
    private val shapeCaption = ShapeCaption((connector as? ConvolutionConnector)?.kernelShapeString ?: "")

    /** For ConvolutionConnector: delegates to model state. */
    private var currentFilter: Int
        get() = (connector as? ConvolutionConnector)?.currentFilter ?: 0
        set(value) { (connector as? ConvolutionConnector)?.currentFilter = value }
    private var currentInputChannel: Int
        get() = (connector as? ConvolutionConnector)?.currentInputChannel ?: 0
        set(value) { (connector as? ConvolutionConnector)?.currentInputChannel = value }

    // Navigation buttons (only created for ConvolutionConnector)
    private val filterPrevButton: PNode? = (connector as? ConvolutionConnector)?.let {
        createArrowButton(ArrowDirection.UP) { previousFilter() }
    }
    private val filterNextButton: PNode? = (connector as? ConvolutionConnector)?.let {
        createArrowButton(ArrowDirection.DOWN) { nextFilter() }
    }
    private val channelPrevButton: PNode? = (connector as? ConvolutionConnector)?.let {
        createArrowButton(ArrowDirection.LEFT) { previousInputChannel() }
    }
    private val channelNextButton: PNode? = (connector as? ConvolutionConnector)?.let {
        createArrowButton(ArrowDirection.RIGHT) { nextInputChannel() }
    }

    /** Container for kernel grid display. */
    val kernelGridGroup = PNode()

    // Pre-allocated resources for ConvolutionConnector visualization

    /** Pre-allocated buffer for extracting a single kernel slice. */
    private val kernelSlice: DoubleArray? = (connector as? ConvolutionConnector)?.let {
        DoubleArray(it.kernelSize * it.kernelSize)
    }

    /** Pre-allocated BufferedImage for single-kernel view. */
    private val singleKernelImage: BufferedImage? = (connector as? ConvolutionConnector)?.let {
        BufferedImage(it.kernelSize, it.kernelSize, BufferedImage.TYPE_INT_RGB)
    }

    /** Pre-allocated grid cell BufferedImages: [filter][channel]. */
    private val gridCellImages: Array<Array<BufferedImage>>? = (connector as? ConvolutionConnector)?.let { conv ->
        Array(conv.numFilters) { Array(conv.source.shape.channels) {
            BufferedImage(conv.kernelSize, conv.kernelSize, BufferedImage.TYPE_INT_RGB)
        }}
    }

    /** Column labels of the kernel grid, one per input channel. */
    private val kernelColumnLabels: List<PText> = (connector as? ConvolutionConnector)?.let { conv ->
        List(conv.source.shape.channels) { kernelGridLabel("C${it + 1}") }
    } ?: emptyList()

    /** Row labels of the kernel grid, one per filter. */
    private val kernelRowLabels: List<PText> = (connector as? ConvolutionConnector)?.let { conv ->
        List(conv.numFilters) { kernelGridLabel("F${it + 1}") }
    } ?: emptyList()

    private fun kernelGridLabel(text: String) = PText(text).apply {
        textPaint = NetworkTheme.current.valueText
        kernelGridGroup.addChild(this)
    }

    /** Pre-allocated grid cell PImage nodes: [filter][channel]. Placed by [layoutKernelGrid]. */
    private val gridCellPImages: Array<Array<SimbrainImage>>? = (connector as? ConvolutionConnector)?.let { conv ->
        Array(conv.numFilters) { f ->
            Array(conv.source.shape.channels) { c ->
                SimbrainImage(gridCellImages!![f][c]).apply {
                    kernelGridGroup.addChild(this)
                    makeKernelPixelSurface(conv) { f to c }
                }
            }
        }
    }

    /** Thin outline around each grid cell: [filter][channel]. */
    private val gridCellBorders: Array<Array<PPath>>? = (connector as? ConvolutionConnector)?.let { conv ->
        Array(conv.numFilters) {
            Array(conv.source.shape.channels) {
                createRectangle(0.0, 0.0, 1.0, 1.0).apply {
                    paint = null
                    strokePaint = NetworkTheme.current.imageBorder
                    stroke = BasicStroke(0.5f)
                    pickable = false
                    kernelGridGroup.addChild(this)
                }
            }
        }
    }

    /** Side of one kernel tile in the grid, as last laid out. */
    private var kernelTileSize = 0.0

    /**
     * Place the grid's tiles and labels for tiles of side [tileSize]. Labels shrink with the tiles, and where they
     * would still run into each other only every few are shown.
     */
    private fun layoutKernelGrid(tileSize: kotlin.Double) {
        val images = gridCellPImages ?: return
        val borders = gridCellBorders ?: return
        kernelTileSize = tileSize
        val pitch = tileSize + kernelTileGap(tileSize)

        val labelFont = Theme.font((tileSize * 0.4).toInt().coerceIn(5, 10))
        (kernelColumnLabels + kernelRowLabels).forEach { it.font = labelFont }
        val left = kernelRowLabels.maxOf { it.width } + KERNEL_LABEL_GAP
        val top = kernelColumnLabels.maxOf { it.height } + KERNEL_LABEL_GAP

        val columnStep = ceil((kernelColumnLabels.maxOf { it.width } + 1) / pitch).toInt().coerceAtLeast(1)
        kernelColumnLabels.forEachIndexed { c, label ->
            label.visible = c % columnStep == 0
            label.setOffset(left + c * pitch + (tileSize - label.width) / 2, 0.0)
        }
        val rowStep = ceil(kernelRowLabels.maxOf { it.height } / pitch).toInt().coerceAtLeast(1)
        kernelRowLabels.forEachIndexed { f, label ->
            label.visible = f % rowStep == 0
            label.setOffset(0.0, top + f * pitch + (tileSize - label.height) / 2)
        }

        for (f in images.indices) {
            for (c in images[f].indices) {
                val x = left + c * pitch
                val y = top + f * pitch
                images[f][c].setBounds(x, y, tileSize, tileSize)
                borders[f][c].setBounds(x, y, tileSize, tileSize)
            }
        }
        // The group's own bounds are what selection handles attach to
        kernelGridGroup.getUnionOfChildrenBounds(null)?.let { kernelGridGroup.setBounds(it) }
    }

    /** Height the label box takes above the kernel grid, with the space between them. */
    private val kernelGridChromeAbove: kotlin.Double
        get() = interactionBox.fullBounds.height + KERNEL_GRID_LABEL_BOX_GAP

    /** Height the shape caption takes below the kernel grid, with the space between them. */
    private val kernelGridChromeBelow: kotlin.Double
        get() = if (shapeCaption.visible) shapeCaption.height + KERNEL_GRID_CAPTION_GAP else 0.0

    /**
     * Side of one kernel tile such that the grid fills the space between the source and target layers. That
     * space is taken from the arrow's [shaft], which runs from the edge of the source layer to the base of the
     * arrowhead: along the shaft the grid gets its length, less a margin and, for layers stacked vertically, the
     * room its label box and caption take. Across the shaft nothing bounds the grid, so it gets a fixed extent.
     * Tiles stop growing at [KERNEL_TILE_MAX] and stop shrinking at [KERNEL_TILE_MIN], below which the grid
     * overruns the space.
     */
    private fun fittedKernelTileSize(conv: ConvolutionConnector, shaft: CubicCurve2D): kotlin.Double {
        val shaftWidth = abs(shaft.x2 - shaft.x1)
        val shaftHeight = abs(shaft.y2 - shaft.y1)
        val (availableWidth, availableHeight) = if (shaftHeight >= shaftWidth) {
            val chrome = kernelGridChromeAbove + kernelGridChromeBelow
            KERNEL_GRID_CROSS_EXTENT to shaftHeight - chrome - 2 * KERNEL_GRID_MARGIN
        } else {
            shaftWidth - 2 * KERNEL_GRID_MARGIN to KERNEL_GRID_CROSS_EXTENT
        }
        val columns = conv.source.shape.channels
        val rows = conv.numFilters
        fun tileSizeWithGap(gap: kotlin.Double) = minOf(
            (availableWidth - KERNEL_LABEL_ALLOWANCE - (columns - 1) * gap) / columns,
            (availableHeight - KERNEL_LABEL_ALLOWANCE - (rows - 1) * gap) / rows
        )
        val roomy = tileSizeWithGap(KERNEL_TILE_GAP)
        val tileSize = if (roomy >= KERNEL_COMPACT_BELOW) {
            roomy
        } else {
            // Small tiles are laid out with a narrower gap, which leaves them slightly more room
            tileSizeWithGap(KERNEL_COMPACT_TILE_GAP).coerceAtMost(KERNEL_COMPACT_BELOW - 0.01)
        }
        return tileSize.coerceIn(KERNEL_TILE_MIN, KERNEL_TILE_MAX)
    }

    /** Re-lay out the grid if the space along the arrow's [shaft] now calls for a different tile size. */
    private fun fitKernelGrid(conv: ConvolutionConnector, shaft: CubicCurve2D) {
        val fitted = fittedKernelTileSize(conv, shaft)
        if (abs(fitted - kernelTileSize) > 0.25) layoutKernelGrid(fitted)
    }

    /**
     * Make this node, which draws the kernel given by [slice] as a filter and input channel, respond to the mouse
     * weight by weight: a value readout on hover and selection on press.
     */
    private fun PNode.makeKernelPixelSurface(conv: ConvolutionConnector, slice: () -> Pair<Int, Int>) {
        val surface = this
        pickable = true
        markAsPixelTarget()
        addInputEventListener(object : PBasicInputEventHandler() {
            private fun weightIndexAt(event: PInputEvent): Int {
                val point = event.getPositionRelativeTo(surface)
                val k = conv.kernelSize
                val row = ((point.y - surface.bounds.y) / surface.bounds.height * k).toInt().coerceIn(0, k - 1)
                val col = ((point.x - surface.bounds.x) / surface.bounds.width * k).toInt().coerceIn(0, k - 1)
                val (filter, channel) = slice()
                return conv.kernelIndex(filter, channel, row, col)
            }
            override fun mouseEntered(event: PInputEvent) {
                networkPanel.hideCanvasTooltip()
                event.isHandled = true
            }
            override fun mouseMoved(event: PInputEvent) {
                networkPanel.showPixelTooltip(event, pixelSelection.isNotEmpty()) {
                    kernelTooltipText(conv, weightIndexAt(event))
                }
            }
            override fun mouseExited(event: PInputEvent) {
                networkPanel.hideCanvasTooltip()
            }
            override fun mousePressed(event: PInputEvent) {
                val index = weightIndexAt(event)
                networkPanel.pressPixel(event, this@TensorConnectorNode) { selectWeight(index, addToSelection = it) }
            }
        })
    }

    /** Value readout for the kernel weight at [index], with filter and channel numbered from 1 as in the labels. */
    internal fun kernelTooltipText(conv: ConvolutionConnector, index: Int): String {
        val (filter, channel, row, col) = kernelCellOf(conv, index)
        return "F${filter + 1} C${channel + 1} (row $row, col $col): ${conv.kernels[index].formatSignificant(3)}"
    }

    /** Filter, input channel, row and column of the kernel weight at [index]. */
    private fun kernelCellOf(conv: ConvolutionConnector, index: Int): List<Int> {
        val k = conv.kernelSize
        val channels = conv.source.shape.channels
        return listOf(index / (k * k * channels), index / (k * k) % channels, index / k % k, index % k)
    }

    private fun selectWeight(index: Int, addToSelection: Boolean) {
        pixelSelection = if (addToSelection) {
            if (index in pixelSelection) pixelSelection - index else pixelSelection + index
        } else {
            setOf(index)
        }
    }

    override val hasPixelSelection get() = pixelSelection.isNotEmpty()

    override fun clearPixelSelection() {
        if (pixelSelection.isNotEmpty()) pixelSelection = emptySet()
    }

    override fun selectedPixels(): List<PixelTarget> {
        val conv = connector as? ConvolutionConnector ?: return emptyList()
        return pixelSelection.sorted().map { PixelTarget.KernelPixel(conv, it) }
    }

    override fun pixelsUnder(ellipse: Ellipse2D): List<PixelTarget> {
        val conv = connector as? ConvolutionConnector ?: return emptyList()
        val k = conv.kernelSize
        fun PNode.weightsUnder(filter: Int, channel: Int): List<PixelTarget> {
            val brush = Rectangle2D.Double().apply { setRect(ellipse.bounds2D) }
            globalToLocal(brush)
            val cellW = bounds.width / k
            val cellH = bounds.height / k
            return (0 until k * k)
                .filter { brush.intersects(bounds.x + it % k * cellW, bounds.y + it / k * cellH, cellW, cellH) }
                .map { PixelTarget.KernelPixel(conv, conv.kernelIndex(filter, channel, it / k, it % k)) }
        }
        return if (conv.kernelGridMode) {
            val images = gridCellPImages ?: return emptyList()
            images.flatMapIndexed { f, row -> row.flatMapIndexed { c, image -> image.weightsUnder(f, c) } }
        } else {
            imageBox.weightsUnder(currentFilter, currentInputChannel)
        }
    }

    private val arrow: BezierArrow

    init {
        pickable = true

        arrow = bezierArrow {
            color = NetworkPreferences.connectorArrowColor
            padding {
                tail = 0.0
                head = 5.0 + arrowSize
            }

            lateralOffset { 0.5 }

            onUpdated { curve ->
                val pt = curve?.p(0.5) ?: line(connector.source.location, connector.target.location).p(0.5)
                val px = pt.x
                val py = pt.y

                val conv = connector as? ConvolutionConnector
                if (conv != null && conv.kernelGridMode) {
                    // Grid mode: size the grid to the space between the layers, then center it, together with
                    // the label box above it and the caption below it, on the midpoint
                    if (curve != null) fitKernelGrid(conv, curve)
                    val gridY = py + (kernelGridChromeAbove - kernelGridChromeBelow) / 2.0
                    kernelGridGroup.centerFullBoundsOnPoint(px, gridY)
                    val gridHalfH = kernelGridGroup.fullBounds.height / 2.0
                    shapeCaption.centerFullBoundsOnPoint(
                        px, gridY + gridHalfH + shapeCaption.height / 2.0 + KERNEL_GRID_CAPTION_GAP
                    )
                    interactionBox.centerFullBoundsOnPoint(
                        px, gridY - gridHalfH - interactionBox.fullBounds.height / 2.0 - KERNEL_GRID_LABEL_BOX_GAP
                    )
                } else if (connector is ConvolutionConnector) {
                    imageBox.centerFullBoundsOnPoint(px, py)

                    val hi = imgSize.toDouble() / 2.0
                    val gap = 3.0
                    val bh = filterPrevButton!!.fullBounds.height
                    val bw = channelPrevButton!!.fullBounds.width

                    filterPrevButton.centerFullBoundsOnPoint(px, py - hi - gap - bh / 2)
                    filterNextButton!!.centerFullBoundsOnPoint(px, py + hi + gap + bh / 2)
                    channelPrevButton.centerFullBoundsOnPoint(px - hi - gap - bw / 2, py)
                    channelNextButton!!.centerFullBoundsOnPoint(px + hi + gap + bw / 2, py)

                    val bottomEdge = py + hi + gap + bh
                    detailLabel.centerFullBoundsOnPoint(px, bottomEdge + 6.0)
                    shapeCaption.centerFullBoundsOnPoint(
                        px, detailLabel.fullBounds.maxY + shapeCaption.height / 2.0 + 3.0
                    )
                    interactionBox.centerFullBoundsOnPoint(
                        px, py - hi - gap - bh - interactionBox.fullBounds.height / 2.0 - 1.0
                    )
                } else {
                    interactionBox.centerFullBoundsOnPoint(px, py)
                }
            }
        }

        addChild(arrow)
        addChild(interactionBox)

        if (connector is ConvolutionConnector) {
            addChild(filterPrevButton)
            addChild(filterNextButton)
            addChild(channelPrevButton)
            addChild(channelNextButton)
            addChild(imageBox)
            addChild(detailLabel)
            addChild(kernelGridGroup)
            addChild(shapeCaption)
            layoutKernelGrid(KERNEL_TILE_DEFAULT)
            renderKernelImage()
            syncKernelDisplayMode()
        }

        // Wire up events
        val connectorEvents = connector.events
        connectorEvents.updated.on(Dispatchers.Default) { connectorEvents.updateGraphics.fire() }.untilDisposed()
        connectorEvents.updateGraphics.on(Dispatchers.Swing) {
            if (connector is ConvolutionConnector) {
                if (connector.kernelGridMode) {
                    renderKernelGrid()
                } else {
                    renderKernelImage()
                }
            }
            updateDetailLabel()
        }.untilDisposed()
        connectorEvents.labelChanged.on(Dispatchers.Swing) { _, _ ->
            interactionBox.setText(connectorDisplayText())
        }.untilDisposed()
        connectorEvents.visualPropertiesChanged.on(Dispatchers.Swing) {
            syncKernelDisplayMode()
        }.untilDisposed()

        connector.source.events.locationChanged.on(Dispatchers.Swing) {
            arrow.invalidateFullBounds()
        }.untilDisposed()
        connector.target.events.locationChanged.on(Dispatchers.Swing) {
            arrow.invalidateFullBounds()
        }.untilDisposed()

        interactionBox.setText(connectorDisplayText())
        arrow.invalidateFullBounds()
        updateDetailLabel()
    }

    /** Switch visibility between single-kernel UI and grid UI. */
    private fun syncKernelDisplayMode() {
        val conv = connector as? ConvolutionConnector ?: return
        if (conv.kernelGridMode) {
            imageBox.visible = false
            filterPrevButton?.visible = false
            filterNextButton?.visible = false
            channelPrevButton?.visible = false
            channelNextButton?.visible = false
            detailLabel.visible = false
            kernelGridGroup.visible = true
            renderKernelGrid()
        } else {
            imageBox.visible = true
            filterPrevButton?.visible = true
            filterNextButton?.visible = true
            channelPrevButton?.visible = true
            channelNextButton?.visible = true
            detailLabel.visible = true
            kernelGridGroup.visible = false
            renderKernelImage()
            updateDetailLabel()
        }
        arrow.invalidateFullBounds()
    }

    /** Trace highlight set by the receptive field tracer. */
    var traceHighlight: ConnectorTraceHighlight? = null

    override fun refreshTheme() {
        renderKernelImage()
        renderKernelGrid()
        updateArrowColorFromPreferences()
        updateDetailLabel()
        shapeCaption.refresh()
        kernelGridGroup.allNodes.forEach { node ->
            when (node) {
                is PText -> node.textPaint = NetworkTheme.current.valueText
                is PPath -> node.strokePaint = NetworkTheme.current.imageBorder
            }
        }
    }

    fun renderKernelImage() {
        val conv = connector as? ConvolutionConnector ?: return
        val slice = kernelSlice ?: return
        val img = singleKernelImage ?: return
        val kSize = conv.kernelSize
        val inC = conv.source.shape.channels
        val kernelArea = kSize * kSize
        val filterOffset = currentFilter * inC * kernelArea + currentInputChannel * kernelArea
        for (i in 0 until kernelArea) {
            slice[i] = conv.kernels[filterOffset + i]
        }
        slice.writeSimbrainColorImage(img)
        imageBox.image = img
    }

    private fun renderKernelGrid() {
        val conv = connector as? ConvolutionConnector ?: return
        val slice = kernelSlice ?: return
        val cellImages = gridCellImages ?: return
        val cellPImages = gridCellPImages ?: return

        val inputChannels = conv.source.shape.channels
        val kSize = conv.kernelSize
        val kernelArea = kSize * kSize

        for (f in 0 until conv.numFilters) {
            for (c in 0 until inputChannels) {
                val filterOffset = f * inputChannels * kernelArea + c * kernelArea
                for (i in 0 until kernelArea) {
                    slice[i] = conv.kernels[filterOffset + i]
                }
                slice.writeSimbrainColorImage(cellImages[f][c])
                cellPImages[f][c].invalidatePaint()
            }
        }
    }

    private fun drawSingleKernelNumericOverlay(g2: Graphics2D, conv: ConvolutionConnector) {
        val slice = kernelSlice ?: return
        val kSize = conv.kernelSize
        val boxOffset = imageBox.offset
        g2.drawNumericOverlay(
            data = slice,
            rows = kSize, cols = kSize,
            imageWidth = imgSize.toDouble(), imageHeight = imgSize.toDouble(),
            scalingFactor = networkPanel.scalingFactor,
            decimalPlaces = NetworkPreferences.neuronActivationDecimalPlaces,
            offsetX = boxOffset.x,
            offsetY = boxOffset.y
        )
    }

    private fun drawKernelGridNumericOverlay(g2: Graphics2D, conv: ConvolutionConnector) {
        val slice = kernelSlice ?: return
        val cellPImages = gridCellPImages ?: return
        val inputChannels = conv.source.shape.channels
        val kernelArea = conv.kernelSize * conv.kernelSize
        val gridOffset = kernelGridGroup.offset

        for (f in 0 until conv.numFilters) {
            for (c in 0 until inputChannels) {
                val filterOffset = f * inputChannels * kernelArea + c * kernelArea
                for (i in 0 until kernelArea) {
                    slice[i] = conv.kernels[filterOffset + i]
                }

                val cellBounds = cellPImages[f][c].bounds
                g2.drawNumericOverlay(
                    data = slice,
                    rows = conv.kernelSize,
                    cols = conv.kernelSize,
                    imageWidth = cellBounds.width,
                    imageHeight = cellBounds.height,
                    scalingFactor = networkPanel.scalingFactor,
                    decimalPlaces = NetworkPreferences.neuronActivationDecimalPlaces,
                    offsetX = gridOffset.x + cellBounds.x,
                    offsetY = gridOffset.y + cellBounds.y
                )
            }
        }
    }

    fun updateDetailLabel() {
        detailLabel.textPaint = NetworkTheme.current.valueText
        if (connector is ConvolutionConnector) {
            val c = connector
            detailLabel.text = "F${currentFilter + 1}/${c.numFilters} Ch${currentInputChannel + 1}/${c.source.shape.channels}"
        }
    }

    fun nextFilter() {
        val conv = connector as? ConvolutionConnector ?: return
        clearPixelSelection()
        currentFilter = (currentFilter + 1) % conv.numFilters
        renderKernelImage()
        updateDetailLabel()
    }

    fun previousFilter() {
        val conv = connector as? ConvolutionConnector ?: return
        clearPixelSelection()
        currentFilter = (currentFilter - 1 + conv.numFilters) % conv.numFilters
        renderKernelImage()
        updateDetailLabel()
    }

    fun nextInputChannel() {
        val conv = connector as? ConvolutionConnector ?: return
        clearPixelSelection()
        currentInputChannel = (currentInputChannel + 1) % conv.source.shape.channels
        renderKernelImage()
        updateDetailLabel()
    }

    fun previousInputChannel() {
        val conv = connector as? ConvolutionConnector ?: return
        clearPixelSelection()
        currentInputChannel = (currentInputChannel - 1 + conv.source.shape.channels) % conv.source.shape.channels
        renderKernelImage()
        updateDetailLabel()
    }

    fun updateArrowColorFromPreferences() {
        arrow.updateColorFromPreferences()
        layoutChildren()
    }

    override fun layoutChildren() {
        val srcBodyBounds = sourceNode?.globalBounds ?: return
        val tgtBodyBounds = targetNode?.globalBounds ?: return
        val srcTopBounds = sourceNode?.globalFullBounds ?: return
        val tgtTopBounds = targetNode?.globalFullBounds ?: return
        arrow.layout(
            directionalOutlines(srcTopBounds, srcBodyBounds),
            directionalOutlines(tgtTopBounds, tgtBodyBounds),
            false
        )
    }

    override fun paint(paintContext: PPaintContext) {
        paintContext.graphics.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        )
        super.paint(paintContext)
    }

    override fun paintAfterChildren(paintContext: PPaintContext) {
        super.paintAfterChildren(paintContext)
        val g2 = paintContext.graphics
        val conv = connector as? ConvolutionConnector

        // Trace highlighting
        val highlight = traceHighlight
        if (highlight != null && conv != null && conv.kernelGridMode) {
            val cellPImages = gridCellPImages
            if (cellPImages != null) {
                g2.color = highlight.color
                g2.stroke = BasicStroke(2f)
                val gridOffset = kernelGridGroup.offset

                val cells = when (highlight.mode) {
                    HighlightMode.ROW -> {
                        val f = highlight.filter.coerceIn(0, cellPImages.size - 1)
                        (0 until cellPImages[f].size).map { c -> cellPImages[f][c] }
                    }
                    HighlightMode.COLUMN -> {
                        val c = highlight.inputChannel.coerceIn(0, cellPImages[0].size - 1)
                        (0 until cellPImages.size).map { f -> cellPImages[f][c] }
                    }
                    HighlightMode.CELL -> {
                        val f = highlight.filter.coerceIn(0, cellPImages.size - 1)
                        val c = highlight.inputChannel.coerceIn(0, cellPImages[0].size - 1)
                        listOf(cellPImages[f][c])
                    }
                }

                for (cell in cells) {
                    val cellBounds = cell.bounds
                    val drawX = gridOffset.x + cellBounds.x
                    val drawY = gridOffset.y + cellBounds.y
                    g2.drawRect(drawX.toInt(), drawY.toInt(), cellBounds.width.toInt(), cellBounds.height.toInt())
                }
            }
        }

        if (conv != null) drawPixelSelection(g2, conv)

        if (NetworkPreferences.showNumericOverlays && conv != null) {
            if (conv.kernelGridMode) {
                drawKernelGridNumericOverlay(g2, conv)
            } else {
                drawSingleKernelNumericOverlay(g2, conv)
            }
        }
    }

    private fun drawPixelSelection(g2: Graphics2D, conv: ConvolutionConnector) {
        if (pixelSelection.isEmpty()) return
        val k = conv.kernelSize
        g2.color = NeuronArrayNode.PIXEL_SELECTION_COLOR
        for (index in pixelSelection) {
            val (filter, channel, row, col) = kernelCellOf(conv, index)
            // Top left corner and side of the kernel this weight is drawn in, or skip it if that kernel is not shown
            val (x, y, side) = if (conv.kernelGridMode) {
                val cell = gridCellPImages?.getOrNull(filter)?.getOrNull(channel)?.bounds ?: continue
                Triple(kernelGridGroup.offset.x + cell.x, kernelGridGroup.offset.y + cell.y, cell.width)
            } else {
                if (filter != currentFilter || channel != currentInputChannel) continue
                Triple(imageBox.offset.x, imageBox.offset.y, imgSize.toDouble())
            }
            val cellSize = side / k
            // Thinner on small tiles so the outline does not swallow the weight it marks
            g2.stroke = BasicStroke((cellSize / 3).coerceIn(1.0, 3.0).toFloat())
            g2.draw(Rectangle2D.Double(x + col * cellSize, y + row * cellSize, cellSize, cellSize))
        }
    }

    /**
     * Text shown on the interaction box: the custom label if set, otherwise the short [name] (e.g. "Pooling")
     * instead of the full id.
     */
    private fun connectorDisplayText(): String =
        connector.label?.takeIf { it.isNotEmpty() } ?: connector.name

    override val isDraggable: Boolean = false

    override val contextMenu: JPopupMenu
        get() {
            val contextMenu = JPopupMenu()
            contextMenu.add(networkPanel.networkActions.deleteAction)
            contextMenu.addSeparator()
            contextMenu.add(networkPanel.createAction(name = "Edit...") {
                createEditDialog()?.display()
            })

            if (connector is ConvolutionConnector) {
                contextMenu.addSeparator()

                // Kernel grid toggle
                contextMenu.add(networkPanel.createAction(
                    name = if (connector.kernelGridMode) "Single Kernel View" else "Kernel Grid View"
                ) {
                    connector.kernelGridMode = !connector.kernelGridMode
                })

                contextMenu.addSeparator()
                contextMenu.add(networkPanel.networkActions.randomizeObjectsAction)
            }

            return contextMenu
        }

    override fun createEditDialog(): StandardDialog? {
        return networkPanel.createSelectionEditDialog(connector)
    }

    override val propertyDialog: StandardDialog?
        get() = (if (pixelSelection.isNotEmpty()) networkPanel.createPixelEditDialog() else null) ?: createEditDialog()

    override val model: TensorConnector get() = connector

    /** Tooltip giving the full input/output shapes and, for convolutions, the kernel shape with named axes. */
    override val toolTipText: String?
        get() = if (pixelSelection.isNotEmpty()) null else createTooltipText(connector) {
            val src = connector.source.shape.displayString
            val tgt = connector.target.shape.displayString
            when (connector) {
                is ConvolutionConnector -> {
                    val k = connector.kernelSize
                    val inC = connector.source.shape.channels
                    val weights = connector.numFilters * inC * k * k
                    """
                    ${connector.displayName}: $src → $tgt
                    Kernels: ${connector.kernelShapeString} (${connector.numFilters} filters × $inC channels × ${k}×$k)
                    Stride ${connector.stride}, ${connector.padding.name.lowercase()} padding
                    Parameters: $weights weights + ${connector.numFilters} biases
                    """.trimIndent()
                }
                is PoolingConnector -> """
                    ${connector.displayName}: $src → $tgt
                    ${connector.poolingType.name.lowercase().replaceFirstChar { it.uppercase() }} pool ${connector.poolSize}×${connector.poolSize}, stride ${connector.stride}
                    """.trimIndent()
                else -> "${connector.displayName}: $src → $tgt"
            }
        }

    override fun isIntersecting(bound: PBounds?): Boolean {
        if (bound == null) return false
        return imageBox.globalBounds.intersects(bound) ||
                arrow.globalBounds.intersects(bound) ||
                interactionBox.globalBounds.intersects(bound) ||
                detailLabel.globalBounds.intersects(bound) ||
                kernelGridGroup.globalBounds.intersects(bound) ||
                (filterPrevButton?.globalBounds?.intersects(bound) == true) ||
                (filterNextButton?.globalBounds?.intersects(bound) == true) ||
                (channelPrevButton?.globalBounds?.intersects(bound) == true) ||
                (channelNextButton?.globalBounds?.intersects(bound) == true)
    }

    inner class TensorConnectorInteractionBox(net: NetworkPanel) : InteractionBox(net) {
        override val contextMenu: JPopupMenu
            get() = this@TensorConnectorNode.contextMenu
        override fun createEditDialog(): StandardDialog? =
            this@TensorConnectorNode.createEditDialog()
        override val propertyDialog: StandardDialog?
            get() = this@TensorConnectorNode.createEditDialog()
        override val isDraggable: Boolean get() = false
        override val model: TensorConnector
            get() = this@TensorConnectorNode.connector
        override val toolTipText: String?
            get() = this@TensorConnectorNode.toolTipText
    }

    companion object {

        /** Tile side before the first layout against the layers. */
        private const val KERNEL_TILE_DEFAULT = 20.0

        private const val KERNEL_TILE_MIN = 8.0

        private const val KERNEL_TILE_MAX = 80.0

        private const val KERNEL_TILE_GAP = 2.0

        /** Tiles smaller than this are spaced by [KERNEL_COMPACT_TILE_GAP] instead of [KERNEL_TILE_GAP]. */
        private const val KERNEL_COMPACT_BELOW = 10.0

        private const val KERNEL_COMPACT_TILE_GAP = 1.0

        private fun kernelTileGap(tileSize: kotlin.Double) =
            if (tileSize < KERNEL_COMPACT_BELOW) KERNEL_COMPACT_TILE_GAP else KERNEL_TILE_GAP

        /** Space between the grid's labels and its tiles. */
        private const val KERNEL_LABEL_GAP = 2.0

        /** Room set aside for the row or column labels when fitting the grid, whatever font they end up in. */
        private const val KERNEL_LABEL_ALLOWANCE = 16.0

        /** How far the grid may extend across the direction its layers are separated in. */
        private const val KERNEL_GRID_CROSS_EXTENT = 240.0

        /** Space kept clear between the grid, with its label box and caption, and each end of the arrow's shaft. */
        private const val KERNEL_GRID_MARGIN = 6.0

        private const val KERNEL_GRID_LABEL_BOX_GAP = 3.0

        private const val KERNEL_GRID_CAPTION_GAP = 4.0
    }
}
